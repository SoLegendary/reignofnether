package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.layers.CustomUnitHeadLayer;
import com.solegendary.reignofnether.unit.modelling.layers.VillagerUnitArmorLayer;
import com.solegendary.reignofnether.unit.modelling.models.IllagerArmorModel;
import com.solegendary.reignofnether.unit.modelling.models.VillagerUnitModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// based on IllagerRenderer
@OnlyIn(Dist.CLIENT)
public abstract class AbstractVillagerUnitRenderer<T extends AbstractIllager> extends MobRenderer<T, VillagerUnitModel<T>> {

    public static ModelLayerLocation VILLAGER_ARMOR_OUTER_LAYER = new ModelLayerLocation(ResourceLocation.parse(ReignOfNether.MOD_ID), "illager_outerarmor");
    public static ModelLayerLocation VILLAGER_ARMOR_INNER_LAYER = new ModelLayerLocation(ResourceLocation.parse(ReignOfNether.MOD_ID), "illager_innerarmor");

    protected AbstractVillagerUnitRenderer(EntityRendererProvider.Context context, VillagerUnitModel<T> model, float pShadowRadius) {
        super(context, model, pShadowRadius);
        this.addLayer(new CustomUnitHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new VillagerUnitArmorLayer<>(this,
                new IllagerArmorModel<>(context.bakeLayer(VILLAGER_ARMOR_INNER_LAYER)),
                new IllagerArmorModel<>(context.bakeLayer(VILLAGER_ARMOR_OUTER_LAYER))));
    }

    protected void scale(T p_114919_, PoseStack p_114920_, float p_114921_) {
        float f = 0.9375F;
        p_114920_.scale(0.9375F, 0.9375F, 0.9375F);
    }
}