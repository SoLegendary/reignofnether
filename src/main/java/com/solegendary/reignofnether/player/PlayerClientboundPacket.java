package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class PlayerClientboundPacket {

    PlayerAction playerAction;
    String playerName;
    Long rtsGameTime;

    public static void enableRTSStatus(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.ENABLE_RTS, playerName, 0L));
    }

    public static void disableRTSStatus(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.DISABLE_RTS, playerName, 0L));
    }

    public static void defeat(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.DEFEAT, playerName, 0L));
    }

    public static void victory(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.VICTORY, playerName, 0L));
    }

    public static void resetRTS() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.RESET_RTS, "", 0L));
    }

    public static void syncRtsGameTime(Long rtsGameTicks) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.RESET_RTS, "", rtsGameTicks));
    }

    public PlayerClientboundPacket(PlayerAction playerAction, String playerName, Long rtsGameTime) {
        this.playerAction = playerAction;
        this.playerName = playerName;
        this.rtsGameTime = rtsGameTime;
    }

    public PlayerClientboundPacket(FriendlyByteBuf buffer) {
        this.playerAction = buffer.readEnum(PlayerAction.class);
        this.playerName = buffer.readUtf();
        this.rtsGameTime = buffer.readLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.playerAction);
        buffer.writeUtf(this.playerName);
        buffer.writeLong(this.rtsGameTime);
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
                            case ENABLE_RTS -> PlayerClientEvents.enableRTS(playerName);
                            case RESET_RTS -> PlayerClientEvents.resetRTS();
                            case SYNC_RTS_GAME_TIME -> PlayerClientEvents.syncRtsGameTime(rtsGameTime);
                        }
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
