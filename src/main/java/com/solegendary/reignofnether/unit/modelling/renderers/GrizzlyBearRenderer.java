package com.solegendary.reignofnether.unit.modelling.renderers;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.PolarBear;

public class GrizzlyBearRenderer extends PolarBearRenderer {

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/grizzly_bear_unit.png");

    public GrizzlyBearRenderer(EntityRendererProvider.Context p_174356_) {
        super(p_174356_);
    }

    @Override
    public ResourceLocation getTextureLocation(PolarBear pEntity) {
        return TEXTURE_LOCATION;
    }
}
