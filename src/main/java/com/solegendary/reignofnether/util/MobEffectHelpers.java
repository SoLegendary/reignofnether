package com.solegendary.reignofnether.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Bridging helpers for 1.21.1's holder-based effect and attribute APIs.
 *
 * <p>In 1.20.1 {@code MobEffectInstance}, {@code hasEffect}, {@code addAttribute} and friends
 * took the registry object directly. In 1.21.1 they all take a {@link Holder} instead, because
 * the registry entries themselves became holders — the mod's own effects and attributes are
 * registered the same way, so every one of its ~130 call sites now needs a holder that the
 * compiler can check.
 *
 * <p>Both flavours are accepted here so call sites can keep using whichever handle they already
 * hold: {@code MobEffects.SPEED} is a {@code Holder} in 1.21.1 while
 * {@code MobEffectRegistrar.SOULS_AFLAME.get()} is still a {@link MobEffect}. {@code holder}
 * looks the value up in the frozen registry, so this is a type change, not a behaviour change.
 */
public final class MobEffectHelpers {

    private MobEffectHelpers() { }

    public static Holder<MobEffect> holder(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    public static Holder<MobEffect> holder(Holder<MobEffect> effect) {
        return effect;
    }

    // Overloads mirroring MobEffectInstance's own constructors, for both handle flavours.
    public static MobEffectInstance instance(MobEffect effect) {
        return new MobEffectInstance(holder(effect));
    }

    public static MobEffectInstance instance(Holder<MobEffect> effect) {
        return new MobEffectInstance(effect);
    }

    public static MobEffectInstance instance(MobEffect effect, int duration) {
        return new MobEffectInstance(holder(effect), duration);
    }

    public static MobEffectInstance instance(Holder<MobEffect> effect, int duration) {
        return new MobEffectInstance(effect, duration);
    }

    public static MobEffectInstance instance(MobEffect effect, int duration, int amplifier) {
        return new MobEffectInstance(holder(effect), duration, amplifier);
    }

    public static MobEffectInstance instance(Holder<MobEffect> effect, int duration, int amplifier) {
        return new MobEffectInstance(effect, duration, amplifier);
    }

    public static MobEffectInstance instance(MobEffect effect, int duration, int amplifier,
                                             boolean ambient, boolean visible) {
        return new MobEffectInstance(holder(effect), duration, amplifier, ambient, visible);
    }

    public static MobEffectInstance instance(Holder<MobEffect> effect, int duration, int amplifier,
                                             boolean ambient, boolean visible) {
        return new MobEffectInstance(effect, duration, amplifier, ambient, visible);
    }

    public static Holder<net.minecraft.world.entity.ai.attributes.Attribute> attributeHolder(
            net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
    }

    public static Holder<net.minecraft.world.entity.ai.attributes.Attribute> attributeHolder(
            Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        return attribute;
    }
}
