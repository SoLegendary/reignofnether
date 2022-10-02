package com.solegendary.reignofnether.building.productionitems;

import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class CreeperUnitProd extends ProductionItem {

    public final static String itemName = "Creeper";

    int ticksToProduce = 100; // build time in ticks
    int ticksLeft = 100;
    ProductionBuilding building;
    Consumer<Level> onComplete;

    public CreeperUnitProd(ProductionBuilding building) {
        super(building, (Level level) -> {
            System.out.println("produced creeper unit!");
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.CREEPER_UNIT.get());
        });
    }
}
