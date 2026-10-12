package com.solegendary.reignofnether.gamerules;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.gamemode.ClientGameModeHelper;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.items.RandomItemDropRule;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.neoforged.neoforge.network.PacketDistributor;
import com.solegendary.reignofnether.gamerules.GameruleClientboundPacket;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.gamerules.GameruleAction;

public class GameruleClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<GameruleClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "gamerule_clientbound"));

    @Override
    public CustomPacketPayload.Type<GameruleClientboundPacket> type() {
        return TYPE;
    }

    GameruleAction action;
    String playerName;
    Long value;

    public static void setLogFalling(boolean logFalling) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_LOG_FALLING, "", logFalling ? 1L : 0L));
    }
    public static void setNeutralAggro(boolean neutralAggro) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_NEUTRAL_AGGRO, "", neutralAggro ? 1L : 0L));
    }
    public static void setMaxPopulation(long maxPopulation) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_MAX_POPULATION, "", maxPopulation));
    }
    public static void setUnitGriefing(boolean unitGriefing) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_UNIT_GRIEFING, "", unitGriefing ? 1L : 0L));
    }
    public static void setPlayerGriefing(boolean playerGriefing) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_PLAYER_GRIEFING, "", playerGriefing ? 1L : 0L));
    }
    public static void setGroundYLevel(long groundYLevel) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_GROUND_Y_LEVEL, "", groundYLevel));
    }
    public static void setFlyingMaxYLevel(long flyingMaxYLevel) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_FLYING_MAX_Y_LEVEL, "", flyingMaxYLevel));
    }
    public static void setAllowBeacons(boolean allowBeacons) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_ALLOW_BEACONS, "", allowBeacons ? 1L : 0L));
    }
    public static void setPvpModesOnly(boolean pvpModesOnly) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_PVP_MODES_ONLY, "", pvpModesOnly ? 1L : 0L));
    }
    public static void setBeaconWinMinutes(long beaconWinMinutes) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_BEACON_WIN_MINUTES, "", beaconWinMinutes));
    }
    public static void setSlantedBuilding(boolean slantedBuilding) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_SLANTED_BUILDING, "", slantedBuilding ? 1L : 0L));
    }
    public static void setAllowedHeroes(long allowedHeroes) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_ALLOWED_HEROES, "", allowedHeroes));
    }
    public static void setLockAlliances(boolean lockAlliances) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_LOCK_ALLIANCES, "", lockAlliances ? 1L : 0L));
    }
    public static void setScenarioMode(boolean scenarioMode) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_SCENARIO_MODE, "", scenarioMode ? 1L : 0L));
    }
    public static void setCoopMode(boolean coopMode) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_COOP_MODE, "", coopMode ? 1L : 0L));
    }
    public static void setBuildingsOutsideBorder(boolean buildingsOutsideBorder) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_BUILDINGS_OUTSIDE_BORDER, "", buildingsOutsideBorder ? 1L : 0L));
    }
    public static void setRtsPathfinding(boolean rtsPathfinding) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_RTS_PATHFINDING, "", rtsPathfinding ? 1L : 0L));
    }
    public static void setAnimalSpawnYDiff(long yDiff) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_ANIMAL_SPAWN_Y_DIFF, "", yDiff));
    }
    public static void setRandomItemDrops(long value) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_RANDOM_ITEM_DROPS, "", value));
    }

    public GameruleClientboundPacket(GameruleAction action, String playerName, Long value) {
        this.action = action;
        this.playerName = playerName;
        this.value = value;
    }

    public GameruleClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(GameruleAction.class);
        this.playerName = buffer.readUtf();
        this.value = buffer.readLong();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.playerName);
        buffer.writeLong(this.value);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (action) {
                            case SET_LOG_FALLING -> GameruleClient.doLogFalling = value == 1L;
                            case SET_NEUTRAL_AGGRO -> GameruleClient.neutralAggro = value == 1L;
                            case SET_MAX_POPULATION -> GameruleClient.maxPopulation = Math.toIntExact(value);
                            case SET_UNIT_GRIEFING -> GameruleClient.doUnitGriefing = value == 1L;
                            case SET_PLAYER_GRIEFING -> GameruleClient.doPlayerGriefing = value == 1L;
                            case SET_GROUND_Y_LEVEL -> {
                                GameruleClient.groundYLevel = value;
                                OrthoviewClientEvents.setMinOrthoviewY(value + 30);
                            }
                            case SET_FLYING_MAX_Y_LEVEL -> GameruleClient.flyingMaxYLevel = value;
                            case SET_ALLOW_BEACONS -> GameruleClient.allowBeacons = value == 1L;
                            case SET_PVP_MODES_ONLY -> {
                                GameruleClient.pvpModesOnly = value == 1L;
                                if (GameruleClient.pvpModesOnly) {
                                    ClientGameModeHelper.gameMode = GameMode.CLASSIC;
                                }
                            }
                            case SET_BEACON_WIN_MINUTES -> GameruleClient.beaconWinMinutes = value;
                            case SET_SLANTED_BUILDING -> GameruleClient.slantedBuilding = value == 1L;
                            case SET_ALLOWED_HEROES -> {
                                GameruleClient.allowedHeroes = Math.toIntExact(value);
                                for (BuildingPlacement buildingPlacement : BuildingClientEvents.getBuildings()) {
                                    if (buildingPlacement instanceof ProductionPlacement pp) {
                                        pp.updateButtons();
                                    }
                                }
                            }
                            case SET_LOCK_ALLIANCES -> GameruleClient.lockAlliances = value == 1L;
                            case SET_SCENARIO_MODE -> GameruleClient.scenarioMode = value == 1L;
                            case SET_COOP_MODE -> GameruleClient.coopMode = value == 1L;
                            case SET_BUILDINGS_OUTSIDE_BORDER -> GameruleClient.buildingsOutsideBorder = value == 1L;
                            case SET_RTS_PATHFINDING -> GameruleClient.rtsPathfinding = value == 1L;
                            case SET_ANIMAL_SPAWN_Y_DIFF -> GameruleClient.animalSpawnYDiff = Math.toIntExact(value);
                            case SET_RANDOM_ITEM_DROPS -> GameruleClient.randomItemDrops = RandomItemDropRule.fromValue(Math.toIntExact(value));
                        }
                    });
        });
        return;
    }
}
