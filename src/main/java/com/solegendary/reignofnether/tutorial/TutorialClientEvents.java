package com.solegendary.reignofnether.tutorial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.hud.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

import static com.solegendary.reignofnether.tutorial.TutorialStage.*;

public class TutorialClientEvents {

    // TODO: force camera pan feature

    private static Minecraft MC = Minecraft.getInstance();
    private static TutorialStage tutorialStage = INTRO;
    private static boolean enabled = false;

    private static int ticksOnStage = 0;

    public static boolean isEnabled() { return enabled; }

    public static void setEnabled(boolean value) {
        if (value && !enabled && MC.player != null) {
            MC.player.sendSystemMessage(Component.literal("Welcome to the Reign of Nether Tutorial!").withStyle(Style.EMPTY.withBold(true)));
            MC.player.sendSystemMessage(Component.literal("Press F12 to get started."));
        }
        enabled = value;
    }

    public static TutorialStage getStage() { return tutorialStage; }

    private static void incrementStage() {
        tutorialStage = tutorialStage.next();
        ticksOnStage = 0;
    }
    private static void decrementStage() {
        tutorialStage = tutorialStage.prev();
        ticksOnStage = 0;
    }

    // check if we need to render an arrow to point at the next button
    public static void checkAndHighlightNextButton(PoseStack poseStack, ArrayList<Button> buttons) {
        TutorialRendering.highlightNextButton(poseStack, buttons);
    }

    // whenever doing anything that could be a tutorial action like enabling orthoview or building your first building,
    // check here to progress the tutorial. Also could
    public static void updateStage() {
        if (!isEnabled())
            return;


    }

    @SubscribeEvent
    public static void TickEvent(TickEvent.ClientTickEvent evt) {
        if (ticksOnStage < 999999)
            ticksOnStage += 1;
    }
}
