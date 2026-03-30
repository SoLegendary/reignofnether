package com.solegendary.reignofnether.gamerules;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class GameruleServerboundPacket {

    GameruleAction action;
    String playerName;
    Long value;

    public static void setLogFalling(boolean logFalling) {
        PacketHandler.INSTANCE.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_LOG_FALLING, "", logFalling ? 1L : 0L));
    }
    public static void setNeutralAggro(boolean neutralAggro) {
        PacketHandler.INSTANCE.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_NEUTRAL_AGGRO, "", neutralAggro ? 1L : 0L));
    }
    public static void setMaxPopulation(long maxPopulation) {
        PacketHandler.INSTANCE.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_MAX_POPULATION, "", maxPopulation));
    }
    public static void setUnitGriefing(boolean unitGriefing) {
        PacketHandler.INSTANCE.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_UNIT_GRIEFING, "", unitGriefing ? 1L : 0L));
    }
    public static void setPlayerGriefing(boolean playerGriefing) {
        PacketHandler.INSTANCE.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_PLAYER_GRIEFING, "", playerGriefing ? 1L : 0L));
    }
    public static void setImprovedPathfinding(boolean improvedPathfinding) {
        PacketHandler.INSTANCE.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_IMPROVED_PATHFINDING, "", improvedPathfinding ? 1L : 0L));
    }
    public static void setGroundYLevel(long groundYLevel) {
        PacketHandler.INSTANCE.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_GROUND_Y_LEVEL, "", groundYLevel));
    }
    public static void setFlyingMaxYLevel(long flyingMaxYLevel) {
        PacketHandler.INSTANCE.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_FLYING_MAX_Y_LEVEL, "", flyingMaxYLevel));
    }
    public static void setAllowBeacons(boolean allowBeacons) {
        PacketHandler.INSTANCE.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_ALLOW_BEACONS, "", allowBeacons ? 1L : 0L));
    }
    public static void setPvpModesOnly(boolean pvpModesOnly) {
        PacketHandler.INSTANCE.sendToServer(
            new GameruleServerboundPacket(GameruleAction.SET_PVP_MODES_ONLY, "", pvpModesOnly ? 1L : 0L));
    }
    public static void setBeaconWinMinutes(long beaconWinMinutes) {
        PacketHandler.INSTANCE.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_BEACON_WIN_MINUTES, "", beaconWinMinutes));
    }
    public static void setSlantedBuilding(boolean slantedBuilding) {
        PacketHandler.INSTANCE.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_SLANTED_BUILDING, "", slantedBuilding ? 1L : 0L));
    }
    public static void setAllowedHeroes(long allowedHeroes) {
        PacketHandler.INSTANCE.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_ALLOWED_HEROES, "", allowedHeroes));
    }
    public static void setLockAlliances(boolean lockAlliances) {
        PacketHandler.INSTANCE.sendToServer(
                new GameruleServerboundPacket(GameruleAction.SET_LOCK_ALLIANCES, "", lockAlliances ? 1L : 0L));
    }

    public GameruleServerboundPacket(GameruleAction action, String playerName, Long value) {
        this.action = action;
        this.playerName = playerName;
        this.value = value;
    }

    public GameruleServerboundPacket(FriendlyByteBuf buffer) {
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
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("GameruleServerboundPacket: Sender was null");
                success.set(false);
                return;
            }
            else if (!player.hasPermissions(4)) {
                ReignOfNether.LOGGER.warn("GameruleServerboundPacket: Tried to process packet from " + player.getName() + " with insufficient permissions");
                success.set(false);
                return;
            }
            MinecraftServer server = player.level().getServer();
            GameRules gameRules = player.level().getGameRules();
            boolean booleanValue = value == 1L;

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
                case SET_IMPROVED_PATHFINDING -> {
                    gameRules.getRule(GameRuleRegistrar.IMPROVED_PATHFINDING).set(booleanValue, server);
                    for (LivingEntity le : UnitServerEvents.getAllUnits()) {
                        UnitServerEvents.improvedPathfinding = booleanValue;
                        AttributeInstance ai = le.getAttribute(Attributes.FOLLOW_RANGE);
                        if (ai != null)
                            ai.setBaseValue(Unit.getFollowRange());
                    }
                    GameruleClientboundPacket.setImprovedPathfinding(booleanValue);
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
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}