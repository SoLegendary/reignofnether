package com.solegendary.reignofnether.unit.villagers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// TODO: add arms from VindicatorRenderer

// based on VillagerRenderer
@OnlyIn(Dist.CLIENT)
public class VillagerUnitRenderer extends MobRenderer<Villager, VillagerUnitModel<Villager>> {
    private static final ResourceLocation VILLAGER_BASE_SKIN = new ResourceLocation("textures/entity/villager/villager.png");

    public VillagerUnitRenderer(EntityRendererProvider.Context p_174437_) {
        super(p_174437_, new VillagerUnitModel<>(p_174437_.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new CustomHeadLayer<>(this, p_174437_.getModelSet(), p_174437_.getItemInHandRenderer()));
        this.addLayer(new VillagerProfessionLayer<>(this, p_174437_.getResourceManager(), "villager"));
        this.addLayer(new CrossedArmsItemLayer<>(this, p_174437_.getItemInHandRenderer()));

        // from IllagerRenderer
        this.addLayer(new CustomHeadLayer<>(this, p_174437_.getModelSet(), p_174437_.getItemInHandRenderer()));
    }

    public ResourceLocation getTextureLocation(Villager p_116312_) {
        return VILLAGER_BASE_SKIN;
    }

    protected void scale(Villager p_116314_, PoseStack p_116315_, float p_116316_) {
        float f = 0.9375F;
        if (p_116314_.isBaby()) {
            f *= 0.5F;
            this.shadowRadius = 0.25F;
        } else {
            this.shadowRadius = 0.5F;
        }

        p_116315_.scale(f, f, f);
    }
}