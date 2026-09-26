package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.entities.ThrownHeroExperienceBottle;
import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.items.unititems.EmptyUnitItem;
import com.solegendary.reignofnether.items.unititems.MerchantEquipmentItem;
import com.solegendary.reignofnether.items.unititems.TotemItem;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.ParticleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE;

public class UnitItems {

    public static final UnitItem EMPTY = new EmptyUnitItem();

    public static final UnitItem MERCHANT_TRIDENT = new MerchantEquipmentItem(UnitItemBuilder.of(Items.TRIDENT)
            .descId("merchant_trident")
            .type(UnitItemType.UPGRADE)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/trident.png"))
            .enchant(Enchantments.FLAMING_ARROWS, 1)
            .enchant(Enchantments.MOB_LOOTING, 1),
            le -> le instanceof HeadhunterUnit headhunterUnit && !headhunterUnit.hasFlameTrident());

    public static final UnitItem MERCHANT_SWORD = new MerchantEquipmentItem(UnitItemBuilder.of(Items.NETHERITE_SWORD)
            .descId("merchant_sword")
            .type(UnitItemType.UPGRADE)
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_sword.png"))
            .enchant(Enchantments.FIRE_ASPECT, 1),
            le -> le instanceof BruteUnit bruteUnit && !bruteUnit.hasEnchantedNetheriteSword());

    public static final UnitItem MERCHANT_CHESTPLATE = new MerchantEquipmentItem(UnitItemBuilder.of(Items.NETHERITE_CHESTPLATE)
            .descId("merchant_chestplate")
            .type(UnitItemType.UPGRADE)
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

    public static final UnitItem STAFF_OF_LIGHTNING = UnitItemBuilder.of(ItemRegistrar.STAFF_OF_LIGHTNING.get())
            .descId("staff_of_lightning")
            .type(UnitItemType.ACTIVE)
            .buyCost(600)
            .sellValue(300)
            .cooldownTicks(60)
            .manaCost(10)
            .range(10)
            .doCastAnimation()
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
            .buyCost(300)
            .sellValue(150)
            .attribute(Attributes.MAX_HEALTH, 50, ADDITION)
            .build();

    public static final UnitItem AZURE_MEDALLION = UnitItemBuilder.of(ItemRegistrar.AZURE_MEDALLION.get())
            .descId("azure_medallion")
            .type(UnitItemType.PASSIVE)
            .buyCost(300)
            .sellValue(150)
            .attribute(AttributeRegistrar.BASE_MAX_MANA.get(), 50, ADDITION)
            .build();

    public static final UnitItem IRON_HIDE_AMULET = UnitItemBuilder.of(ItemRegistrar.IRON_HIDE_AMULET.get())
            .descId("iron_hide_amulet")
            .type(UnitItemType.PASSIVE)
            .buyCost(300)
            .sellValue(150)
            .attribute(Attributes.ARMOR, 5, ADDITION)
            .build();

    public static final UnitItem SOUL_COLLECTOR = UnitItemBuilder.of(ItemRegistrar.SOUL_COLLECTOR.get())
            .descId("soul_collector")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200) // TODO
            .build();

    public static final UnitItem BROADSWORD = UnitItemBuilder.of(ItemRegistrar.BROADSWORD.get())
            .descId("broadsword")
            .type(UnitItemType.PASSIVE)
            .buyCost(300)
            .sellValue(150)
            .attribute(AttributeRegistrar.ATTACK_DAMAGE.get(), 0.25, MULTIPLY_BASE)
            .build();

    public static final UnitItem KATANA = UnitItemBuilder.of(ItemRegistrar.KATANA.get())
            .descId("katana")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200)
            .attribute(AttributeRegistrar.CRITICAL_HIT_CHANCE.get(), 0.17, ADDITION)
            .build();

    public static final UnitItem HEARTSTEALER = UnitItemBuilder.of(ItemRegistrar.HEARTSTEALER.get())
            .descId("heartstealer")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200)
            .attribute(AttributeRegistrar.LIFESTEAL.get(), 0.15, ADDITION)
            .build();

    public static final UnitItem SOUL_SCYTHE = UnitItemBuilder.of(ItemRegistrar.SOUL_SCYTHE.get())
            .descId("soul_scythe")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200)
            .attribute(AttributeRegistrar.MANA_ON_HIT.get(), 0.25, ADDITION)
            .build();

    public static final UnitItem POWERSHAKER = UnitItemBuilder.of(ItemRegistrar.POWERSHAKER.get())
            .descId("powershaker")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200)
            .attribute(AttributeRegistrar.EXPLOSIVE_HIT_CHANCE.get(), 0.2, ADDITION)
            .build();

    public static final UnitItem GREAT_HAMMER = UnitItemBuilder.of(ItemRegistrar.GREAT_HAMMER.get())
            .descId("great_hammer")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200)
            .attribute(AttributeRegistrar.BUILDING_DAMAGE_BONUS.get(), 2.0, ADDITION)
            .build();

    public static final UnitItem SPARKLER = UnitItemBuilder.of(ItemRegistrar.SPARKLER.get())
            .descId("sparkler")
            .type(UnitItemType.PASSIVE)
            .buyCost(300)
            .sellValue(150)
            .attribute(Attributes.ATTACK_SPEED, 0.20, MULTIPLY_BASE)
            .build();

    public static final UnitItem LIGHT_FEATHER = UnitItemBuilder.of(ItemRegistrar.LIGHT_FEATHER.get())
            .descId("light_feather")
            .type(UnitItemType.PASSIVE)
            .buyCost(300)
            .sellValue(150)
            .attribute(AttributeRegistrar.EVASION_CHANCE.get(), 0.15, ADDITION)
            .build();

    public static final UnitItem SPYGLASS = UnitItemBuilder.of(Items.SPYGLASS)
            .descId("spyglass")
            .type(UnitItemType.PASSIVE)
            .buyCost(300)
            .sellValue(150)
            .attribute(AttributeRegistrar.SIGHT_RANGE.get(), 4, ADDITION)
            .build();

    public static final UnitItem BOOTS_OF_SWIFTNESS = UnitItemBuilder.of(ItemRegistrar.BOOTS_OF_SWIFTNESS.get())
            .descId("boots_of_swiftness")
            .type(UnitItemType.PASSIVE)
            .buyCost(300)
            .sellValue(150)
            .attribute(Attributes.MOVEMENT_SPEED, 0.04, ADDITION)
            .build();

    public static final UnitItem FROST_WALKER_BOOTS = UnitItemBuilder.of(ItemRegistrar.FROST_WALKER_BOOTS.get())
            .descId("frost_walker_boots")
            .type(UnitItemType.PASSIVE)
            .buyCost(500)
            .sellValue(250) // TODO
            .build();

    public static final UnitItem MAGMA_WALKER_BOOTS = UnitItemBuilder.of(ItemRegistrar.MAGMA_WALKER_BOOTS.get())
            .descId("magma_walker_boots")
            .type(UnitItemType.PASSIVE)
            .buyCost(500)
            .sellValue(250) // TODO
            .build();

    public static final UnitItem SATCHEL_OF_SNACKS = UnitItemBuilder.of(ItemRegistrar.SATCHEL_OF_SNACKS.get())
            .descId("satchel_of_snacks")
            .type(UnitItemType.PASSIVE)
            .buyCost(400)
            .sellValue(200) // TODO
            .build();

    public static final UnitItem BUZZY_NEST = UnitItemBuilder.of(ItemRegistrar.BUZZY_NEST.get())
            .descId("buzzy_nest")
            .type(UnitItemType.PASSIVE)
            .buyCost(0)
            .sellValue(0) // TODO
            .build();

    private static final float HEALTH_POTION_RESTORE_AMOUNT = 50f;
    public static final UnitItem HEALTH_POTION = UnitItemBuilder.of(ItemRegistrar.HEALTH_POTION.get())
            .descId("health_potion")
            .type(UnitItemType.CONSUMABLE)
            .buyCost(150)
            .sellValue(75) // TODO
            .pointDesc("item.reignofnether.health_potion.point1", HEALTH_POTION_RESTORE_AMOUNT)
            .build();

    private static final float MANA_POTION_RESTORE_AMOUNT = 50f;
    public static final UnitItem MANA_POTION = UnitItemBuilder.of(ItemRegistrar.MANA_POTION.get())
            .descId("mana_potion")
            .type(UnitItemType.CONSUMABLE)
            .buyCost(150)
            .sellValue(75) // TODO
            .pointDesc("item.reignofnether.mana_potion.point1", MANA_POTION_RESTORE_AMOUNT)
            .build();

    private static final int GHOST_CLOAK_DURATION_SECONDS = 15;
    public static final UnitItem GHOST_CLOAK = UnitItemBuilder.of(ItemRegistrar.GHOST_CLOAK.get())
            .descId("ghost_cloak")
            .type(UnitItemType.ACTIVE)
            .buyCost(400)
            .sellValue(200)
            .pointDesc("item.reignofnether.ghost_cloak.point1", GHOST_CLOAK_DURATION_SECONDS)
            .manaCost(25)
            .cooldownTicks(60 * 20)
            .noBehaviourReset()
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                boolean result = le.addEffect(new MobEffectInstance(MobEffectRegistrar.PHASING.get(), GHOST_CLOAK_DURATION_SECONDS * 20, 0, false, true));
                le.addEffect(new MobEffectInstance(MobEffectRegistrar.MINOR_MOVEMENT_SPEED.get(), GHOST_CLOAK_DURATION_SECONDS * 20, 1, true, false));
                if (result && !le.level().isClientSide())
                    SoundClientboundPacket.playSoundAtPos(SoundAction.GHOST_CLOAK, le.blockPosition(), 1.5f);
                return result;
            })
            .build();

    private static final int GONG_OF_WEAKENING_DURATION_SECONDS = 10;
    private static final int GONG_OF_WEAKENING_RADIUS = 10;
    public static final UnitItem GONG_OF_WEAKENING = UnitItemBuilder.of(ItemRegistrar.GONG_OF_WEAKENING.get())
            .descId("gong_of_weakening")
            .type(UnitItemType.ACTIVE)
            .buyCost(500)
            .sellValue(250)
            .pointDesc("item.reignofnether.gong_of_weakening.point1", GONG_OF_WEAKENING_DURATION_SECONDS)
            .radius(10)
            //.manaCost(50)
            //.cooldownTicks(120 * 20)
            .showRadiusCircle()
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    for (Mob mob : MiscUtil.getEntitiesWithinRange(le.getEyePosition(), GONG_OF_WEAKENING_RADIUS, Mob.class, le.level())) {
                        if (UnitServerEvents.getRl(unit, mob) != Relationship.FRIENDLY) {
                            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, GONG_OF_WEAKENING_DURATION_SECONDS, 0, false, true));
                            mob.addEffect(new MobEffectInstance(MobEffectRegistrar.DAMAGE_TAKEN_INCREASE.get(), GONG_OF_WEAKENING_DURATION_SECONDS, 1, true, false));
                        }
                    }
                    ParticleUtil.spawnRadialVibrations((ServerLevel) le.level(), le.getEyePosition(), 8, 10, 40);
                    SoundClientboundPacket.playSoundAtPos(SoundAction.GONG_OF_WEAKNING, le.blockPosition(), 2.0f);
                }
                return true;
            })
            .build();

    private static final int ICE_WAND_DURATION_SECONDS = 8;
    public static final UnitItem ICE_WAND = UnitItemBuilder.of(ItemRegistrar.ICE_WAND.get())
            .descId("ice_wand")
            .type(UnitItemType.ACTIVE)
            .buyCost(400)
            .sellValue(200) // TODO
            .pointDesc("item.reignofnether.ice_wand.point1", ICE_WAND_DURATION_SECONDS)
            .pointDesc("item.reignofnether.ice_wand.point2")
            .build();

    private static final int UPDRAFT_TOME_DURATION_SECONDS = 6;
    public static final UnitItem UPDRAFT_TOME = UnitItemBuilder.of(ItemRegistrar.UPDRAFT_TOME.get())
            .descId("updraft_tome")
            .type(UnitItemType.ACTIVE)
            .buyCost(500)
            .sellValue(250) // TODO
            .pointDesc("item.reignofnether.updraft_tome.point1", UPDRAFT_TOME_DURATION_SECONDS)
            .pointDesc("item.reignofnether.updraft_tome.point2")
            .build();

    public static final UnitItem TOME_OF_DUPLICATION = UnitItemBuilder.of(ItemRegistrar.TOME_OF_DUPLICATION.get())
            .descId("tome_of_duplication")
            .type(UnitItemType.CONSUMABLE)
            .buyCost(600)
            .sellValue(300)
            .suppressDefaultError(true)
            .onUse(unit -> {
                boolean success = false;
                if (unit.getAbilities() != null) {
                    for (Ability ability : unit.getAbilities().get()) {
                        ability.setCooldown(0, unit);
                        success = true;
                    }
                }
                if (!success && !((LivingEntity) unit).level().isClientSide()) {
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.tome_of_duplication.error");
                } else if (success) {
                    // TODO particle effects
                }
                return success;
            })
            .build();

    private static final int SHADOW_SHIFTER_DURATION_SECONDS = 45;
    private static final int SHADOW_SHIFTER_RADIUS = 20;
    public static final UnitItem SHADOW_SHIFTER = UnitItemBuilder.of(ItemRegistrar.SHADOW_SHIFTER.get())
            .descId("shadow_shifter")
            .type(UnitItemType.ACTIVE)
            .buyCost(600)
            .sellValue(300)
            .pointDesc("item.reignofnether.shadow_shifter.point1", SHADOW_SHIFTER_DURATION_SECONDS)
            //.manaCost(25)
            //.cooldownTicks(180 * 20)
            .radius(SHADOW_SHIFTER_RADIUS)
            .showRadiusCircle()
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    le.addEffect(new MobEffectInstance(MobEffectRegistrar.NIGHT_WARPING.get(), SHADOW_SHIFTER_DURATION_SECONDS * 20, SHADOW_SHIFTER_RADIUS));
                    SoundClientboundPacket.playSoundAtPos(SoundAction.SHADOW_SHIFTER, le.blockPosition(), 2.0f);
                }
                return true;
            })
            .build();

    public static final UnitItem POCKET_PORTAL = UnitItemBuilder.of(ItemRegistrar.POCKET_PORTAL.get())
            .descId("pocket_portal")
            .type(UnitItemType.ACTIVE)
            .buyCost(200)
            .sellValue(100) // TODO
            .build();

    private static final int WAR_HORN_DURATION_SECONDS = 30;
    private static final int WAR_HORN_RADIUS = 15;
    public static final UnitItem WAR_HORN = UnitItemBuilder.of(ItemRegistrar.WAR_HORN.get())
            .descId("war_horn")
            .type(UnitItemType.ACTIVE)
            .buyCost(500)
            .sellValue(250)
            .pointDesc("item.reignofnether.war_horn.point1", WAR_HORN_DURATION_SECONDS)
            //.manaCost(50)
            //.cooldownTicks(120 * 20)
            .radius(WAR_HORN_RADIUS)
            .showRadiusCircle()
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    for (Mob mob : MiscUtil.getEntitiesWithinRange(le.getEyePosition(), WAR_HORN_RADIUS, Mob.class, le.level())) {
                        if (UnitServerEvents.getRl(unit, mob) == Relationship.FRIENDLY) {
                            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, WAR_HORN_DURATION_SECONDS, 0, false, true));
                        }
                    }
                    ParticleUtil.spawnRadialVibrations((ServerLevel) le.level(), le.getEyePosition(), 8, 10, 40);
                    SoundClientboundPacket.playSoundAtPos(SoundAction.WAR_HORN, le.blockPosition(), 2.0f);
                }
                return true;
            })
            .build();

    private static final int TOTEM_OF_REGENERATION_DURATION_SECONDS = 30;
    public static final UnitItem TOTEM_OF_REGENERATION = new TotemItem(UnitItemBuilder.of(ItemRegistrar.TOTEM_OF_REGENERATION.get())
            .descId("totem_of_regeneration")
            .pointDesc("item.reignofnether.totem_of_regeneration.point1", TOTEM_OF_REGENERATION_DURATION_SECONDS),
            EntityRegistrar.TOTEM_OF_REGENERATION.get()
    );

    private static final int TOTEM_OF_SHIELDING_DURATION_SECONDS = 20;
    public static final UnitItem TOTEM_OF_SHIELDING = new TotemItem(UnitItemBuilder.of(ItemRegistrar.TOTEM_OF_SHIELDING.get())
            .descId("totem_of_shielding")
            .pointDesc("item.reignofnether.totem_of_shielding.point1", TOTEM_OF_SHIELDING_DURATION_SECONDS),
            EntityRegistrar.TOTEM_OF_SHIELDING.get()
    );

    private static final int TOTEM_OF_PROTECTION_DURATION_SECONDS = 30;
    public static final UnitItem TOTEM_OF_PROTECTION = new TotemItem(UnitItemBuilder.of(ItemRegistrar.TOTEM_OF_SHIELDING.get())
            .descId("totem_of_protection")
            .pointDesc("item.reignofnether.totem_of_protection.point1", TOTEM_OF_PROTECTION_DURATION_SECONDS),
            EntityRegistrar.TOTEM_OF_PROTECTION.get()
    );

    private static final int TOTEM_OF_CASTING_DURATION_SECONDS = 30;
    public static final UnitItem TOTEM_OF_CASTING = new TotemItem(UnitItemBuilder.of(ItemRegistrar.TOTEM_OF_CASTING.get())
            .descId("totem_of_casting")
            .pointDesc("item.reignofnether.totem_of_casting.point1", TOTEM_OF_CASTING_DURATION_SECONDS),
            EntityRegistrar.TOTEM_OF_CASTING.get()
    );

    // TODO: auto convert villagers in range
    // TODO: show range when active always
    // TODO: check circles are not shown in fog
    public static final int BELL_OF_ARMS_RANGE = 20;
    public static final UnitItem BELL_OF_ARMS = UnitItemBuilder.of(Items.BELL)
            .descId("bell_of_arms")
            .type(UnitItemType.ACTIVE)
            .buyCost(600)
            .sellValue(300)
            .toggleActiveOnUse()
            .range(BELL_OF_ARMS_RANGE)
            .showRangeCircle()
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    SoundClientboundPacket.playSoundAtPos(SoundAction.BELL, le.blockPosition());
                    CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS).execute(() -> {
                        SoundClientboundPacket.playSoundAtPos(SoundAction.BELL, le.blockPosition());
                    });
                }
                return true;
            })
            .build();

    public static final int TOTEM_OF_CASTING_INVINCIBILITY_DURATION_SECONDS = 5;
    public static final UnitItem TOTEM_OF_UNDYING = UnitItemBuilder.of(Items.TOTEM_OF_UNDYING)
            .descId("totem_of_undying")
            .type(UnitItemType.CONSUMABLE)
            .buyCost(500)
            .sellValue(250) // Handled in HeroServerEvents.onLivingDeath
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/totem_of_undying.png"))
            .pointDesc("item.reignofnether.totem_of_undying.point1", TOTEM_OF_CASTING_INVINCIBILITY_DURATION_SECONDS)
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
            IRON_HIDE_AMULET,
            SOUL_COLLECTOR,
            BROADSWORD,
            KATANA,
            HEARTSTEALER,
            SOUL_SCYTHE,
            POWERSHAKER,
            GREAT_HAMMER,
            SPARKLER,
            LIGHT_FEATHER,
            SPYGLASS,
            BOOTS_OF_SWIFTNESS,
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
