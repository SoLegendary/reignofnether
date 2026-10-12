//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.models.BatUnitModel;
import com.solegendary.reignofnether.unit.units.monsters.BatUnit;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Ghast;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BatUnitRenderer extends MobRenderer<BatUnit, BatUnitModel> {
    private static final ResourceLocation BAT_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/bat.png");

    public BatUnitRenderer(EntityRendererProvider.Context p_173929_) {
        // ModelLayers.BAT on purpose: the unit draws the vanilla bat texture
        // (textures/entity/bat.png), so it must use the 1.21.1 mesh that texture is mapped for - see
        // BatUnitModel for what breaks if the mesh and the animation come from different versions.
        super(p_173929_, new BatUnitModel(p_173929_.bakeLayer(ModelLayers.BAT)), 0.25F);
    }

    public ResourceLocation getTextureLocation(BatUnit pEntity) {
        return BAT_LOCATION;
    }

    protected void scale(BatUnit pLivingEntity, PoseStack pPoseStack, float pPartialTickTime) {
        pPoseStack.scale(0.7F, 0.7F, 0.7F);
    }

    // 1.21.1 added a trailing Entity#getScale() parameter to setupRotations.
    protected void setupRotations(BatUnit pEntityLiving, PoseStack pPoseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks, float pScale) {
        pPoseStack.translate(0.0F, Mth.cos(pAgeInTicks * 0.3F) * 0.1F, 0.0F);
        super.setupRotations(pEntityLiving, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTicks, pScale);
        pPoseStack.translate(0.0D, -0.3D, -0.2D);
    }

}
