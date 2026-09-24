package com.solegendary.reignofnether.hud.effecticons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
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
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/block/ice.png"),
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

    public static final MobEffectIcon ATTACK_SLOWDOWN = new MobEffectIcon(
            MobEffectRegistrar.ATTACK_SLOWDOWN.get(),
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/slowness.png"),
            "attack_slowdown"
    );

    public static final MobEffectIcon TEMPORARY_EFFICIENCY = new MobEffectIcon(
            MobEffectRegistrar.TEMPORARY_EFFICIENCY.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/civil_enchantment.png"),
            "temporary_efficiency"
    );

    public static final MobEffectIcon BLOODLUST = new MobEffectIcon(
            MobEffectRegistrar.BLOODLUST.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/bloodlust.png"),
            "bloodlust"
    );

    public static final MobEffectIcon FROST_DAMAGE = new MobEffectIcon(
            MobEffectRegistrar.FROST_DAMAGE.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/bitter_frost.png"),
            "frost_damage"
    );

    public static final MobEffectIcon ENCHANTMENT_AMPLIFIER = new MobEffectIcon(
            MobEffectRegistrar.ENCHANTMENT_AMPLIFIER.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/march_of_progress.png"),
            "enchantment_amplifier"
    );

    public static final MobEffectIcon DISARM = new MobEffectIcon(
            MobEffectRegistrar.DISARM.get(),
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/weakness.png"),
            "disarm"
    );

    public static final MobEffectIcon SOULS_AFLAME = new MobEffectIcon(
            MobEffectRegistrar.SOULS_AFLAME.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/souls_aflame.png"),
            "souls_aflame"
    );

    public static final MobEffectIcon WARM = new MobEffectIcon(
            MobEffectRegistrar.WARM.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/fire.png"),
            "warm"
    );

    public static final MobEffectIcon VIGOR = new MobEffectIcon(
            MobEffectRegistrar.VIGOR.get(),
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/stick.png"),
            "vigor"
    );

    public static final MobEffectIcon NIGHT_WARPING = new MobEffectIcon(
            MobEffectRegistrar.NIGHT_WARPING.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/item/shadow_shifter.png"),
            "night_warping"
    );

    public static final MobEffectIcon COLD = new MobEffectIcon(
            MobEffectRegistrar.COLD.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/bitter_frost.png"),
            "cold"
    );

    public static final MobEffectIcon PHASING = new MobEffectIcon(
            MobEffectRegistrar.PHASING.get(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/item/ghost_cloak.png"),
            "phasing"
    );

    public static final MobEffectIcon STRENGTH = new MobEffectIcon(
            MobEffects.DAMAGE_BOOST,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/strength.png"),
            "strength"
    );

    public static final MobEffectIcon WEAKNESS = new MobEffectIcon(
            MobEffects.WEAKNESS,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/weakness.png"),
            "weakness"
    );

    public static final MobEffectIcon HASTE = new MobEffectIcon(
            MobEffects.DIG_SPEED,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/haste.png"),
            "haste"
    );

    public static final MobEffectIcon RESISTANCE = new MobEffectIcon(
            MobEffects.DAMAGE_RESISTANCE,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/resistance.png"),
            "resistance"
    );

    public static final MobEffectIcon REGENERATION = new MobEffectIcon(
            MobEffects.REGENERATION,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/regeneration.png"),
            "regeneration"
    );

    public static final MobEffectIcon POISON = new MobEffectIcon(
            MobEffects.POISON,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/poison.png"),
            "poison"
    );

    public static final MobEffectIcon WITHER = new MobEffectIcon(
            MobEffects.WITHER,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/wither.png"),
            "wither"
    );

    public static final MobEffectIcon LEVITATION = new MobEffectIcon(
            MobEffects.LEVITATION,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/levitation.png"),
            "levitation"
    );

    public static final MobEffectIcon FIRE_RESISTANCE = new MobEffectIcon(
            MobEffects.LEVITATION,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/fire_resistance.png"),
            "fire_resistance"
    );

    @Nullable
    public static MobEffectIcon getIcon(MobEffectInstance mei) {
        for (MobEffectIcon effectIcon : EFFECT_ICONS)
            if (effectIcon.effect == mei.getEffect())
                return effectIcon.copyWithInstance(mei);
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
            PHASING,
            ATTACK_SLOWDOWN,
            TEMPORARY_EFFICIENCY,
            BLOODLUST,
            FROST_DAMAGE,
            ENCHANTMENT_AMPLIFIER,
            DISARM,
            SOULS_AFLAME,
            WARM,
            VIGOR,
            NIGHT_WARPING,
            COLD,
            STRENGTH,
            WEAKNESS,
            HASTE,
            RESISTANCE,
            REGENERATION,
            POISON,
            WITHER,
            LEVITATION,
            FIRE_RESISTANCE
    );
}
