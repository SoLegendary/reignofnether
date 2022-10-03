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

public class CreeperUnitProd extends ProductionItem {

    public final static String itemName = "Creeper";

    public CreeperUnitProd(ProductionBuilding building) {
        super(building, 100);
        this.onComplete = (Level level) -> {
            System.out.println("produced creeper unit!");
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.CREEPER_UNIT.get());
        };
    }

    public String getItemName() {
        return CreeperUnitProd.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding) {
        return new Button(
            "Creeper",
            14,
            "textures/mobheads/creeper.png",
            Keybinding.keyQ,
            () -> false,
            () -> false,
            () -> true,
            () -> {
                System.out.println("added creeper to queue");
                prodBuilding.productionQueue.add(new CreeperUnitProd(prodBuilding));
                BuildingServerboundPacket.startProduction(Building.getMinCorner(prodBuilding.getBlocks()), CreeperUnitProd.itemName);
            }
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            "Creeper",
            14,
            "textures/mobheads/creeper.png",
            (KeyMapping) null,
            () -> false,
            () -> false,
            () -> true,
            () -> removeSelfFromQueue(first, itemName)
        );
    }
}
