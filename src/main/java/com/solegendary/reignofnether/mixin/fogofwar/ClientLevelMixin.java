package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Final @Shadow private DimensionSpecialEffects effects;

    @Inject(
            method = "getShade(Lnet/minecraft/core/Direction;Z)F",
            at = @At("HEAD"),
            cancellable = true
    )
    public void getShade(Direction pDirection, boolean pShade, CallbackInfoReturnable<Float> cir) {
        cir.cancel();

        float shade;

        boolean flag = this.effects.constantAmbientLight();
        if (!pShade) {
            shade = flag ? 0.9F : 1.0F;
        } else {
            switch (pDirection) {
                case DOWN -> shade = flag ? 0.9F : 0.5F;
                case UP -> shade = flag ? 0.9F : 1.0F;
                case NORTH, SOUTH ->shade = 0.8F;
                case WEST, EAST -> shade = 0.6F;
                default -> shade = 1.0F;
            }
        }
        cir.setReturnValue(shade * FogOfWarClientEvents.brightnessMulti * 0.5f);
    }
}
