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
    public static final CreeperProd CREEPER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "creeper"), new CreeperProd());
    public static final SkeletonProd SKELETON = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "skeleton"), new SkeletonProd());
    public static final BoggedProd BOGGED = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "bogged"), new BoggedProd());
    public static final ZombieProd ZOMBIE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "zombie"), new ZombieProd());
    public static final StrayProd STRAY = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "stray"), new StrayProd());
    public static final HuskProd HUSK = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "husk"), new HuskProd());
    public static final DrownedProd DROWNED = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "drowned"), new DrownedProd());
    public static final SpiderProd SPIDER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "spider"), new SpiderProd());
    public static final PoisonSpiderProd POISON_SPIDER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "poison_spider"), new PoisonSpiderProd());
    public static final VillagerProd VILLAGER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "villager"), new VillagerProd());
    public static final ZombieVillagerProd ZOMBIE_VILLAGER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "zombie_villager"), new ZombieVillagerProd());
    public static final VindicatorProd VINDICATOR = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "vindicator"), new VindicatorProd());
    public static final PillagerProd PILLAGER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "pillager"), new PillagerProd());
    public static final IronGolemProd IRON_GOLEM = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "iron_golem"), new IronGolemProd());
    public static final WitchProd WITCH = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "witch"), new WitchProd());
    public static final EvokerProd EVOKER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "evoker"), new EvokerProd());
    public static final SlimeProd SLIME = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "slime"), new SlimeProd());
    public static final WardenProd WARDEN = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "warden"), new WardenProd());
    public static final RavagerProd RAVAGER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "ravager"), new RavagerProd());

    public static final GruntProd GRUNT = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "grunt"), new GruntProd());
    public static final BruteProd BRUTE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "brute"), new BruteProd());
    public static final HeadhunterProd HEADHUNTER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "headhunter"), new HeadhunterProd());
    public static final MarauderProd MARAUDER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "marauder"), new MarauderProd());
    public static final HoglinProd HOGLIN = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "hoglin"), new HoglinProd());
    public static final BlazeProd BLAZE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "blaze"), new BlazeProd());
    public static final WitherSkeletonProd WITHER_SKELETON = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wither_skeleton"), new WitherSkeletonProd());
    public static final MagmaCubeProd MAGMA_CUBE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "magma_cube"), new MagmaCubeProd());
    public static final GhastProd GHAST = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "ghast"), new GhastProd());
    public static final RoyalGuardProd ROYAL_GUARD = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "royal_guard"), new RoyalGuardProd());
    public static final RoyalGuardReviveProd ROYAL_GUARD_REVIVE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "royal_guard_revive"), new RoyalGuardReviveProd());
    public static final EnchanterProd ENCHANTER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "enchanter"), new EnchanterProd());
    public static final EnchanterReviveProd ENCHANTER_REVIVE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "enchanter_revive"), new EnchanterReviveProd());
    public static final NecromancerProd NECROMANCER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "necromancer"), new NecromancerProd());
    public static final NecromancerReviveProd NECROMANCER_REVIVE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "necromancer_revive"), new NecromancerReviveProd());
    public static final WretchedWraithProd WRETCHED_WRAITH = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretched_wraith"), new WretchedWraithProd());
    public static final WretchedWraithReviveProd WRETCHED_WRAITH_REVIVE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretched_wraith_revive"), new WretchedWraithReviveProd());
    public static final PiglinMerchantProd PIGLIN_MERCHANT = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "piglin_merchant"), new PiglinMerchantProd());
    public static final PiglinMerchantReviveProd PIGLIN_MERCHANT_REVIVE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "piglin_merchant_revive"), new PiglinMerchantReviveProd());
    public static final WildfireProd WILDFIRE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wildfire"), new WildfireProd());
    public static final WildfireReviveProd WILDFIRE_REVIVE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wildfire_revive"), new WildfireReviveProd());
    public static final ZombiePiglinProd ZOMBIE_PIGLIN = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "zombie_piglin"), new ZombiePiglinProd());
    public static final ZoglinProd ZOGLIN = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "zoglin"), new ZoglinProd());
    public static final EndermanProd ENDERMAN = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "enderman"), new EndermanProd());
    public static final PolarBearProd POLAR_BEAR = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "polar_bear"), new PolarBearProd());
    public static final GrizzlyBearProd GRIZZLY_BEAR = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "grizzly_bear"), new GrizzlyBearProd());
    public static final PandaProd PANDA = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "panda"), new PandaProd());
    public static final WolfProd WOLF = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wolf"), new WolfProd());
    public static final LlamaProd LLAMA = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "llama"), new LlamaProd());

    public static final ResearchVindicatorAxes RESEARCH_VINDICATOR_AXES = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "vindicator_axes"), new ResearchVindicatorAxes());
    public static final ResearchPillagerCrossbows RESEARCH_PILLAGER_CROSSBOWS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "pillager_crossbows"), new ResearchPillagerCrossbows());
    public static final ResearchMilitiaBows RESEARCH_MILITIA_BOWS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "militia_bows"), new ResearchMilitiaBows());
    public static final ResearchLabLightningRod RESEARCH_LAB_LIGHTNING_ROD = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "lab_lightning_rod"), new ResearchLabLightningRod());
    public static final ResearchResourceCapacity RESEARCH_RESOURCE_CAPACITY = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "resource_capacity"), new ResearchResourceCapacity());
    public static final ResearchSpiderJockeys RESEARCH_SPIDER_JOCKEYS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "spider_jockeys"), new ResearchSpiderJockeys());
    public static final ResearchPoisonSpiders RESEARCH_POISON_SPIDERS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "research_poison_spiders"), new ResearchPoisonSpiders());
    public static final ResearchHusks RESEARCH_HUSKS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "research_husks"), new ResearchHusks());
    public static final ResearchDrowned RESEARCH_DROWNED = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "research_drowned"), new ResearchDrowned());
    public static final ResearchStrays RESEARCH_STRAYS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "research_strays"), new ResearchStrays());
    public static final ResearchBogged RESEARCH_BOGGED = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "research_bogged"), new ResearchBogged());
    public static final ResearchSlimeConversion RESEARCH_SLIME_CONVERSION = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "slime_conversion"), new ResearchSlimeConversion());
    public static final ResearchLingeringPotions RESEARCH_LINGERING_POTIONS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "lingering_potions"), new ResearchLingeringPotions());
    public static final ResearchWaterPotions RESEARCH_WATER_POTIONS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "water_potions"), new ResearchWaterPotions());
    public static final ResearchHealingPotions RESEARCH_HEALING_POTIONS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "healing_potions"), new ResearchHealingPotions());
    public static final ResearchEvokerVexes RESEARCH_EVOKER_VEXES = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "evoker_vexes"), new ResearchEvokerVexes());
    public static final ResearchGolemSmithing RESEARCH_GOLEM_SMITHING = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "golem_smithing"), new ResearchGolemSmithing());
    public static final ResearchSilverfish RESEARCH_SILVERFISH = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "silverfish"), new ResearchSilverfish());
    public static final ResearchSculkAmplifiers RESEARCH_SCULK_AMPLIFIERS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "sculk_amplifiers"), new ResearchSculkAmplifiers());
    public static final ResearchCastleFlag RESEARCH_CASTLE_FLAG = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "castle_flag"), new ResearchCastleFlag());
    public static final ResearchRavagerCavalry RESEARCH_RAVAGER_CAVALRY = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "ravager_cavalry"), new ResearchRavagerCavalry());
    public static final ResearchBruteShields RESEARCH_BRUTE_SHIELDS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "brute_shields"), new ResearchBruteShields());
    public static final ResearchHoglinCavalry RESEARCH_HOGLIN_CAVALRY = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "hoglin_cavalry"), new ResearchHoglinCavalry());
    public static final ResearchHeavyTridents RESEARCH_HEAVY_TRIDENTS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "heavy_tridents"), new ResearchHeavyTridents());
    public static final ResearchCleavingFlails RESEARCH_CLEAVING_FLAILS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "cleaving_flails"), new ResearchCleavingFlails());
    public static final ResearchBlazeFirewall RESEARCH_BLAZE_FIREWALL = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "blaze_firewall"), new ResearchBlazeFirewall());
    public static final ResearchWitherClouds RESEARCH_WITHER_CLOUDS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wither_clouds"), new ResearchWitherClouds());
    public static final ResearchAdvancedPortals RESEARCH_ADVANCED_PORTALS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "advanced_portals"), new ResearchAdvancedPortals());
    public static final ResearchFireResistance RESEARCH_FIRE_RESISTANCE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "fire_resistance"), new ResearchFireResistance());
    public static final ResearchGrandLibrary RESEARCH_GRAND_LIBRARY = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "grand_library"), new ResearchGrandLibrary());
    public static final ResearchSuperiorBlacksmith RESEARCH_SUPERIOR_BLACKSMITH = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "superior_blacksmith"), new ResearchSuperiorBlacksmith());
    public static final ResearchSpiderWebs RESEARCH_SPIDER_WEBS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "spider_webs"), new ResearchSpiderWebs());
    public static final ResearchBloodlust RESEARCH_BLOODLUST = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "bloodlust"), new ResearchBloodlust());
    public static final ResearchCubeMagma RESEARCH_CUBE_MAGMA = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "cube_magma"), new ResearchCubeMagma());
    public static final ResearchSoulFireballs RESEARCH_SOUL_FIREBALLS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "soul_fireballs"), new ResearchSoulFireballs());

    public static final ResearchPortalForCivilian RESEARCH_PORTAL_FOR_CIVILIAN = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "portal_for_civilian"), new ResearchPortalForCivilian());
    public static final ResearchPortalForMilitary RESEARCH_PORTAL_FOR_MILITARY = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "portal_for_military"), new ResearchPortalForMilitary());
    public static final ResearchPortalForTransport RESEARCH_PORTAL_FOR_TRANSPORT = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "portal_for_transport"), new ResearchPortalForTransport());
    public static final ResearchBeaconLevel1 RESEARCH_BEACON_LEVEL_1 = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "beacon_level_1"), new ResearchBeaconLevel1());
    public static final ResearchBeaconLevel2 RESEARCH_BEACON_LEVEL_2 = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "beacon_level_2"), new ResearchBeaconLevel2());
    public static final ResearchBeaconLevel3 RESEARCH_BEACON_LEVEL_3 = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "beacon_level_3"), new ResearchBeaconLevel3());
    public static final ResearchBeaconLevel4 RESEARCH_BEACON_LEVEL_4 = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "beacon_level_4"), new ResearchBeaconLevel4());
    public static final ResearchBeaconLevel5 RESEARCH_BEACON_LEVEL_5 = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "beacon_level_5"), new ResearchBeaconLevel5());



    private static <T extends ProductionItem> T register(ResourceLocation id, T building) {
        return Registry.register(ReignOfNetherRegistries.PRODUCTION_ITEM, id, building);
    }

    public static void init() {}
}
