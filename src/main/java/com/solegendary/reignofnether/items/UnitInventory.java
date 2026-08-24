package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.building.BuildingPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public interface UnitInventory {
    int MAX_INVENTORY_SIZE = 6;

    List<ItemStack> getAllItems();
    boolean isFull();
    ItemStack get(int index);
    ItemStack get(UUID uuid);
    void set(int index, ItemStack stack, UUID uuid);
    void set(int index, ItemStack stack);
    void swapSlots(int index1, int index2);
    boolean dropSlot(int index, BlockPos bp);
    boolean dropUUID(UUID uuid, BlockPos bp);
    boolean deleteUUID(UUID uuid);
    boolean tryAdding(ItemStack itemStack);
    void giveTo(UUID uuid, UnitInventory inv);
    void useOnGround(UUID uuid, BlockPos blockPos);
    void useOnEntity(UUID uuid, LivingEntity entity);
    void useOnBuilding(UUID uuid, BuildingPlacement building);
    void use(UUID uuid);
}
