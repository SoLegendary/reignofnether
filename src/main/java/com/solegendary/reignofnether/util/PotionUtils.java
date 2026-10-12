package com.solegendary.reignofnether.util;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.List;
import java.util.Optional;

/**
 * Replaces {@code net.minecraft.world.item.alchemy.PotionUtils}, deleted in 1.21.1.
 *
 * <p>A thrown potion's contents are now a single {@link PotionContents} data component instead
 * of being spread across the item id plus an {@code CustomEffects} tag. These are the same
 * questions the old static helpers answered, read off the component instead.
 */
public final class PotionUtils {

    private PotionUtils() { }

    private static PotionContents contents(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    }

    /** Replaces {@code PotionUtils.getPotion}; empty when the stack carries no potion. */
    public static Holder<Potion> getPotion(ItemStack stack) {
        return contents(stack).potion().orElse(null);
    }

    /** Replaces {@code PotionUtils.getMobEffects}. */
    public static List<MobEffectInstance> getMobEffects(ItemStack stack) {
        return contents(stack).potion()
                .map(potion -> potion.value().getEffects())
                .orElse(List.of());
    }

    /** Replaces {@code PotionUtils.getCustomEffects} — the effects stored on the stack itself. */
    public static List<MobEffectInstance> getCustomEffects(ItemStack stack) {
        return contents(stack).customEffects();
    }

    /** Replaces {@code PotionUtils.setPotion}. */
    public static ItemStack setPotion(ItemStack stack, Holder<Potion> potion) {
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.of(potion), Optional.empty(), List.of()));
        return stack;
    }

    /** Replaces {@code PotionUtils.getColor}; water renders as the default blue. */
    public static int getColor(Holder<Potion> potion) {
        return new PotionContents(potion).getColor();
    }

    /** Replaces {@code PotionUtils.getColor(ItemStack)}. */
    public static int getColor(ItemStack stack) {
        return contents(stack).getColor();
    }

    /** Convenience for the plain water potion, which is only ever referenced by handle. */
    public static Holder<Potion> water() {
        return Potions.WATER;
    }
}
