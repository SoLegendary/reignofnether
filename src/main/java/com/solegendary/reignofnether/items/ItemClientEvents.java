package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;

public class ItemClientEvents {

    // items moused over
    private static final ArrayList<ItemEntity> preselectedItems = new ArrayList<>();

    public static void addPreselectedItem(ItemEntity itemEntity) {
        if (!FogOfWarClientEvents.isInBrightChunk(itemEntity))
            return;
        preselectedItems.add(itemEntity);
    }
}
