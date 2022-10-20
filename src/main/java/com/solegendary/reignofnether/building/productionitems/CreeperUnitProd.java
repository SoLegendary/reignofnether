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
            () -> BuildingServerboundPacket.startProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName),
            List.of(
                FormattedCharSequence.forward("Creeper", Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("Food: 50", Style.EMPTY.withItalic(true)),
                FormattedCharSequence.forward("Ore: 100", Style.EMPTY.withItalic(true)),
                FormattedCharSequence.forward("Time: 5s", Style.EMPTY.withItalic(true)),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("An unstable, explosive monster that can blow up", Style.EMPTY),
                FormattedCharSequence.forward("buildings and units alike. Has no regular attack.", Style.EMPTY)
            )
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
            () -> BuildingServerboundPacket.cancelProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName, first),
            null
        );
    }
}
