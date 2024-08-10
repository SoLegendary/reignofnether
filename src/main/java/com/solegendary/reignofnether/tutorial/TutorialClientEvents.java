package com.solegendary.reignofnether.tutorial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.goals.MoveToTargetBlockGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;

import static com.solegendary.reignofnether.tutorial.TutorialStage.*;

public class TutorialClientEvents {

    private static Minecraft MC = Minecraft.getInstance();
    private static TutorialStage tutorialStage = INTRO;
    private static boolean enabled = false;

    private static int ticksOnStage = 0;
    private static int msgsOnStage = 0;
    private static int stageProgress = 0; // used to track progress within each TutorialStage

    public static boolean pannedUp = false;
    public static boolean pannedDown = false;
    public static boolean pannedLeft = false;
    public static boolean pannedRight = false;
    public static boolean clickedMinimap = false;

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
            if (!pannedUp)
                TutorialRendering.pointAtWithArrow(poseStack, MC.screen.width / 2, -Button.iconFrameSize, true);
            if (!pannedDown)
                TutorialRendering.pointAtWithArrow(poseStack, MC.screen.width / 2, MC.screen.height, true);
            if (!pannedLeft)
                TutorialRendering.pointAtWithArrow(poseStack, -Button.iconFrameSize, MC.screen.height / 2, false);
            if (!pannedRight)
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
    public static void onKeyRelease(ScreenEvent.KeyReleased.Pre evt) { updateStage(); }

    @SubscribeEvent
    public static void TickEvent(TickEvent.ClientTickEvent evt) {
        if (ticksOnStage < Integer.MAX_VALUE) {
            if (ticksOnStage % 20 == 0)
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
        if (soundEvent != null)
            MC.player.playSound(soundEvent.get(), 0.5f, 1.0f);
    }

    public static boolean isAtOrPastStage(TutorialStage stage) {
        if (!isEnabled())
            return true;
        return tutorialStage.ordinal() >= stage.ordinal();
    }

    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getPoseStack(), MC.font, new String[] {
                "stage: " + getStage(),
                "progress: " + stageProgress,
        });
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
                    msgWithSound("Welcome to RTS view. Instead of the usual first-person minecraft camera, " +
                                 "here we can view the world from above.");
                    stageProgress += 1;
                }
                else if (stageProgress == 1 && ticksOnStage >= 60) {
                    nextStage();
                }
            }
            case PAN_CAMERA -> {
                if (stageProgress == 0 && OrthoviewClientEvents.isEnabled()) {
                    msgWithSound("To move your camera, move your mouse to the edges of the screen, try it now.");
                    stageProgress += 1;
                }
                else if (stageProgress == 1 && pannedUp && pannedDown && pannedLeft && pannedRight) {
                    nextStage();
                }
            }
            case CAMERA_TIPS -> {
                if (stageProgress == 0) {
                    msgWithSound("Good work. You can also move it in the same way with the arrow keys.");
                    nextStage();
                    stageProgress += 1;
                }
                else if (stageProgress == 1 && ticksOnStage >= 60) {
                    msgWithSound("You can also zoom the camera with CTRL+SCROLL and rotate it with " +
                                 "ALT+SCROLL.");
                    stageProgress += 1;
                }
                else if (stageProgress == 2 && ticksOnStage >= 120) {
                    nextStage();
                }
            }
            case MINIMAP_CLICK -> {
                if (stageProgress == 0) {
                    msgWithSound("Since you could get lost with a top-down view like this, here's a minimap " +
                                 "for you to help navigate.");
                    stageProgress += 1;
                }
                else if (stageProgress == 1 && ticksOnStage >= 60) {
                    msgWithSound("You can move around the world quickly by clicking on a spot on the map. " +
                                 "Try doing that now.");
                    stageProgress += 1;
                }
                else if (stageProgress == 2 && clickedMinimap) {
                    nextStage();
                }
            }
            case MINIMAP_TIPS -> {
                if (stageProgress == 0) {
                    msgWithSound("Good work. If you need to move really far away, you can also SHIFT+CLICK " +
                                 "to recentre the map on that location.");
                    stageProgress += 1;
                }
                else if (stageProgress == 1 && ticksOnStage >= 60) {
                    msgWithSound("You can also press M or click the bottom right button to expand the map.");
                    nextStage();
                }
            }
            case PLACE_WORKERS -> {
                if (stageProgress == 0) {
                    msgWithSound("It's time to start playing with some units.");
                    stageProgress += 1;
                }
                else if (stageProgress == 1 && ticksOnStage >= 60) {
                    msgWithSound("Let's get started by spawning in some villagers here.");
                    OrthoviewClientEvents.forceMoveCam(SPAWN_POS, 20);
                    stageProgress += 1;
                }
                else if (stageProgress == 2 && ticksOnStage >= 120) {
                    msgWithSound("Left-click the button at the top right and then click on the ground where " +
                                 "you want to place them.");
                    TutorialRendering.setButtonName("Villagers");
                    stageProgress += 1;
                }
                else if (stageProgress == 3 && PlayerClientEvents.isRTSPlayer) {
                    TutorialRendering.clearButtonName();
                    nextStage();
                }
            }
            case SELECT_UNIT -> {
                if (stageProgress == 0) {
                    msgWithSound("Villagers are your worker units who can build and gather resources.");
                    stageProgress += 1;
                }
                else if (stageProgress == 1 && ticksOnStage >= 60) {
                    msgWithSound("Try selecting one by clicking them.");
                    stageProgress += 1;
                }
                else if (stageProgress == 2 && UnitClientEvents.getSelectedUnits().size() > 0) {
                    msgWithSound("Now, try right clicking where you want to move.");
                    nextStage();
                }
            }
            case MOVE_UNIT -> {
                if (stageProgress == 0 && UnitClientEvents.getSelectedUnits().size() > 0) {
                    Unit unit = (Unit) UnitClientEvents.getSelectedUnits().get(0);
                    MoveToTargetBlockGoal goal = unit.getMoveGoal();
                    if (goal != null && goal.getMoveTarget() != null) {
                        msgWithSound("Excellent. Now we need to get the rest here too.");
                        stageProgress += 1;
                    }
                }
            }
            case BOX_SELECT_UNITS -> {
            }
            case MOVE_UNITS -> {
            }
            case UNIT_TIPS -> {
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
