package com.solegendary.reignofnether.tutorial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.hud.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;

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

    public static void renderPointingAtNextButton(PoseStack poseStack, ArrayList<Button> arrayList) {

    }
}
