package com.solegendary.reignofnether.unit.modelling.renderers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.models.ArmouredHoglinUnitModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

public class ArmouredHoglinUnitRenderer extends MobRenderer<Hoglin, ArmouredHoglinUnitModel<Hoglin>> {

    private static final ResourceLocation ARMOURED_HOGLIN_LOCATION = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/entities/armoured_hoglin.png");

    public ArmouredHoglinUnitRenderer(EntityRendererProvider.Context p_174165_) {
        super(p_174165_, new ArmouredHoglinUnitModel(p_174165_.bakeLayer(ArmouredHoglinUnitModel.LAYER_LOCATION)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(Hoglin pEntity) {
        return ARMOURED_HOGLIN_LOCATION;
    }
}
