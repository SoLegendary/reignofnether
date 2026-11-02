package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItem;
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
            BotPlan.ArmyUnit best = null;
            double bestScore = Double.MAX_VALUE;
            for (BotPlan.ArmyUnit a : bot.matchup.army()) {
                int count = have.getOrDefault(a.item(), 0);
                if (!BotBrain.makes(b, a.item()) || count >= a.cap() || (a.needs() != null && !bot.hasFinished(a.needs())) ||
                        !bot.canAfford(a.item().getCost(false, bot.name), false))
                    continue;
                // the unit furthest below its share of the mix goes next
                double score = (count + 1) / (a.weight() * lean.getOrDefault(a.item(), 1.0));
                if (score < bestScore) {
                    bestScore = score;
                    best = a;
                }
            }
            if (best != null && bot.startProduction(pp, best.item())) // re-reads resources for the next building
                have.merge(best.item(), 1, Integer::sum);
        }
    }

    // alive and in production, per item
    private Map<ProductionItem, Integer> counts() {
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
