package com.solegendary.reignofnether.unit.modelling.renderers;

import com.solegendary.reignofnether.unit.modelling.models.SkeletonVanguardModel;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonVanguardUnit;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class SkeletonVanguardRenderer extends MobRenderer<SkeletonVanguardUnit, SkeletonVanguardModel<SkeletonVanguardUnit>> {

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/skeleton_vanguard_unit.png");

    public SkeletonVanguardRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonVanguardModel<>(context.bakeLayer(SkeletonVanguardModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SkeletonVanguardUnit skeletonVanguardUnit) {
        return TEXTURE_LOCATION;
    }
}
