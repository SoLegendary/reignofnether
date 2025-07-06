package com.solegendary.reignofnether.building.production;

public class ActiveProduction {
    public boolean completed;
    public float ticksLeft;
    public ProductionItem item;
    public ActiveProduction(ProductionItem item, boolean isClientside, String ownerName) {
        this.item = item;
        this.ticksLeft = item.getCost(isClientside, ownerName).ticks;
    }
}
