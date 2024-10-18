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

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;// I18n

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
            Component.translatable("tutorial.help_button").getString(),
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
            List.of(FormattedCharSequence.forward(Component.translatable("tutorial.help_button").getString(), Style.EMPTY)) // 本地化按钮提示文本
        );

    public static void loadStage(TutorialStage stage) {
        if (stage == null || stage == getStage() || stage == INTRO || stage == COMPLETED)
            return;

        specialMsg(Component.translatable("tutorial.resuming_stage", stage.name().replace("_", " ")).getString());

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
        MC.player.sendSystemMessage(Component.literal(""));
        MC.player.sendSystemMessage(Component.translatable("tutorial.welcome_message").withStyle(Style.EMPTY.withBold(true)));
        MC.player.sendSystemMessage(Component.translatable("tutorial.press_f12"));
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
            MutableComponent messageComponent = Component.translatable("tutorial.skip_message", getStage().name());
            specialMsg(messageComponent.getString());
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
        String localizedMsg = Component.translatable("tutorial.press_space_continue").getString();
        msg(localizedMsg, true, CHAT);
        
        setHelpButtonText(localizedMsg);
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
                    msg(Component.translatable("tutorial.welcome_rts").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.tip_chat").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 2) {
                    msg(Component.translatable("tutorial.help_reminder").getString());
                    setHelpButtonText(Component.translatable("tutorial.help_button_clicked").getString());
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
                    msg(Component.translatable("tutorial.camera_move").getString());
                    setHelpButtonText(Component.translatable("tutorial.help_camera_move").getString());
                    progressStage();
                }
                else if (stageProgress == 1 && pannedUp && pannedDown && pannedLeft && pannedRight) {
                    specialMsg(Component.translatable("tutorial.nicely_done").getString());
                    nextStageAfterDelay(100);
                }
            }
            case CAMERA_TIPS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.camera_tips.move_with_arrows").getString());
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.camera_tips.zoom_rotate").getString());
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 2) {
                    nextStageAfterSpace();
                }
            }
            case MINIMAP_CLICK -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.minimap_click.introduction").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 1) {
                    clickedMinimap = false;
                    msg(Component.translatable("tutorial.minimap_click.move_camera").getString());
                    setHelpButtonText(Component.translatable("tutorial.minimap_click.help_text").getString());
                    progressStage();
                }
                else if (stageProgress == 2 && clickedMinimap) {
                    specialMsg(Component.translatable("tutorial.minimap_click.good_work").getString());
                    nextStageAfterDelay(100);
                }
            }
            case MINIMAP_TIPS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.minimap_tips.recentre").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.minimap_tips.expand").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 2) {
                    nextStageAfterSpace();
                }
            }
            case PLACE_WORKERS_A -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.place_workers_a.start_units").getString());
                    progressStageAfterDelay(120);
                } else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.place_workers_a.spawn_villagers").getString());
                    OrthoviewClientEvents.forceMoveCam(SPAWN_POS, 50);
                    OrthoviewClientEvents.lockCam();
                    nextStageAfterDelay(100);
                }
            }
            case PLACE_WORKERS_B -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.place_workers_b.place_villagers").getString());
                    setHelpButtonText(Component.translatable("tutorial.place_workers_b.place_help").getString());
                    TutorialRendering.setButtonName("Villagers");
                    progressStage();
                }
                else if (stageProgress == 1 && PlayerClientEvents.isRTSPlayer) {
                    TutorialRendering.clearButtonName();
                    specialMsg(Component.translatable("tutorial.place_workers_b.excellent").getString());
                    OrthoviewClientEvents.unlockCam();
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    nextStageAfterDelay(100);
                }
            }
            case SELECT_UNIT -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.select_unit.introduction").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.select_unit.select_hint").getString());
                    setHelpButtonText(Component.translatable("tutorial.select_unit.select_help").getString());
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
                    msg(Component.translatable("tutorial.move_unit.right_click").getString());
                    setHelpButtonText(Component.translatable("tutorial.move_unit.help").getString());
                    progressStage();
                }
                else if (stageProgress == 1 && hasUnitSelected("villager")) {
                    Unit unit = (Unit) UnitClientEvents.getSelectedUnits().get(0);
                    MoveToTargetBlockGoal goal = unit.getMoveGoal();
                    if (goal != null && goal.getMoveTarget() != null) {
                        specialMsg(Component.translatable("tutorial.move_unit.nice_work").getString());
                        nextStageAfterDelay(100);
                    }
                }
            }
            case BOX_SELECT_UNITS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.box_select_units.introduction").getString());
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.box_select_units.selection_hint").getString());
                    setHelpButtonText(Component.translatable("tutorial.box_select_units.help").getString());
                    progressStage();
                }
                else if (stageProgress == 2 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    nextStage();
                }
            }
            case MOVE_UNITS -> {
                if (stageProgress == 0 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.move_units.right_click").getString());
                    setHelpButtonText(Component.translatable("tutorial.move_units.help").getString());
                    progressStage();
                }
                else if (stageProgress == 1 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    Unit unit = (Unit) UnitClientEvents.getSelectedUnits().get(1);
                    MoveToTargetBlockGoal goal = unit.getMoveGoal();
                    if (goal != null && goal.getMoveTarget() != null) {
                        specialMsg(Component.translatable("tutorial.move_units.great_job").getString());
                        nextStageAfterDelay(100);
                    }
                }
            }
            case UNIT_TIPS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.unit_tips.double_click").getString());
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.unit_tips.deselect").getString());
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 2) {
                    msg(Component.translatable("tutorial.unit_tips.control_grouping").getString());
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 3) {
                    nextStageAfterSpace();
                }
            }
            case BUILD_INTRO -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.build_intro.start_base").getString());
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.build_intro.first_building").getString());
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 2) {
                    msg(Component.translatable("tutorial.build_intro.good_spot").getString());
                    OrthoviewClientEvents.forceMoveCam(BUILD_CAM_POS, 50);
                    progressStageAfterDelay(180);
                }
                else if (stageProgress == 3) {
                    msg(Component.translatable("tutorial.build_intro.resources").getString());
                    nextStageAfterDelay(120);
                }
            }
            case BUILD_TOWN_CENTRE -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.build_town_centre.select_workers").getString());
                    setHelpButtonText(Component.translatable("tutorial.build_town_centre.help").getString());
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
                    msg(Component.translatable("tutorial.building_tips.villagers_help").getString());
                    setHelpButtonText(Component.translatable("tutorial.building_tips.help").getString());
                    progressStageAfterDelay(240);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.building_tips.select_building").getString());
                    progressStage();
                }
                else if (stageProgress == 2 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre && townCentre.isBuilt) {
                        specialMsg(Component.translatable("tutorial.building_tips.congratulations").getString());
                        nextStageAfterDelay(100);
                    }
                }
            }
            case TRAIN_WORKER -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.train_worker.capitol_info").getString());
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.train_worker.food_cost").getString());
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 2) {
                    msg(Component.translatable("tutorial.train_worker.rally_point").getString());
                    setHelpButtonText(Component.translatable("tutorial.train_worker.rally_help").getString());
                    progressStage();
                }
                else if (stageProgress == 3 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre && townCentre.getRallyPoint() != null) {
                        msg(Component.translatable("tutorial.train_worker.villager_production").getString());
                        setHelpButtonText(Component.translatable("tutorial.train_worker.villager_help").getString());
                        TutorialRendering.setButtonName(VillagerProd.itemName);
                        progressStage();
                    }
                }
                else if (stageProgress == 4 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre && townCentre.productionQueue.size() > 0) {
                        msg(Component.translatable("tutorial.train_worker.queue_tip").getString());
                        progressStage();
                    }
                }
                else if (stageProgress == 5 && UnitClientEvents.getAllUnits().size() > 3) {
                    specialMsg(Component.translatable("tutorial.train_worker.worker_advice").getString());
                    clearHelpButtonText();
                    TutorialRendering.clearButtonName();
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 6) {
                    msg(Component.translatable("tutorial.train_worker.cancel_tip").getString());
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 7) {
                    nextStageAfterSpace();
                }
            }
            case GATHER_WOOD -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.gather_wood.introduction").getString());
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.gather_wood.forest").getString());
                    OrthoviewClientEvents.forceMoveCam(WOOD_POS, 100);
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 2) {
                    msg(Component.translatable("tutorial.gather_wood.gather_instruction").getString());
                    setHelpButtonText(Component.translatable("tutorial.gather_wood.gather_instruction").getString());
                    progressStage();
                }
                else if (stageProgress == 3) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager &&
                            villager.getGatherResourceGoal().isGathering() &&
                            villager.getGatherResourceGoal().getTargetResourceName() == ResourceName.WOOD) {
                            specialMsg(Component.translatable("tutorial.gather_wood.success").getString());
                            clearHelpButtonText();
                            progressStageAfterDelay(100);
                            break;
                        }
                    }
                }
                else if (stageProgress == 4) {
                    msg(Component.translatable("tutorial.gather_wood.tip").getString());
                    nextStageAfterDelay(160);
                }
            }
            case GATHER_ORE -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.gather_ore.introduction").getString());
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.gather_ore.beach").getString());
                    OrthoviewClientEvents.forceMoveCam(ORE_POS, 50);
                    progressStage();
                }
                else if (stageProgress == 2) {
                    msg(Component.translatable("tutorial.gather_ore.gather_instruction").getString());
                    setHelpButtonText(Component.translatable("tutorial.gather_ore.gather_instruction").getString());
                    progressStage();
                }
                else if (stageProgress == 3) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager &&
                            villager.getGatherResourceGoal().isGathering() &&
                            villager.getGatherResourceGoal().getTargetResourceName() == ResourceName.ORE) {
                            specialMsg(Component.translatable("tutorial.gather_ore.success").getString());
                            clearHelpButtonText();
                            progressStageAfterDelay(100);
                            break;
                        }
                    }
                }
                else if (stageProgress == 4) {
                    msg(Component.translatable("tutorial.gather_ore.tip").getString());
                    nextStageAfterDelay(120);
                }
            }
            case HUNT_ANIMALS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.hunt_animals.introduction").getString());
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 1) {
                    OrthoviewClientEvents.forceMoveCam(FOOD_POS, 50);
                    TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_ANIMALS);
                    msg(Component.translatable("tutorial.hunt_animals.pigs").getString());
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 2) {
                    msg(Component.translatable("tutorial.hunt_animals.gather_instruction").getString());
                    setHelpButtonText(Component.translatable("tutorial.hunt_animals.help").getString());
                    progressStageAfterDelay(180);
                }
                else if (stageProgress == 3) {
                    msg(Component.translatable("tutorial.hunt_animals.worker_warning").getString());
                    progressStage();
                }
                else if (stageProgress == 4) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager) {
                            LivingEntity targetEntity = villager.getTarget();
                            if (ResourceSources.isHuntableAnimal(targetEntity) && targetEntity.getHealth() < targetEntity.getMaxHealth()) {
                                msg(Component.translatable("tutorial.hunt_animals.hold_warning").getString());
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
                            if (ResourceSources.isHuntableAnimal(targetEntity) && targetEntity.getHealth() < targetEntity.getMaxHealth() / 2) {
                                msg(Component.translatable("tutorial.hunt_animals.dropped_items").getString());
                                progressStage();
                                break;
                            }
                        }
                    }
                }
                else if (stageProgress == 6) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager) {
                            for (ItemStack itemStack : villager.getItems()) {
                                if (Resources.getTotalResourcesFromItems(List.of(itemStack)).food >= 100) {
                                    villagersHoldingFood += 1;
                                }
                            }
                        }
                    }
                    if (villagersHoldingFood > 0) {
                        specialMsg(Component.translatable("tutorial.hunt_animals.great_work").getString());
                        clearHelpButtonText();
                        progressStageAfterDelay(100);
                    }
                }
                else if (stageProgress == 7) {
                    msg(Component.translatable("tutorial.hunt_animals.return_food").getString());
                    setHelpButtonText(Component.translatable("tutorial.hunt_animals.return_help").getString());
                    progressStage();
                }
                else if (stageProgress == 8) {
                    int villagersHoldingFoodNow = 0;
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager) {
                            for (ItemStack itemStack : villager.getItems()) {
                                Resources res = Resources.getTotalResourcesFromItems(List.of(itemStack));
                                if (res.food > 0) villagersHoldingFoodNow += 1;
                            }
                        }
                    }
                    if (villagersHoldingFoodNow < villagersHoldingFood) {
                        specialMsg(Component.translatable("tutorial.hunt_animals.excellent").getString());
                        clearHelpButtonText();
                        progressStageAfterDelay(100);
                    }
                }
                else if (stageProgress == 9) {
                    msg(Component.translatable("tutorial.hunt_animals.food_warning").getString());
                    nextStageAfterSpace();
                }
            }
            case EXPLAIN_BUILDINGS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.explain_buildings.introduction").getString());
                    setHelpButtonText(Component.translatable("tutorial.explain_buildings.help").getString());
                    shouldPauseTicking = () -> UnitClientEvents.getSelectedUnits().isEmpty() ||
                                            !(UnitClientEvents.getSelectedUnits().get(0) instanceof VillagerUnit) ||
                                            BuildingClientEvents.getBuildingToPlace() != null;
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 1 && hasUnitSelected("villager")) {
                    TutorialRendering.setButtonName(OakStockpile.buildingName);
                    msg(Component.translatable("tutorial.explain_buildings.stockpile").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 2 && hasUnitSelected("villager")) {
                    TutorialRendering.setButtonName(VillagerHouse.buildingName);
                    msg(Component.translatable("tutorial.explain_buildings.house").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 3 && hasUnitSelected("villager")) {
                    TutorialRendering.setButtonName(WheatFarm.buildingName);
                    msg(Component.translatable("tutorial.explain_buildings.farm").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 4 && hasUnitSelected("villager")) {
                    clearHelpButtonText();
                    TutorialRendering.setButtonName(Barracks.buildingName);
                    msg(Component.translatable("tutorial.explain_buildings.barracks").getString());
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
                    msg(Component.translatable("tutorial.build_base.introduction").getString());
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.build_base.barracks_info").getString());
                    setHelpButtonText(Component.translatable("tutorial.build_base.barracks_help").getString());
                    progressStage();
                }
                else if (stageProgress == 2) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building instanceof Barracks barracks && barracks.isBuilt) {
                            specialMsg(Component.translatable("tutorial.build_base.great_job").getString());
                            nextStageAfterDelay(100);
                            break;
                        }
                    }
                }
            }
            case EXPLAIN_BARRACKS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(Component.translatable("tutorial.explain_barracks.introduction").getString());
                    setHelpButtonText(Component.translatable("tutorial.explain_barracks.help").getString());
                    shouldPauseTicking = () -> BuildingClientEvents.getSelectedBuildings().isEmpty() ||
                                            !(BuildingClientEvents.getSelectedBuildings().get(0) instanceof Barracks);
                    progressStageAfterDelay(140);
                }
                else if (stageProgress == 1 && hasBuildingSelected(Barracks.buildingName)) {
                    TutorialRendering.setButtonName(VindicatorProd.itemName);
                    msg(Component.translatable("tutorial.explain_barracks.vindicator").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 2 && hasBuildingSelected(Barracks.buildingName)) {
                    clearHelpButtonText();
                    TutorialRendering.setButtonName(PillagerProd.itemName);
                    msg(Component.translatable("tutorial.explain_barracks.pillager").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 3) {
                    nextStage();
                }
            }
            case BUILD_ARMY -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    setHelpButtonText(Component.translatable("tutorial.build_army.introduction").getString());
                    TutorialRendering.clearButtonName();
                    msg(Component.translatable("tutorial.build_army.try_build").getString());
                    progressStage();
                }
                else if (stageProgress == 1) {
                    int armyCount = 0;
                    for (LivingEntity entity : UnitClientEvents.getAllUnits())
                        if (entity instanceof VindicatorUnit || entity instanceof PillagerUnit)
                            armyCount += 1;
                    if (armyCount >= 3) {
                        specialMsg(Component.translatable("tutorial.build_army.awesome").getString());
                        progressStageAfterDelay(100);
                        TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_MONSTER_WORKERS);
                    }
                }
                else if (stageProgress == 2) {
                    msg(Component.translatable("tutorial.build_army.tip").getString());
                    nextStageAfterDelay(200);
                    TutorialServerboundPacket.doServerAction(TutorialAction.START_MONSTER_BASE);
                }
            }
            case DEFEND_BASE -> {
                if (stageProgress == 0) {
                    msg(Component.translatable("tutorial.defend_base.introduction").getString());
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_NIGHT_TIME);
                    TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_MONSTERS_A);
                    OrthoviewClientEvents.forceMoveCam(MONSTER_CAMERA_POS, 50);
                    progressStageAfterDelay(120);
                }
                if (stageProgress == 1) {
                    setHelpButtonText(Component.translatable("tutorial.defend_base.help").getString());
                    msg(Component.translatable("tutorial.defend_base.help").getString());
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
                        msg(Component.translatable("tutorial.defend_base.monster_warning").getString());
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
                    specialMsg(Component.translatable("tutorial.defend_base.night_time").getString());
                    progressStage();
                }
                else if (stageProgress == 6) {
                    if (UnitClientEvents.getAllUnits().stream().filter(
                                    u -> u instanceof ZombieUnit || u instanceof SkeletonUnit)
                            .toList().isEmpty()) {
                        specialMsg(Component.translatable("tutorial.defend_base.success").getString());
                        clearHelpButtonText();
                        progressStageAfterDelay(100);
                    }
                }
                else if (stageProgress == 7) {
                    msg(Component.translatable("tutorial.defend_base.worker_tip").getString());
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
                        msg(Component.translatable("tutorial.repair_building.introduction").getString());
                        progressStageAfterDelay(100);
                    }
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.repair_building.repair_instruction").getString());
                    setHelpButtonText(Component.translatable("tutorial.repair_building.help").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 2) {
                    for (Building building : damagedBuildings) {
                        if (building.getHealth() >= building.getMaxHealth()) {
                            specialMsg(Component.translatable("tutorial.repair_building.good_job").getString());
                            clearHelpButtonText();
                            progressStageAfterDelay(100);
                            break;
                        }
                    }
                }
                else if (stageProgress == 3) {
                    msg(Component.translatable("tutorial.repair_building.health_tip").getString());
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
                        msg(Component.translatable("tutorial.build_bridge.introduction").getString() + " (but you can't see it yet because fog of war is enabled).");
                    else
                        msg(Component.translatable("tutorial.build_bridge.introduction").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.build_bridge.cross_instruction").getString());
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 2) {
                    OrthoviewClientEvents.forceMoveCam(BRIDGE_POS, 50);
                    msg(Component.translatable("tutorial.build_bridge.spot_instruction").getString());
                    setHelpButtonText(Component.translatable("tutorial.build_bridge.help").getString());
                    progressStage();
                }
                else if (stageProgress == 3) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building instanceof OakBridge bridge) {
                            msg(Component.translatable("tutorial.build_bridge.bridge_tip").getString());
                            progressStage();
                            break;
                        }
                    }
                }
                else if (stageProgress == 4) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building instanceof OakBridge bridge && bridge.isBuilt) {
                            specialMsg(Component.translatable("tutorial.build_bridge.nice_job").getString());
                            progressStageAfterDelay(100);
                            break;
                        }
                    }
                }
                else if (stageProgress == 5) {
                    msg(Component.translatable("tutorial.build_bridge.bridge_neutral_tip").getString());
                    progressStageAfterDelay(200);
                }
                else if (stageProgress == 6) {
                    msg(Component.translatable("tutorial.build_bridge.crossing_caution").getString());
                    progressStageAfterDelay(200);
                }
                else if (stageProgress == 7) {
                    nextStage();
                }
            }
            case ATTACK_ENEMY_BASE -> {
                if (stageProgress == 0) {
                    msg(Component.translatable("tutorial.attack_enemy_base.reinforcements").getString());
                    TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_FRIENDLY_ARMY);
                    OrthoviewClientEvents.forceMoveCam(ARMY_POS, 50);
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.attack_enemy_base.iron_golem").getString());
                    setHelpButtonText(Component.translatable("tutorial.attack_enemy_base.help").getString());
                    progressStageAfterDelay(200);
                }
                else if (stageProgress == 2) {
                    msg(Component.translatable("tutorial.attack_enemy_base.ranged_units_tip").getString());
                    progressStageAfterDelay(200);
                }
                else if (stageProgress == 3) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building.getFaction() == Faction.MONSTERS && building.getHealth() < building.getMaxHealth()) {
                            msg(Component.translatable("tutorial.attack_enemy_base.mausoleum_tip").getString());
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
                        specialMsg(Component.translatable("tutorial.attack_enemy_base.victory").getString());
                        progressStageAfterDelay(200);
                    }
                }
                else if (stageProgress == 5) {
                    nextStageAfterSpace();
                }
            }
            case OUTRO -> {
                if (stageProgress == 0) {
                    specialMsg(Component.translatable("tutorial.outro.congratulations").getString());
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 1) {
                    msg(Component.translatable("tutorial.outro.unlocks").getString());
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 2) {
                    msg(Component.translatable("tutorial.outro.reset").getString());
                    progressStageAfterDelay(120);
                }
                else if (stageProgress == 3) {
                    msg(Component.translatable("tutorial.outro.server_guide").getString());
                    progressStageAfterDelay(160);
                }
                else if (stageProgress == 4) {
                    msg(Component.translatable("tutorial.outro.good_luck").getString());
                    progressStageAfterDelay(100);
                }
                else if (stageProgress == 5) {
                    specialMsg(Component.translatable("tutorial.outro.mode_disabled").getString());
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
