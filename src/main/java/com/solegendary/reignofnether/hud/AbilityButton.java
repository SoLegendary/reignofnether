package com.solegendary.reignofnether.hud;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

public class AbilityButton extends Button {

    float cooldown = 0;
    float range = 0; // if <= 0, is melee
    float radius = 0; // if <= 0, is single target
    boolean active = false; // light up when being used, channeled, toggled on, etc.

    public AbilityButton(String abilityName, int iconSize, String iconResourcePath,
                         KeyMapping hotkey, Supplier<Boolean> isSelected, Runnable onClick,
                         float cooldown, float range, float radius) {

        // generate x/y based on given position (starting at 0 which is bottom left 1 row above generic action buttons)
        super(abilityName, iconSize, iconResourcePath, hotkey, isSelected, onClick);

        this.cooldown = cooldown;
        this.range = range;
        this.radius = radius;
    }
}
