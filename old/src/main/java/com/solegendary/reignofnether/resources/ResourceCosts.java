package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.util.FormattedCharSequence;

// defined here because we need to be able to access in both
// static (for ProductionItems) and nonstatic (for getCurrentPopulation) contexts
// and we can't declare static getters in the Unit interface
public class ResourceCosts {
    private static final String ID = ReignOfNether.MOD_ID;

    public static final ResourceCost ZOMBIE_VILLAGER = new ResourceCost(ID, "ZOMBIE_VILLAGER");
    public static final ResourceCost CREEPER = new ResourceCost(ID, "CREEPER");
    public static final ResourceCost ZOMBIE = new ResourceCost(ID, "ZOMBIE");
    public static final ResourceCost SKELETON = new ResourceCost(ID, "SKELETON");
    public static final ResourceCost STRAY = new ResourceCost(ID, "STRAY");
    public static final ResourceCost HUSK = new ResourceCost(ID, "HUSK");
    public static final ResourceCost DROWNED = new ResourceCost(ID, "DROWNED");
    public static final ResourceCost SPIDER = new ResourceCost(ID, "SPIDER");
    public static final ResourceCost POISON_SPIDER = new ResourceCost(ID, "POISON_SPIDER");
    public static final ResourceCost SLIME = new ResourceCost(ID, "SLIME");
    public static final ResourceCost WARDEN = new ResourceCost(ID, "WARDEN");
    public static final ResourceCost ZOMBIE_PIGLIN = new ResourceCost(ID, "ZOMBIE_PIGLIN");
    public static final ResourceCost ZOGLIN = new ResourceCost(ID, "ZOGLIN");
    public static final ResourceCost VILLAGER = new ResourceCost(ID, "VILLAGER");
    public static final ResourceCost MILITIA = new ResourceCost(ID, "MILITIA");
    public static final ResourceCost IRON_GOLEM = new ResourceCost(ID, "IRON_GOLEM");
    public static final ResourceCost PILLAGER = new ResourceCost(ID, "PILLAGER");
    public static final ResourceCost VINDICATOR = new ResourceCost(ID, "VINDICATOR");
    public static final ResourceCost WITCH = new ResourceCost(ID, "WITCH");
    public static final ResourceCost EVOKER = new ResourceCost(ID, "EVOKER");
    public static final ResourceCost RAVAGER = new ResourceCost(ID, "RAVAGER");
    public static final ResourceCost GRUNT = new ResourceCost(ID, "GRUNT");
    public static final ResourceCost BRUTE = new ResourceCost(ID, "BRUTE");
    public static final ResourceCost HEADHUNTER = new ResourceCost(ID, "HEADHUNTER");
    public static final ResourceCost HOGLIN = new ResourceCost(ID, "HOGLIN");
    public static final ResourceCost BLAZE = new ResourceCost(ID, "BLAZE");
    public static final ResourceCost WITHER_SKELETON = new ResourceCost(ID, "WITHER_SKELETON");
    public static final ResourceCost GHAST = new ResourceCost(ID, "GHAST");
    public static final ResourceCost MAGMA_CUBE = new ResourceCost(ID, "MAGMA_CUBE");

    public static final ResourceCost NECROMANCER = new ResourceCost(ID, "NECROMANCER");
    public static final ResourceCost ROYAL_GUARD = new ResourceCost(ID, "ROYAL_GUARD");
    public static final ResourceCost PIGLIN_MERCHANT = new ResourceCost(ID, "PIGLIN_MERCHANT");
    public static final ResourceCost HERO_BASE_REVIVE_COST = new ResourceCost(ID, "HERO_BASE_REVIVE_COST");
    public static final ResourceCost HERO_EXTRA_REVIVE_COST_PER_LEVEL = new ResourceCost(ID, "HERO_REVIVE_COST_PER_LEVEL");

    public static final ResourceCost ENDERMAN = new ResourceCost(ID, "ENDERMAN");
    public static final ResourceCost POLAR_BEAR = new ResourceCost(ID, "POLAR_BEAR");
    public static final ResourceCost GRIZZLY_BEAR = new ResourceCost(ID, "GRIZZLY_BEAR");
    public static final ResourceCost PANDA = new ResourceCost(ID, "PANDA");
    public static final ResourceCost WOLF = new ResourceCost(ID, "WOLF");
    public static final ResourceCost LLAMA = new ResourceCost(ID, "LLAMA");

    //BUILDINGS
    public static final ResourceCost BEACON = new ResourceCost(ID, "BEACON");

    public static final ResourceCost STOCKPILE = new ResourceCost(ID, "STOCKPILE");
    public static final ResourceCost OAK_BRIDGE = new ResourceCost(ID, "OAK_BRIDGE");
    public static final ResourceCost SPRUCE_BRIDGE = new ResourceCost(ID, "SPRUCE_BRIDGE");
    public static final ResourceCost BLACKSTONE_BRIDGE = new ResourceCost(ID, "BLACKSTONE_BRIDGE");
    //Monster
    public static final ResourceCost MAUSOLEUM = new ResourceCost(ID, "MAUSOLEUM");
    public static final ResourceCost HAUNTED_HOUSE = new ResourceCost(ID, "HAUNTED_HOUSE");
    public static final ResourceCost PUMPKIN_FARM = new ResourceCost(ID, "PUMPKIN_FARM");
    public static final ResourceCost SCULK_CATALYST = new ResourceCost(ID, "SCULK_CATALYST");
    public static final ResourceCost GRAVEYARD = new ResourceCost(ID, "GRAVEYARD");
    public static final ResourceCost SPIDER_LAIR = new ResourceCost(ID, "SPIDER_LAIR");
    public static final ResourceCost DUNGEON = new ResourceCost(ID, "DUNGEON");
    public static final ResourceCost LABORATORY = new ResourceCost(ID, "LABORATORY");
    public static final ResourceCost DARK_WATCHTOWER = new ResourceCost(ID, "DARK_WATCHTOWER");
    public static final ResourceCost SLIME_PIT = new ResourceCost(ID, "SLIME_PIT");
    public static final ResourceCost STRONGHOLD = new ResourceCost(ID, "STRONGHOLD");
    public static final ResourceCost ALTAR_OF_DARKNESS = new ResourceCost(ID, "ALTAR_OF_DARKNESS");
    //Villagers
    public static final ResourceCost TOWN_CENTRE = new ResourceCost(ID, "TOWN_CENTRE");
    public static final ResourceCost VILLAGER_HOUSE = new ResourceCost(ID, "VILLAGER_HOUSE");
    public static final ResourceCost WHEAT_FARM = new ResourceCost(ID, "WHEAT_FARM");
    public static final ResourceCost BARRACKS = new ResourceCost(ID, "BARRACKS");
    public static final ResourceCost BLACKSMITH = new ResourceCost(ID, "BLACKSMITH");
    public static final ResourceCost ARCANE_TOWER = new ResourceCost(ID, "ARCANE_TOWER");
    public static final ResourceCost LIBRARY = new ResourceCost(ID, "LIBRARY");
    public static final ResourceCost WATCHTOWER = new ResourceCost(ID, "WATCHTOWER");
    public static final ResourceCost CASTLE = new ResourceCost(ID, "CASTLE");
    public static final ResourceCost IRON_GOLEM_BUILDING = new ResourceCost(ID, "IRON_GOLEM_BUILDING");
    public static final ResourceCost SHRINE_OF_PROSPERITY = new ResourceCost(ID, "SHRINE_OF_PROSPERITY");
    //Piglins
    public static final ResourceCost CENTRAL_PORTAL = new ResourceCost(ID, "CENTRAL_PORTAL");
    public static final ResourceCost BASIC_PORTAL = new ResourceCost(ID, "BASIC_PORTAL");
    public static final ResourceCost CIVILIAN_PORTAL = new ResourceCost(ID, "CIVILIAN_PORTAL");
    public static final ResourceCost NETHERWART_FARM = new ResourceCost(ID, "NETHERWART_FARM");
    public static final ResourceCost BASTION = new ResourceCost(ID, "BASTION");
    public static final ResourceCost HOGLIN_STABLES = new ResourceCost(ID, "HOGLIN_STABLES");
    public static final ResourceCost FLAME_SANCTUARY = new ResourceCost(ID, "FLAME_SANCTUARY");
    public static final ResourceCost BASALT_SPRINGS = new ResourceCost(ID, "BASALT_SPRINGS");
    public static final ResourceCost WITHER_SHRINE = new ResourceCost(ID, "WITHER_SHRINE");
    public static final ResourceCost FORTRESS = new ResourceCost(ID, "FORTRESS");
    public static final ResourceCost INFERNAL_PORTAL = new ResourceCost(ID, "INFERNAL_PORTAL");

    // RESEARCH

    public static final ResourceCost RESEARCH_GOLEM_SMITHING = new ResourceCost(ID, "RESEARCH_GOLEM_SMITHING");
    public static final ResourceCost RESEARCH_MILITIA_BOWS = new ResourceCost(ID, "RESEARCH_MILITIA_BOWS");
    public static final ResourceCost RESEARCH_LAB_LIGHTNING_ROD = new ResourceCost(ID, "RESEARCH_LAB_LIGHTNING_ROD");
    public static final ResourceCost RESEARCH_RESOURCE_CAPACITY = new ResourceCost(ID, "RESEARCH_RESOURCE_CAPACITY");
    public static final ResourceCost RESEARCH_SPIDER_JOCKEYS = new ResourceCost(ID, "RESEARCH_SPIDER_JOCKEYS");
    public static final ResourceCost RESEARCH_SPIDER_WEBS = new ResourceCost(ID, "RESEARCH_SPIDER_WEBS");
    public static final ResourceCost RESEARCH_POISON_SPIDERS = new ResourceCost(ID, "RESEARCH_POISON_SPIDERS");
    public static final ResourceCost RESEARCH_HUSKS = new ResourceCost(ID, "RESEARCH_HUSKS");
    public static final ResourceCost RESEARCH_DROWNED = new ResourceCost(ID, "RESEARCH_DROWNED");
    public static final ResourceCost RESEARCH_STRAYS = new ResourceCost(ID, "RESEARCH_STRAYS");
    public static final ResourceCost RESEARCH_SLIME_CONVERSION = new ResourceCost(ID, "RESEARCH_SLIME_CONVERSION");
    public static final ResourceCost RESEARCH_LINGERING_POTIONS = new ResourceCost(ID, "RESEARCH_LINGERING_POTIONS");
    public static final ResourceCost RESEARCH_HEALING_POTIONS = new ResourceCost(ID, "RESEARCH_HEALING_POTIONS");
    public static final ResourceCost RESEARCH_WATER_POTIONS = new ResourceCost(ID, "RESEARCH_WATER_POTIONS");
    public static final ResourceCost RESEARCH_EVOKER_VEXES = new ResourceCost(ID, "RESEARCH_EVOKER_VEXES");
    public static final ResourceCost RESEARCH_CASTLE_FLAG = new ResourceCost(ID, "RESEARCH_CASTLE_FLAG");
    public static final ResourceCost RESEARCH_GRAND_LIBRARY = new ResourceCost(ID, "RESEARCH_GRAND_LIBRARY");
    public static final ResourceCost RESEARCH_SILVERFISH = new ResourceCost(ID, "RESEARCH_SILVERFISH");
    public static final ResourceCost RESEARCH_SCULK_AMPLIFIERS = new ResourceCost(ID, "RESEARCH_SCULK_AMPLIFIERS");
    public static final ResourceCost RESEARCH_RAVAGER_ARTILLERY = new ResourceCost(ID, "RESEARCH_RAVAGER_ARTILLERY");
    public static final ResourceCost RESEARCH_BRUTE_SHIELDS = new ResourceCost(ID, "RESEARCH_BRUTE_SHIELDS");
    public static final ResourceCost RESEARCH_HOGLIN_CAVALRY = new ResourceCost(ID, "RESEARCH_HOGLIN_CAVALRY");
    public static final ResourceCost RESEARCH_HEAVY_TRIDENTS = new ResourceCost(ID, "RESEARCH_HEAVY_TRIDENTS");
    public static final ResourceCost RESEARCH_BLAZE_FIRE_WALL = new ResourceCost(ID, "RESEARCH_BLAZE_FIRE_WALL");
    public static final ResourceCost RESEARCH_FIRE_RESISTANCE = new ResourceCost(ID, "RESEARCH_FIRE_RESISTANCE");
    public static final ResourceCost RESEARCH_WITHER_CLOUDS = new ResourceCost(ID, "RESEARCH_WITHER_CLOUDS");
    public static final ResourceCost RESEARCH_BLOODLUST = new ResourceCost(ID, "RESEARCH_BLOODLUST");
    public static final ResourceCost RESEARCH_ADVANCED_PORTALS = new ResourceCost(ID, "RESEARCH_ADVANCED_PORTALS");
    public static final ResourceCost RESEARCH_CIVILIAN_PORTAL = new ResourceCost(ID, "RESEARCH_CIVILIAN_PORTAL");
    public static final ResourceCost RESEARCH_MILITARY_PORTAL = new ResourceCost(ID, "RESEARCH_MILITARY_PORTAL");
    public static final ResourceCost RESEARCH_TRANSPORT_PORTAL = new ResourceCost(ID, "RESEARCH_TRANSPORT_PORTAL");
    public static final ResourceCost RESEARCH_CUBE_MAGMA = new ResourceCost(ID, "RESEARCH_CUBE_MAGMA");
    public static final ResourceCost RESEARCH_SOUL_FIREBALLS = new ResourceCost(ID, "RESEARCH_SOUL_FIREBALLS");
    public static final ResourceCost RESEARCH_BEACON_LEVEL1 = new ResourceCost(ID, "RESEARCH_BEACON_LEVEL1");
    public static final ResourceCost RESEARCH_BEACON_LEVEL2 = new ResourceCost(ID, "RESEARCH_BEACON_LEVEL2");
    public static final ResourceCost RESEARCH_BEACON_LEVEL3 = new ResourceCost(ID, "RESEARCH_BEACON_LEVEL3");
    public static final ResourceCost RESEARCH_BEACON_LEVEL4 = new ResourceCost(ID, "RESEARCH_BEACON_LEVEL4");
    public static final ResourceCost RESEARCH_BEACON_LEVEL5 = new ResourceCost(ID, "RESEARCH_BEACON_LEVEL5");

    // ENCHANTMENTS

    public static final ResourceCost ENCHANT_MAIMING = new ResourceCost(ID, "ENCHANT_MAIMING");
    public static final ResourceCost ENCHANT_QUICK_CHARGE = new ResourceCost(ID, "ENCHANT_QUICK_CHARGE");
    public static final ResourceCost ENCHANT_SHARPNESS = new ResourceCost(ID, "ENCHANT_SHARPNESS");
    public static final ResourceCost ENCHANT_MULTISHOT = new ResourceCost(ID, "ENCHANT_MULTISHOT");
    public static final ResourceCost ENCHANT_VIGOR = new ResourceCost(ID, "ENCHANT_VIGOR");

    // UNUSED

    public static final ResourceCost RESEARCH_VINDICATOR_AXES = ResourceCost.Research(0,200,400, 150);
    public static final ResourceCost RESEARCH_PILLAGER_CROSSBOWS = ResourceCost.Research(0,600,300, 180);

    public static FormattedCharSequence getFormattedCost(ResourceCost resCost) {
        String str = "";
        if (resCost.food > 0)
            str += "\uE000  " + resCost.food + "     ";
        if (resCost.wood > 0)
            str += "\uE001  " + resCost.wood + "     ";
        if (resCost.ore > 0)
            str += "\uE002  " + resCost.ore + "     ";

        if (str.isEmpty())
            str += "\uE000  0     ";
        str = str.trim();
        return FormattedCharSequence.forward(str, MyRenderer.iconStyle);
    }
    public static FormattedCharSequence getFormattedPopAndTime(ResourceCost resCost) {
        return FormattedCharSequence.forward("\uE003  " + resCost.population + "     \uE004  " + resCost.ticks/ResourceCost.TICKS_PER_SECOND + "s", MyRenderer.iconStyle);
    }
    public static FormattedCharSequence getFormattedPop(ResourceCost resCost) {
        return FormattedCharSequence.forward("\uE003  " + resCost.population, MyRenderer.iconStyle);
    }
    public static FormattedCharSequence getFormattedTime(ResourceCost resCost) {
        return FormattedCharSequence.forward("\uE004  " + resCost.ticks/ResourceCost.TICKS_PER_SECOND + "s", MyRenderer.iconStyle);
    }

    public static final int REPLANT_WOOD_COST = 1;
    public static final int REDUCED_REPLANT_WOOD_COST = 0;
    public static final int DEFAULT_MAX_POPULATION = 150;
    /*
        Unit costs are defined here during CommonSetup
        Do not read values and initialize from config earlier, else will result in IllegalStateException !!!
     */
    public static void deferredLoadResourceCosts() {
        // All resource cost loading is now handled by JsonConfigManager in ConfigClientEvents
        // This method remains empty for backwards compatibility
    }
}
