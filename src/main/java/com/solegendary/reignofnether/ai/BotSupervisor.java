package com.solegendary.reignofnether.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.unit.UnitRole;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPOutputStream;

// The thinking, once a second, on a 32x32 map of the game (BotGrid): where the main army and a second army split off
// it go and what they do there, each hero, the raid army and a scout, which building goes up where, the next research,
// whether to train workers, how they gather and which troops come next. With a net (BotGridNet) loaded it decides and
// the rest of the bot is its puppet: paths, the exact spot, gathering, micro and spells (BotPuppet carries out the
// orders). Without one the script plays as before, and in the arena its choices are written down in the same terms
// so RoN-Bots can teach a net to do the same, then better. Format: RoN-Bots/FORMAT.md (grid-1).
public class BotSupervisor {

    static final int DECIDE_EVERY = 20;
    static final int HOLD = 0, MOVE = 1, ATTACK = 2, RETREAT = 3; // army orders
    static final int FOLLOW = 0, HERO_MOVE = 1, HERO_ATTACK = 2, HERO_HOME = 3; // hero orders
    static final int HOME = 0, OUT = 1; // raid ATTACK, scout SCOUT
    static final double[] SPLITS = {0, 0.25, 0.5}; // of the main army, for the second
    // the troops a villager net can ask for, in its output order; ones the matchup doesn't train are masked out
    static final List<String> UNITS = List.of("vindicator", "pillager", "iron_golem", "witch", "evoker", "ravager", "windcaller");
    // food / wood / ore shares of the gatherers it can pick from; the first is the plan's
    static final List<BotPlan.Shares> GATHER = List.of(new BotPlan.Shares(0.3, 0.5, 0.2), new BotPlan.Shares(0.45, 0.4, 0.15),
            new BotPlan.Shares(0.2, 0.65, 0.15), new BotPlan.Shares(0.25, 0.4, 0.35), new BotPlan.Shares(0.35, 0.35, 0.3));
    private static final List<UnitRole> ROLES = List.of(UnitRole.FRONTLINE, UnitRole.RANGED, UnitRole.CASTER, UnitRole.HEALER, UnitRole.FLYER, UnitRole.SIEGE);
    static final List<String> INPUTS = inputNames();
    static final List<String> HEADS = List.of("mainOrder", "mainCell", "secondSplit", "secondOrder", "secondCell", "hero0Order", "hero0Cell",
            "hero1Order", "hero1Cell", "raidOrder", "raidCell", "scoutOrder", "scoutCell", "build", "buildCell", "research", "workers", "gather", "unit");

    private final BotBrain bot;
    final BotGrid grid;
    private final BotTally tally;
    @Nullable private BotGridNet net;
    private boolean netResolved = false;
    private long lastDecide = -1_000_000, enemyArmySeenTick = -1_000_000;
    private final Random random = new Random();

    // an ally's ping: help me here, or attack here. Bot allies' armies on the move count as attack calls too
    record AllyCall(BlockPos pos, boolean help, long tick) {}
    private static final long CALL_TICKS = 60 * 20;
    private static final long HOLD_TICKS = 15 * 20, SCOUT_HOLD_TICKS = 60 * 20; // how long a group keeps its cell
    private final List<AllyCall> calls = new ArrayList<>();
    private final Set<BlockPos> answered = new HashSet<>();

    // a group's order and where (null when the order needs no place)
    record Group(int order, @Nullable BlockPos at) {}

    // the current orders, as the puppet reads them
    Group main = new Group(HOLD, null), second = new Group(HOLD, null), raid = new Group(HOME, null), scout = new Group(OUT, null);
    final Group[] heroes = {new Group(FOLLOW, null), new Group(FOLLOW, null)};
    double split = 0;
    @Nullable Building build;
    @Nullable BlockPos buildAt;
    @Nullable BotPlan.Step research;
    boolean trainWorkers = true;
    private BotPlan.Shares shares = GATHER.get(0);
    private List<ProductionItem> units = List.of();

    // what the script did since the last decision, for its label
    @Nullable private Building builtNow;
    @Nullable private BlockPos builtAt;
    @Nullable private ProductionItem researchedNow;

    BotSupervisor(BotBrain bot) {
        this.bot = bot;
        this.grid = new BotGrid(bot);
        this.tally = new BotTally(bot);
    }

    // an ally pinged for help or an attack: a place the army can go for a minute (BotArmy.answerHelp / answerAttack)
    void allyAsked(BlockPos pos, boolean help) {
        calls.removeIf(c -> c.pos().distSqr(pos) < 24 * 24);
        calls.add(new AllyCall(pos, help, bot.tick));
    }

    void built(Building building, BlockPos at) {
        builtNow = building;
        builtAt = at;
    }

    void researched(ProductionItem item) {
        researchedNow = item;
    }

    // a net makes the decisions; otherwise every order above is unused and the script decides
    boolean controls() {
        resolveNet();
        return net != null;
    }

    BotPlan.Shares shares() {
        return shares;
    }

    // troops in the order the net wants them
    List<ProductionItem> units() {
        return units;
    }

    private void resolveNet() {
        if (netResolved || bot.faction == null)
            return;
        netResolved = true;
        Path file = ArenaServerEvents.netFor(bot.name);
        try {
            net = file != null ? BotGridNet.parse(JsonParser.parseString(Files.readString(file)).getAsJsonObject()) : BotNets.get(bot.faction);
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("[Bot] {} can't load its net, the script plays", bot.name, e);
            net = null;
        }
        if (net != null && net.inputs != INPUTS.size()) {
            ReignOfNether.LOGGER.error("[Bot] {} net takes {} inputs, the bot gives {}: the script plays", bot.name, net.inputs, INPUTS.size());
            net = null;
        }
        if (net != null)
            bot.log("supervisor: a net does the thinking");
    }

    void think() {
        if (bot.matchup == null || bot.tick - lastDecide < DECIDE_EVERY)
            return;
        lastDecide = bot.tick;
        boolean logging = ArenaServerEvents.decisionsDir() != null;
        if (!controls() && !logging)
            return;
        if (bot.memory.visibleEnemies.stream().anyMatch(BotSupervisor::isArmy))
            enemyArmySeenTick = bot.tick;
        grid.update();
        double[] global = global();
        Masks masks = masks();
        JsonObject act = null, odds = null;
        BotGridNet.Output out = null;
        double[] lp = {0};
        if (net != null) {
            out = net.run(grid.input(), global);
            odds = new JsonObject();
            act = decide(out, masks, lp, odds);
        }
        JsonObject label = label();
        builtNow = null;
        researchedNow = null;
        if (logging) {
            tally.update();
            write(global, masks, label, act, lp[0], out);
            writeLive(global, label, act, out, odds, masks);
        }
    }

    // ---------------------------------------------------------------- the vocab

    List<String> buildingNames() {
        return net != null ? net.buildings : names(buildingVocab().keySet());
    }

    List<String> researchNames() {
        return net != null ? net.research : names(researchVocab().keySet());
    }

    private static List<String> names(Set<String> set) {
        return set.stream().sorted().toList();
    }

    // what the net can put up: every building in the plan but houses, farms and the capitol (the puppet keeps those
    // going), each with the buildings it needs first
    private Map<String, BotPlan.Step> buildingVocab() {
        Map<String, BotPlan.Step> map = new HashMap<>();
        for (BotPlan.Step s : allSteps())
            if (s.type() == BotPlan.StepType.BUILD && s.building() != bot.plan.supply() && s.building() != bot.plan.farm() && s.building() != bot.plan.capitol())
                map.putIfAbsent(key(s.building()), s);
        for (BotPlan.Matchup m : bot.plan.matchups().values())
            for (BotPlan.ArmyUnit a : m.army()) {
                Building p = BotProduction.producerOf(a.item());
                if (p != null)
                    map.putIfAbsent(key(p), new BotPlan.Step(BotPlan.StepType.BUILD, p, 1, null, a.needs() == null ? List.of() : List.of(a.needs())));
            }
        map.putIfAbsent(key(bot.plan.dropOff()), new BotPlan.Step(BotPlan.StepType.BUILD, bot.plan.dropOff(), 1, null, List.of()));
        return map;
    }

    private Map<String, BotPlan.Step> researchVocab() {
        Map<String, BotPlan.Step> map = new HashMap<>();
        for (BotPlan.Step s : allSteps())
            if (s.type() == BotPlan.StepType.RESEARCH)
                map.putIfAbsent(name(s.item()), s);
        return map;
    }

    private List<BotPlan.Step> allSteps() {
        List<BotPlan.Step> steps = new ArrayList<>(bot.plan.opening());
        bot.plan.matchups().values().forEach(m -> steps.addAll(m.tech()));
        return steps;
    }

    private static String key(Building b) {
        ResourceLocation k = ReignOfNetherRegistries.BUILDING.getKey(b);
        return k == null ? b.name : k.getPath();
    }

    private static String name(ProductionItem item) {
        ResourceLocation key = ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item);
        return key == null ? "" : key.getPath();
    }

    private List<String> heroNames() {
        return bot.plan.heroes().stream().map(h -> name(h.unit())).toList();
    }

    // the hero in a plan slot, if it's alive
    @Nullable
    LivingEntity hero(int slot) {
        if (slot >= bot.plan.heroes().size())
            return null;
        var type = Factions.unitTypeOf(bot.plan.heroes().get(slot).unit());
        return bot.fighters.stream().filter(e -> BotBrain.isHero(e) && e.getType() == type).findFirst().orElse(null);
    }

    // the scout the net steers; any others scout as before
    @Nullable
    LivingEntity netScout() {
        return bot.scouts.stream().min(Comparator.comparingInt(LivingEntity::getId)).orElse(null);
    }

    // ---------------------------------------------------------------- masks

    record Masks(boolean[] build, boolean[] research, boolean[] unit, boolean hero0, boolean hero1) {}

    private Masks masks() {
        Map<String, BotPlan.Step> bv = buildingVocab(), rv = researchVocab();
        List<String> bn = buildingNames(), rn = researchNames();
        boolean base = bot.hasFinished(bot.plan.capitol());
        boolean[] build = new boolean[bn.size() + 1], research = new boolean[rn.size() + 1];
        build[0] = research[0] = true;
        for (int i = 0; i < bn.size(); i++) {
            BotPlan.Step s = bv.get(bn.get(i));
            build[i + 1] = s != null && base && !bot.hasUnbuilt(s.building()) && s.needs().stream().allMatch(bot::hasFinished);
        }
        for (int i = 0; i < rn.size(); i++) {
            BotPlan.Step s = rv.get(rn.get(i));
            research[i + 1] = s != null && !bot.builder.isDone(s) && bot.hasFinished(s.building()) && s.needs().stream().allMatch(bot::hasFinished);
        }
        return new Masks(build, research, unitMask(), hero(0) != null, hero(1) != null);
    }

    private boolean[] unitMask() {
        boolean[] mask = new boolean[UNITS.size()];
        Map<ProductionItem, Integer> have = bot.production.counts();
        for (BotPlan.ArmyUnit a : bot.matchup.army()) {
            int i = UNITS.indexOf(name(a.item()));
            if (i >= 0 && bot.production.unlocked(a, have))
                mask[i] = true;
        }
        return mask;
    }

    // ---------------------------------------------------------------- deciding

    private JsonObject decide(BotGridNet.Output out, Masks m, double[] lp, JsonObject odds) {
        boolean sample = ArenaServerEvents.sampling();
        JsonObject act = new JsonObject();
        for (String h : HEADS)
            act.addProperty(h, -1);

        int mo = pick(out, "mainOrder", null, sample, act, lp, odds);
        main = new Group(mo, mo == RETREAT ? null : held(out, "mainCell", mo, HOLD_TICKS, false, sample, act, lp, odds));
        int sp = pick(out, "secondSplit", null, sample, act, lp, odds);
        split = SPLITS[sp];
        if (sp > 0) {
            int so = pick(out, "secondOrder", null, sample, act, lp, odds);
            second = new Group(so, so == RETREAT ? null : held(out, "secondCell", so, HOLD_TICKS, false, sample, act, lp, odds));
        }
        boolean[] alive = {m.hero0(), m.hero1()};
        for (int k = 0; k < 2; k++) {
            if (!alive[k])
                continue;
            int o = pick(out, "hero" + k + "Order", null, sample, act, lp, odds);
            heroes[k] = new Group(o, o == HERO_MOVE || o == HERO_ATTACK ? held(out, "hero" + k + "Cell", o, HOLD_TICKS, false, sample, act, lp, odds) : null);
        }
        int ro = pick(out, "raidOrder", null, sample, act, lp, odds);
        raid = new Group(ro, ro == OUT ? held(out, "raidCell", ro, HOLD_TICKS, false, sample, act, lp, odds) : null);
        int sc = pick(out, "scoutOrder", null, sample, act, lp, odds);
        LivingEntity s = netScout();
        Held was = this.held.get("scoutCell");
        boolean there = s != null && was != null && was.at() != null && s.blockPosition().distSqr(was.at()) < 16 * 16;
        scout = new Group(sc, sc == OUT ? held(out, "scoutCell", sc, SCOUT_HOLD_TICKS, there, sample, act, lp, odds) : null);

        int b = pick(out, "build", m.build(), sample, act, lp, odds);
        buildAt = b > 0 ? cell(out, "buildCell", grid.buildable, sample, act, lp, odds) : null;
        build = b > 0 && buildAt != null ? buildingVocab().get(buildingNames().get(b - 1)).building() : null;
        int r = pick(out, "research", m.research(), sample, act, lp, odds);
        research = r > 0 ? researchVocab().get(researchNames().get(r - 1)) : null;
        trainWorkers = pick(out, "workers", null, sample, act, lp, odds) == 1;
        shares = GATHER.get(pick(out, "gather", null, sample, act, lp, odds));

        // the drawn unit first, then the rest by the net's odds: a building that can't train it trains the next one
        int u = pick(out, "unit", m.unit(), sample, act, lp, odds);
        List<Integer> order = BotNet.ranked(out.logits("unit"), m.unit());
        if (u >= 0) {
            order.remove(Integer.valueOf(u));
            order.add(0, u);
        }
        List<ProductionItem> list = new ArrayList<>();
        for (int i : order) {
            ProductionItem item = item(UNITS.get(i));
            if (item != null)
                list.add(item);
        }
        units = list;
        if (main.order() == ATTACK && main.at() != null && !main.at().equals(pinged)) { // tell the team, as the script does
            pinged = main.at();
            bot.comms.attacking(main.at());
        }
        answerCalls();
        return act;
    }

    private int pick(BotGridNet.Output out, String head, @Nullable boolean[] mask, boolean sample, JsonObject act, double[] lp, JsonObject odds) {
        double[] logits = out.logits(head);
        BotNet.Pick p = BotNet.pick(logits, mask, sample, random);
        act.addProperty(head, p.index());
        if (p.index() >= 0)
            lp[0] += p.logProb();
        odds.add(head, numbers(softmax(logits, mask)));
        return p.index();
    }

    @Nullable
    private BlockPos cell(BotGridNet.Output out, String head, boolean[] mask, boolean sample, JsonObject act, double[] lp, JsonObject odds) {
        int c = pick(out, head, mask, sample, act, lp, odds);
        return c < 0 ? null : grid.pos(c);
    }

    // a group keeps its cell until it gets there, its order changes or the time is up: asked again every second (and
    // drawn by odds in self-play) it turned round before arriving anywhere. Kept seconds aren't choices (act -1)
    private record Held(int order, @Nullable BlockPos at, long tick) {}

    private final Map<String, Held> held = new HashMap<>();
    @Nullable private BlockPos pinged; // the last attack the team was told about

    @Nullable
    private BlockPos held(BotGridNet.Output out, String head, int order, long keep, boolean arrived, boolean sample, JsonObject act, double[] lp, JsonObject odds) {
        Held h = held.get(head);
        if (h != null && h.at() != null && h.order() == order && !arrived && bot.tick - h.tick() < keep)
            return h.at();
        BlockPos at = cell(out, head, grid.reach, sample, act, lp, odds);
        held.put(head, new Held(order, at, bot.tick));
        return at;
    }

    // the army heads for an ally's call: say so once
    private void answerCalls() {
        allyCalls();
        if (main.at() == null || (main.order() != ATTACK && main.order() != MOVE))
            return;
        double near = grid.cellBlocks() * 1.5;
        calls.stream().filter(c -> !answered.contains(c.pos()) && c.pos().distSqr(main.at()) < near * near).findFirst().ifPresent(c -> {
            answered.add(c.pos());
            bot.comms.answered(c.help(), c.pos());
            bot.log("supervisor: answers an ally's {} call {} from home", c.help() ? "help" : "attack", bot.fromHome(c.pos()));
        });
    }

    // this minute's ally calls, plus where ally bots' armies are attacking
    private void allyCalls() {
        calls.removeIf(c -> bot.tick - c.tick() > CALL_TICKS);
        answered.removeIf(p -> calls.stream().noneMatch(c -> c.pos().equals(p)));
        for (String ally : AlliancesServerEvents.getAllAllies(bot.name)) {
            BotBrain b = BotServerEvents.brain(ally);
            BlockPos going = b == null ? null : b.army.heading();
            if (going != null && calls.stream().noneMatch(c -> c.pos().distSqr(going) < 32 * 32))
                calls.add(new AllyCall(going, false, bot.tick));
        }
    }

    // ---------------------------------------------------------------- what the script did, in the same terms

    private JsonObject label() {
        JsonObject l = new JsonObject();
        for (String h : HEADS)
            l.addProperty(h, -1);
        Group m = bot.army.scriptMain();
        l.addProperty("mainOrder", m.order());
        l.addProperty("mainCell", cellOf(m.at()));
        Group s = bot.army.scriptSecond();
        int mainPop = BotMath.popOf(bot.army.main()), homePop = BotMath.popOf(bot.army.home());
        int sp = s == null ? 0 : homePop * 8 >= (mainPop + homePop) * 3 ? 2 : 1; // a quarter, or nearer half
        l.addProperty("secondSplit", sp);
        if (s != null) {
            l.addProperty("secondOrder", s.order());
            l.addProperty("secondCell", cellOf(s.at()));
        }
        for (int k = 0; k < 2; k++) {
            LivingEntity h = hero(k);
            if (h != null)
                l.addProperty("hero" + k + "Order", bot.unit(h).fleeing ? HERO_HOME : FOLLOW);
        }
        BlockPos raidAt = bot.army.raid.target();
        l.addProperty("raidOrder", bot.army.raid.isOut() && raidAt != null ? OUT : HOME);
        if (bot.army.raid.isOut() && raidAt != null)
            l.addProperty("raidCell", cellOf(raidAt));
        LivingEntity sc = netScout();
        if (sc != null) {
            BlockPos t = bot.unit(sc).scoutTarget;
            l.addProperty("scoutOrder", t != null ? OUT : HOME);
            if (t != null)
                l.addProperty("scoutCell", cellOf(t));
        }
        int b = builtNow == null ? -1 : buildingNames().indexOf(key(builtNow));
        l.addProperty("build", b + 1); // not one of the net's buildings (a house, a farm): nothing
        if (b >= 0)
            l.addProperty("buildCell", cellOf(builtAt));
        l.addProperty("research", researchedNow == null ? 0 : researchNames().indexOf(name(researchedNow)) + 1);
        l.addProperty("workers", bot.economy.wantsWorkers() ? 1 : 0);
        BotPlan.Shares sh = bot.economy.scriptShares();
        int best = 0;
        for (int i = 1; i < GATHER.size(); i++)
            if (distance(GATHER.get(i), sh) < distance(GATHER.get(best), sh))
                best = i;
        l.addProperty("gather", best);
        ProductionItem next = bot.production.scriptChoice();
        l.addProperty("unit", next == null ? -1 : UNITS.indexOf(name(next)));
        return l;
    }

    private int cellOf(@Nullable BlockPos p) {
        return p == null ? -1 : grid.cell(p);
    }

    private static double distance(BotPlan.Shares a, BotPlan.Shares b) {
        return Math.abs(a.food() - b.food()) + Math.abs(a.wood() - b.wood()) + Math.abs(a.ore() - b.ore());
    }

    // ---------------------------------------------------------------- the global numbers

    private static List<String> inputNames() {
        List<String> n = new ArrayList<>(List.of("minutes", "food", "wood", "ore", "emerald", "workers", "pop", "supply",
                "gatherFood", "gatherWood", "gatherOre", "army", "main", "home", "raid",
                "rally", "attack", "retreat", "defend", "support", "theirArmy", "theirArmyAgeMin", "theirArmyKnown",
                "simMain", "simAll", "threatPop", "defending", "theirBuildings", "buildings", "outposts", "buildingTypes",
                "maxed", "crushing", "waves", "saving", "nowHold", "nowAttack", "nowRetreat",
                "allies", "allyArmy", "allyArmyFromUs", "allyNeedsHelp", "allyAttacking"));
        for (UnitRole r : ROLES)
            n.add("our" + r.name());
        for (UnitRole r : ROLES)
            n.add("their" + r.name());
        return List.copyOf(n);
    }

    private double[] global() {
        List<LivingEntity> main = bot.army.main();
        double[] g = new double[INPUTS.size()];
        int i = 0;
        Map<String, Integer> gatherers = bot.economy.gatherers();
        List<LivingEntity> theirs = bot.memory.recentEnemyArmy();
        List<CombatSim.Fighter> known = bot.memory.recentEnemyFighters();
        g[i++] = bot.minutesPlayed() / 45.0;
        g[i++] = bot.resources.food / 1000.0;
        g[i++] = bot.resources.wood / 1000.0;
        g[i++] = bot.resources.ore / 1000.0;
        g[i++] = bot.resources.emerald / 1000.0;
        g[i++] = bot.workers.size() / 40.0;
        g[i++] = bot.getPop() / 150.0;
        g[i++] = bot.getSupply() / 150.0;
        g[i++] = gatherers.getOrDefault("F", 0) / 30.0;
        g[i++] = gatherers.getOrDefault("W", 0) / 30.0;
        g[i++] = gatherers.getOrDefault("O", 0) / 30.0;
        g[i++] = BotMath.popOf(bot.fighters) / 100.0;
        g[i++] = BotMath.popOf(main) / 100.0;
        g[i++] = BotMath.popOf(bot.army.home()) / 100.0;
        g[i++] = BotMath.popOf(bot.army.raid()) / 100.0;
        for (BotArmy.State s : BotArmy.State.values())
            g[i++] = bot.army.state == s ? 1 : 0;
        g[i++] = BotMath.popOf(theirs) / 100.0;
        g[i++] = Math.min(5, (bot.tick - enemyArmySeenTick) / 1200.0) / 5;
        g[i++] = known.isEmpty() ? 0 : 1;
        g[i++] = known.isEmpty() ? 0 : CombatSim.fight(BotFighters.of(main, true), known).margin();
        g[i++] = known.isEmpty() ? 0 : CombatSim.fight(BotFighters.of(bot.fighters, true), known).margin();
        g[i++] = BotMath.popOf(bot.defence.threats) / 50.0;
        g[i++] = bot.defence.isDefending() ? 1 : 0;
        g[i++] = bot.memory.enemyBuildings.size() / 30.0;
        g[i++] = bot.buildings.size() / 50.0;
        g[i++] = bot.expansion.outposts() / 5.0;
        g[i++] = bot.buildings.stream().filter(b -> b.isBuilt).map(b -> b.getBuilding().name).distinct().count() / 15.0;
        g[i++] = bot.army.maxed() ? 1 : 0;
        g[i++] = bot.army.isCrushing() ? 1 : 0;
        g[i++] = bot.army.wavesSent() / 10.0;
        g[i++] = bot.reserve != null ? 1 : 0;
        g[i++] = main().order() == HOLD || main().order() == MOVE ? 1 : 0;
        g[i++] = main().order() == ATTACK ? 1 : 0;
        g[i++] = main().order() == RETREAT ? 1 : 0;
        List<LivingEntity> allyArmy = bot.memory.allyArmy();
        g[i++] = AlliancesServerEvents.getAllAllies(bot.name).size() / 3.0;
        g[i++] = BotMath.popOf(allyArmy) / 100.0;
        g[i++] = allyArmy.isEmpty() ? 0 : Math.sqrt(BotMath.centroid(allyArmy).distSqr(main.isEmpty() ? bot.home : BotMath.centroid(main))) / 300;
        g[i++] = calls.stream().anyMatch(AllyCall::help) ? 1 : 0;
        g[i++] = calls.stream().anyMatch(c -> !c.help()) ? 1 : 0;
        for (UnitRole r : ROLES)
            g[i++] = rolePop(bot.fighters, r) / 50.0;
        for (UnitRole r : ROLES)
            g[i++] = rolePop(theirs, r) / 50.0;
        return g;
    }

    // the main army's order now: the net's when it plays, else what the script is doing
    private Group main() {
        return net != null ? main : bot.army.scriptMain();
    }

    private static int rolePop(List<LivingEntity> units, UnitRole role) {
        return BotMath.popOf(units.stream().filter(e -> e instanceof Unit u && u.getUnitRoles().contains(role)).toList());
    }

    private static boolean isArmy(LivingEntity e) {
        return e instanceof Unit u && !u.getOwnerName().isBlank() && !u.getUnitRoles().contains(UnitRole.WORKER) &&
                !u.getUnitRoles().contains(UnitRole.SCOUT);
    }

    @Nullable
    private ProductionItem item(String name) {
        for (BotPlan.ArmyUnit a : bot.matchup.army())
            if (name(a.item()).equals(name))
                return a.item();
        return null;
    }

    // ---------------------------------------------------------------- the trainer's log

    private static final Map<String, BufferedWriter> logs = new HashMap<>();

    private void write(double[] global, Masks m, JsonObject label, @Nullable JsonObject act, double lp, @Nullable BotGridNet.Output out) {
        try {
            BufferedWriter w = logs.get(bot.name);
            if (w == null) {
                Path dir = ArenaServerEvents.decisionsDir();
                Files.createDirectories(dir);
                w = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(dir.resolve(bot.name + ".jsonl.gz"))), StandardCharsets.UTF_8));
                logs.put(bot.name, w);
                JsonObject header = new JsonObject();
                header.addProperty("format", "grid-1");
                header.addProperty("bot", bot.name);
                header.addProperty("net", net != null);
                header.addProperty("team", ArenaServerEvents.team(bot.name));
                header.add("inputs", strings(INPUTS));
                header.add("buildings", strings(buildingNames()));
                header.add("research", strings(researchNames()));
                header.add("units", strings(UNITS));
                header.add("heroes", strings(heroNames()));
                header.add("bounds", bounds());
                w.write(header + "\n");
                grid.staticChanged = true; // the first decision line needs the terrain before it
            }
            if (grid.staticChanged) {
                JsonObject s = new JsonObject();
                s.addProperty("static", grid.encode(0, BotGrid.STATIC));
                s.addProperty("tick", bot.tick);
                w.write(s + "\n");
            }
            JsonObject d = new JsonObject();
            d.addProperty("tick", bot.tick);
            d.add("g", numbers(global));
            d.addProperty("grid", grid.encode(BotGrid.STATIC, BotGrid.LAYERS));
            d.addProperty("reach", BotGrid.bits(grid.reach));
            d.addProperty("buildable", BotGrid.bits(grid.buildable));
            JsonObject masks = new JsonObject();
            masks.add("build", flags(m.build()));
            masks.add("research", flags(m.research()));
            masks.add("unit", flags(m.unit()));
            masks.addProperty("hero0", m.hero0() ? 1 : 0);
            masks.addProperty("hero1", m.hero1() ? 1 : 0);
            d.add("masks", masks);
            d.add("label", label);
            d.add("stats", tally.json());
            if (act != null) {
                d.add("act", act);
                d.addProperty("lp", lp);
                d.addProperty("value", out.value());
            }
            w.write(d + "\n");
        } catch (IOException e) {
            ReignOfNether.LOGGER.error("[Bot] {} can't write its decisions", bot.name, e);
        }
    }

    // what the RoN-Bots Watch page shows: this second's view and choices, overwritten every second
    private void writeLive(double[] global, JsonObject label, @Nullable JsonObject act, @Nullable BotGridNet.Output out,
                           @Nullable JsonObject odds, Masks m) {
        JsonObject live = new JsonObject();
        live.addProperty("bot", bot.name);
        live.addProperty("tick", bot.tick);
        live.addProperty("net", net != null);
        live.addProperty("team", ArenaServerEvents.team(bot.name));
        live.add("bounds", bounds());
        JsonObject inputs = new JsonObject();
        for (int i = 0; i < INPUTS.size(); i++)
            inputs.addProperty(INPUTS.get(i), Math.round(global[i] * 1000) / 1000.0);
        live.add("inputs", inputs);
        live.add("home", xz(bot.home));
        live.addProperty("grid", grid.encode(0, BotGrid.LAYERS));
        JsonObject groups = new JsonObject();
        groups.add("main", group(bot.army.main()));
        groups.add("second", group(bot.army.home()));
        groups.add("raid", group(bot.army.raid()));
        for (int k = 0; k < 2; k++) {
            LivingEntity h = hero(k);
            groups.add("hero" + k, group(h == null ? List.of() : List.of(h)));
        }
        LivingEntity sc = netScout();
        groups.add("scout", group(sc == null ? List.of() : List.of(sc)));
        live.add("groups", groups);
        JsonArray ours = new JsonArray(), theirs = new JsonArray(), enemies = new JsonArray(), allyBuildings = new JsonArray(), allies = new JsonArray();
        bot.buildings.forEach(b -> ours.add(xz(b.centrePos)));
        bot.memory.enemyBuildings.values().forEach(sb -> theirs.add(xz(sb.pos())));
        bot.memory.recentEnemyArmy().forEach(e -> enemies.add(xz(bot.memory.lastSeenPos(e))));
        for (BuildingPlacement b : BuildingServerEvents.getBuildings())
            if (!b.isDestroyedServerside && !b.ownerName.equals(bot.name) && AlliancesServerEvents.isAllied(bot.name, b.ownerName))
                allyBuildings.add(xz(b.centrePos));
        bot.memory.allyArmy().forEach(e -> allies.add(xz(e.blockPosition())));
        live.add("buildings", ours);
        live.add("allyBuildings", allyBuildings);
        live.add("allies", allies);
        live.add("enemyBuildings", theirs);
        live.add("enemies", enemies);
        JsonObject names = new JsonObject();
        names.add("buildings", strings(buildingNames()));
        names.add("research", strings(researchNames()));
        names.add("units", strings(UNITS));
        names.add("heroes", strings(heroNames()));
        live.add("names", names);
        live.add("unitMask", flags(m.unit()));
        live.add("script", label);
        if (act != null) {
            live.add("choice", act);
            live.addProperty("value", out.value());
            live.add("odds", odds);
        }
        try {
            Path dir = ArenaServerEvents.decisionsDir(), tmp = dir.resolve(bot.name + ".live.tmp");
            Files.createDirectories(dir);
            Files.writeString(tmp, live.toString());
            Files.move(tmp, dir.resolve(bot.name + ".live.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            ReignOfNether.LOGGER.error("[Bot] {} can't write its live view", bot.name, e);
        }
    }

    private JsonObject bounds() {
        JsonObject b = new JsonObject();
        b.addProperty("minX", grid.minX);
        b.addProperty("minZ", grid.minZ);
        b.addProperty("size", grid.size);
        return b;
    }

    private static JsonObject group(List<LivingEntity> units) {
        JsonObject o = new JsonObject();
        o.add("at", units.isEmpty() ? null : xz(BotMath.centroid(units)));
        o.addProperty("pop", BotMath.popOf(units));
        return o;
    }

    private static JsonArray xz(BlockPos p) {
        JsonArray a = new JsonArray();
        a.add(p.getX());
        a.add(p.getZ());
        return a;
    }

    private static double[] softmax(double[] logits, @Nullable boolean[] mask) {
        double max = Double.NEGATIVE_INFINITY, sum = 0;
        for (int i = 0; i < logits.length; i++)
            if (mask == null || mask[i])
                max = Math.max(max, logits[i]);
        double[] p = new double[logits.length];
        for (int i = 0; i < logits.length; i++)
            if (mask == null || mask[i])
                sum += p[i] = Math.exp(logits[i] - max);
        for (int i = 0; i < p.length; i++)
            p[i] = sum > 0 ? p[i] / sum : 0;
        return p;
    }

    private static JsonArray strings(List<String> list) {
        JsonArray a = new JsonArray();
        list.forEach(a::add);
        return a;
    }

    private static JsonArray flags(boolean[] mask) {
        JsonArray a = new JsonArray();
        for (boolean m : mask)
            a.add(m ? 1 : 0);
        return a;
    }

    private static JsonArray numbers(double[] values) {
        JsonArray a = new JsonArray();
        for (double v : values)
            a.add(Math.round(v * 1000) / 1000.0);
        return a;
    }

    static void closeLogs() {
        for (BufferedWriter w : logs.values()) {
            try {
                w.close();
            } catch (IOException e) {
                ReignOfNether.LOGGER.error("[Bot] couldn't finish a decision log", e);
            }
        }
        logs.clear();
    }
}
