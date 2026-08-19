package com.solegendary.reignofnether.items.unititems;

import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.items.UnitItemBuilder;
import com.solegendary.reignofnether.items.UnitItemType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public class InspiringHorn extends UnitItem {

    public InspiringHorn() {
        super(UnitItemBuilder.of(Items.GOAT_HORN)
                .type(UnitItemType.PASSIVE)
                .enchant(Enchantments.SHARPNESS, 1)
                .sellValue(150)
                .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/diamond_sword.png"))
                .descKey("item.reignofnether.diamond_sword.desc")
                .pointKey("item.reignofnether.diamond_sword.point1")
                .canUnitPickup(false)
                .canUnitAutopickup(false)
        );
    }
}
