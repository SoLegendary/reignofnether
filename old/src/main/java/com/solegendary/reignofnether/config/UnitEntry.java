package com.solegendary.reignofnether.config;

public class UnitEntry extends JsonResourceCostEntry {
    
    public UnitEntry() {
        super();
    }

    public UnitEntry(int food, int wood, int ore, int seconds, int population) {
        super(food, wood, ore, seconds, population);
    }
}