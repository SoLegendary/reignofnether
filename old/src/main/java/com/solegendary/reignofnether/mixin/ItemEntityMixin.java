package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.units.piglins.PiglinMerchantUnit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Shadow public Entity getOwner() { return null; }

    @Inject(
            method = "isMergable",
            at = @At("HEAD"),
            cancellable = true
    )
    private void isMergable(CallbackInfoReturnable<Boolean> cir) {
        if (getOwner() instanceof PiglinMerchantUnit) {
            cir.setReturnValue(false);
        }
    }
}