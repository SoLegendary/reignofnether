package com.solegendary.reignofnether.util;

import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Map;
import net.minecraft.resources.ResourceKey;

/**
 * Enchantment level bookkeeping for the mod's two-level enchantment scheme.
 *
 * <p>1.21.1 replaced the {@code Map<Enchantment, Integer>} that
 * {@code ItemStack#getAllEnchantments()} used to return with an immutable
 * {@link ItemEnchantments} component keyed by {@link Holder}. The mod rewrites enchantment
 * levels on equipped items, so it now builds a fresh component instead of mutating a map.
 */
public class EnchantmentUtil {

    /**
     * The two vanilla enchantments the mod levels differently from their default.
     *
     * <p>1.21.1 turned {@code Enchantments.SHARPNESS} and friends into {@link ResourceKey}s, so
     * the lookup is keyed by key and resolved to a holder through
     * {@link com.solegendary.reignofnether.registrars.EnchantmentRegistrar#vanilla} on each call -
     * a key-to-holder lookup is a hash lookup, not worth caching.
     */
    private static final Map<ResourceKey<Enchantment>, Item> level2Enchants = Map.of(
            Enchantments.SHARPNESS, Items.IRON_AXE,
            Enchantments.QUICK_CHARGE, Items.CROSSBOW
    );

    public static int getRegularEnchantLevel(Holder<Enchantment> enchantment, ItemStack itemStack) {
        return level2Enchants.get(enchantment.unwrapKey().orElseThrow()) == itemStack.getItem() ? 2 : 1;
    }

    /** Strips a single enchantment; 1.21.1 removed {@code ItemStack#removeEnchantment}. */
    public static void removeEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (stack.isEmpty() || !stack.isEnchanted()) return;

        ItemEnchantments.Mutable updated = new ItemEnchantments.Mutable(stack.getEnchantments());
        updated.removeIf(enchantment::equals);
        stack.set(DataComponents.ENCHANTMENTS, updated.toImmutable());
    }

    /**
     * Replaces {@code EnchantmentHelper#hasFrostWalker}, which 1.21.1 dropped.
     *
     * <p>Same test, spelled out: is Frost Walker on the entity's boots?
     */
    public static boolean hasFrostWalker(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.FEET)
                .getEnchantmentLevel(EnchantmentRegistrar.vanilla(Enchantments.FROST_WALKER)) > 0;
    }

    /** Replaces {@code EnchantmentHelper#hasBindingCurse}, also dropped in 1.21.1. */
    public static boolean hasBindingCurse(ItemStack stack) {
        return stack.getEnchantmentLevel(EnchantmentRegistrar.vanilla(Enchantments.BINDING_CURSE)) > 0;
    }

    /** Strips every enchantment; 1.21.1's {@code EnchantmentHelper#setEnchantments} takes a component. */
    public static void clearEnchantments(ItemStack stack) {
        if (stack.isEmpty() || !stack.isEnchanted()) return;
        stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    public static void updateEnchantLevels(LivingEntity entity, boolean regularLevels) {
        rescale(entity.getItemBySlot(EquipmentSlot.CHEST), regularLevels);
        rescale(entity.getItemBySlot(EquipmentSlot.MAINHAND), regularLevels);
    }

    private static void rescale(ItemStack stack, boolean regularLevels) {
        if (stack.isEmpty()) return;

        int multiplier = regularLevels ? 1 : 2;
        ItemEnchantments.Mutable updated = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

        stack.getEnchantments().entrySet().forEach(entry ->
                updated.set(entry.getKey(),
                        getRegularEnchantLevel(entry.getKey(), stack) * multiplier));

        stack.set(DataComponents.ENCHANTMENTS, updated.toImmutable());
    }
}
