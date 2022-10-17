package com.solegendary.reignofnether.building.productionitems;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.PopulationCosts;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class SkeletonUnitProd extends ProductionItem {

    public final static String itemName = "Skeleton";

    public SkeletonUnitProd(ProductionBuilding building) {
        super(building, 100);
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.SKELETON_UNIT.get(), building.ownerName);
        };
        this.foodCost = 60;
        this.woodCost = 40;
        this.oreCost = 0;
        this.popCost = PopulationCosts.SKELETON;
    }

    public String getItemName() {
        return SkeletonUnitProd.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding) {
        return new Button(
            "Skeleton",
            14,
            "textures/mobheads/skeleton.png",
            Keybinding.keyW,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.startProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName)
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            "Skeleton",
            14,
            "textures/mobheads/skeleton.png",
            (KeyMapping) null,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.cancelProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName, first)
        );
    }
}
