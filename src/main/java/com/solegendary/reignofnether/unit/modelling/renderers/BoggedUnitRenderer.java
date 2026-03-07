//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.modelling.renderers;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BoggedUnitRenderer extends SkeletonRenderer {
    private static final ResourceLocation BOGGED_SKELETON_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/bogged.png");

    public BoggedUnitRenderer(EntityRendererProvider.Context p_174409_) {
        super(p_174409_, ModelLayers.STRAY, ModelLayers.STRAY_INNER_ARMOR, ModelLayers.STRAY_OUTER_ARMOR);
        this.addLayer(new BoggedClothingLayer<>(this, p_174409_.getModelSet()));
    }

    public ResourceLocation getTextureLocation(AbstractSkeleton pEntity) {
        return BOGGED_SKELETON_LOCATION;
    }
}
