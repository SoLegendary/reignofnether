package com.solegendary.reignofnether.enchantments;

import com.solegendary.reignofnether.resources.ResourceCost;

/**
 * Tuning constants that used to live on the enchantment classes themselves.
 *
 * <p>In 1.20.1 each enchantment was a subclass of {@code Enchantment}, and the gameplay code
 * read its tuning (cooldown multiplier, effect duration) off the class. 1.21.1 makes
 * {@code Enchantment} a final data-driven record, so the constants live here instead; the ids
 * and holders are in {@code EnchantmentRegistrar}.
 */
public final class RTSEnchantments {

    private RTSEnchantments() { }

    /** Vigor: cooldowns are multiplied by this per level. */
    public static final float VIGOR_CD_MULTIPLIER = 0.75f;

    /** Maiming: how long the slowness applied on a hit lasts, in ticks. */
    public static final int MAIMING_SLOWNESS_DURATION = 5 * ResourceCost.TICKS_PER_SECOND;
}