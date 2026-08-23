package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogChunkSnapshot;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.solegendary.reignofnether.player.PlayerServerEvents.sendMessageToAllPlayers;

/**
 * Prevent changing worldborder because otherwise we need to spend time recaching and that freezes the server
 */
@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {

    @Inject(method = "setSize(D)V", at = @At("TAIL"), cancellable = true)
    private void borderlock$onSetSize(double size, CallbackInfo ci) {
        if (EffectiveSide.get() == LogicalSide.SERVER && FogChunkSnapshot.hasAny()) {
            FogChunkSnapshot.shouldRecapture = true;
        }
    }

    @Inject(method = "lerpSizeBetween(DDJ)V", at = @At("TAIL"), cancellable = true)
    private void borderlock$onLerpSizeBetween(double oldSize, double newSize, long time, CallbackInfo ci) {
        if (EffectiveSide.get() == LogicalSide.SERVER && FogChunkSnapshot.hasAny()) {
            FogChunkSnapshot.shouldRecapture = true;
        }
    }

    @Inject(method = "setCenter(DD)V", at = @At("TAIL"), cancellable = true)
    private void borderlock$onSetCenter(double x, double z, CallbackInfo ci) {
        if (EffectiveSide.get() == LogicalSide.SERVER && FogChunkSnapshot.hasAny()) {
            FogChunkSnapshot.shouldRecapture = true;
        }
    }
}