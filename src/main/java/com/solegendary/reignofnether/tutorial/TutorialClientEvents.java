package com.solegendary.reignofnether.tutorial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

import static com.solegendary.reignofnether.tutorial.TutorialStage.*;

public class TutorialClientEvents {

    private static Minecraft MC = Minecraft.getInstance();
    private static TutorialStage tutorialStage = PAN_CAMERA;
    private static boolean enabled = false;

    private static int ticksOnStage = 0;
    private static int msgsOnStage = 0;

    public static boolean isEnabled() {
        return enabled && MC.hasSingleplayerServer();
    }

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
        msgsOnStage = 0;
    }
    private static void decrementStage() {
        tutorialStage = tutorialStage.prev();
        ticksOnStage = 0;
        msgsOnStage = 0;
    }

    // check if we need to render an arrow to point at the next button
    public static void checkAndRenderNextAction(PoseStack poseStack, ArrayList<Button> buttons) {
        if (tutorialStage == PAN_CAMERA && MC.screen != null) {
            TutorialRendering.pointAtWithArrow(poseStack, MC.screen.width / 2, -Button.iconFrameSize, true);
            TutorialRendering.pointAtWithArrow(poseStack, MC.screen.width / 2, MC.screen.height, true);
            TutorialRendering.pointAtWithArrow(poseStack, -Button.iconFrameSize, MC.screen.height / 2, false);
            TutorialRendering.pointAtWithArrow(poseStack, MC.screen.width, MC.screen.height / 2, false);
        } else {
            TutorialRendering.highlightNextButton(poseStack, buttons);
        }
    }

    @SubscribeEvent
    public static void TickEvent(TickEvent.ClientTickEvent evt) {
        if (ticksOnStage < Integer.MAX_VALUE) {
            if (ticksOnStage % 40 == 0)
                updateStage();
            ticksOnStage += 1;
        }
    }

    @SubscribeEvent
    public static void onButtonPress(ScreenEvent.KeyPressed.Pre evt) {
        if (evt.getKeyCode() == GLFW.GLFW_KEY_DELETE) {
            OrthoviewClientEvents.forceMoveCam(-2950, -1166, 20);
        }
    }

    // whenever doing anything that could be a tutorial action like enabling orthoview or building your first building,
    // check here to progress the tutorial. Also could
    public static void updateStage() {
        if (!isEnabled())
            return;

        switch(tutorialStage) {
        }
    }
}
