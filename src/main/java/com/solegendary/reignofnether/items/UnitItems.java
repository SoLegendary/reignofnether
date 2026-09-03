package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.items.unititems.EmptyUnitItem;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;
import java.util.UUID;

public class UnitItems {

    public static final UnitItem EMPTY = new EmptyUnitItem();

    public static final UnitItem MERCHANT_TRIDENT = new MerchantEquipmentItem(UnitItemBuilder.of(Items.TRIDENT)
            .uuid("f44bbd1f-cd29-4a08-849e-fa9e6a64eba8")
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/trident.png"))
            .desc(descStr("item.reignofnether.merchant_trident.desc"))
            .enchant(Enchantments.FLAMING_ARROWS, 1)
            .enchant(Enchantments.MOB_LOOTING, 1),
            le -> le instanceof HeadhunterUnit headhunterUnit && !headhunterUnit.hasFlameTrident());

    public static final UnitItem MERCHANT_SWORD = new MerchantEquipmentItem(UnitItemBuilder.of(Items.NETHERITE_SWORD)
            .uuid("e8be3e96-97a5-416c-b4f4-16664a79951d")
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_sword.png"))
            .desc(descStr("item.reignofnether.merchant_sword.desc"))
            .enchant(Enchantments.FIRE_ASPECT, 1),
            le -> le instanceof BruteUnit bruteUnit && !bruteUnit.hasEnchantedNetheriteSword());

    public static final UnitItem MERCHANT_CHESTPLATE = new MerchantEquipmentItem(UnitItemBuilder.of(Items.NETHERITE_CHESTPLATE)
            .uuid("1ceab5a6-a531-4928-a098-e9aa886ddfd2")
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_chestplate.png"))
            .desc(descStr("item.reignofnether.merchant_chestplate.desc"))
            .pointDesc(descStr("item.reignofnether.merchant_chestplate.point1")),
            le -> (le instanceof BruteUnit bruteUnit && !bruteUnit.hasNetheriteChestplate()) ||
                    (le instanceof HeadhunterUnit headhunterUnit && !headhunterUnit.hasNetheriteChestplate()) ||
                    (le instanceof MarauderUnit marauderUnit && !marauderUnit.hasNetheriteChestplate()) ||
                    (le instanceof HoglinUnit && !(le instanceof ArmouredHoglinUnit)));


    private static final int EXPERIENCE_BOTTLE_EXP_VALUE = 100;
    public static final UnitItem HERO_EXPERIENCE_BOTTLE = UnitItemBuilder.of(ItemRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE.get())
            .uuid("2713c95e-46de-4021-9d27-58cc1ccaa1b3")
            .type(UnitItemType.CONSUMABLE)
            .buyCost(100)
            .sellValue(50)
            .icon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,"textures/item/thrown_hero_experience_bottle.png"))
            .desc(descStr("item.reignofnether.hero_experience_bottle.desc", EXPERIENCE_BOTTLE_EXP_VALUE))
            .build();

    public static final UnitItem DIAMOND_SWORD = UnitItemBuilder.of(Items.DIAMOND_SWORD)
            .uuid("ff7b52b8-2689-452e-8ac1-2527b11aa9f7")
            .type(UnitItemType.PASSIVE)
            .enchant(Enchantments.SHARPNESS, 1)
            .sellValue(150)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/diamond_sword.png"))
            .desc(descStr("item.reignofnether.diamond_sword.desc"))
            .pointDesc(descStr("item.reignofnether.diamond_sword.point1"))
            .build();

    public static final UnitItem TOTEM_OF_UNDYING = UnitItemBuilder.of(Items.TOTEM_OF_UNDYING)
            .uuid("8945cc89-8804-4aa7-9b0b-c1f3a49d75f3")
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
