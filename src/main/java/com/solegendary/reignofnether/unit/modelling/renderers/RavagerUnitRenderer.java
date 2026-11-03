package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.unit.modelling.models.RavagerUnitModel;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RavagerUnitRenderer extends MobRenderer<Ravager, RavagerUnitModel> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/illager/ravager.png");

    public RavagerUnitRenderer(EntityRendererProvider.Context p_174362_) {
        super(p_174362_, new RavagerUnitModel(p_174362_.bakeLayer(ModelLayers.RAVAGER)), 1.1F);
    }

    // sink the model into the ground while burrowing (the rider is lowered separately via getPassengersRidingOffset)
    @Override
    protected void setupRotations(Ravager pEntity, PoseStack pPoseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
        super.setupRotations(pEntity, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
        if (pEntity instanceof RavagerUnit unit)
            pPoseStack.translate(0, -unit.getSiegeSinkOffset(pPartialTicks), 0);
    }

    public ResourceLocation getTextureLocation(Ravager pEntity) {
        return TEXTURE_LOCATION;
    }
}