package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.buildings.neutral.Beacon;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.faction.Faction;

import java.util.ArrayList;
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
