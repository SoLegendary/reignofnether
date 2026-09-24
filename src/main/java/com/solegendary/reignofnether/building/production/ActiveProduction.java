package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;

public class ActiveProduction {
    public boolean completed;
    public float ticksLeft;
    public ProductionItem item;
    public ActiveProduction(ProductionItem item, boolean isClientside, String ownerName) {
        this.item = item;
        this.ticksLeft = item.getCost(isClientside, ownerName).ticks;
    }

    public void complete(ProductionPlacement placement) {
        this.item.recordScore(placement);
        this.item.onComplete.accept(placement.getLevel(), placement);
        this.completed = true;
    }
}
