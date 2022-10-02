package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.Unit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

// units and/or research tech that a ProductionBuilding can produce
public abstract class ProductionItem {

    public static String itemName;

    // TODO: cost for each resource
    int ticksToProduce; // build time in ticks
    int ticksLeft;
    boolean canDuplicate; // are we allowed to build more than one of these? eg. tech upgrades can't be duplicated
    ProductionBuilding building;
    Consumer<Level> onComplete;

    public ProductionItem(ProductionBuilding building, Consumer<Level> onComplete) {
        this.building = building;
        this.onComplete = onComplete;
    }

    // return true if the tick finished
    public boolean tick(Level level) {
        this.ticksLeft -= 1;
        if (this.ticksLeft <= 0) {
            onComplete.accept(level);
            return true;
        }
        return false;
    }
}
