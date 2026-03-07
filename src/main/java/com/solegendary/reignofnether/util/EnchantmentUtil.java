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

    private static final HashMap<Enchantment, Item> level2Enchants = new HashMap<>();

    static {
        level2Enchants.put(Enchantments.SHARPNESS, Items.IRON_AXE);
        level2Enchants.put(Enchantments.QUICK_CHARGE, Items.CROSSBOW);
    }

    public static int getRegularEnchantLevel(Enchantment enchantment, ItemStack itemStack) {
        if (level2Enchants.get(enchantment) == itemStack.getItem()) {
            return 2;
        }
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
