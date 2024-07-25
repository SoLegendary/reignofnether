package com.solegendary.reignofnether.tutorial;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class TutorialClientEvents {

    private static Minecraft MC = Minecraft.getInstance();
    private static int tutorialStage = 0;
    private static boolean enabled = false;

    public static boolean isEnabled() { return enabled; }

    public static void setEnabled(boolean value) {
        if (value && !enabled && MC.player != null) {
            MC.player.sendSystemMessage(Component.literal("Welcome to the Reign of Nether Tutorial!").withStyle(Style.EMPTY.withBold(true)));
            MC.player.sendSystemMessage(Component.literal("Press F12 to get started."));
        }
        enabled = value;
    }

}
