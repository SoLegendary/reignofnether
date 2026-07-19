package com.solegendary.reignofnether.items.unititems;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.items.UnitItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class MerchantTrident extends UnitItem {

    public MerchantTrident() {
        super(
            Items.TRIDENT,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/trident.png"),
            List.of(
                new Pair<>(Enchantments.FLAMING_ARROWS, 1),
                new Pair<>(Enchantments.MOB_LOOTING, 1)
            )
        );
    }
}
