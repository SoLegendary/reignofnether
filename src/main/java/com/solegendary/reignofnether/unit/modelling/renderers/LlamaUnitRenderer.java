package com.solegendary.reignofnether.unit.modelling.renderers;

import net.minecraft.client.model.LlamaModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LlamaDecorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LlamaUnitRenderer extends MobRenderer<Llama, LlamaModel<Llama>> {
    private static final ResourceLocation[] LLAMA_LOCATIONS = new ResourceLocation[]{
        ResourceLocation.parse("textures/entity/llama/creamy.png"),
        ResourceLocation.parse("textures/entity/llama/white.png"),
        ResourceLocation.parse("textures/entity/llama/brown.png"),
        ResourceLocation.parse("textures/entity/llama/gray.png")
    };

    public LlamaUnitRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new LlamaModel(pContext.bakeLayer(ModelLayers.LLAMA)), 0.7F);
        this.addLayer(new LlamaDecorLayer(this, pContext.getModelSet()));
    }

    public ResourceLocation getTextureLocation(Llama pEntity) {
        return LLAMA_LOCATIONS[pEntity.getVariant().getId()];
    }
}