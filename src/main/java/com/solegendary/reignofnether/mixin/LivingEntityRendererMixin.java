package com.solegendary.reignofnether.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> {

    protected LivingEntityRendererMixin(EntityRendererProvider.Context ctx) { super(ctx); }

    // always shake when on wraith snow
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

    private static boolean isTransparent(LivingEntity entity) {
        return entity.hasEffect(MobEffectRegistrar.PHASING.get());
    }
    private static final float ALPHA = 0.5f;

    @Unique private T ron$currentEntity = null;

    @Inject(
            method = "getRenderType",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ron$render(T entity, boolean bodyVisible, boolean translucent,
                                  boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        ron$currentEntity = entity;
        if (isTransparent(entity)) {
            cir.setReturnValue(RenderType.entityTranslucent(this.getTextureLocation(entity)));
        }
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V")
    )
    private void wm$renderWithAlpha(EntityModel<T> model, PoseStack poseStack, VertexConsumer buffer,
                                    int packedLight, int packedOverlay,
                                    float red, float green, float blue, float alpha) {
        float finalAlpha = ron$currentEntity != null && isTransparent(ron$currentEntity) ? alpha * ALPHA : alpha;
        model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, finalAlpha);
    }


    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER)
    )
    private void wm$applyScale(T entity, float entityYaw, float partialTicks,
                               PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                               CallbackInfo ci) {
        if (entity instanceof Unit unit) {
            float scale = unit.getScaleAttribute();
            poseStack.scale(scale, scale, scale);
        }
    }
}