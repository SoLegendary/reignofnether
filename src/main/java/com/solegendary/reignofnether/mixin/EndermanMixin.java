package com.solegendary.reignofnether.mixin;

import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderMan.class)
public class EndermanMixin {

    // prevent endermen randomly teleporting during the day
    @Inject(
            method = "customServerAiStep",
            at = @At("HEAD"),
            cancellable = true
    )
    public void customServerAiStep(CallbackInfo ci) {
        ci.cancel();
    }

}
