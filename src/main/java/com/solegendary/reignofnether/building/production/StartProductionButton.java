package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Supplier;

public class StartProductionButton extends Button {
   public StartProductionButton(String name, ResourceLocation rl, Keybinding hotkey,
                                Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled,
                                List<FormattedCharSequence> tooltipLines, ProductionItem production) {

        // generate x/y based on given position (starting at 0 which is bottom left 1 row above generic action buttons)
        super(name, Button.itemIconSize, rl, hotkey, () -> false, isHidden, isEnabled, () -> BuildingServerboundPacket.startProduction(production), () -> {}, tooltipLines);
    }

    public StartProductionButton(String name, ResourceLocation iconRl, ResourceLocation frameRl, Keybinding hotkey,
                                 Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled,
                                 List<FormattedCharSequence> tooltipLines, ProductionItem production) {

        // generate x/y based on given position (starting at 0 which is bottom left 1 row above generic action buttons)
        super(name, Button.itemIconSize, iconRl, frameRl, hotkey, () -> false, isHidden, isEnabled, () -> BuildingServerboundPacket.startProduction(production), () -> {}, tooltipLines);
    }
}
