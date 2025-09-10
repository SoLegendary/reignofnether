package com.solegendary.reignofnether.config;

public class EnchantmentEntry extends JsonResourceCostEntry {
    
    public EnchantmentEntry() {
        super();
    }

    public EnchantmentEntry(int food, int wood, int ore, int seconds, int population) {
        super(food, wood, ore, seconds, population);
    }
}