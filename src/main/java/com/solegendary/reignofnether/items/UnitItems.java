package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.entities.ThrownHeroExperienceBottle;
import com.solegendary.reignofnether.items.unititems.EmptyUnitItem;
import com.solegendary.reignofnether.items.unititems.MerchantEquipmentItem;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.*;

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
            .pointDesc("item.reignofnether.hero_experience_bottle.point1", EXPERIENCE_BOTTLE_EXP_VALUE)
            .buyCost(100)
            .sellValue(50)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft","textures/item/experience_bottle.png"))
            .onUse(unit -> {
                Level level = ((Entity) unit).level();
                ThrownHeroExperienceBottle bottle = new ThrownHeroExperienceBottle(level, ((LivingEntity) unit));
                //$$4.setItem();
                level.addFreshEntity(bottle);
                return true;
            })
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

    public static final UnitItem HEART_MEDALLION = UnitItemBuilder.of(ItemRegistrar.HEART_MEDALLION.get())
            .descId("heart_medallion")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200)
            .attribute(Attributes.MAX_HEALTH, 50, ADDITION)
            .build();

    public static final UnitItem AZURE_MEDALLION = UnitItemBuilder.of(ItemRegistrar.AZURE_MEDALLION.get())
            .descId("azure_medallion")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200)
            .attribute(AttributeRegistrar.BASE_MAX_MANA.get(), 50, ADDITION)
            .build();

    public static final UnitItem SOUL_COLLECTOR = UnitItemBuilder.of(ItemRegistrar.SOUL_COLLECTOR.get())
            .descId("soul_collector")
            .type(UnitItemType.PASSIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem BROADSWORD = UnitItemBuilder.of(ItemRegistrar.BROADSWORD.get())
            .descId("broadsword")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200)
            .attribute(AttributeRegistrar.ATTACK_DAMAGE.get(), 0.25, MULTIPLY_BASE)
            .build();

    public static final UnitItem KATANA = UnitItemBuilder.of(ItemRegistrar.KATANA.get())
            .descId("katana")
            .type(UnitItemType.PASSIVE)
            .buyCost(500)
            .sellValue(250) // TODO
            .build();

    public static final UnitItem HEARTSTEALER = UnitItemBuilder.of(ItemRegistrar.HEARTSTEALER.get())
            .descId("heartstealer")
            .type(UnitItemType.PASSIVE)
            .buyCost(500)
            .sellValue(250)
            .build();

    public static final UnitItem SOUL_SCYTHE = UnitItemBuilder.of(ItemRegistrar.SOUL_SCYTHE.get())
            .descId("soul_scythe")
            .type(UnitItemType.PASSIVE)
            .buyCost(500)
            .sellValue(250) // TODO
            .build();

    public static final UnitItem POWERSHAKER = UnitItemBuilder.of(ItemRegistrar.POWERSHAKER.get())
            .descId("powershaker")
            .type(UnitItemType.PASSIVE)
            .buyCost(500)
            .sellValue(250) // TODO
            .build();

    public static final UnitItem BOOTS_OF_SWIFTNESS = UnitItemBuilder.of(ItemRegistrar.BOOTS_OF_SWIFTNESS.get())
            .descId("boots_of_swiftness")
            .type(UnitItemType.PASSIVE)
            .buyCost(500)
            .sellValue(250)
            .attribute(Attributes.MOVEMENT_SPEED, 0.04, ADDITION)
            .build();

    public static final UnitItem SPARKLER = UnitItemBuilder.of(ItemRegistrar.SPARKLER.get())
            .descId("sparkler")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200)
            .attribute(Attributes.ATTACK_SPEED, 0.20, MULTIPLY_BASE)
            .build();

    public static final UnitItem IRON_HIDE_AMULET = UnitItemBuilder.of(ItemRegistrar.IRON_HIDE_AMULET.get())
            .descId("iron_hide_amulet")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200)
            .attribute(Attributes.ARMOR, 5, ADDITION)
            .build();

    public static final UnitItem LIGHT_FEATHER = UnitItemBuilder.of(ItemRegistrar.LIGHT_FEATHER.get())
            .descId("light_feather")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(250)
            .attribute(AttributeRegistrar.EVASION_CHANCE.get(), 0.15, ADDITION)
            .build();

    public static final UnitItem SPYGLASS = UnitItemBuilder.of(Items.SPYGLASS)
            .descId("spyglass")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200)
            .attribute(AttributeRegistrar.SIGHT_RANGE.get(), 4, ADDITION)
            .build();

    public static final UnitItem FROST_WALKER_BOOTS = UnitItemBuilder.of(ItemRegistrar.FROST_WALKER_BOOTS.get())
            .descId("frost_walker_boots")
            .type(UnitItemType.PASSIVE)
            .buyCost(750)
            .sellValue(375) // TODO
            .build();

    public static final UnitItem MAGMA_WALKER_BOOTS = UnitItemBuilder.of(ItemRegistrar.MAGMA_WALKER_BOOTS.get())
            .descId("magma_walker_boots")
            .type(UnitItemType.PASSIVE)
            .buyCost(750)
            .sellValue(375) // TODO
            .build();

    public static final UnitItem SATCHEL_OF_SNACKS = UnitItemBuilder.of(ItemRegistrar.SATCHEL_OF_SNACKS.get())
            .descId("satchel_of_snacks")
            .type(UnitItemType.PASSIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem BUZZY_NEST = UnitItemBuilder.of(ItemRegistrar.BUZZY_NEST.get())
            .descId("buzzy_nest")
            .type(UnitItemType.PASSIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem BELL_OF_ARMS = UnitItemBuilder.of(Items.BELL)
            .descId("bell_of_arms")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem HEALTH_POTION = UnitItemBuilder.of(ItemRegistrar.HEALTH_POTION.get())
            .descId("health_potion")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem MANA_POTION = UnitItemBuilder.of(ItemRegistrar.MANA_POTION.get())
            .descId("mana_potion")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem GHOST_CLOAK = UnitItemBuilder.of(ItemRegistrar.GHOST_CLOAK.get())
            .descId("ghost_cloak")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem GONG_OF_WEAKENING = UnitItemBuilder.of(ItemRegistrar.GONG_OF_WEAKENING.get())
            .descId("gong_of_weakening")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem ICE_WAND = UnitItemBuilder.of(ItemRegistrar.ICE_WAND.get())
            .descId("ice_wand")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem UPDRAFT_TOME = UnitItemBuilder.of(ItemRegistrar.UPDRAFT_TOME.get())
            .descId("updraft_tome")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem TOME_OF_DUPLICATION = UnitItemBuilder.of(ItemRegistrar.TOME_OF_DUPLICATION.get())
            .descId("tome_of_duplication")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem SHADOW_SHIFTER = UnitItemBuilder.of(ItemRegistrar.SHADOW_SHIFTER.get())
            .descId("shadow_shifter")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem POCKET_PORTAL = UnitItemBuilder.of(ItemRegistrar.POCKET_PORTAL.get())
            .descId("pocket_portal")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem WAR_HORN = UnitItemBuilder.of(ItemRegistrar.WAR_HORN.get())
            .descId("war_horn")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem TOTEM_OF_REGENERATION = UnitItemBuilder.of(ItemRegistrar.TOTEM_OF_REGENERATION.get())
            .descId("totem_of_regeneration")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem TOTEM_OF_SHIELDING = UnitItemBuilder.of(ItemRegistrar.TOTEM_OF_SHIELDING.get())
            .descId("totem_of_shielding")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem TOTEM_OF_PROTECTION = UnitItemBuilder.of(ItemRegistrar.TOTEM_OF_PROTECTION.get())
            .descId("totem_of_protection")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final UnitItem TOTEM_OF_CASTING = UnitItemBuilder.of(ItemRegistrar.TOTEM_OF_CASTING.get())
            .descId("totem_of_casting")
            .type(UnitItemType.ACTIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    public static final List<UnitItem> ITEMS = List.of(
            EMPTY,
            MERCHANT_TRIDENT,
            MERCHANT_SWORD,
            MERCHANT_CHESTPLATE,
            HERO_EXPERIENCE_BOTTLE,
            TOTEM_OF_UNDYING,
            STAFF_OF_LIGHTNING,
            HEART_MEDALLION,
            AZURE_MEDALLION,
            SOUL_COLLECTOR,
            BROADSWORD,
            KATANA,
            HEARTSTEALER,
            SOUL_SCYTHE,
            POWERSHAKER,
            BOOTS_OF_SWIFTNESS,
            SPARKLER,
            IRON_HIDE_AMULET,
            LIGHT_FEATHER,
            SPYGLASS,
            FROST_WALKER_BOOTS,
            MAGMA_WALKER_BOOTS,
            BELL_OF_ARMS,
            SATCHEL_OF_SNACKS,
            BUZZY_NEST,
            HEALTH_POTION,
            MANA_POTION,
            GHOST_CLOAK,
            GONG_OF_WEAKENING,
            ICE_WAND,
            UPDRAFT_TOME,
            TOME_OF_DUPLICATION,
            SHADOW_SHIFTER,
            POCKET_PORTAL,
            WAR_HORN,
            TOTEM_OF_REGENERATION,
            TOTEM_OF_SHIELDING,
            TOTEM_OF_PROTECTION,
            TOTEM_OF_CASTING
    );
}
