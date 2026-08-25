package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.items.unititems.EmptyUnitItem;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class UnitItems {

    public static final UnitItem EMPTY = new EmptyUnitItem();

    public static final UnitItem MERCHANT_TRIDENT = UnitItemBuilder.of(Items.TRIDENT)
            .type(UnitItemType.UPGRADE)
            .sellValue(50)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/trident.png"))
            .desc(descStr("item.reignofnether.merchant_trident.desc"))
            .enchant(Enchantments.FLAMING_ARROWS, 1)
            .enchant(Enchantments.MOB_LOOTING, 1)
            .build();

    public static final UnitItem MERCHANT_SWORD = UnitItemBuilder.of(Items.NETHERITE_SWORD)
            .type(UnitItemType.UPGRADE)
            .sellValue(50)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_sword.png"))
            .desc(descStr("item.reignofnether.merchant_sword.desc"))
            .enchant(Enchantments.FIRE_ASPECT, 1)
            .build();

    public static final UnitItem MERCHANT_CHESTPLATE = UnitItemBuilder.of(Items.NETHERITE_CHESTPLATE)
            .type(UnitItemType.UPGRADE)
            .sellValue(50)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_chestplate.png"))
            .desc(descStr("item.reignofnether.merchant_chestplate.desc"))
            .pointDesc(descStr("item.reignofnether.merchant_chestplate.point1"))
            .build();

    private static final int EXPERIENCE_BOTTLE_EXP_VALUE = 100;
    public static final UnitItem HERO_EXPERIENCE_BOTTLE = UnitItemBuilder.of(ItemRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE.get())
            .type(UnitItemType.CONSUMABLE)
            .buyCost(100)
            .sellValue(50)
            .icon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,"textures/item/thrown_hero_experience_bottle.png"))
            .desc(descStr("item.reignofnether.hero_experience_bottle.desc", EXPERIENCE_BOTTLE_EXP_VALUE))
            .build();

    public static final UnitItem DIAMOND_SWORD = UnitItemBuilder.of(Items.DIAMOND_SWORD)
            .type(UnitItemType.PASSIVE)
            .enchant(Enchantments.SHARPNESS, 1)
            .sellValue(150)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/diamond_sword.png"))
            .desc(descStr("item.reignofnether.diamond_sword.desc"))
            .pointDesc(descStr("item.reignofnether.diamond_sword.point1"))
            .build();

    public static final UnitItem TOTEM_OF_UNDYING = UnitItemBuilder.of(Items.TOTEM_OF_UNDYING)
            .type(UnitItemType.CONSUMABLE)
            .sellValue(250)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/totem_of_undying.png"))
            .desc(descStr("item.reignofnether.totem_of_undying.desc"))
            .pointDesc(descStr("item.reignofnether.totem_of_undying.point1"))
            .build();

    public static String descStr(String descKey, Object... pArgs) {
        return Component.translatable(descKey, pArgs).getString();
    }

    public static final List<UnitItem> ITEMS = List.of(
        EMPTY,
        MERCHANT_TRIDENT,
        MERCHANT_SWORD,
        MERCHANT_CHESTPLATE,
        HERO_EXPERIENCE_BOTTLE,
        DIAMOND_SWORD,
        TOTEM_OF_UNDYING
    );
}
