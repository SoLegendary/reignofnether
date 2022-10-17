package com.solegendary.reignofnether.building.productionitems;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.PopulationCosts;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class CreeperUnitProd extends ProductionItem {

    public final static String itemName = "Creeper";

    public CreeperUnitProd(ProductionBuilding building) {
        super(building, 100);
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.CREEPER_UNIT.get(), building.ownerName);
        };
        this.foodCost = 50;
        this.woodCost = 0;
        this.oreCost = 100;
        this.popCost = PopulationCosts.CREEPER;
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
            () -> BuildingServerboundPacket.startProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName)
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
            () -> BuildingServerboundPacket.cancelProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName, first)
        );
    }
}
