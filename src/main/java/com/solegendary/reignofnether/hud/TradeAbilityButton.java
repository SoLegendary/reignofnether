package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.TradeResources;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class TradeAbilityButton extends AbilityButton {

    private final int rate;

    public TradeAbilityButton(String name, ResourceLocation rl, Keybinding hotkey, Supplier<Boolean> isSelected, Supplier<Boolean> isHidden,
                              Supplier<Boolean> isEnabled, Runnable onLeftClick, Runnable onRightClick, List<FormattedCharSequence> tooltipLines,
                              @Nullable Ability ability, BuildingPlacement placement, int rate) {
        super(name, rl, hotkey, isSelected, isHidden, isEnabled, onLeftClick, onRightClick, tooltipLines, ability, placement);
        this.rate = rate;
    }

    @Override
    protected void renderHotkey(GuiGraphics guiGraphics, int x, int y) {
        // hotkey letter
        if (this.hotkey != null) {
            String hotkeyStr = hotkey.getCurrentLabel();
            hotkeyStr = hotkeyStr.substring(0,Math.min(3, hotkeyStr.length()));
            guiGraphics.pose().translate(0,0,1);
            MyRenderer.drawScaledCenteredString(guiGraphics,
                    MC.font,
                    hotkeyStr,
                    x + iconSize + 9 - (hotkeyStr.length() * 4),
                    y + iconSize + 1,
                    0xFFFFFF,
                    0.75f);
        }

        MyRenderer.drawScaledString(guiGraphics,
                MC.font,
                String.valueOf(rate),
                x + 1,
                y + 2,
                0xFFFFFF,
                0.75f);
    }
}
