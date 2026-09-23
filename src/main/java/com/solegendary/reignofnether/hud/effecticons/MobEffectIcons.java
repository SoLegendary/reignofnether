package com.solegendary.reignofnether.hud.effecticons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

import javax.annotation.Nullable;
import java.util.List;

public class MobEffectIcons {

    public static final MobEffectIcon STUN = new MobEffectIcon(
            MobEffectRegistrar.STUN.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/effects/stun.png"),
            "stun"
    );

    public static final MobEffectIcon FREEZE = new MobEffectIcon(
            MobEffectRegistrar.FREEZE.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/bitter_frost.png"),
            "freeze"
    );

    public static final MobEffectIcon UNCONTROLLABLE = new MobEffectIcon(
            MobEffectRegistrar.UNCONTROLLABLE.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/taunting_cry.png"),
            "uncontrollable"
    );

    // TODO: call from a MobEffectEvent.Added clientside event subscriber
    @Nullable
    public static MobEffectIcon getIcon(MobEffectInstance mei) {
        for (MobEffectIcon effectIcon : EFFECT_ICONS)
            if (effectIcon.effect == mei.getEffect())
                return effectIcon.copy(mei);
        return null;
    }

    public static final List<MobEffectIcon> EFFECT_ICONS = List.of(
            STUN,
            FREEZE,
            UNCONTROLLABLE
    );
}
