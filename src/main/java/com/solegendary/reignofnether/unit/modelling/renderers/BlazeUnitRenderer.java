package com.solegendary.reignofnether.unit.modelling.renderers;

import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.units.piglins.BlazeUnit;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Blaze;

public class BlazeUnitRenderer extends BlazeRenderer {

    private static final ResourceLocation BLAZE_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/blaze.png");
    private static final ResourceLocation SOULFIRE_BLAZE_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/soulfire_blaze_unit.png");

    public BlazeUnitRenderer(EntityRendererProvider.Context p_173933_) {
        super(p_173933_);
    }

    @Override
    public ResourceLocation getTextureLocation(Blaze blazeUnit) {
        if (blazeUnit.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get()))
            return SOULFIRE_BLAZE_LOCATION;
        else
            return BLAZE_LOCATION;
    }
}
