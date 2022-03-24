package com.solegendary.reignofnether.hud;

import net.minecraft.client.KeyMapping;

import java.util.function.Supplier;

public class AbilityButton extends Button {

    float cooldown = 0;
    float range = 10;
    float radius = 0; // if <= 0, is single target

    public AbilityButton(String abilityName, int x, int y, int iconSize, String iconResourcePath,
                         KeyMapping hotkey, Supplier<Boolean> isSelected, Runnable onClick,
                         float cooldown, float range, float radius) {

        super(abilityName, x, y, iconSize, iconResourcePath, hotkey, isSelected, onClick);

        this.cooldown = cooldown;
        this.range = range;
        this.radius = radius;
    }
}
