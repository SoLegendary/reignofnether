package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.unit.modelling.models.WitherbornModel;
import com.solegendary.reignofnether.unit.units.piglins.PiglinMerchantUnit;
import com.solegendary.reignofnether.unit.units.piglins.WitherbornUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class WitherbornRenderer extends MobRenderer<WitherbornUnit, WitherbornModel<WitherbornUnit>> {

    public static final float SCALE_MULT = 0.75f;

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/witherborn_unit.png");

    public WitherbornRenderer(EntityRendererProvider.Context context) {
        super(context, new WitherbornModel<>(context.bakeLayer(WitherbornModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull WitherbornUnit witherbornUnit) {
        return TEXTURE_LOCATION;
    }

    protected void scale(@NotNull WitherbornUnit pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime) {
        pMatrixStack.scale(SCALE_MULT, SCALE_MULT, SCALE_MULT);
    }
}
