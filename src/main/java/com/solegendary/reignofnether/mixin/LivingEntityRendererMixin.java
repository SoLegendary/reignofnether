package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {

    @Inject(
            method = "isShaking(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void reignofnether$forceShake(
            T entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity.hasEffect(MobEffectRegistrar.ATTACK_SLOWDOWN.get()) &&
            entity.level().getBlockState(entity.getOnPos().above()).getBlock() == BlockRegistrar.WRAITH_SNOW_LAYER.get()) {
            cir.setReturnValue(true);
        }
    }
}