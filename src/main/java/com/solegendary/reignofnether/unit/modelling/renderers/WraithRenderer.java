package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.models.WraithModel;
import com.solegendary.reignofnether.unit.modelling.models.WretchedWraithModel;
import com.solegendary.reignofnether.unit.units.monsters.WraithUnit;
import com.solegendary.reignofnether.unit.units.monsters.WretchedWraithUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class WraithRenderer extends MobRenderer<WraithUnit, WraithModel<WraithUnit>> {

    public static final float SCALE_MULT = 1.0f;

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/entities/wraith_unit.png");

    public WraithRenderer(EntityRendererProvider.Context context) {
        super(context, new WraithModel<>(context.bakeLayer(WraithModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull WraithUnit wraithUnit) {
        return TEXTURE_LOCATION;
    }

    public WraithRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation mll) {
        super(pContext, new WraithModel<>(pContext.bakeLayer(mll)), 0.5F);
    }

    protected void scale(@NotNull WraithUnit pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime) {
        pMatrixStack.scale(SCALE_MULT, SCALE_MULT, SCALE_MULT);
    }

    @Override
    protected RenderType getRenderType(
            WraithUnit entity,
            boolean bodyVisible,
            boolean translucent,
            boolean outline) {
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }
}
