package com.solegendary.reignofnether.ai;

import com.google.gson.JsonObject;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.player.RTSPlayerScoresEnum;
import com.solegendary.reignofnether.unit.UnitRole;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

// What happened this game so far, counted up for the trainer's reward (RoN-Bots ppo.py): what our team killed, what
// we and our allies lost, buildings razed and lost, seconds stuck at the house cap. Counted from the real game, not
// from what the bot saw: a reward may know what the bot can't. Neutral camps don't count
class BotTally {

    private enum Side { US, ALLY, ENEMY }

    private record Seen(Side side, int pop, boolean worker, boolean hero, BlockPos at, String doing) {}

    private record Built(Side side, boolean capitol) {}

    private final BotBrain bot;
    private Map<Integer, Seen> units = new HashMap<>();
    private Map<BlockPos, Built> buildings = new HashMap<>();
    private int kills, lost, workersLost, heroesLost, allyLost, razed, buildingsLost, capitolsRazed, capped, held;

    BotTally(BotBrain bot) {
        this.bot = bot;
    }

    private Side side(String owner) {
        if (owner == null || owner.isBlank())
            return null;
        return owner.equals(bot.name) ? Side.US : AlliancesServerEvents.isAllied(bot.name, owner) ? Side.ALLY : Side.ENEMY;
    }

    // once a second: whoever was there last time and isn't now has died
    void update() {
        Map<Integer, Seen> now = new HashMap<>();
        for (LivingEntity e : UnitServerEvents.getAllUnits()) {
            Side s = e.isAlive() && e instanceof Unit u ? side(u.getOwnerName()) : null;
            if (s != null)
                now.put(e.getId(), new Seen(s, BotMath.popOf(e), BotBrain.has(e, UnitRole.WORKER), BotBrain.isHero(e), e.blockPosition(),
                        s == Side.US && e instanceof WorkerUnit w ? doing(w) : ""));
        }
        units.forEach((id, s) -> {
            if (now.containsKey(id))
                return;
            switch (s.side()) {
                case ENEMY -> kills += s.pop();
                case ALLY -> allyLost += s.pop();
                case US -> {
                    lost += s.pop();
                    workersLost += s.worker() ? 1 : 0;
                    if (s.worker()) // where and doing what: the net lost five times the script's workers
                        bot.log("tally: worker died {} from home, {}, {}", bot.fromHome(s.at()), s.doing(), bot.supervisor.controls() ? "net" : "script");
                    heroesLost += s.hero() ? 1 : 0;
                }
            }
        });
        units = now;
        Map<BlockPos, Built> standing = new HashMap<>();
        for (BuildingPlacement b : BuildingServerEvents.getBuildings()) {
            Side s = b.isDestroyedServerside ? null : side(b.ownerName);
            if (s != null && s != Side.ALLY)
                standing.put(b.originPos, new Built(s, b.isCapitol));
        }
        buildings.forEach((pos, b) -> {
            if (standing.containsKey(pos))
                return;
            if (b.side() == Side.ENEMY) {
                razed++;
                capitolsRazed += b.capitol() ? 1 : 0;
            } else {
                buildingsLost++;
            }
        });
        buildings = standing;
        if (bot.getPop() >= bot.getSupply() - 1 && bot.getSupply() < UnitServerEvents.maxPopulation)
            capped++;
        for (BuildingPlacement b : BuildingServerEvents.getBuildings()) // capture points we own, each second
            if (b.getBuilding().capturable && b.ownerName.equals(bot.name) && !b.isDestroyedServerside)
                held++;
    }

    private static String doing(WorkerUnit w) {
        if (w.getBuildRepairGoal() != null && w.getBuildRepairGoal().getBuildingTarget() != null)
            return "building";
        return w.getGatherResourceGoal() != null && w.getGatherResourceGoal().isGathering() ? "gathering " + w.getGatherResourceGoal().getTargetResourceName() : "idle";
    }

    JsonObject json() {
        JsonObject o = new JsonObject();
        o.addProperty("kills", kills);
        o.addProperty("lost", lost);
        o.addProperty("workersLost", workersLost);
        o.addProperty("heroesLost", heroesLost);
        o.addProperty("allyLost", allyLost);
        o.addProperty("razed", razed);
        o.addProperty("buildingsLost", buildingsLost);
        o.addProperty("capitolsRazed", capitolsRazed);
        o.addProperty("capped", capped);
        o.addProperty("held", held);
        RTSPlayer p = PlayerServerEvents.getRTSPlayer(bot.name);
        o.addProperty("harvested", p == null ? 0 : p.scores.getScoreList().getOrDefault(RTSPlayerScoresEnum.TOTAL_RESOURCES_HARVESTED, 0));
        o.addProperty("army", BotMath.popOf(bot.fighters));
        o.addProperty("bank", bot.resources.food + bot.resources.wood + bot.resources.ore);
        o.addProperty("explored", Math.round(bot.supervisor.grid.explored() * 1000) / 1000.0);
        o.addProperty("techTypes", bot.buildings.stream().filter(b -> b.isBuilt).map(b -> b.getBuilding().name).distinct().count());
        return o;
    }
}
