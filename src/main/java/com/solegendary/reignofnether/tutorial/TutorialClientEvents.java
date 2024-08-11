package com.solegendary.reignofnether.tutorial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.goals.MoveToTargetBlockGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerProd;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
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
                    msg("TIP: If you want to see any of these messages again, opening chat " +
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
                    specialMsg("Nicely done.");
                    nextStageAfterDelay(60);
                }
            }
            case CAMERA_TIPS -> {
                if (stageProgress == 0) {
                    msg("You can also move it in the same way with the arrow keys.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    msg("You can also zoom the camera with ALT+SCROLL and rotate it with " +
                        "ALT+RIGHT-CLICK.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
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
                    specialMsg("Good work!");
                    nextStageAfterDelay(60);
                }
            }
            case MINIMAP_TIPS -> {
                if (stageProgress == 0) {
                    msg("If you need to move really far away, you can also SHIFT+CLICK " +
                        "to recentre the map on that location.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    msg("You can also press M or click the bottom right button to expand the map.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
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
                    specialMsg("Excellent.");
                    nextStageAfterDelay(60);
                }
            }
            case SELECT_UNIT -> {
                if (stageProgress == 0) {
                    msg("Villagers are your worker units who can build and gather resources " +
                        "and are essential to starting and maintaining a good base.");
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 1) {
                    msg("Try selecting one with LEFT-CLICK.");
                    UnitClientEvents.clearSelectedUnits();
                    progressStage();
                }
                else if (stageProgress == 2 && UnitClientEvents.getSelectedUnits().size() > 0) {
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
                        specialMsg("Nice work.");
                        nextStageAfterDelay(60);
                    }
                }
            }
            case BOX_SELECT_UNITS -> {
                if (stageProgress == 0) {
                    msg("Now let's try selecting a group of villagers.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    msg("To do this, LEFT-CLICK and DRAG your mouse across them, then release to select.");
                    progressStage();
                }
                else if (stageProgress == 2 && UnitClientEvents.getSelectedUnits().size() > 1) {
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
                        specialMsg("Great job!");
                        nextStageAfterDelay(60);
                    }
                }
            }
            case UNIT_TIPS -> {
                if (stageProgress == 0) {
                    msg("TIP: You can also double click a unit to select all units of the same type.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    msg("TIP: If you want to deselect your units, press F1.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
                    msg("TIP: For you RTS fans, control grouping with CTRL+NUM is also a feature.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 3) {
                    nextStageAfterSpace();
                }
            }
            case BUILD_INTRO -> {
                if (stageProgress == 0) {
                    msg("It's time to start your base.");
                    progressStageAfterDelay(60);
                }
                else if (stageProgress == 1) {
                    msg("The first and most important building of any base is always the capitol.");
                    progressStageAfterDelay(80);
                }
                else if (stageProgress == 2) {
                    msg("This looks like a good spot for it, being flat ground, near lots of resources and with " +
                        "plenty of space around it for other buildings.");
                    OrthoviewClientEvents.forceMoveCam(BUILD_POS, 120);
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 3) {
                    msg("Note that building takes resources. Luckily, the TOP-LEFT shows we have more than enough " +
                        "WOOD and ORE needed.");
                    nextStageAfterDelay(120);
                }
            }
            case BUILD_TOWN_CENTRE -> {
                if (stageProgress == 0) {
                    msg("Select your workers, then click the bottom-left button and click on the " +
                        "ground where you want to place your capitol.");
                    TutorialRendering.setButtonName(TownCentre.buildingName);
                    progressStage();
                }
                else if (stageProgress == 1 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre)
                        nextStage();
                }
            }
            case BUILDING_TIPS -> {
                if (stageProgress == 0) {
                    msg("If they aren't already, you can have all of your villagers help to build to speed up progress." +
                        "To do this, select your workers and RIGHT-CLICK the building.");
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 1) {
                    msg("You can also select the building itself like a unit to see how far along it is in building.");
                    progressStage();
                }
                else if (stageProgress == 2 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre && townCentre.isBuilt) {
                        specialMsg("Congratulations, you now have a base set up!");
                        nextStageAfterDelay(80);
                    }
                }
            } //msg("You can also set a rally point for your building with RIGHT-CLICK.");
            case TRAIN_WORKER -> {
                if (stageProgress == 0) {
                    msg("Capitols like the Town Centre are the only building that can produce workers like villagers.");
                    progressStageAfterDelay(100);
                }
                if (stageProgress == 1) {
                    msg("Note that producing workers takes 50 FOOD each. We should have enough resources for 3 of them.");
                    progressStageAfterDelay(100);
                }
                if (stageProgress == 2) {
                    msg("With your building selected, RIGHT-CLICK to set a rally point - units will " +
                        "automatically go to that spot when they appear.");
                    progressStageAfterDelay(100);
                }
                if (stageProgress == 3 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre &&
                        townCentre.getRallyPoint() != null) {
                        msg("Now let's try making a villager. LEFT-CLICK to select your Town Centre, then click the " +
                            "bottom-left button to start producing.");
                        TutorialRendering.setButtonName(VillagerProd.itemName);
                        progressStage();
                    }
                }
                if (stageProgress == 4 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre &&
                            townCentre.productionQueue.size() > 0) {
                        msg("TIP: you can queue up as many units to build as you can afford.");
                        progressStage();
                    }
                }
                if (stageProgress == 5 && UnitClientEvents.getAllUnits().size() > 3) {
                    specialMsg("Nice work. Having lots of workers are vital for a healthy base!");
                    TutorialRendering.clearButtonName();
                    progressStageAfterDelay(80);
                }
                if (stageProgress == 6) {
                    msg("TIP: you can cancel any in-progress units for a full refund by clicking their icon at the bottom.");
                    progressStageAfterDelay(100);
                }
                if (stageProgress == 7) {
                    nextStageAfterSpace();
                }
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
