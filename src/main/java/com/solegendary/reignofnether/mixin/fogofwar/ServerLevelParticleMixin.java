package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// gate ALL particle sends (including sculk sensor vibration particles) behind fog of war
@Mixin(ServerLevel.class)
public abstract class ServerLevelParticleMixin {

    @Inject(
            method = "sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void reignofnether$gateParticleVisibility(ServerPlayer player, boolean longDistance,
                                                      double x, double y, double z, Packet<?> packet,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (!FogOfWarServerEvents.isFogActiveFor(player)) return;
        if (FogOfWarServerEvents.isBlockVisibleFor(player, (int) x, (int) z)) return;
        cir.setReturnValue(false);
    }
}