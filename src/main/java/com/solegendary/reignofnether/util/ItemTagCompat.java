package com.solegendary.reignofnether.util;

import com.solegendary.reignofnether.util.ItemTagCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;

/**
 * Access to an {@link ItemStack}'s NBT through 1.21.1's data components.
 *
 * <p>1.21.1 removed {@code ItemStack#getTag}/{@code setTag}; the tag moved behind
 * {@link DataComponents#CUSTOM_DATA} as a {@link CustomData} component, which is immutable —
 * reading it hands back a defensive copy through {@link CustomData#copyTag()}.
 *
 * <p>The mod stores a UUID in item NBT (unit identity, fireworks data) in ~17 places, including
 * inside mixins where the {@code ItemStack} is a shadowed vanilla field. Routing all of it
 * through this helper keeps the null-handling in one place instead of twelve, and keeps the
 * mixins from having to know how components work.
 */
public final class ItemTagCompat {

    private ItemTagCompat() { }

    /** The stack's custom data as a mutable copy, or null when it has none. */
    public static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean hasTag(ItemStack stack) {
        return stack.get(DataComponents.CUSTOM_DATA) != null;
    }

    /**
     * The stack's custom data, created (and written back) if it had none - the replacement for
     * the old {@code ItemStack#getOrCreateTag()}, whose mutability callers relied on.
     */
    public static CompoundTag getOrCreateTag(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (tag == null) {
            tag = new CompoundTag();
            setTag(stack, tag);
        }
        return tag;
    }
}