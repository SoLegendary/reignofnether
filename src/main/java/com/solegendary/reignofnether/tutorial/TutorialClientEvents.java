package com.solegendary.reignofnether.tutorial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.buildings.villagers.*;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.goals.MoveToTargetBlockGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonUnit;
import com.solegendary.reignofnether.unit.units.monsters.ZombieUnit;
import com.solegendary.reignofnether.unit.units.villagers.*;
import com.solegendary.reignofnether.util.Faction;
import com.solegendary.reignofnether.util.TextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
    private static int villagersHoldingFood = 0;

    private static final ArrayList<Building> damagedBuildings = new ArrayList<>();

    // all these positions are for camera only, actual spawn locations differ slightly on the serverside
    private static final Vec3i SPAWN_POS = new Vec3i(-2950, 0, -1166);
    public static final Vec3i BUILD_CAM_POS = new Vec3i(-2944, 0, -1200);
    public static final Vec3i BUILD_CAPITOL_POS = new Vec3i(-2936, 67, -1217);
    private static final Vec3i WOOD_POS = new Vec3i(-2919, 0, -1196);
    private static final Vec3i ORE_POS = new Vec3i(-2951, 0, -1224);
    private static final Vec3i FOOD_POS = new Vec3i(-2939, 0, -1173);

    private static final Vec3i MONSTER_CAMERA_POS = new Vec3i(-2983, 64, -1199);
    private static final Vec3i MONSTER_BASE_POS = new Vec3i(-3085, 72, -1277);

    private static final Vec3i BRIDGE_POS = new Vec3i(-2997,0,-1206);
    private static final Vec3i ARMY_POS = new Vec3i(-2963, 64, -1160);

    private static Supplier<Boolean> shouldPauseTicking = () -> false;

    private static int helpButtonClicks = 0;
    private static String helpButtonText = "";
    public static final Button helpButton = new Button(
            // TODO: make button names translatable
            "Tutorial Help",
            18,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/help.png"),
            (Keybinding) null,
            () -> false,
            () -> !isEnabled(),
            () -> isEnabled() && !helpButtonText.isEmpty(),
            () ->  {
                helpButtonClicks += 1;
                if (getStage() == INTRO)
                    specialMsg(helpButtonText);
                else
                    msg(helpButtonText, true, CHAT);
            },
            () -> { },
            List.of(FormattedCharSequence.forward(TextUtil.translateText("tutorial.client_events.help_button.tooltip"), Style.EMPTY))
    );

    public static void loadStage(TutorialStage stage) {
        if (stage == null || stage == getStage() || stage == INTRO || stage == COMPLETED)
            return;

        // TODO: make stage names translatable
        specialMsg(TextUtil.translateText("tutorial.client_events.load_stage.special_msg").formatted(stage.name().replace("_", " ")));

        tutorialStage = stage;
        ticksOnStage = 0;
        stageProgress = 0;
        blockUpdateStage = false;
        clearHelpButtonText();
        TutorialRendering.clearButtonName();
        updateStage();
        shouldPauseTicking = () -> false;
    }

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
            MC.player.sendSystemMessage(TextUtil.translate("tutorial.client_events.set_enabled.msg_0"));
            MC.player.sendSystemMessage(TextUtil.translate("tutorial.client_events.set_enabled.msg_1").withStyle(Style.EMPTY.withBold(true)));
            MC.player.sendSystemMessage(TextUtil.translate("tutorial.client_events.set_enabled.msg_2"));
            MC.player.sendSystemMessage(TextUtil.translate("tutorial.client_events.set_enabled.msg_3"));
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
        TutorialRendering.clearButtonName();
        updateStage();
        shouldPauseTicking = () -> false;
        TutorialServerboundPacket.saveStage(tutorialStage);
    }
    private static void prevStage() {
        tutorialStage = tutorialStage.prev();
        ticksOnStage = 0;
        stageProgress = 0;
        blockUpdateStage = false;
        clearHelpButtonText();
        TutorialRendering.clearButtonName();
        updateStage();
        shouldPauseTicking = () -> false;
        TutorialServerboundPacket.saveStage(tutorialStage);
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
        if (!OrthoviewClientEvents.isEnabled())
            return;

        if (Keybindings.ctrlMod.isDown() && Keybindings.altMod.isDown() && evt.getKeyCode() == GLFW.GLFW_KEY_SPACE) {
            nextStage();
            // TODO: make stage names translatable
            specialMsg(TextUtil.translateText("tutorial.client_events.on_key_press.special_msg").formatted(getStage().name()));
        }

        if (pressSpaceToContinue && evt.getKeyCode() == GLFW.GLFW_KEY_SPACE) {
            pressSpaceToContinue = false;
            nextStage();
        }

        if (pressSpaceToContinue && evt.getKeyCode() == GLFW.GLFW_KEY_SPACE) {
            pressSpaceToContinue = false;
            nextStage();
        }
    }

    @SubscribeEvent
    public static void TickEvent(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || MC.isPaused() || !isEnabled() || !OrthoviewClientEvents.isEnabled())
            return;

        if (shouldPauseTicking.get())
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

    /*
    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getPoseStack(), MC.font, new String[] {
                "stage: " + getStage(),
                "progress: " + stageProgress,
                "ticks: " + ticksOnStage
        });
    } */



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
        TutorialRendering.clearButtonName();
    }

    private static void nextStageAfterSpace() {
        blockUpdateStage = true;
        msg(TextUtil.translateText("tutorial.client_events.next_stage_after_space.msg"), true, CHAT);
        setHelpButtonText(TextUtil.translateText("tutorial.client_events.next_stage_after_space.help_button"));
        TutorialRendering.clearButtonName();
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
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    OrthoviewClientEvents.lockCam();
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.intro.msg_0"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.intro.msg_1"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 2) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.intro.msg_2"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.intro.help_button"));
                    TutorialRendering.setButtonName(helpButton.name);
                    progressStage();
                }
                else if (stageProgress == 3 && helpButtonClicks > 0) {
                    nextStageAfterDelay(120);
                }
            }
            case PAN_CAMERA -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    OrthoviewClientEvents.unlockCam();
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.pan_camera.msg"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.pan_camera.help_button"));
                    progressStage();
                }
                else if (stageProgress == 1 && pannedUp && pannedDown && pannedLeft && pannedRight) {
                    specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.pan_camera.special_msg"));
                    nextStageAfterDelay(100);
                }
            }
            case CAMERA_TIPS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.camera_tips.msg_0"));
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.camera_tips.msg_1"));
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 2) {
                    nextStageAfterSpace();
                }
            }
            case MINIMAP_CLICK -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.minimap_click.msg_0"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 1) {
                    clickedMinimap = false;
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.minimap_click.msg_1"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.minimap_click.help_button"));
                    progressStage();
                }
                else if (stageProgress == 2 && clickedMinimap) {
                    specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.minimap_click.special_msg"));
                    nextStageAfterDelay(100);
                }
            }
            case MINIMAP_TIPS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.minimap_tips.msg_0"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.minimap_tips.msg_1"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 2) {
                    nextStageAfterSpace();
                }
            }
            case PLACE_WORKERS_A -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.place_workers_a.msg_0"));
                    progressStageAfterDelay(120);
                } else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.place_workers_a.msg_1"));
                    OrthoviewClientEvents.forceMoveCam(SPAWN_POS, 50);
                    OrthoviewClientEvents.lockCam();
                    nextStageAfterDelay(100);
                }
            }
            case PLACE_WORKERS_B -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.place_workers_b.msg"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.place_workers_b.help_button"));
                    // TODO: make button names translatable
                    TutorialRendering.setButtonName("Villagers");
                    progressStage();
                }
                else if (stageProgress == 1 && PlayerClientEvents.isRTSPlayer) {
                    TutorialRendering.clearButtonName();
                    specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.place_workers_b.special_msg"));
                    OrthoviewClientEvents.unlockCam();
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    nextStageAfterDelay(100);
                }
            }
            case SELECT_UNIT -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.select_unit.msg_0"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.select_unit.msg_1"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.select_unit.help_button"));
                    UnitClientEvents.clearSelectedUnits();
                    progressStage();
                }
                else if (stageProgress == 2 && hasUnitSelected("villager")) {
                    nextStage();
                }
            }
            case MOVE_UNIT -> {
                if (stageProgress == 0 && hasUnitSelected("villager")) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.move_unit.msg"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.move_unit.help_button"));
                    progressStage();
                }
                else if (stageProgress == 1 && hasUnitSelected("villager")) {
                    Unit unit = (Unit) UnitClientEvents.getSelectedUnits().get(0);
                    MoveToTargetBlockGoal goal = unit.getMoveGoal();
                    if (goal != null && goal.getMoveTarget() != null) {
                        specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.move_unit.special_msg"));
                        nextStageAfterDelay(100);
                    }
                }
            }
            case BOX_SELECT_UNITS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.box_select_units.msg_0"));
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.box_select_units.msg_1"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.box_select_units.help_button"));
                    progressStage();
                }
                else if (stageProgress == 2 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    nextStage();
                }
            }
            case MOVE_UNITS -> {
                if (stageProgress == 0 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.move_units.msg"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.move_units.help_button"));
                    progressStage();
                }
                else if (stageProgress == 1 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    Unit unit = (Unit) UnitClientEvents.getSelectedUnits().get(1);
                    MoveToTargetBlockGoal goal = unit.getMoveGoal();
                    if (goal != null && goal.getMoveTarget() != null) {
                        specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.move_units.special_msg"));
                        nextStageAfterDelay(100);
                    }
                }
            }
            case UNIT_TIPS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.unit_tips.msg_0"));
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.unit_tips.msg_1"));
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 2) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.unit_tips.msg_2"));
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 3) {
                    nextStageAfterSpace();
                }
            }
            case BUILD_INTRO -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_intro.msg_0"));
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_intro.msg_1"));
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 2) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_intro.msg_2"));
                    OrthoviewClientEvents.forceMoveCam(BUILD_CAM_POS, 50);
                    progressStageAfterDelay(180);
                }
                else if (stageProgress == 3) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_intro.msg_3"));
                    nextStageAfterDelay(120);
                }
            }
            case BUILD_TOWN_CENTRE -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_town_centre.msg"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.build_town_centre.help_button"));
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
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.building_tips.msg_0"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.building_tips.help_button"));
                    progressStageAfterDelay(240);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.building_tips.msg_1"));
                    progressStage();
                }
                else if (stageProgress == 2 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre && townCentre.isBuilt) {
                        specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.building_tips.special_msg"));
                        nextStageAfterDelay(100);
                    }
                }
            } //msg("You can also set a rally point for your building with RIGHT-CLICK.");
            case TRAIN_WORKER -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.train_worker.msg_0"));
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.train_worker.msg_1"));
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 2) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.train_worker.msg_2"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.train_worker.help_button_0"));
                    progressStage();
                }
                else if (stageProgress == 3 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre &&
                        townCentre.getRallyPoint() != null) {
                        msg(TextUtil.translateText("tutorial.client_events.update_stage.train_worker.msg_3"));
                        setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.train_worker.help_button_1"));
                        TutorialRendering.setButtonName(VillagerProd.itemName);
                        progressStage();
                    }
                }
                else if (stageProgress == 4 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre &&
                            townCentre.productionQueue.size() > 0) {
                        msg(TextUtil.translateText("tutorial.client_events.update_stage.train_worker.msg_4"));
                        progressStage();
                    }
                }
                else if (stageProgress == 5 && UnitClientEvents.getAllUnits().size() > 3) {
                    specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.train_worker.special_msg"));
                    clearHelpButtonText();
                    TutorialRendering.clearButtonName();
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 6) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.train_worker.msg_5"));
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 7) {
                    nextStageAfterSpace();
                }
            }
            case GATHER_WOOD -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.gather_wood.msg_0"));
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.gather_wood.msg_1"));
                    OrthoviewClientEvents.forceMoveCam(WOOD_POS, 100);
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 2) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.gather_wood.msg_2"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.gather_wood.help_button"));
                    progressStage();
                }
                else if (stageProgress == 3) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager &&
                                villager.getGatherResourceGoal().isGathering() &&
                                villager.getGatherResourceGoal().getTargetResourceName() == ResourceName.WOOD) {
                            specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.gather_wood.special_msg"));
                            clearHelpButtonText();
                            progressStageAfterDelay(100);
                            break;
                        }
                    }
                }
                else if (stageProgress == 4) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.gather_wood.msg_3"));
                    nextStageAfterDelay(160);
                }
            }
            case GATHER_ORE -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.gather_ore.msg_0"));
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.gather_ore.msg_1"));
                    OrthoviewClientEvents.forceMoveCam(ORE_POS, 50);
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 2) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.gather_ore.msg_2"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.gather_ore.help_button"));
                    progressStage();
                }
                else if (stageProgress == 3) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager &&
                            villager.getGatherResourceGoal().isGathering() &&
                            villager.getGatherResourceGoal().getTargetResourceName() == ResourceName.ORE) {
                            specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.gather_ore.special_msg"));
                            clearHelpButtonText();
                            progressStageAfterDelay(100);
                            break;
                        }
                    }
                }
                else if (stageProgress == 4) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.gather_ore.msg_3"));
                    nextStageAfterDelay(120);
                }
            }
            case HUNT_ANIMALS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.hunt_animals.msg_0"));
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 1) {
                    OrthoviewClientEvents.forceMoveCam(FOOD_POS, 50);
                    TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_ANIMALS);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.hunt_animals.msg_1"));
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 2) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.hunt_animals.msg_2"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.hunt_animals.help_button_0"));
                    progressStageAfterDelay(180);
                }
                else if (stageProgress == 3) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.hunt_animals.msg_3"));
                    progressStage();
                }
                else if (stageProgress == 4) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager) {
                            LivingEntity targetEntity = villager.getTarget();
                            if (ResourceSources.isHuntableAnimal(targetEntity) &&
                                    targetEntity.getHealth() < targetEntity.getMaxHealth()) {
                                msg(TextUtil.translateText("tutorial.client_events.update_stage.hunt_animals.msg_4"));
                                progressStage();
                                break;
                            }
                        }
                    }
                }
                else if (stageProgress == 5) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager) {
                            LivingEntity targetEntity = villager.getTarget();
                            if (ResourceSources.isHuntableAnimal(targetEntity) &&
                                    targetEntity.getHealth() < targetEntity.getMaxHealth() / 2) {
                                msg(TextUtil.translateText("tutorial.client_events.update_stage.hunt_animals.msg_5"));
                                progressStage();
                                break;
                            }
                        }
                    }
                }
                else if (stageProgress == 6) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits())
                        if (entity instanceof VillagerUnit villager)
                            for (ItemStack itemStack : villager.getItems())
                                if (Resources.getTotalResourcesFromItems(List.of(itemStack)).food >= 100)
                                    villagersHoldingFood += 1;

                    if (villagersHoldingFood > 0) {
                        specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.hunt_animals.special_msg_0"));
                        clearHelpButtonText();
                        progressStageAfterDelay(100);
                    }
                }
                else if (stageProgress == 7) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.hunt_animals.msg_6"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.hunt_animals.help_button_1"));
                    progressStage();
                }
                else if (stageProgress == 8) {
                    int villagersHoldingFoodNow = 0;
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager) {
                            for (ItemStack itemStack : villager.getItems()) {
                                Resources res = Resources.getTotalResourcesFromItems(List.of(itemStack));
                                if (res.food > 0)
                                    villagersHoldingFoodNow += 1;
                            }
                        }
                    }
                    if (villagersHoldingFoodNow < villagersHoldingFood) {
                        specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.hunt_animals.special_msg_1"));
                        clearHelpButtonText();
                        progressStageAfterDelay(100);
                    }
                }
                else if (stageProgress == 9) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.hunt_animals.msg_7"));
                    nextStageAfterSpace();
                }
            }
            case EXPLAIN_BUILDINGS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.explain_buildings.msg_0"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.explain_buildings.help_button"));
                    shouldPauseTicking = () -> UnitClientEvents.getSelectedUnits().isEmpty() ||
                            !(UnitClientEvents.getSelectedUnits().get(0) instanceof VillagerUnit) ||
                            BuildingClientEvents.getBuildingToPlace() != null;
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 1 && hasUnitSelected("villager")) {
                    TutorialRendering.setButtonName(OakStockpile.buildingName);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.explain_buildings.msg_1"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 2 && hasUnitSelected("villager")) {
                    TutorialRendering.setButtonName(VillagerHouse.buildingName);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.explain_buildings.msg_2"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 3 && hasUnitSelected("villager")) {
                    TutorialRendering.setButtonName(WheatFarm.buildingName);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.explain_buildings.msg_3"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 4 && hasUnitSelected("villager")) {
                    clearHelpButtonText();
                    TutorialRendering.setButtonName(Barracks.buildingName);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.explain_buildings.msg_4"));
                    progressStageAfterDelay(160);
                }
                else {
                    nextStage();
                }
            }
            case BUILD_BASE -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    TutorialRendering.clearButtonName();
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_base.msg_0"));
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_base.msg_1"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.build_base.help_button"));
                    progressStage();
                }
                else if (stageProgress == 2) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building instanceof Barracks barracks && barracks.isBuilt) {
                            specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.build_base.special_msg"));
                            nextStageAfterDelay(100);
                            break;
                        }
                    }
                }
            }
            case EXPLAIN_BARRACKS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.explain_barracks.msg_0"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.explain_barracks.help_button"));
                    shouldPauseTicking = () -> BuildingClientEvents.getSelectedBuildings().isEmpty() ||
                            !(BuildingClientEvents.getSelectedBuildings().get(0) instanceof Barracks);
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 1 && hasBuildingSelected(Barracks.buildingName)) {
                    TutorialRendering.setButtonName(VindicatorProd.itemName);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.explain_barracks.msg_1"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 2 && hasBuildingSelected(Barracks.buildingName)) {
                    clearHelpButtonText();
                    TutorialRendering.setButtonName(PillagerProd.itemName);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.explain_barracks.msg_2"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 3) {
                    nextStage();
                }
            }
            case BUILD_ARMY -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.build_army.help_button"));
                    TutorialRendering.clearButtonName();
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_army.msg_0"));
                    progressStage();
                }
                else if (stageProgress == 1) {
                    int armyCount = 0;
                    for (LivingEntity entity : UnitClientEvents.getAllUnits())
                        if (entity instanceof VindicatorUnit || entity instanceof PillagerUnit)
                            armyCount += 1;
                    if (armyCount >= 3) {
                        specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.build_army.special_msg"));
                        progressStageAfterDelay(100);
                        TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_MONSTER_WORKERS);
                    }
                }
                else if (stageProgress == 2) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_army.msg_1"));
                    nextStageAfterDelay(200);
                    TutorialServerboundPacket.doServerAction(TutorialAction.START_MONSTER_BASE);
                }
            }
            case DEFEND_BASE -> {
                if (stageProgress == 0) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.defend_base.msg_0"));
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_NIGHT_TIME);
                    TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_MONSTERS_A);
                    OrthoviewClientEvents.forceMoveCam(MONSTER_CAMERA_POS, 50);
                    progressStageAfterDelay(120);
                }
                if (stageProgress == 1) {
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.defend_base.help_button"));
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.defend_base.msg_1"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 2) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.ATTACK_WITH_MONSTERS_A);
                    progressStage();
                }
                else if (stageProgress == 3) {
                    if (UnitClientEvents.getAllUnits().stream().filter(
                            u -> u instanceof PillagerUnit || u instanceof VindicatorUnit)
                            .toList().isEmpty()) {
                        progressStage();
                    }
                    if (UnitClientEvents.getAllUnits().stream().filter(
                            u -> u instanceof ZombieUnit || u instanceof SkeletonUnit)
                            .toList().isEmpty()) {
                        msg(TextUtil.translateText("tutorial.client_events.update_stage.defend_base.msg_2"));
                        TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_MONSTERS_B);
                        OrthoviewClientEvents.forceMoveCam(MONSTER_CAMERA_POS, 50);
                        progressStageAfterDelay(100);
                    }
                }
                else if (stageProgress == 4) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.ATTACK_WITH_MONSTERS_B);
                    progressStageAfterDelay(200);
                }
                else if (stageProgress == 5) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.EXPAND_MONSTER_BASE_A);
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.defend_base.special_msg_0"));
                    progressStage();
                }
                else if (stageProgress == 6) {
                    if (UnitClientEvents.getAllUnits().stream().filter(
                                    u -> u instanceof ZombieUnit || u instanceof SkeletonUnit)
                            .toList().isEmpty()) {
                        specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.defend_base.special_msg_1"));
                        clearHelpButtonText();
                        progressStageAfterDelay(100);
                    }
                }
                else if (stageProgress == 7) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.defend_base.msg_3"));
                    nextStageAfterDelay(200);
                }
            }
            case REPAIR_BUILDING -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.EXPAND_MONSTER_BASE_B);
                    for (Building building : BuildingClientEvents.getBuildings())
                        if (building.getHealth() < building.getMaxHealth() && building.getFaction() == Faction.VILLAGERS && damagedBuildings.size() < 3)
                            damagedBuildings.add(building);
                    if (damagedBuildings.isEmpty()) {
                        nextStage();
                    } else {
                        msg(TextUtil.translateText("tutorial.client_events.update_stage.repair_building.msg_0"));
                        progressStageAfterDelay(100);
                    }
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.repair_building.msg_1"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.repair_building.help_button"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 2) {
                    for (Building building : damagedBuildings) {
                        if (building.getHealth() >= building.getMaxHealth()) {
                            specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.repair_building.special_msg"));
                            clearHelpButtonText();
                            progressStageAfterDelay(100);
                            break;
                        }
                    }
                }
                else if (stageProgress == 3) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.repair_building.msg_2"));
                    progressStageAfterDelay(200);
                }
                else if (stageProgress == 4) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_MONSTER_BASE_ARMY);
                    nextStageAfterSpace();
                }
            }
            case BUILD_BRIDGE -> {
                if (stageProgress == 0) {
                    OrthoviewClientEvents.forceMoveCam(MONSTER_BASE_POS, 80);
                    if (FogOfWarClientEvents.isEnabled())
                        msg(TextUtil.translateText("tutorial.client_events.update_stage.build_bridge.msg_0"));
                    else
                        msg(TextUtil.translateText("tutorial.client_events.update_stage.build_bridge.msg_1"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_bridge.msg_2"));
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
                    OrthoviewClientEvents.forceMoveCam(BRIDGE_POS, 50);
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_bridge.msg_3"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.build_bridge.help_button"));
                    progressStage();
                }
                else if (stageProgress == 3) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building instanceof OakBridge bridge) {
                            msg(TextUtil.translateText("tutorial.client_events.update_stage.build_bridge.msg_4"));
                            progressStage();
                            break;
                        }
                    }
                }
                else if (stageProgress == 4) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building instanceof OakBridge bridge && bridge.isBuilt) {
                            specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.build_bridge.special_msg"));
                            progressStageAfterDelay(100);
                            break;
                        }
                    }
                }
                else if (stageProgress == 5) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_bridge.msg_5"));
                    progressStageAfterDelay(200);
                }
                else if (stageProgress == 6) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.build_bridge.msg_6"));
                    progressStageAfterDelay(200);
                }
                else if (stageProgress == 7) {
                    nextStage();
                }
            }
            case ATTACK_ENEMY_BASE -> {
                if (stageProgress == 0) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.attack_enemy_base.msg_0"));
                    TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_FRIENDLY_ARMY);
                    OrthoviewClientEvents.forceMoveCam(ARMY_POS, 50);
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.attack_enemy_base.msg_1"));
                    setHelpButtonText(TextUtil.translateText("tutorial.client_events.update_stage.attack_enemy_base.help_button"));
                    progressStageAfterDelay(200);
                }
                else if (stageProgress == 2) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.attack_enemy_base.msg_2"));
                    progressStageAfterDelay(200);
                }
                else if (stageProgress == 3) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building.getFaction() == Faction.MONSTERS && building.getHealth() < building.getMaxHealth()) {
                            msg(TextUtil.translateText("tutorial.client_events.update_stage.attack_enemy_base.msg_3"));
                            progressStage();
                            break;
                        }
                    }
                }
                else if (stageProgress == 4) {
                    boolean botAlive = false;
                    for (Building building : BuildingClientEvents.getBuildings())
                        if (building.getFaction() == Faction.MONSTERS)
                            botAlive = true;

                    if (!botAlive) {
                        // should show the standard victory screen
                        progressStageAfterDelay(200);
                    }
                }
                else if (stageProgress == 5) {
                    nextStageAfterSpace();
                }
            }
            case OUTRO -> {
                if (stageProgress == 0) {
                    specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.attack_enemy_base.special_msg"));
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.attack_enemy_base.msg_4"));
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 2) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.attack_enemy_base.msg_5"));
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 3) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.attack_enemy_base.msg_6"));
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 4) {
                    msg(TextUtil.translateText("tutorial.client_events.update_stage.attack_enemy_base.msg_7"));
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 5) {
                    specialMsg(TextUtil.translateText("tutorial.client_events.update_stage.attack_enemy_base.msg_8"));
                    nextStage();
                }
            }
            case COMPLETED -> {
                if (stageProgress == 0) {
                    setEnabled(false);
                    progressStage();
                }
            }
        }
    }
}
