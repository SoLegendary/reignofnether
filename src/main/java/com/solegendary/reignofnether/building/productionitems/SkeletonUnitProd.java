package com.solegendary.reignofnether.building.productionitems;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class SkeletonUnitProd extends ProductionItem {

    public final static String itemName = "Skeleton";

    int ticksToProduce = 100; // build time in ticks
    int ticksLeft = 100;

    public SkeletonUnitProd(ProductionBuilding building) {
        super(building);
        this.onComplete = (Level level) -> {
            System.out.println("produced skeleton unit!");
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.SKELETON_UNIT.get());
        };
    }

    public Button getStartButton(ProductionBuilding prodBuilding) {
        return new Button(
            "Skeleton",
            14,
            "textures/mobheads/skeleton.png",
            Keybinding.keyQ,
            () -> false,
            () -> false,
            () -> {
                System.out.println("added skeleton to queue");
                prodBuilding.productionQueue.add(new SkeletonUnitProd(prodBuilding));
                BuildingServerboundPacket.startProduction(Building.getMinCorner(prodBuilding.getBlocks()), SkeletonUnitProd.itemName);
            }
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding) {
        return new Button(
            "Skeleton",
            14,
            "textures/mobheads/skeleton.png",
            Keybinding.keyQ,
            () -> false,
            () -> false,
            () -> {
                System.out.println("removed skeleton from queue");
            }
        );
    }
}
