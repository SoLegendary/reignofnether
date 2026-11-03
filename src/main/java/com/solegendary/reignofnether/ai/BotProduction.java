package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Troops. Every free military building trains, and the build order's savings never hold it back: the economy is
// there for the army. What to train is the matchup's mix (BotPlan), leaned towards whatever the combat sim says
// helps most against the enemy army we've seen, per resource spent.
public class BotProduction {

    private static final int RICH_RAVAGERS = 12, TECH_FIRST_ARMY = 20, WOOD_FOR_EXPANSION = 225;
    static final int MAX_QUEUE = 2; // per production building, for every queue the bot fills
    private static final int SIM_EVERY = 5 * 20; // the sim's per-unit verdict is refreshed this often
    private static final double MIN_LEAN = 0.5, MAX_LEAN = 3;

    private final BotBrain bot;
    private final Map<ProductionItem, Double> lean = new HashMap<>(); // unit -> how much the sim wants it (1 = neutral)
    private long leanTick = -1_000_000;
    private static final Map<ProductionItem, Optional<Building>> producers = new HashMap<>();

    BotProduction(BotBrain bot) {
        this.bot = bot;
    }

    void think() {
        if (bot.tick - leanTick >= SIM_EVERY) {
            leanTick = bot.tick;
            updateLean();
        }
        Map<ProductionItem, Integer> have = counts();
        for (BuildingPlacement b : bot.buildings) {
            if (!(b instanceof ProductionPlacement pp) || !b.isBuilt || b.isCapitol || pp.productionQueue.size() >= MAX_QUEUE)
                continue;
            BotPlan.ArmyUnit best = bot.supervisor.controls() ? netPick(b, have) : scriptPick(b, have);
            if (best != null && bot.startProduction(pp, best.item())) // re-reads resources for the next building
                have.merge(best.item(), 1, Integer::sum);
        }
    }

    // the unit furthest below its share of the mix
    @Nullable
    private BotPlan.ArmyUnit scriptPick(BuildingPlacement b, Map<ProductionItem, Integer> have) {
        BotPlan.ArmyUnit best = null;
        double bestScore = Double.MAX_VALUE;
        for (BotPlan.ArmyUnit a : bot.matchup.army()) {
            if (!canTrain(b, a, have))
                continue;
            double score = (have.getOrDefault(a.item(), 0) + 1) / (a.weight() * lean.getOrDefault(a.item(), 1.0));
            if (score < bestScore) {
                bestScore = score;
                best = a;
            }
        }
        return best;
    }

    // the supervisor's most wanted unit that this building trains and we can pay for
    @Nullable
    private BotPlan.ArmyUnit netPick(BuildingPlacement b, Map<ProductionItem, Integer> have) {
        for (ProductionItem item : bot.supervisor.units())
            for (BotPlan.ArmyUnit a : bot.matchup.army())
                if (a.item() == item && canTrain(b, a, have))
                    return a;
        return null;
    }

    private boolean canTrain(BuildingPlacement b, BotPlan.ArmyUnit a, Map<ProductionItem, Integer> have) {
        return BotBrain.makes(b, a.item()) && unlocked(a, have) && affordable(a.item().getCost(false, bot.name)) && !spendsLastWood(a.item());
    }

    // trained somewhere we have, its building or research done, below its cap: what the supervisor may ask for
    boolean unlocked(BotPlan.ArmyUnit a, Map<ProductionItem, Integer> have) {
        Building producer = producerOf(a.item());
        return producer != null && bot.hasFinished(producer) && have.getOrDefault(a.item(), 0) < cap(a) &&
                (a.needs() == null || bot.hasFinished(a.needs()));
    }

    // what the script would train next at the first free military building, for the supervisor to learn from
    @Nullable
    ProductionItem scriptChoice() {
        Map<ProductionItem, Integer> have = counts();
        for (BuildingPlacement b : bot.buildings)
            if (b instanceof ProductionPlacement pp && b.isBuilt && !b.isCapitol && pp.productionQueue.size() < MAX_QUEUE) {
                BotPlan.ArmyUnit a = scriptPick(b, have);
                if (a != null)
                    return a.item();
            }
        return null;
    }

    // past a small army the build order's wood and ore savings hold back troops that need them; its food never does:
    // a 400-food research held up the vindicators and even the ravagers it was for
    private boolean affordable(ResourceCost c) {
        ResourceCost r = bot.reserve;
        if (!bot.canAfford(c, false))
            return false;
        return !techFirst() || r == null || (BotBrain.affordable(bot.resources.wood, c.wood, r.wood) && BotBrain.affordable(bot.resources.ore, c.ore, r.ore));
    }

    private boolean spendsLastWood(ProductionItem item) {
        ResourceCost c = item.getCost(false, bot.name);
        // wood running out: the next stockpile comes before more troops, not after the last tree
        int keep = bot.expansion.woodRunningLow() ? WOOD_FOR_EXPANSION : BotEconomy.BROKE_WOOD;
        return c.wood > 0 && bot.resources.wood - c.wood < keep;
    }

    // left to compete with troops, the Witch Hut waited 14 minutes and the Castle never got paid for
    private boolean techFirst() {
        return BotMath.popOf(bot.fighters) >= TECH_FIRST_ARMY;
    }

    // artillery is what breaks a base, and a full bank has nothing better to buy
    private int cap(BotPlan.ArmyUnit a) {
        return a.item() == ProductionItems.RAVAGER && bot.rich() ? RICH_RAVAGERS : a.cap();
    }

    // alive and in production, per item
    Map<ProductionItem, Integer> counts() {
        Map<EntityType<?>, Integer> byType = new HashMap<>();
        for (LivingEntity e : bot.fighters)
            byType.merge(e.getType(), 1, Integer::sum);
        Map<ProductionItem, Integer> counts = new HashMap<>();
        for (BotPlan.ArmyUnit a : bot.matchup.army()) {
            EntityType<?> type = Factions.unitTypeOf(a.item());
            counts.put(a.item(), type == null ? 0 : byType.getOrDefault(type, 0));
        }
        for (BuildingPlacement b : bot.buildings)
            if (b instanceof ProductionPlacement pp)
                for (ActiveProduction ap : pp.productionQueue)
                    counts.computeIfPresent(ap.item, (k, n) -> n + 1);
        return counts;
    }

    // for each unit of the mix: how much one more of it improves the fight against the enemy army we've seen,
    // per 100 resources. With nothing seen yet the mix stays as the plan has it
    private void updateLean() {
        lean.clear();
        List<CombatSim.Fighter> theirs = bot.memory.recentEnemyFighters();
        if (theirs.isEmpty())
            return;
        List<CombatSim.Fighter> ours = BotFighters.of(bot.fighters, true);
        double base = CombatSim.fight(ours, theirs).margin();
        for (BotPlan.ArmyUnit a : bot.matchup.army()) {
            CombatSim.Fighter extra = BotFighters.of(bot.level, a.item());
            if (extra == null)
                continue;
            List<CombatSim.Fighter> with = new ArrayList<>(ours);
            with.add(extra);
            double gain = CombatSim.fight(with, theirs).margin() - base;
            lean.put(a.item(), leanFor(gain, totalCost(a.item().getCost(false, bot.name))));
        }
    }

    // 1 + 3x the margin gained per 100 resources, kept to a nudge: the mix still decides most of the army
    static double leanFor(double marginGain, int cost) {
        return Math.max(MIN_LEAN, Math.min(MAX_LEAN, 1 + 3 * marginGain * 100 / Math.max(50, cost)));
    }

    private static int totalCost(ResourceCost c) {
        return c.food + c.wood + c.ore;
    }

    // ---------------------------------------------------------------- producers

    // the building that trains the matchup's first unit (the barracks for villagers)
    @Nullable
    Building mainProducer() {
        List<BotPlan.ArmyUnit> army = bot.matchup.army();
        return army.isEmpty() ? null : producerOf(army.get(0).item());
    }

    boolean hasMilitaryProducer() {
        for (BotPlan.ArmyUnit a : bot.matchup.army()) {
            Building p = producerOf(a.item());
            if (p != null && bot.hasFinished(p))
                return true;
        }
        return false;
    }

    // which building trains an item, looked up once in the building registry
    @Nullable
    static Building producerOf(ProductionItem item) {
        return producers.computeIfAbsent(item, i -> {
            for (Building b : ReignOfNetherRegistries.BUILDING)
                if (b instanceof ProductionBuilding pb && pb.productions.get().contains(i))
                    return Optional.of(b);
            return Optional.empty();
        }).orElse(null);
    }
}
