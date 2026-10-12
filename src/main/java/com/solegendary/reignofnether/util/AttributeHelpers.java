package com.solegendary.reignofnether.util;

import com.solegendary.reignofnether.util.AttributeHelpers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * Wraps a plain {@link Attribute} in the {@link Holder} that 1.21.1 requires.
 *
 * <p>{@code LivingEntity#getAttribute}, {@code AttributeSupplier.Builder#add} and
 * {@code ItemStack#addAttributeModifier} all switched from the registry object to a holder in
 * 1.21.1. The mod's attributes are registered through a DeferredRegister, so it holds
 * {@code Supplier<Attribute>} handles; this is the single place that bridges them.
 */
public final class AttributeHelpers {

    private AttributeHelpers() { }

    public static Holder<Attribute> holder(Attribute attribute) {
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
    }

    /** Passthrough for call sites that already hold one, e.g. {@code Attributes.ATTACK_DAMAGE}. */
    public static Holder<Attribute> holder(Holder<Attribute> attribute) {
        return attribute;
    }
}