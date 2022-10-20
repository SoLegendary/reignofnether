package com.solegendary.reignofnether.hud;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Supplier;

public class AbilityButton extends Button {

    float cooldown = 0;
    float range = 0; // if <= 0, is melee
    float radius = 0; // if <= 0, is single target

    public AbilityButton(String abilityName, int iconSize, String iconResourcePath,
                         KeyMapping hotkey, Supplier<Boolean> isSelected, Supplier<Boolean> isActive,
                         Supplier<Boolean> isEnabled, Runnable onClick, List<FormattedCharSequence> tooltipLines,
                         float cooldown, float range, float radius) {

        // generate x/y based on given position (starting at 0 which is bottom left 1 row above generic action buttons)
        super(abilityName, iconSize, iconResourcePath, hotkey, isSelected, isActive, isEnabled, onClick, tooltipLines);

        this.cooldown = cooldown;
        this.range = range;
        this.radius = radius;
    }
}
