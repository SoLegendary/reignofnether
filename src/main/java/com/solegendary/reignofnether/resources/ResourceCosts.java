package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.util.FormattedCharSequence;

// defined here because we need to be able to access in both
// static (for ProductionItems) and nonstatic (for getCurrentPopulation) contexts
// and we can't declare static getters in the Unit interface
public class ResourceCosts {

    public static FormattedCharSequence getFormattedCost(ResourceCost resCost) {
        String str = "";
        if (resCost.food > 0)
            str += "\uE000  " + resCost.food + "     ";
        if (resCost.wood > 0)
            str += "\uE001  " + resCost.wood + "     ";
        if (resCost.ore > 0)
            str += "\uE002  " + resCost.ore + "     ";
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
    public static final int REDUCED_REPLANT_WOOD_COST = 1;
    public static final int MAX_POPULATION = 100; // max possible pop you can have regardless of buildings

    // ******************* UNITS ******************* //
    // Monsters
    public static ResourceCost ZOMBIE_VILLAGER = ResourceCost.Unit(50,0,0,15,1);
    public static ResourceCost CREEPER = ResourceCost.Unit(50,0,100,35,2);
    public static ResourceCost SKELETON = ResourceCost.Unit(70,50,0,20,1);
    public static ResourceCost STRAY = ResourceCost.Unit(70,50,0,20,1);
    public static ResourceCost ZOMBIE = ResourceCost.Unit(90,0,0,20,1);
    public static ResourceCost HUSK = ResourceCost.Unit(90,0,0,20,1);
    public static ResourceCost SPIDER = ResourceCost.Unit(80,60,0,25,2);
    public static ResourceCost POISON_SPIDER = ResourceCost.Unit(80,0,60,25,2);
    public static ResourceCost ENDERMAN = ResourceCost.Unit(100,100,100,30,3);
    public static ResourceCost WARDEN = ResourceCost.Unit(200,0,100,35,4);
    public static ResourceCost SILVERFISH = ResourceCost.Unit(10,0,0,10,0);

    // Villagers
    public static ResourceCost VILLAGER = ResourceCost.Unit(50,0,0,15,1);
    public static ResourceCost IRON_GOLEM = ResourceCost.Unit(0,0,250,40,4);
    public static ResourceCost PILLAGER = ResourceCost.Unit(100,80,0,30,2);
    public static ResourceCost VINDICATOR = ResourceCost.Unit(150,0,0,30,2);
    public static ResourceCost WITCH = ResourceCost.Unit(100,100,100,35,3);
    public static ResourceCost EVOKER = ResourceCost.Unit(150,0,150,35,3);
    public static ResourceCost RAVAGER = ResourceCost.Unit(300,50,150,45,6);

    // ******************* BUILDINGS ******************* //
    public static ResourceCost STOCKPILE = ResourceCost.Building(0,75,0, 0);

    // Monsters
    public static ResourceCost MAUSOLEUM = ResourceCost.Building(0,300,150, 10);
    public static ResourceCost HAUNTED_HOUSE = ResourceCost.Building(0,100,0, 10);
    public static ResourceCost PUMPKIN_FARM = ResourceCost.Building(0,150,0, 0);
    public static ResourceCost GRAVEYARD = ResourceCost.Building(0,150,0, 0);
    public static ResourceCost SPIDER_LAIR = ResourceCost.Building(0,150,75, 0);
    public static ResourceCost DUNGEON = ResourceCost.Building(0,150,75, 0);
    public static ResourceCost LABORATORY = ResourceCost.Building(0,250,150, 0);
    public static ResourceCost DARK_WATCHTOWER = ResourceCost.Building(0,100,100, 0);
    public static ResourceCost STRONGHOLD = ResourceCost.Building(0,400,400, 0);

    // Villagers
    public static ResourceCost TOWN_CENTRE = ResourceCost.Building(0,300,150, 10);
    public static ResourceCost VILLAGER_HOUSE = ResourceCost.Building(0,100,0, 10);
    public static ResourceCost WHEAT_FARM = ResourceCost.Building(0,100,0, 0);
    public static ResourceCost BARRACKS = ResourceCost.Building(0,150,0, 0);
    public static ResourceCost BLACKSMITH = ResourceCost.Building(0,250,150, 0);
    public static ResourceCost ARCANE_TOWER = ResourceCost.Building(0,200,100, 0);
    public static ResourceCost LIBRARY = ResourceCost.Building(0,350,100, 0);
    public static ResourceCost WATCHTOWER = ResourceCost.Building(0,100,100, 0);
    public static ResourceCost CASTLE = ResourceCost.Building(0,400,400, 0);

    // Netherlings
    public static ResourceCost PORTAL = ResourceCost.Building(0, 200, 0, 0);

    // ******************* RESEARCH ******************* //
    public static ResourceCost RESEARCH_VINDICATOR_AXES = ResourceCost.Research(0,200,400, 180);
    public static ResourceCost RESEARCH_PILLAGER_CROSSBOWS = ResourceCost.Research(0,600,300, 180);
    public static ResourceCost RESEARCH_LAB_LIGHTNING_ROD = ResourceCost.Research(0,100,500, 120);
    public static ResourceCost RESEARCH_RESOURCE_CAPACITY = ResourceCost.Research(200,200,0, 90);
    public static ResourceCost RESEARCH_SPIDER_JOCKEYS = ResourceCost.Research(150,150,0, 60);
    public static ResourceCost RESEARCH_POISON_SPIDERS = ResourceCost.Research(400,0,200, 90);
    public static ResourceCost RESEARCH_HUSKS = ResourceCost.Research(500,0,300, 180);
    public static ResourceCost RESEARCH_STRAYS = ResourceCost.Research(500,500,0, 180);
    public static ResourceCost RESEARCH_LINGERING_POTIONS = ResourceCost.Research(300,300,300, 150);
    public static ResourceCost RESEARCH_EVOKERS = ResourceCost.Research(500,0,300, 150);
    public static ResourceCost RESEARCH_EVOKER_VEXES = ResourceCost.Research(500,0,300, 120);
    public static ResourceCost RESEARCH_CASTLE_FLAG = ResourceCost.Research(200,0,100, 60);
    public static ResourceCost RESEARCH_SILVERFISH = ResourceCost.Research(0,300,300, 120);
    public static ResourceCost RESEARCH_RAVAGER_CAVALRY = ResourceCost.Research(250,250,0, 90);
}
