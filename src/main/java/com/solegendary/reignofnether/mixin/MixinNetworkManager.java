package com.solegendary.reignofnether.mixin;

import io.netty.channel.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import com.solegendary.reignofnether.replay.ReplayServerEvents;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Connection.class)
public class MixinNetworkManager {

    @Shadow private Channel channel;
    @Shadow private Component disconnectedReason;

    /**
     * @author
     * @reason
     */

    @Overwrite
    @OnlyIn(Dist.CLIENT)
    public void disconnect(Component p_150718_1_) {
        if (this.channel.isOpen()) {
            this.channel.close().awaitUninterruptibly();
            this.disconnectedReason = p_150718_1_;

            if (Minecraft.getInstance().screen instanceof GenericDirtMessageScreen dirtMessageScreen) {
                Component title = dirtMessageScreen.getTitle();

                if (title.getContents() instanceof TranslatableContents translatable) {
                    if (translatable.getKey().equals("connect.negotiating"))
                        Minecraft.getInstance().setScreen(
                                new DisconnectedScreen(
                                        new JoinMultiplayerScreen(new TitleScreen()), Component.literal(""), disconnectedReason));
                }
            }
        }
    }

    // Replays record every packet the server sends a player. Outgoing packets have no Forge event, and there are
    // hundreds of send sites; ServerGamePacketListenerImpl.send would miss SimpleChannel's PacketDistributor.PLAYER,
    // which goes straight to the Connection, so this is the one place every server packet passes through.
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
            at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, @Nullable PacketSendListener listener, CallbackInfo ci) {
        if (((Connection) (Object) this).getPacketListener() instanceof ServerGamePacketListenerImpl server
                && ReplayServerEvents.onSend(server.player, packet))
            ci.cancel();
    }
}
