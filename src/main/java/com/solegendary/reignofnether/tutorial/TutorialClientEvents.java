package com.solegendary.reignofnether.tutorial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

import static com.solegendary.reignofnether.registrars.SoundRegistrar.*;
import static com.solegendary.reignofnether.tutorial.TutorialStage.*;

public class TutorialClientEvents {

    private static Minecraft MC = Minecraft.getInstance();
    private static TutorialStage tutorialStage = INTRO;
    private static boolean enabled = false;

    private static int ticksToProgressStage = 0;
    private static int ticksToNextStage = 0;
    private static int ticksOnStage = 0;
    private static int stageProgress = 0; // used to track progress within each TutorialStage
    private static boolean pressSpaceToContinue = false;
    private static boolean blockUpdateStage = false; // prevent updateStage being called when we block it with a delay

    public static boolean pannedUp = false;
    public static boolean pannedDown = false;
    public static boolean pannedLeft = false;
    public static boolean pannedRight = false;
    public static boolean clickedMinimap = false;

    private static final Vec3i SPAWN_POS = new Vec3i(-2950, 0, -1166);
    private static final Vec3i BUILD_POS = new Vec3i(-2944, 0, -1200);

    public static int getStageProgress() {
        return stageProgress;
    }

    public static boolean isEnabled() {
        return enabled && MC.hasSingleplayerServer();
    }

    public static void setEnabled(boolean value) {
        if (value && !enabled && MC.player != null) {
            MC.player.sendSystemMessage(Component.literal(""));
            MC.player.sendSystemMessage(Component.literal("Welcome to the Reign of Nether Tutorial!").withStyle(Style.EMPTY.withBold(true)));
            MC.player.sendSystemMessage(Component.literal("Press F12 to get started."));
            MC.player.sendSystemMessage(Component.literal(""));
        }
        enabled = value;
    }

    public static TutorialStage getStage() { return tutorialStage; }

    private static void nextStage() {
        tutorialStage = tutorialStage.next();
        ticksOnStage = 0;
        stageProgress = 0;
        blockUpdateStage = false;
        updateStage();
    }
    private static void prevStage() {
        tutorialStage = tutorialStage.prev();
        ticksOnStage = 0;
        stageProgress = 0;
        blockUpdateStage = false;
        updateStage();
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
    public static void onKeyRelease(ScreenEvent.KeyReleased.Pre evt) { updateStage(); }

    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Pre evt) {
        if (pressSpaceToContinue && evt.getKeyCode() == GLFW.GLFW_KEY_SPACE) {
            pressSpaceToContinue = false;
            nextStage();
        }
    }

    @SubscribeEvent
    public static void TickEvent(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;
        if (ticksOnStage < Integer.MAX_VALUE) {
            if (ticksOnStage % 20 == 0)
                updateStage();
            ticksOnStage += 1;
        }
        if (ticksToProgressStage > 0) {
            ticksToProgressStage -= 1;
            if (ticksToProgressStage == 0) {
                progressStage();
                updateStage();
            }
        }
        if (ticksToNextStage > 0) {
            ticksToNextStage -= 1;
            if (ticksToNextStage == 0)
                nextStage();
        }
    }

    private static void msg(String msg, boolean bold, RegistryObject<SoundEvent> soundEvt) {
        if (MC.player == null)
            return;
        MC.player.sendSystemMessage(Component.literal(""));
        if (bold)
            MC.player.sendSystemMessage(Component.literal(msg).withStyle(Style.EMPTY.withBold(true)));
        else
            MC.player.sendSystemMessage(Component.literal(msg));
        MC.player.sendSystemMessage(Component.literal(""));
        MC.player.playSound(soundEvt.get(), 1.2f, 1.0f);
    }

    private static void msg(String msg) {
        msg(msg, false, CHAT);
    }

    private static void specialMsg(String msg) {
        msg(msg, true, ALLY);
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

    private static void progressStage() {
        blockUpdateStage = false;
        stageProgress += 1;
    }

    private static void progressStageAfterDelay(int delay) {
        blockUpdateStage = true;
        ticksToProgressStage = delay;
    }

    private static void nextStageAfterDelay(int delay) {
        blockUpdateStage = true;
        ticksToNextStage = delay;
    }

    private static void nextStageAfterSpace() {
        blockUpdateStage = true;
        msg("Press SPACE when you're ready to continue.", true, CHAT);
        pressSpaceToContinue = true;
    }

    // Whenever doing anything that could be a tutorial action like enabling orthoview or building your first building,
    // check here to progress the tutorial and give updates to the player.
    // This theoretically should be able to be called at any time but will only actually progress if actions are done
    // This should be called in specific clientside events like
    public static void updateStage() {
        if (!isEnabled() || blockUpdateStage)
            return;

        switch(tutorialStage) {
            case INTRO -> {
                if (stageProgress == 0 && OrthoviewClientEvents.isEnabled()) {
                    OrthoviewClientEvents.lockCam();
                    msg("Welcome to RTS view. Instead of the usual first-person minecraft camera, " +
                        "here we can view the world from a top-down perspective.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    msg("Note that if you want to see any of these messages again, opening chat " +
                        "has been changed to the ENTER key.");
                    nextStageAfterDelay(100);
                }
            }
            case PAN_CAMERA -> {
                if (stageProgress == 0) {
                    OrthoviewClientEvents.unlockCam();
                    msg("To move your camera, move your mouse to the edges of the screen. Try it now.");
                    progressStage();
                }
                else if (stageProgress == 1 && pannedUp && pannedDown && pannedLeft && pannedRight) {
                    nextStage();
                }
            }
            case CAMERA_TIPS -> {
                if (stageProgress == 0) {
                    specialMsg("Nicely done.");
                    progressStageAfterDelay(50);
                }
                if (stageProgress == 1) {
                    msg("You can also move it in the same way with the arrow keys.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
                    msg("You can also zoom the camera with ALT+SCROLL and rotate it with " +
                        "ALT+RIGHT-CLICK.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 3) {
                    nextStageAfterSpace();
                }
            }
            case MINIMAP_CLICK -> {
                if (stageProgress == 0) {
                    msg("Since you could get lost with a top-down view like this, here's a minimap " +
                        "for you to help navigate.");
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 1) {
                    msg("You can move around the world quickly by clicking on a spot on the map. " +
                        "Try doing that now.");
                    progressStage();
                }
                else if (stageProgress == 2 && clickedMinimap) {
                    nextStage();
                }
            }
            case MINIMAP_TIPS -> {
                if (stageProgress == 0) {
                    specialMsg("Good work!");
                    progressStageAfterDelay(50);
                }
                if (stageProgress == 1) {
                    msg("If you need to move really far away, you can also SHIFT+CLICK " +
                        "to recentre the map on that location.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
                    msg("You can also press M or click the bottom right button to expand the map.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 3) {
                    nextStageAfterSpace();
                }
            }
            case PLACE_WORKERS_A -> {
                if (stageProgress == 0) {
                    msg("It's time to start playing with some units.");
                    progressStageAfterDelay(100);
                } else if (stageProgress == 1) {
                    msg("Let's get started by spawning in some villagers here.");
                    OrthoviewClientEvents.forceMoveCam(SPAWN_POS, 30);
                    nextStageAfterDelay(100);
                }
            }
            case PLACE_WORKERS_B -> {
                if (stageProgress == 0) {
                    msg("Click the button at the top right and then click on the ground where " +
                        "you want to place them.");
                    TutorialRendering.setButtonName("Villagers");
                    progressStage();
                }
                else if (stageProgress == 1 && PlayerClientEvents.isRTSPlayer) {
                    TutorialRendering.clearButtonName();
                    nextStage();
                }
            }
            case SELECT_UNIT -> {
                if (stageProgress == 0) {
                    specialMsg("Excellent.");
                    progressStageAfterDelay(50);
                }
                else if (stageProgress == 1) {
                    msg("Villagers are your worker units who can build and gather resources " +
                        "and are vital to starting and maintaining a good base.");
                    progressStageAfterDelay(150);
                }
                else if (stageProgress == 2) {
                    msg("Try selecting one with LEFT-CLICK.");
                    UnitClientEvents.clearSelectedUnits();
                    progressStage();
                }
                else if (stageProgress == 3 && UnitClientEvents.getSelectedUnits().size() > 0) {
                    nextStage();
                }
            }
            case MOVE_UNIT -> {
                if (stageProgress == 0 && UnitClientEvents.getSelectedUnits().size() > 0) {
                    msg("Now, RIGHT-CLICK where you want to move.");
                    progressStage();
                }
                else if (stageProgress == 1 && UnitClientEvents.getSelectedUnits().size() > 0) {
                    Unit unit = (Unit) UnitClientEvents.getSelectedUnits().get(0);
                    MoveToTargetBlockGoal goal = unit.getMoveGoal();
                    if (goal != null && goal.getMoveTarget() != null) {
                        nextStage();
                    }
                }
            }
            case BOX_SELECT_UNITS -> {
                if (stageProgress == 0) {
                    specialMsg("Nice work.");
                    progressStageAfterDelay(50);
                }
                if (stageProgress == 1) {
                    msg("Now let's try selecting a group of villagers.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
                    msg("To do this, LEFT-CLICK and DRAG your mouse across them, then release to select.");
                    progressStage();
                }
                else if (stageProgress == 3 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    nextStage();
                }
            }
            case MOVE_UNITS -> {
                if (stageProgress == 0 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    msg("Now, RIGHT-CLICK where you want to move the group.");
                    progressStage();
                }
                else if (stageProgress == 1 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    Unit unit = (Unit) UnitClientEvents.getSelectedUnits().get(1);
                    MoveToTargetBlockGoal goal = unit.getMoveGoal();
                    if (goal != null && goal.getMoveTarget() != null) {
                        nextStage();
                    }
                }
            }
            case UNIT_TIPS -> {
                if (stageProgress == 0) {
                    specialMsg("Great job!");
                    progressStageAfterDelay(50);
                }
                else if (stageProgress == 1) {
                    msg("You can also double click a unit to select all units of the same type.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
                    msg("If you want to deselect your units, press F1.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 3) {
                    msg("For you RTS fans out there, control grouping with CTRL+NUM is also a feature.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 4) {
                    nextStageAfterSpace();
                }
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
