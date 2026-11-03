package com.solegendary.reignofnether.entities.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.models.TotemOfRegenerationModel;
import com.solegendary.reignofnether.unit.units.monsters.AbstractTotem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class TotemOfRegenerationRenderer extends MobRenderer<AbstractTotem, TotemOfRegenerationModel<AbstractTotem>> {

    public static final float SCALE_MULT = 1.15f;

    public TotemOfRegenerationRenderer(EntityRendererProvider.Context context) {
        super(context, new TotemOfRegenerationModel<>(context.bakeLayer(TotemOfRegenerationModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AbstractTotem totem) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/entities/totem_of_regeneration.png");
    }

    protected float getFlipDegrees(AbstractTotem pLivingEntity) {
        return 0; // don't rotate when dying
    }

    protected void scale(@NotNull AbstractTotem totem, PoseStack pMatrixStack, float pPartialTickTime) {
        pMatrixStack.translate(0.03f, 0, -0.06f);
        pMatrixStack.scale(SCALE_MULT, SCALE_MULT, SCALE_MULT);
    }
}
