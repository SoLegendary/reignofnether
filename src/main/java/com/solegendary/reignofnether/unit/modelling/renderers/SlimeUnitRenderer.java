//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.unit.units.monsters.SlimeUnit;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SlimeUnitRenderer extends MobRenderer<SlimeUnit, SlimeModel<SlimeUnit>> {
    private static final ResourceLocation SLIME_LOCATION = new ResourceLocation("textures/entity/slime/slime.png");

    public SlimeUnitRenderer(EntityRendererProvider.Context p_174391_) {
        super(p_174391_, new SlimeModel(p_174391_.bakeLayer(ModelLayers.SLIME)), 0.25F);
        this.addLayer(new SlimeOuterLayer(this, p_174391_.getModelSet()));
    }

    public void render(SlimeUnit pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        this.shadowRadius = 0.25F * (float)pEntity.getSize();
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    protected void scale(SlimeUnit slime, PoseStack pPoseStack, float pPartialTickTime) {
        float $$3 = 0.999F;
        pPoseStack.scale(0.999F, 0.999F, 0.999F);
        pPoseStack.translate(0.0F, 0.001F, 0.0F);
        float $$4 = (float)slime.getSize();
        float $$5 = Mth.lerp(pPartialTickTime, slime.oSquish + slime.extraSquish, slime.squish + slime.extraSquish) / ($$4 * 0.5F + 1.0F);
        float $$6 = 1.0F / ($$5 + 1.0F);
        pPoseStack.scale($$6 * $$4, 1.0F / $$6 * $$4, $$6 * $$4);
    }

    public ResourceLocation getTextureLocation(SlimeUnit pEntity) {
        return SLIME_LOCATION;
    }

    @Override
    protected void setupRotations(SlimeUnit entity, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTicks) {
        // "sideways" axis running left-to-right across the model.
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);

        float rollDegrees = Mth.lerp(partialTicks, entity.oRollAngle, entity.rollAngle);

        // Pivot around the model's vertical center rather than its feet, so it
        // reads as a ball rolling forward instead of tipping over at the base.
        float halfHeight = entity.getBbHeight() / 2.0F;
        poseStack.translate(0.0, halfHeight, 0.0);
        poseStack.mulPose(Axis.XN.rotationDegrees(rollDegrees));
        poseStack.translate(0.0, -halfHeight, 0.0);

    }
}
