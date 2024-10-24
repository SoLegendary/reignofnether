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
import net.minecraft.client.resources.language.I18n;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.registrars.SoundRegistrar.ALLY;
import static com.solegendary.reignofnether.registrars.SoundRegistrar.CHAT;
import static com.solegendary.reignofnether.tutorial.TutorialStage.*;

public class TutorialClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
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

    private static final Vec3i BRIDGE_POS = new Vec3i(-2997, 0, -1206);
    private static final Vec3i ARMY_POS = new Vec3i(-2963, 64, -1160);

    private static Supplier<Boolean> shouldPauseTicking = () -> false;

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
        () -> {
            helpButtonClicks += 1;
            if (getStage() == INTRO) {
                specialMsg(helpButtonText);
            } else {
                msg(helpButtonText, true, CHAT);
            }
        },
        () -> {
        },
        List.of(FormattedCharSequence.forward(I18n.get("tutorial.reignofnether.tutorial_help"), Style.EMPTY))
    );

    public static void loadStage(TutorialStage stage) {
        if (stage == null || stage == getStage() || stage == INTRO || stage == COMPLETED) {
            return;
        }

        specialMsg("tutorial.reignofnether.resuming", stage.name().replace("_", " "));

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
        helpButtonText = I18n.get(text);
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
            MC.player.sendSystemMessage(Component.translatable("Welcome to the Reign of Nether Tutorial!")
                .withStyle(Style.EMPTY.withBold(true)));
            MC.player.sendSystemMessage(Component.translatable("tutorial.reignofnether.get_started"));
            MC.player.sendSystemMessage(Component.literal(""));
        }
        enabled = value;
    }

    public static TutorialStage getStage() {
        return tutorialStage;
    }

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
            if (!pannedUp) {
                TutorialRendering.pointAtWithArrow(poseStack, MC.screen.width / 2, -Button.iconFrameSize, true);
            }
            if (!pannedDown) {
                TutorialRendering.pointAtWithArrow(poseStack, MC.screen.width / 2, MC.screen.height, true);
            }
            if (!pannedLeft) {
                TutorialRendering.pointAtWithArrow(poseStack, -Button.iconFrameSize, MC.screen.height / 2, false);
            }
            if (!pannedRight) {
                TutorialRendering.pointAtWithArrow(poseStack, MC.screen.width, MC.screen.height / 2, false);
            }
        } else {
            TutorialRendering.highlightNextButton(poseStack, buttons);
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre evt) {
        updateStage();
    }

    @SubscribeEvent
    public static void onKeyRelease(ScreenEvent.KeyReleased.Pre evt) {
        updateStage();
    }

    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Pre evt) {
        if (!OrthoviewClientEvents.isEnabled()) {
            return;
        }

        if (Keybindings.ctrlMod.isDown() && Keybindings.altMod.isDown() && evt.getKeyCode() == GLFW.GLFW_KEY_SPACE) {
            nextStage();
            specialMsg("tutorial.reignofnether.skipping", getStage().name());
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
        if (evt.phase != TickEvent.Phase.END || MC.isPaused() || !isEnabled() || !OrthoviewClientEvents.isEnabled()) {
            return;
        }

        if (shouldPauseTicking.get()) {
            return;
        }

        if (ticksOnStage < Integer.MAX_VALUE) {
            if (ticksOnStage % 20 == 0) {
                updateStage();
            }
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
            if (ticksToNextStage == 0) {
                nextStage();
            }
        }
    }

    private static void msg(String msg, boolean bold, RegistryObject<SoundEvent> soundEvt, Object... params) {
        if (MC.player == null) {
            return;
        }
        MC.player.playSound(soundEvt.get(), 1.2f, 1.0f);
        MC.player.sendSystemMessage(Component.literal(""));
        if (bold) {
            MC.player.sendSystemMessage(Component.translatable(msg, params).withStyle(Style.EMPTY.withBold(true)));
        } else {
            MC.player.sendSystemMessage(Component.translatable(msg, params));
        }
        MC.player.sendSystemMessage(Component.literal(""));
    }

    private static void msg(String msg, Object... params) {
        msg(msg, false, CHAT, params);
    }

    private static void specialMsg(String msg, Object... params) {
        msg(msg, true, ALLY, params);
    }


    public static boolean isAtOrPastStage(TutorialStage stage) {
        if (!isEnabled()) {
            return true;
        }
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
        msg("tutorial.reignofnether.ready_continue", true, CHAT);
        setHelpButtonText("tutorial.reignofnether.ready_continue");
        TutorialRendering.clearButtonName();
        pressSpaceToContinue = true;
    }

    private static boolean hasUnitSelected(String unitName) {
        return UnitClientEvents.getSelectedUnits().size() > 0 && UnitClientEvents.getSelectedUnits()
            .get(0)
            .getName()
            .getString()
            .toLowerCase()
            .contains(unitName.toLowerCase());
    }

    private static boolean hasBuildingSelected(String buildingName) {
        return BuildingClientEvents.getSelectedBuildings().size() > 0 && BuildingClientEvents.getSelectedBuildings()
            .get(0).name.equalsIgnoreCase(buildingName);
    }

    // Whenever doing anything that could be a tutorial action like enabling orthoview or building your first building,
    // check here to progress the tutorial and give updates to the player.
    // This theoretically should be able to be called at any time but will only actually progress if actions are done
    // This should be called in specific clientside events like
    public static void updateStage() {
        if (!isEnabled() || blockUpdateStage) {
            return;
        }

        switch (tutorialStage) {
            case INTRO -> {
                if (stageProgress == 0 && OrthoviewClientEvents.isEnabled()) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    OrthoviewClientEvents.lockCam();
                    msg("tutorial.reignofnether.rts_view");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 1) {
                    msg("tutorial.reignofnether.see_again");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 2) {
                    msg("tutorial.reignofnether.reminder");
                    setHelpButtonText("tutorial.reignofnether.need_click");
                    TutorialRendering.setButtonName(helpButton.name);
                    progressStage();
                } else if (stageProgress == 3 && helpButtonClicks > 0) {
                    nextStageAfterDelay(120);
                }
            }
            case PAN_CAMERA -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    OrthoviewClientEvents.unlockCam();
                    msg("tutorial.reignofnether.move_camera");
                    setHelpButtonText("tutorial.reignofnether.move_camera2");
                    progressStage();
                } else if (stageProgress == 1 && pannedUp && pannedDown && pannedLeft && pannedRight) {
                    specialMsg("tutorial.reignofnether.nicely_done");
                    nextStageAfterDelay(100);
                }
            }
            case CAMERA_TIPS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.tip.arrow_keys");
                    progressStageAfterDelay(140);
                } else if (stageProgress == 1) {
                    msg("tutorial.reignofnether.tip.zoom");
                    progressStageAfterDelay(140);
                } else if (stageProgress == 2) {
                    nextStageAfterSpace();
                }
            }
            case MINIMAP_CLICK -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.minimap");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 1) {
                    clickedMinimap = false;
                    msg("tutorial.reignofnether.map_click");
                    setHelpButtonText("tutorial.reignofnether.map_click2");
                    progressStage();
                } else if (stageProgress == 2 && clickedMinimap) {
                    specialMsg("tutorial.reignofnether.good_work");
                    nextStageAfterDelay(100);
                }
            }
            case MINIMAP_TIPS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.recenter");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 1) {
                    msg("tutorial.reignofnether.tip.expand_map");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 2) {
                    nextStageAfterSpace();
                }
            }
            case PLACE_WORKERS_A -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.playing_units");
                    progressStageAfterDelay(120);
                } else if (stageProgress == 1) {
                    msg("tutorial.reignofnether.spawn_villagers");
                    OrthoviewClientEvents.forceMoveCam(SPAWN_POS, 50);
                    OrthoviewClientEvents.lockCam();
                    nextStageAfterDelay(100);
                }
            }
            case PLACE_WORKERS_B -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.spawn_villagers2");
                    setHelpButtonText("tutorial.reignofnether.spawn_villagers2");
                    TutorialRendering.setButtonName("tutorial.reignofnether.villagers");
                    progressStage();
                } else if (stageProgress == 1 && PlayerClientEvents.isRTSPlayer) {
                    TutorialRendering.clearButtonName();
                    specialMsg("tutorial.reignofnether.excellent");
                    OrthoviewClientEvents.unlockCam();
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    nextStageAfterDelay(100);
                }
            }
            case SELECT_UNIT -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.worker_units");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 1) {
                    msg("tutorial.reignofnether.select_worker");
                    setHelpButtonText("tutorial.reignofnether.select_worker2");
                    UnitClientEvents.clearSelectedUnits();
                    progressStage();
                } else if (stageProgress == 2 && hasUnitSelected("villager")) {
                    nextStage();
                }
            }
            case MOVE_UNIT -> {
                if (stageProgress == 0 && hasUnitSelected("villager")) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.move_worker");
                    setHelpButtonText("tutorial.reignofnether.move_worker2");
                    progressStage();
                } else if (stageProgress == 1 && hasUnitSelected("villager")) {
                    Unit unit = (Unit) UnitClientEvents.getSelectedUnits().get(0);
                    MoveToTargetBlockGoal goal = unit.getMoveGoal();
                    if (goal != null && goal.getMoveTarget() != null) {
                        specialMsg("tutorial.reignofnether.nice_work");
                        nextStageAfterDelay(100);
                    }
                }
            }
            case BOX_SELECT_UNITS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.select_villagers");
                    progressStageAfterDelay(120);
                } else if (stageProgress == 1) {
                    msg("tutorial.reignofnether.select_villagers2");
                    setHelpButtonText("tutorial.reignofnether.select_villagers3");
                    progressStage();
                } else if (stageProgress == 2 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    nextStage();
                }
            }
            case MOVE_UNITS -> {
                if (stageProgress == 0 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.move_group");
                    setHelpButtonText("tutorial.reignofnether.move_group2");
                    progressStage();
                } else if (stageProgress == 1 && UnitClientEvents.getSelectedUnits().size() > 1) {
                    Unit unit = (Unit) UnitClientEvents.getSelectedUnits().get(1);
                    MoveToTargetBlockGoal goal = unit.getMoveGoal();
                    if (goal != null && goal.getMoveTarget() != null) {
                        specialMsg("tutorial.reignofnether.great_job");
                        nextStageAfterDelay(100);
                    }
                }
            }
            case UNIT_TIPS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.tip.select_same");
                    progressStageAfterDelay(140);
                } else if (stageProgress == 1) {
                    msg("tutorial.reignofnether.tip.deselect");
                    progressStageAfterDelay(140);
                } else if (stageProgress == 2) {
                    msg("tutorial.reignofnether.tip.grouping");
                    progressStageAfterDelay(140);
                } else if (stageProgress == 3) {
                    nextStageAfterSpace();
                }
            }
            case BUILD_INTRO -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.start_base");
                    progressStageAfterDelay(100);
                } else if (stageProgress == 1) {
                    msg("tutorial.reignofnether.capitol");
                    progressStageAfterDelay(120);
                } else if (stageProgress == 2) {
                    msg("tutorial.reignofnether.good_spot");
                    OrthoviewClientEvents.forceMoveCam(BUILD_CAM_POS, 50);
                    progressStageAfterDelay(180);
                } else if (stageProgress == 3) {
                    msg("tutorial.reignofnether.building_resources");
                    nextStageAfterDelay(120);
                }
            }
            case BUILD_TOWN_CENTRE -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.place_capitol");
                    setHelpButtonText("tutorial.reignofnether.place_capitol2");
                    TutorialRendering.setButtonName(TownCentre.buildingName);
                    progressStage();
                } else if (stageProgress == 1 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre) {
                        nextStage();
                    }
                }
            }
            case BUILDING_TIPS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.all_workers_build");
                    setHelpButtonText("tutorial.reignofnether.wait_for_capitol");
                    progressStageAfterDelay(240);
                } else if (stageProgress == 1) {
                    msg("tutorial.reignofnether.select_building");
                    progressStage();
                } else if (stageProgress == 2 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre
                        && townCentre.isBuilt) {
                        specialMsg("tutorial.reignofnether.congratulations");
                        nextStageAfterDelay(100);
                    }
                }
            } //msg("You can also set a rally point for your building with RIGHT-CLICK.");
            case TRAIN_WORKER -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.capitols_produce_workers");
                    progressStageAfterDelay(120);
                } else if (stageProgress == 1) {
                    msg("tutorial.reignofnether.worker_cost");
                    progressStageAfterDelay(120);
                } else if (stageProgress == 2) {
                    msg("tutorial.reignofnether.rally_point");
                    setHelpButtonText("tutorial.reignofnether.rally_point2");
                    progressStage();
                } else if (stageProgress == 3 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre
                        && townCentre.getRallyPoint() != null) {
                        msg("tutorial.reignofnether.make_villager");
                        setHelpButtonText("tutorial.reignofnether.make_villager2");
                        TutorialRendering.setButtonName(VillagerProd.itemName);
                        progressStage();
                    }
                } else if (stageProgress == 4 && BuildingClientEvents.getBuildings().size() > 0) {
                    if (BuildingClientEvents.getBuildings().get(0) instanceof TownCentre townCentre
                        && townCentre.productionQueue.size() > 0) {
                        msg("tutorial.reignofnether.tip.queue");
                        progressStage();
                    }
                } else if (stageProgress == 5 && UnitClientEvents.getAllUnits().size() > 3) {
                    specialMsg("tutorial.reignofnether.nice_work_workers");
                    clearHelpButtonText();
                    TutorialRendering.clearButtonName();
                    progressStageAfterDelay(100);
                } else if (stageProgress == 6) {
                    msg("tutorial.reignofnether.tip.cancel");
                    progressStageAfterDelay(140);
                } else if (stageProgress == 7) {
                    nextStageAfterSpace();
                }
            }
            case GATHER_WOOD -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.gather_resources");
                    progressStageAfterDelay(120);
                } else if (stageProgress == 1) {
                    msg("tutorial.reignofnether.forest");
                    OrthoviewClientEvents.forceMoveCam(WOOD_POS, 100);
                    progressStageAfterDelay(120);
                } else if (stageProgress == 2) {
                    msg("tutorial.reignofnether.gather_wood");
                    setHelpButtonText("tutorial.reignofnether.gather_wood2");
                    progressStage();
                } else if (stageProgress == 3) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager && villager.getGatherResourceGoal().isGathering()
                            && villager.getGatherResourceGoal().getTargetResourceName() == ResourceName.WOOD) {
                            specialMsg("tutorial.reignofnether.well_done");
                            clearHelpButtonText();
                            progressStageAfterDelay(100);
                            break;
                        }
                    }
                } else if (stageProgress == 4) {
                    msg("tutorial.reignofnether.tip.autoreturn");
                    nextStageAfterDelay(160);
                }
            }
            case GATHER_ORE -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.gather_ore");
                    progressStageAfterDelay(100);
                } else if (stageProgress == 1) {
                    msg("tutorial.reignofnether.beach_ore");
                    OrthoviewClientEvents.forceMoveCam(ORE_POS, 50);
                    progressStageAfterDelay(120);
                } else if (stageProgress == 2) {
                    msg("tutorial.reignofnether.click_ore");
                    setHelpButtonText("tutorial.reignofnether.click_ore2");
                    progressStage();
                } else if (stageProgress == 3) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager && villager.getGatherResourceGoal().isGathering()
                            && villager.getGatherResourceGoal().getTargetResourceName() == ResourceName.ORE) {
                            specialMsg("tutorial.reignofnether.well_done");
                            clearHelpButtonText();
                            progressStageAfterDelay(100);
                            break;
                        }
                    }
                } else if (stageProgress == 4) {
                    msg("tutorial.reignofnether.tip.sand_richer");
                    nextStageAfterDelay(120);
                }
            }
            case HUNT_ANIMALS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg("tutorial.reignofnether.gather_food");
                    progressStageAfterDelay(120);
                } else if (stageProgress == 1) {
                    OrthoviewClientEvents.forceMoveCam(FOOD_POS, 50);
                    TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_ANIMALS);
                    msg("tutorial.reignofnether.pigs");
                    progressStageAfterDelay(140);
                } else if (stageProgress == 2) {
                    msg("tutorial.reignofnether.hunt_pig");
                    setHelpButtonText("tutorial.reignofnether.hunt_pig2");
                    progressStageAfterDelay(180);
                } else if (stageProgress == 3) {
                    msg("tutorial.reignofnether.empty_pockets");
                    progressStage();
                } else if (stageProgress == 4) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager) {
                            LivingEntity targetEntity = villager.getTarget();
                            if (ResourceSources.isHuntableAnimal(targetEntity)
                                && targetEntity.getHealth() < targetEntity.getMaxHealth()) {
                                msg(    "tutorial.reignofnether.tip.dropped");
                                progressStage();
                                break;
                            }
                        }
                    }
                } else if (stageProgress == 5) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager) {
                            LivingEntity targetEntity = villager.getTarget();
                            if (ResourceSources.isHuntableAnimal(targetEntity)
                                && targetEntity.getHealth() < targetEntity.getMaxHealth() / 2) {
                                msg(    "tutorial.reignofnether.tip.pick_up");
                                progressStage();
                                break;
                            }
                        }
                    }
                } else if (stageProgress == 6) {
                    for (LivingEntity entity : UnitClientEvents.getAllUnits())
                        if (entity instanceof VillagerUnit villager) {
                            for (ItemStack itemStack : villager.getItems())
                                if (Resources.getTotalResourcesFromItems(List.of(itemStack)).food >= 100) {
                                    villagersHoldingFood += 1;
                                }
                        }

                    if (villagersHoldingFood > 0) {
                        specialMsg(    "tutorial.reignofnether.great_work");
                        clearHelpButtonText();
                        progressStageAfterDelay(100);
                    }
                } else if (stageProgress == 7) {
                    msg(    "tutorial.reignofnether.return_food");
                    setHelpButtonText(    "tutorial.reignofnether.return_food2");
                    progressStage();
                } else if (stageProgress == 8) {
                    int villagersHoldingFoodNow = 0;
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity instanceof VillagerUnit villager) {
                            for (ItemStack itemStack : villager.getItems()) {
                                Resources res = Resources.getTotalResourcesFromItems(List.of(itemStack));
                                if (res.food > 0) {
                                    villagersHoldingFoodNow += 1;
                                }
                            }
                        }
                    }
                    if (villagersHoldingFoodNow < villagersHoldingFood) {
                        specialMsg("tutorial.reignofnether.excellent");
                        clearHelpButtonText();
                        progressStageAfterDelay(100);
                    }
                } else if (stageProgress == 9) {
                    msg(    "tutorial.reignofnether.tip.max_carry");
                    nextStageAfterSpace();
                }
            }
            case EXPLAIN_BUILDINGS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(    "tutorial.reignofnether.expand_base");
                    setHelpButtonText(    "tutorial.reignofnether.new_buildings");
                    shouldPauseTicking = () -> UnitClientEvents.getSelectedUnits().isEmpty()
                        || !(UnitClientEvents.getSelectedUnits().get(0) instanceof VillagerUnit)
                        || BuildingClientEvents.getBuildingToPlace() != null;
                    progressStageAfterDelay(140);
                } else if (stageProgress == 1 && hasUnitSelected("villager")) {
                    TutorialRendering.setButtonName(OakStockpile.buildingName);
                    msg(    "tutorial.reignofnether.stockpiles");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 2 && hasUnitSelected("villager")) {
                    TutorialRendering.setButtonName(VillagerHouse.buildingName);
                    msg(    "tutorial.reignofnether.houses");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 3 && hasUnitSelected("villager")) {
                    TutorialRendering.setButtonName(WheatFarm.buildingName);
                    msg(    "tutorial.reignofnether.farms");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 4 && hasUnitSelected("villager")) {
                    clearHelpButtonText();
                    TutorialRendering.setButtonName(Barracks.buildingName);
                    msg(    "tutorial.reignofnether.barracks");
                    progressStageAfterDelay(160);
                } else {
                    nextStage();
                }
            }
            case BUILD_BASE -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    TutorialRendering.clearButtonName();
                    msg(    "tutorial.reignofnether.check_out_buildings");
                    progressStageAfterDelay(120);
                } else if (stageProgress == 1) {
                    msg(    "tutorial.reignofnether.build_barracks");
                    setHelpButtonText(    "tutorial.reignofnether.build_barracks2");
                    progressStage();
                } else if (stageProgress == 2) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building instanceof Barracks barracks && barracks.isBuilt) {
                            specialMsg("tutorial.reignofnether.great_job");
                            nextStageAfterDelay(100);
                            break;
                        }
                    }
                }
            }
            case EXPLAIN_BARRACKS -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    msg(    "tutorial.reignofnether.select_barracks");
                    setHelpButtonText(    "tutorial.reignofnether.select_barracks2");
                    shouldPauseTicking = () -> BuildingClientEvents.getSelectedBuildings().isEmpty()
                        || !(BuildingClientEvents.getSelectedBuildings().get(0) instanceof Barracks);
                    progressStageAfterDelay(140);
                } else if (stageProgress == 1 && hasBuildingSelected(Barracks.buildingName)) {
                    TutorialRendering.setButtonName(VindicatorProd.itemName);
                    msg(    "tutorial.reignofnether.vindicators");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 2 && hasBuildingSelected(Barracks.buildingName)) {
                    clearHelpButtonText();
                    TutorialRendering.setButtonName(PillagerProd.itemName);
                    msg(    "tutorial.reignofnether.pillagers");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 3) {
                    nextStage();
                }
            }
            case BUILD_ARMY -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    setHelpButtonText(
                        "tutorial.reignofnether.produce_illagers");
                    TutorialRendering.clearButtonName();
                    msg(    "tutorial.reignofnether.three_units");
                    progressStage();
                } else if (stageProgress == 1) {
                    int armyCount = 0;
                    for (LivingEntity entity : UnitClientEvents.getAllUnits())
                        if (entity instanceof VindicatorUnit || entity instanceof PillagerUnit) {
                            armyCount += 1;
                        }
                    if (armyCount >= 3) {
                        specialMsg(    "tutorial.reignofnether.awesome");
                        progressStageAfterDelay(100);
                        TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_MONSTER_WORKERS);
                    }
                } else if (stageProgress == 2) {
                    msg(    "tutorial.reignofnether.tip.select_military");
                    nextStageAfterDelay(200);
                    TutorialServerboundPacket.doServerAction(TutorialAction.START_MONSTER_BASE);
                }
            }
            case DEFEND_BASE -> {
                if (stageProgress == 0) {
                    msg(    "tutorial.reignofnether.monster_attack");
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_NIGHT_TIME);
                    TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_MONSTERS_A);
                    OrthoviewClientEvents.forceMoveCam(MONSTER_CAMERA_POS, 50);
                    progressStageAfterDelay(120);
                }
                if (stageProgress == 1) {
                    setHelpButtonText(    "tutorial.reignofnether.attack");
                    msg(    "tutorial.reignofnether.attack");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 2) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.ATTACK_WITH_MONSTERS_A);
                    progressStage();
                } else if (stageProgress == 3) {
                    if (UnitClientEvents.getAllUnits()
                        .stream()
                        .filter(u -> u instanceof PillagerUnit || u instanceof VindicatorUnit)
                        .toList()
                        .isEmpty()) {
                        progressStage();
                    }
                    if (UnitClientEvents.getAllUnits()
                        .stream()
                        .filter(u -> u instanceof ZombieUnit || u instanceof SkeletonUnit)
                        .toList()
                        .isEmpty()) {
                        msg(    "tutorial.reignofnether.more_incoming");
                        TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_MONSTERS_B);
                        OrthoviewClientEvents.forceMoveCam(MONSTER_CAMERA_POS, 50);
                        progressStageAfterDelay(100);
                    }
                } else if (stageProgress == 4) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.ATTACK_WITH_MONSTERS_B);
                    progressStageAfterDelay(200);
                } else if (stageProgress == 5) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.EXPAND_MONSTER_BASE_A);
                    TutorialServerboundPacket.doServerAction(TutorialAction.SET_DAY_TIME);
                    specialMsg(    "tutorial.reignofnether.dawn_breaks");
                    progressStage();
                } else if (stageProgress == 6) {
                    if (UnitClientEvents.getAllUnits()
                        .stream()
                        .filter(u -> u instanceof ZombieUnit || u instanceof SkeletonUnit)
                        .toList()
                        .isEmpty()) {
                        specialMsg(    "tutorial.reignofnether.defended");
                        clearHelpButtonText();
                        progressStageAfterDelay(100);
                    }
                } else if (stageProgress == 7) {
                    msg(    "tutorial.reignofnether.tip.death_drops");
                    nextStageAfterDelay(200);
                }
            }
            case REPAIR_BUILDING -> {
                if (stageProgress == 0) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.EXPAND_MONSTER_BASE_B);
                    for (Building building : BuildingClientEvents.getBuildings())
                        if (building.getHealth() < building.getMaxHealth() && building.getFaction() == Faction.VILLAGERS
                            && damagedBuildings.size() < 3) {
                            damagedBuildings.add(building);
                        }
                    if (damagedBuildings.isEmpty()) {
                        nextStage();
                    } else {
                        msg(    "tutorial.reignofnether.buildings_damaged");
                        progressStageAfterDelay(100);
                    }
                } else if (stageProgress == 1) {
                    msg(    "tutorial.reignofnether.repair_buildings");
                    setHelpButtonText(
                        "tutorial.reignofnether.repair_buildings2");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 2) {
                    for (Building building : damagedBuildings) {
                        if (building.getHealth() >= building.getMaxHealth()) {
                            specialMsg(    "tutorial.reignofnether.good_job");
                            clearHelpButtonText();
                            progressStageAfterDelay(100);
                            break;
                        }
                    }
                } else if (stageProgress == 3) {
                    msg(    "tutorial.reignofnether.tip.building_health");
                    progressStageAfterDelay(200);
                } else if (stageProgress == 4) {
                    TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_MONSTER_BASE_ARMY);
                    nextStageAfterSpace();
                }
            }
            case BUILD_BRIDGE -> {
                if (stageProgress == 0) {
                    OrthoviewClientEvents.forceMoveCam(MONSTER_BASE_POS, 80);
                    if (FogOfWarClientEvents.isEnabled()) {
                        msg(    "tutorial.reignofnether.monster_base");
                    } else {
                        msg(    "tutorial.reignofnether.monster_base2");
                    }
                    progressStageAfterDelay(160);
                } else if (stageProgress == 1) {
                    msg(    "tutorial.reignofnether.build_bridge");
                    progressStageAfterDelay(100);
                } else if (stageProgress == 2) {
                    OrthoviewClientEvents.forceMoveCam(BRIDGE_POS, 50);
                    msg(    "tutorial.reignofnether.build_bridge2");
                    setHelpButtonText(
                        "tutorial.reignofnether.build_bridge3");
                    progressStage();
                } else if (stageProgress == 3) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building instanceof OakBridge bridge) {
                            msg(    "tutorial.reignofnether.tip.bridge_segments");
                            progressStage();
                            break;
                        }
                    }
                } else if (stageProgress == 4) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building instanceof OakBridge bridge && bridge.isBuilt) {
                            specialMsg(    "tutorial.reignofnether.nice_job");
                            progressStageAfterDelay(100);
                            break;
                        }
                    }
                } else if (stageProgress == 5) {
                    msg(    "tutorial.reignofnether.tip.bridges_neutral");
                    progressStageAfterDelay(200);
                } else if (stageProgress == 6) {
                    msg(    "tutorial.reignofnether.tip.crossing_bridges");
                    progressStageAfterDelay(200);
                } else if (stageProgress == 7) {
                    nextStage();
                }
            }
            case ATTACK_ENEMY_BASE -> {
                if (stageProgress == 0) {
                    msg(    "tutorial.reignofnether.reinforcements");
                    TutorialServerboundPacket.doServerAction(TutorialAction.SPAWN_FRIENDLY_ARMY);
                    OrthoviewClientEvents.forceMoveCam(ARMY_POS, 50);
                    progressStageAfterDelay(160);
                } else if (stageProgress == 1) {
                    msg(    "tutorial.reignofnether.iron_golem");
                    setHelpButtonText(    "tutorial.reignofnether.destroy_monster_base");
                    progressStageAfterDelay(200);
                } else if (stageProgress == 2) {
                    msg(    "tutorial.reignofnether.tip.ranged_no_buildings");
                    progressStageAfterDelay(200);
                } else if (stageProgress == 3) {
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (building.getFaction() == Faction.MONSTERS
                            && building.getHealth() < building.getMaxHealth()) {
                            msg(    "tutorial.reignofnether.tip.monster_capitol");
                            progressStage();
                            break;
                        }
                    }
                } else if (stageProgress == 4) {
                    boolean botAlive = false;
                    for (Building building : BuildingClientEvents.getBuildings())
                        if (building.getFaction() == Faction.MONSTERS) {
                            botAlive = true;
                        }

                    if (!botAlive) {
                        // should show the standard victory screen
                        progressStageAfterDelay(200);
                    }
                } else if (stageProgress == 5) {
                    nextStageAfterSpace();
                }
            }
            case OUTRO -> {
                if (stageProgress == 0) {
                    specialMsg(    "tutorial.reignofnether.congratulations_tutorial");
                    progressStageAfterDelay(100);
                } else if (stageProgress == 1) {
                    msg(    "tutorial.reignofnether.continue");
                    progressStageAfterDelay(120);
                } else if (stageProgress == 2) {
                    msg(    "tutorial.reignofnether.reset_game");
                    progressStageAfterDelay(120);
                } else if (stageProgress == 3) {
                    msg(    "tutorial.reignofnether.server_hosting");
                    progressStageAfterDelay(160);
                } else if (stageProgress == 4) {
                    msg(    "tutorial.reignofnether.good_luck");
                    progressStageAfterDelay(100);
                } else if (stageProgress == 5) {
                    specialMsg(    "tutorial.reignofnether.tutorial_disabled");
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
