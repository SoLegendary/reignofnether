package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Supplier;

public class StopProductionButton extends Button {
   public StopProductionButton(String name, ResourceLocation rl, ProductionPlacement placement, ProductionItem production, boolean first) {

        // generate x/y based on given position (starting at 0 which is bottom left 1 row above generic action buttons)
        super(name, Button.itemIconSize, rl, (Keybinding) null, () -> false, () -> false, () -> true, () -> BuildingServerboundPacket.cancelProduction(placement.minCorner, production, first), () -> {}, null);
    }

    public StopProductionButton(String name, ResourceLocation iconRl, ResourceLocation frameRl, ProductionPlacement placement, ProductionItem production, boolean first) {

        // generate x/y based on given position (starting at 0 which is bottom left 1 row above generic action buttons)
        super(name, Button.itemIconSize, iconRl, frameRl, null, () -> false, () -> false, () -> true, () -> BuildingServerboundPacket.cancelProduction(placement.originPos, production, first), () -> {}, null);
    }
}
