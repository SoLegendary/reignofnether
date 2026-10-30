package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Finds a valid building spot around an anchor: as near it as possible and on the side away from the enemy.
// RoN's own validator decides legality (flat ground, overlap, nether ground, border); on top of that the bot
// keeps a gap around every building so units can walk between them, and skips spots that failed before.
public class BotPlacer {

    public static final int GAP = 2;
    private static final int RING_STEP = 2;
    private static final int MAX_CANDIDATES = 6000; // cheap checks only; rings get sparser further out
    // RoN's validator re-reads the structure file on every call, so only spots that pass the cheap checks get it
    private static final int MAX_VALIDATIONS = 48;

    private static final Map<Building, Vec3i> sizeCache = new HashMap<>();
    private static final int MAX_VALIDATIONS_PER_TICK = 96; // shared by all bots
    private static final long FAILED_SPOT_TICKS = 5 * 60 * 20;
    private static long budgetTick = -1;
    private static int budgetUsed = 0;
    private final Map<Long, Long> failedSpots = new HashMap<>(); // cell -> game time it failed (any building)
    // origins RoN's validator rejected, per building: a farm that needs nether ground mustn't push a portal away.
    // Exact origins, not cells: a raised try failing mustn't skip the sunk one, or a whole cell next to it
    private final Map<Building, Map<Long, Long>> rejected = new HashMap<>();
    public java.util.function.Predicate<BlockPos> avoid = p -> false; // e.g. near neutral camps
    public java.util.function.Predicate<BlockPos> reachable = p -> true; // a worker can walk there
    // set for each search: the direction the enemy is in (spots on the far side come first in each ring), and where
    // enemy troops were seen lately (no building goes up within DANGER_RANGE of those)
    public Vec3 enemyDir = null;
    public List<BlockPos> danger = List.of();
    // set for each search: drop-offs (capitol, stockpiles) keep this much more room around them, so a ring of farms
    // still fits there (0 when placing a farm)
    public int dropOffMargin = 0;
    private static final double ENEMY_SIDE_WEIGHT = 0.8; // a spot facing the enemy counts as this much further
    private static final int DANGER_RANGE = 20;
    public boolean waiting; // set by reachable while its path is still being worked out: stop and retry shortly
    private static final int MAX_ASKS = 3;
    public int invalid, unwalkable; // spots the last search turned down, for the bot's log
    // spots the cheap checks turned down, and why the search stopped, also for the log
    public int camp, notFlat, trees, failedBefore, noGap;
    public String stop = "searched";
    public final Map<String, Integer> reasons = new TreeMap<>(); // RoN's reasons for the invalid ones

    public static Vec3i getSize(ServerLevel level, Building building) {
        return sizeCache.computeIfAbsent(building, b -> BuildingUtils.getBuildingSize(b.getRelativeBlockData(level)));
    }

    public void markFailed(BlockPos origin, long gameTime) {
        failedSpots.put(cell(origin), gameTime);
    }

    private static long cell(BlockPos origin) {
        return BlockPos.asLong(origin.getX() >> 2, 0, origin.getZ() >> 2);
    }

    private boolean hasFailed(Building building, BlockPos origin, long gameTime) {
        Long when = failedSpots.get(cell(origin));
        Long rej = rejected.getOrDefault(building, Map.of()).get(origin.asLong());
        return (when != null && gameTime - when < FAILED_SPOT_TICKS) || (rej != null && gameTime - rej < FAILED_SPOT_TICKS);
    }

    // returns a building origin (as passed to placeBuilding) or null if nothing fits between minR and maxR
    @Nullable
    public BlockPos findSpot(ServerLevel level, Building building, String ownerName, BlockPos anchor, int minR, int maxR) {
        Vec3i size = getSize(level, building);
        long now = level.getGameTime();
        if (budgetTick != now) {
            budgetTick = now;
            budgetUsed = 0;
        }
        failedSpots.values().removeIf(when -> now - when >= FAILED_SPOT_TICKS);
        rejected.values().forEach(cells -> cells.values().removeIf(when -> now - when >= FAILED_SPOT_TICKS));
        waiting = false;
        invalid = unwalkable = camp = notFlat = trees = failedBefore = noGap = 0;
        reasons.clear();
        stop = "searched";
        boolean asking = false; // a spot's walk is still being worked out
        int asks = 0;
        int candidates = 0, validations = 0;
        // only buildings the search area can touch are checked for the gap, not every building on the server
        int reach = maxR + Math.max(size.getX(), size.getZ()) + GAP + dropOffMargin;
        List<BuildingPlacement> nearby = BuildingServerEvents.getBuildings().stream().filter(b -> overlaps(
                anchor.getX() - reach, anchor.getZ() - reach, anchor.getX() + reach, anchor.getZ() + reach,
                b.minCorner.getX(), b.minCorner.getZ(), b.maxCorner.getX(), b.maxCorner.getZ())).toList();
        for (BlockPos centre : bestFirst(anchor, minR, maxR, enemyDir)) {
            if (++candidates > MAX_CANDIDATES || validations >= MAX_VALIDATIONS || budgetUsed >= MAX_VALIDATIONS_PER_TICK) {
                stop = candidates > MAX_CANDIDATES ? "candidate cap" : validations >= MAX_VALIDATIONS ? "validation cap" : "tick budget";
                // other bots used this tick's validations: that's no answer, so retry shortly like a pending path
                waiting = asking || budgetUsed >= MAX_VALIDATIONS_PER_TICK;
                return null;
            }
            if (!level.getWorldBorder().isWithinBounds(centre))
                continue;
            if (avoid.test(centre) || inDanger(centre)) {
                camp++;
                continue;
            }
            Integer top = supportTop(level, centre, size);
            if (top == null) {
                notFlat++;
                if (onTrees(level, centre, size))
                    trees++;
                continue;
            }
            BlockPos ground = new BlockPos(centre.getX(), top, centre.getZ());
            // sits on the highest ground under it: never clips into a block, and supportTop made sure enough of
            // it rests on ground, so RoN's validator nearly always agrees and no validation is wasted
            BlockPos origin = BuildingUtils.getBuildingOriginPos(ground.above(), false, Rotation.NONE, size);
            if (hasFailed(building, origin, now)) {
                failedBefore++;
                continue;
            }
            if (!hasGap(origin, size, nearby, dropOffMargin)) {
                noGap++;
                continue;
            }
            validations++;
            budgetUsed++;
            // a spot that fails is skipped by later searches, so they reach further out each time
            // instead of re-checking the same full rings near the anchor
            String error = BuildingValidators.getPlacementValidityError(level, building, origin, ownerName,
                    Rotation.NONE, false, false, true);
            boolean valid = error == null;
            if (!valid)
                reasons.merge(error.replace("building.reignofnether.", ""), 1, Integer::sum);
            if (!valid || !reachable.test(ground.above())) {
                if (valid && waiting) {
                    // ask the pathfinder about a few spots at once, then wait for the answers. Never keep
                    // validating past that: the validation budget is shared by every bot each tick
                    if (++asks >= MAX_ASKS) {
                        stop = "waiting for paths";
                        return null; // waiting stays true
                    }
                    asking = true;
                    waiting = false;
                    continue;
                }
                if (valid) unwalkable++; else invalid++;
                rejected.computeIfAbsent(building, k -> new HashMap<>()).put(origin.asLong(), now);
                continue;
            }
            // a nearer spot is still being asked about: don't jump past it to one further out
            if (asking)
                continue;
            return origin;
        }
        waiting = asking;
        return null;
    }

    private static final double MIN_SUPPORT = 0.7; // RoN wants 60% of the floor on solid ground; a margin for gaps between samples

    // the highest ground under the footprint, sampled every 2 blocks, or null when it can't sit there: water, more
    // than a block of height difference, or too little of it at that top height to carry the floor
    @Nullable
    static Integer supportTop(ServerLevel level, BlockPos centre, Vec3i size) {
        int x0 = centre.getX() - size.getX() / 2, z0 = centre.getZ() - size.getZ() / 2;
        int x1 = x0 + size.getX() - 1, z1 = z0 + size.getZ() - 1;
        List<Integer> ys = new ArrayList<>();
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int x = x0; x <= x1; x = x == x1 ? x + 1 : Math.min(x + 2, x1))
            for (int z = z0; z <= z1; z = z == z1 ? z + 1 : Math.min(z + 2, z1)) {
                BlockPos ground = MiscUtil.getHighestNonAirBlockFromHeightmap(level, new BlockPos(x, centre.getY(), z), true);
                if (!level.getFluidState(ground).isEmpty())
                    return null;
                min = Math.min(min, ground.getY());
                max = Math.max(max, ground.getY());
                if (max - min > 1)
                    return null;
                ys.add(ground.getY());
            }
        int top = max;
        return ys.stream().filter(y -> y == top).count() >= MIN_SUPPORT * ys.size() ? top : null;
    }

    // the footprint sits on tree trunks (leaves are ignored as ground, logs aren't): a forest, not a slope
    private static boolean onTrees(ServerLevel level, BlockPos centre, Vec3i size) {
        int hx = size.getX() / 2, hz = size.getZ() / 2;
        for (BlockPos p : List.of(centre, centre.offset(-hx, 0, -hz), centre.offset(hx, 0, -hz), centre.offset(-hx, 0, hz), centre.offset(hx, 0, hz)))
            if (level.getBlockState(MiscUtil.getHighestNonAirBlockFromHeightmap(level, p, true)).is(BlockTags.LOGS))
                return true;
        return false;
    }

    // keeps a walkable gap between the footprint and every other building, plus dropOffMargin around drop-offs
    static boolean hasGap(BlockPos origin, Vec3i size, List<BuildingPlacement> buildings, int dropOffMargin) {
        for (BuildingPlacement b : buildings) {
            int gap = GAP + (b.isCapitol || b.getBuilding().canAcceptResources ? dropOffMargin : 0);
            if (overlaps(origin.getX() - gap, origin.getZ() - gap, origin.getX() + size.getX() + gap, origin.getZ() + size.getZ() + gap,
                    b.minCorner.getX(), b.minCorner.getZ(), b.maxCorner.getX(), b.maxCorner.getZ()))
                return false;
        }
        return true;
    }

    static boolean overlaps(int ax1, int az1, int ax2, int az2, int bx1, int bz1, int bx2, int bz2) {
        return ax1 <= bx2 && ax2 >= bx1 && az1 <= bz2 && az2 >= bz1;
    }

    private boolean inDanger(BlockPos p) {
        for (BlockPos d : danger)
            if (d.distSqr(p) < DANGER_RANGE * DANGER_RANGE)
                return true;
        return false;
    }

    // every ring's spots between minR and maxR, best first: nearest the anchor, a spot facing the enemy counting
    // as further than one facing away
    static List<BlockPos> bestFirst(BlockPos anchor, int minR, int maxR, @Nullable Vec3 enemyDir) {
        List<BlockPos> all = new ArrayList<>();
        for (int r = minR; r <= maxR; r += RING_STEP)
            all.addAll(ring(anchor, r));
        all.sort(Comparator.comparingDouble(p -> score(p, anchor, enemyDir)));
        return all;
    }

    static double score(BlockPos p, BlockPos anchor, @Nullable Vec3 enemyDir) {
        int dx = p.getX() - anchor.getX(), dz = p.getZ() - anchor.getZ();
        double towardsEnemy = enemyDir == null ? 0 : dx * enemyDir.x + dz * enemyDir.z;
        return Math.sqrt(dx * dx + dz * dz) + ENEMY_SIDE_WEIGHT * towardsEnemy;
    }

    // block positions on the perimeter of a square of radius r around the anchor, spaced wider on big rings so
    // a search still reaches the outer rings of a full base before the candidate cap
    static List<BlockPos> ring(BlockPos anchor, int r) {
        List<BlockPos> list = new ArrayList<>();
        if (r == 0) {
            list.add(anchor);
            return list;
        }
        int step = Math.max(RING_STEP, r / 10);
        for (int i = -r; i < r; i += step) {
            list.add(anchor.offset(i, 0, -r));
            list.add(anchor.offset(r, 0, i));
            list.add(anchor.offset(-i, 0, r));
            list.add(anchor.offset(-r, 0, -i));
        }
        return list;
    }
}
