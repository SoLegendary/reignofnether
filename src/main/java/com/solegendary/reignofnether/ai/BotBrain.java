package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingProductionClientboundPacket;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitRole;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.goals.RangedAttackBuildingGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.TemporaryMilitiaUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import com.solegendary.reignofnether.building.production.ReviveHeroProductionItem;

import java.util.*;

// One AI player. It plays only with what a player has: the same orders, placements and production, and what its
// own units and buildings can see (BotMemory). Every order costs one action from a budget set by the difficulty,
// so a harder bot just reacts faster. Each think() takes a fresh snapshot, then runs the managers, most urgent first.
public class BotBrain {

    private static final long SLOW_THINK_NANOS = 2_000_000; // a think over 2 ms is logged

    public final String name;
    public final BotDifficulty difficulty;
    public boolean takeover = false; // playing a disconnected human's base (botTakeover gamerule)

    public Faction faction;
    public BotPlan plan;
    public BotPlan.Matchup matchup;
    private String matchupKey = "";
    public BotMemory memory;

    final BotPlacer placer = new BotPlacer();
    final BotWalk walk = new BotWalk();
    final BotEconomy economy = new BotEconomy(this);
    final BotExpansion expansion = new BotExpansion(this);
    final BotBuilder builder = new BotBuilder(this);
    final BotProduction production = new BotProduction(this);
    final BotDefence defence = new BotDefence(this);
    final BotArmy army = new BotArmy(this);
    final BotMicro micro = new BotMicro(this);
    final BotScout scout = new BotScout(this);
    final BotHeroes heroes = new BotHeroes(this);
    final BotComms comms = new BotComms(this);
    final BotDiplomacy diplomacy = new BotDiplomacy(this);

    // per-think snapshot
    public ServerLevel level;
    public long tick;
    public Resources resources;
    public BlockPos home = BlockPos.ZERO;
    final List<BuildingPlacement> buildings = new ArrayList<>();
    final List<LivingEntity> units = new ArrayList<>();
    final List<LivingEntity> workers = new ArrayList<>();
    final List<LivingEntity> fighters = new ArrayList<>(); // army units incl. heroes, excl. workers and scouts
    final List<LivingEntity> scouts = new ArrayList<>();
    final List<LivingEntity> militia = new ArrayList<>(); // villagers called to arms: they only defend (BotDefence)
    private final Map<Integer, BotUnit> state = new HashMap<>(); // our living units by entity id
    private int pop, supply;

    boolean growing = false; // past the opening, for good (a lost opening building is just rebuilt)
    ResourceCost reserve = null; // the build order is saving for this; production won't spend into it (BotBuilder)
    ResourceCost heroReserve = null; // a dead hero's revive we can't pay for yet: nothing else spends into it (BotHeroes)
    // two budgets: the fights get 60% of the APM (micro is where a bot's speed wins), the economy the rest, and
    // neither can starve the other
    private double actions = 0, combatActions = 0;
    private static final Set<String> COMBAT_STEPS = Set.of("defence", "army", "micro", "heroes");
    private int starved = 0; // actions wanted but not available, since the last status line
    public long firstThinkTick = -1;
    public long startTick = -1; // set by BotServerEvents: when this bot's think rhythm starts
    private long lastSlowLog = -1_000_000;

    public BotBrain(String name, BotDifficulty difficulty) {
        this.name = name;
        this.difficulty = difficulty;
    }

    public void think(ServerLevel level, long tick) {
        long started = System.nanoTime();
        this.level = level;
        this.tick = tick;
        if (!snapshot())
            return;
        if (memory == null) {
            memory = new BotMemory(name, home);
            placer.avoid = p -> memory.nearCamp(p, BotEconomy.CAMP_AVOID) ||
                    (expansion.dropOffFor != null && expansion.badDropOffSpot(p));
            placer.reachable = builder::reachable;
        }
        memory.update(tick);
        updateMatchup();
        if (firstThinkTick < 0)
            firstThinkTick = tick;
        if (maybeSurrender())
            return;
        if (gameWon())
            return;
        double eco = difficulty.actionsPerThink() * 0.4, combat = difficulty.actionsPerThink() * 0.6;
        // a slow bot can't bank actions for a burst
        actions = Math.min(actions + eco, eco * 2 + 1);
        combatActions = Math.min(combatActions + combat, combat * 2 + 1);

        // two phases: the plan's opening first (only its own buildings, plus workers, troops and a house when
        // capped), then growth for good (farms, houses ahead of production, more barracks, outposts, the matchup's
        // tech). Each step runs on its own, so a bug in one logs and is skipped instead of freezing the whole bot
        if (!growing && (builder.openingDone() || minutesPlayed() >= plan.openingMaxMinutes())) {
            growing = true;
            log("growth phase{}", builder.openingDone() ? "" : " (the opening ran out of time)");
        }
        step("defence", defence::think);
        step("supply", builder::buildSupply);
        step("heroRevive", heroes::trainAndRevive); // a dead hero first: its money is held back from everything below
        step("production", production::think); // army first: troops get the food before new workers
        step("workers", economy::trainWorkers);
        step("economy", economy::think);
        step("army", army::think);
        step("heroes", heroes::think); // casts before micro: an ability is worth more than a step back
        step("micro", micro::think);
        step("buildOrder", builder::think);
        step("farms", economy::keepFarms);
        step("expansion", expansion::think);
        step("scout", scout::think);
        step("comms", comms::think);
        if (!takeover) // a disconnected human's alliances are theirs to make
            step("diplomacy", diplomacy::think);
        logStatus();
        long took = System.nanoTime() - started;
        if (took > SLOW_THINK_NANOS && tick - lastSlowLog >= 1200) {
            lastSlowLog = tick;
            log("slow think: {} ms", String.format(Locale.ROOT, "%.1f", took / 1e6));
        }
    }

    private final Map<String, Long> stepFailedLogged = new HashMap<>(); // step -> tick its error was last logged

    private String currentStep = "";
    private final Map<String, Integer> spentBy = new TreeMap<>(); // actions per step since the last status line

    private void step(String stepName, Runnable r) {
        currentStep = stepName;
        try {
            r.run();
        } catch (RuntimeException ex) {
            if (tick - stepFailedLogged.getOrDefault(stepName, -1_000_000L) >= 1200) { // a full trace once a minute per step
                stepFailedLogged.put(stepName, tick);
                ReignOfNether.LOGGER.error("[Bot] {} step {} failed", name, stepName, ex);
            }
        }
    }

    private void logStatus() {
        if ((tick - firstThinkTick) % 1200 >= difficulty.ticksPerThink)
            return;
        log("{}min vs {} pop {}/{} F{} W{} O{} E{} workers {} army {} buildings {} reserve {}{} starved {} spent {}",
                minutesPlayed(), matchupKey, pop, supply, resources.food, resources.wood, resources.ore, resources.emerald,
                workers.size(), army.describe(), buildings.size(),
                reserve == null ? "-" : reserve.food + "/" + reserve.wood + "/" + reserve.ore,
                heroReserve == null ? "" : " hero " + heroReserve.food + "/" + heroReserve.wood + "/" + heroReserve.ore, starved, spentBy);
        starved = 0;
        spentBy.clear();
    }

    // event log for playtests: grep "[Bot] <name>" in latest.log. Positions are given as distance from home
    void log(String msg, Object... args) {
        ReignOfNether.LOGGER.info("[Bot] " + name + " " + msg, args);
    }

    int fromHome(BlockPos p) {
        return BotMath.dist(p, home);
    }

    public int minutesPlayed() {
        return (int) ((tick - firstThinkTick) / 1200);
    }

    // ---------------------------------------------------------------- snapshot

    private boolean snapshot() {
        RTSPlayer rtsPlayer = PlayerServerEvents.getRTSPlayer(name);
        if (rtsPlayer == null)
            return false;
        if (faction != rtsPlayer.faction || plan == null) {
            faction = rtsPlayer.faction;
            plan = BotPlans.get(faction);
        }
        if (plan == null)
            return false; // the faction's AI is in maintenance (BotPlans)
        resources = findResources();
        if (resources == null)
            return false;

        buildings.clear();
        for (BuildingPlacement b : BuildingServerEvents.getBuildings())
            if (b.ownerName.equals(name) && !b.isDestroyedServerside)
                buildings.add(b);

        units.clear();
        workers.clear();
        fighters.clear();
        scouts.clear();
        militia.clear();
        Set<Integer> alive = new HashSet<>();
        for (LivingEntity e : UnitServerEvents.getAllUnits()) {
            if (!e.isAlive() || !(e instanceof Unit u) || !u.getOwnerName().equals(name))
                continue;
            units.add(e);
            alive.add(e.getId());
            state.computeIfAbsent(e.getId(), id -> new BotUnit(e)).entity = e;
            Set<UnitRole> roles = u.getUnitRoles();
            if (e instanceof TemporaryMilitiaUnit)
                militia.add(e);
            else if (roles.contains(UnitRole.WORKER))
                workers.add(e);
            else if (roles.contains(UnitRole.SCOUT))
                scouts.add(e);
            else if (!roles.contains(UnitRole.SUMMON))
                fighters.add(e);
        }
        state.keySet().retainAll(alive);

        BuildingPlacement capitol = getCapitol();
        if (capitol != null)
            home = capitol.centrePos;
        else if (!buildings.isEmpty())
            home = buildings.get(0).centrePos;

        pop = UnitServerEvents.getCurrentPopulation(name);
        supply = BuildingServerEvents.getTotalPopulationSupply(name);
        return !units.isEmpty() || !buildings.isEmpty();
    }

    // what we're up against: the faction most of our living enemies play (its key's path, e.g. "monsters")
    private void updateMatchup() {
        Map<String, Integer> count = new HashMap<>();
        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.rtsPlayers)
                if (memory.isEnemy(p.name) && p.faction != null && p.faction.key != null)
                    count.merge(p.faction.key.getPath(), 1, Integer::sum);
        }
        String key = count.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("default");
        if (!key.equals(matchupKey) || matchup == null) {
            matchupKey = key;
            matchup = plan.matchup(key);
            log("matchup: vs {}", key);
        }
    }

    BotUnit unit(LivingEntity e) {
        return state.computeIfAbsent(e.getId(), id -> new BotUnit(e));
    }

    @Nullable
    public BuildingPlacement getCapitol() {
        for (BuildingPlacement b : buildings)
            if (b.isCapitol)
                return b;
        return null;
    }

    public boolean hasFinished(Building building) {
        for (BuildingPlacement b : buildings)
            if (b.isBuilt && b.getBuilding().isTypeOf(building))
                return true;
        return false;
    }

    int countOwned(Building building) {
        int n = 0;
        for (BuildingPlacement b : buildings)
            if (b.getBuilding().isTypeOf(building)) // same as hasFinished: an upgraded portal still counts
                n++;
        return n;
    }

    boolean hasUnbuilt(Building building) {
        for (BuildingPlacement b : buildings)
            if (!b.isBuilt && b.getBuilding() == building)
                return true;
        return false;
    }

    public int getPop() {
        return pop;
    }

    public int getSupply() {
        return supply;
    }

    static boolean isHero(LivingEntity e) {
        return e instanceof HeroUnit;
    }

    static boolean has(LivingEntity e, UnitRole role) {
        return e instanceof Unit u && u.getUnitRoles().contains(role);
    }

    // the building a unit is hitting, if any
    @Nullable
    static BuildingPlacement buildingTarget(LivingEntity e) {
        if (!(e instanceof AttackerUnit a))
            return null;
        return a.getAttackBuildingGoal() instanceof MeleeAttackBuildingGoal melee ? melee.getBuildingTarget()
                : a.getAttackBuildingGoal() instanceof RangedAttackBuildingGoal<?> ranged ? ranged.getBuildingTarget() : null;
    }

    // idle, or still hitting the ruins of a destroyed building (RoN only lets go once its last block is gone):
    // either way free for a new order
    static boolean isFree(LivingEntity e) {
        BuildingPlacement b = buildingTarget(e);
        return ((Unit) e).isIdle() || (b != null && b.shouldBeDestroyed());
    }

    // ---------------------------------------------------------------- resources

    public boolean canAfford(ResourceCost cost, boolean respectReserve) {
        ResourceCost r = respectReserve ? reserve : null, h = heroReserve;
        return affordable(resources.food, cost.food, (r == null ? 0 : r.food) + (h == null ? 0 : h.food)) &&
                affordable(resources.wood, cost.wood, (r == null ? 0 : r.wood) + (h == null ? 0 : h.wood)) &&
                affordable(resources.ore, cost.ore, (r == null ? 0 : r.ore) + (h == null ? 0 : h.ore));
    }

    // the build order's savings only hold back what an item actually spends: ore saved for a step mustn't
    // block a unit that costs no ore (that soft-locked a bot whose ore income had dried up)
    static boolean affordable(int have, int cost, int reserved) {
        return have >= cost && (cost == 0 || have - cost >= reserved);
    }

    @Nullable
    private Resources findResources() {
        for (Resources r : ResourcesServerEvents.resourcesList)
            if (r.ownerName.equals(name))
                return r;
        return null;
    }

    // spending replaces the Resources object, so re-read it after placing, producing or buying
    void refreshResources() {
        Resources r = findResources();
        if (r != null)
            resources = r;
    }

    // ---------------------------------------------------------------- production

    // the least busy finished building of this type that makes item
    @Nullable
    ProductionPlacement findProducer(Building building, ProductionItem item) {
        ProductionPlacement best = null;
        for (BuildingPlacement b : buildings)
            if (b instanceof ProductionPlacement pp && b.isBuilt && b.getBuilding().isTypeOf(building) && makes(b, item) &&
                    (best == null || pp.productionQueue.size() < best.productionQueue.size()))
                best = pp;
        return best;
    }

    static boolean makes(BuildingPlacement b, ProductionItem item) {
        return b.getBuilding() instanceof ProductionBuilding prod && prod.productions.get().contains(item);
    }

    boolean startProduction(ProductionPlacement pp, ProductionItem item) {
        // everything but a revive leaves the money held for a dead hero alone
        boolean heldBack = heroReserve != null && !(item instanceof ReviveHeroProductionItem) && !canAfford(item.getCost(false, name), false);
        if (!makes(pp, item) || !item.canProduce(pp) || !item.canAfford(pp) || heldBack || !spendAction())
            return false;
        if (!pp.startProductionItem(item))
            return false;
        ResourceLocation key = ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item);
        if (key != null)
            BuildingProductionClientboundPacket.startProduction(name, pp.centrePos, key.toString());
        refreshResources();
        return true;
    }

    // ---------------------------------------------------------------- orders

    boolean spendAction() {
        boolean combat = COMBAT_STEPS.contains(currentStep);
        if ((combat ? combatActions : actions) < 1) {
            starved++;
            return false;
        }
        if (combat)
            combatActions--;
        else
            actions--;
        spentBy.merge(currentStep, 1, Integer::sum);
        return true;
    }

    // a whole think's worth of actions is left
    boolean hasActions(int n) {
        return (COMBAT_STEPS.contains(currentStep) ? combatActions : actions) >= n;
    }

    // one order to a group is one action, like a player box-selecting and clicking; false when out of actions.
    // Units given this exact order a moment ago are left out: a unit that can't quite get somewhere would
    // otherwise be re-ordered every think and eat the whole budget
    boolean order(UnitAction action, List<LivingEntity> who, BlockPos pos) {
        who = notRepeat(notCasting(action, who), action, pos.asLong());
        if (who.isEmpty() || !spendAction())
            return false;
        int[] ids = markOrdered(who, action, pos.asLong());
        // players always click a block; flying units ignore a move onto air, so aim at the ground below instead.
        // Only for moves: BUILD_REPAIR/FARM find their building at pos, and a foundation's centre is air
        if ((action == UnitAction.MOVE || action == UnitAction.ATTACK_MOVE) && level.getBlockState(pos).isAir())
            pos = BotComms.ground(this, pos); // under the canopy, not on it
        UnitServerEvents.addActionItem(name, action, -1, ids, pos, pos);
        return true;
    }

    boolean orderTarget(UnitAction action, List<LivingEntity> who, LivingEntity target) {
        who = notRepeat(notCasting(action, who), action, target.getId());
        if (who.isEmpty() || !spendAction())
            return false;
        UnitServerEvents.addActionItem(name, action, target.getId(), markOrdered(who, action, target.getId()), target.blockPosition(), target.blockPosition());
        return true;
    }

    private static final int REPEAT_TICKS = 3 * 20;

    private List<LivingEntity> notRepeat(List<LivingEntity> who, UnitAction action, long target) {
        if (who.stream().noneMatch(e -> isRepeat(unit(e), action, target)))
            return who;
        return who.stream().filter(e -> !isRepeat(unit(e), action, target)).toList();
    }

    private boolean isRepeat(BotUnit u, UnitAction action, long target) {
        return u.lastAction == action && u.lastTarget == target && u.sinceOrdered(tick) < REPEAT_TICKS;
    }

    private int[] markOrdered(List<LivingEntity> who, UnitAction action, long target) {
        int[] ids = new int[who.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = who.get(i).getId();
            BotUnit u = unit(who.get(i));
            u.lastOrdered = tick;
            u.lastAction = action;
            u.lastTarget = target;
        }
        return ids;
    }

    // any new order restarts a channelled ability, and cooldown is only charged when it completes: leave casters be
    private static List<LivingEntity> notCasting(UnitAction action, List<LivingEntity> who) {
        if (action == UnitAction.STOP || who.stream().noneMatch(BotHeroes::busyCasting))
            return who;
        return who.stream().filter(e -> !BotHeroes.busyCasting(e)).toList();
    }

    // ---------------------------------------------------------------- surrender

    // a bot gives up like a player would: with no workers left and no way to train one (no capitol, or no food for
    // a worker and nobody to gather it), or with its capitol, most of its army and most buildings gone. A takeover
    // never surrenders a human's base
    private static final int SURRENDER_TICKS = 20 * 20;
    private int peakArmyPop = 0, peakBuildings = 0;
    private long beatenSince = -1;

    private boolean hadEnemy = false, won = false;

    // no enemy left in the game: everyone home once, then nothing more to do. An alliance broken later wakes it up
    private boolean gameWon() {
        boolean enemyLeft;
        synchronized (PlayerServerEvents.rtsPlayers) {
            enemyLeft = PlayerServerEvents.rtsPlayers.stream().anyMatch(p -> memory.isEnemy(p.name));
        }
        hadEnemy |= enemyLeft;
        if (enemyLeft || !hadEnemy) {
            won = false;
            return false;
        }
        if (!won) {
            won = true;
            actions = Math.max(actions, 1);
            log("no enemy left: everyone home");
            order(UnitAction.MOVE, units, home);
        }
        return true;
    }

    private boolean maybeSurrender() {
        int armyPop = BotMath.popOf(fighters);
        peakArmyPop = Math.max(peakArmyPop, armyPop);
        peakBuildings = Math.max(peakBuildings, buildings.size());
        BuildingPlacement capitol = getCapitol();
        boolean canTrainWorker = capitol != null && capitol.isBuilt && canAfford(plan.worker().getCost(false, name), false);
        if (takeover || !(noEconomy(workers.size(), canTrainWorker) || beaten(capitol != null, armyPop, peakArmyPop, buildings.size(), peakBuildings))) {
            beatenSince = -1;
            return false;
        }
        if (beatenSince < 0)
            beatenSince = tick;
        if (tick - beatenSince < SURRENDER_TICKS)
            return false;
        log("surrenders");
        PlayerServerEvents.defeat(name, Component.translatable("server.reignofnether.surrendered").getString());
        return true; // BotServerEvents says gg once the bot is out of the game
    }

    static boolean noEconomy(int workers, boolean canTrainWorker) {
        return workers == 0 && !canTrainWorker;
    }

    static boolean beaten(boolean hasCapitol, int armyPop, int peakArmyPop, int buildings, int peakBuildings) {
        return !hasCapitol && armyPop <= Math.max(3, peakArmyPop / 4) && buildings <= Math.max(2, peakBuildings / 4);
    }
}
