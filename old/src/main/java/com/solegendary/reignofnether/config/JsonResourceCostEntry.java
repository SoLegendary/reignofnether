package com.solegendary.reignofnether.config;

public class JsonResourceCostEntry {
    public int food;
    public int wood;
    public int ore;
    public int seconds;
    public int population;

    public JsonResourceCostEntry() {}

    public JsonResourceCostEntry(int food, int wood, int ore, int seconds, int population) {
        this.food = food;
        this.wood = wood;
        this.ore = ore;
        this.seconds = seconds;
        this.population = population;
    }

}