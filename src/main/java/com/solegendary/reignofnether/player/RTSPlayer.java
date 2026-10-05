package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.ability.TradeAction;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.buildings.neutral.Beacon;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.scenario.ScenarioUtils;

import java.util.*;

import static com.solegendary.reignofnether.ability.TradeAction.*;
import static com.solegendary.reignofnether.ability.abilities.TradeResources.*;
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
    public Map<TradeAction, Integer> tradeRates = new HashMap<>();
    public boolean isDogPerson = true;
    public int itemsDropped = 0;
    public int creepScore = 0; // value of neutral enemies killed, for credit towards item drops
    public Long itemSeed = 0L;
    public ArrayDeque<UnitItem> itemDropQueue = new ArrayDeque<>();

    public static RTSPlayer getNewScenarioPlayer(String playerName, Faction faction, int id, int scenarioRoleIndex) {
        RTSPlayer rtsPlayer = new RTSPlayer(playerName, faction, id);
        rtsPlayer.scenarioRoleIndex = scenarioRoleIndex;
        return rtsPlayer;
    }

    private RTSPlayer(String playerName, Faction faction, int id) {
        this.name = playerName;
        this.id = id;
        this.faction = faction;
        initTradeRates();
    }

    public static RTSPlayer getNewPlayer(String playerName, Faction faction, int id, int startPosColorId, boolean isDogPerson, Long itemSeed) {
        return new RTSPlayer(playerName, faction, id, startPosColorId, isDogPerson, itemSeed);
    }

    private RTSPlayer(String playerName, Faction faction, int id, int startPosColorId, boolean isDogPerson, Long itemSeed) {
        this.name = playerName;
        this.id = id;
        this.faction = faction;
        this.startPosColorId = startPosColorId;
        this.isDogPerson = isDogPerson;
        initTradeRates();
        if (itemSeed >= 0)
            this.itemDropQueue = ItemUtil.getRandomItemDropsList(itemSeed);
    }

    public static RTSPlayer getNewBot(String name, Faction faction) {
        return new RTSPlayer(name, faction);
    }

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
        initTradeRates();
    }

    public static RTSPlayer getFromSave(String name, int id, int ticksWithoutCapitol, Faction faction, int beaconOwnerTicks,
                                        int[] scores, int scenarioRoleIndex, Map<TradeAction, Integer> tradeRates,
                                        int creepScore, int itemsDropped, Long itemDropSeed) {
        return new RTSPlayer(
                name, id, ticksWithoutCapitol, faction, beaconOwnerTicks, scores,
                scenarioRoleIndex, tradeRates, creepScore, itemsDropped, itemDropSeed
        );
    }

    private RTSPlayer(String name, int id, int ticksWithoutCapitol, Faction faction, int beaconOwnerTicks,
                      int[] scores, int scenarioRoleIndex, Map<TradeAction, Integer> tradeRates,
                      int creepScore, int itemsDropped, Long itemSeed) {
        this.name = name;
        this.id = id;
        this.ticksWithoutCapitol = ticksWithoutCapitol;
        this.faction = faction;
        this.beaconOwnerTicks = beaconOwnerTicks;
        this.scores.setScoreListFromArray(scores);
        this.scenarioRoleIndex = scenarioRoleIndex;
        this.tradeRates = tradeRates;
        this.creepScore = creepScore;
        this.itemsDropped = itemsDropped;
        this.itemSeed = itemSeed;
        this.itemDropQueue = ItemUtil.getRandomItemDropsList(itemSeed);
        for (int i = 0; i < itemsDropped; i++)
            this.itemDropQueue.pollFirst();
    }

    private void initTradeRates() {
        tradeRates.put(FOOD_FOR_EMERALD, START_SELL_RATE);
        tradeRates.put(WOOD_FOR_EMERALD, START_SELL_RATE);
        tradeRates.put(ORE_FOR_EMERALD, START_SELL_RATE);
        tradeRates.put(EMERALD_FOR_FOOD, START_BUY_RATE);
        tradeRates.put(EMERALD_FOR_WOOD, START_BUY_RATE);
        tradeRates.put(EMERALD_FOR_ORE, START_BUY_RATE);
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
                    if (!ScenarioUtils.isScenarioNpc(false, scenarioRoleIndex))
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
