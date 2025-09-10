package com.solegendary.reignofnether.config;

public class BuildingEntry extends JsonResourceCostEntry {
    
    public BuildingEntry() {
        super();
    }

    public BuildingEntry(int food, int wood, int ore, int seconds, int population) {
        super(food, wood, ore, seconds, population);
    }
}