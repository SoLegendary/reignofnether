package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class BuildingPlaceButton extends Button {
    public BuildingPlaceButton(String name, ResourceLocation rl, Keybinding hotkey, Supplier<Boolean> isSelected,
                               Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled,
                               List<FormattedCharSequence> tooltipLines, Building building) {

        // generate x/y based on given position (starting at 0 which is bottom left 1 row above generic action buttons)
        super(name, Button.itemIconSize, rl, hotkey, isSelected, isHidden, isEnabled, () -> BuildingClientEvents.setBuildingToPlace(building), null, tooltipLines);
    }
}
