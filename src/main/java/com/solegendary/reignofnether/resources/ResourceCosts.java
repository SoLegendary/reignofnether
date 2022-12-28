package com.solegendary.reignofnether.resources;

// defined here because we need to be able to access in both
// static (for ProductionItems) and nonstatic (for getCurrentPopulation) contexts
// and we can't declare static getters in the Unit interface
public class ResourceCosts {

    public static final int REPLANT_WOOD_COST = 3;
    public static final int MAX_POPULATION = 100; // max possible pop you can have regardless of buildings
    public static final int TICKS_PER_SECOND = 20;

    public static class ZombieVillager {
        public static final int FOOD = 50;
        public static final int WOOD = 0;
        public static final int ORE = 0;
        public static final int TICKS = TICKS_PER_SECOND * 15;
        public static final int POPULATION = 1;
    }
    public static class Creeper {
        public static final int FOOD = 50;
        public static final int WOOD = 0;
        public static final int ORE = 100;
        public static final int TICKS = TICKS_PER_SECOND * 35;
        public static final int POPULATION = 2;
    }
    public static class Skeleton {
        public static final int FOOD = 70;
        public static final int WOOD = 50;
        public static final int ORE = 0;
        public static final int TICKS = TICKS_PER_SECOND * 20;
        public static final int POPULATION = 1;
    }
    public static class Zombie {
        public static final int FOOD = 90;
        public static final int WOOD = 0;
        public static final int ORE = 0;
        public static final int TICKS = TICKS_PER_SECOND * 20;
        public static final int POPULATION = 1;
    }
    public static class Villager {
        public static final int FOOD = 50;
        public static final int WOOD = 0;
        public static final int ORE = 0;
        public static final int TICKS = TICKS_PER_SECOND * 15;
        public static final int POPULATION = 1;
    }
    public static class IronGolem {
        public static final int FOOD = 0;
        public static final int WOOD = 0;
        public static final int ORE = 250;
        public static final int TICKS = TICKS_PER_SECOND * 50;
        public static final int POPULATION = 6;
    }
    public static class Pillager {
        public static final int FOOD = 100;
        public static final int WOOD = 80;
        public static final int ORE = 0;
        public static final int TICKS = TICKS_PER_SECOND * 30;
        public static final int POPULATION = 2;
    }
    public static class Vindicator {
        public static final int FOOD = 150;
        public static final int WOOD = 0;
        public static final int ORE = 0;
        public static final int TICKS = TICKS_PER_SECOND * 30;
        public static final int POPULATION = 2;
    }
    public static class Stockpile {
        public static final int FOOD = 0;
        public static final int WOOD = 75;
        public static final int ORE = 0;
        public static final int SUPPLY = 0;
    }
    public static class TownCentre {
        public static final int FOOD = 0;
        public static final int WOOD = 300;
        public static final int ORE = 150;
        public static final int SUPPLY = 10;
    }
    public static class Mausoleum {
        public static final int FOOD = 0;
        public static final int WOOD = 300;
        public static final int ORE = 150;
        public static final int SUPPLY = 10;
    }
    public static class VillagerHouse {
        public static final int FOOD = 0;
        public static final int WOOD = 100;
        public static final int ORE = 100;
        public static final int SUPPLY = 10;
    }
    public static class HauntedHouse {
        public static final int FOOD = 0;
        public static final int WOOD = 100;
        public static final int ORE = 0;
        public static final int SUPPLY = 10;
    }
    public static class WheatFarm {
        public static final int FOOD = 0;
        public static final int WOOD = 100;
        public static final int ORE = 0;
        public static final int SUPPLY = 0;
    }
    public static class PumpkinFarm {
        public static final int FOOD = 0;
        public static final int WOOD = 100;
        public static final int ORE = 0;
        public static final int SUPPLY = 0;
    }
    public static class Barracks {
        public static final int FOOD = 0;
        public static final int WOOD = 150;
        public static final int ORE = 0;
        public static final int SUPPLY = 0;
    }
    public static class Graveyard {
        public static final int FOOD = 0;
        public static final int WOOD = 150;
        public static final int ORE = 0;
        public static final int SUPPLY = 0;
    }
    public static class Blacksmith {
        public static final int FOOD = 0;
        public static final int WOOD = 250;
        public static final int ORE = 100;
        public static final int SUPPLY = 0;
    }
    public static class Laboratory {
        public static final int FOOD = 0;
        public static final int WOOD = 250;
        public static final int ORE = 100;
        public static final int SUPPLY = 0;
    }


    public static class ResearchVindicatorAxes {
        public static final int FOOD = 0;
        public static final int WOOD = 200;
        public static final int ORE = 400;
        public static final int TICKS = TICKS_PER_SECOND * 12;
    }
    public static class ResearchPillagerCrossbows {
        public static final int FOOD = 0;
        public static final int WOOD = 500;
        public static final int ORE = 250;
        public static final int TICKS = TICKS_PER_SECOND * 12;
    }
    public static class ResearchLabLightningRod {
        public static final int FOOD = 0;
        public static final int WOOD = 100;
        public static final int ORE = 500;
        public static final int TICKS = TICKS_PER_SECOND * 12;
    }
    public static class ResearchResourceCapacity {
        public static final int FOOD = 200;
        public static final int WOOD = 200;
        public static final int ORE = 0;
        public static final int TICKS = TICKS_PER_SECOND * 9;
    }
}
