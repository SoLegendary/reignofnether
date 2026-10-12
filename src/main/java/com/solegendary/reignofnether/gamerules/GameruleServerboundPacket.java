package com.solegendary.reignofnether.gamerules;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import com.solegendary.reignofnether.gamerules.GameruleServerboundPacket;
import com.solegendary.reignofnether.gamerules.GameruleClientboundPacket;
import com.solegendary.reignofnether.gamerules.GameruleAction;

public class GameruleServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<GameruleServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "gamerule_serverbound"));

    @Override
    public CustomPacketPayload.Type<GameruleServerboundPacket> type() {
        return TYPE;
    }

    GameruleAction action;
    String playerName;
    Long value;

    public static void setLogFalling(boolean logFalling) {
        PacketDistributor.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_LOG_FALLING, "", logFalling ? 1L : 0L));
    }
    public static void setNeutralAggro(boolean neutralAggro) {
        PacketDistributor.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_NEUTRAL_AGGRO, "", neutralAggro ? 1L : 0L));
    }
    public static void setMaxPopulation(long maxPopulation) {
        PacketDistributor.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_MAX_POPULATION, "", maxPopulation));
    }
    public static void setUnitGriefing(boolean unitGriefing) {
        PacketDistributor.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_UNIT_GRIEFING, "", unitGriefing ? 1L : 0L));
    }
    public static void setPlayerGriefing(boolean playerGriefing) {
        PacketDistributor.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_PLAYER_GRIEFING, "", playerGriefing ? 1L : 0L));
    }
    public static void setGroundYLevel(long groundYLevel) {
        PacketDistributor.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_GROUND_Y_LEVEL, "", groundYLevel));
    }
    public static void setFlyingMaxYLevel(long flyingMaxYLevel) {
        PacketDistributor.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_FLYING_MAX_Y_LEVEL, "", flyingMaxYLevel));
    }
    public static void setAllowBeacons(boolean allowBeacons) {
        PacketDistributor.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_ALLOW_BEACONS, "", allowBeacons ? 1L : 0L));
    }
    public static void setPvpModesOnly(boolean pvpModesOnly) {
        PacketDistributor.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_PVP_MODES_ONLY, "", pvpModesOnly ? 1L : 0L));
    }
    public static void setBeaconWinMinutes(long beaconWinMinutes) {
        PacketDistributor.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_BEACON_WIN_MINUTES, "", beaconWinMinutes));
    }
    public static void setSlantedBuilding(boolean slantedBuilding) {
        PacketDistributor.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_SLANTED_BUILDING, "", slantedBuilding ? 1L : 0L));
    }
    public static void setAllowedHeroes(long allowedHeroes) {
        PacketDistributor.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_ALLOWED_HEROES, "", allowedHeroes));
    }
    public static void setLockAlliances(boolean lockAlliances) {
        PacketDistributor.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_LOCK_ALLIANCES, "", lockAlliances ? 1L : 0L));
    }
    public static void setCoopMode(boolean coopMode) {
        PacketDistributor.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_COOP_MODE, "", coopMode ? 1L : 0L));
    }
    public static void setRtsPathfinding(boolean rtsPathfinding) {
        PacketDistributor.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_RTS_PATHFINDING, "", rtsPathfinding ? 1L : 0L));
    }
    public static void setAnimalSpawnYDiff(long animalSpawnYDiff) {
        PacketDistributor.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_ANIMAL_SPAWN_Y_DIFF, "", animalSpawnYDiff));
    }
    public static void setRandomItemDrops(long randomItemDrops) {
        PacketDistributor.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_RANDOM_ITEM_DROPS, "", randomItemDrops));
    }

    public GameruleServerboundPacket(GameruleAction action, String playerName, Long value) {
        this.action = action;
        this.playerName = playerName;
        this.value = value;
    }

    public GameruleServerboundPacket(RegistryFriendlyByteBuf buffer) {
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
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                ReignOfNether.LOGGER.warn("GameruleServerboundPacket: Sender was null");
                return;
            }
            else if (!player.hasPermissions(4)) {
                ReignOfNether.LOGGER.warn("GameruleServerboundPacket: Tried to process packet from " + player.getName() + " with insufficient permissions");
                return;
            }
            MinecraftServer server = player.level().getServer();
            GameRules gameRules = player.level().getGameRules();
            boolean booleanValue = value == 1L;

            ReignOfNether.LOGGER.info("[Gamerule] {} set {} to {} (value: {})", player.getName(), action, booleanValue ? "ON" : "OFF", value);

            switch (action) {
                case SET_LOG_FALLING -> {
                    gameRules.getRule(GameRuleRegistrar.LOG_FALLING).set(booleanValue, server);
                    GameruleClientboundPacket.setLogFalling(booleanValue);
                }
                case SET_NEUTRAL_AGGRO -> {
                    gameRules.getRule(GameRuleRegistrar.NEUTRAL_AGGRO).set(booleanValue, server);
                    GameruleClientboundPacket.setNeutralAggro(booleanValue);
                }
                case SET_MAX_POPULATION -> {
                    UnitServerEvents.maxPopulation = Math.toIntExact(value);
                    gameRules.getRule(GameRuleRegistrar.MAX_POPULATION).set(UnitServerEvents.maxPopulation, server);
                    GameruleClientboundPacket.setMaxPopulation(UnitServerEvents.maxPopulation);
                }
                case SET_UNIT_GRIEFING -> {
                    gameRules.getRule(GameRuleRegistrar.DO_UNIT_GRIEFING).set(booleanValue, server);
                    GameruleClientboundPacket.setUnitGriefing(booleanValue);
                }
                case SET_PLAYER_GRIEFING -> {
                    gameRules.getRule(GameRuleRegistrar.DO_PLAYER_GRIEFING).set(booleanValue, server);
                    GameruleClientboundPacket.setPlayerGriefing(booleanValue);
                }
                case SET_GROUND_Y_LEVEL -> {
                    gameRules.getRule(GameRuleRegistrar.GROUND_Y_LEVEL).set(Math.toIntExact(value), server);
                    GameruleClientboundPacket.setGroundYLevel(value);
                }
                case SET_FLYING_MAX_Y_LEVEL -> {
                    gameRules.getRule(GameRuleRegistrar.FLYING_MAX_Y_LEVEL).set(Math.toIntExact(value), server);
                    GameruleClientboundPacket.setFlyingMaxYLevel(value);
                }
                case SET_ALLOW_BEACONS -> {
                    gameRules.getRule(GameRuleRegistrar.ALLOW_BEACONS).set(booleanValue, server);
                    GameruleClientboundPacket.setAllowBeacons(booleanValue);
                }
                case SET_PVP_MODES_ONLY -> {
                    gameRules.getRule(GameRuleRegistrar.PVP_MODES_ONLY).set(booleanValue, server);
                    GameruleClientboundPacket.setPvpModesOnly(booleanValue);
                }
                case SET_BEACON_WIN_MINUTES -> {
                    gameRules.getRule(GameRuleRegistrar.BEACON_WIN_MINUTES).set(Math.toIntExact(value), server);
                    GameruleClientboundPacket.setBeaconWinMinutes(value);
                }
                case SET_SLANTED_BUILDING -> {
                    gameRules.getRule(GameRuleRegistrar.SLANTED_BUILDING).set(booleanValue, server);
                    GameruleClientboundPacket.setSlantedBuilding(booleanValue);
                }
                case SET_ALLOWED_HEROES -> {
                    gameRules.getRule(GameRuleRegistrar.ALLOWED_HEROES).set(Math.toIntExact(value), server);
                    GameruleClientboundPacket.setAllowedHeroes(value);
                }
                case SET_LOCK_ALLIANCES -> {
                    gameRules.getRule(GameRuleRegistrar.LOCK_ALLIANCES).set(booleanValue, server);
                    GameruleClientboundPacket.setLockAlliances(booleanValue);
                }
                case SET_COOP_MODE -> {
                    gameRules.getRule(GameRuleRegistrar.COOP_MODE).set(booleanValue, server);
                    GameruleClientboundPacket.setCoopMode(booleanValue);
                }
                case SET_RTS_PATHFINDING -> {
                    gameRules.getRule(GameRuleRegistrar.RTS_PATHFINDING).set(booleanValue, server);
                    UnitServerEvents.rtsPathfinding = booleanValue;
                    GameruleClientboundPacket.setRtsPathfinding(booleanValue);
                }
                case SET_ANIMAL_SPAWN_Y_DIFF -> {
                    gameRules.getRule(GameRuleRegistrar.ANIMAL_SPAWN_Y_DIFF).set(Math.toIntExact(value), server);
                    GameruleClientboundPacket.setAnimalSpawnYDiff(value);
                }
                case SET_RANDOM_ITEM_DROPS -> {
                    gameRules.getRule(GameRuleRegistrar.RANDOM_ITEM_DROPS).set(Math.toIntExact(value), server);
                    GameruleClientboundPacket.setRandomItemDrops(value);
                }
            }
        });
        return;
    }
}