package com.solegendary.reignofnether.tutorial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;

import static com.solegendary.reignofnether.tutorial.TutorialStage.PAN_CAMERA;

public class TutorialClientEvents {

    private static Minecraft MC = Minecraft.getInstance();
    private static TutorialStage tutorialStage = PAN_CAMERA;
    private static boolean enabled = false;

    private static int ticksOnStage = 0;
    private static int msgsOnStage = 0;
    private static int stageProgress = 0; // used to track progress within each TutorialStage

    private static final Vec3i SPAWN_POS = new Vec3i(-2950, 0, -1166);

    public static int getStageProgress() {
        return stageProgress;
    }

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

    private static void nextStage() {
        tutorialStage = tutorialStage.next();
        ticksOnStage = 0;
        msgsOnStage = 0;
    }
    private static void prevStage() {
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
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre evt) { updateStage(); }

    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Pre evt) { updateStage(); }

    @SubscribeEvent
    public static void TickEvent(TickEvent.ClientTickEvent evt) {
        if (ticksOnStage < Integer.MAX_VALUE) {
            if (ticksOnStage % 40 == 0)
                updateStage();
            ticksOnStage += 1;
        }
    }

    private static void msgWithSound(String msg) {
        msgWithSound(msg, SoundRegistrar.CHAT);
    }

    private static void msgWithSound(String msg, RegistryObject<SoundEvent> soundEvent) {
        if (MC.player == null)
            return;
        MC.player.sendSystemMessage(Component.literal(msg));
        MC.player.playSound(soundEvent.get(), 0.2f, 1.0f);
    }

    public boolean isAtOrPastStage(TutorialStage stage) {
        return tutorialStage.ordinal() >= stage.ordinal();
    }

    // Whenever doing anything that could be a tutorial action like enabling orthoview or building your first building,
    // check here to progress the tutorial and give updates to the player.
    // This theoretically should be able to be called at any time but will only actually progress if actions are done
    // This should be called in specific clientside events like
    public static void updateStage() {
        if (!isEnabled())
            return;

        switch(tutorialStage) {
            case INTRO -> {
                if (stageProgress == 0 && OrthoviewClientEvents.isEnabled()) {
                    OrthoviewClientEvents.lockCam();
                    stageProgress += 1;
                    msgWithSound("Welcome to RTS view. Instead of the usual first-person minecraft camera," +
                                 "here we can view the world from above.");
                    nextStage();
                }
            }
            case PLACE_VILLAGERS -> {
                if (stageProgress == 0 && ticksOnStage >= 40) {
                    msgWithSound("Let's get started by spawning in some villagers here.");
                    OrthoviewClientEvents.forceMoveCam(SPAWN_POS, 20);
                    stageProgress += 1;
                } else if (stageProgress == 1 && ticksOnStage >= 80) {
                    msgWithSound("Left-click the button at the top right and then click on the ground where" +
                                 "you want to place them.");
                    TutorialRendering.setButtonName("Villagers");
                    stageProgress += 1;
                } else if (stageProgress == 2 && UnitClientEvents.getAllUnits().size() > 0) {
                    TutorialRendering.clearButtonName();
                    nextStage();
                }
            }
            case SELECT_UNIT -> {
            }
            case MOVE_UNIT -> {
            }
            case BOX_SELECT_UNITS -> {
            }
            case MOVE_UNITS -> {
            }
            case UNIT_TIPS -> {
            }
            case PAN_CAMERA -> {
                if (stageProgress == 0 && OrthoviewClientEvents.isEnabled()) {
                    OrthoviewClientEvents.unlockCam();
                    stageProgress += 1;
                    msgWithSound("Welcome to RTS view. Instead of the usual first-person minecraft camera," +
                            "here we can view the world from above.");
                    nextStage();
                }
            }
            case PAN_CAMERA_TIPS -> {
            }
            case MINIMAP_INTRO -> {
            }
            case MINIMAP_CLICK -> {
            }
            case MINIMAP_SHIFT_CLICK -> {
            }
            case MINIMAP_TIPS -> {
            }
            case BUILD_TOWN_CENTRE -> {
            }
            case BUILDING_TIPS -> {
            }
            case SELECT_BUILDING -> {
            }
            case TRAIN_WORKER -> {
            }
            case GATHER_RESOURCES -> {
            }
            case HUNT_ANIMALS -> {
            }
            case RETURN_FOOD -> {
            }
            case BUILD_BASE -> {
            }
            case BUILD_ARMY -> {
            }
            case DEFEND_BASE -> {
            }
            case DEFEND_BASE_AGAIN -> {
            }
            case REPAIR_BUILDING -> {
            }
            case BUILD_BRIDGE -> {
            }
            case ATTACK_ENEMY_BASE -> {
            }
            case OUTRO -> {
            }
        }
    }
}
