package com.solegendary.reignofnether.items.unititems;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.items.UnitItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class MerchantSword extends UnitItem {

    public MerchantSword() {
        super(
            Items.NETHERITE_SWORD,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_sword.png"),
            List.of(new Pair<>(Enchantments.FIRE_ASPECT, 1))
        );
    }
}
