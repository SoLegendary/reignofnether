package com.solegendary.ageofcraft.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

public class ItemBase extends Item {

    public ItemBase() {
        // which creative mode tab the item should go into
        super(new Item.Properties().tab(ItemGroup.TAB_MATERIALS));
    }
}
