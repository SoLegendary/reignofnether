package com.solegendary.reignofnether.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.util.MobEffectHelpers;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> {

    protected LivingEntityRendererMixin(EntityRendererProvider.Context ctx) { super(ctx); }

    @Inject(
            method = "isShaking(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void reignofnether$forceShake(
            T entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity.hasEffect(MobEffectHelpers.holder(MobEffectRegistrar.ATTACK_SLOWDOWN.get())) &&
            entity.level().getBlockState(entity.getOnPos().above()).getBlock() == BlockRegistrar.WRAITH_SNOW_LAYER.get()) {
            cir.setReturnValue(true);
        }
    }

    // 1.5.0 renders phasing units (ghost cloak, phasing mob effect) at half opacity
    private static boolean isTransparent(LivingEntity entity) {
        return entity.hasEffect(MobEffectHelpers.holder(MobEffectRegistrar.PHASING.get()));
    }

    private static final float ALPHA = 0.5f;

    @Unique private T ron$currentEntity = null;

    @Inject(
            method = "getRenderType",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ron$phasingTranslucent(T entity, boolean bodyVisible, boolean translucent,
                                        boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        ron$currentEntity = entity;
        if (isTransparent(entity)) {
            cir.setReturnValue(RenderType.entityTranslucent(this.getTextureLocation(entity)));
        }
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V")
    )
    private void ron$renderWithAlpha(EntityModel<T> model, PoseStack poseStack, VertexConsumer buffer,
                                     int packedLight, int packedOverlay, int packedColor) {
        // 1.21.1 packs the vertex colour into one ARGB int where 1.20.1 passed four floats, so the
        // old alpha factor is applied to the alpha byte of that int
        if (ron$currentEntity != null && isTransparent(ron$currentEntity)) {
            int alpha = (int) (((packedColor >>> 24) & 0xFF) * ALPHA);
            packedColor = (alpha << 24) | (packedColor & 0x00FFFFFF);
        }
        model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, packedColor);
    }

    // the scale attribute is visual only: it scales the model, not the hitbox
    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER)
    )
    private void ron$applyUnitScale(T entity, float entityYaw, float partialTicks,
                                    PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                    CallbackInfo ci) {
        if (entity instanceof Unit unit) {
            float scale = unit.getScaleAttribute();
            poseStack.scale(scale, scale, scale);
        }
    }
}
