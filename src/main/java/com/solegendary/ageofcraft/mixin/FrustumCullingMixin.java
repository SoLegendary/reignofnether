package com.solegendary.ageofcraft.mixin;

import com.solegendary.ageofcraft.orthoview.OrthoviewClientVanillaEvents;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public class FrustumCullingMixin {
    // disables frustum culling
    @Inject(
            method = "cubeInFrustum(FFFFFF)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cubeInFrustum(
            float f1, float f2, float f3, float f4, float f5, float f6,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (OrthoviewClientVanillaEvents.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
}