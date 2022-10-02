package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.hud.Button;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

// units and/or research tech that a ProductionBuilding can produce
public abstract class ProductionItem {

    public static String itemName;

    // TODO: cost for each resource
    int ticksToProduce; // build time in ticks
    int ticksLeft;
    boolean canDuplicate; // is building allowed to build more than one of these? eg. tech upgrades can't be duplicated
    protected ProductionBuilding building;
    protected Consumer<Level> onComplete;


    public ProductionItem(ProductionBuilding building) {
        this.building = building;
    }

    // Button object for use in the hud (to build and to show in-progress items)
    public Button getStartButton(ProductionBuilding prodBuilding) {
        return null;
    }
    // Button object for use in the hud (to build and to show in-progress items)
    public Button getCancelButton(ProductionBuilding prodBuilding) {
        return null;
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
