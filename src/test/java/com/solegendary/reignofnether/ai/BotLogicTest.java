package com.solegendary.reignofnether.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.UnitRole;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BotLogicTest {

    // ---------------------------------------------------------------- difficulty

    @Test
    void atMostFourMiners() {
        assertEquals(1, BotEconomy.minerCap(3));
        assertEquals(4, BotEconomy.minerCap(25));
    }

    @Test
    void mixedPatchBeatsBiggerForest() {
        assertTrue(BotExpansion.richness(40, 15, 0) > BotExpansion.richness(80, 0, 0));
        assertTrue(BotExpansion.richness(30, 0, 30) > BotExpansion.richness(40, 0, 0));
    }

    @Test
    void unknownDifficultyIsNormal() {
        assertEquals(BotDifficulty.NORMAL, BotDifficulty.fromOrdinal(-1));
    }

    @Test
    void harderBotsOnlyActFaster() {
        double prev = 0;
        for (BotDifficulty d : BotDifficulty.values()) {
            double perSecond = d.actionsPerThink() * 20 / d.ticksPerThink;
            assertEquals(d.actionsPerMinute / 60.0, perSecond, 1e-9);
            assertTrue(perSecond > prev);
            prev = perSecond;
        }
    }

    // ---------------------------------------------------------------- combat sim

    private static CombatSim.Fighter unit(double hp, double dps, double range, UnitRole... roles) {
        return new CombatSim.Fighter(hp, dps, range, 0, 0, 0, Set.of(roles));
    }

    private static CombatSim.Fighter vindicator() {
        return unit(65, 3, 2, UnitRole.FRONTLINE);
    }

    private static CombatSim.Fighter pillager() {
        return unit(45, 2.2, 16, UnitRole.RANGED, UnitRole.ANTI_AIR);
    }

    private static List<CombatSim.Fighter> many(int n, CombatSim.Fighter f) {
        return new ArrayList<>(Collections.nCopies(n, f));
    }

    @Test
    void biggerArmyWins() {
        assertTrue(CombatSim.fight(many(8, vindicator()), many(4, vindicator())).margin() > 0.3);
        assertTrue(CombatSim.fight(many(4, vindicator()), many(8, vindicator())).margin() < -0.3);
        assertEquals(0, CombatSim.fight(many(5, vindicator()), many(5, vindicator())).margin(), 0.2);
    }

    @Test
    void meleeCantFightFlyers() {
        CombatSim.Fighter ghast = unit(50, 1.2, 30, UnitRole.FLYER, UnitRole.RANGED, UnitRole.ANTI_AIR);
        assertEquals(0, CombatSim.dpsAgainst(vindicator(), ghast, 1));
        assertTrue(CombatSim.fight(many(8, vindicator()), many(2, ghast)).margin() < 0); // they can't touch it
        assertTrue(CombatSim.fight(many(8, pillager()), many(2, ghast)).margin() > 0);
    }

    @Test
    void harmingDoesNothingToUndead() {
        CombatSim.Fighter witch = unit(40, 3, 8, UnitRole.HEALER, UnitRole.CASTER, UnitRole.AOE, UnitRole.HARMING);
        CombatSim.Fighter zombie = unit(40, 1.8, 2, UnitRole.FRONTLINE, UnitRole.UNDEAD);
        assertEquals(0, CombatSim.dpsAgainst(witch, zombie, 1));
        assertTrue(CombatSim.dpsAgainst(witch, vindicator(), 1) > 0);
    }

    @Test
    void resistsAndSplashCount() {
        CombatSim.Fighter golem = new CombatSim.Fighter(120, 4, 3, 0, 0.4, 0, Set.of(UnitRole.FRONTLINE, UnitRole.SIEGE));
        assertEquals(2.2 * 0.6, CombatSim.dpsAgainst(pillager(), golem, 1), 1e-9); // ranged resist
        assertEquals(4, CombatSim.dpsAgainst(golem, pillager(), 1), 1e-9); // melee ignores it
        CombatSim.Fighter evoker = unit(40, 4, 10, UnitRole.CASTER, UnitRole.AOE);
        assertTrue(CombatSim.dpsAgainst(evoker, vindicator(), 8) > CombatSim.dpsAgainst(evoker, vindicator(), 2));
    }

    @Test
    void nobodyCanHurtAnybodyIsAStandoff() {
        CombatSim.Fighter bat = unit(15, 0, 2, UnitRole.FLYER, UnitRole.SCOUT);
        CombatSim.Result r = CombatSim.fight(many(3, vindicator()), many(3, bat));
        assertEquals(1, r.ourLeft(), 1e-9);
        assertEquals(1, r.theirLeft(), 1e-9);
    }

    @Test
    void emptySides() {
        assertEquals(1, CombatSim.fight(many(2, vindicator()), List.of()).margin(), 1e-9);
        assertEquals(-1, CombatSim.fight(List.of(), many(2, vindicator())).margin(), 1e-9);
    }

    @Test
    void fewestFindsTheSmallestWinningGroup() {
        assertEquals(3, BotMath.fewest(10, k -> k >= 3));
        assertEquals(0, BotMath.fewest(10, k -> true));
        assertEquals(-1, BotMath.fewest(10, k -> false));
        assertEquals(-1, BotMath.fewest(0, k -> k > 0));
        List<CombatSim.Fighter> theirs = many(4, vindicator());
        int n = BotMath.fewest(20, k -> CombatSim.fight(many(k, vindicator()), theirs).margin() >= 0.3);
        assertTrue(n > 4 && CombatSim.fight(many(n - 1, vindicator()), theirs).margin() < 0.3);
    }

    // every shipped plan has a status, and an enabled one has what BotPlan.parse needs (parse itself needs the
    // game's registries, so this only checks the keys: a typo shows up here instead of disabling the faction)
    @Test
    void shippedPlansHaveTheirKeys() throws Exception {
        for (String faction : List.of("villagers", "monsters", "piglins")) {
            try (var in = BotLogicTest.class.getResourceAsStream("/data/reignofnether/bot_plans/" + faction + ".json")) {
                assertNotNull(in, faction);
                JsonObject json = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
                assertTrue(json.has("status"), faction + " has a status");
                if (!"enabled".equals(json.get("status").getAsString()))
                    continue;
                for (String key : List.of("capitol", "worker", "scout", "supply", "farm", "drop_off", "hero_building",
                        "heroes", "workers", "waves", "gather", "matchups"))
                    assertTrue(json.has(key), faction + " has " + key);
                assertTrue(json.getAsJsonObject("matchups").has("default"), faction + " has a default matchup");
            }
        }
    }

    @Test
    void productionLeanIsANudge() {
        assertEquals(1, BotProduction.leanFor(0, 200), 1e-9);
        assertTrue(BotProduction.leanFor(0.2, 200) > 1);
        assertEquals(3, BotProduction.leanFor(5, 100), 1e-9); // capped
        assertEquals(0.5, BotProduction.leanFor(-5, 100), 1e-9);
    }

    // ---------------------------------------------------------------- economy

    @Test
    void workerTargetsFollowThePlan() {
        List<BotPlan.WorkerTarget> plan = List.of(new BotPlan.WorkerTarget(0, 15), new BotPlan.WorkerTarget(4, 20));
        assertEquals(15, BotEconomy.planTarget(plan, 0));
        assertEquals(15, BotEconomy.planTarget(plan, 3));
        assertEquals(20, BotEconomy.planTarget(plan, 4));
        assertEquals(15, BotEconomy.planTarget(List.of(), 9));
    }

    @Test
    void workersFollowTheArmy() {
        assertEquals(15, BotEconomy.armyCap(15, 0, 0.75)); // the opening always
        assertEquals(15, BotEconomy.armyCap(15, 12, 0.75));
        assertEquals(30, BotEconomy.armyCap(15, 40, 0.75)); // then 3 workers per 4 pop of army
    }

    @Test
    void gatherersMoveFromPilesToShortages() {
        assertArrayEquals(new ResourceName[]{ResourceName.FOOD, ResourceName.WOOD}, BotEconomy.rebalance(2500, 0, 1500));
        assertNull(BotEconomy.rebalance(350, 0, 300));
        assertNull(BotEconomy.rebalance(2500, 700, 900));
    }

    @Test
    void farmsGrowWithWorkers() {
        assertEquals(2, BotEconomy.farmsWanted(3, 0.3, false));
        assertEquals(5, BotEconomy.farmsWanted(15, 0.3, false));
        assertEquals(9, BotEconomy.farmsWanted(30, 0.3, false)); // one per food worker the plan wants
        assertEquals(11, BotEconomy.farmsWanted(30, 0.3, true)); // short of food: a few more
    }

    @Test
    void savingsOnlyHoldBackWhatAnItemSpends() {
        assertTrue(BotBrain.affordable(100, 0, 500)); // costs no ore: ore savings don't block it
        assertFalse(BotBrain.affordable(300, 100, 250));
        assertTrue(BotBrain.affordable(400, 100, 250));
    }

    @Test
    void longWalkAroundIsADetour() {
        assertFalse(BotBuilder.isDetour(30, 25));
        assertTrue(BotBuilder.isDetour(80, 30));
    }

    @Test
    void surrendersOnlyWhenTrulyBeaten() {
        assertFalse(BotBrain.beaten(true, 0, 40, 1, 20)); // still has its capitol
        assertTrue(BotBrain.beaten(false, 2, 40, 2, 20));
        assertFalse(BotBrain.beaten(false, 20, 40, 2, 20)); // still has an army
    }

    @Test
    void surrendersWithNoWayToGetWorkers() {
        assertTrue(BotBrain.noEconomy(0, false));
        assertFalse(BotBrain.noEconomy(0, true)); // can still train one
        assertFalse(BotBrain.noEconomy(3, false)); // still has workers
    }
}
