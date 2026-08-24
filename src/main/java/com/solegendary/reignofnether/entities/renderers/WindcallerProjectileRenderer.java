package com.solegendary.reignofnether.entities.renderers;

import com.solegendary.reignofnether.entities.NecromancerProjectile;
import com.solegendary.reignofnether.entities.WindcallerProjectile;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class WindcallerProjectileRenderer extends MagicProjectileRenderer<WindcallerProjectile> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/spark.png");

    public WindcallerProjectileRenderer(EntityRendererProvider.Context ctx) { super(ctx, TEXTURE); }
}