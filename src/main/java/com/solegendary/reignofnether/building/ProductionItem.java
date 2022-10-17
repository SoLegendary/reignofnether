package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.productionitems.CreeperUnitProd;
import com.solegendary.reignofnether.building.productionitems.SkeletonUnitProd;
import com.solegendary.reignofnether.building.productionitems.ZombieUnitProd;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

// units and/or research tech that a ProductionBuilding can produce
public abstract class ProductionItem {

    public static String itemName;

    public int foodCost;
    public int woodCost;
    public int oreCost;
    public int popCost; // 0 for non-unit prodItems

    public int ticksToProduce; // build time in ticks
    public int ticksLeft;
    public boolean canDuplicate; // is building allowed to build more than one of these? eg. tech upgrades can't be duplicated
    protected ProductionBuilding building;
    protected Consumer<Level> onComplete;

    public ProductionItem(ProductionBuilding building, int ticksToProduce) {
        this.building = building;
        this.ticksToProduce = ticksToProduce;
        this.ticksLeft = ticksToProduce;
    }

    public String getItemName() {
        return ProductionItem.itemName;
    }

    public boolean canAfford(String ownerName) {
        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName))
                return (resources.food >= foodCost &&
                        resources.wood >= woodCost &&
                        resources.ore >= oreCost);
        return false;
    } // TODO: population check
    public boolean canAffordFood(String ownerName) {
        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName))
                return resources.food >= foodCost;
        return false;
    }
    public boolean canAffordWood(String ownerName) {
        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName))
                return resources.wood >= woodCost;
        return false;
    }
    public boolean canAffordOre(String ownerName) {
        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName))
                return resources.ore >= oreCost;
        return false;
    }

    // Button object to build - start buttons are static as they aren't tied to an existing prodItem
    public static Button getStartButton(ProductionBuilding prodBuilding) {
        return null;
    }
    // Button object to show in-progress items
    // firstItem means this button will cancel the currently-building item
    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
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
