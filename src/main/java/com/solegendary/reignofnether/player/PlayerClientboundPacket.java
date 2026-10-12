package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.ability.TradeAction;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class PlayerClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<PlayerClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "player_clientbound"));

    @Override
    public CustomPacketPayload.Type<PlayerClientboundPacket> type() {
        return TYPE;
    }

    PlayerAction playerAction;
    String playerName;
    Long value1;
    int value2;
    Faction faction;
    TradeAction tradeAction; // for updating market rates
    BlockPos pos;
    boolean isDogPerson;
    // true when the client is only catching up on state it already had (re-login/sync), so
    // one-time client resets must not run again
    boolean isForLogin;

    public static void addRTSPlayer(String playerName, Faction faction, Long id, int startPosColorId, boolean isDogPerson, boolean isForLogin) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.ADD_RTS_PLAYER, playerName, id, startPosColorId, faction, isDogPerson, isForLogin));
    }

    public static void addScenarioNPCRTSPlayer(String playerName, Faction faction, Long id, int scenarioRoleIndex) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.ADD_SCENARIO_NPC_RTS_PLAYER, playerName, id, scenarioRoleIndex, faction, true, true));
    }

    public static void removeRTSPlayer(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.REMOVE_RTS_PLAYER, playerName, 0L));
    }

    public static void defeat(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.DEFEAT, playerName, 0L));
    }

    public static void victory(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.VICTORY, playerName, 0L));
    }

    public static void resetRTS(boolean hard) {
        if (hard) {
            PacketHandler.send(PacketHandler.allPlayers(),
                    new PlayerClientboundPacket(PlayerAction.RESET_RTS_HARD, "", 0L));
        } else {
            PacketHandler.send(PacketHandler.allPlayers(),
                    new PlayerClientboundPacket(PlayerAction.RESET_RTS, "", 0L));
        }
    }

    public static void publishScenarioMap() {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.PUBLISH_SCENARIO_MAP, "", 0L));
    }

    public static void syncRtsGameTime(Long rtsGameTicks) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.SYNC_RTS_GAME_TIME, "", rtsGameTicks));
    }

    public static void lockRTS(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.LOCK_RTS, playerName, 0L));
    }

    public static void unlockRTS(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.UNLOCK_RTS, playerName, 0L));
    }

    // prevent one particular player from joining the match
    public static void disableStartRTS(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.DISABLE_START_RTS, playerName, 0L));
    }
    public static void enableStartRTS(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.ENABLE_START_RTS, playerName, 0L));
    }

    public static void syncBeaconOwnerTicks(String playerName, long ticks) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.SYNC_BEACON_OWNER_TICKS, playerName, ticks));
    }

    public static void syncItemsDropped(String playerName, long itemsDropped) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.SYNC_ITEMS_DROPPED, playerName, itemsDropped));
    }

    public static void setRTSCamera(String playerName, boolean value) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.SET_RTS_CAMERA, playerName, (long) (value ? 1 : 0)));
    }

    public static void setMarketRate(TradeAction tradeAction, String playerName, int value) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(tradeAction, playerName, (long) value));
    }

    public static void teleport(String playerName, BlockPos pos) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.TELEPORT, playerName, pos));
    }

    public PlayerClientboundPacket(PlayerAction playerAction, String playerName, BlockPos pos) {
        this.playerAction = playerAction;
        this.playerName = playerName;
        this.value1 = 0L;
        this.value2 = 0;
        this.faction = Factions.NONE;
        this.tradeAction = TradeAction.EMERALD_FOR_FOOD; // dummy value
        this.pos = pos;
        this.isDogPerson = true;
    }

    public PlayerClientboundPacket(PlayerAction playerAction, String playerName, Long value1, int value2, Faction faction, boolean isDogPerson, boolean isForLogin) {
        this.playerAction = playerAction;
        this.playerName = playerName;
        this.value1 = value1;
        this.value2 = value2;
        this.faction = faction;
        this.tradeAction = TradeAction.EMERALD_FOR_FOOD; // dummy value
        this.pos = new BlockPos(0,0,0);
        this.isDogPerson = isDogPerson;
        this.isForLogin = isForLogin;
    }

    public PlayerClientboundPacket(PlayerAction playerAction, String playerName, Long value1) {
        this.playerAction = playerAction;
        this.playerName = playerName;
        this.value1 = value1;
        this.value2 = 0;
        this.faction = Factions.NONE;
        this.tradeAction = TradeAction.EMERALD_FOR_FOOD; // dummy value
        this.pos = new BlockPos(0,0,0);
        this.isDogPerson = true;
    }

    public PlayerClientboundPacket(TradeAction tradeAction, String playerName, Long value1) {
        this.playerAction = PlayerAction.SET_MARKET_RATE;
        this.playerName = playerName;
        this.value1 = value1;
        this.value2 = 0;
        this.faction = Factions.NONE;
        this.tradeAction = tradeAction;
        this.pos = new BlockPos(0,0,0);
        this.isDogPerson = true;
    }

    public PlayerClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.playerAction = buffer.readEnum(PlayerAction.class);
        this.playerName = buffer.readUtf();
        this.value1 = buffer.readLong();
        this.value2 = buffer.readInt();
        this.faction = Factions.getFaction(buffer.readResourceLocation());
        this.tradeAction = buffer.readEnum(TradeAction.class);
        this.pos = buffer.readBlockPos();
        this.isDogPerson = buffer.readBoolean();
        this.isForLogin = buffer.readBoolean();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.playerAction);
        buffer.writeUtf(this.playerName);
        buffer.writeLong(this.value1);
        buffer.writeInt(this.value2);
        buffer.writeResourceLocation(this.faction.key);
        buffer.writeEnum(this.tradeAction);
        buffer.writeBlockPos(this.pos);
        buffer.writeBoolean(this.isDogPerson);
        buffer.writeBoolean(this.isForLogin);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (playerAction) {
                            case TELEPORT -> OrthoviewClientEvents.centreCameraOnPosForPlayer(playerName, pos);
                            case DEFEAT -> PlayerClientEvents.defeat(playerName);
                            case VICTORY -> PlayerClientEvents.victory(playerName);
                            case ADD_RTS_PLAYER -> PlayerClientEvents.addRTSPlayer(playerName, faction, value1, value2, isDogPerson, isForLogin);
                            case ADD_SCENARIO_NPC_RTS_PLAYER -> PlayerClientEvents.addScenarioNPCRTSPlayer(playerName, faction, value1, value2);
                            case REMOVE_RTS_PLAYER -> PlayerClientEvents.removeRTSPlayer(playerName);
                            case RESET_RTS -> PlayerClientEvents.resetRTS(false);
                            case RESET_RTS_HARD -> PlayerClientEvents.resetRTS(true);
                            case PUBLISH_SCENARIO_MAP -> PlayerClientEvents.publishScenarioMap();
                            case SYNC_RTS_GAME_TIME -> PlayerClientEvents.syncRtsGameTime(value1);
                            case LOCK_RTS -> PlayerClientEvents.setRTSLock(true);
                            case UNLOCK_RTS -> PlayerClientEvents.setRTSLock(false);
                            case ENABLE_START_RTS -> PlayerClientEvents.setCanStartRTS(true);
                            case DISABLE_START_RTS -> PlayerClientEvents.setCanStartRTS(false);
                            case SYNC_BEACON_OWNER_TICKS -> PlayerClientEvents.syncBeaconOwnerTicks(playerName, value1);
                            case SET_RTS_CAMERA -> OrthoviewClientEvents.tryToSetCamera(playerName, value1 == 1L);
                            case SET_MARKET_RATE -> PlayerClientEvents.setMarketRate(tradeAction, playerName, Math.toIntExact(value1));
                            case SYNC_ITEMS_DROPPED -> PlayerClientEvents.setItemsDropped(playerName, Math.toIntExact(value1));
                        }
                    });
        });
        return;
    }
}
