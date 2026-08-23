package com.solegendary.reignofnether.items;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface UnitInventory {
    int MAX_INVENTORY_SIZE = 6;

    List<ItemStack> getAllItems();
    ItemStack get(int index);
    void setSlot(int index, ItemStack stack);
    void swapSlots(int index1, int index2);
    void dropSlot(int index, BlockPos bp);
    boolean tryAdding(ItemStack itemStack);
    void giveTo(int index, UnitInventory inv);
    boolean isFull();
}
