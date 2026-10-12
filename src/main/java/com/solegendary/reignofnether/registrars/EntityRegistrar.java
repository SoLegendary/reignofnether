package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import com.solegendary.reignofnether.entities.*;
import com.solegendary.reignofnether.hero.*;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.unit.units.neutral.*;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.unit.units.villagers.*;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class EntityRegistrar {

    private static final int UNIT_CLIENT_TRACKING_RANGE = 100;

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ReignOfNether.MOD_ID);

    public static final Supplier<EntityType<ZombieVillagerUnit>> ZOMBIE_VILLAGER_UNIT = ENTITIES.register("zombie_villager_unit",
            () -> EntityType.Builder.of(ZombieVillagerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ZOMBIE_VILLAGER.getWidth(), EntityType.ZOMBIE_VILLAGER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "zombie_villager_unit").toString()));

    public static final Supplier<EntityType<BatUnit>> BAT_UNIT = ENTITIES.register("bat_unit",
            () -> EntityType.Builder.of(BatUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.BAT.getWidth(), EntityType.BAT.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "bat_unit").toString()));

    public static final Supplier<EntityType<ZombieUnit>> ZOMBIE_UNIT = ENTITIES.register("zombie_unit",
            () -> EntityType.Builder.of(ZombieUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ZOMBIE.getWidth(), EntityType.ZOMBIE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "zombie_unit").toString()));

    public static final Supplier<EntityType<HuskUnit>> HUSK_UNIT = ENTITIES.register("husk_unit",
            () -> EntityType.Builder.of(HuskUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.HUSK.getWidth(), EntityType.HUSK.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "husk_unit").toString()));

    public static final Supplier<EntityType<DrownedUnit>> DROWNED_UNIT = ENTITIES.register("drowned_unit",
            () -> EntityType.Builder.of(DrownedUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.DROWNED.getWidth(), EntityType.DROWNED.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "drowned_unit").toString()));

    public static final Supplier<EntityType<ZombiePiglinUnit>> ZOMBIE_PIGLIN_UNIT = ENTITIES.register("zombie_piglin_unit",
            () -> EntityType.Builder.of(ZombiePiglinUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ZOMBIFIED_PIGLIN.getWidth(), EntityType.ZOMBIFIED_PIGLIN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "zombie_piglin_unit").toString()));

    public static final Supplier<EntityType<ZoglinUnit>> ZOGLIN_UNIT = ENTITIES.register("zoglin_unit",
            () -> EntityType.Builder.of(ZoglinUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ZOGLIN.getWidth(), EntityType.ZOGLIN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "zoglin_unit").toString()));

    public static final Supplier<EntityType<SkeletonUnit>> SKELETON_UNIT = ENTITIES.register("skeleton_unit",
            () -> EntityType.Builder.of(SkeletonUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SKELETON.getWidth(), EntityType.SKELETON.getHeight())
                    // vanilla skeleton rides at -0.7F; without it a rider is placed at the top of the
                    // hitbox instead of on the mount's back
                    .ridingOffset(-0.7F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "skeleton_unit").toString()));

    public static final Supplier<EntityType<StrayUnit>> STRAY_UNIT = ENTITIES.register("stray_unit",
            () -> EntityType.Builder.of(StrayUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.STRAY.getWidth(), EntityType.STRAY.getHeight())
                    // vanilla stray rides at -0.7F; without it a rider is placed at the top of the
                    // hitbox instead of on the mount's back
                    .ridingOffset(-0.7F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "stray_unit").toString()));

    public static final Supplier<EntityType<BoggedUnit>> BOGGED_UNIT = ENTITIES.register("bogged_unit",
            () -> EntityType.Builder.of(BoggedUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SKELETON.getWidth(), EntityType.SKELETON.getHeight())
                    // vanilla bogged rides at -0.7F; without it a rider is placed at the top of the
                    // hitbox instead of on the mount's back
                    .ridingOffset(-0.7F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "bogged_unit").toString()));

    public static final Supplier<EntityType<CreeperUnit>> CREEPER_UNIT = ENTITIES.register("creeper_unit",
            () -> EntityType.Builder.of(CreeperUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.CREEPER.getWidth(), EntityType.CREEPER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "creeper_unit").toString()));

    public static final Supplier<EntityType<SpiderUnit>> SPIDER_UNIT = ENTITIES.register("spider_unit",
            () -> EntityType.Builder.of(SpiderUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SPIDER.getWidth(), EntityType.SPIDER.getHeight())
                    // vanilla spider attachment: without it a rider defaults to the top of the hitbox
                    // (0.9) instead of standing on the back (0.765) - a skeleton floating above the spider
                    .passengerAttachments(0.765F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "spider_unit").toString()));

    public static final Supplier<EntityType<PoisonSpiderUnit>> POISON_SPIDER_UNIT = ENTITIES.register("poison_spider_unit",
            () -> EntityType.Builder.of(PoisonSpiderUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SPIDER.getWidth(), EntityType.SPIDER.getHeight())
                    // vanilla spider attachment: without it a rider defaults to the top of the hitbox
                    // (0.9) instead of standing on the back (0.765) - a skeleton floating above the spider
                    .passengerAttachments(0.765F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "poison_spider_unit").toString()));

    public static final Supplier<EntityType<WraithUnit>> WRAITH_UNIT = ENTITIES.register("wraith_unit",
            () -> EntityType.Builder.of(WraithUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.BLAZE.getWidth(), EntityType.BLAZE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wraith_unit").toString()));

    public static final Supplier<EntityType<VillagerUnit>> VILLAGER_UNIT = ENTITIES.register("villager_unit",
            () -> EntityType.Builder.of(VillagerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VILLAGER.getWidth(), EntityType.VILLAGER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "villager_unit").toString()));

    public static final Supplier<EntityType<ScoutDogUnit>> SCOUT_DOG_UNIT = ENTITIES.register("scout_dog_unit",
            () -> EntityType.Builder.of(ScoutDogUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.WOLF.getWidth(), EntityType.WOLF.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "scout_dog_unit").toString()));

    public static final Supplier<EntityType<ScoutCatUnit>> SCOUT_CAT_UNIT = ENTITIES.register("scout_cat_unit",
            () -> EntityType.Builder.of(ScoutCatUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.WOLF.getWidth(), EntityType.WOLF.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "scout_cat_unit").toString()));

    public static final Supplier<EntityType<MilitiaUnit>> MILITIA_UNIT = ENTITIES.register("militia_unit",
            () -> EntityType.Builder.of(MilitiaUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VILLAGER.getWidth(), EntityType.VILLAGER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "militia_unit").toString()));

    public static final Supplier<EntityType<TemporaryMilitiaUnit>> TEMPORARY_MILITIA_UNIT = ENTITIES.register("temporary_militia_unit",
            () -> EntityType.Builder.of(TemporaryMilitiaUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VILLAGER.getWidth(), EntityType.VILLAGER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "temporary_militia_unit").toString()));

    public static final Supplier<EntityType<VindicatorUnit>> VINDICATOR_UNIT = ENTITIES.register("vindicator_unit",
            () -> EntityType.Builder.of(VindicatorUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VINDICATOR.getWidth(), EntityType.VINDICATOR.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "vindicator_unit").toString()));

    public static final Supplier<EntityType<PillagerUnit>> PILLAGER_UNIT = ENTITIES.register("pillager_unit",
            () -> EntityType.Builder.of(PillagerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PILLAGER.getWidth(), EntityType.PILLAGER.getHeight())
                    // vanilla pillager rides at -0.6F; without it a rider is placed at the top of the
                    // hitbox instead of on the mount's back
                    .ridingOffset(-0.6F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "pillager_unit").toString()));

    public static final Supplier<EntityType<WindcallerUnit>> WINDCALLER_UNIT = ENTITIES.register("windcaller_unit",
            () -> EntityType.Builder.of(WindcallerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PILLAGER.getWidth(), EntityType.PILLAGER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "windcaller_unit").toString()));

    public static final Supplier<EntityType<IronGolemUnit>> IRON_GOLEM_UNIT = ENTITIES.register("iron_golem_unit",
            () -> EntityType.Builder.of(IronGolemUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.IRON_GOLEM.getWidth(), EntityType.IRON_GOLEM.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "iron_golem_unit").toString()));

    public static final Supplier<EntityType<WitchUnit>> WITCH_UNIT = ENTITIES.register("witch_unit",
            () -> EntityType.Builder.of(WitchUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.WITCH.getWidth(), EntityType.WITCH.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "witch_unit").toString()));

    public static final Supplier<EntityType<EvokerUnit>> EVOKER_UNIT = ENTITIES.register("evoker_unit",
            () -> EntityType.Builder.of(EvokerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.EVOKER.getWidth(), EntityType.EVOKER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "evoker_unit").toString()));

    public static final Supplier<EntityType<EndermanUnit>> ENDERMAN_UNIT = ENTITIES.register("enderman_unit",
            () -> EntityType.Builder.of(EndermanUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ENDERMAN.getWidth(), EntityType.ENDERMAN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "enderman_unit").toString()));

    public static final Supplier<EntityType<RavagerUnit>> RAVAGER_UNIT = ENTITIES.register("ravager_unit",
            () -> EntityType.Builder.of(RavagerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.RAVAGER.getWidth(), EntityType.RAVAGER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "ravager_unit").toString()));

    public static final Supplier<EntityType<WardenUnit>> WARDEN_UNIT = ENTITIES.register("warden_unit",
            () -> EntityType.Builder.of(WardenUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.WARDEN.getWidth(), EntityType.WARDEN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "warden_unit").toString()));

    public static final Supplier<EntityType<SilverfishUnit>> SILVERFISH_UNIT = ENTITIES.register("silverfish_unit",
            () -> EntityType.Builder.of(SilverfishUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SILVERFISH.getWidth(), EntityType.SILVERFISH.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "silverfish_unit").toString()));

    public static final Supplier<EntityType<GruntUnit>> GRUNT_UNIT = ENTITIES.register("grunt_unit",
            () -> EntityType.Builder.of(GruntUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PIGLIN.getWidth(), EntityType.PIGLIN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "grunt_unit").toString()));

    public static final Supplier<EntityType<StriderUnit>> STRIDER_UNIT = ENTITIES.register("strider_unit",
            () -> EntityType.Builder.of(StriderUnit::new, MobCategory.CREATURE)
                    .fireImmune()
                    .sized(EntityType.STRIDER.getWidth(), EntityType.STRIDER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "strider_unit").toString()));

    public static final Supplier<EntityType<BruteUnit>> BRUTE_UNIT = ENTITIES.register("brute_unit",
            () -> EntityType.Builder.of(BruteUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PIGLIN_BRUTE.getWidth(), EntityType.PIGLIN_BRUTE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "brute_unit").toString()));

    public static final Supplier<EntityType<HeadhunterUnit>> HEADHUNTER_UNIT = ENTITIES.register("headhunter_unit",
            () -> EntityType.Builder.of(HeadhunterUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PIGLIN_BRUTE.getWidth(), EntityType.PIGLIN_BRUTE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "headhunter_unit").toString()));

    public static final Supplier<EntityType<MarauderUnit>> MARAUDER_UNIT = ENTITIES.register("marauder_unit",
            () -> EntityType.Builder.of(MarauderUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.IRON_GOLEM.getWidth(), EntityType.IRON_GOLEM.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "marauder_unit").toString()));

    public static final Supplier<EntityType<HoglinUnit>> HOGLIN_UNIT = ENTITIES.register("hoglin_unit",
            () -> EntityType.Builder.of(HoglinUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.HOGLIN.getWidth(), EntityType.HOGLIN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "hoglin_unit").toString()));

    public static final Supplier<EntityType<ArmouredHoglinUnit>> ARMOURED_HOGLIN_UNIT = ENTITIES.register("armoured_hoglin_unit",
            () -> EntityType.Builder.of(ArmouredHoglinUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.HOGLIN.getWidth(), EntityType.HOGLIN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "armoured_hoglin_unit").toString()));

    public static final Supplier<EntityType<BlazeUnit>> BLAZE_UNIT = ENTITIES.register("blaze_unit",
            () -> EntityType.Builder.of(BlazeUnit::new, MobCategory.CREATURE)
                    .fireImmune()
                    .sized(EntityType.BLAZE.getWidth(), EntityType.BLAZE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "blaze_unit").toString()));

    public static final Supplier<EntityType<WitherSkeletonUnit>> WITHER_SKELETON_UNIT = ENTITIES.register("wither_skeleton_unit",
            () -> EntityType.Builder.of(WitherSkeletonUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.WITHER_SKELETON.getWidth(), EntityType.WITHER_SKELETON.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wither_skeleton_unit").toString()));

    public static final Supplier<EntityType<GhastUnit>> GHAST_UNIT = ENTITIES.register("ghast_unit",
            () -> EntityType.Builder.of(GhastUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.GHAST.getWidth(), EntityType.GHAST.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "ghast_unit").toString()));

    public static final Supplier<EntityType<MagmaCubeUnit>> MAGMA_CUBE_UNIT = ENTITIES.register("magma_cube_unit",
            () -> EntityType.Builder.of(MagmaCubeUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.MAGMA_CUBE.getWidth(), EntityType.MAGMA_CUBE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "magma_cube_unit").toString()));

    public static final Supplier<EntityType<SlimeUnit>> SLIME_UNIT = ENTITIES.register("slime_unit",
            () -> EntityType.Builder.of(SlimeUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SLIME.getWidth(), EntityType.SLIME.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "slime_unit").toString()));

    public static final Supplier<EntityType<RoyalGuardUnit>> ROYAL_GUARD_UNIT = ENTITIES.register("royal_guard_unit",
            () -> EntityType.Builder.of(RoyalGuardUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VINDICATOR.getWidth(), EntityType.VINDICATOR.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "royal_guard_unit").toString()));

    public static final Supplier<EntityType<EnchanterUnit>> ENCHANTER_UNIT = ENTITIES.register("enchanter_unit",
            () -> EntityType.Builder.of(EnchanterUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VINDICATOR.getWidth(), EntityType.VINDICATOR.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "enchanter_unit").toString()));

    public static final Supplier<EntityType<NecromancerUnit>> NECROMANCER_UNIT = ENTITIES.register("necromancer_unit",
            () -> EntityType.Builder.of(NecromancerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SKELETON.getWidth(), EntityType.SKELETON.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "necromancer_unit").toString()));

    public static final Supplier<EntityType<WretchedWraithUnit>> WRETCHED_WRAITH_UNIT = ENTITIES.register("wretched_wraith_unit",
            () -> EntityType.Builder.of(WretchedWraithUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ZOMBIE.getWidth(), EntityType.ZOMBIE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretched_wraith_unit").toString()));

    public static final Supplier<EntityType<PiglinMerchantUnit>> PIGLIN_MERCHANT_UNIT = ENTITIES.register("piglin_merchant_unit",
            () -> EntityType.Builder.of(PiglinMerchantUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.IRON_GOLEM.getWidth(), EntityType.IRON_GOLEM.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "piglin_merchant_unit").toString()));

    public static final Supplier<EntityType<WildfireUnit>> WILDFIRE_UNIT = ENTITIES.register("wildfire_unit",
            () -> EntityType.Builder.of(WildfireUnit::new, MobCategory.CREATURE)
                    .fireImmune()
                    .sized(EntityType.BLAZE.getWidth(), EntityType.BLAZE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wildfire_unit").toString()));

    public static final Supplier<EntityType<PolarBearUnit>> POLAR_BEAR_UNIT = ENTITIES.register("polar_bear_unit",
            () -> EntityType.Builder.of(PolarBearUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.POLAR_BEAR.getWidth(), EntityType.POLAR_BEAR.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "polar_bear_unit").toString()));

    public static final Supplier<EntityType<GrizzlyBearUnit>> GRIZZLY_BEAR_UNIT = ENTITIES.register("grizzly_bear_unit",
            () -> EntityType.Builder.of(GrizzlyBearUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.POLAR_BEAR.getWidth(), EntityType.POLAR_BEAR.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "grizzly_bear_unit").toString()));

    public static final Supplier<EntityType<PandaUnit>> PANDA_UNIT = ENTITIES.register("panda_unit",
            () -> EntityType.Builder.of(PandaUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PANDA.getWidth(), EntityType.PANDA.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "panda_unit").toString()));

    public static final Supplier<EntityType<WolfUnit>> WOLF_UNIT = ENTITIES.register("wolf_unit",
            () -> EntityType.Builder.of(WolfUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.WOLF.getWidth(), EntityType.WOLF.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wolf_unit").toString()));

    public static final Supplier<EntityType<LlamaUnit>> LLAMA_UNIT = ENTITIES.register("llama_unit",
            () -> EntityType.Builder.of(LlamaUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.LLAMA.getWidth(), EntityType.LLAMA.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "llama_unit").toString()));

    public static final Supplier<EntityType<PhantomSummon>> PHANTOM_SUMMON = ENTITIES.register("phantom_summon",
            () -> EntityType.Builder.of(PhantomSummon::new, MobCategory.MONSTER)
                    .sized(EntityType.PHANTOM.getWidth(), EntityType.PHANTOM.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "phantom_summon").toString()));

    public static final Supplier<EntityType<HeroExperienceOrb>> HERO_EXPERIENCE_ORB = ENTITIES.register("hero_experience_orb",
            () -> EntityType.Builder.of(HeroExperienceOrb::new, MobCategory.MISC)
                    .sized(EntityType.EXPERIENCE_ORB.getWidth(), EntityType.EXPERIENCE_ORB.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "hero_experience_orb").toString()));

    public static final Supplier<EntityType<KillerRabbitUnit>> KILLER_RABBIT_UNIT = ENTITIES.register("killer_rabbit_unit",
            () -> EntityType.Builder.of(KillerRabbitUnit::new, MobCategory.MISC)
                    .sized(EntityType.RABBIT.getWidth(), EntityType.RABBIT.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "killer_rabbit_unit").toString()));

    public static final Supplier<EntityType<ThrowableTntProjectile>> THROWABLE_TNT_PROJECTILE = ENTITIES.register("tnt_throwable_projectile",
            () -> EntityType.Builder.<ThrowableTntProjectile>of(ThrowableTntProjectile::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "tnt_throwable_projectile").toString()));

    public static final Supplier<EntityType<ThrownHeroExperienceBottle>> THROWN_HERO_EXPERIENCE_BOTTLE = ENTITIES.register("thrown_hero_experience_bottle",
            () -> EntityType.Builder.<ThrownHeroExperienceBottle>of(ThrownHeroExperienceBottle::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "thrown_hero_experience_bottle").toString()));

    public static final Supplier<EntityType<AdjustablePrimedTnt>> ADJUSTABLE_PRIMED_TNT = ENTITIES.register("adjustable_primed_tnt",
            () -> EntityType.Builder.<AdjustablePrimedTnt>of(AdjustablePrimedTnt::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "adjustable_primed_tnt").toString()));

    public static final Supplier<EntityType<NecromancerProjectile>> NECROMANCER_PROJECTILE = ENTITIES.register("necromancer_projectile",
            () -> EntityType.Builder.<NecromancerProjectile>of(NecromancerProjectile::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "necromancer_projectile").toString()));

    public static final Supplier<EntityType<WindcallerProjectile>> WINDCALLER_PROJECTILE = ENTITIES.register("windcaller_projectile",
            () -> EntityType.Builder.<WindcallerProjectile>of(WindcallerProjectile::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "windcaller_projectile").toString()));

    public static final Supplier<EntityType<WraithSnowball>> WRAITH_SNOWBALL = ENTITIES.register("wraith_snowball",
            () -> EntityType.Builder.<WraithSnowball>of(WraithSnowball::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wraith_snowball").toString()));

    public static final Supplier<EntityType<MoltenBombProjectile>> MOLTEN_BOMB_PROJECTILE = ENTITIES.register("molten_bomb_projectile",
            () -> EntityType.Builder.<MoltenBombProjectile>of(MoltenBombProjectile::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "molten_bomb_projectile").toString()));

    public static final Supplier<EntityType<BeeUnit>> BEE_UNIT = ENTITIES.register("bee_unit",
            () -> EntityType.Builder.of(BeeUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.BEE.getWidth(), EntityType.BEE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "bee_unit").toString()));
    public static final Supplier<EntityType<TotemOfRegeneration>> TOTEM_OF_REGENERATION = ENTITIES.register("totem_of_regeneration",
            () -> EntityType.Builder.of(TotemOfRegeneration::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.75f, 1.25f)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "totem_of_regeneration").toString()));
    public static final Supplier<EntityType<TotemOfCasting>> TOTEM_OF_CASTING = ENTITIES.register("totem_of_casting",
            () -> EntityType.Builder.of(TotemOfCasting::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.75f, 1.25f)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "totem_of_casting").toString()));
    public static final Supplier<EntityType<TotemOfProtection>> TOTEM_OF_PROTECTION = ENTITIES.register("totem_of_protection",
            () -> EntityType.Builder.of(TotemOfProtection::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.75f, 1.25f)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "totem_of_protection").toString()));
    public static final Supplier<EntityType<TotemOfShielding>> TOTEM_OF_SHIELDING = ENTITIES.register("totem_of_shielding",
            () -> EntityType.Builder.of(TotemOfShielding::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.75f, 1.25f)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "totem_of_shielding").toString()));

    /** Maps a production-item name onto the entity that item summons. */
    public static EntityType<? extends Mob> getEntityType(String id) {
        return switch (id) {
            case "bee_unit" -> EntityRegistrar.BEE_UNIT.get();
            case "totem_of_regeneration" -> EntityRegistrar.TOTEM_OF_REGENERATION.get();
            case "totem_of_casting" -> EntityRegistrar.TOTEM_OF_CASTING.get();
            case "totem_of_protection" -> EntityRegistrar.TOTEM_OF_PROTECTION.get();
            case "totem_of_shielding" -> EntityRegistrar.TOTEM_OF_SHIELDING.get();
            case "Creeper" -> EntityRegistrar.CREEPER_UNIT.get();
            case "Skeleton" -> EntityRegistrar.SKELETON_UNIT.get();
            case "Zombie" -> EntityRegistrar.ZOMBIE_UNIT.get();
            case "Stray" -> EntityRegistrar.STRAY_UNIT.get();
            case "Bogged" -> EntityRegistrar.BOGGED_UNIT.get();
            case "Husk" -> EntityRegistrar.HUSK_UNIT.get();
            case "Drowned" -> EntityRegistrar.DROWNED_UNIT.get();
            case "Spider" -> EntityRegistrar.SPIDER_UNIT.get();
            case "Poison Spider" -> EntityRegistrar.POISON_SPIDER_UNIT.get();
            case "Wraith" -> EntityRegistrar.WRAITH_UNIT.get();
            case "Villager" -> EntityRegistrar.VILLAGER_UNIT.get();
            case "Scout Dog" -> EntityRegistrar.SCOUT_DOG_UNIT.get();
            case "Scout Cat" -> EntityRegistrar.SCOUT_CAT_UNIT.get();
            case "Zombie Villager" -> EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get();
            case "Bat" -> EntityRegistrar.BAT_UNIT.get();
            case "Vindicator" -> EntityRegistrar.VINDICATOR_UNIT.get();
            case "Pillager" -> EntityRegistrar.PILLAGER_UNIT.get();
            case "Windcaller" -> EntityRegistrar.WINDCALLER_UNIT.get();
            case "Iron Golem" -> EntityRegistrar.IRON_GOLEM_UNIT.get();
            case "Witch" -> EntityRegistrar.WITCH_UNIT.get();
            case "Evoker" -> EntityRegistrar.EVOKER_UNIT.get();
            case "Slime" -> EntityRegistrar.SLIME_UNIT.get();
            case "Warden" -> EntityRegistrar.WARDEN_UNIT.get();
            case "Ravager" -> EntityRegistrar.RAVAGER_UNIT.get();
            case "Grunt" -> EntityRegistrar.GRUNT_UNIT.get();
            case "Strider" -> EntityRegistrar.STRIDER_UNIT.get();
            case "Brute" -> EntityRegistrar.BRUTE_UNIT.get();
            case "Headhunter" -> EntityRegistrar.HEADHUNTER_UNIT.get();
            case "Marauder" -> EntityRegistrar.MARAUDER_UNIT.get();
            case "Hoglin" -> EntityRegistrar.HOGLIN_UNIT.get();
            case "Blaze" -> EntityRegistrar.BLAZE_UNIT.get();
            case "Wither Skeleton" -> EntityRegistrar.WITHER_SKELETON_UNIT.get();
            case "Magma Cube" -> EntityRegistrar.MAGMA_CUBE_UNIT.get();
            case "Ghast" -> EntityRegistrar.GHAST_UNIT.get();
            case "Necromancer" -> EntityRegistrar.NECROMANCER_UNIT.get();
            case "Piglin Merchant" -> EntityRegistrar.PIGLIN_MERCHANT_UNIT.get();
            case "Wildfire" -> EntityRegistrar.WILDFIRE_UNIT.get();
            case "Royal Guard" -> EntityRegistrar.ROYAL_GUARD_UNIT.get();
            case "Enchanter" -> EntityRegistrar.ENCHANTER_UNIT.get();
            case "Wretched Wraith" -> EntityRegistrar.WRETCHED_WRAITH_UNIT.get();
            case "Enderman" -> EntityRegistrar.ENDERMAN_UNIT.get();
            case "Zombie Piglin" -> EntityRegistrar.ZOMBIE_PIGLIN_UNIT.get();
            case "Zoglin" -> EntityRegistrar.ZOGLIN_UNIT.get();
            case "Polar Bear" -> EntityRegistrar.POLAR_BEAR_UNIT.get();
            case "Grizzly Bear" -> EntityRegistrar.GRIZZLY_BEAR_UNIT.get();
            case "Panda" -> EntityRegistrar.PANDA_UNIT.get();
            case "Wolf" -> EntityRegistrar.WOLF_UNIT.get();
            case "Llama" -> EntityRegistrar.LLAMA_UNIT.get();
            case "Killer Rabbit" -> EntityRegistrar.KILLER_RABBIT_UNIT.get();
            case "Militia" -> EntityRegistrar.MILITIA_UNIT.get();
            default -> null;
        };
    }

    public static void init(ModContainer container) {
        ENTITIES.register(container.getEventBus());
    }
}
