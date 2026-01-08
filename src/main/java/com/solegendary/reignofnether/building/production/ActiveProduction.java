package com.solegendary.reignofnether.building.production;

public class ActiveProduction {
    public boolean completed;
    public float ticksLeft;
    public ProductionItem item;
    /**
     * For Graveyard overflow upgrade: when true, this production is allowed to finish even if population is full,
     * and on completion it will be stored (stockpiled) instead of immediately spawning.
     */
    public boolean overflowStockpile;
    public ActiveProduction(ProductionItem item, boolean isClientside, String ownerName) {
        this.item = item;
        this.ticksLeft = item.getCost(isClientside, ownerName).ticks;
        this.overflowStockpile = false;
    }

    public ActiveProduction(ProductionItem item, boolean isClientside, String ownerName, boolean overflowStockpile) {
        this.item = item;
        this.ticksLeft = item.getCost(isClientside, ownerName).ticks;
        this.overflowStockpile = overflowStockpile;
    }
}
