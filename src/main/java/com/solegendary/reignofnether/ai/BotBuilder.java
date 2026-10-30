package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Buildings: the plan's opening, then the matchup's tech, houses ahead of the population, more production when
// it's all busy, placing on good ground, and cancelling foundations that stopped going up.
public class BotBuilder {

    // buildings at home go on the nearest valid, walkable spot up to this far out, else next to our other buildings
    private static final int FAR = 56;
    private static final int HOME_MIN_R = 4; // as near home as the gaps allow
    private static final int WALK_RANGE = 90; // the RTS pathfinder searches up to 96 blocks out
    private static final int MAX_SAVING_TICKS = 45 * 20;
    private static final int STUCK_TICKS = 90 * 20;
    private static final int MAX_PRODUCERS = 10;

    private final BotBrain bot;
    private final Map<BotPlan.Step, Integer> stepFailures = new HashMap<>();
    private final Map<BotPlan.Step, Long> stepSkipUntil = new HashMap<>();
    private final Map<BotPlan.Step, Long> savingSince = new HashMap<>();
    private record Search(Building building, long anchorCell, int minR) {} // a wider ring round the same anchor is its own search
    private final Map<Search, Long> placeRetryTick = new HashMap<>(); // back off after a failed spot search
    private final Map<BlockPos, Float> lastBuildProgress = new HashMap<>();
    private final Map<BlockPos, Long> buildProgressSince = new HashMap<>();

    BotBuilder(BotBrain bot) {
        this.bot = bot;
    }

    void think() {
        placeRetryTick.values().removeIf(until -> until < bot.tick);
        if (bot.growing)
            keepProducers();
        followBuildOrder();
        cancelStuckFoundations();
    }

    // ---------------------------------------------------------------- build order

    // the opening, then the matchup's tech. Savings go to the first step we can't pay for and nothing after it
    // spends them (production never waits for them: BotBrain.canAfford). Steps whose needs aren't built are passed
    private void followBuildOrder() {
        bot.reserve = null;
        List<BotPlan.Step> steps = new ArrayList<>(bot.plan.opening());
        if (bot.growing)
            steps.addAll(bot.matchup.tech());
        for (BotPlan.Step step : steps) {
            if (isDone(step) || bot.tick < stepSkipUntil.getOrDefault(step, 0L) || !step.needs().stream().allMatch(bot::hasFinished) ||
                    (step.building() == Buildings.BEACON && !bot.level.getGameRules().getBoolean(GameRuleRegistrar.ALLOW_BEACONS)))
                continue;
            ResourceCost cost = step.type() == BotPlan.StepType.BUILD ? step.building().cost : step.item().getCost(false, bot.name);
            if (!bot.canAfford(cost, false)) {
                // a saving that hasn't filled in 45 s (its resource dried up) is dropped for a minute
                long since = savingSince.computeIfAbsent(step, k -> bot.tick);
                if (bot.tick - since > MAX_SAVING_TICKS) {
                    savingSince.remove(step);
                    stepSkipUntil.put(step, bot.tick + 60 * 20);
                    bot.log("build order: gave up saving for {} for a minute", describe(step));
                    continue;
                }
                bot.reserve = cost;
                return;
            }
            savingSince.remove(step);
            boolean ok = switch (step.type()) {
                case BUILD -> step.building() == bot.plan.dropOff() ? bot.expansion.placePlannedDropOff() : placeAtHome(step.building()) != null;
                case RESEARCH -> {
                    ProductionPlacement at = bot.findProducer(step.building(), step.item());
                    yield at != null && bot.startProduction(at, step.item());
                }
            };
            if (ok) {
                stepFailures.remove(step);
                continue; // on to the next step: a fast bot starts several things at once
            }
            // a step that keeps failing (no space, busy building) is skipped for a minute; later steps go meanwhile
            if (stepFailures.merge(step, 1, Integer::sum) >= 3) {
                stepFailures.remove(step);
                stepSkipUntil.put(step, bot.tick + 60 * 20);
                bot.log("build order: {} failed 3 times ({}), skipped for a minute", describe(step), bot.placer.stop);
            }
        }
    }

    private boolean isDone(BotPlan.Step step) {
        return switch (step.type()) {
            case BUILD -> bot.countOwned(step.building()) >= step.count();
            case RESEARCH -> ResearchServerEvents.playerHasResearch(bot.name, step.item()) || step.item().itemIsBeingProduced(false, bot.name);
        };
    }

    private static String describe(BotPlan.Step step) {
        return step.item() != null ? String.valueOf(step.item()) : step.building().name;
    }

    // ---------------------------------------------------------------- houses and producers

    // the plan comes first: during the opening its own house steps are the houses, and an extra one only goes up
    // when we're actually at the population cap. After it, houses stay ahead of what's training, without
    // spending the build order's savings unless we're capped
    void buildSupply() {
        int supply = bot.getSupply();
        if (supply >= UnitServerEvents.maxPopulation || !bot.hasFinished(bot.plan.capitol()))
            return;
        int pending = 0;
        for (BuildingPlacement b : bot.buildings)
            if (!b.isBuilt && b.getBuilding() == bot.plan.supply())
                pending++;
        boolean capped = bot.getPop() >= supply - 1;
        if (bot.growing) {
            // only what's actually training counts: every tech building is a ProductionPlacement too, and counting
            // those kept 30+ pop of houses spare (150 supply for 60 pop)
            int training = 0;
            for (BuildingPlacement b : bot.buildings)
                if (b instanceof ProductionPlacement pp && b.isBuilt && !pp.productionQueue.isEmpty())
                    training++;
            if (bot.getPop() + 4 + 2 * training < supply || pending >= (supply >= 60 ? 2 : 1))
                return;
        } else if (!capped || pending > 0) {
            return;
        }
        if (bot.canAfford(bot.plan.supply().cost, !capped))
            placeAtHome(bot.plan.supply());
    }

    boolean openingDone() {
        for (BotPlan.Step step : bot.plan.opening())
            if (!isDone(step))
                return false;
        return true;
    }

    // another barracks when the ones we have are all busy and resources pile up (the guide's second barracks
    // comes from the opening), up to 1 per 3 workers: as many as the income keeps busy
    private void keepProducers() {
        Building producer = bot.production.mainProducer();
        if (producer == null || !bot.hasFinished(producer) || bot.defence.isDefending() || bot.hasUnbuilt(producer))
            return;
        if (bot.countOwned(producer) >= Math.min(MAX_PRODUCERS, 1 + bot.workers.size() / 3))
            return;
        for (BuildingPlacement b : bot.buildings)
            if (b instanceof ProductionPlacement pp && b.isBuilt && b.getBuilding().isTypeOf(producer) && pp.productionQueue.size() < BotProduction.MAX_QUEUE)
                return; // one is still free
        if (bot.resources.food >= 300 && bot.resources.wood >= 150)
            placeAtHome(producer);
    }

    // ---------------------------------------------------------------- placing

    @Nullable
    BuildingPlacement placeAtHome(Building building) {
        if (building == bot.plan.farm())
            return placeFarm();
        // farms, houses and barracks are fine to lose; tech built once (towers, library, forge...) goes at the back
        // of the base, on the far side from the enemy. Nothing fits there: anywhere at home
        // houses go a bit in front of the capitol instead, as a wall the enemy hits first
        Vec3 enemy = bot.army.enemyDirection();
        boolean front = building == bot.plan.supply();
        if (enemy != null && (front || isSpecial(building))) {
            BlockPos anchor = BlockPos.containing(Vec3.atCenterOf(bot.home).add(enemy.scale(front ? IN_FRONT : -BEHIND)));
            // houses fill in round their spot in front: leaning away from the enemy would pull them behind the capitol
            nearestToAnchor = front;
            BuildingPlacement placed;
            try {
                placed = place(building, anchor, 0, FAR);
            } finally {
                nearestToAnchor = false;
            }
            if (placed != null || bot.placer.waiting)
                return placed;
        }
        return place(building, bot.home, HOME_MIN_R, FAR);
    }

    private static final int BEHIND = 16, IN_FRONT = 14;
    private boolean nearestToAnchor = false;

    private boolean isSpecial(Building building) {
        BotPlan plan = bot.plan;
        return building != plan.supply() && building != plan.dropOff() && building != plan.capitol() &&
                building != bot.production.mainProducer();
    }

    private static final int FARM_RANGE = 16, FARM_FAR = 32; // farms hug their drop-off

    // a farmer carries its harvest to the nearest drop-off: farms go right by the capitol, else by a stockpile,
    // never out on their own
    @Nullable
    private BuildingPlacement placeFarm() {
        List<BuildingPlacement> dropOffs = bot.buildings.stream().filter(b -> b.isBuilt && (b.isCapitol || b.getBuilding().canAcceptResources))
                .sorted(Comparator.comparing((BuildingPlacement b) -> !b.isCapitol).thenComparingDouble(b -> b.centrePos.distSqr(bot.home)))
                .toList();
        for (BuildingPlacement d : dropOffs) {
            BuildingPlacement farm = place(bot.plan.farm(), d.centrePos, 6, FARM_RANGE);
            if (farm != null || bot.placer.waiting)
                return farm;
        }
        // all full round their drop-offs: a little further from the capitol beats no farm at all
        return dropOffs.isEmpty() || !dropOffs.get(0).isCapitol ? null : place(bot.plan.farm(), dropOffs.get(0).centrePos, FARM_RANGE, FARM_FAR);
    }

    // places a building near an anchor with workers assigned; null if no spot or it was rejected
    @Nullable
    BuildingPlacement place(Building building, BlockPos anchor, int minR, int maxR) {
        Search search = new Search(building, BlockPos.asLong(anchor.getX() >> 3, 0, anchor.getZ() >> 3), minR);
        if (bot.tick < placeRetryTick.getOrDefault(search, 0L) || !bot.hasActions(1) ||
                (bot.heroReserve != null && !bot.canAfford(building.cost, false))) // a dead hero's revive comes first
            return null;
        BotPlacer placer = bot.placer;
        placer.enemyDir = nearestToAnchor ? null : bot.army.enemyDirection();
        placer.danger = bot.memory.dangerSpots(bot.tick);
        // farms go right around the capitol and stockpiles: other buildings leave a farm's width free there
        placer.dropOffMargin = building == bot.plan.farm() || building == bot.plan.dropOff() ? 0 :
                Math.max(BotPlacer.getSize(bot.level, bot.plan.farm()).getX(), BotPlacer.getSize(bot.level, bot.plan.farm()).getZ());
        BlockPos origin = placer.findSpot(bot.level, building, bot.name, anchor, minR, maxR);
        // a crowded or hilly home: try next to our other buildings in the base, nearest home first (not farms: they
        // stay by a drop-off; not outposts: a barracks out there is a long walk and the first thing the enemy hits)
        if (origin == null && anchor.equals(bot.home) && !placer.waiting && building != bot.plan.farm()) {
            List<BuildingPlacement> inBase = bot.buildings.stream().filter(b -> b.isBuilt && !b.isCapitol &&
                            b.centrePos.distSqr(bot.home) < BotArmy.CORE_RANGE * BotArmy.CORE_RANGE)
                    .sorted(Comparator.comparingDouble(b -> b.centrePos.distSqr(bot.home))).limit(4).toList();
            for (BuildingPlacement b : inBase)
                if (origin == null && !placer.waiting)
                    origin = placer.findSpot(bot.level, building, bot.name, b.centrePos, 6, 20);
        }
        if (origin == null) {
            if (!placer.waiting)
                bot.log("place: no spot for {} around {} from home (r {}-{}), turned down {} invalid {}, {} unwalkable, {} not flat, " +
                                "{} no gap, {} near camp, {} failed before; {}", building.name, bot.fromHome(anchor), minR, maxR,
                        placer.invalid, placer.reasons, placer.unwalkable, placer.notFlat + " (" + placer.trees + " on trees)", placer.noGap,
                        placer.camp, placer.failedBefore, placer.stop);
            placeRetryTick.put(search, bot.tick + (placer.waiting ? 20 : 10 * 20)); // waiting: a path is on its way
            return null;
        }
        if (!bot.spendAction())
            return null;
        // the base's builders; none free: the foundation waits for the next free one (assignBuilders)
        int count = building == bot.plan.farm() ? 1 : building == bot.plan.capitol() ? 4 : building == bot.plan.dropOff() ? 2 : 1; // more join later (assignBuilders)
        List<LivingEntity> builders = bot.economy.buildersFor(building, Vec3.atCenterOf(origin), count);
        for (LivingEntity b : builders)
            bot.unit(b).lastOrdered = bot.tick;
        int[] ids = builders.stream().mapToInt(LivingEntity::getId).toArray();
        BuildingPlacement placed = BuildingServerEvents.placeBuilding(building, origin, Rotation.NONE, bot.name, ids, false, false, false, false);
        if (placed == null) {
            bot.log("place: {} refused at {} from home", building.name, bot.fromHome(origin));
            placer.markFailed(origin, bot.level.getGameTime());
            return null;
        }
        bot.log("place: {} at {} from home", building.name, bot.fromHome(placed.centrePos));
        bot.buildings.add(placed); // so the rest of this think counts it
        bot.refreshResources();
        return placed;
    }

    // can our workers walk there from the base, without a long detour (round a ravine, lava, a cliff)? Measured from
    // a worker standing by our nearest drop-off, so a spot across a river from the base fails even when some worker
    // happens to be over there. A drop-off must also be a short walk from its resource. Asked of the RTS
    // pathfinder, which answers later: until then the placer waits (BotPlacer.waiting) and retries shortly
    boolean reachable(BlockPos p) {
        BlockPos base = baseNear(p);
        LivingEntity w = bot.workers.stream().min(Comparator.comparingDouble(e -> e.blockPosition().distSqr(base))).orElse(null);
        if (!(w instanceof Mob mob))
            return true; // no workers left: nothing to walk with
        if (w.blockPosition().distSqr(p) > WALK_RANGE * WALK_RANGE)
            return false;
        BotWalk.Walk fromBase = bot.walk.get(mob, w.blockPosition(), p, bot.tick, base);
        BlockPos resource = bot.expansion.dropOffFor;
        BotWalk.Walk toResource = resource == null ? null : bot.walk.get(mob, p, resource, bot.tick);
        if (fromBase == null || (resource != null && toResource == null)) {
            bot.placer.waiting = true;
            return false;
        }
        if (!fromBase.reached() || isDetour(fromBase.length(), fromBase.straight()))
            return false;
        return resource == null || (toResource.reached() && toResource.length() <= BotExpansion.DROP_OFF_WALK);
    }

    // the built drop-off (capitol or stockpile) nearest p, else p; the walk is measured from the worker nearest it
    private BlockPos baseNear(BlockPos p) {
        return bot.buildings.stream().filter(b -> b.isBuilt && (b.isCapitol || b.getBuilding().canAcceptResources))
                .map(b -> b.centrePos).min(Comparator.comparingDouble(c -> c.distSqr(p))).orElse(p);
    }

    static boolean isDetour(double walked, double straight) {
        return walked > straight * 1.6 + 12;
    }

    // foundations that stop going up get cancelled and their spot avoided from then on
    private void cancelStuckFoundations() {
        Set<BlockPos> unbuilt = new HashSet<>();
        for (BuildingPlacement b : new ArrayList<>(bot.buildings)) {
            if (b.isBuilt || b.isCapitol)
                continue;
            unbuilt.add(b.originPos);
            float progress = b.getBlocksPlacedPercent();
            Float last = lastBuildProgress.get(b.originPos);
            if (last == null || progress > last) {
                lastBuildProgress.put(b.originPos, progress);
                buildProgressSince.put(b.originPos, bot.tick);
            } else if (bot.tick - buildProgressSince.getOrDefault(b.originPos, bot.tick) > STUCK_TICKS) {
                bot.log("watchdog: cancelling stuck {} at {} from home, {}% built", b.getBuilding().name, bot.fromHome(b.centrePos), (int) (progress * 100));
                bot.placer.markFailed(b.originPos, bot.level.getGameTime());
                BuildingServerEvents.cancelBuilding(b, bot.name);
                buildProgressSince.put(b.originPos, bot.tick);
            }
        }
        lastBuildProgress.keySet().retainAll(unbuilt);
        buildProgressSince.keySet().retainAll(unbuilt);
    }
}
