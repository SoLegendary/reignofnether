package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.unit.modelling.models.MarauderModel;
import com.solegendary.reignofnether.unit.modelling.models.PiglinMerchantModel;
import com.solegendary.reignofnether.unit.units.piglins.MarauderUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class MarauderRenderer extends MobRenderer<MarauderUnit, MarauderModel<MarauderUnit>> {

    public static final float SCALE_MULT = 1.0f;

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/marauder_unit.png");
    private static final ResourceLocation TEXTURE_LOCATION_ARMORED = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/marauder_unit_armored.png");

    public MarauderRenderer(EntityRendererProvider.Context context) {
        super(context, new MarauderModel<>(context.bakeLayer(MarauderModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MarauderUnit marauderUnit) {
        return marauderUnit.hasNetheriteChestplate() ? TEXTURE_LOCATION_ARMORED : TEXTURE_LOCATION;
    }

    public MarauderRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation mll) {
        super(pContext, new MarauderModel<>(pContext.bakeLayer(mll)), 0.5F);
    }

    protected void scale(@NotNull MarauderUnit pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime) {
        pMatrixStack.scale(SCALE_MULT, SCALE_MULT, SCALE_MULT);
    }
}
