package com.solegendary.reignofnether.building.productionitems;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.PopulationCosts;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class ZombieUnitProd extends ProductionItem {

    public final static String itemName = "Zombie";

    public ZombieUnitProd(ProductionBuilding building) {
        super(building, 100);
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.ZOMBIE_UNIT.get(), building.ownerName);
        };
        this.foodCost = 100;
        this.woodCost = 0;
        this.oreCost = 0;
        this.popCost = PopulationCosts.ZOMBIE;
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
            () -> BuildingServerboundPacket.startProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName),
            List.of(
                FormattedCharSequence.forward("Zombie", Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("Food: 100", Style.EMPTY.withItalic(true)),
                FormattedCharSequence.forward("Time: 5s", Style.EMPTY.withItalic(true)),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("An undead monster with a basic melee attack.", Style.EMPTY)
            )
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
            () -> BuildingServerboundPacket.cancelProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName, first),
            null
        );
    }
}