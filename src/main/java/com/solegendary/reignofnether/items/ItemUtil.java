package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.resources.ResourceSource;
import com.solegendary.reignofnether.resources.ResourceSources;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class ItemUtil {

    public static boolean isUnitItem(ItemStack itemStack) {
        return isUnitItem(itemStack.getItem());
    }

    public static boolean isUnitItem(ItemEntity entity) {
        return isUnitItem(entity.getItem().getItem());
    }

    public static boolean isUnitItem(Item item) {
        ResourceSource res = ResourceSources.getFromItem(item);
        return (res != null && res.resourceValue > 0) || getUnitItem(item) != null;
    }

    @Nullable
    public static UnitItem getUnitItem(Item item) {
        for (UnitItem unitItem : UnitItems.ITEMS)
            if (unitItem.item == item)
                return unitItem;
        return null;
    }
}
