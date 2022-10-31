package com.solegendary.reignofnether.building.productionitems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.PopulationCosts;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
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
                FormattedCharSequence.forward("\uE000  50     \uE002  100", MyRenderer.iconStyle),
                FormattedCharSequence.forward("\uE003  2     \uE004  5s", MyRenderer.iconStyle),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("An explosive monster that can blow up units", Style.EMPTY),
                FormattedCharSequence.forward("and buildings alike. Has no regular attack.", Style.EMPTY)
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
