package com.solegendary.reignofnether.mixin.fogofwar;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(
            method = "getShade(FFFZ)F",
            at = @At("TAIL"),
            cancellable = true,
            remap = false
    )
    private void getShade(float normalX, float normalY, float normalZ, boolean shade, CallbackInfoReturnable<Float> cir) {
        //cir.setReturnValue(0.5f);
    }
}
