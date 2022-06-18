package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.world.entity.monster.AbstractIllager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


// prevents IllagerModel.setupAnim from setting arm visibility (so the portrait only renders the head)

@Mixin(AbstractIllager.class)
public class AbstractIllagerMixin {

    @Inject(
            method = "getArmPose()Lnet/minecraft/world/entity/monster/AbstractIllager$IllagerArmPose;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void getArmPose(CallbackInfoReturnable<AbstractIllager.IllagerArmPose> cir
    ) {
        if (OrthoviewClientEvents.isEnabled())
            cir.setReturnValue(AbstractIllager.IllagerArmPose.CELEBRATING);
    }
}
