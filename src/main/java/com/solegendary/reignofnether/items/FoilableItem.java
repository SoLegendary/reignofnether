package com.solegendary.reignofnether.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FoilableItem extends Item {

    private final boolean isFoil;

    public FoilableItem(Properties pProperties) {
        super(pProperties);
        this.isFoil = false;
    }

    public FoilableItem(Properties pProperties, boolean isFoil) {
        super(pProperties);
        this.isFoil = isFoil;
    }

    public boolean isFoil(ItemStack pStack) {
        return isFoil;
    }
}
