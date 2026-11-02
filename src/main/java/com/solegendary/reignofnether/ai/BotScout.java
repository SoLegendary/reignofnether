package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Two scouts keep the bot informed: enemy starts first, then whatever part of the map we've gone longest without
// seeing, enemy bases counting most. The map is split into cells, each remembering when any of our units last saw
// it. Scouts run from fights on the way, but push into an enemy base they were sent to look at, even if it costs
// the scout: knowing what's there is worth a dog. A lost scout is replaced every few minutes while fog of war is on
// (without fog everything is visible anyway).
public class BotScout {

    private static final int FLEE_RANGE = 12;
    private static final int SCOUTS = 2;
    private static final int RETRAIN_TICKS = 2 * 60 * 20;
    private static final int RETARGET_TICKS = 60 * 20; // couldn't get there in this long: give up on that spot
    private static final int CELL = 48;
    private static final int PUSH_IN_RANGE = 40; // this close to an enemy base it was sent to, a scout stops running
    private static final int BASE_WEIGHT = 3; // a cell with an enemy building goes stale this much faster

    private final BotBrain bot;
    private long lastTrainTick = -1_000_000;
    private final Map<Long, Long> cellSeen = new HashMap<>(); // cell -> tick any of our units last saw its middle
    private long cellCheckTick = -1_000_000;

    BotScout(BotBrain bot) {
        this.bot = bot;
    }

    void think() {
        if (bot.scouts.size() < SCOUTS && bot.tick - lastTrainTick > RETRAIN_TICKS)
            retrain();
        updateCells();
        if (lookAt != null) { // the nearest scout goes to look now
            BlockPos p = lookAt;
            lookAt = null;
            bot.scouts.stream().filter(s -> !bot.unit(s).fleeing)
                    .min(Comparator.comparingDouble(s -> s.blockPosition().distSqr(p))).ifPresent(s -> send(s, bot.unit(s), p));
        }
        for (LivingEntity s : bot.scouts) {
            BotUnit state = bot.unit(s);
            if (flee(s, state))
                continue;
            BlockPos target = state.scoutTarget;
            boolean arrived = target != null && s.blockPosition().distSqr(target) < 8 * 8;
            boolean stale = target != null && bot.tick - state.scoutTargetTick > RETARGET_TICKS;
            if (stale)
                cellSeen.put(cell(target), bot.tick); // stuck or unreachable: try somewhere else, come back later
            // a start position or cell is crossed off as soon as it's in sight, no need to walk all the way
            boolean seen = target != null && (bot.memory.isEnemyStart(target) ? !bot.memory.unscoutedStarts.contains(target) :
                    bot.tick - cellSeen.getOrDefault(cell(target), 0L) < 40);
            if (target == null || arrived || stale || seen)
                send(s, state, nextTarget(s));
            else if (((Unit) s).isIdle())
                bot.order(UnitAction.MOVE, List.of(s), target); // got interrupted on the way
        }
    }

    // every 2 s: the cells any of our units or buildings see right now
    private void updateCells() {
        if (bot.tick - cellCheckTick < 40 || bot.level == null)
            return;
        cellCheckTick = bot.tick;
        int[] a = area();
        for (int x = a[0]; x < a[2]; x += CELL)
            for (int z = a[1]; z < a[3]; z += CELL)
                if (bot.memory.isSeen(new BlockPos(x, 0, z)))
                    cellSeen.put(cell(new BlockPos(x, 0, z)), bot.tick);
    }

    private static final int MAX_AREA = 768; // never further from home than this: a world without a map border is 60M wide

    // cell centres to scan: the world border, cut down to MAX_AREA around home. {minX, minZ, maxX, maxZ}
    private int[] area() {
        var border = bot.level.getWorldBorder();
        int minX = (int) Math.max(border.getMinX(), bot.home.getX() - MAX_AREA), minZ = (int) Math.max(border.getMinZ(), bot.home.getZ() - MAX_AREA);
        int maxX = (int) Math.min(border.getMaxX(), bot.home.getX() + MAX_AREA), maxZ = (int) Math.min(border.getMaxZ(), bot.home.getZ() + MAX_AREA);
        return new int[]{Math.floorDiv(minX, CELL) * CELL + CELL / 2, Math.floorDiv(minZ, CELL) * CELL + CELL / 2, maxX, maxZ};
    }

    private static long cell(BlockPos p) {
        return BlockPos.asLong(Math.floorDiv(p.getX(), CELL), 0, Math.floorDiv(p.getZ(), CELL));
    }

    private boolean flee(LivingEntity s, BotUnit state) {
        List<LivingEntity> danger = bot.memory.enemiesNear(s.position(), FLEE_RANGE).stream().filter(e -> e instanceof AttackerUnit).toList();
        BotMemory.Camp camp = bot.memory.camps.values().stream()
                .filter(c -> c.pos().distSqr(s.blockPosition()) < FLEE_RANGE * FLEE_RANGE).findFirst().orElse(null);
        if (danger.isEmpty() && camp == null)
            return false;
        // sent to look at an enemy base and nearly there: keep going, what it sees is worth the scout
        if (camp == null && state.scoutTarget != null && isEnemyBase(state.scoutTarget) &&
                s.blockPosition().distSqr(state.scoutTarget) < PUSH_IN_RANGE * PUSH_IN_RANGE)
            return false;
        if (state.scoutTarget != null) // the way there is blocked: that spot waits, or we'd run back and forth
            cellSeen.put(cell(state.scoutTarget), bot.tick);
        BlockPos from = !danger.isEmpty() ? BotMath.centroid(danger) : camp.pos();
        Vec3 away = s.position().subtract(Vec3.atCenterOf(from)).multiply(1, 0, 1);
        Vec3 dir = away.lengthSqr() < 0.01 ? Vec3.atCenterOf(bot.home).subtract(s.position()) : away;
        BlockPos to = BlockPos.containing(s.position().add(dir.normalize().scale(16)));
        BlockPos going = ((Unit) s).getMoveGoal().getMoveTarget();
        if (going == null || going.distSqr(to) > 8 * 8) // already running that way: no new order
            bot.order(UnitAction.MOVE, List.of(s), to);
        state.scoutTarget = null;
        return true;
    }

    private boolean send(LivingEntity s, BotUnit state, BlockPos to) {
        if (!bot.order(UnitAction.MOVE, List.of(s), to))
            return false;
        state.scoutTarget = to;
        state.scoutTargetTick = bot.tick;
        return true;
    }

    // any of our units saw p's cell this recently
    boolean seenWithin(BlockPos p, long ticks) {
        return !FogOfWarServerEvents.isEnabled() || bot.memory.isSeen(p) || bot.tick - cellSeen.getOrDefault(cell(p), -1_000_000L) < ticks;
    }

    // the economy wants to send workers there but nobody has looked lately: the next free scout goes first
    void lookAt(BlockPos p) {
        if (lookAt == null)
            lookAt = p;
    }

    private BlockPos lookAt = null;

    // an ally pinged an enemy: the scout nearest it goes to look
    boolean answerEnemy(BlockPos pos) {
        LivingEntity s = bot.scouts.stream().min(Comparator.comparingDouble(e -> e.blockPosition().distSqr(pos))).orElse(null);
        return s != null && send(s, bot.unit(s), pos);
    }

    private boolean isEnemyBase(BlockPos p) {
        return bot.memory.isEnemyStart(p) || bot.memory.enemyBuildings.values().stream().anyMatch(b -> b.pos().distSqr(p) < CELL * CELL);
    }

    // the army's pick when it knows of nothing: the stalest spot on the map
    BlockPos nextTarget() {
        return nextTarget(null);
    }

    // unscouted enemy starts first (nearest), then the cell we've gone longest without seeing: enemy bases count
    // BASE_WEIGHT times, nearer is a little better, neutral camps and the other scout's spot are left out
    BlockPos nextTarget(@Nullable LivingEntity scout) {
        Set<Long> taken = new HashSet<>();
        for (LivingEntity o : bot.scouts)
            if (o != scout && bot.unit(o).scoutTarget != null)
                taken.add(cell(bot.unit(o).scoutTarget));
        if (scout != null && lookAt != null && !taken.contains(cell(lookAt))) {
            BlockPos p = lookAt;
            lookAt = null;
            return p;
        }
        for (BlockPos start : bot.memory.unscoutedStarts)
            if (!taken.contains(cell(start)))
                return start;
        BlockPos from = scout != null ? scout.blockPosition() : bot.home;
        Set<Long> baseCells = new HashSet<>();
        for (BotMemory.SeenBuilding b : bot.memory.enemyBuildings.values())
            baseCells.add(cell(b.pos()));
        int[] a = area();
        BlockPos best = null;
        double bestScore = -1;
        for (int x = a[0]; x < a[2]; x += CELL)
            for (int z = a[1]; z < a[3]; z += CELL) {
                BlockPos c = new BlockPos(x, bot.home.getY(), z);
                long key = cell(c);
                if (taken.contains(key) || bot.memory.nearCamp(c, 24))
                    continue;
                double age = bot.tick - cellSeen.getOrDefault(key, 0L);
                double score = age * (baseCells.contains(key) ? BASE_WEIGHT : 1) / (1 + Math.sqrt(c.distSqr(from)) / 256);
                if (score > bestScore) {
                    bestScore = score;
                    best = c;
                }
            }
        return best != null ? best : bot.home;
    }

    private void retrain() {
        if (!FogOfWarServerEvents.isEnabled())
            return;
        lastTrainTick = bot.tick; // one attempt per interval
        BuildingPlacement capitol = bot.getCapitol();
        if (capitol instanceof ProductionPlacement pp && capitol.isBuilt)
            bot.startProduction(pp, bot.plan.scout());
    }
}
