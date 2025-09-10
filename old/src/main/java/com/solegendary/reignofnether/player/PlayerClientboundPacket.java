package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.util.Faction;
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
    Long value;
    Faction faction;

    public static void enableRTSStatus(String playerName, Faction faction) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.ENABLE_RTS, playerName, 0L, faction));
    }

    public static void disableRTSStatus(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.DISABLE_RTS, playerName, 0L, Faction.NONE));
    }

    public static void defeat(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.DEFEAT, playerName, 0L, Faction.NONE));
    }

    public static void victory(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.VICTORY, playerName, 0L, Faction.NONE));
    }

    public static void resetRTS(boolean hard) {
        if (hard) {
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new PlayerClientboundPacket(PlayerAction.RESET_RTS_HARD, "", 0L, Faction.NONE));
        } else {
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new PlayerClientboundPacket(PlayerAction.RESET_RTS, "", 0L, Faction.NONE));
        }
    }

    public static void syncRtsGameTime(Long rtsGameTicks) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.SYNC_RTS_GAME_TIME, "", rtsGameTicks, Faction.NONE));
    }

    public static void lockRTS(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.LOCK_RTS, playerName, 0L, Faction.NONE));
    }

    public static void unlockRTS(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.UNLOCK_RTS, playerName, 0L, Faction.NONE));
    }

    // prevent one particular player from joining the match
    public static void disableStartRTS(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.DISABLE_START_RTS, playerName, 0L, Faction.NONE));
    }
    public static void enableStartRTS(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.ENABLE_START_RTS, playerName, 0L, Faction.NONE));
    }

    public static void syncBeaconOwnerTicks(String playerName, long ticks) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.SYNC_BEACON_OWNER_TICKS, playerName, ticks, Faction.NONE));
    }

    public PlayerClientboundPacket(PlayerAction playerAction, String playerName, Long value, Faction faction) {
        this.playerAction = playerAction;
        this.playerName = playerName;
        this.value = value;
        this.faction = faction;
    }

    public PlayerClientboundPacket(FriendlyByteBuf buffer) {
        this.playerAction = buffer.readEnum(PlayerAction.class);
        this.playerName = buffer.readUtf();
        this.value = buffer.readLong();
        this.faction = buffer.readEnum(Faction.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.playerAction);
        buffer.writeUtf(this.playerName);
        buffer.writeLong(this.value);
        buffer.writeEnum(this.faction);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (playerAction) {
                            case DEFEAT -> PlayerClientEvents.defeat(playerName);
                            case VICTORY -> PlayerClientEvents.victory(playerName);
                            case DISABLE_RTS -> PlayerClientEvents.disableRTS(playerName);
                            case ENABLE_RTS -> PlayerClientEvents.enableRTS(playerName, faction);
                            case RESET_RTS -> PlayerClientEvents.resetRTS(false);
                            case RESET_RTS_HARD -> PlayerClientEvents.resetRTS(true);
                            case SYNC_RTS_GAME_TIME -> PlayerClientEvents.syncRtsGameTime(value);
                            case LOCK_RTS -> PlayerClientEvents.setRTSLock(true);
                            case UNLOCK_RTS -> PlayerClientEvents.setRTSLock(false);
                            case ENABLE_START_RTS -> PlayerClientEvents.setCanStartRTS(true);
                            case DISABLE_START_RTS -> PlayerClientEvents.setCanStartRTS(false);
                            case SYNC_BEACON_OWNER_TICKS -> PlayerClientEvents.syncBeaconOwnerTicks(playerName, value);
                        }
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
