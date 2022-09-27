package com.solegendary.reignofnether.building.productionitems;

import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.units.CreeperUnit;
import com.solegendary.reignofnether.unit.units.SkeletonUnit;
import net.minecraft.server.level.ServerLevel;

import java.util.function.Consumer;

public class SkeletonUnitProd extends ProductionItem {

    int ticksToProduce = 100; // build time in ticks
    int ticksLeft = 100;
    ProductionBuilding building;

    Consumer<ServerLevel> onComplete = (ServerLevel level) -> {
        System.out.println("produced skeleton unit!");
        this.building.produceUnit(level, EntityRegistrar.SKELETON_UNIT.get());
    };

    public SkeletonUnitProd(ProductionBuilding building) {
        super();
        this.building = building;
    }
}
