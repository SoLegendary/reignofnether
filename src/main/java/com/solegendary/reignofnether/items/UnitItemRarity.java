package com.solegendary.reignofnether.items;

import net.minecraft.world.item.Rarity;

/**
 * 1.21.1 turned Rarity into a closed vanilla enum with no custom values, so the
 * mod's legendary/mythic tiers both map onto EPIC. The two names are kept because
 * call sites use them to mean "the best stuff".
 */
public class UnitItemRarity {
    public static final Rarity LEGENDARY = Rarity.EPIC;
    public static final Rarity MYTHIC = Rarity.EPIC;
}
