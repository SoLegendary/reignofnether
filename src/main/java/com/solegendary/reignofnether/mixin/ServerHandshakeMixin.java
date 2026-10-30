package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.p2p.HandshakeSecret;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// While hosting P2P, internet players without the join code's secret are turned away (see HandshakeSecret)
@Mixin(ServerHandshakePacketListenerImpl.class)
public class ServerHandshakeMixin {

    @Shadow @Final private Connection connection;

    @Inject(method = "handleIntention", at = @At("HEAD"), cancellable = true)
    private void checkSecret(ClientIntentionPacket packet, CallbackInfo ci) {
        if (packet.getIntention() != ConnectionProtocol.LOGIN || !HandshakeSecret.rejects(connection.getRemoteAddress(), packet.getHostName()))
            return;
        Component reason = Component.translatable("p2p.reignofnether.error.bad_secret");
        connection.setProtocol(ConnectionProtocol.LOGIN);
        connection.send(new ClientboundLoginDisconnectPacket(reason));
        connection.disconnect(reason);
        ci.cancel();
    }
}
