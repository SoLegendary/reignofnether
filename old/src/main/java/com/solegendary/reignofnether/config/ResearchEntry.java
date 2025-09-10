package com.solegendary.reignofnether.config;

public class ResearchEntry extends JsonResourceCostEntry {
    
    public ResearchEntry() {
        super();
    }

    public ResearchEntry(int food, int wood, int ore, int seconds, int population) {
        super(food, wood, ore, seconds, population);
    }
}