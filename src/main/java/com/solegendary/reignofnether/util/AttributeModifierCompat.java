package com.solegendary.reignofnether.util;

import com.solegendary.reignofnether.util.AttributeModifierCompat;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Adds attribute modifiers to an item the way 1.21.1 expects.
 *
 * <p>1.21.1 deleted {@code ItemStack#addAttributeModifier}; the modifiers now live in the
 * {@link DataComponents#ATTRIBUTE_MODIFIERS} component and are addressed by an
 * {@link EquipmentSlotGroup} rather than a single slot. The mod grants a flat damage bonus when
 * it hands a unit its starting weapon, so the component is rebuilt from whatever the stack
 * already had plus the new entry.
 *
 * <p>The id has to be a {@link ResourceLocation} now (1.20.1 used a random {@code UUID}). A
 * random id would work but would also regenerate on every world load and break tooltips and
 * comparisons, so it is derived from the attribute instead - stable across runs, which is what
 * the old UUID constants were for.
 */
public final class AttributeModifierCompat {

    private AttributeModifierCompat() { }

    public static void addModifier(ItemStack stack, Holder<Attribute> attribute, double amount,
                                   AttributeModifier.Operation operation, EquipmentSlot slot) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        ItemAttributeModifiers existing = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (existing != null) {
            for (ItemAttributeModifiers.Entry entry : existing.modifiers()) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                ReignOfNether.MOD_ID, attribute.unwrapKey().map(k -> k.location().getPath()).orElse("modifier"));

        // 1.21.1 replaced the per-slot EquipmentSlot with the coarser EquipmentSlotGroup.
        builder.add(attribute, new AttributeModifier(id, amount, operation), EquipmentSlotGroup.MAINHAND);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }
}