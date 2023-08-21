//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.units.modelling;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

import com.solegendary.reignofnether.registrars.EntityRegistrar;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PiglinUnitRenderer extends HumanoidMobRenderer<Mob, PiglinModel<Mob>> {
    private static final Map<EntityType<?>, ResourceLocation> TEXTURES;
    private static final float PIGLIN_CUSTOM_HEAD_SCALE = 1.0019531F;

    public PiglinUnitRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation p_174345_, ModelLayerLocation p_174346_, ModelLayerLocation p_174347_, boolean p_174348_) {
        super(pContext, createModel(pContext.getModelSet(), p_174345_, p_174348_), 0.5F, 1.0019531F, 1.0F, 1.0019531F);
        this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(pContext.bakeLayer(p_174346_)), new HumanoidModel(pContext.bakeLayer(p_174347_))));
    }

    private static PiglinModel<Mob> createModel(EntityModelSet p_174350_, ModelLayerLocation pLayer, boolean p_174352_) {
        PiglinModel<Mob> $$3 = new PiglinModel(p_174350_.bakeLayer(pLayer));
        if (p_174352_) {
            $$3.rightEar.visible = false;
        }

        return $$3;
    }

    public ResourceLocation getTextureLocation(Mob pEntity) {
        ResourceLocation $$1 = TEXTURES.get(pEntity.getType());
        if ($$1 == null) {
            throw new IllegalArgumentException("I don't know what texture to use for " + pEntity.getType());
        } else {
            return $$1;
        }
    }

    protected boolean isShaking(Mob pEntity) {
        return super.isShaking(pEntity) || pEntity instanceof AbstractPiglin && ((AbstractPiglin)pEntity).isConverting();
    }

    static {
        TEXTURES = ImmutableMap.of(
            EntityRegistrar.PIGLIN_GRUNT_UNIT.get(), new ResourceLocation("textures/entity/piglin/piglin.png")
        );
    }
}
