package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.unit.modelling.models.PiglinMerchantModel;
import com.solegendary.reignofnether.unit.units.piglins.PiglinMerchantUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class PiglinMerchantRenderer extends MobRenderer<PiglinMerchantUnit, PiglinMerchantModel<PiglinMerchantUnit>> {

    public static final float SCALE_MULT = 1.5f;

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/piglin_merchant_unit.png");

    public PiglinMerchantRenderer(EntityRendererProvider.Context context) {
        super(context, new PiglinMerchantModel<>(context.bakeLayer(PiglinMerchantModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull PiglinMerchantUnit piglinMerchantUnit) {
        return TEXTURE_LOCATION;
    }

    public PiglinMerchantRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation mll) {
        super(pContext, new PiglinMerchantModel<>(pContext.bakeLayer(mll)), 0.5F);
    }

    protected void scale(@NotNull PiglinMerchantUnit pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime) {
        pMatrixStack.scale(SCALE_MULT, SCALE_MULT, SCALE_MULT);
    }
}
