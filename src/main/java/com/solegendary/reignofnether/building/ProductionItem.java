package com.solegendary.reignofnether.building;

import net.minecraft.server.level.ServerLevel;

import java.util.function.Consumer;

// units and/or research tech that a ProductionBuilding can produce
public abstract class ProductionItem {

    // TODO: cost for each resource
    int ticksToProduce; // build time in ticks
    int ticksLeft;
    Consumer<ServerLevel> onComplete;

    public ProductionItem() {
    }

    // return true if the tick finished
    public boolean tick(ServerLevel level) {
        this.ticksLeft -= 1;
        if (this.ticksLeft <= 0) {
            onComplete.accept(level);
            return true;
        }
        return false;
    }
}
