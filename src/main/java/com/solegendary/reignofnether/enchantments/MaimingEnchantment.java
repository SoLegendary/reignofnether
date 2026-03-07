package com.solegendary.reignofnether.enchantments;

import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class MaimingEnchantment extends Enchantment {

    public static final int SLOWNESS_DURATION = 5 * ResourceCost.TICKS_PER_SECOND;

    public MaimingEnchantment() {
        super(
                Rarity.RARE,
                EnchantmentCategory.WEAPON,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND}
        );
    }
}