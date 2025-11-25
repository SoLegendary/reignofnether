package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.unit.modelling.models.EnchanterModel;
import com.solegendary.reignofnether.unit.units.villagers.EnchanterUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class EnchanterRenderer extends MobRenderer<EnchanterUnit, EnchanterModel<EnchanterUnit>> {

    public static final float SCALE_MULT = 1.25f;

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/enchanter_unit.png");

    public EnchanterRenderer(EntityRendererProvider.Context context) {
        super(context, new EnchanterModel<>(context.bakeLayer(EnchanterModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EnchanterUnit enchanterUnit) {
        return TEXTURE_LOCATION;
    }

    public EnchanterRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation mll) {
        super(pContext, new EnchanterModel<>(pContext.bakeLayer(mll)), 0.5F);
    }

    protected void scale(@NotNull EnchanterUnit pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime) {
        float bonusScale = pLivingEntity.getScale();
        pMatrixStack.scale(SCALE_MULT * bonusScale, SCALE_MULT * bonusScale, SCALE_MULT * bonusScale);
    }
}
