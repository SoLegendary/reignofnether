package com.solegendary.reignofnether.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class StaffOfLightning extends Item {
    public StaffOfLightning(Properties pProperties) {
        super(pProperties);
    }

    public boolean isFoil(ItemStack pStack) {
        return true;
    }
}
