package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.productionitems.CreeperUnitProd;
import com.solegendary.reignofnether.building.productionitems.SkeletonUnitProd;
import com.solegendary.reignofnether.building.productionitems.ZombieUnitProd;
import com.solegendary.reignofnether.hud.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

// units and/or research tech that a ProductionBuilding can produce
public abstract class ProductionItem {

    public static String itemName;

    // TODO: cost for each resource
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
