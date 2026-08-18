package com.solegendary.reignofnether.items;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class UnitItems {

    public static final UnitItem MERCHANT_TRIDENT = UnitItemBuilder.of(Items.TRIDENT)
            .type(UnitItemType.UNIT_UPGRADE)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/trident.png"))
            .enchant(Enchantments.FLAMING_ARROWS, 1)
            .enchant(Enchantments.MOB_LOOTING, 1)
            .build();

    public static final UnitItem MERCHANT_SWORD = UnitItemBuilder.of(Items.NETHERITE_SWORD)
            .type(UnitItemType.UNIT_UPGRADE)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_sword.png"))
            .enchant(Enchantments.FIRE_ASPECT, 1)
            .build();

    public static final UnitItem MERCHANT_GOLDEN_APPLE = UnitItemBuilder.of(Items.ENCHANTED_GOLDEN_APPLE)
            .type(UnitItemType.CONSUMABLE)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/golden_apple.png"))
            .build();

    public static final UnitItem MERCHANT_CHESTPLATE = UnitItemBuilder.of(Items.NETHERITE_CHESTPLATE)
            .type(UnitItemType.UNIT_UPGRADE)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_chestplate.png"))
            .build();

    public static final List<UnitItem> ITEMS = List.of(
        MERCHANT_TRIDENT,
        MERCHANT_SWORD,
        MERCHANT_GOLDEN_APPLE,
        MERCHANT_CHESTPLATE
    );
}
