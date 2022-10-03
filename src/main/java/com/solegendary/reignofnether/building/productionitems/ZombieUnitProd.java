package com.solegendary.reignofnether.building.productionitems;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class ZombieUnitProd extends ProductionItem {

    public final static String itemName = "Zombie";

    public ZombieUnitProd(ProductionBuilding building) {
        super(building, 100);
        this.onComplete = (Level level) -> {
            System.out.println("produced zombie unit!");
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.ZOMBIE_UNIT.get());
        };
    }

    public String getItemName() {
        return ZombieUnitProd.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding) {
        return new Button(
                "Zombie",
                14,
                "textures/mobheads/zombie.png",
                Keybinding.keyE,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    System.out.println("added zombie to queue");
                    prodBuilding.productionQueue.add(new ZombieUnitProd(prodBuilding));
                    BuildingServerboundPacket.startProduction(Building.getMinCorner(prodBuilding.getBlocks()), ZombieUnitProd.itemName);
                }
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            "Zombie",
            14,
            "textures/mobheads/zombie.png",
            (KeyMapping) null,
            () -> false,
            () -> false,
            () -> true,
            () -> removeSelfFromQueue(first)
        );
    }
}