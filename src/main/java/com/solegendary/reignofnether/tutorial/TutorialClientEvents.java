package com.solegendary.reignofnether.tutorial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.buildings.villagers.*;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.goals.MoveToTargetBlockGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.*;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.registrars.SoundRegistrar.*;
import static com.solegendary.reignofnether.tutorial.TutorialStage.*;

public class TutorialClientEvents {

    // TODO: option to have fog of war locked in during tutorial (add more steps during movement and attack/defense stages)

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

    private static int foodBeforeHunting = 0;

    private static final Vec3i SPAWN_POS = new Vec3i(-2950, 0, -1166);
    private static final Vec3i BUILD_POS = new Vec3i(-2944, 0, -1200);
    private static final Vec3i WOOD_POS = new Vec3i(-2919, 0, -1196);
    private static final Vec3i ORE_POS = new Vec3i(-2951, 0, -1224);
    private static final Vec3i FOOD_POS = new Vec3i(-2939, 0, -1173);

    private static int helpButtonClicks = 0;
    private static String helpButtonText = "";
    public static final Button helpButton = new Button(
            "Tutorial Help",
            18,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/help.png"),
            (Keybinding) null,
            () -> false,
            () -> !isEnabled(),
            () -> isEnabled() && !helpButtonText.isEmpty(),
            () ->  {
                helpButtonClicks += 1;
                msg(helpButtonText, true, CHAT);
            },
            () -> { },
            List.of(FormattedCharSequence.forward("Tutorial Help", Style.EMPTY))
    );

    private static void setHelpButtonText(String text) {
        helpButtonClicks = 0;
        helpButtonText = text;
    }

    private static void clearHelpButtonText() {
        helpButtonClicks = 0;
        helpButtonText = "";
    }

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
        clearHelpButtonText();
        updateStage();
    }
    private static void prevStage() {
        tutorialStage = tutorialStage.prev();
        ticksOnStage = 0;
        stageProgress = 0;
        blockUpdateStage = false;
        clearHelpButtonText();
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
        if (evt.phase != TickEvent.Phase.END || MC.isPaused())
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
        MC.player.playSound(soundEvt.get(), 1.2f, 1.0f);
        MC.player.sendSystemMessage(Component.literal(""));
        if (bold)
            MC.player.sendSystemMessage(Component.literal(msg).withStyle(Style.EMPTY.withBold(true)));
        else
            MC.player.sendSystemMessage(Component.literal(msg));
        MC.player.sendSystemMessage(Component.literal(""));
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
        updateStage();
    }

    private static void progressStageAfterDelay(int delay) {
        blockUpdateStage = true;
        ticksToProgressStage = delay;
    }

    private static void nextStageAfterDelay(int delay) {
        blockUpdateStage = true;
        ticksToNextStage = delay;
        clearHelpButtonText();
    }

    private static void nextStageAfterSpace() {
        blockUpdateStage = true;
        msg("Press SPACE when you're ready to continue.", true, CHAT);
        setHelpButtonText("Press SPACE when you're ready to continue.");
        pressSpaceToContinue = true;
    }

    private static boolean hasUnitSelected(String unitName) {
        return UnitClientEvents.getSelectedUnits().size() > 0 &&
                UnitClientEvents.getSelectedUnits().get(0)
                .getName().getString().toLowerCase().contains(unitName.toLowerCase());
    }

    private static boolean hasBuildingSelected(String buildingName) {
        return BuildingClientEvents.getSelectedBuildings().size() > 0 &&
                BuildingClientEvents.getSelectedBuildings().get(0)
                .name.toLowerCase().equals(buildingName.toLowerCase());
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
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
                    msg("If at any point you're lost or need a reminder on what to do next, click the button at the " +
                        "top right. Try doing that now to continue.");
                    setHelpButtonText("You needed to click this button, which you just did. Great work!");
                    TutorialRendering.setButtonName(helpButton.name);
                    progressStage();
                }
                else if (stageProgress == 3 && helpButtonClicks > 0) {
                    nextStageAfterDelay(100);
                }
            }
            case PAN_CAMERA -> {
                if (stageProgress == 0) {
                    OrthoviewClientEvents.unlockCam();
                    msg("To move your camera, move your mouse to the edges of the screen. Try it now.");
                    setHelpButtonText("Move your mouse to each edge of your screen until the camera starts moving, " +
                                        "as shown by the arrows.");
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
                    clickedMinimap = false;
                    msg("You can move around the world quickly by clicking on a spot on the map. " +
                        "Try doing that now.");
                    setHelpButtonText("Click a spot on the minimap at the bottom right to move the camera there");
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
                    OrthoviewClientEvents.forceMoveCam(SPAWN_POS, 50);
                    nextStageAfterDelay(80);
                }
            }
            case PLACE_WORKERS_B -> {
                if (stageProgress == 0) {
                    msg("Click the button at the top right and then click on the ground where " +
                        "you want to place them.");
                    setHelpButtonText("Click the button at the top right and then click on the ground where " +
                                        "you want to place your villagers.");
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
                    setHelpButtonText("LEFT-CLICK a villager to select it.");
                    UnitClientEvents.clearSelectedUnits();
                    progressStage();
                }
                else if (stageProgress == 2 && hasUnitSelected("villager")) {
                    nextStage();
                }
            }
            case MOVE_UNIT -> {
                if (stageProgress == 0 && hasUnitSelected("villager")) {
                    msg("Now, RIGHT-CLICK where you want to move.");
                    setHelpButtonText("LEFT-CLICK a villager to select it, then RIGHT-CLICK on the ground to move it.");
                    progressStage();
                }
                else if (stageProgress == 1 && hasUnitSelected("villager")) {
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
                    msg("To do this, hold LEFT-CLICK and DRAG your mouse across them, then release to select.");
                    setHelpButtonText("Hold LEFT-CLICK and DRAG your mouse across a group of villagers, " +
                                    "then release to select them.");
                    progressStage();
                }
                else if (stageProgress == 2 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    nextStage();
                }
            }
            case MOVE_UNITS -> {
                if (stageProgress == 0 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    msg("Now, RIGHT-CLICK where you want to move the group.");
                    setHelpButtonText("With a group of villagers selected, RIGHT-CLICK on the ground to move them.");
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
                    OrthoviewClientEvents.forceMoveCam(BUILD_POS, 50);
                    progressStageAfterDelay(100);
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
                    setHelpButtonText("Select your workers, then click the bottom-left button and click on the " +
                                      "ground to place a town centre.");
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
                    setHelpButtonText("Just wait for your Town Centre to complete. If your workers stopped building " +
                                      "for some reason, just select them and RIGHT-CLICK it to resume.");
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
                else if (stageProgress == 1) {
                    msg("Note that producing workers takes 50 FOOD each. We should have enough resources for 3 of them.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
                    msg("LEFT-CLICK to select your Town Centre, RIGHT-CLICK to set a rally point - units will " +
                        "automatically go to that spot when they appear.");
                    setHelpButtonText("LEFT-CLICK to select your Town Centre, then RIGHT-CLICK on the ground to set a rally point.");
                    progressStage();
                }
                else if (stageProgress == 3 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre &&
                        townCentre.getRallyPoint() != null) {
                        msg("Now let's try making a villager. With your Town Centre selected, click the " +
                            "bottom-left button to start producing one.");
                        setHelpButtonText("LEFT-CLICK to select your Town Centre, click the bottom-left button make a villager, " +
                                          "then wait for it to be completed.");
                        TutorialRendering.setButtonName(VillagerProd.itemName);
                        progressStage();
                    }
                }
                else if (stageProgress == 4 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre &&
                            townCentre.productionQueue.size() > 0) {
                        msg("TIP: you can queue up as many units to build as you can afford.");
                        progressStage();
                    }
                }
                else if (stageProgress == 5 && UnitClientEvents.getAllUnits().size() > 3) {
                    specialMsg("Nice work. Workers are vital for a healthy base!");
                    clearHelpButtonText();
                    TutorialRendering.clearButtonName();
                    progressStageAfterDelay(80);
                }
                else if (stageProgress == 6) {
                    msg("TIP: you can cancel any in-progress units for a full refund by clicking their icon at the bottom.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 7) {
                    nextStageAfterSpace();
                }
            }
            case GATHER_WOOD -> {
                if (stageProgress == 0) {
                    msg("Now that we have a bunch of workers, we can start gathering some resources.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    msg("There's a forest over here that we can get wood from.");
                    OrthoviewClientEvents.forceMoveCam(WOOD_POS, 100);
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
                    msg("To gather a resource, select a worker, then RIGHT-CLICK a tree.");
                    setHelpButtonText("Select any villager, then RIGHT-CLICK any tree.");
                    progressStage();
                }
                else if (stageProgress == 3) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager &&
                                villager.getGatherResourceGoal().isGathering() &&
                                villager.getGatherResourceGoal().getTargetResourceName() == ResourceName.WOOD) {
                            specialMsg("Well done.");
                            clearHelpButtonText();
                            progressStageAfterDelay(60);
                        }
                    }
                }
                else if (stageProgress == 4) {
                    msg("TIP: Workers will keep gathering until told to do something else. " +
                        "Once they have at least 50 total resources they return it to the town centre.");
                    nextStageAfterDelay(100);
                }
            }
            case GATHER_ORE -> {
                if (stageProgress == 0) {
                    msg("Now let's do the same thing for ore.");
                    progressStageAfterDelay(80);
                }
                else if (stageProgress == 1) {
                    msg("Here's a beach with coal and iron we can get ore from.");
                    OrthoviewClientEvents.forceMoveCam(ORE_POS, 50);
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
                    msg("Select a worker, then RIGHT-CLICK an ore block.");
                    setHelpButtonText("Select any villager, then RIGHT-CLICK any ore block.");
                    progressStage();
                }
                else if (stageProgress == 3) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager &&
                            villager.getGatherResourceGoal().isGathering() &&
                            villager.getGatherResourceGoal().getTargetResourceName() == ResourceName.ORE) {
                            specialMsg("Well done.");
                            clearHelpButtonText();
                            progressStageAfterDelay(60);
                        }
                    }
                }
                else if (stageProgress == 4) {
                    msg("TIP: Sandy biomes like deserts and beaches have more and better ores.");
                    nextStageAfterDelay(100);
                }
            }
            case HUNT_ANIMALS -> {
                if (stageProgress == 0) {
                    msg("While your other workers are busy on wood and ore, let's find some food.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    foodBeforeHunting = ResourcesClientEvents.getOwnResources().food;
                    OrthoviewClientEvents.forceMoveCam(FOOD_POS, 50);
                    TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_ANIMALS);
                    msg("Here are some pigs that you can hunt for food.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
                    msg("Just like wood and ore, select a worker and right click a pig to start hunting it.");
                    setHelpButtonText("Select any villager, then RIGHT-CLICK a pig and wait for it to be killed." +
                                        "Make sure that villager doesn't already have a full inventory.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 3) {
                    msg("TIP: If your hunting villager has a full inventory of resources already, the porkchops will drop to the ground."+
                        "Any unit with a free inventory can pick up items like porkchops or saplings and return them for resources.");
                    progressStage();
                }
                else if (stageProgress == 4) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager) {
                            for (ItemStack itemStack : villager.getItems()) {
                                if (itemStack.getItem().equals(Items.PORKCHOP)) {
                                    specialMsg("Great work!");
                                    clearHelpButtonText();
                                    progressStageAfterDelay(60);
                                    break;
                                }
                            }
                        }
                    }
                }
                else if (stageProgress == 5) {
                    msg("Your villager should now return some porkchops to your town centre, but if they aren't, " +
                        "simply select the villager and RIGHT-CLICK your Town Centre.");
                    setHelpButtonText("Select your villager that hunted the pig and RIGHT-CLICK your Town Centre.");
                    progressStage();
                }
                else if (stageProgress == 6 && ResourcesClientEvents.getOwnResources().food >= foodBeforeHunting + 50) {
                    specialMsg("Excellent. You now have more food to build new units.");
                    clearHelpButtonText();
                    progressStageAfterDelay(80);
                }
                else if (stageProgress == 7) {
                    msg("TIP: Units hold up to 100 total resources, but hunting allows you to go above this maximum.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 8) {
                    msg("This might take a while, but animals offer a good amount of food for the effort.");
                    nextStageAfterDelay(100);
                }
            }
            case BUILD_BASE -> {
                if (stageProgress == 0) {
                    msg("Let's expand your base. Some new buildings have been unlocked for you. Select a worker if you " +
                        "haven't already to check them out.");
                    setHelpButtonText("Select any worker to check out your new building options");
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 2 && hasUnitSelected("villager")) {
                    clearHelpButtonText();
                    TutorialRendering.setButtonName(OakStockpile.buildingName);
                    msg("Stockpiles can give your workers a place to drop off resources faster.");
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 2 && hasUnitSelected("villager")) {
                    TutorialRendering.setButtonName(VillagerHouse.buildingName);
                    msg("Houses raise your max unit population. Check the top left for your current totals.");
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 3 && hasUnitSelected("villager")) {
                    TutorialRendering.setButtonName(WheatFarm.buildingName);
                    msg("Farms give you a slow but renewable source of food in exchange for wood.");
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 4 && hasUnitSelected("villager")) {
                    TutorialRendering.setButtonName(OakStockpile.buildingName);
                    msg("Finally, barracks let you start training soldiers to fight enemies.");
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 5) {
                    TutorialRendering.clearButtonName();
                    msg("Let's take some time to check out a few of these buildings.");
                    progressStageAfterDelay(80);
                }
                else if (stageProgress == 6) {
                    msg("When you're ready, build a barracks and get prepare to train your first army.");
                    setHelpButtonText("Select any villager and build a barracks building at the bottom right. Make sure " +
                                      "you've gathered enough wood to afford it!");
                    progressStage();
                }
                else if (stageProgress == 7) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building instanceof Barracks barracks && barracks.isBuilt) {
                            specialMsg("Great job.");
                            nextStage();
                        }
                    }
                }
            }
            case BUILD_ARMY -> {
                if (stageProgress == 0) {
                    msg("The barracks is one of many buildings (though the only one available in this tutorial) " +
                        "that can produce military units. Select your barracks to check them out.");
                    setHelpButtonText("Select your barracks to check out your military unit options");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2 && hasBuildingSelected(Barracks.buildingName)) {
                    clearHelpButtonText();
                    TutorialRendering.setButtonName(VindicatorProd.itemName);
                    msg("Vindicators are melee units with high health and moderate damage.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 3 && hasBuildingSelected(Barracks.buildingName)) {
                    TutorialRendering.setButtonName(PillagerProd.itemName);
                    msg("Pillagers are ranged units which attack slowly but with high damage.");
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 4) {
                    setHelpButtonText("Select your barracks and produce a total of 3 Pillagers and/or Vindicators. If you need more food " +
                                    "try building a farm or hunt more animals. If you need more population supply, try building a house.");
                    TutorialRendering.clearButtonName();
                    msg("Try building 3 units from here to continue.");
                    progressStage();
                }
                else if (stageProgress == 5) {
                    int armyCount = 0;
                    for (LivingEntity entity : UnitClientEvents.getAllUnits())
                        if (entity instanceof VindicatorUnit || entity instanceof PillagerUnit)
                            armyCount += 1;
                    if (armyCount >= 3) {
                        specialMsg("Awesome!");
                        nextStage();
                    }
                }
            }
            case DEFEND_BASE -> {
            }
            case DEFEND_BASE_AGAIN -> {

                // tip to protect villagers because they drop items and the fact you can raid enemy workers
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
