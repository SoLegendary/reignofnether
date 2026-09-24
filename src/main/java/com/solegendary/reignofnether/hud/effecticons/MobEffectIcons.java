package com.solegendary.reignofnether.hud.effecticons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

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

    public static final MobEffectIcon ZOMBIE_INFECTED = new MobEffectIcon(
            MobEffectRegistrar.ZOMBIE_INFECTED.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/drowned.png"),
            "zombie_infected"
    );

    public static final MobEffectIcon SLIME_INFECTED = new MobEffectIcon(
            MobEffectRegistrar.SLIME_INFECTED.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/slime.png"),
            "slime_infected"
    );

    public static final MobEffectIcon MINOR_SPEED = new MobEffectIcon(
            MobEffectRegistrar.MINOR_MOVEMENT_SPEED.get(),
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/speed.png"),
            "minor_speed"
    );

    public static final MobEffectIcon SPEED = new MobEffectIcon(
            MobEffects.MOVEMENT_SPEED,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/speed.png"),
            "speed"
    );

    public static final MobEffectIcon MINOR_SLOW = new MobEffectIcon(
            MobEffectRegistrar.MINOR_MOVEMENT_SLOWDOWN.get(),
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/slowness.png"),
            "minor_slow"
    );

    public static final MobEffectIcon SLOW = new MobEffectIcon(
            MobEffects.MOVEMENT_SLOWDOWN,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/slowness.png"),
            "slow"
    );

    public static final MobEffectIcon DAMAGE_TAKEN_INCREASE = new MobEffectIcon(
            MobEffectRegistrar.DAMAGE_TAKEN_INCREASE.get(),
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/wither.png"),
            "damage_taken_increase"
    );

    public static final MobEffectIcon SCORCHING_FIRE = new MobEffectIcon(
            MobEffectRegistrar.SCORCHING_FIRE.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/scorching_gaze.png"),
            "scorching_fire"
    );

    public static final MobEffectIcon INTENSE_HEAT = new MobEffectIcon(
            MobEffectRegistrar.INTENSE_HEAT.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/intense_heat.png"),
            "intense_heat"
    );



    public static final MobEffectIcon PHASING = new MobEffectIcon(
            MobEffectRegistrar.PHASING.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/items/ghost_cloak.png"),
            "phasing"
    );


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
            UNCONTROLLABLE,
            ZOMBIE_INFECTED,
            SLIME_INFECTED,
            MINOR_SPEED,
            SPEED,
            MINOR_SLOW,
            SLOW,
            DAMAGE_TAKEN_INCREASE,
            SCORCHING_FIRE,
            INTENSE_HEAT,
            PHASING
    );
}
