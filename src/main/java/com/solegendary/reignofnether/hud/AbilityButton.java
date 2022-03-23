package com.solegendary.reignofnether.hud;

import net.minecraft.client.KeyMapping;

import java.util.function.Supplier;

public class AbilityButton extends Button {

    float cooldown = 0;
    float range = 10;
    float radius = 0; // if <= 0, is single target

    public AbilityButton(String abilityName, int x, int y, int iconSize, int iconFrameSize, int iconFrameSelectedSize,
                         String iconResourcePath, String iconFrameResourcePath, String iconFrameSelectedResourcePath,
                         KeyMapping hotkey, Supplier<Boolean> isSelected, Runnable onClick) {

        super(abilityName, x, y, iconSize, iconFrameSize, iconFrameSelectedSize, iconResourcePath,
                iconFrameResourcePath, iconFrameSelectedResourcePath, hotkey, isSelected, onClick);
    }
}
