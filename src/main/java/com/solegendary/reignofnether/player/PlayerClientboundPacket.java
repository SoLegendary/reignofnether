package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.ability.TradeAction;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class PlayerClientboundPacket {

    PlayerAction playerAction;
    String playerName;
    Long value1;
    int value2;
    Faction faction;
    TradeAction tradeAction; // for updating market rates
    BlockPos pos;
    boolean isDogPerson;

    public static void addRTSPlayer(String playerName, Faction faction, Long id, int startPosColorId, boolean isDogPerson) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.ADD_RTS_PLAYER, playerName, id, startPosColorId, faction, isDogPerson));
    }

    public static void addScenarioNPCRTSPlayer(String playerName, Faction faction, Long id, int scenarioRoleIndex) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.ADD_SCENARIO_NPC_RTS_PLAYER, playerName, id, scenarioRoleIndex, faction, true));
    }

    public static void removeRTSPlayer(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.REMOVE_RTS_PLAYER, playerName, 0L));
    }

    public static void defeat(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.DEFEAT, playerName, 0L));
    }

    public static void victory(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.VICTORY, playerName, 0L));
    }

    public static void resetRTS(boolean hard) {
        if (hard) {
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new PlayerClientboundPacket(PlayerAction.RESET_RTS_HARD, "", 0L));
        } else {
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new PlayerClientboundPacket(PlayerAction.RESET_RTS, "", 0L));
        }
    }

    public static void publishScenarioMap() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.PUBLISH_SCENARIO_MAP, "", 0L));
    }

    public static void syncRtsGameTime(Long rtsGameTicks) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.SYNC_RTS_GAME_TIME, "", rtsGameTicks));
    }

    public static void lockRTS(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.LOCK_RTS, playerName, 0L));
    }

    public static void unlockRTS(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.UNLOCK_RTS, playerName, 0L));
    }

    // prevent one particular player from joining the match
    public static void disableStartRTS(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.DISABLE_START_RTS, playerName, 0L));
    }
    public static void enableStartRTS(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.ENABLE_START_RTS, playerName, 0L));
    }

    public static void syncBeaconOwnerTicks(String playerName, long ticks) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.SYNC_BEACON_OWNER_TICKS, playerName, ticks));
    }

    public static void setRTSCamera(String playerName, boolean value) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.SET_RTS_CAMERA, playerName, (long) (value ? 1 : 0)));
    }

    public static void setMarketRate(TradeAction tradeAction, String playerName, int value) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(tradeAction, playerName, (long) value));
    }

    public static void teleport(String playerName, BlockPos pos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.TELEPORT, playerName, pos));
    }

    public PlayerClientboundPacket(PlayerAction playerAction, String playerName, BlockPos pos) {
        this.playerAction = playerAction;
        this.playerName = playerName;
        this.value1 = 0L;
        this.value2 = 0;
        this.faction = Faction.NONE;
        this.tradeAction = TradeAction.FOOD_FOR_WOOD; // dummy value
        this.pos = pos;
        this.isDogPerson = true;
    }

    public PlayerClientboundPacket(PlayerAction playerAction, String playerName, Long value1, int value2, Faction faction, boolean isDogPerson) {
        this.playerAction = playerAction;
        this.playerName = playerName;
        this.value1 = value1;
        this.value2 = value2;
        this.faction = faction;
        this.tradeAction = TradeAction.FOOD_FOR_WOOD; // dummy value
        this.pos = new BlockPos(0,0,0);
        this.isDogPerson = isDogPerson;
    }

    public PlayerClientboundPacket(PlayerAction playerAction, String playerName, Long value1) {
        this.playerAction = playerAction;
        this.playerName = playerName;
        this.value1 = value1;
        this.value2 = 0;
        this.faction = Faction.NONE;
        this.tradeAction = TradeAction.FOOD_FOR_WOOD; // dummy value
        this.pos = new BlockPos(0,0,0);
        this.isDogPerson = true;
    }

    public PlayerClientboundPacket(TradeAction tradeAction, String playerName, Long value1) {
        this.playerAction = PlayerAction.SET_MARKET_RATE;
        this.playerName = playerName;
        this.value1 = value1;
        this.value2 = 0;
        this.faction = Faction.NONE;
        this.tradeAction = tradeAction;
        this.pos = new BlockPos(0,0,0);
        this.isDogPerson = true;
    }

    public PlayerClientboundPacket(FriendlyByteBuf buffer) {
        this.playerAction = buffer.readEnum(PlayerAction.class);
        this.playerName = buffer.readUtf();
        this.value1 = buffer.readLong();
        this.value2 = buffer.readInt();
        this.faction = buffer.readEnum(Faction.class);
        this.tradeAction = buffer.readEnum(TradeAction.class);
        this.pos = buffer.readBlockPos();
        this.isDogPerson = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.playerAction);
        buffer.writeUtf(this.playerName);
        buffer.writeLong(this.value1);
        buffer.writeInt(this.value2);
        buffer.writeEnum(this.faction);
        buffer.writeEnum(this.tradeAction);
        buffer.writeBlockPos(this.pos);
        buffer.writeBoolean(this.isDogPerson);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (playerAction) {
                            case TELEPORT -> OrthoviewClientEvents.centreCameraOnPosForPlayer(playerName, pos);
                            case DEFEAT -> PlayerClientEvents.defeat(playerName);
                            case VICTORY -> PlayerClientEvents.victory(playerName);
                            case ADD_RTS_PLAYER -> PlayerClientEvents.addRTSPlayer(playerName, faction, value1, value2, isDogPerson);
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
                        }
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
