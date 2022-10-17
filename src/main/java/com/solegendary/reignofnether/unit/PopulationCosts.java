package com.solegendary.reignofnether.unit;

// defined here because we need to be able to access in both
// static (for ProductionItems) and nonstatic (for getCurrentPopulation) contexts
// and we can't declare static getters in the Unit interface
public class PopulationCosts {
    public static final int POPULATION_CAP = 100; // max possible pop you can have regardless of buildings
    public static final int CREEPER = 2;
    public static final int SKELETON = 1;
    public static final int ZOMBIE = 1;
}
