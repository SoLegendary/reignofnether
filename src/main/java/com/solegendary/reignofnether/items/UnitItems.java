package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.blocks.BlockServerEvents;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingValidators;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.entities.ThrownHeroExperienceBottle;
import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.items.unititems.EmptyUnitItem;
import com.solegendary.reignofnether.items.unititems.MerchantEquipmentItem;
import com.solegendary.reignofnether.items.unititems.TotemItem;
import com.solegendary.reignofnether.registrars.*;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.taskscheduler.TaskSchedulerServerEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitSyncAbilityClientboundPacket;
import com.solegendary.reignofnether.unit.units.monsters.WretchedWraithUnit;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.ParticleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.Comparator;
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
            .maxStackSize(64)
            .pointDesc("item.reignofnether.hero_experience_bottle.point1", EXPERIENCE_BOTTLE_EXP_VALUE)
            .buyCost(100)
            .sellValue(50)
            .noRandomDrop()
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
            .rarity(Rarity.EPIC)
            .noRandomDrop()
            .cooldownTicks(60 * 20)
            .manaCost(50)
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
            .rarity(Rarity.COMMON)
            .noRandomDrop()
            .attribute(Attributes.MAX_HEALTH, 50, ADDITION)
            .build();

    public static final UnitItem AZURE_MEDALLION = UnitItemBuilder.of(ItemRegistrar.AZURE_MEDALLION.get())
            .descId("azure_medallion")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.COMMON)
            .noRandomDrop()
            .attribute(AttributeRegistrar.BASE_MAX_MANA.get(), 50, ADDITION)
            .build();

    public static final UnitItem IRON_HIDE_AMULET = UnitItemBuilder.of(ItemRegistrar.IRON_HIDE_AMULET.get())
            .descId("iron_hide_amulet")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.COMMON)
            .noRandomDrop()
            .attribute(Attributes.ARMOR, 5, ADDITION)
            .build();

    public static final int SOUL_COLLECTOR_RADIUS = 20;
    public static final UnitItem SOUL_COLLECTOR = UnitItemBuilder.of(ItemRegistrar.SOUL_COLLECTOR.get())
            .descId("soul_collector")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.UNCOMMON)
            .radius(SOUL_COLLECTOR_RADIUS)
            .build();

    public static final UnitItem BROADSWORD = UnitItemBuilder.of(ItemRegistrar.BROADSWORD.get())
            .descId("broadsword")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.COMMON)
            .noRandomDrop()
            .attribute(AttributeRegistrar.ATTACK_DAMAGE.get(), 0.30, MULTIPLY_BASE)
            .build();

    public static final UnitItem KATANA = UnitItemBuilder.of(ItemRegistrar.KATANA.get())
            .descId("katana")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.UNCOMMON)
            .attribute(AttributeRegistrar.CRITICAL_HIT_CHANCE.get(), 0.17, ADDITION)
            .build();

    public static final UnitItem HEARTSTEALER = UnitItemBuilder.of(ItemRegistrar.HEARTSTEALER.get())
            .descId("heartstealer")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.UNCOMMON)
            .attribute(AttributeRegistrar.LIFESTEAL.get(), 0.20, ADDITION)
            .build();

    public static final UnitItem RITUAL_DAGGER = UnitItemBuilder.of(ItemRegistrar.RITUAL_DAGGER.get())
            .descId("ritual_dagger")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.UNCOMMON)
            .attribute(AttributeRegistrar.MANA_ON_HIT.get(), 0.30, ADDITION)
            .build();

    public static final UnitItem POWERSHAKER = UnitItemBuilder.of(ItemRegistrar.POWERSHAKER.get())
            .descId("powershaker")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.UNCOMMON)
            .attribute(AttributeRegistrar.EXPLOSIVE_HIT_CHANCE.get(), 0.2, ADDITION)
            .build();

    public static final UnitItem GREAT_HAMMER = UnitItemBuilder.of(ItemRegistrar.GREAT_HAMMER.get())
            .descId("great_hammer")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.UNCOMMON)
            .attribute(AttributeRegistrar.BUILDING_DAMAGE_BONUS.get(), 2.0, ADDITION)
            .build();

    public static final UnitItem SPARKLER = UnitItemBuilder.of(ItemRegistrar.SPARKLER.get())
            .descId("sparkler")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.COMMON)
            .attribute(AttributeRegistrar.ATTACKS_PER_SECOND.get(), 0.25, MULTIPLY_BASE)
            .build();

    public static final UnitItem LIGHT_FEATHER = UnitItemBuilder.of(ItemRegistrar.LIGHT_FEATHER.get())
            .descId("light_feather")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.COMMON)
            .attribute(AttributeRegistrar.EVASION_CHANCE.get(), 0.17, ADDITION)
            .build();

    public static final UnitItem SPYGLASS = UnitItemBuilder.of(Items.SPYGLASS)
            .descId("spyglass")
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/spyglass.png"))
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.COMMON)
            .attribute(AttributeRegistrar.SIGHT_RANGE.get(), 8, ADDITION)
            .build();

    public static final UnitItem BOOTS_OF_SWIFTNESS = UnitItemBuilder.of(ItemRegistrar.BOOTS_OF_SWIFTNESS.get())
            .descId("boots_of_swiftness")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.COMMON)
            .attribute(Attributes.MOVEMENT_SPEED, 0.04, ADDITION)
            .build();

    public static final UnitItem FROST_WALKER_BOOTS = UnitItemBuilder.of(ItemRegistrar.FROST_WALKER_BOOTS.get())
            .descId("frost_walker_boots")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.RARE)
            .noRandomDrop()
            .build();

    public static final UnitItem MAGMA_WALKER_BOOTS = UnitItemBuilder.of(ItemRegistrar.MAGMA_WALKER_BOOTS.get())
            .descId("magma_walker_boots")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.RARE)
            .noRandomDrop()
            .build();

    public static final UnitItem SATCHEL_OF_SNACKS = UnitItemBuilder.of(ItemRegistrar.SATCHEL_OF_SNACKS.get())
            .descId("satchel_of_snacks")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.UNCOMMON)
            .noBehaviourReset()
            .suppressDefaultError()
            .cooldownTicks(15 * 20)
            .forceAutocast()
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    if (unit.isEatingFood()) {
                        return false;
                    }
                    boolean fullHealth = le.getHealth() >= le.getMaxHealth();
                    if (!fullHealth) {
                        Unit.startEatingOrDrinking(unit, new ItemEntity(le.level(), le.getX(), le.getY(), le.getZ(),
                                new ItemStack(Items.COOKIE)));
                        return true;
                    } else {
                        return false;
                    }
                }
                return true;
            })
            .build();

    public static final float BEENEST_ARMOUR_DAMAGE_PER_BEE = 40f;
    public static final UnitItem BEENEST_ARMOUR = UnitItemBuilder.of(ItemRegistrar.BEENEST_ARMOUR.get())
            .descId("beenest_armour")
            .type(UnitItemType.PASSIVE)
            .rarity(Rarity.RARE)
            .pointDesc("item.reignofnether.beenest_armour.point1", BEENEST_ARMOUR_DAMAGE_PER_BEE)
            .build();

    public static final float HEALTH_POTION_RESTORE_AMOUNT = 50f;
    public static final UnitItem HEALTH_POTION = UnitItemBuilder.of(ItemRegistrar.HEALTH_POTION.get())
            .descId("health_potion")
            .type(UnitItemType.CONSUMABLE)
            .maxStackSize(9)
            .buyCost(150)
            .sellValue(75)
            .pointDesc("item.reignofnether.health_potion.point1", (int) HEALTH_POTION_RESTORE_AMOUNT)
            .suppressDefaultError()
            .noRandomDrop()
            .cooldownTicks(10 * 20)
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    if (unit.isEatingFood()) {
                        HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.potion.error2");
                        return false;
                    }
                    boolean fullHealth = le.getHealth() >= le.getMaxHealth();
                    if (!fullHealth) {
                        Unit.startEatingOrDrinking(unit, new ItemEntity(le.level(), le.getX(), le.getY(), le.getZ(),
                                new ItemStack(ItemRegistrar.HEALTH_POTION.get())));
                        return true;
                    } else {
                        HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.health_potion.error");
                        return false;
                    }
                }
                return true;
            })
            .build();

    public static final float MANA_POTION_RESTORE_AMOUNT = 50f;
    public static final UnitItem MANA_POTION = UnitItemBuilder.of(ItemRegistrar.MANA_POTION.get())
            .descId("mana_potion")
            .type(UnitItemType.CONSUMABLE)
            .maxStackSize(9)
            .buyCost(150)
            .sellValue(75)
            .pointDesc("item.reignofnether.mana_potion.point1", (int) MANA_POTION_RESTORE_AMOUNT)
            .suppressDefaultError()
            .noRandomDrop()
            .cooldownTicks(10 * 20)
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    if (unit.isEatingFood()) {
                        HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.potion.error2");
                        return false;
                    }
                    boolean fullMana = le instanceof HeroUnit heroUnit && heroUnit.getMana() >= heroUnit.getMaxMana();
                    if (!fullMana) {
                        Unit.startEatingOrDrinking(unit, new ItemEntity(le.level(), le.getX(), le.getY(), le.getZ(),
                                new ItemStack(ItemRegistrar.MANA_POTION.get())));
                        return true;
                    } else {
                        HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.mana_potion.error");
                        return false;
                    }
                }
                return true;
            })
            .build();

    private static final int GHOST_CLOAK_DURATION_SECONDS = 15;
    public static final UnitItem GHOST_CLOAK = UnitItemBuilder.of(ItemRegistrar.GHOST_CLOAK.get())
            .descId("ghost_cloak")
            .type(UnitItemType.ACTIVE)
            .rarity(Rarity.UNCOMMON)
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
            .rarity(Rarity.RARE)
            .pointDesc("item.reignofnether.gong_of_weakening.point1", GONG_OF_WEAKENING_DURATION_SECONDS)
            .radius(10)
            .manaCost(50)
            .cooldownTicks(90 * 20)
            .showRadiusCircle()
            .noRandomDrop()
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    for (Mob mob : MiscUtil.getEntitiesWithinRange(le.getEyePosition(), GONG_OF_WEAKENING_RADIUS, Mob.class, le.level())) {
                        if (UnitServerEvents.getRl(unit, mob) != Relationship.FRIENDLY) {
                            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, GONG_OF_WEAKENING_DURATION_SECONDS * 20, 0, false, true));
                            mob.addEffect(new MobEffectInstance(MobEffectRegistrar.DAMAGE_TAKEN_INCREASE.get(), GONG_OF_WEAKENING_DURATION_SECONDS * 20, 1, true, false));
                        }
                    }
                    ParticleUtil.spawnRadialVibrations((ServerLevel) le.level(), le.getEyePosition(), 8, 10, 40);
                    SoundClientboundPacket.playSoundAtPos(SoundAction.GONG_OF_WEAKNING, le.blockPosition(), 2.0f);
                }
                return true;
            })
            .build();

    private static final int ICE_WAND_DURATION_SECONDS = 12;
    public static final UnitItem ICE_WAND = UnitItemBuilder.of(ItemRegistrar.ICE_WAND.get())
            .descId("ice_wand")
            .type(UnitItemType.ACTIVE)
            .rarity(Rarity.UNCOMMON)
            .pointDesc("item.reignofnether.ice_wand.point1", ICE_WAND_DURATION_SECONDS)
            .pointDesc("item.reignofnether.ice_wand.point2")
            .cooldownTicks(45 * 20)
            .manaCost(35)
            .range(10)
            .showRangeCircle()
            .onUseEntity((unit, entity) -> {
                LivingEntity le = entity;
                if (!le.level().isClientSide()) {
                    if (!le.level().isClientSide()) {
                        if (le.onGround() && !(le instanceof WretchedWraithUnit) &&
                                !le.hasEffect(MobEffectRegistrar.FREEZE.get())) {

                            SoundClientboundPacket.playSoundAtPos(SoundAction.ICE_WAND, le.blockPosition());
                            int duration = ICE_WAND_DURATION_SECONDS * 20;
                            if (le instanceof HeroUnit) duration /= 2;
                            BlockServerEvents.addTempBlock((ServerLevel) le.level(), le.getOnPos().above(),
                                    Blocks.PACKED_ICE.defaultBlockState(), Blocks.AIR.defaultBlockState(), duration, true);
                            BlockServerEvents.addTempBlock((ServerLevel) le.level(), le.getOnPos().above().above(),
                                    Blocks.PACKED_ICE.defaultBlockState(), Blocks.AIR.defaultBlockState(), duration, true);
                            BlockServerEvents.addTempBlock((ServerLevel) le.level(), le.getOnPos().above().above().above(),
                                    BlockRegistrar.WRAITH_SNOW_LAYER.get().defaultBlockState(), Blocks.AIR.defaultBlockState(), duration, true);
                            ServerLevel serverLevel = (ServerLevel) le.level();
                            int entityId = le.getId();
                            BlockServerEvents.getSnowPositions(le.level(), le.getOnPos().above(), 2)
                                    .forEach((pos, delay) -> TaskSchedulerServerEvents.schedule(delay, () -> {
                                        BlockServerEvents.placeWraithSnow(serverLevel, pos, entityId);
                                    }));
                            le.addEffect(new MobEffectInstance(MobEffectRegistrar.FREEZE.get(), duration));
                            le.addEffect(new MobEffectInstance(MobEffectRegistrar.FROST_DAMAGE.get(), duration));
                            ParticleUtil.addParticleExplosion(ParticleTypes.SNOWFLAKE, 15, le.level(), le.position());

                            ParticleUtil.addParticleExplosion(ParticleTypes.SNOWFLAKE, 10, le.level(), ((LivingEntity) unit).getEyePosition());
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
                return true;
            })
            .build();

    private static final int UPDRAFT_TOME_DURATION_SECONDS = 8;
    private static final int UPDRAFT_TOME_RADIUS = 3;
    private static final int UPDRAFT_TOME_MAX_TARGETS = 5;
    public static final UnitItem UPDRAFT_TOME = UnitItemBuilder.of(ItemRegistrar.UPDRAFT_TOME.get())
            .descId("updraft_tome")
            .type(UnitItemType.ACTIVE)
            .rarity(Rarity.RARE)
            .pointDesc("item.reignofnether.updraft_tome.point1", UPDRAFT_TOME_DURATION_SECONDS)
            .pointDesc("item.reignofnether.updraft_tome.point2")
            .cooldownTicks(60 * 20)
            .manaCost(50)
            .range(10)
            .radius(UPDRAFT_TOME_RADIUS)
            .showRadiusAtCursor()
            .showRadiusCircle()
            .onUseGround((unit, bp) -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    int numAffectedUnits = 0;
                    List<Mob> mobs = MiscUtil.getEntitiesWithinRange(bp.above().getCenter(), UPDRAFT_TOME_RADIUS, Mob.class, le.level())
                            .stream()
                            .sorted(Comparator.comparing(mob -> mob.distanceToSqr(bp.getCenter())))
                            .toList();
                    for (Mob mob : mobs) {
                        if (UnitServerEvents.getRl(unit, mob) != Relationship.FRIENDLY) {
                            mob.addEffect(new MobEffectInstance(MobEffects.LEVITATION, UPDRAFT_TOME_DURATION_SECONDS * 20, 0, true, false));
                            numAffectedUnits += 1;
                        }
                        if (numAffectedUnits >= UPDRAFT_TOME_MAX_TARGETS)
                            break;
                    }
                    for (int i = 0; i < Math.max(1, numAffectedUnits); i++) {
                        CompletableFuture.delayedExecutor(300 * i, TimeUnit.MILLISECONDS).execute(() -> {
                            SoundClientboundPacket.playSoundAtPos(SoundAction.WINDCALLER_LIFT, le.blockPosition(), 1.5f);
                        });
                    }
                    ParticleUtil.addParticleExplosion(ParticleTypes.POOF, 30, le.level(), bp.getCenter());
                    ParticleUtil.addParticleExplosion(ParticleTypes.POOF, 15, le.level(), le.getEyePosition());
                }
                return true;
            })
            .build();

    public static final UnitItem TOME_OF_DUPLICATION = UnitItemBuilder.of(ItemRegistrar.TOME_OF_DUPLICATION.get())
            .descId("tome_of_duplication")
            .type(UnitItemType.CONSUMABLE)
            .rarity(Rarity.RARE)
            .suppressDefaultError()
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                boolean success = false;
                if (unit.getAbilities() != null) {
                    for (Ability ability : unit.getAbilities().get()) {
                        ability.setCooldown(0, unit);
                        success = true;
                    }
                }
                if (!le.level().isClientSide()) {
                    if (!success) {
                        HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.tome_of_duplication.error");
                    } else {
                        ParticleUtil.addParticleExplosion(ParticleRegistrar.MANA.get(), 30, le.level(),le.getEyePosition());
                        SoundClientboundPacket.playSoundAtPos(SoundAction.TOME_OF_DUPLICATION, le.blockPosition(), 1.5f);
                        UnitSyncAbilityClientboundPacket.sendSyncAbilitiesPacket(le);
                    }
                }
                return success;
            })
            .build();

    private static final int SHADOW_SHIFTER_DURATION_SECONDS = 45;
    private static final int SHADOW_SHIFTER_RADIUS = 20;
    public static final UnitItem SHADOW_SHIFTER = UnitItemBuilder.of(ItemRegistrar.SHADOW_SHIFTER.get())
            .descId("shadow_shifter")
            .type(UnitItemType.ACTIVE)
            .rarity(Rarity.EPIC)
            .pointDesc("item.reignofnether.shadow_shifter.point1", SHADOW_SHIFTER_DURATION_SECONDS)
            .manaCost(50)
            .cooldownTicks(180 * 20)
            .range(SHADOW_SHIFTER_RADIUS)
            .showRangeCircle()
            .noRandomDrop()
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    le.addEffect(new MobEffectInstance(MobEffectRegistrar.NIGHT_WARPING.get(), SHADOW_SHIFTER_DURATION_SECONDS * 20, SHADOW_SHIFTER_RADIUS - 1));
                    SoundClientboundPacket.playSoundAtPos(SoundAction.SHADOW_SHIFTER, le.blockPosition(), 1.5f);
                    ParticleUtil.addParticleExplosion(ParticleTypes.WITCH, 30, le.level(), le.getEyePosition());
                }
                return true;
            })
            .build();

    private static final int POCKET_PORTAL_RANGE = 5;
    public static final UnitItem POCKET_PORTAL = UnitItemBuilder.of(ItemRegistrar.POCKET_PORTAL.get())
            .descId("pocket_portal")
            .type(UnitItemType.CONSUMABLE)
            .maxStackSize(3)
            .buyCost(150)
            .sellValue(75)
            .noRandomDrop()
            .showRangeCircle()
            .range(POCKET_PORTAL_RANGE)
            .suppressDefaultError()
            .onUseGround((unit, pos) -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    String error = BuildingValidators.getPlacementValidityError(
                            le.level(),
                            Buildings.PORTAL_POCKET,
                            pos.offset(-1,0,-1),
                            unit.getOwnerName(),
                            Rotation.NONE,
                            false,
                            false,
                            true
                    );
                    if (error != null) {
                        HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), error);
                        return false;
                    } else {
                        BuildingServerEvents.placeBuilding(
                                Buildings.PORTAL_POCKET,
                                pos.offset(-1,0,-1),
                                Rotation.NONE,
                                unit.getOwnerName(),
                                new int[] {},
                                false,
                                false,
                                true,
                                true
                        );
                        return true;
                    }
                }
                return true;
            })
            .build();

    private static final int WAR_HORN_DURATION_SECONDS = 20;
    private static final int WAR_HORN_RADIUS = 15;
    public static final UnitItem WAR_HORN = UnitItemBuilder.of(ItemRegistrar.WAR_HORN.get())
            .descId("war_horn")
            .type(UnitItemType.ACTIVE)
            .rarity(Rarity.RARE)
            .pointDesc("item.reignofnether.war_horn.point1", WAR_HORN_DURATION_SECONDS)
            .manaCost(50)
            .cooldownTicks(90 * 20)
            .radius(WAR_HORN_RADIUS)
            .showRadiusCircle()
            .noRandomDrop()
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    for (Mob mob : MiscUtil.getEntitiesWithinRange(le.getEyePosition(), WAR_HORN_RADIUS, Mob.class, le.level())) {
                        if (UnitServerEvents.getRl(unit, mob) == Relationship.FRIENDLY) {
                            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, WAR_HORN_DURATION_SECONDS * 20, 0, false, true));
                        }
                    }
                    ParticleUtil.spawnRadialVibrations((ServerLevel) le.level(), le.getEyePosition(), 8, 10, 40);
                    SoundClientboundPacket.playSoundAtPos(SoundAction.WAR_HORN, le.blockPosition(), 2.0f);
                }
                return true;
            })
            .build();

    public static final int BELL_OF_ARMS_RANGE = 20;
    public static final UnitItem BELL_OF_ARMS = UnitItemBuilder.of(Items.BELL)
            .descId("bell_of_arms")
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/bell.png"))
            .type(UnitItemType.ACTIVE)
            .rarity(Rarity.EPIC)
            .toggleActiveOnUse()
            .range(BELL_OF_ARMS_RANGE)
            .showRangeCircle()
            .noBehaviourReset()
            .noRandomDrop()
            .onUse(unit -> {
                LivingEntity le = (LivingEntity) unit;
                if (!le.level().isClientSide()) {
                    SoundClientboundPacket.playSoundAtPos(SoundAction.BELL, le.blockPosition());
                    CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS).execute(() -> {
                        SoundClientboundPacket.playSoundAtPos(SoundAction.BELL, le.blockPosition());
                    });
                    le.addEffect(new MobEffectInstance(MobEffectRegistrar.VILLAGER_INSPIRATION.get(), 30, 0, true, false));
                }
                return true;
            })
            .build();

    public static final int TOTEM_OF_UNDYING_INVINCIBILITY_DURATION_SECONDS = 10;
    public static final UnitItem TOTEM_OF_UNDYING = UnitItemBuilder.of(Items.TOTEM_OF_UNDYING)
            .descId("totem_of_undying")
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/totem_of_undying.png"))
            .type(UnitItemType.CONSUMABLE)
            .rarity(Rarity.EPIC) // Handled in HeroServerEvents.onLivingDeath
            .icon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/totem_of_undying.png"))
            .pointDesc("item.reignofnether.totem_of_undying.point1", TOTEM_OF_UNDYING_INVINCIBILITY_DURATION_SECONDS)
            .build();

    public static final int TOTEM_OF_REGENERATION_DURATION_SECONDS = 30;
    public static final UnitItem TOTEM_OF_REGENERATION = new TotemItem(UnitItemBuilder.of(ItemRegistrar.TOTEM_OF_REGENERATION.get())
            .descId("totem_of_regeneration")
            .pointDesc("item.reignofnether.totem_of_regeneration.point1", TOTEM_OF_REGENERATION_DURATION_SECONDS),
            EntityRegistrar.TOTEM_OF_REGENERATION.get()
    );

    public static final int TOTEM_OF_SHIELDING_DURATION_SECONDS = 20;
    public static final UnitItem TOTEM_OF_SHIELDING = new TotemItem(UnitItemBuilder.of(ItemRegistrar.TOTEM_OF_SHIELDING.get())
            .descId("totem_of_shielding")
            .pointDesc("item.reignofnether.totem_of_shielding.point1", TOTEM_OF_SHIELDING_DURATION_SECONDS),
            EntityRegistrar.TOTEM_OF_SHIELDING.get()
    );

    public static final int TOTEM_OF_PROTECTION_DURATION_SECONDS = 30;
    public static final UnitItem TOTEM_OF_PROTECTION = new TotemItem(UnitItemBuilder.of(ItemRegistrar.TOTEM_OF_PROTECTION.get())
            .descId("totem_of_protection")
            .pointDesc("item.reignofnether.totem_of_protection.point1", TOTEM_OF_PROTECTION_DURATION_SECONDS),
            EntityRegistrar.TOTEM_OF_PROTECTION.get()
    );

    public static final int TOTEM_OF_CASTING_DURATION_SECONDS = 40;
    public static final UnitItem TOTEM_OF_CASTING = new TotemItem(UnitItemBuilder.of(ItemRegistrar.TOTEM_OF_CASTING.get())
            .descId("totem_of_casting")
            .pointDesc("item.reignofnether.totem_of_casting.point1", TOTEM_OF_CASTING_DURATION_SECONDS),
            EntityRegistrar.TOTEM_OF_CASTING.get()
    );

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
            RITUAL_DAGGER,
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
            BEENEST_ARMOUR,
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
