package com.solegendary.reignofnether.unit.modelling.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class GlintLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public GlintLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!shouldGlint(entity)) return;

        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        for (int i = 0; i < 4; i++) { // repeat for better visibility
            VertexConsumer vc = buffers.getBuffer(RenderType.entityGlint());
            getParentModel().renderToBuffer(pose, vc, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private boolean shouldGlint(T entity) {
        MobEffectInstance mei = entity.getEffect(MobEffectRegistrar.ENCHANTED_TETHER.get());
        return mei != null && mei.getDuration() > 0;
    }
}