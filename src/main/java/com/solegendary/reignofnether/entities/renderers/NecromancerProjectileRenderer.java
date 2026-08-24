package com.solegendary.reignofnether.entities.renderers;

import com.solegendary.reignofnether.entities.NecromancerProjectile;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class NecromancerProjectileRenderer extends MagicProjectileRenderer<NecromancerProjectile> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/necromancer_projectile.png");

    public NecromancerProjectileRenderer(EntityRendererProvider.Context ctx) { super(ctx, TEXTURE); }
}