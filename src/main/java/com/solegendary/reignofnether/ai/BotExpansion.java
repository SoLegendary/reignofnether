package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.resources.ResourceIndex;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Drop-offs and outposts: a stockpile next to resources that are a long walk from every drop-off, the next patch
// worth taking, leaving an outpost under attack, and tearing down one with nothing left around it.
public class BotExpansion {

    private static final int PATCH_R = 12, MIN_PATCH = 40; // ponytail: rough, tune from playtests
    private static final double ORE_WEIGHT = 2, FOOD_WEIGHT = 0.5, MIXED_BONUS = 0.5; // per extra resource type
    private static final int OUTPOST_DIST = 32; // a drop-off this far from home is an outpost
    private static final int OUTPOST_RANGE = 24;
    private static final long OUTPOST_CALM_TICKS = 30 * 20; // an outpost is safe again this long after its last enemy
    private static final int NEW_DROP_OFF_DIST = 32; // resources this far from every drop-off (the capitol too) get one
    private static final int DROP_OFF_SPACING = 28; // a new drop-off never goes closer than this to another one
    private static final int LONG_WALK = 40; // or closer, when the walk there is this long (round a ravine, a lake)
    static final int DROP_OFF_WALK = 16; // a stockpile is at most this many walking blocks from its resource
    private static final int DEPLETED_RANGE = 20;
    static final int WORKERS_PER_OUTPOST = 5;

    private final BotBrain bot;
    private final Map<BlockPos, Long> unsafeUntil = new HashMap<>(); // outpost centre -> tick it's safe again
    private final Map<BlockPos, Long> depletedSince = new HashMap<>();
    private final Map<Long, Long> badPatchUntil = new HashMap<>(); // 16-block cell -> tick: no drop-off spot found there
    private static final long BAD_PATCH_TICKS = 3 * 60 * 20;
    private static final double SWITCH_PATCH = 1.3; // a new patch has to be this much better to change our plan
    private long depletedCheckTick = 0;
    private BlockPos expansionSpot = null;
    private long expansionCheckTick = -1_000_000;
    BlockPos campToClear = null; // resources we want, guarded by neutrals: the army clears them first (BotArmy)
    BlockPos dropOffFor = null; // the resource a drop-off being placed serves (BotBuilder.reachable)

    BotExpansion(BotBrain bot) {
        this.bot = bot;
    }

    void think() {
        abandonUnsafeOutposts();
        if (bot.growing || woodRunningLow()) // new outposts come in the growth phase, or as soon as wood runs out
            expand();
        removeDepletedDropOffs();
    }

    // the plan's own stockpile step: on the best wood patch near home (the richest, nearest first)
    boolean placePlannedDropOff() {
        Map<BlockPos, Double> score = new HashMap<>();
        for (ResourceName type : PATCH_TYPES)
            for (BlockPos c : patchCandidates(type, bot.home, 96))
                if (bot.memory.campUnitsNear(c, 24).isEmpty())
                    score.put(c, patchScore(richness(c), c));
        List<BlockPos> spots = new ArrayList<>(score.keySet());
        spots.sort(Comparator.comparingDouble(c -> -score.get(c)));
        for (BlockPos c : spots.subList(0, Math.min(3, spots.size())))
            if (placeDropOff(c))
                return true;
        return false;
    }

    int outposts() {
        int n = 0;
        for (BuildingPlacement b : bot.buildings)
            if (b.isBuilt && !b.isCapitol && b.getBuilding().canAcceptResources && b.centrePos.distSqr(bot.home) > OUTPOST_DIST * OUTPOST_DIST)
                n++;
        return n;
    }

    @Nullable
    BlockPos nearestDropOff(BlockPos pos) {
        BlockPos best = null;
        for (BuildingPlacement b : bot.buildings)
            if (b.isBuilt && b.getBuilding().canAcceptResources && (best == null || b.centrePos.distSqr(pos) < best.distSqr(pos)))
                best = b.centrePos;
        return best;
    }

    boolean nearOurDropOff(BlockPos p, int range) {
        for (BuildingPlacement b : bot.buildings)
            if ((b.isCapitol || b.getBuilding().canAcceptResources) && b.centrePos.distSqr(p) < (long) range * range)
                return true;
        return false;
    }

    boolean isUnsafe(BlockPos p) {
        for (BlockPos c : unsafeUntil.keySet())
            if (c.distSqr(p) < OUTPOST_RANGE * OUTPOST_RANGE)
                return true;
        return false;
    }

    // an outpost with armed enemies around is left: its workers go home and nobody is sent there until it's been
    // calm for a while. Whether to defend it is the army's call
    private void abandonUnsafeOutposts() {
        for (BuildingPlacement b : bot.buildings)
            if (!b.isCapitol && b.getBuilding().canAcceptResources && b.centrePos.distSqr(bot.home) > BotArmy.CORE_RANGE * BotArmy.CORE_RANGE &&
                    bot.memory.enemiesNear(Vec3.atCenterOf(b.centrePos), OUTPOST_RANGE).stream().anyMatch(e -> e instanceof AttackerUnit) &&
                    unsafeUntil.put(b.centrePos, bot.tick + OUTPOST_CALM_TICKS) == null)
                bot.log("outpost: {} at {} from home is under threat, workers leave", b.getBuilding().name, bot.fromHome(b.centrePos));
        unsafeUntil.values().removeIf(until -> until < bot.tick);
        if (unsafeUntil.isEmpty())
            return;
        List<LivingEntity> leave = bot.workers.stream().filter(w -> isUnsafe(w.blockPosition()) && bot.unit(w).fightingId < 0 &&
                !bot.home.equals(((Unit) w).getMoveGoal().getMoveTarget())).toList();
        if (!leave.isEmpty())
            bot.order(UnitAction.MOVE, leave, bot.home);
    }

    // ---------------------------------------------------------------- new drop-offs

    // a stockpile next to resources far from every drop-off (or a long walk round a ravine from the nearest)
    void maybeExpandDropOff(BlockPos resourcePos, ResourceName type) {
        // the opening too: a worker only gets here when nothing nearer was left, so waiting for the growth phase starves it
        if (isBad(resourcePos) || !bot.memory.isSeen(resourcePos) || (!farFromDropOffs(resourcePos) && !longWalkFromDropOff(resourcePos)))
            return; // unseen: a camp could be standing there, wait until a worker gets eyes on it
        // gatherers are already walking there, so a smaller patch than a planned expansion is worth a stockpile
        if (richness(resourcePos) >= MIN_PATCH / 3.0)
            expandTo(resourcePos);
    }

    // one drop-off going up at a time, no limit on how many: like a player, the bot keeps taking new patches
    private void expandTo(BlockPos resource) {
        if (bot.hasFinished(bot.plan.capitol()) && !bot.hasUnbuilt(bot.plan.dropOff()) && bot.canAfford(bot.plan.dropOff().cost, false))
            placeDropOff(resource);
    }

    // the next patch to take: the richest seen wood or ore away from every drop-off and enemy base, nearer is
    // better. The main army holds it between waves (BotArmy) and the outpost follows. Rechecked every 10 s
    @Nullable
    BlockPos nextExpansion() {
        if (expansionSpot != null && isBad(expansionSpot))
            expansionSpot = null;
        if (bot.tick - expansionCheckTick < 10 * 20)
            return expansionSpot;
        expansionCheckTick = bot.tick;
        BlockPos best = null;
        double bestScore = 0, currentScore = 0;
        for (ResourceName type : PATCH_TYPES)
            for (BlockPos c : patchCandidates(type, bot.home, 96 + 24 * outposts())) {
                // neutrals guarding it are fine if the army can clear them first (BotArmy holds and clears it)
                if (!bot.army.canClear(c))
                    continue;
                double rich = richness(c);
                double score = patchScore(rich, c);
                if (expansionSpot != null && c.distSqr(expansionSpot) < 16 * 16)
                    currentScore = Math.max(currentScore, score);
                if (rich >= MIN_PATCH && score > bestScore) {
                    bestScore = score;
                    best = c;
                }
            }
        if (currentScore > 0 && bestScore < currentScore * SWITCH_PATCH)
            best = expansionSpot; // keep the plan unless something clearly better turned up
        if (best != null && !best.equals(expansionSpot))
            bot.log("expand: next patch {} from home", bot.fromHome(best));
        expansionSpot = best;
        if (best != null && bot.memory.nearCamp(best, 24))
            campToClear = best;
        return best;
    }

    // the outposts fill up with workers before the next one goes down, so the bot grows instead of spreading thin
    // ponytail: worker count stands in for "current patches are saturated"
    // farms eat wood, so wood running out (or soon to) near our drop-offs means expand now, whatever the worker
    // count: the next stockpile is up before the last trees fall, not minutes after
    boolean wantsExpansion() {
        // out of wood near every drop-off: always, or the bot locks itself with nothing to build anything with
        return bot.tick - bot.economy.noWoodTick < 60 * 20 || woodRunningLow() ||
                (bot.production.hasMilitaryProducer() && bot.workers.size() >= 10 + WORKERS_PER_OUTPOST * outposts());
    }

    private static final int LOW_WOOD_LEFT = 150; // wood blocks left around all our drop-offs together
    private boolean woodLow = false;
    private long woodCheckTick = -1_000_000;

    private boolean woodRunningLow() {
        if (bot.tick - woodCheckTick >= 10 * 20) {
            woodCheckTick = bot.tick;
            int left = 0;
            for (BuildingPlacement b : bot.buildings)
                if (b.isBuilt && (b.isCapitol || b.getBuilding().canAcceptResources) && bot.level.hasChunkAt(b.centrePos))
                    left += ResourceIndex.get(bot.level).count(bot.level, b.centrePos, NEW_DROP_OFF_DIST, ResourceName.WOOD);
            woodLow = left < LOW_WOOD_LEFT;
        }
        return woodLow;
    }

    private void expand() {
        if (!wantsExpansion())
            return;
        BlockPos spot = nextExpansion();
        if (spot != null && (bot.army.isHolding(spot) || bot.memory.enemiesNear(Vec3.atCenterOf(spot), 24).isEmpty()))
            expandTo(spot);
    }

    private static final long SEEN_TICKS = 5 * 60 * 20; // a patch our units saw this recently counts as known

    // up to 12 seen resources of a type nearest `from` that could take a drop-off, at least 16 blocks apart
    private List<BlockPos> patchCandidates(ResourceName type, BlockPos from, int range) {
        List<BlockPos> found = new ArrayList<>();
        ResourceIndex.get(bot.level).findClosest(bot.level, from, range, type, bp -> {
            if (!isBad(bp) && bot.scout.seenWithin(bp, SEEN_TICKS) && farFromDropOffs(bp) && !nearEnemyBase(bp) && !nearAllyBuilding(bp) &&
                    !BuildingUtils.isPosInsideAnyBuilding(false, bp) && found.stream().allMatch(f -> f.distSqr(bp) >= 16 * 16))
                found.add(bp);
            return found.size() >= 12; // stops the search
        });
        return found;
    }

    // richer is better, nearer home is better, further from our allies is better: their side of the map is theirs
    private double patchScore(double richness, BlockPos c) {
        double fromAlly = Math.sqrt(nearestAllyDistSqr(c));
        return richness / (1 + Math.sqrt(c.distSqr(bot.home)) / 32) * Math.min(1, Math.max(0.2, fromAlly / ALLY_FAR));
    }

    private static final int ALLY_FAR = 96; // a patch this far from every ally building gets no penalty
    static final int ALLY_DROP_OFF_RANGE = 32; // resources this near an ally's drop-off are left to them

    private double nearestAllyDistSqr(BlockPos p) {
        double best = Double.MAX_VALUE;
        for (BuildingPlacement b : BuildingServerEvents.getBuildings())
            if (!b.isDestroyedServerside && !b.ownerName.equals(bot.name) && AlliancesServerEvents.isAllied(bot.name, b.ownerName))
                best = Math.min(best, b.centrePos.distSqr(p));
        return best;
    }

    // our allies' drop-offs (capitols and stockpiles): what's around them is theirs to gather
    List<BlockPos> allyDropOffs() {
        List<BlockPos> list = new ArrayList<>();
        for (BuildingPlacement b : BuildingServerEvents.getBuildings())
            if ((b.isCapitol || b.getBuilding().canAcceptResources) && !b.isDestroyedServerside &&
                    !b.ownerName.equals(bot.name) && AlliancesServerEvents.isAllied(bot.name, b.ownerName))
                list.add(b.centrePos);
        return list;
    }

    private int patchSize(BlockPos p, ResourceName type) {
        return ResourceIndex.get(bot.level).count(bot.level, p, PATCH_R, type);
    }

    private static final List<ResourceName> PATCH_TYPES = List.of(ResourceName.WOOD, ResourceName.ORE, ResourceName.FOOD);

    // what one stockpile there would serve: wood, ore (rarer, counts double) and food, with a bonus per extra type,
    // so a spot with wood and ore beats a bigger forest
    private double richness(BlockPos p) {
        return richness(patchSize(p, ResourceName.WOOD), patchSize(p, ResourceName.ORE), patchSize(p, ResourceName.FOOD));
    }

    static double richness(int wood, int ore, int food) {
        int types = (wood > 0 ? 1 : 0) + (ore > 0 ? 1 : 0) + (food > 0 ? 1 : 0);
        return (wood + ORE_WEIGHT * ore + FOOD_WEIGHT * food) * (1 + MIXED_BONUS * Math.max(0, types - 1));
    }

    // an ally's base or outpost: their ground, not ours to take
    private boolean nearAllyBuilding(BlockPos p) {
        for (BuildingPlacement b : BuildingServerEvents.getBuildings())
            if (!b.ownerName.equals(bot.name) && AlliancesServerEvents.isAllied(bot.name, b.ownerName) && b.centrePos.distSqr(p) < 32 * 32)
                return true;
        return false;
    }

    private boolean nearEnemyBase(BlockPos p) {
        return bot.memory.enemyBuildings.values().stream().anyMatch(b -> b.pos().distSqr(p) < 48 * 48);
    }

    // unbuilt ones count too (at the start the capitol is still going up), and allies' ones
    private boolean farFromDropOffs(BlockPos p) {
        return !nearDropOff(p, NEW_DROP_OFF_DIST);
    }

    // ours or an ally's: a stockpile next to an ally's would just eat their resources
    private boolean nearDropOff(BlockPos p, int range) {
        for (BuildingPlacement b : BuildingServerEvents.getBuildings())
            if ((b.isCapitol || b.getBuilding().canAcceptResources) && !b.isDestroyedServerside &&
                    b.centrePos.distSqr(p) < (long) range * range &&
                    (b.ownerName.equals(bot.name) || AlliancesServerEvents.isAllied(bot.name, b.ownerName)))
                return true;
        return false;
    }

    // a spot a drop-off may not take: next to another drop-off, or under fog where a camp could stand
    boolean badDropOffSpot(BlockPos p) {
        return nearDropOff(p, DROP_OFF_SPACING) || !bot.memory.isSeen(p);
    }

    // close as the crow flies but a long walk (round a ravine, a lake): a stockpile there saves the trips.
    // Measured by the RTS pathfinder from a worker near the drop-off; until it answers, the next worker asks again
    private boolean longWalkFromDropOff(BlockPos p) {
        BlockPos from = nearestDropOff(p);
        if (from == null || from.distSqr(p) < 24 * 24)
            return false;
        LivingEntity w = bot.workers.stream().filter(e -> e.blockPosition().distSqr(from) < 12 * 12)
                .min(Comparator.comparingDouble(e -> e.blockPosition().distSqr(from))).orElse(null);
        if (!(w instanceof Mob mob))
            return false;
        BotWalk.Walk walk = bot.walk.get(mob, p, bot.tick);
        return walk != null && walk.reached() && walk.length() >= LONG_WALK;
    }

    // a stockpile goes a short walk from its resource. Neutrals guarding it are cleared by the army first
    private boolean placeDropOff(BlockPos resource) {
        if (isBad(resource) || !bot.hasActions(1))
            return false; // out of actions says nothing about the spot
        if (bot.memory.nearCamp(resource, 24)) {
            if (!resource.equals(campToClear))
                bot.log("expand: resource {} from home is guarded by a neutral camp, asking the army to clear it", bot.fromHome(resource));
            campToClear = resource;
            return false;
        }
        if (nearEnemyBase(resource) || isUnsafe(resource))
            return false;
        dropOffFor = resource;
        try {
            // inside a forest every spot is trees: try the patch's edge on the side facing our base first, where a
            // player would put it, then around the resource itself
            if (bot.builder.place(bot.plan.dropOff(), edgeTowardsBase(resource), 0, 10) != null ||
                    bot.builder.place(bot.plan.dropOff(), resource, 3, 14) != null)
                return true;
            if (!bot.placer.waiting) {
                badPatchUntil.put(cell(resource), bot.tick + BAD_PATCH_TICKS);
                bot.log("expand: no spot for a drop-off at {} from home, trying other patches for a while", bot.fromHome(resource));
            }
            return false;
        } finally {
            dropOffFor = null;
        }
    }

    // 6 blocks from the resource towards our nearest drop-off (home at first)
    private BlockPos edgeTowardsBase(BlockPos resource) {
        BlockPos to = nearestDropOff(resource);
        return BotMath.towards(resource, Vec3.atCenterOf(to != null ? to : bot.home), 6, resource);
    }

    private boolean isBad(BlockPos p) {
        Long until = badPatchUntil.get(cell(p));
        return until != null && bot.tick < until;
    }

    private static long cell(BlockPos p) {
        return BlockPos.asLong(p.getX() >> 4, 0, p.getZ() >> 4);
    }

    // a drop-off with nothing left to gather around it for 2 minutes is torn down, like a player would.
    // Finished buildings refund nothing, so nothing is lost
    private void removeDepletedDropOffs() {
        if (bot.tick - depletedCheckTick < 100)
            return;
        depletedCheckTick = bot.tick;
        for (BuildingPlacement b : new ArrayList<>(bot.buildings)) {
            if (!b.isBuilt || b.getBuilding() != bot.plan.dropOff())
                continue;
            // farms drop off at the nearest stockpile, and the resource index is empty for unloaded chunks: keep it
            boolean feedsFarm = bot.buildings.stream().anyMatch(f -> f.getBuilding() == bot.plan.farm() &&
                    f.centrePos.distSqr(b.centrePos) < DEPLETED_RANGE * DEPLETED_RANGE);
            int left = 0;
            if (!feedsFarm && bot.level.hasChunkAt(b.centrePos))
                for (ResourceName type : List.of(ResourceName.WOOD, ResourceName.ORE, ResourceName.FOOD))
                    left += ResourceIndex.get(bot.level).count(bot.level, b.centrePos, DEPLETED_RANGE, type);
            if (feedsFarm || !bot.level.hasChunkAt(b.centrePos) || left > 0) {
                depletedSince.remove(b.centrePos);
                continue;
            }
            long since = depletedSince.computeIfAbsent(b.centrePos, k -> bot.tick);
            if (bot.tick - since >= 2 * 60 * 20) {
                bot.log("expand: {} at {} from home has nothing left around it, removing it", b.getBuilding().name, bot.fromHome(b.centrePos));
                depletedSince.remove(b.centrePos);
                BuildingServerEvents.cancelBuilding(b, bot.name);
            }
        }
    }
}
