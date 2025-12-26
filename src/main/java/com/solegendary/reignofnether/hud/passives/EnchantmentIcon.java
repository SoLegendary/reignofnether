package com.solegendary.reignofnether.hud.passives;

import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// "Buttons" that are just to show off passive upgrades and effects that certain units have such as:
// - Villager professions
// - Enchantments
public class EnchantmentIcon extends Button {

    public static final int ICON_SIZE = 8;
    public final Enchantment enchantment;
    public final EquipmentSlot slot;

    public EnchantmentIcon(Enchantment enchantment, EquipmentSlot slot, ResourceLocation iconRl, @Nullable List<FormattedCharSequence> tooltipLines) {
        super("Passive Icon", ICON_SIZE, iconRl, (Keybinding) null, () -> false, () -> true, () -> true, null, null, tooltipLines);
        this.enchantment = enchantment;
        this.slot = slot;
    }

    public EnchantmentIcon(Enchantment enchantment, EquipmentSlot slot, ItemStack iconItem, @Nullable List<FormattedCharSequence> tooltipLines) {
        super("Passive Icon", ICON_SIZE, null, (Keybinding) null, () -> false, () -> true, () -> true, null, null, tooltipLines);
        this.iconItem = iconItem;
        this.enchantment = enchantment;
        this.slot = slot;
    }
}
