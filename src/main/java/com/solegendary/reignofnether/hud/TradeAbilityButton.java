package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class TradeAbilityButton extends AbilityButton {

    public TradeAbilityButton(String name, ResourceLocation rl, Keybinding hotkey, Supplier<Boolean> isSelected, Supplier<Boolean> isHidden,
                              Supplier<Boolean> isEnabled, Runnable onLeftClick, Runnable onRightClick, List<FormattedCharSequence> tooltipLines,
                              @Nullable Ability ability, BuildingPlacement placement, int rate) {
        super(name, rl, hotkey, isSelected, isHidden, isEnabled, onLeftClick, onRightClick, tooltipLines, ability, placement);
        // what you get for 100 paid, shown as the cell's bottom-left count
        String rateStr = "+" + rate;
        this.bottomLeftText = () -> rateStr;
    }
}
