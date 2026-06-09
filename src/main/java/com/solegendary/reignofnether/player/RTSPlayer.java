package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.buildings.neutral.Beacon;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.building.buildings.shared.Market;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.resources.ResourceName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.solegendary.reignofnether.player.PlayerServerEvents.TICKS_TO_REVEAL;

public class RTSPlayer {
    public String name;
    public int id; // for AI, always negative
    public int ticksWithoutCapitol = 0;
    public Faction faction;
    public int beaconOwnerTicks = 0; // ticks owning a beacon - will win upon reaching
    public int startPosColorId = 0;
    public RTSPlayerScores scores = new RTSPlayerScores();
    public int scenarioRoleIndex = -1;

    // Market trade rates, fixed order: [F->W, F->O, W->F, W->O, O->F, O->W].
    // Each trade spends Market.TRADE_CHUNK of source for `rate` of target; the rate decreases by
    // Market.RATE_STEP per same-direction trade (floor Market.MIN_RATE) and the opposite rises by the same.
    public int[] tradeRates = defaultTradeRates();

    public static int[] defaultTradeRates() {
        int[] rates = new int[6];
        Arrays.fill(rates, Market.START_RATE);
        return rates;
    }

    // -1 if from==to or any NONE
    public static int tradeIndex(ResourceName from, ResourceName to) {
        switch (from) {
            case FOOD:
                if (to == ResourceName.WOOD) return 0;
                if (to == ResourceName.ORE) return 1;
                break;
            case WOOD:
                if (to == ResourceName.FOOD) return 2;
                if (to == ResourceName.ORE) return 3;
                break;
            case ORE:
                if (to == ResourceName.FOOD) return 4;
                if (to == ResourceName.WOOD) return 5;
                break;
            default:
                break;
        }
        return -1;
    }

    public int getTradeRate(ResourceName from, ResourceName to) {
        int i = tradeIndex(from, to);
        return i < 0 ? 0 : tradeRates[i];
    }

    public void applyTrade(ResourceName from, ResourceName to) {
        int i = tradeIndex(from, to);
        int j = tradeIndex(to, from);
        if (i < 0 || j < 0) return;
        tradeRates[i] = Math.max(Market.MIN_RATE, tradeRates[i] - Market.RATE_STEP);
        tradeRates[j] = tradeRates[j] + Market.RATE_STEP;
    }

    private RTSPlayer(String playerName, Faction faction, int id) {
        this.name = playerName;
        this.id = id;
        this.faction = faction;
    }

    private RTSPlayer(String playerName, Faction faction, int id, int startPosColorId) {
        this.name = playerName;
        this.id = id;
        this.faction = faction;
        this.startPosColorId = startPosColorId;
    }

    // bot
    private RTSPlayer(String name, Faction faction) {
        int minId = Integer.MAX_VALUE;
        if (!PlayerServerEvents.rtsPlayers.isEmpty()) {
            for (RTSPlayer r : PlayerServerEvents.rtsPlayers) {
                minId = Math.min(r.id, minId);
            }
        }
        if (minId >= 0) {
            this.id = -1;
        } else {
            this.id = minId - 1;
        }
        this.faction = faction;
        this.name = name;
    }

    private RTSPlayer(String name, int id, int ticksWithoutCapitol, Faction faction, int beaconOwnerTicks, int[] scores, int scenarioRoleIndex) {
        this.name = name;
        this.id = id;
        this.ticksWithoutCapitol = ticksWithoutCapitol;
        this.faction = faction;
        this.beaconOwnerTicks = beaconOwnerTicks;
        this.scores.setScoreListFromArray(scores);
        this.scenarioRoleIndex = scenarioRoleIndex;
    }

    public static RTSPlayer getFromSave(String name, int id, int ticksWithoutCapitol, Faction faction, int beaconOwnerTicks, int[] scores, int scenarioRoleIndex) {
        return new RTSPlayer(name, id, ticksWithoutCapitol, faction, beaconOwnerTicks, scores, scenarioRoleIndex);
    }

    public static RTSPlayer getNewPlayer(String playerName, Faction faction, int id) {
        return new RTSPlayer(playerName, faction, id);
    }

    public static RTSPlayer getNewPlayer(String playerName, Faction faction, int id, int startPosColorId) {
        return new RTSPlayer(playerName, faction, id, startPosColorId);
    }

    public static RTSPlayer getNewScenarioPlayer(String playerName, Faction faction, int id, int scenarioRoleIndex) {
        RTSPlayer rtsPlayer = new RTSPlayer(playerName, faction, id);
        rtsPlayer.scenarioRoleIndex = scenarioRoleIndex;
        return rtsPlayer;
    }

    public static RTSPlayer getNewBot(String name, Faction faction) {
        return new RTSPlayer(name, faction);
    }

    public boolean isBot() {
        return id < 0;
    }

    public void serverTick() {
        int numBuildingsOwned = 0;
        for (BuildingPlacement buildingPlacement : BuildingServerEvents.getBuildings()) {
            if (buildingPlacement.ownerName.equals(this.name)) numBuildingsOwned++;
        }
        int numCapitolsOwned = 0;
        for (BuildingPlacement b : BuildingServerEvents.getBuildings()) {
            if (b.ownerName.equals(this.name) && b.isCapitol) numCapitolsOwned++;
        }

        if (numBuildingsOwned > 0 && numCapitolsOwned == 0) {
            if (ticksWithoutCapitol < TICKS_TO_REVEAL) {
                this.ticksWithoutCapitol += 1;
                if (ticksWithoutCapitol == TICKS_TO_REVEAL) {
                    if (FogOfWarServerEvents.isEnabled()) {
                        PlayerServerEvents.sendMessageToAllPlayers("server.reignofnether.revealed", false, this.name);
                    }
                    FogOfWarClientboundPacket.revealOrHidePlayer(true, this.name);
                }
            }
        } else {
            this.ticksWithoutCapitol = 0;
        }

        for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
            if (building instanceof BeaconPlacement beacon && beacon.isBuilt && building.ownerName.equals(this.name)) {
                if (beacon.getUpgradeLevel() == Beacon.MAX_UPGRADE_LEVEL) {
                    beaconOwnerTicks += 1;
                    if (beaconOwnerTicks == Beacon.getTicksToWin(beacon.getLevel()) / 4 ||
                            beaconOwnerTicks == Beacon.getTicksToWin(beacon.getLevel()) / 2 ||
                            beaconOwnerTicks == (Beacon.getTicksToWin(beacon.getLevel()) * 3) / 4 ||
                            beaconOwnerTicks == Beacon.getTicksToWin(beacon.getLevel()) - 1200)
                        beacon.sendWarning("time_warning");
                }
            }
        }
    }

    public boolean isRevealed() {
        return this.ticksWithoutCapitol >= TICKS_TO_REVEAL;
    }
}
