package com.solegendary.reignofnether.unit.modelling.renderers;

import com.solegendary.reignofnether.unit.modelling.models.RavagerUnitModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RavagerUnitRenderer extends MobRenderer<Ravager, RavagerUnitModel> {
    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("textures/entity/illager/ravager.png");

    public RavagerUnitRenderer(EntityRendererProvider.Context p_174362_) {
        super(p_174362_, new RavagerUnitModel(p_174362_.bakeLayer(ModelLayers.RAVAGER)), 1.1F);
    }

    public ResourceLocation getTextureLocation(Ravager pEntity) {
        return TEXTURE_LOCATION;
    }
}