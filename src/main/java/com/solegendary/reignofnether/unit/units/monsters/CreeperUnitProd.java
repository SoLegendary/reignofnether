package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class CreeperUnitProd extends ProductionItem {

    public final static String itemName = "Creeper";

    public CreeperUnitProd(ProductionBuilding building) {
        super(building, ResourceCosts.Creeper.TICKS);
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.CREEPER_UNIT.get(), building.ownerName, true);
        };
        this.foodCost = ResourceCosts.Creeper.FOOD;
        this.woodCost = ResourceCosts.Creeper.WOOD;
        this.oreCost = ResourceCosts.Creeper.ORE;
        this.popCost = ResourceCosts.Creeper.POPULATION;
    }

    public String getItemName() {
        return CreeperUnitProd.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding, Keybinding hotkey) {
        return new Button(
            CreeperUnitProd.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png"),
            hotkey,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, itemName),
            null,
            List.of(
                FormattedCharSequence.forward(CreeperUnitProd.itemName, Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("\uE000  " + ResourceCosts.Creeper.FOOD + "     \uE002  " + ResourceCosts.Creeper.ORE, MyRenderer.iconStyle),
                FormattedCharSequence.forward("\uE003  " + ResourceCosts.Creeper.POPULATION + "     \uE004 " + ResourceCosts.Creeper.TICKS/20 + "s", MyRenderer.iconStyle),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("An explosive monster that can blow up units", Style.EMPTY),
                FormattedCharSequence.forward("and buildings alike. Has no regular attack.", Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            CreeperUnitProd.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png"),
            (Keybinding) null,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.cancelProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName, first),
            null,
            null
        );
    }
}