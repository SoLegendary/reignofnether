package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.models.WretchedWraithModel;
import com.solegendary.reignofnether.unit.units.monsters.WretchedWraithUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class WretchedWraithRenderer extends MobRenderer<WretchedWraithUnit, WretchedWraithModel<WretchedWraithUnit>> {

    public static final float SCALE_MULT = 1.0f;

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/entities/wretched_wraith_unit.png");

    public WretchedWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new WretchedWraithModel<>(context.bakeLayer(WretchedWraithModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull WretchedWraithUnit wretchedWraithUnit) {
        return TEXTURE_LOCATION;
    }

    public WretchedWraithRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation mll) {
        super(pContext, new WretchedWraithModel<>(pContext.bakeLayer(mll)), 0.5F);
    }

    protected void scale(@NotNull WretchedWraithUnit pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime) {
        pMatrixStack.scale(SCALE_MULT, SCALE_MULT, SCALE_MULT);
    }

    @Override
    protected RenderType getRenderType(
            WretchedWraithUnit entity,
            boolean bodyVisible,
            boolean translucent,
            boolean outline) {
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }
}
