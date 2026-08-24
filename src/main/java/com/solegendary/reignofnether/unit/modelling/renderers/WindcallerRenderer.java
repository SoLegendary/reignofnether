package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.models.WindcallerModel;
import com.solegendary.reignofnether.unit.modelling.models.WretchedWraithModel;
import com.solegendary.reignofnether.unit.units.monsters.WraithUnit;
import com.solegendary.reignofnether.unit.units.villagers.WindcallerUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class WindcallerRenderer extends MobRenderer<WindcallerUnit, WindcallerModel<WindcallerUnit>> {

    public static final float SCALE_MULT = 1.0f;

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/entities/windcaller_unit.png");

    public WindcallerRenderer(EntityRendererProvider.Context context) {
        super(context, new WindcallerModel<>(context.bakeLayer(WindcallerModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull WindcallerUnit windcallerUnit) {
        return TEXTURE_LOCATION;
    }

    public WindcallerRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation mll) {
        super(pContext, new WindcallerModel<>(pContext.bakeLayer(mll)), 0.5F);
    }

    protected void scale(@NotNull WindcallerUnit pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime) {
        pMatrixStack.scale(SCALE_MULT, SCALE_MULT, SCALE_MULT);
    }

    @Override
    protected RenderType getRenderType(
            WindcallerUnit entity,
            boolean bodyVisible,
            boolean translucent,
            boolean outline) {
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }
}
