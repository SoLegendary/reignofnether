package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.research.researchItems.*;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.unit.units.neutral.*;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.unit.units.villagers.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

public class ProductionItems {
    public static final CreeperProd CREEPER = register(new ResourceLocation(ReignOfNether.MOD_ID, "creeper"), new CreeperProd());
    public static final SkeletonProd SKELETON = register(new ResourceLocation(ReignOfNether.MOD_ID, "skeleton"), new SkeletonProd());
    public static final ZombieProd ZOMBIE = register(new ResourceLocation(ReignOfNether.MOD_ID, "zombie"), new ZombieProd());
    public static final StrayProd STRAY = register(new ResourceLocation(ReignOfNether.MOD_ID, "stray"), new StrayProd());
    public static final HuskProd HUSK = register(new ResourceLocation(ReignOfNether.MOD_ID, "husk"), new HuskProd());
    public static final DrownedProd DROWNED = register(new ResourceLocation(ReignOfNether.MOD_ID, "drowned"), new DrownedProd());
    public static final SpiderProd SPIDER = register(new ResourceLocation(ReignOfNether.MOD_ID, "spider"), new SpiderProd());
    public static final PoisonSpiderProd POISON_SPIDER = register(new ResourceLocation(ReignOfNether.MOD_ID, "poison_spider"), new PoisonSpiderProd());
    public static final VillagerProd VILLAGER = register(new ResourceLocation(ReignOfNether.MOD_ID, "villager"), new VillagerProd());
    public static final ZombieVillagerProd ZOMBIE_VILLAGER = register(new ResourceLocation(ReignOfNether.MOD_ID, "zombie_villager"), new ZombieVillagerProd());
    public static final VindicatorProd VINDICATOR = register(new ResourceLocation(ReignOfNether.MOD_ID, "vindicator"), new VindicatorProd());
    public static final PillagerProd PILLAGER = register(new ResourceLocation(ReignOfNether.MOD_ID, "pillager"), new PillagerProd());
    public static final IronGolemProd IRON_GOLEM = register(new ResourceLocation(ReignOfNether.MOD_ID, "iron_golem"), new IronGolemProd());
    public static final WitchProd WITCH = register(new ResourceLocation(ReignOfNether.MOD_ID, "witch"), new WitchProd());
    public static final EvokerProd EVOKER = register(new ResourceLocation(ReignOfNether.MOD_ID, "evoker"), new EvokerProd());
    public static final SlimeProd SLIME = register(new ResourceLocation(ReignOfNether.MOD_ID, "slime"), new SlimeProd());
    public static final WardenProd WARDEN = register(new ResourceLocation(ReignOfNether.MOD_ID, "warden"), new WardenProd());
    public static final RavagerProd RAVAGER = register(new ResourceLocation(ReignOfNether.MOD_ID, "ravager"), new RavagerProd());

    public static final GruntProd GRUNT = register(new ResourceLocation(ReignOfNether.MOD_ID, "grunt"), new GruntProd());
    public static final BruteProd BRUTE = register(new ResourceLocation(ReignOfNether.MOD_ID, "brute"), new BruteProd());
    public static final HeadhunterProd HEADHUNTER = register(new ResourceLocation(ReignOfNether.MOD_ID, "headhunter"), new HeadhunterProd());
    public static final HoglinProd HOGLIN = register(new ResourceLocation(ReignOfNether.MOD_ID, "hoglin"), new HoglinProd());
    public static final BlazeProd BLAZE = register(new ResourceLocation(ReignOfNether.MOD_ID, "blaze"), new BlazeProd());
    public static final WitherSkeletonProd WITHER_SKELETON = register(new ResourceLocation(ReignOfNether.MOD_ID, "wither_skeleton"), new WitherSkeletonProd());
    public static final MagmaCubeProd MAGMA_CUBE = register(new ResourceLocation(ReignOfNether.MOD_ID, "magma_cube"), new MagmaCubeProd());
    public static final GhastProd GHAST = register(new ResourceLocation(ReignOfNether.MOD_ID, "ghast"), new GhastProd());
    public static final RoyalGuardProd ROYAL_GUARD = register(new ResourceLocation(ReignOfNether.MOD_ID, "royal_guard"), new RoyalGuardProd());
    public static final RoyalGuardReviveProd ROYAL_GUARD_REVIVE = register(new ResourceLocation(ReignOfNether.MOD_ID, "royal_guard_revive"), new RoyalGuardReviveProd());
    public static final NecromancerProd NECROMANCER = register(new ResourceLocation(ReignOfNether.MOD_ID, "necromancer"), new NecromancerProd());
    public static final NecromancerReviveProd NECROMANCER_REVIVE = register(new ResourceLocation(ReignOfNether.MOD_ID, "necromancer_revive"), new NecromancerReviveProd());
    public static final PiglinMerchantProd PIGLIN_MERCHANT = register(new ResourceLocation(ReignOfNether.MOD_ID, "piglin_merchant"), new PiglinMerchantProd());
    public static final PiglinMerchantReviveProd PIGLIN_MERCHANT_REVIVE = register(new ResourceLocation(ReignOfNether.MOD_ID, "piglin_merchant_revive"), new PiglinMerchantReviveProd());
    public static final ZombiePiglinProd ZOMBIE_PIGLIN = register(new ResourceLocation(ReignOfNether.MOD_ID, "zombie_piglin"), new ZombiePiglinProd());
    public static final ZoglinProd ZOGLIN = register(new ResourceLocation(ReignOfNether.MOD_ID, "zoglin"), new ZoglinProd());
    public static final EndermanProd ENDERMAN = register(new ResourceLocation(ReignOfNether.MOD_ID, "enderman"), new EndermanProd());
    public static final PolarBearProd POLAR_BEAR = register(new ResourceLocation(ReignOfNether.MOD_ID, "polar_bear"), new PolarBearProd());
    public static final GrizzlyBearProd GRIZZLY_BEAR = register(new ResourceLocation(ReignOfNether.MOD_ID, "grizzly_bear"), new GrizzlyBearProd());
    public static final PandaProd PANDA = register(new ResourceLocation(ReignOfNether.MOD_ID, "panda"), new PandaProd());
    public static final WolfProd WOLF = register(new ResourceLocation(ReignOfNether.MOD_ID, "wolf"), new WolfProd());
    public static final LlamaProd LLAMA = register(new ResourceLocation(ReignOfNether.MOD_ID, "llama"), new LlamaProd());

    public static final ResearchVindicatorAxes RESEARCH_VINDICATOR_AXES = register(new ResourceLocation(ReignOfNether.MOD_ID, "vindicator_axes"), new ResearchVindicatorAxes());
    public static final ResearchPillagerCrossbows RESEARCH_PILLAGER_CROSSBOWS = register(new ResourceLocation(ReignOfNether.MOD_ID, "pillager_crossbows"), new ResearchPillagerCrossbows());
    public static final ResearchMilitiaBows RESEARCH_MILITIA_BOWS = register(new ResourceLocation(ReignOfNether.MOD_ID, "militia_bows"), new ResearchMilitiaBows());
    public static final ResearchLabLightningRod RESEARCH_LAB_LIGHTNING_ROD = register(new ResourceLocation(ReignOfNether.MOD_ID, "lab_lightning_rod"), new ResearchLabLightningRod());
    public static final ResearchResourceCapacity RESEARCH_RESOURCE_CAPACITY = register(new ResourceLocation(ReignOfNether.MOD_ID, "resource_capacity"), new ResearchResourceCapacity());
    public static final ResearchSpiderJockeys RESEARCH_SPIDER_JOCKEYS = register(new ResourceLocation(ReignOfNether.MOD_ID, "spider_jockeys"), new ResearchSpiderJockeys());
    public static final ResearchPoisonSpiders RESEARCH_POISON_SPIDERS = register(new ResourceLocation(ReignOfNether.MOD_ID, "research_poison_spiders"), new ResearchPoisonSpiders());
    public static final ResearchHusks RESEARCH_HUSKS = register(new ResourceLocation(ReignOfNether.MOD_ID, "research_husks"), new ResearchHusks());
    public static final ResearchDrowned RESEARCH_DROWNED = register(new ResourceLocation(ReignOfNether.MOD_ID, "research_drowned"), new ResearchDrowned());
    public static final ResearchStrays RESEARCH_STRAYS = register(new ResourceLocation(ReignOfNether.MOD_ID, "research_strays"), new ResearchStrays());
    public static final ResearchSlimeConversion RESEARCH_SLIME_CONVERSION = register(new ResourceLocation(ReignOfNether.MOD_ID, "slime_conversion"), new ResearchSlimeConversion());
    public static final ResearchLingeringPotions RESEARCH_LINGERING_POTIONS = register(new ResourceLocation(ReignOfNether.MOD_ID, "lingering_potions"), new ResearchLingeringPotions());
    public static final ResearchWaterPotions RESEARCH_WATER_POTIONS = register(new ResourceLocation(ReignOfNether.MOD_ID, "water_potions"), new ResearchWaterPotions());
    public static final ResearchHealingPotions RESEARCH_HEALING_POTIONS = register(new ResourceLocation(ReignOfNether.MOD_ID, "healing_potions"), new ResearchHealingPotions());
    public static final ResearchEvokerVexes RESEARCH_EVOKER_VEXES = register(new ResourceLocation(ReignOfNether.MOD_ID, "evoker_vexes"), new ResearchEvokerVexes());
    public static final ResearchGolemSmithing RESEARCH_GOLEM_SMITHING = register(new ResourceLocation(ReignOfNether.MOD_ID, "golem_smithing"), new ResearchGolemSmithing());
    public static final ResearchSilverfish RESEARCH_SILVERFISH = register(new ResourceLocation(ReignOfNether.MOD_ID, "silverfish"), new ResearchSilverfish());
    public static final ResearchSculkAmplifiers RESEARCH_SCULK_AMPLIFIERS = register(new ResourceLocation(ReignOfNether.MOD_ID, "sculk_amplifiers"), new ResearchSculkAmplifiers());
    public static final ResearchCastleFlag RESEARCH_CASTLE_FLAG = register(new ResourceLocation(ReignOfNether.MOD_ID, "castle_flag"), new ResearchCastleFlag());
    public static final ResearchRavagerCavalry RESEARCH_RAVAGER_CAVALRY = register(new ResourceLocation(ReignOfNether.MOD_ID, "ravager_cavalry"), new ResearchRavagerCavalry());
    public static final ResearchBruteShields RESEARCH_BRUTE_SHIELDS = register(new ResourceLocation(ReignOfNether.MOD_ID, "brute_shields"), new ResearchBruteShields());
    public static final ResearchHoglinCavalry RESEARCH_HOGLIN_CAVALRY = register(new ResourceLocation(ReignOfNether.MOD_ID, "hoglin_cavalry"), new ResearchHoglinCavalry());
    public static final ResearchHeavyTridents RESEARCH_HEAVY_TRIDENTS = register(new ResourceLocation(ReignOfNether.MOD_ID, "heavy_tridents"), new ResearchHeavyTridents());
    public static final ResearchBlazeFirewall RESEARCH_BLAZE_FIREWALL = register(new ResourceLocation(ReignOfNether.MOD_ID, "blaze_firewall"), new ResearchBlazeFirewall());
    public static final ResearchWitherClouds RESEARCH_WITHER_CLOUDS = register(new ResourceLocation(ReignOfNether.MOD_ID, "wither_clouds"), new ResearchWitherClouds());
    public static final ResearchAdvancedPortals RESEARCH_ADVANCED_PORTALS = register(new ResourceLocation(ReignOfNether.MOD_ID, "advanced_portals"), new ResearchAdvancedPortals());
    public static final ResearchFireResistance RESEARCH_FIRE_RESISTANCE = register(new ResourceLocation(ReignOfNether.MOD_ID, "fire_resistance"), new ResearchFireResistance());
    public static final ResearchGrandLibrary RESEARCH_GRAND_LIBRARY = register(new ResourceLocation(ReignOfNether.MOD_ID, "grand_library"), new ResearchGrandLibrary());
    public static final ResearchSpiderWebs RESEARCH_SPIDER_WEBS = register(new ResourceLocation(ReignOfNether.MOD_ID, "spider_webs"), new ResearchSpiderWebs());
    public static final ResearchBloodlust RESEARCH_BLOODLUST = register(new ResourceLocation(ReignOfNether.MOD_ID, "bloodlust"), new ResearchBloodlust());
    public static final ResearchCubeMagma RESEARCH_CUBE_MAGMA = register(new ResourceLocation(ReignOfNether.MOD_ID, "cube_magma"), new ResearchCubeMagma());
    public static final ResearchSoulFireballs RESEARCH_SOUL_FIREBALLS = register(new ResourceLocation(ReignOfNether.MOD_ID, "soul_fireballs"), new ResearchSoulFireballs());

    public static final ResearchPortalForCivilian RESEARCH_PORTAL_FOR_CIVILIAN = register(new ResourceLocation(ReignOfNether.MOD_ID, "portal_for_civilian"), new ResearchPortalForCivilian());
    public static final ResearchPortalForMilitary RESEARCH_PORTAL_FOR_MILITARY = register(new ResourceLocation(ReignOfNether.MOD_ID, "portal_for_military"), new ResearchPortalForMilitary());
    public static final ResearchPortalForTransport RESEARCH_PORTAL_FOR_TRANSPORT = register(new ResourceLocation(ReignOfNether.MOD_ID, "portal_for_transport"), new ResearchPortalForTransport());
    public static final ResearchBeaconLevel1 RESEARCH_BEACON_LEVEL_1 = register(new ResourceLocation(ReignOfNether.MOD_ID, "beacon_level_1"), new ResearchBeaconLevel1());
    public static final ResearchBeaconLevel2 RESEARCH_BEACON_LEVEL_2 = register(new ResourceLocation(ReignOfNether.MOD_ID, "beacon_level_2"), new ResearchBeaconLevel2());
    public static final ResearchBeaconLevel3 RESEARCH_BEACON_LEVEL_3 = register(new ResourceLocation(ReignOfNether.MOD_ID, "beacon_level_3"), new ResearchBeaconLevel3());
    public static final ResearchBeaconLevel4 RESEARCH_BEACON_LEVEL_4 = register(new ResourceLocation(ReignOfNether.MOD_ID, "beacon_level_4"), new ResearchBeaconLevel4());
    public static final ResearchBeaconLevel5 RESEARCH_BEACON_LEVEL_5 = register(new ResourceLocation(ReignOfNether.MOD_ID, "beacon_level_5"), new ResearchBeaconLevel5());



    private static <T extends ProductionItem> T register(ResourceLocation id, T building) {
        return Registry.register(ReignOfNetherRegistries.PRODUCTION_ITEM, id, building);
    }

    public static void init() {}
}
