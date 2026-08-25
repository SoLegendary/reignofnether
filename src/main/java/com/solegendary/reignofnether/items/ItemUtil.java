package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.items.unititems.EdibleFoodItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ItemUtil {

    public static final float HEALTH_PER_BREAD = 12;
    public static final float HEALTH_PER_CHICKEN = 18;
    public static final float HEALTH_PER_BEEF = 24;
    public static final float HEAL_PER_NUTRITION = 2.5f;

    public static boolean hasUUID(ItemStack itemStack) {
        return itemStack != null && itemStack.getTag() != null && itemStack.getTag().hasUUID("uuid");
    }

    public static UUID getUUID(ItemStack itemStack) { // if no uuid, return a random one so we don't crash but just do nothing
        return hasUUID(itemStack) ? itemStack.getTag().getUUID("uuid") : UUID.randomUUID();
    }

    public static boolean isUnitItem(ItemStack itemStack) {
        return itemStack != null && isUnitItem(itemStack.getItem());
    }

    public static boolean isUnitItem(ItemEntity entity) {
        return entity != null && isUnitItem(entity.getItem().getItem());
    }

    public static boolean isUnitItem(Item item) {
        return item != null && getUnitItem(item) != null;
    }

    @Nullable
    public static UnitItem getUnitItem(Item item) {
        if (isPreparedEdibleFood(item))
            return new EdibleFoodItem(item);
        for (UnitItem unitItem : UnitItems.ITEMS)
            if (unitItem.item == item)
                return unitItem;
        return null;
    }

    private static List<Item> edibleFoods = List.of(
            Items.COOKED_BEEF,
            Items.COOKED_CHICKEN,
            Items.COOKED_COD,
            Items.COOKED_PORKCHOP,
            Items.COOKED_RABBIT,
            Items.COOKED_SALMON,
            Items.COOKED_MUTTON,
            Items.COOKIE,
            Items.BREAD,
            Items.PUMPKIN_PIE,
            Items.MUSHROOM_STEW,
            Items.RABBIT_STEW,
            Items.BEETROOT_SOUP,
            Items.BAKED_POTATO,
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_CARROT
    );

    public static boolean isPreparedEdibleFood(Item item) {
        return item.isEdible() && edibleFoods.contains(item);
    }

    public static float getFoodHealAmount(ItemStack itemStack) {
        FoodProperties props = itemStack.getItem().getFoodProperties(itemStack, null);
        int nutrition = props != null ? props.getNutrition() : 0;
        if (itemStack.getItem() == Items.BREAD) {
            return HEALTH_PER_BREAD;
        } else if (itemStack.getItem() == Items.COOKED_CHICKEN) {
            return HEALTH_PER_CHICKEN;
        } else if (itemStack.getItem() == Items.COOKED_BEEF) {
            return HEALTH_PER_BEEF;
        } else {
            return nutrition * HEAL_PER_NUTRITION;
        }
    }
}
