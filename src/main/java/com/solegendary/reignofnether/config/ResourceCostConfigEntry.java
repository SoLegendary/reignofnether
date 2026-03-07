package com.solegendary.reignofnether.config;

import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class ResourceCostConfigEntry {
    public static final List<ResourceCostConfigEntry> ENTRIES = new ArrayList<>();

    private ForgeConfigSpec.ConfigValue<Integer> FOOD;
    private ForgeConfigSpec.ConfigValue<Integer> WOOD;
    private ForgeConfigSpec.ConfigValue<Integer> ORE;
    private ForgeConfigSpec.ConfigValue<Integer> SECONDS;
    private ForgeConfigSpec.ConfigValue<Integer> POPULATION;

    private final int default_food;
    private final int default_wood;
    private final int default_ore;
    private final int default_seconds;
    private final int default_population;
    public final String id;
    //TODO: Use translateable component, add to lang file
    private final String comment;

    private ResourceCostConfigEntry(int food, int wood, int ore, int seconds, int population, ResourceCost associatedCost, String comment) {
        this.default_food = food;
        this.default_wood = wood;
        this.default_ore = ore;
        this.default_seconds = seconds;
        this.default_population = population;
        this.comment = comment;
        this.id = associatedCost.id;
        ENTRIES.add(this);

    }
    public static ResourceCostConfigEntry Unit(int food, int wood, int ore, int seconds, int population, ResourceCost associatedCost, String comment) { // units
        return new ResourceCostConfigEntry(food, wood, ore, seconds, population, associatedCost, comment);
    }
    public static ResourceCostConfigEntry Research(int food, int wood, int ore, int seconds, ResourceCost associatedCost, String comment) { // research
        return new ResourceCostConfigEntry(food, wood, ore, seconds, 0, associatedCost, comment);
    }
    public static ResourceCostConfigEntry Building(int food, int wood, int ore, int supply, ResourceCost associatedCost, String comment) { // buildings
        return new ResourceCostConfigEntry(food, wood, ore, 0, supply, associatedCost, comment);
    }
    public static ResourceCostConfigEntry Ability(int food, int wood, int ore, ResourceCost associatedCost, String comment) { // enchantments and equipment
        return new ResourceCostConfigEntry(food, wood, ore, 0, 0, associatedCost, comment);
    }

    //Defines each config value for the given ResourceCostConfigEntry
    public void define(ForgeConfigSpec.Builder builder) {
        builder.push(this.comment);
        this.FOOD = builder.define("Food cost", this.default_food);
        this.WOOD = builder.define("Wood cost", this.default_wood);
        this.ORE = builder.define("Ore cost", this.default_ore);
        this.SECONDS = builder.define("Time to create", this.default_seconds);
        this.POPULATION = builder.define("Population value", this.default_population);
        builder.pop();
    }

    public Integer getFood() {return this.FOOD.get();}
    public Integer getWood() {return this.WOOD.get();}
    public Integer getOre() {return this.ORE.get();}
    public Integer getSeconds() {return this.SECONDS.get();}
    public Integer getPopulation() {return this.POPULATION.get();}
}
