package com.solegendary.reignofnether.mixin.fogofwar;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// hide the vanilla border wall; RTS uses the border as invisible bounds
@Mixin(LevelRenderer.class)
public abstract class WorldBorderRenderMixin {

    @Inject(method = "renderWorldBorder", at = @At("HEAD"), cancellable = true)
    private void reignofnether$skipBorder(Camera camera, CallbackInfo ci) {
        ci.cancel();
    }
}
