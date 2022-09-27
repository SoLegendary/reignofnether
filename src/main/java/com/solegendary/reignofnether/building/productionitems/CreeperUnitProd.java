package com.solegendary.reignofnether.building.productionitems;

import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.units.CreeperUnit;
import net.minecraft.server.level.ServerLevel;

import java.util.function.Consumer;

public class CreeperUnitProd extends ProductionItem {

    int ticksToProduce = 100; // build time in ticks
    int ticksLeft = 100;
    ProductionBuilding building;

    Consumer<ServerLevel> onComplete = (ServerLevel level) -> {
        System.out.println("produced creeper unit!");
        this.building.produceUnit(level, EntityRegistrar.CREEPER_UNIT.get());
    };

    public CreeperUnitProd(ProductionBuilding building) {
        super();
        this.building = building;
    }
}
