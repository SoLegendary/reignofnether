//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.unit.units.monsters.SlimeUnit;
import net.minecraft.client.model.LavaSlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MagmaCubeUnitRenderer extends MobRenderer<SlimeUnit, LavaSlimeModel<SlimeUnit>> {
    private static final ResourceLocation MAGMACUBE_LOCATION = ResourceLocation.parse("textures/entity/slime/magmacube.png");

    public MagmaCubeUnitRenderer(EntityRendererProvider.Context p_174391_) {
        super(p_174391_, new LavaSlimeModel<>(p_174391_.bakeLayer(ModelLayers.MAGMA_CUBE)), 0.25F);
    }

    public void render(SlimeUnit pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        this.shadowRadius = 0.25F * (float)pEntity.getSize();
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    protected void scale(SlimeUnit slime, PoseStack pMatrixStack, float pPartialTickTime) {
        pMatrixStack.scale(0.999F, 0.999F, 0.999F);
        pMatrixStack.translate(0.0, 0.0010000000474974513, 0.0);
        float $$4 = (float)slime.getSize();
        float $$5 = Mth.lerp(pPartialTickTime, slime.oSquish, slime.squish) / ($$4 * 0.5F + 1.0F);
        float $$6 = 1.0F / ($$5 + 1.0F);
        pMatrixStack.scale($$6 * $$4, 1.0F / $$6 * $$4, $$6 * $$4);
    }

    public ResourceLocation getTextureLocation(SlimeUnit pEntity) {
        return MAGMACUBE_LOCATION;
    }

    @Override
    protected void setupRotations(SlimeUnit entity, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);

        float rollDegrees = Mth.lerp(partialTicks, entity.oRollAngle, entity.rollAngle);

        float halfHeight = entity.getBbHeight() / 2.0F;
        poseStack.translate(0.0, halfHeight, 0.0);
        poseStack.mulPose(Axis.XN.rotationDegrees(rollDegrees));
        poseStack.translate(0.0, -halfHeight, 0.0);
    }
}
