package com.solegendary.reignofnether.items.unititems;

import com.solegendary.reignofnether.items.UnitItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class MerchantChestplate extends UnitItem {

    public MerchantChestplate() {
        super(
            Items.NETHERITE_CHESTPLATE,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_chestplate.png")
        );
    }
}
