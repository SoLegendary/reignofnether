package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityRenderMixin {

    @Shadow private static double viewScale;
    @Shadow private AABB bb;
    @Shadow public abstract EntityType<?> getType();

    @Inject(
        method = "shouldRenderAtSqrDistance(D)Z",
        at = @At("HEAD"),
        cancellable=true
    )
    private void shouldRenderAtSqrDistance(
            double pDistance, CallbackInfoReturnable<Boolean> cir
    ) {
        if (!OrthoviewClientEvents.isEnabled() || this.getType() != EntityType.ITEM)
            return;

        double d0 = this.bb.getSize();
        if (Double.isNaN(d0)) {
            d0 = 1.0D;
        }
        // make item entities render at 4x normal distance
        d0 *= 64.0D * viewScale * 4;
        cir.setReturnValue(pDistance < d0 * d0);
    }

}
