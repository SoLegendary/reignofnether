package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.p2p.HandshakeSecret;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A direct P2P join sends the code's secret in the handshake host name (see HandshakeSecret)
@Mixin(ClientIntentionPacket.class)
public class ClientIntentionPacketMixin {

    @Shadow @Final @Mutable private String hostName;

    @Inject(method = "<init>(Ljava/lang/String;ILnet/minecraft/network/ConnectionProtocol;)V", at = @At("RETURN"))
    private void addSecret(String hostName, int port, ConnectionProtocol intention, CallbackInfo ci) {
        if (intention == ConnectionProtocol.LOGIN)
            this.hostName = HandshakeSecret.addTo(this.hostName);
    }
}
