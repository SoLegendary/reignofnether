package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.layers.CustomUnitHeadLayer;
import com.solegendary.reignofnether.unit.modelling.layers.VillagerUnitArmorLayer;
import com.solegendary.reignofnether.unit.modelling.models.IllagerArmorModel;
import com.solegendary.reignofnether.unit.modelling.models.VillagerUnitModel;
<<<<<<< HEAD
import net.minecraft.client.model.geom.ModelLayerLocation;
=======
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
>>>>>>> 8247d300 (Graveyard Stockpile & Blacksmith Armour)
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// based on IllagerRenderer
@OnlyIn(Dist.CLIENT)
public abstract class AbstractVillagerUnitRenderer<T extends AbstractIllager> extends MobRenderer<T, VillagerUnitModel<T>> {
<<<<<<< HEAD

    public static ModelLayerLocation VILLAGER_ARMOR_OUTER_LAYER = new ModelLayerLocation(ResourceLocation.parse(ReignOfNether.MOD_ID), "illager_outerarmor");
    public static ModelLayerLocation VILLAGER_ARMOR_INNER_LAYER = new ModelLayerLocation(ResourceLocation.parse(ReignOfNether.MOD_ID), "illager_innerarmor");

    protected AbstractVillagerUnitRenderer(EntityRendererProvider.Context context, VillagerUnitModel<T> model, float pShadowRadius) {
        super(context, model, pShadowRadius);
        this.addLayer(new CustomUnitHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new VillagerUnitArmorLayer<>(this,
                new IllagerArmorModel<>(context.bakeLayer(VILLAGER_ARMOR_INNER_LAYER)),
                new IllagerArmorModel<>(context.bakeLayer(VILLAGER_ARMOR_OUTER_LAYER))));
=======
    protected AbstractVillagerUnitRenderer(EntityRendererProvider.Context p_174182_, VillagerUnitModel<T> p_174183_, float p_174184_) {
        super(p_174182_, p_174183_, p_174184_);
        this.addLayer(new CustomHeadLayer<>(this, p_174182_.getModelSet(), p_174182_.getItemInHandRenderer()));
<<<<<<< HEAD
        this.addLayer(new VillagerUnitArmorLayer<>(this,
                new HumanoidArmorModel<>(p_174182_.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidArmorModel<>(p_174182_.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR))
        ));
>>>>>>> 8247d300 (Graveyard Stockpile & Blacksmith Armour)
=======
>>>>>>> 13d20ae4 (Removed armor and layer related code.)
    }

    protected void scale(T p_114919_, PoseStack p_114920_, float p_114921_) {
        float f = 0.9375F;
        p_114920_.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
