package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.unit.modelling.models.NecromancerModel;
import com.solegendary.reignofnether.unit.units.monsters.NecromancerUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class NecromancerRenderer extends MobRenderer<NecromancerUnit, NecromancerModel<NecromancerUnit>> {

    public static final float SCALE_MULT = 1.25f;

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/necromancer_unit.png");

    public NecromancerRenderer(EntityRendererProvider.Context context) {
        super(context, new NecromancerModel<>(context.bakeLayer(NecromancerModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull NecromancerUnit necromancerUnit) {
        return TEXTURE_LOCATION;
    }

    public NecromancerRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation mll) {
        super(pContext, new NecromancerModel<>(pContext.bakeLayer(mll)), 0.5F);
    }

    protected void scale(@NotNull NecromancerUnit pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime) {
        pMatrixStack.scale(SCALE_MULT, SCALE_MULT, SCALE_MULT);
    }
}
