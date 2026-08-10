//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BatUnitRenderer extends MobRenderer<Bat, BatModel> {
    private static final ResourceLocation BAT_LOCATION = new ResourceLocation("textures/entity/bat.png");

    public BatUnitRenderer(EntityRendererProvider.Context p_173929_) {
        super(p_173929_, new BatModel(p_173929_.bakeLayer(ModelLayers.BAT)), 0.25F);
    }

    public ResourceLocation getTextureLocation(Bat pEntity) {
        return BAT_LOCATION;
    }

    protected void scale(Bat pLivingEntity, PoseStack pPoseStack, float pPartialTickTime) {
        pPoseStack.scale(0.35F, 0.35F, 0.35F);
    }

    protected void setupRotations(Bat pEntityLiving, PoseStack pPoseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
        if (pEntityLiving.isResting()) {
            pPoseStack.translate(0.0F, -0.1F, 0.0F);
        } else {
            pPoseStack.translate(0.0F, Mth.cos(pAgeInTicks * 0.3F) * 0.1F, 0.0F);
        }

        super.setupRotations(pEntityLiving, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
    }
}
