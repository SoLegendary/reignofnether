package com.solegendary.reignofnether.util;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.HashMap;
import java.util.Map;

public class EnchantmentUtil {

    public static int getRegularEnchantLevel(Enchantment enchantment, ItemStack itemStack) {
        if (itemStack.getItem() == Items.IRON_AXE && enchantment == Enchantments.SHARPNESS)
            return 3;
        if (itemStack.getItem() == Items.CROSSBOW && enchantment == Enchantments.QUICK_CHARGE)
            return 2;
        return 1;
    }

    public static void updateEnchantLevels(LivingEntity entity, boolean regularLevels) {
        ItemStack chestItem = entity.getItemBySlot(EquipmentSlot.CHEST);
        Map<Enchantment, Integer> chestEnchants = chestItem.getAllEnchantments();
        chestEnchants.replaceAll((enchant, level) -> getRegularEnchantLevel(enchant, chestItem) * (regularLevels ? 1 : 2));
        EnchantmentHelper.setEnchantments(chestEnchants, chestItem);

        ItemStack mainhandItem = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        Map<Enchantment, Integer> mainhandEnchants = mainhandItem.getAllEnchantments();
        mainhandEnchants.replaceAll((enchant, level) -> getRegularEnchantLevel(enchant, mainhandItem) * (regularLevels ? 1 : 2));
        EnchantmentHelper.setEnchantments(mainhandEnchants, mainhandItem);
    }
}
