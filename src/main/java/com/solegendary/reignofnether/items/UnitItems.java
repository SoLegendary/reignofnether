package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.items.unititems.EmptyUnitItem;
import com.solegendary.reignofnether.items.unititems.MerchantEquipmentItem;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class UnitItems {

    public static final UnitItem EMPTY = new EmptyUnitItem();

    public static final UnitItem MERCHANT_TRIDENT = new MerchantEquipmentItem(UnitItemBuilder.of(Items.TRIDENT)
            .descId("merchant_trident")
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/trident.png"))
            .enchant(Enchantments.FLAMING_ARROWS, 1)
            .enchant(Enchantments.MOB_LOOTING, 1),
            le -> le instanceof HeadhunterUnit headhunterUnit && !headhunterUnit.hasFlameTrident());

    public static final UnitItem MERCHANT_SWORD = new MerchantEquipmentItem(UnitItemBuilder.of(Items.NETHERITE_SWORD)
            .descId("merchant_sword")
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_sword.png"))
            .enchant(Enchantments.FIRE_ASPECT, 1),
            le -> le instanceof BruteUnit bruteUnit && !bruteUnit.hasEnchantedNetheriteSword());

    public static final UnitItem MERCHANT_CHESTPLATE = new MerchantEquipmentItem(UnitItemBuilder.of(Items.NETHERITE_CHESTPLATE)
            .descId("merchant_chestplate")
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_chestplate.png")),
            le -> (le instanceof BruteUnit bruteUnit && !bruteUnit.hasNetheriteChestplate()) ||
                    (le instanceof HeadhunterUnit headhunterUnit && !headhunterUnit.hasNetheriteChestplate()) ||
                    (le instanceof MarauderUnit marauderUnit && !marauderUnit.hasNetheriteChestplate()) ||
                    (le instanceof HoglinUnit && !(le instanceof ArmouredHoglinUnit)));

    private static final int EXPERIENCE_BOTTLE_EXP_VALUE = 100;
    public static final UnitItem HERO_EXPERIENCE_BOTTLE = UnitItemBuilder.of(ItemRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE.get())
            .descId("hero_experience_bottle")
            .type(UnitItemType.CONSUMABLE)
            .buyCost(100)
            .sellValue(50)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft","textures/item/experience_bottle.png"))
            .build();

    public static final UnitItem TOTEM_OF_UNDYING = UnitItemBuilder.of(Items.TOTEM_OF_UNDYING)
            .descId("totem_of_undying")
            .type(UnitItemType.CONSUMABLE)
            .buyCost(500)
            .sellValue(250)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/totem_of_undying.png"))
            .build();

    public static final UnitItem STAFF_OF_LIGHTNING = UnitItemBuilder.of(ItemRegistrar.STAFF_OF_LIGHTNING.get())
            .descId("staff_of_lightning")
            .type(UnitItemType.ACTIVE)
            .buyCost(600)
            .sellValue(300)
            .cooldownTicks(600)
            .manaCost(10)
            .range(10)
            .onUseGround(((unit, blockPos) -> {
                LivingEntity le = (LivingEntity) unit;
                BlockPos bp = MiscUtil.getHighestNonAirBlock(le.level(), blockPos);
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(le.level());
                if (bolt != null) {
                    bolt.moveTo(bp.getX(), bp.getY(), bp.getZ());
                    le.level().addFreshEntity(bolt);
                    if (le.level().getBlockState(blockPos.above()).isAir())
                        le.level().setBlockAndUpdate(blockPos.above(), Blocks.FIRE.defaultBlockState());
                    return true;
                }
                return false;
            }))
            .build();

    public static final List<UnitItem> ITEMS = List.of(
        EMPTY,
        MERCHANT_TRIDENT,
        MERCHANT_SWORD,
        MERCHANT_CHESTPLATE,
        HERO_EXPERIENCE_BOTTLE,
        TOTEM_OF_UNDYING,
        STAFF_OF_LIGHTNING
    );
}
