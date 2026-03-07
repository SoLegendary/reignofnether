package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.entities.*;
import com.solegendary.reignofnether.hero.HeroExperienceOrb;
import com.solegendary.reignofnether.unit.modelling.renderers.*;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.unit.units.neutral.*;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.unit.units.villagers.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class EntityRegistrar {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ReignOfNether.MOD_ID);

    private static final int UNIT_CLIENT_TRACKING_RANGE = 100;

    public static final RegistryObject<EntityType<ZombieVillagerUnit>> ZOMBIE_VILLAGER_UNIT = ENTITIES.register("zombie_villager_unit",
            () -> EntityType.Builder.of(ZombieVillagerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ZOMBIE_VILLAGER.getWidth(), EntityType.ZOMBIE_VILLAGER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "zombie_villager_unit").toString()));

    public static final RegistryObject<EntityType<ZombieUnit>> ZOMBIE_UNIT = ENTITIES.register("zombie_unit",
            // can add other attributes here like sized() for hitbox, no summon, fire immunity, etc.
            () -> EntityType.Builder.of(ZombieUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ZOMBIE.getWidth(), EntityType.ZOMBIE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "zombie_unit").toString()));

    public static final RegistryObject<EntityType<HuskUnit>> HUSK_UNIT = ENTITIES.register("husk_unit",
            () -> EntityType.Builder.of(HuskUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.HUSK.getWidth(), EntityType.HUSK.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "husk_unit").toString()));

    public static final RegistryObject<EntityType<DrownedUnit>> DROWNED_UNIT = ENTITIES.register("drowned_unit",
            () -> EntityType.Builder.of(DrownedUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.DROWNED.getWidth(), EntityType.DROWNED.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "drowned_unit").toString()));

    public static final RegistryObject<EntityType<ZombiePiglinUnit>> ZOMBIE_PIGLIN_UNIT = ENTITIES.register("zombie_piglin_unit",
            () -> EntityType.Builder.of(ZombiePiglinUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ZOMBIFIED_PIGLIN.getWidth(), EntityType.ZOMBIFIED_PIGLIN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "zombie_piglin_unit").toString()));

    public static final RegistryObject<EntityType<ZoglinUnit>> ZOGLIN_UNIT = ENTITIES.register("zoglin_unit",
            () -> EntityType.Builder.of(ZoglinUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ZOGLIN.getWidth(), EntityType.ZOGLIN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "zoglin_unit").toString()));

    public static final RegistryObject<EntityType<SkeletonUnit>> SKELETON_UNIT = ENTITIES.register("skeleton_unit",
            () -> EntityType.Builder.of(SkeletonUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SKELETON.getWidth(), EntityType.SKELETON.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "skeleton_unit").toString()));

    public static final RegistryObject<EntityType<StrayUnit>> STRAY_UNIT = ENTITIES.register("stray_unit",
            () -> EntityType.Builder.of(StrayUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.STRAY.getWidth(), EntityType.STRAY.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "stray_unit").toString()));

    public static final RegistryObject<EntityType<BoggedUnit>> BOGGED_UNIT = ENTITIES.register("bogged_unit",
            () -> EntityType.Builder.of(BoggedUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SKELETON.getWidth(), EntityType.SKELETON.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "bogged_unit").toString()));

    public static final RegistryObject<EntityType<CreeperUnit>> CREEPER_UNIT = ENTITIES.register("creeper_unit",
            () -> EntityType.Builder.of(CreeperUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.CREEPER.getWidth(), EntityType.CREEPER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "creeper_unit").toString()));

    public static final RegistryObject<EntityType<SpiderUnit>> SPIDER_UNIT = ENTITIES.register("spider_unit",
            () -> EntityType.Builder.of(SpiderUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SPIDER.getWidth(), EntityType.SPIDER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "spider_unit").toString()));

    public static final RegistryObject<EntityType<PoisonSpiderUnit>> POISON_SPIDER_UNIT = ENTITIES.register("cave_spider_unit",
            () -> EntityType.Builder.of(PoisonSpiderUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SPIDER.getWidth(), EntityType.SPIDER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "cave_spider_unit").toString()));

    public static final RegistryObject<EntityType<VillagerUnit>> VILLAGER_UNIT = ENTITIES.register("villager_unit",
            () -> EntityType.Builder.of(VillagerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VILLAGER.getWidth(), EntityType.VILLAGER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "villager_unit").toString()));

    public static final RegistryObject<EntityType<MilitiaUnit>> MILITIA_UNIT = ENTITIES.register("militia_unit",
            () -> EntityType.Builder.of(MilitiaUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VILLAGER.getWidth(), EntityType.VILLAGER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "militia_unit").toString()));

    public static final RegistryObject<EntityType<VindicatorUnit>> VINDICATOR_UNIT = ENTITIES.register("vindicator_unit",
            () -> EntityType.Builder.of(VindicatorUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VINDICATOR.getWidth(), EntityType.VINDICATOR.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "vindicator_unit").toString()));

    public static final RegistryObject<EntityType<PillagerUnit>> PILLAGER_UNIT = ENTITIES.register("pillager_unit",
            () -> EntityType.Builder.of(PillagerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PILLAGER.getWidth(), EntityType.PILLAGER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "pillager_unit").toString()));

    public static final RegistryObject<EntityType<IronGolemUnit>> IRON_GOLEM_UNIT = ENTITIES.register("iron_golem_unit",
            () -> EntityType.Builder.of(IronGolemUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.IRON_GOLEM.getWidth(), EntityType.IRON_GOLEM.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "iron_golem_unit").toString()));

    public static final RegistryObject<EntityType<WitchUnit>> WITCH_UNIT = ENTITIES.register("witch_unit",
            () -> EntityType.Builder.of(WitchUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.WITCH.getWidth(), EntityType.WITCH.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "witch_unit").toString()));

    public static final RegistryObject<EntityType<EvokerUnit>> EVOKER_UNIT = ENTITIES.register("evoker_unit",
            () -> EntityType.Builder.of(EvokerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.EVOKER.getWidth(), EntityType.EVOKER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "evoker_unit").toString()));

    public static final RegistryObject<EntityType<EndermanUnit>> ENDERMAN_UNIT = ENTITIES.register("enderman_unit",
            () -> EntityType.Builder.of(EndermanUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ENDERMAN.getWidth(), EntityType.ENDERMAN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "enderman_unit").toString()));

    public static final RegistryObject<EntityType<RavagerUnit>> RAVAGER_UNIT = ENTITIES.register("ravager_unit",
            () -> EntityType.Builder.of(RavagerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.RAVAGER.getWidth(), EntityType.RAVAGER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "ravager_unit").toString()));

    public static final RegistryObject<EntityType<WardenUnit>> WARDEN_UNIT = ENTITIES.register("warden_unit",
            () -> EntityType.Builder.of(WardenUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.WARDEN.getWidth(), EntityType.WARDEN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "warden_unit").toString()));

    public static final RegistryObject<EntityType<SilverfishUnit>> SILVERFISH_UNIT = ENTITIES.register("silverfish_unit",
            () -> EntityType.Builder.of(SilverfishUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SILVERFISH.getWidth(), EntityType.SILVERFISH.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "silverfish_unit").toString()));

    public static final RegistryObject<EntityType<GruntUnit>> GRUNT_UNIT = ENTITIES.register("grunt_unit",
            () -> EntityType.Builder.of(GruntUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PIGLIN.getWidth(), EntityType.PIGLIN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "grunt_unit").toString()));

    public static final RegistryObject<EntityType<BruteUnit>> BRUTE_UNIT = ENTITIES.register("brute_unit",
            () -> EntityType.Builder.of(BruteUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PIGLIN_BRUTE.getWidth(), EntityType.PIGLIN_BRUTE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "brute_unit").toString()));

    public static final RegistryObject<EntityType<HeadhunterUnit>> HEADHUNTER_UNIT = ENTITIES.register("headhunter_unit",
            () -> EntityType.Builder.of(HeadhunterUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PIGLIN_BRUTE.getWidth(), EntityType.PIGLIN_BRUTE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "headhunter_unit").toString()));

    public static final RegistryObject<EntityType<MarauderUnit>> MARAUDER_UNIT = ENTITIES.register("marauder_unit",
            () -> EntityType.Builder.of(MarauderUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PIGLIN_BRUTE.getWidth(), EntityType.PIGLIN_BRUTE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "marauder_unit").toString()));

    public static final RegistryObject<EntityType<HoglinUnit>> HOGLIN_UNIT = ENTITIES.register("hoglin_unit",
            () -> EntityType.Builder.of(HoglinUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.HOGLIN.getWidth(), EntityType.HOGLIN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "hoglin_unit").toString()));

    public static final RegistryObject<EntityType<HoglinUnit>> ARMOURED_HOGLIN_UNIT = ENTITIES.register("armoured_hoglin_unit",
            () -> EntityType.Builder.of(HoglinUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.HOGLIN.getWidth(), EntityType.HOGLIN.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "armoured_hoglin_unit").toString()));

    public static final RegistryObject<EntityType<BlazeUnit>> BLAZE_UNIT = ENTITIES.register("blaze_unit",
            () -> EntityType.Builder.of(BlazeUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.BLAZE.getWidth(), EntityType.BLAZE.getHeight())
                    .fireImmune()
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "blaze_unit").toString()));

    public static final RegistryObject<EntityType<WitherSkeletonUnit>> WITHER_SKELETON_UNIT = ENTITIES.register("wither_skeleton_unit",
            () -> EntityType.Builder.of(WitherSkeletonUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.WITHER_SKELETON.getWidth(), EntityType.WITHER_SKELETON.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wither_skeleton_unit").toString()));

    public static final RegistryObject<EntityType<GhastUnit>> GHAST_UNIT = ENTITIES.register("ghast_unit",
            () -> EntityType.Builder.of(GhastUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.GHAST.getWidth() * GhastUnitRenderer.SCALE_MULT,
                            EntityType.GHAST.getHeight() * GhastUnitRenderer.SCALE_MULT)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "ghast_unit").toString()));

    public static final RegistryObject<EntityType<MagmaCubeUnit>> MAGMA_CUBE_UNIT = ENTITIES.register("magma_cube_unit",
            () -> EntityType.Builder.of(MagmaCubeUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.MAGMA_CUBE.getWidth(), EntityType.MAGMA_CUBE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "magma_cube_unit").toString()));

    public static final RegistryObject<EntityType<SlimeUnit>> SLIME_UNIT = ENTITIES.register("slime_unit",
            () -> EntityType.Builder.of(SlimeUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SLIME.getWidth(), EntityType.SLIME.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "slime_unit").toString()));

    public static final RegistryObject<EntityType<RoyalGuardUnit>> ROYAL_GUARD_UNIT = ENTITIES.register("royal_guard_unit",
            () -> EntityType.Builder.of(RoyalGuardUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VINDICATOR.getWidth(),
                            EntityType.VINDICATOR.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "royal_guard_unit").toString()));

    public static final RegistryObject<EntityType<EnchanterUnit>> ENCHANTER_UNIT = ENTITIES.register("enchanter_unit",
            () -> EntityType.Builder.of(EnchanterUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VINDICATOR.getWidth(),
                            EntityType.VINDICATOR.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "enchanter_unit").toString()));

    public static final RegistryObject<EntityType<NecromancerUnit>> NECROMANCER_UNIT = ENTITIES.register("necromancer_unit",
            () -> EntityType.Builder.of(NecromancerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SKELETON.getWidth() * NecromancerRenderer.SCALE_MULT,
                            EntityType.SKELETON.getHeight() * NecromancerRenderer.SCALE_MULT)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "necromancer_unit").toString()));

    public static final RegistryObject<EntityType<WretchedWraithUnit>> WRETCHED_WRAITH_UNIT = ENTITIES.register("wretched_wraith_unit",
            () -> EntityType.Builder.of(WretchedWraithUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ZOMBIE.getWidth(), EntityType.ZOMBIE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretched_wraith_unit").toString()));

    public static final RegistryObject<EntityType<PiglinMerchantUnit>> PIGLIN_MERCHANT_UNIT = ENTITIES.register("piglin_merchant_unit",
            () -> EntityType.Builder.of(PiglinMerchantUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PIGLIN_BRUTE.getWidth(), EntityType.PIGLIN_BRUTE.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "piglin_merchant_unit").toString()));

    public static final RegistryObject<EntityType<WildfireUnit>> WILDFIRE_UNIT = ENTITIES.register("wildfire_unit",
            () -> EntityType.Builder.of(WildfireUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.BLAZE.getWidth() * WildfireRenderer.SCALE_MULT,
                            EntityType.BLAZE.getHeight() * WildfireRenderer.SCALE_MULT)
                    .fireImmune()
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wildfire_unit").toString()));

    public static final RegistryObject<EntityType<PolarBearUnit>> POLAR_BEAR_UNIT = ENTITIES.register("polar_bear_unit",
            () -> EntityType.Builder.of(PolarBearUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.POLAR_BEAR.getWidth(), EntityType.POLAR_BEAR.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "polar_bear_unit").toString()));

    public static final RegistryObject<EntityType<GrizzlyBearUnit>> GRIZZLY_BEAR_UNIT = ENTITIES.register("grizzly_bear_unit",
            () -> EntityType.Builder.of(GrizzlyBearUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.POLAR_BEAR.getWidth(), EntityType.POLAR_BEAR.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "grizzly_bear_unit").toString()));

    public static final RegistryObject<EntityType<PandaUnit>> PANDA_UNIT = ENTITIES.register("panda_unit",
            () -> EntityType.Builder.of(PandaUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PANDA.getWidth(), EntityType.PANDA.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "panda_unit").toString()));

    public static final RegistryObject<EntityType<WolfUnit>> WOLF_UNIT = ENTITIES.register("wolf_unit",
            () -> EntityType.Builder.of(WolfUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.WOLF.getWidth(), EntityType.WOLF.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wolf_unit").toString()));

    public static final RegistryObject<EntityType<LlamaUnit>> LLAMA_UNIT = ENTITIES.register("llama_unit",
            () -> EntityType.Builder.of(LlamaUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.LLAMA.getWidth(), EntityType.LLAMA.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "llama_unit").toString()));

    public static final RegistryObject<EntityType<PhantomSummon>> PHANTOM_SUMMON = ENTITIES.register("phantom_summon",
            () -> EntityType.Builder.of(PhantomSummon::new, MobCategory.MONSTER)
                    .sized(EntityType.PHANTOM.getWidth(), EntityType.PHANTOM.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "phantom_summon").toString()));

    public static final RegistryObject<EntityType<HeroExperienceOrb>> HERO_EXPERIENCE_ORB = ENTITIES.register("hero_experience_orb",
            () -> EntityType.Builder.of(HeroExperienceOrb::new, MobCategory.MISC)
                    .sized(EntityType.EXPERIENCE_ORB.getWidth(), EntityType.EXPERIENCE_ORB.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "hero_experience_orb").toString()));

    public static final RegistryObject<EntityType<KillerRabbitUnit>> KILLER_RABBIT_UNIT = ENTITIES.register("killer_rabbit_unit",
            () -> EntityType.Builder.of(KillerRabbitUnit::new, MobCategory.MISC)
                    .sized(EntityType.RABBIT.getWidth(), EntityType.RABBIT.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "killer_rabbit_unit").toString()));

    public static final RegistryObject<EntityType<ThrowableTntProjectile>> THROWABLE_TNT_PROJECTILE = ENTITIES.register("tnt_throwable_projectile",
            () -> EntityType.Builder.<ThrowableTntProjectile>of(ThrowableTntProjectile::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "tnt_throwable_projectile").toString()));

    public static final RegistryObject<EntityType<ThrownHeroExperienceBottle>> THROWN_HERO_EXPERIENCE_BOTTLE = ENTITIES.register("thrown_hero_experience_bottle",
            () -> EntityType.Builder.<ThrownHeroExperienceBottle>of(ThrownHeroExperienceBottle::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "thrown_hero_experience_bottle").toString()));

    public static final RegistryObject<EntityType<AdjustablePrimedTnt>> ADJUSTABLE_PRIMED_TNT = ENTITIES.register("adjustable_primed_tnt",
            () -> EntityType.Builder.<AdjustablePrimedTnt>of(AdjustablePrimedTnt::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "adjustable_primed_tnt").toString()));

    public static final RegistryObject<EntityType<NecromancerProjectile>> NECROMANCER_PROJECTILE = ENTITIES.register("necromancer_projectile",
            () -> EntityType.Builder.<NecromancerProjectile>of(NecromancerProjectile::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "necromancer_projectile").toString()));

    public static final RegistryObject<EntityType<WraithSnowball>> WRAITH_SNOWBALL = ENTITIES.register("wraith_snowball",
            () -> EntityType.Builder.of(WraithSnowball::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wraith_snowball").toString()));

    public static final RegistryObject<EntityType<MoltenBombProjectile>> MOLTEN_BOMB_PROJECTILE = ENTITIES.register("molten_bomb_projectile",
            () -> EntityType.Builder.<MoltenBombProjectile>of(MoltenBombProjectile::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.6f, 0.6f)
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "molten_bomb_projectile").toString()));

    public static void init(FMLJavaModLoadingContext context) {
        ENTITIES.register(context.getModEventBus());
    }

    public static EntityType<? extends Mob> getEntityType(String unitName) {
        return switch(unitName) {
            case CreeperProd.itemName -> EntityRegistrar.CREEPER_UNIT.get();
            case SkeletonProd.itemName -> EntityRegistrar.SKELETON_UNIT.get();
            case ZombieProd.itemName -> EntityRegistrar.ZOMBIE_UNIT.get();
            case StrayProd.itemName -> EntityRegistrar.STRAY_UNIT.get();
            case BoggedProd.itemName -> EntityRegistrar.BOGGED_UNIT.get();
            case HuskProd.itemName -> EntityRegistrar.HUSK_UNIT.get();
            case DrownedProd.itemName -> EntityRegistrar.DROWNED_UNIT.get();
            case SpiderProd.itemName -> EntityRegistrar.SPIDER_UNIT.get();
            case PoisonSpiderProd.itemName -> EntityRegistrar.POISON_SPIDER_UNIT.get();
            case VillagerProd.itemName -> EntityRegistrar.VILLAGER_UNIT.get();
            case ZombieVillagerProd.itemName -> EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get();
            case VindicatorProd.itemName -> EntityRegistrar.VINDICATOR_UNIT.get();
            case PillagerProd.itemName -> EntityRegistrar.PILLAGER_UNIT.get();
            case IronGolemProd.itemName -> EntityRegistrar.IRON_GOLEM_UNIT.get();
            case WitchProd.itemName -> EntityRegistrar.WITCH_UNIT.get();
            case EvokerProd.itemName -> EntityRegistrar.EVOKER_UNIT.get();
            case SlimeProd.itemName -> EntityRegistrar.SLIME_UNIT.get();
            case WardenProd.itemName -> EntityRegistrar.WARDEN_UNIT.get();
            case RavagerProd.itemName -> EntityRegistrar.RAVAGER_UNIT.get();
            case GruntProd.itemName -> EntityRegistrar.GRUNT_UNIT.get();
            case BruteProd.itemName -> EntityRegistrar.BRUTE_UNIT.get();
            case HeadhunterProd.itemName -> EntityRegistrar.HEADHUNTER_UNIT.get();
            case MarauderProd.itemName -> EntityRegistrar.MARAUDER_UNIT.get();
            case HoglinProd.itemName -> EntityRegistrar.HOGLIN_UNIT.get();
            case BlazeProd.itemName -> EntityRegistrar.BLAZE_UNIT.get();
            case WitherSkeletonProd.itemName -> EntityRegistrar.WITHER_SKELETON_UNIT.get();
            case MagmaCubeProd.itemName -> EntityRegistrar.MAGMA_CUBE_UNIT.get();
            case GhastProd.itemName -> EntityRegistrar.GHAST_UNIT.get();
            case NecromancerProd.itemName -> EntityRegistrar.NECROMANCER_UNIT.get();
            case PiglinMerchantProd.itemName -> EntityRegistrar.PIGLIN_MERCHANT_UNIT.get();
            case WildfireProd.itemName -> EntityRegistrar.WILDFIRE_UNIT.get();
            case RoyalGuardProd.itemName -> EntityRegistrar.ROYAL_GUARD_UNIT.get();
            case EnchanterProd.itemName -> EntityRegistrar.ENCHANTER_UNIT.get();
            case WretchedWraithProd.itemName -> EntityRegistrar.WRETCHED_WRAITH_UNIT.get();
            case EndermanProd.itemName -> EntityRegistrar.ENDERMAN_UNIT.get();
            case ZombiePiglinProd.itemName -> EntityRegistrar.ZOMBIE_PIGLIN_UNIT.get();
            case ZoglinProd.itemName -> EntityRegistrar.ZOGLIN_UNIT.get();
            case PolarBearProd.itemName -> EntityRegistrar.POLAR_BEAR_UNIT.get();
            case GrizzlyBearProd.itemName -> EntityRegistrar.GRIZZLY_BEAR_UNIT.get();
            case PandaProd.itemName -> EntityRegistrar.PANDA_UNIT.get();
            case WolfProd.itemName -> EntityRegistrar.WOLF_UNIT.get();
            case LlamaProd.itemName -> EntityRegistrar.LLAMA_UNIT.get();
            case "Killer Rabbit" -> EntityRegistrar.KILLER_RABBIT_UNIT.get();
            default -> null;
        };
    }
}