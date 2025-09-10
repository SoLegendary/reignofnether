package com.solegendary.reignofnether.resources;


import java.util.HashMap;

// defined here because we need to be able to access in both
// static (for ProductionItems) and nonstatic (for getCurrentPopulation) contexts
// and we can't declare static getters in the Unit interface

public class ResourceCost {
    public static final HashMap<String, ResourceCost> ENTRIES = new HashMap<>();
    public static final int TICKS_PER_SECOND = 20;

    public int food;
    public int wood;
    public int ore;
    public int ticks;
    public int population; // for a building, indicates supply, for a unit, indicates usage
    public String id;

    private ResourceCost(int food, int wood, int ore, int seconds, int population) { // units
        this.food = food;
        this.wood = wood;
        this.ore = ore;
        this.ticks = seconds * TICKS_PER_SECOND;
        this.population = population;
    }

    public ResourceCost(String modid, String id) {
        //Constructor for static configurable ResourceCosts
        this.id = modid + "." + id;
        ENTRIES.put(this.id, this);
    }

    public static ResourceCost Unit(int food, int wood, int ore, int seconds, int population) { // buildings
        return new ResourceCost(food, wood, ore, seconds, population);
    }
    public static ResourceCost Research(int food, int wood, int ore, int seconds) { // buildings
        return new ResourceCost(food, wood, ore, seconds, 0);
    }
    public static ResourceCost Building(int food, int wood, int ore, int supply) { // buildings
        return new ResourceCost(food, wood, ore, 0, supply);
    }
    public static ResourceCost Enchantment(int food, int wood, int ore) { // buildings
        return new ResourceCost(food, wood, ore, 0, 0);
    }
    public void bakeValuesFromJson(com.solegendary.reignofnether.config.JsonResourceCostEntry jsonEntry) {
        this.food = jsonEntry.food;
        this.wood = jsonEntry.wood;
        this.ore = jsonEntry.ore;
        this.ticks = jsonEntry.seconds * TICKS_PER_SECOND;
        this.population = jsonEntry.population;
    }
}
