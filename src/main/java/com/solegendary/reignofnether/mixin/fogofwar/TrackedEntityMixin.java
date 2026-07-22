package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// gate entity packets behind fog; target by name since TrackedEntity is package-private
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {

    @Shadow @Final Entity entity;

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void reignofnether$gateEntityVisibility(ServerPlayer player, CallbackInfo ci) {
        if (!FogOfWarServerEvents.isFogActiveFor(player)) return;
        ChunkPos cp = new ChunkPos(entity.blockPosition());
        if (FogOfWarServerEvents.isChunkBrightFor(player, cp)) return;
        ci.cancel();
    }
}
