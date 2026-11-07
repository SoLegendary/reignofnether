package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.unit.units.piglins.BabyHoglinUnit;
import net.minecraft.client.model.HoglinModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

// same as HoglinRenderer but with slower limb movement, since the baby's short legs look frantic at hoglin speeds
public class BabyHoglinUnitRenderer extends MobRenderer<Hoglin, BabyHoglinUnitRenderer.SlowLimbHoglinModel> {

    private static final ResourceLocation HOGLIN_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/hoglin/hoglin.png");
    private static final float LIMB_SPEED_MULTIPLIER = 0.33f;

    public BabyHoglinUnitRenderer(EntityRendererProvider.Context context) {
        super(context, new SlowLimbHoglinModel(context.bakeLayer(ModelLayers.HOGLIN)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(Hoglin pEntity) {
        return HOGLIN_LOCATION;
    }

    @Override
    protected void scale(Hoglin hoglin, PoseStack pPoseStack, float pPartialTickTime) {
        if (hoglin instanceof BabyHoglinUnit babyHoglinUnit) {
            pPoseStack.scale(0.999F, 0.999F, 0.999F);
            pPoseStack.translate(0.0F, 0.001F, 0.0F);
            float $$5 = Mth.lerp(pPartialTickTime, babyHoglinUnit.oSquish, babyHoglinUnit.squish) / (0.5F + 1.0F);
            float $$6 = 1.0F / ($$5 + 1.0F);
            pPoseStack.scale($$6, 1.0F / $$6, $$6);
        }
        else
            super.scale(hoglin, pPoseStack, pPartialTickTime);
    }

    @Override
    protected boolean isShaking(Hoglin pEntity) {
        return super.isShaking(pEntity) || pEntity.isConverting();
    }

    public static class SlowLimbHoglinModel extends HoglinModel<Hoglin> {
        public SlowLimbHoglinModel(ModelPart pRoot) {
            super(pRoot);
        }

        @Override
        public void setupAnim(Hoglin pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
            super.setupAnim(pEntity, pLimbSwing * LIMB_SPEED_MULTIPLIER, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
        }
    }
}
