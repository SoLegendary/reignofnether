package com.solegendary.reignofnether.items.unititems;

import com.solegendary.reignofnether.items.UnitItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class MerchantGoldenApple extends UnitItem {

    public MerchantGoldenApple() {
        super(
                Items.ENCHANTED_GOLDEN_APPLE,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/golden_apple.png")
        );
    }
}
