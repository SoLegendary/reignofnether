package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class UnitItems {

    // TODO: make an actual Minecraft.Item class called "unitGiveableItem" with use() to give to neutrals to drop

    public static final UnitItem EMPTY = UnitItemBuilder.of(Items.AIR) // empty inventory slots
            .type(UnitItemType.NONE)
            .canUnitPickup(false)
            .canUnitAutopickup(false)
            .enableTooltip(false)
            .build();

    public static final UnitItem MERCHANT_TRIDENT = UnitItemBuilder.of(Items.TRIDENT)
            .type(UnitItemType.UPGRADE)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/trident.png"))
            .enchant(Enchantments.FLAMING_ARROWS, 1)
            .enchant(Enchantments.MOB_LOOTING, 1)
            .canUnitPickup(true)
            .canUnitAutopickup(true)
            .build();

    public static final UnitItem MERCHANT_SWORD = UnitItemBuilder.of(Items.NETHERITE_SWORD)
            .type(UnitItemType.UPGRADE)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_sword.png"))
            .enchant(Enchantments.FIRE_ASPECT, 1)
            .canUnitPickup(true)
            .canUnitAutopickup(true)
            .build();

    public static final UnitItem MERCHANT_GOLDEN_APPLE = UnitItemBuilder.of(Items.ENCHANTED_GOLDEN_APPLE)
            .type(UnitItemType.CONSUMABLE)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/golden_apple.png"))
            .canUnitPickup(true)
            .canUnitAutopickup(true)
            .build();

    public static final UnitItem MERCHANT_CHESTPLATE = UnitItemBuilder.of(Items.NETHERITE_CHESTPLATE)
            .type(UnitItemType.UPGRADE)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_chestplate.png"))
            .canUnitPickup(true)
            .canUnitAutopickup(true)
            .build();

    public static final UnitItem HERO_EXPERIENCE_BOTTLE = UnitItemBuilder.of(ItemRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE.get())
            .type(UnitItemType.CONSUMABLE)
            .stackQty(1)
            .sellValue(10)
            .icon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,"textures/item/hero_experience_bottle.png"))
            .descKey("item.reignofnether.hero_experience_bottle.desc")
            .build();

    public static final UnitItem DIAMOND_SWORD = UnitItemBuilder.of(Items.DIAMOND_SWORD)
            .type(UnitItemType.PASSIVE)
            .enchant(Enchantments.SHARPNESS, 1)
            .sellValue(150)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/diamond_sword.png"))
            .descKey("item.reignofnether.diamond_sword.desc")
            .pointKey("item.reignofnether.diamond_sword.point1")
            .build();

    public static final UnitItem TOTEM_OF_UNDYING = UnitItemBuilder.of(Items.TOTEM_OF_UNDYING)
            .type(UnitItemType.CONSUMABLE)
            .sellValue(250)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/totem_of_undying.png"))
            .descKey("item.reignofnether.totem_of_undying.desc")
            .pointKey("item.reignofnether.totem_of_undying.point1")
            .build();

    public static final List<UnitItem> ITEMS = List.of(
        EMPTY,
        MERCHANT_TRIDENT,
        MERCHANT_SWORD,
        MERCHANT_GOLDEN_APPLE,
        MERCHANT_CHESTPLATE,
        HERO_EXPERIENCE_BOTTLE,
        DIAMOND_SWORD,
        TOTEM_OF_UNDYING
    );
}
