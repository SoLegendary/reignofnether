package com.solegendary.reignofnether.hud;

import net.minecraft.client.KeyMapping;

import java.util.function.Supplier;

public class AbilityButton extends Button {

    float cooldown = 0;
    float range = 10;

    public AbilityButton(int x, int y, int iconSize, int iconFrameSize, int iconFrameSelectedSize,
                         String iconResourcePath, String iconFrameResourcePath, String iconFrameSelectedResourcePath,
                         KeyMapping hotkey, Supplier<Boolean> isSelected, Runnable onClick) {

        super(x, y, iconSize, iconFrameSize, iconFrameSelectedSize, iconResourcePath,
                iconFrameResourcePath, iconFrameSelectedResourcePath, hotkey, isSelected, onClick);
    }
}
