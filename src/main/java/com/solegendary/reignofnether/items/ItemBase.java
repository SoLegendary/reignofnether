package com.solegendary.reignofnether.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;

public class ItemBase extends Item {

    public ItemBase() {
        // which creative mode tab the item should go into
        super(new Item.Properties().tab(CreativeModeTab.TAB_MATERIALS));
    }
}
