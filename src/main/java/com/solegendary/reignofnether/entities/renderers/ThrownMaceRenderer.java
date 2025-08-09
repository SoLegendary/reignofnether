package com.solegendary.reignofnether.entities.renderers;

import com.solegendary.reignofnether.entities.ThrownMaceProjectile;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ThrownMaceRenderer extends EntityRenderer<ThrownMaceProjectile> {

    public ThrownMaceRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownMaceProjectile thrownMaceProjectile) {
        return ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/royal_guard_unit.png");
    }
}