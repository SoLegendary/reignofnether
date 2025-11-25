package com.solegendary.reignofnether.unit.modelling.renderers;


import com.solegendary.reignofnether.unit.modelling.models.WildfireModel;
import com.solegendary.reignofnether.unit.units.piglins.WildfireUnit;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WildfireRenderer extends MobRenderer<WildfireUnit, WildfireModel<WildfireUnit>> {

    public static final float SCALE_MULT = 1.0f;

    private static final ResourceLocation WILDFIRE_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/wildfire_unit.png");

    public WildfireRenderer(EntityRendererProvider.Context context) {
        super(context, new WildfireModel<>(context.bakeLayer(WildfireModel.LAYER_LOCATION)), 0.5F);
    }

    protected int getBlockLightLevel(WildfireUnit pEntity, BlockPos pPos) {
        return 15;
    }

    public ResourceLocation getTextureLocation(WildfireUnit pEntity) {
        return WILDFIRE_LOCATION;
    }
}
