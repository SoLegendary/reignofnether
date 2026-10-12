package com.solegendary.reignofnether.util;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;

/**
 * 1.21.1 removed {@code Mob#getMobType()}. What it returned is now data-driven: vanilla reads the
 * same classification back from the {@code #undead} / {@code #arthropod} / {@code #aquatic} entity
 * type tags (see {@code EntityTypeTags}), and this mod appends its own unit types to the first two
 * in its datapack, so undead and spider-like units keep the responses they had in 1.20.1 - Smite,
 * Bane of Arthropods, inverted healing and harm, and the regeneration/poison immunity.
 *
 * <p>The old call sites all asked one question - "did this mob report {@code MobType.UNDEAD}?" -
 * so this helper answers exactly that, rather than guessing from the spawn category, which differs
 * here: every unit is registered as {@link net.minecraft.world.entity.MobCategory#CREATURE}.
 */
public final class MobCategoryCompat {
	private MobCategoryCompat() {}

	/** True when the entity is in {@code #minecraft:undead}, i.e. was {@code MobType.UNDEAD} in 1.20.1. */
	public static boolean isUndead(Entity entity) {
		return entity.getType().is(EntityTypeTags.UNDEAD);
	}
}
