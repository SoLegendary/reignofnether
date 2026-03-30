package com.solegendary.reignofnether.gamerules;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.gamemode.ClientGameModeHelper;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class GameruleClientboundPacket {

    GameruleAction action;
    String playerName;
    Long value;

    public static void setLogFalling(boolean logFalling) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_LOG_FALLING, "", logFalling ? 1L : 0L));
    }
    public static void setNeutralAggro(boolean neutralAggro) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_NEUTRAL_AGGRO, "", neutralAggro ? 1L : 0L));
    }
    public static void setMaxPopulation(long maxPopulation) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_MAX_POPULATION, "", maxPopulation));
    }
    public static void setUnitGriefing(boolean unitGriefing) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_UNIT_GRIEFING, "", unitGriefing ? 1L : 0L));
    }
    public static void setPlayerGriefing(boolean playerGriefing) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_PLAYER_GRIEFING, "", playerGriefing ? 1L : 0L));
    }
    public static void setImprovedPathfinding(boolean improvedPathfinding) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_IMPROVED_PATHFINDING, "", improvedPathfinding ? 1L : 0L));
    }
    public static void setGroundYLevel(long groundYLevel) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_GROUND_Y_LEVEL, "", groundYLevel));
    }
    public static void setFlyingMaxYLevel(long flyingMaxYLevel) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_FLYING_MAX_Y_LEVEL, "", flyingMaxYLevel));
    }
    public static void setAllowBeacons(boolean allowBeacons) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_ALLOW_BEACONS, "", allowBeacons ? 1L : 0L));
    }
    public static void setPvpModesOnly(boolean pvpModesOnly) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_PVP_MODES_ONLY, "", pvpModesOnly ? 1L : 0L));
    }
    public static void setBeaconWinMinutes(long beaconWinMinutes) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_BEACON_WIN_MINUTES, "", beaconWinMinutes));
    }
    public static void setSlantedBuilding(boolean slantedBuilding) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_SLANTED_BUILDING, "", slantedBuilding ? 1L : 0L));
    }
    public static void setAllowedHeroes(long allowedHeroes) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_ALLOWED_HEROES, "", allowedHeroes));
    }
    public static void setLockAlliances(boolean lockAlliances) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_LOCK_ALLIANCES, "", lockAlliances ? 1L : 0L));
    }
    public static void setScenarioMode(boolean scenarioMode) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameruleClientboundPacket(GameruleAction.SET_SCENARIO_MODE, "", scenarioMode ? 1L : 0L));
    }

    public GameruleClientboundPacket(GameruleAction action, String playerName, Long value) {
        this.action = action;
        this.playerName = playerName;
        this.value = value;
    }

    public GameruleClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(GameruleAction.class);
        this.playerName = buffer.readUtf();
        this.value = buffer.readLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.playerName);
        buffer.writeLong(this.value);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (action) {
                            case SET_LOG_FALLING -> GameruleClient.doLogFalling = value == 1L;
                            case SET_NEUTRAL_AGGRO -> GameruleClient.neutralAggro = value == 1L;
                            case SET_MAX_POPULATION -> GameruleClient.maxPopulation = Math.toIntExact(value);
                            case SET_UNIT_GRIEFING -> GameruleClient.doUnitGriefing = value == 1L;
                            case SET_PLAYER_GRIEFING -> GameruleClient.doPlayerGriefing = value == 1L;
                            case SET_IMPROVED_PATHFINDING -> GameruleClient.improvedPathfinding = value == 1L;
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
                        }
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
