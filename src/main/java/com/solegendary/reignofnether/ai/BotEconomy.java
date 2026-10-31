package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.resources.ResourceIndex;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

// Workers: how many to train, who gathers what, hunting, farms, builders, and unsticking the stuck ones.
public class BotEconomy {

    static final int CAMP_AVOID = 20; // workers and buildings keep this far from neutral camps and enemies
    static final int PLENTY = 1000; // a stockpile past this gets no new farms, and idle workers go elsewhere
    static final int FLOAT = 400; // food or a stockpile floating past this is wasted: send gatherers where we're short
    static final int FARM_WORKERS = 1; // one farmer already harvests a farm as fast as it regrows
    private static final int POP_PER_WORKER = 5; // at 150 max pop: 30 workers
    private static final int REORDER_TICKS = 100;
    private static final int REBALANCE_TICKS = 3 * 20;
    private static final int STUCK_TICKS = 30 * 20;
    private static final int FORAGE_RANGE = 24, GATHER_RANGE = 48, GATHER_FAR_RANGE = 96;
    private static final int HUNT_RANGE = 48;
    private static final int HUNT_GIVE_UP_TICKS = 30 * 20;

    private final BotBrain bot;
    private final Map<BuildingPlacement, Integer> farmersSent = new HashMap<>(); // this think's farm orders land next tick
    private final Set<Integer> unreachablePrey = new HashSet<>(); // animals a hunter chased too long
    private long noGameUntil = 0;
    private long lastRebalance = 0;
    private long lastNoResourceLog = -1_000_000;
    long noWoodTick = -1_000_000; // last time no wood was left near our drop-offs (BotExpansion)

    BotEconomy(BotBrain bot) {
        this.bot = bot;
    }

    void think() {
        trimFarmers();
        hunt();
        assignIdleWorkers();
        rebalanceGatherers();
        assignBuilders();
        staffFarms();
        unstickWorkers();
        collectDrops();
        farmersSent.clear();
        oreSent = 0;
    }

    // ---------------------------------------------------------------- dropped resources

    private static final int DROP_RANGE = 48, DROP_EVERY = 5 * 20, DROP_WALK = 32;
    private long lastDropCheck = 0;
    private final Set<Integer> dropsSentFor = new HashSet<>(); // drops a worker was already sent to

    // resources a dead unit dropped near our base: the nearest worker with room walks over them (workers pick
    // up what they step on). Only where we can see there's no enemy, and one try per drop: one the worker
    // couldn't reach would otherwise pull a worker off its job and cost an action every 5 s
    private void collectDrops() {
        if (bot.tick - lastDropCheck < DROP_EVERY)
            return;
        lastDropCheck = bot.tick;
        List<ItemEntity> drops = bot.level.getEntitiesOfClass(ItemEntity.class, new AABB(bot.home).inflate(DROP_RANGE),
                d -> d.isAlive() && ResourceSources.getFromItem(d.getItem().getItem()) != null);
        dropsSentFor.retainAll(drops.stream().map(ItemEntity::getId).toList());
        for (ItemEntity drop : drops) {
            if (dropsSentFor.contains(drop.getId()) || !bot.memory.isSeen(drop.blockPosition()) ||
                    !bot.memory.enemiesNear(drop.position(), 12).isEmpty())
                continue;
            LivingEntity w = pickWorkers(drop.position(), bot.workers.size()).stream()
                    .filter(e -> !Unit.atMaxResources((Unit) e) && e.distanceToSqr(drop) < DROP_WALK * DROP_WALK)
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(drop))).orElse(null);
            if (w != null && bot.order(UnitAction.MOVE, List.of(w), drop.blockPosition()))
                dropsSentFor.add(drop.getId());
        }
    }

    // ---------------------------------------------------------------- training

    void trainWorkers() {
        BuildingPlacement capitol = bot.getCapitol();
        if (!(capitol instanceof ProductionPlacement pp) || !capitol.isBuilt)
            return;
        int queued = 0;
        for (var active : pp.productionQueue)
            if (active.item == bot.plan.worker())
                queued++;
        if (bot.workers.size() + queued < workerTarget() && queued < BotProduction.MAX_QUEUE && workersAllowed())
            bot.startProduction(pp, bot.plan.worker());
    }

    // the plan's target for this minute (more per outpost), and never more than the army needs: past the
    // opening, the plan's workers per pop of army. The economy is there to feed the army
    int workerTarget() {
        int plan = Math.min(UnitServerEvents.maxPopulation / POP_PER_WORKER, planTarget(bot.plan.workers(), bot.minutesPlayed()) + BotExpansion.WORKERS_PER_OUTPOST * bot.expansion.outposts());
        return Math.min(plan, armyCap(openingWorkers(), BotMath.popOf(bot.fighters), bot.plan.workersPerArmyPop()));
    }

    static int armyCap(int opening, int armyPop, double perPop) {
        return Math.max(opening, (int) Math.round(armyPop * perPop));
    }

    private int openingWorkers() {
        return bot.plan.workers().isEmpty() ? 15 : bot.plan.workers().get(0).count();
    }

    static int planTarget(List<BotPlan.WorkerTarget> targets, int minute) {
        int count = targets.isEmpty() ? 15 : targets.get(0).count();
        for (BotPlan.WorkerTarget t : targets)
            if (minute >= t.minute())
                count = t.count();
        return count;
    }

    // none while the base is under attack: food goes to troops
    private boolean workersAllowed() {
        return !bot.defence.isDefending();
    }

    // ---------------------------------------------------------------- picking workers

    // villagers keep the first profession they earn for good, with a gather or build bonus for it
    static VillagerUnitProfession professionOf(LivingEntity e) {
        return e instanceof VillagerUnit v ? v.getUnitProfession() : VillagerUnitProfession.NONE;
    }

    // nearest workers that aren't building, leaving farmers and hunters alone: masons first (they build faster),
    // then idle ones, then ones without a gathering profession so lumberjacks and miners keep their bonus
    List<LivingEntity> pickWorkers(Vec3 near, int count) {
        List<LivingEntity> candidates = new ArrayList<>();
        for (LivingEntity e : bot.workers) {
            WorkerUnit w = (WorkerUnit) e;
            // orders are queued, so one ordered earlier this think still looks idle: leave it to that order
            if (w.getBuildRepairGoal().getBuildingTarget() == null && farmOf(w) == null && !bot.unit(e).hunter && !bot.unit(e).called &&
                    !bot.unit(e).builder &&
                    bot.unit(e).lastOrdered != bot.tick)
                candidates.add(e);
        }
        candidates.sort(Comparator
                .comparing((LivingEntity e) -> professionOf(e) != VillagerUnitProfession.MASON)
                .thenComparing(e -> !WorkerUnit.isIdle((WorkerUnit) e))
                .thenComparing(e -> professionOf(e) == VillagerUnitProfession.LUMBERJACK || professionOf(e) == VillagerUnitProfession.MINER)
                .thenComparingDouble(e -> e.position().distanceToSqr(near)));
        return candidates.subList(0, Math.min(count, candidates.size()));
    }

    // ---------------------------------------------------------------- builders

    // our workers headed to or working on b; RoN's getBuilders() only counts ones that already arrived
    private List<LivingEntity> buildersOf(BuildingPlacement b) {
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : bot.workers)
            if (((WorkerUnit) e).getBuildRepairGoal().getBuildingTarget() == b)
                list.add(e);
        return list;
    }

    // the base's builders, a job like hunter: 1 at the start, 2 from 10 workers, 3 from 16, 4 from 22, masons first. Only they
    // build (and repair); between buildings they gather like anyone. Farms (their builder is their farmer) and the
    // capitol going up (nothing can be dropped off before it) take anyone
    int builderCap() {
        int n = bot.workers.size();
        return n < 10 ? 1 : n < 16 ? 2 : n < 22 ? 3 : 4;
    }

    private boolean countsAsBuilding(@Nullable BuildingPlacement b) {
        return b != null && b.getBuilding() != bot.plan.farm() && b.getBuilding() != bot.plan.dropOff() && !b.isCapitol;
    }

    private void keepBuilders() {
        List<LivingEntity> have = new ArrayList<>(bot.workers.stream().filter(e -> bot.unit(e).builder).toList());
        int cap = builderCap();
        while (have.size() > cap) {
            LivingEntity drop = have.stream().filter(e -> ((WorkerUnit) e).getBuildRepairGoal().getBuildingTarget() == null)
                    .findFirst().orElse(null);
            if (drop == null)
                break; // all busy: one steps down once it's done
            bot.unit(drop).builder = false;
            have.remove(drop);
        }
        if (have.size() >= cap)
            return;
        // building teaches the mason job to a worker without one: never more than MAX_MASONS of them, masons and
        // builders on their way to it counted. Past that, builders come from workers that already have another job
        int[] masons = {(int) bot.workers.stream().filter(e -> professionOf(e) == VillagerUnitProfession.MASON ||
                (bot.unit(e).builder && professionOf(e) == VillagerUnitProfession.NONE)).count()};
        Vec3 home = Vec3.atCenterOf(bot.home);
        // a mason farming while there was nothing to build is taken off its farm: building is its job
        List<LivingEntity> candidates = bot.workers.stream().filter(e -> !bot.unit(e).builder && !bot.unit(e).hunter && !bot.unit(e).called &&
                        (farmOf((WorkerUnit) e) == null || professionOf(e) == VillagerUnitProfession.MASON))
                .sorted(Comparator.comparing((LivingEntity e) -> professionOf(e) != VillagerUnitProfession.MASON)
                        .thenComparing(e -> professionOf(e) != VillagerUnitProfession.NONE)
                        .thenComparingDouble(e -> e.position().distanceToSqr(home))).toList();
        int need = cap - have.size();
        for (LivingEntity e : candidates) {
            if (need == 0)
                break;
            if (professionOf(e) == VillagerUnitProfession.NONE) {
                if (masons[0] >= MAX_MASONS)
                    continue;
                masons[0]++;
            }
            bot.unit(e).builder = true;
            need--;
        }
    }

    private static final int MAX_MASONS = 2;

    // free builders nearest `near`
    List<LivingEntity> pickBuilders(Vec3 near, int count) {
        keepBuilders();
        return bot.workers.stream().filter(e -> bot.unit(e).builder && bot.unit(e).lastOrdered != bot.tick &&
                        ((WorkerUnit) e).getBuildRepairGoal().getBuildingTarget() == null)
                .sorted(Comparator.comparingDouble(e -> e.position().distanceToSqr(near))).limit(count).toList();
    }

    // who builds b: its farmer for a farm, the gatherers nearest it for a stockpile (they'll use it), anyone for
    // the capitol going up, else the base's builders
    List<LivingEntity> buildersFor(Building building, Vec3 near, int count) {
        if (building == bot.plan.dropOff())
            return bot.workers.stream().filter(e -> !bot.unit(e).builder && !bot.unit(e).hunter && !bot.unit(e).called &&
                            farmOf((WorkerUnit) e) == null && ((WorkerUnit) e).getBuildRepairGoal().getBuildingTarget() == null &&
                            bot.unit(e).lastOrdered != bot.tick)
                    .sorted(Comparator.comparingDouble(e -> e.position().distanceToSqr(near))).limit(count).toList();
        return building == bot.plan.farm() || building == bot.plan.capitol() ? pickWorkers(near, count) : pickBuilders(near, count);
    }

    // several foundations waiting: one builder each before any gets a second. A foundation nobody works on takes a
    // builder from one that has two or more
    private void splitBuilders(List<BuildingPlacement> all) {
        List<BuildingPlacement> waiting = all.stream().filter(b -> !b.isBuilt && countsAsBuilding(b) && !b.getBuilding().selfBuilding &&
                buildersOf(b).isEmpty() && bot.memory.enemiesNear(Vec3.atCenterOf(b.centrePos), 12).isEmpty()).toList();
        for (BuildingPlacement b : waiting) {
            Vec3 c = Vec3.atCenterOf(b.centrePos);
            List<LivingEntity> picked = pickBuilders(c, 1);
            if (picked.isEmpty()) {
                LivingEntity spare = all.stream().filter(o -> o != b && !o.isBuilt && countsAsBuilding(o))
                        .map(this::buildersOf).filter(l -> l.size() >= 2).flatMap(List::stream)
                        .min(Comparator.comparingDouble(e -> e.position().distanceToSqr(c))).orElse(null);
                if (spare == null)
                    return; // every builder is alone on its building
                picked = List.of(spare);
            }
            bot.order(UnitAction.BUILD_REPAIR, picked, b.centrePos);
        }
    }

    private void assignBuilders() {
        int cap = builderCap();
        // a copy: sending extras back to gathering can place a new stockpile, which adds to buildings
        List<BuildingPlacement> all = new ArrayList<>(bot.buildings);
        splitBuilders(all);
        for (BuildingPlacement b : all) {
            List<LivingEntity> have = buildersOf(b);
            int max = countsAsBuilding(b) ? cap : 4;
            if (have.size() > max) { // the farthest extras go back to gathering
                Vec3 c = Vec3.atCenterOf(b.centrePos);
                have.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(c)));
                for (LivingEntity extra : have.subList(max, have.size()))
                    sendToWoodOrOre((WorkerUnit) extra);
                continue;
            }
            if (b.isBuilt || b.getBuilding().selfBuilding || !bot.memory.enemiesNear(Vec3.atCenterOf(b.centrePos), 12).isEmpty())
                continue;
            // every builder of a farm becomes its farmer once it's done, so a farm gets exactly one
            int want = b.getBuilding() == bot.plan.farm() ? 1 : b.isCapitol ? 4 : b.getBuilding().cost.wood + b.getBuilding().cost.ore >= 500 ? 3 : 2;
            // a second builder only once no other foundation is still waiting for its first
            if (have.size() >= want || (have.size() >= 1 && countsAsBuilding(b) && all.stream().anyMatch(o -> o != b && !o.isBuilt &&
                    countsAsBuilding(o) && !o.getBuilding().selfBuilding && buildersOf(o).isEmpty())))
                continue;
            List<LivingEntity> picked = buildersFor(b.getBuilding(), Vec3.atCenterOf(b.centrePos), want - have.size());
            if (!picked.isEmpty())
                bot.order(UnitAction.BUILD_REPAIR, picked, b.centrePos);
        }
        // repair damaged buildings when it's safe
        for (BuildingPlacement b : bot.buildings) {
            if (!b.isBuilt || !b.getBuilding().repairable || b.getHealth() >= b.getMaxHealth() * 0.7f ||
                    !buildersOf(b).isEmpty() || !bot.memory.enemiesNear(Vec3.atCenterOf(b.centrePos), 16).isEmpty())
                continue;
            List<LivingEntity> picked = pickBuilders(Vec3.atCenterOf(b.centrePos), 1);
            if (!picked.isEmpty())
                bot.order(UnitAction.BUILD_REPAIR, picked, b.centrePos);
        }
    }

    // ---------------------------------------------------------------- hunting

    // one worker always hunts animals around the base for food (the guide's veteran hunter). Hunters get
    // bonus damage against animals, masons never hunt. Kills drop food in its inventory and RoN sends it home
    private void hunt() {
        boolean wanted = bot.tick >= noGameUntil && !bot.defence.isDefending() && bot.workers.size() >= 2;
        LivingEntity hunter = bot.workers.stream().filter(e -> bot.unit(e).hunter).findFirst().orElse(null);
        if (!wanted) {
            if (hunter != null)
                bot.unit(hunter).hunter = false; // back to normal work
            return;
        }
        // a Hunter specialist always takes over: it hunts best
        LivingEntity specialist = bot.workers.stream().filter(e -> professionOf(e) == VillagerUnitProfession.HUNTER &&
                !bot.unit(e).builder && !bot.unit(e).called).findFirst().orElse(null);
        if (hunter != null && specialist != null && professionOf(hunter) != VillagerUnitProfession.HUNTER) {
            bot.unit(hunter).hunter = false;
            hunter = null;
        }
        if (hunter == null) {
            hunter = specialist;
            if (hunter == null)
                hunter = pickWorkers(Vec3.atCenterOf(bot.home), bot.workers.size()).stream()
                        .filter(e -> professionOf(e) != VillagerUnitProfession.MASON).findFirst().orElse(null);
            // at the start every worker is building the capitol: take one of them off it, the hunt is worth more
            if (hunter == null)
                hunter = bot.workers.stream().filter(e -> professionOf(e) != VillagerUnitProfession.MASON && !bot.unit(e).builder &&
                        farmOf((WorkerUnit) e) == null && !bot.unit(e).called).findFirst().orElse(null);
            if (hunter == null)
                return;
            bot.unit(hunter).hunter = true;
        }
        BotUnit h = bot.unit(hunter);
        // just ran from (or is fighting) something: don't send it back out yet (BotDefence.protectWorkers)
        if (h.sinceOrdered(bot.tick) < REORDER_TICKS || h.fightingId >= 0 || hunter.tickCount - hunter.getLastHurtByMobTimestamp() < 10 * 20)
            return;
        // one that's chasing or walking food home is left alone, unless it chased the same animal too long
        Unit u = (Unit) hunter;
        LivingEntity chasing = u.getTargetGoal().getTarget();
        boolean returning = u.getReturnResourcesGoal() != null && u.getReturnResourcesGoal().getBuildingTarget() != null;
        if (chasing != null && h.preyId == chasing.getId() && bot.tick - h.preySince > HUNT_GIVE_UP_TICKS)
            unreachablePrey.add(chasing.getId());
        else if (chasing != null || returning)
            return;
        // ponytail: animals are found by range from home, not through fog; fine since they're near our base.
        // Units can't path through water, so animals in it are out of reach
        LivingEntity from = hunter;
        Animal prey = bot.level.getEntitiesOfClass(Animal.class, new AABB(bot.home).inflate(HUNT_RANGE, 24, HUNT_RANGE),
                        a -> a.isAlive() && !(a instanceof Unit) && ResourceSources.isHuntableAnimal(a) && !a.isInWater() &&
                                !unreachablePrey.contains(a.getId()) &&
                                bot.memory.enemiesNear(a.position(), 12).isEmpty() && !bot.memory.nearCamp(a.blockPosition(), CAMP_AVOID))
                .stream().min(Comparator.comparingDouble(a -> a.distanceToSqr(from))).orElse(null);
        if (prey == null) {
            h.hunter = false; // nothing left nearby: back to normal work, look again in a minute
            unreachablePrey.clear();
            noGameUntil = bot.tick + 60 * 20;
            return;
        }
        if (bot.orderTarget(UnitAction.ATTACK, List.of(hunter), prey)) {
            h.preyId = prey.getId();
            h.preySince = bot.tick;
        }
    }

    // ---------------------------------------------------------------- gathering

    private void assignIdleWorkers() {
        int food = 0, wood = 0, ore = 0;
        List<WorkerUnit> idle = new ArrayList<>();
        for (LivingEntity e : bot.workers) {
            WorkerUnit w = (WorkerUnit) e;
            BotUnit s = bot.unit(e);
            if (s.called)
                continue; // on its way to the bell
            if (s.hunter || farmOf(w) != null) {
                food++;
                continue;
            }
            if (WorkerUnit.isIdle(w)) {
                if (s.sinceOrdered(bot.tick) >= REORDER_TICKS)
                    idle.add(w);
                continue;
            }
            switch (w.getGatherResourceGoal().getTargetResourceName()) {
                case FOOD -> food++;
                case WOOD -> wood++;
                case ORE -> ore++;
                default -> {}
            }
        }
        // the plan's opening split until the first military building is up (the guide: wood first), then its normal one
        BotPlan.Shares shares = bot.production.hasMilitaryProducer() ? bot.plan.gather() : bot.plan.openingGather();
        int[] count = {food, wood, ore};
        // specialists go to what they're good at first, unless we're swimming in it; workers without a job then fill
        // whatever is short; the other specialists (an idle mason or hunter, a gatherer whose own resource piles up)
        // go last, so they fill what's left until their own job wants them again (hunt, assignBuilders)
        List<WorkerUnit> later = new ArrayList<>();
        idle.sort(Comparator.comparing(w -> ownResource(professionOf((LivingEntity) w)) == null));
        for (WorkerUnit w : idle) {
            VillagerUnitProfession prof = professionOf((LivingEntity) w);
            ResourceName own = ownResource(prof);
            if (own == null && prof == VillagerUnitProfession.NONE)
                gatherShort(w, count, shares);
            else if (own == null)
                later.add(w);
            else if (stock(own) < PLENTY && (own == ResourceName.FOOD ? sendToFarm(w) : sendToResource(w, own)))
                count[own.ordinal()]++; // FOOD, WOOD, ORE come first in ResourceName
            else
                later.add(w);
        }
        for (WorkerUnit w : later)
            gatherShort(w, count, shares);
    }

    // the resource we're shortest of, falling back to wood, then ore
    private void gatherShort(WorkerUnit w, int[] count, BotPlan.Shares shares) {
        ResourceName wanted = pickResource(count[0], count[1], count[2], shares);
        if (wanted == ResourceName.FOOD && sendToFarm(w))
            count[0]++;
        else if (wanted != ResourceName.ORE && sendToResource(w, ResourceName.WOOD))
            count[1]++;
        else if (sendToResource(w, ResourceName.ORE))
            count[2]++;
        else if (sendToResource(w, ResourceName.WOOD))
            count[1]++;
    }

    @Nullable
    private static ResourceName ownResource(VillagerUnitProfession prof) {
        return switch (prof) {
            case FARMER -> ResourceName.FOOD;
            case LUMBERJACK -> ResourceName.WOOD;
            case MINER -> ResourceName.ORE;
            default -> null; // hunters hunt (hunt), masons build (assignBuilders)
        };
    }

    private int stock(ResourceName r) {
        return r == ResourceName.FOOD ? bot.resources.food : r == ResourceName.WOOD ? bot.resources.wood : bot.resources.ore;
    }

    private static final int LOW = 150;

    // the resource furthest below its share of gatherers. A floating stockpile counts its gatherers 3 times over, so
    // it gets fewer but never none: the starting resources float, and banning them sent every worker to ore.
    // What the build order is saving for comes first
    private ResourceName pickResource(int food, int wood, int ore, BotPlan.Shares shares) {
        double fNeed = (food + 1) / shares.food() * (bot.resources.food >= FLOAT ? 3 : 1);
        double wNeed = (wood + 1) / shares.wood() * (bot.resources.wood >= FLOAT ? 3 : 1);
        double oNeed = (ore + 1) / shares.ore() * (bot.resources.ore >= FLOAT ? 3 : 1);
        if (bot.reserve != null) {
            if (bot.resources.food < bot.reserve.food) fNeed /= 2;
            if (bot.resources.wood < bot.reserve.wood) wNeed /= 2;
            if (bot.resources.ore < bot.reserve.ore) oNeed /= 2;
        }
        return fNeed <= wNeed && fNeed <= oNeed ? ResourceName.FOOD : (wNeed <= oNeed ? ResourceName.WOOD : ResourceName.ORE);
    }

    // gatherers are never reassigned by assignIdleWorkers, so a pile-up (2000 food, 0 wood) never fixes itself:
    // every few seconds move one from the resource we're swimming in to the one we're out of
    private void rebalanceGatherers() {
        if (bot.tick - lastRebalance < REBALANCE_TICKS)
            return;
        ResourceName[] move = rebalance(bot.resources.food, bot.resources.wood, bot.resources.ore);
        if (move == null) {
            swapSpecialist();
            return;
        }
        ResourceName rich = move[0], poor = move[1];
        // the poor resource's specialist first, then a worker without a job, the rich one's specialist last
        LivingEntity pick = bot.workers.stream()
                .filter(e -> !bot.unit(e).hunter && (rich == ResourceName.FOOD || farmOf((WorkerUnit) e) == null) &&
                        gathering((WorkerUnit) e) == rich && bot.unit(e).sinceOrdered(bot.tick) >= REORDER_TICKS)
                .min(Comparator.comparing((LivingEntity e) -> ownResource(professionOf(e)) != poor)
                        .thenComparing(e -> ownResource(professionOf(e)) == rich)).orElse(null);
        if (pick == null)
            return;
        lastRebalance = bot.tick;
        if (poor == ResourceName.FOOD)
            sendToFarm((WorkerUnit) pick);
        else
            sendToResource((WorkerUnit) pick, poor);
    }

    // a specialist gathering something else while a worker without a job does the specialist's job: they swap, one
    // pair per rebalance
    private void swapSpecialist() {
        for (LivingEntity e : bot.workers) {
            ResourceName own = ownResource(professionOf(e)), now = gathering((WorkerUnit) e);
            if (own == null || now == null || now == own || stock(own) >= PLENTY || !free(e))
                continue;
            LivingEntity other = bot.workers.stream().filter(o -> professionOf(o) == VillagerUnitProfession.NONE && free(o) &&
                    gathering((WorkerUnit) o) == own).min(Comparator.comparingDouble(o -> o.distanceToSqr(e))).orElse(null);
            if (other == null)
                continue;
            BuildingPlacement farm = farmOf((WorkerUnit) other);
            boolean sent = farm != null ? bot.order(UnitAction.FARM, List.of(e), farm.centrePos) : sendToResource((WorkerUnit) e, own);
            if (!sent)
                return;
            lastRebalance = bot.tick;
            if (now == ResourceName.FOOD)
                sendToFarm((WorkerUnit) other);
            else
                sendToResource((WorkerUnit) other, now);
            return;
        }
    }

    private boolean free(LivingEntity e) {
        BotUnit u = bot.unit(e);
        return !u.hunter && !u.builder && !u.called && u.sinceOrdered(bot.tick) >= REORDER_TICKS;
    }

    // {rich, poor} when one stockpile is huge and another a fraction of it, else null
    @Nullable
    static ResourceName[] rebalance(int food, int wood, int ore) {
        int[] amount = {food, wood, ore};
        ResourceName[] names = {ResourceName.FOOD, ResourceName.WOOD, ResourceName.ORE};
        int rich = 0, poor = 0;
        for (int i = 1; i < 3; i++) {
            if (amount[i] > amount[rich]) rich = i;
            if (amount[i] < amount[poor]) poor = i;
        }
        return amount[rich] >= FLOAT && amount[poor] < amount[rich] / 4 ? new ResourceName[]{names[rich], names[poor]} : null;
    }

    @Nullable
    private ResourceName gathering(WorkerUnit w) {
        if (w.getBuildRepairGoal().getBuildingTarget() != null)
            return null;
        if (farmOf(w) != null)
            return ResourceName.FOOD;
        ResourceName r = w.getGatherResourceGoal().getTargetResourceName();
        return r == ResourceName.FOOD || r == ResourceName.WOOD || r == ResourceName.ORE ? r : null;
    }

    boolean sendToWoodOrOre(WorkerUnit w) {
        return sendToResource(w, ResourceName.WOOD) || sendToResource(w, ResourceName.ORE);
    }

    private static final int WALK_CHECK = 16; // nearer than this, the straight line is good enough
    private static final int SCOUTED_NEAR = 32; // our drop-offs see this far around them
    private static final long SCOUTED_TICKS = 3 * 60 * 20;
    private static final long LONG_WALK_TICKS = 2 * 60 * 20;
    private final Map<Long, Long> longWalkUntil = new HashMap<>(); // 4-block cell -> tick: a long walk from its drop-off

    private boolean isLongWalk(BlockPos p) {
        Long until = longWalkUntil.get(walkCell(p));
        return until != null && bot.tick < until;
    }

    private static long walkCell(BlockPos p) {
        return BlockPos.asLong(p.getX() >> 2, 0, p.getZ() >> 2);
    }

    private boolean sendToResource(WorkerUnit w, ResourceName type) {
        longWalkUntil.values().removeIf(until -> until < bot.tick);
        if (type == ResourceName.ORE && miners() >= minerCap(bot.workers.size()))
            return false;
        boolean sent = sendToResource(w, type, true);
        if (sent && type == ResourceName.ORE)
            oreSent++; // orders are queued: it isn't mining yet this think
        return sent;
    }

    private int oreSent = 0;

    private int miners() {
        int n = oreSent;
        for (LivingEntity e : bot.workers)
            if (gathering((WorkerUnit) e) == ResourceName.ORE)
                n++;
        return n;
    }

    // ore needs few hands: 1 at the start, then one more at 8, 14 and 20 workers, never more than 4
    static int minerCap(int workers) {
        return workers < 8 ? 1 : workers < 14 ? 2 : workers < 20 ? 3 : 4;
    }

    private boolean sendToResource(WorkerUnit w, ResourceName type, boolean retry) {
        LivingEntity e = (LivingEntity) w;
        BlockPos from = bot.expansion.nearestDropOff(e.blockPosition());
        if (from == null)
            from = bot.home;
        // food (bushes, crops) is only foraged next to a drop-off: it's a trickle, a long walk for it is a loss
        // the worker's own test first: skips tree tops it can't stand next to and blocks it already gave up on
        // resources by an ally's drop-off are theirs: leave them and find our own
        List<BlockPos> allies = bot.expansion.allyDropOffs();
        int r2 = BotExpansion.ALLY_DROP_OFF_RANGE * BotExpansion.ALLY_DROP_OFF_RANGE;
        // and never into the unknown or next to danger: a spot away from our drop-offs must have been seen lately
        // (else the scout goes to look), and one with neutrals by it gets the army to clear them
        BlockPos[] unscouted = {null}, guarded = {null};
        Predicate<BlockPos> ok = bp -> {
            if (allies.stream().anyMatch(a -> a.distSqr(bp) < r2) || !w.getGatherResourceGoal().canGather(bp, type) ||
                    BuildingUtils.isPosInsideAnyBuilding(false, bp) || bot.expansion.isUnsafe(bp) || isLongWalk(bp))
                return false;
            if (bot.memory.nearCamp(bp, CAMP_AVOID)) {
                if (guarded[0] == null) guarded[0] = bp;
                return false;
            }
            if (!bot.memory.enemiesNear(Vec3.atCenterOf(bp), CAMP_AVOID).isEmpty() || !bot.memory.rememberedNear(bp, CAMP_AVOID).isEmpty())
                return false;
            if (!bot.expansion.nearOurDropOff(bp, SCOUTED_NEAR) && !bot.scout.seenWithin(bp, SCOUTED_TICKS)) {
                if (unscouted[0] == null) unscouted[0] = bp;
                return false;
            }
            return true;
        };
        ResourceIndex index = ResourceIndex.get(bot.level);
        Optional<BlockPos> target = index.findClosest(bot.level, from, type == ResourceName.FOOD ? FORAGE_RANGE : GATHER_RANGE, type, ok);
        if (target.isEmpty() && type != ResourceName.FOOD)
            target = index.findClosest(bot.level, e.blockPosition(), GATHER_FAR_RANGE, type, ok);
        // what was nearer but couldn't be used: look at it, or clear it, so it can be next time
        BlockPos got = target.orElse(null), from0 = from;
        if (unscouted[0] != null && (got == null || unscouted[0].distSqr(from0) < got.distSqr(from0)))
            bot.scout.lookAt(unscouted[0]);
        if (guarded[0] != null && bot.expansion.campToClear == null && (got == null || guarded[0].distSqr(from0) < got.distSqr(from0)))
            bot.expansion.campToClear = guarded[0];
        if (target.isEmpty()) {
            if (type == ResourceName.WOOD)
                noWoodTick = bot.tick;
            if (type != ResourceName.FOOD && bot.tick - lastNoResourceLog >= 60 * 20) {
                lastNoResourceLog = bot.tick;
                bot.log("gather: no {} within {} of the drop-off {} from home, nor {} of the worker", type, GATHER_RANGE,
                        bot.fromHome(from), GATHER_FAR_RANGE);
            }
            return false;
        }
        // close as the crow flies can be a long walk (round a river, a cliff): asked of the RTS pathfinder, which
        // answers a few ticks later. Known to be a long walk: skip that spot for a while and pick another
        if (type != ResourceName.FOOD && e instanceof Mob mob && target.get().distSqr(from) > WALK_CHECK * WALK_CHECK) {
            BotWalk.Walk walk = bot.walk.get(mob, from, target.get(), bot.tick);
            if (walk != null && (!walk.reached() || BotBuilder.isDetour(walk.length(), walk.straight()))) {
                longWalkUntil.put(walkCell(target.get()), bot.tick + LONG_WALK_TICKS);
                bot.expansion.maybeExpandDropOff(target.get(), type); // a stockpile over there may be worth it
                return retry && sendToResource(w, type, false);
            }
        }
        if (type != ResourceName.FOOD)
            bot.expansion.maybeExpandDropOff(target.get(), type);
        // never sent out past a drop-off's range on its own: only once a stockpile is going up there. Unless we can't
        // even pay for that stockpile: then the long walk home with it is the only way out, or the bot locks itself
        if (!hasDropOffNear(target.get()) && bot.canAfford(bot.plan.dropOff().cost, false)) {
            if (type == ResourceName.WOOD)
                noWoodTick = bot.tick;
            return false;
        }
        return bot.order(UnitAction.MOVE, List.of(e), target.get());
    }

    // a drop-off of ours, built or going up, within gathering range of p
    private boolean hasDropOffNear(BlockPos p) {
        return bot.buildings.stream().anyMatch(b -> (b.isCapitol || b.getBuilding().canAcceptResources) &&
                b.centrePos.distSqr(p) < GATHER_RANGE * GATHER_RANGE);
    }

    // ---------------------------------------------------------------- farms

    // a farm for each food worker the plan's split wants (workers scale with the max pop, so farms do too), a few
    // more while food runs short
    static int farmsWanted(int workers, double foodShare, boolean foodLow) {
        return Math.max(2, (int) Math.round(workers * foodShare / FARM_WORKERS * (foodLow ? 1.25 : 1)));
    }

    // after the opening (whose own farm steps are the farms until then), food keeps up with the economy, ahead of
    // the build order's savings: food buys workers and troops
    void keepFarms() {
        BotPlan plan = bot.plan;
        if (!bot.growing || !bot.hasFinished(plan.capitol()) || bot.hasUnbuilt(plan.farm()) || bot.resources.food >= FLOAT)
            return;
        if (bot.countOwned(plan.farm()) < farmsWanted(bot.workers.size(), bot.plan.gather().food(), bot.resources.food < LOW) &&
                bot.canAfford(plan.farm().cost, bot.resources.food >= 150))
            bot.builder.placeAtHome(plan.farm());
    }

    private boolean sendToFarm(WorkerUnit w) {
        if (bot.resources.food >= PLENTY)
            return sendToWoodOrOre(w);
        for (BuildingPlacement b : bot.buildings) {
            if (!b.isBuilt || b.getBuilding() != bot.plan.farm())
                continue;
            int assigned = farmersSent.getOrDefault(b, 0);
            for (LivingEntity e : bot.workers)
                if (farmOf((WorkerUnit) e) == b)
                    assigned++;
            if (assigned < FARM_WORKERS && bot.order(UnitAction.FARM, List.of((LivingEntity) w), b.centrePos)) {
                farmersSent.merge(b, 1, Integer::sum);
                return true;
            }
        }
        keepFarms(); // every farm is taken: add one, and forage nearby food meanwhile
        return sendToResource(w, ResourceName.FOOD);
    }

    // the farm a worker works, including while it walks food to a drop-off (RoN clears the farm meanwhile and
    // restores it from saveData when the worker returns)
    @Nullable
    static BuildingPlacement farmOf(WorkerUnit w) {
        var gather = w.getGatherResourceGoal();
        if (gather.getTargetFarm() != null)
            return gather.getTargetFarm();
        var ret = ((Unit) w).getReturnResourcesGoal();
        return ret != null && ret.getBuildingTarget() != null ? gather.saveData.targetFarm : null;
    }

    // extra farmers (builders that turned into farmers, reloads) go to wood or ore; the one closest stays
    private void trimFarmers() {
        Map<BuildingPlacement, List<LivingEntity>> byFarm = new HashMap<>();
        for (LivingEntity e : bot.workers) {
            BuildingPlacement farm = farmOf((WorkerUnit) e);
            if (farm != null)
                byFarm.computeIfAbsent(farm, k -> new ArrayList<>()).add(e);
        }
        byFarm.forEach((farm, farmers) -> {
            for (LivingEntity extra : extraFarmers(farmers, Vec3.atCenterOf(farm.centrePos)))
                if (bot.unit(extra).sinceOrdered(bot.tick) >= 40) // still settling from an order we just gave
                    sendToWoodOrOre((WorkerUnit) extra);
        });
    }

    static List<LivingEntity> extraFarmers(List<LivingEntity> farmers, Vec3 farmCentre) {
        if (farmers.size() <= FARM_WORKERS)
            return List.of();
        List<LivingEntity> sorted = new ArrayList<>(farmers);
        sorted.sort(Comparator.comparing((LivingEntity e) -> professionOf(e) != VillagerUnitProfession.FARMER)
                .thenComparingDouble(e -> e.position().distanceToSqr(farmCentre)));
        return sorted.subList(FARM_WORKERS, sorted.size());
    }

    // a farm we paid for never sits empty: one without a farmer gets the nearest free worker
    private void staffFarms() {
        if (bot.resources.food >= PLENTY) // farmers moved off by rebalanceGatherers stay off until food runs down
            return;
        for (BuildingPlacement b : bot.buildings) {
            if (!b.isBuilt || b.getBuilding() != bot.plan.farm() || farmersSent.getOrDefault(b, 0) > 0 ||
                    !bot.memory.enemiesNear(Vec3.atCenterOf(b.centrePos), 12).isEmpty())
                continue;
            boolean staffed = bot.workers.stream().anyMatch(e -> farmOf((WorkerUnit) e) == b ||
                    ((WorkerUnit) e).getBuildRepairGoal().getBuildingTarget() == b);
            if (staffed)
                continue;
            List<LivingEntity> picked = pickWorkers(Vec3.atCenterOf(b.centrePos), 1);
            if (!picked.isEmpty() && bot.order(UnitAction.FARM, picked, b.centrePos))
                farmersSent.merge(b, 1, Integer::sum);
        }
    }

    // ---------------------------------------------------------------- watchdog

    // a worker that hasn't moved for a while despite having a job gets stopped, so it's reassigned
    private void unstickWorkers() {
        for (LivingEntity e : bot.workers) {
            WorkerUnit w = (WorkerUnit) e;
            BotUnit s = bot.unit(e);
            boolean working = w.getGatherResourceGoal().isGathering() || w.getGatherResourceGoal().isFarming() ||
                    (w.getBuildRepairGoal().getBuildingTarget() != null && w.getBuildRepairGoal().isBuilding());
            if (s.stillAt == null || s.stillAt.distanceToSqr(e.position()) > 1 || WorkerUnit.isIdle(w) || working) {
                s.stillAt = e.position();
                s.stillSince = bot.tick;
            } else if (bot.tick - s.stillSince > STUCK_TICKS) {
                bot.log("watchdog: worker {} stuck {} from home, stopping it", e.getId(), bot.fromHome(e.blockPosition()));
                bot.order(UnitAction.STOP, List.of(e), e.blockPosition());
                s.stillSince = bot.tick;
            }
        }
    }
}
