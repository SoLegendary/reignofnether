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

    public static void enableRTSStatus(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.ENABLE_RTS, playerName));
    }

    public static void disableRTSStatus(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.DISABLE_RTS, playerName));
    }

    public static void defeat(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.DEFEAT, playerName));
    }

    public static void victory(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.VICTORY, playerName));
    }

    public static void resetRTS() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PlayerClientboundPacket(PlayerAction.RESET_RTS, ""));
    }

    public PlayerClientboundPacket(PlayerAction playerAction, String playerName) {
        this.playerAction = playerAction;
        this.playerName = playerName;
    }

    public PlayerClientboundPacket(FriendlyByteBuf buffer) {
        this.playerAction = buffer.readEnum(PlayerAction.class);
        this.playerName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.playerAction);
        buffer.writeUtf(this.playerName);
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
                        }
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
