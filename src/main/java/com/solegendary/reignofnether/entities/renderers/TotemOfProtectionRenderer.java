package com.solegendary.reignofnether.entities.renderers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.modelling.models.TotemOfProtectionModel;
import com.solegendary.reignofnether.unit.modelling.models.TotemOfRegenerationModel;
import com.solegendary.reignofnether.unit.units.monsters.AbstractTotem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class TotemOfProtectionRenderer extends MobRenderer<AbstractTotem, TotemOfProtectionModel<AbstractTotem>> {

    public TotemOfProtectionRenderer(EntityRendererProvider.Context context) {
        super(context, new TotemOfProtectionModel<>(context.bakeLayer(TotemOfProtectionModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AbstractTotem totem) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/entities/totem_of_protection.png");
    }
}
