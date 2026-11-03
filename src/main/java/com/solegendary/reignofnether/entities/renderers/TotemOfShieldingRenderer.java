package com.solegendary.reignofnether.entities.renderers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.modelling.models.TotemOfRegenerationModel;
import com.solegendary.reignofnether.unit.modelling.models.TotemOfShieldingModel;
import com.solegendary.reignofnether.unit.units.monsters.AbstractTotem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class TotemOfShieldingRenderer extends MobRenderer<AbstractTotem, TotemOfShieldingModel<AbstractTotem>> {

    public TotemOfShieldingRenderer(EntityRendererProvider.Context context) {
        super(context, new TotemOfShieldingModel<>(context.bakeLayer(TotemOfShieldingModel.LAYER_LOCATION)), 0.5F);
    }

    protected float getFlipDegrees(AbstractTotem pLivingEntity) {
        return 0; // don't rotate when dying
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AbstractTotem totem) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/entities/totem_of_shielding.png");
    }
}
