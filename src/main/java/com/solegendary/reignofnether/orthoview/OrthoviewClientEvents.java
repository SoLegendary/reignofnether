package com.solegendary.reignofnether.orthoview;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.RangeIndicator;
import com.solegendary.reignofnether.config.ReignOfNetherClientConfigs;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.guiscreen.TopdownGuiServerboundPacket;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.startpos.StartPosServerboundPacket;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.DoubleBuffer;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static net.minecraft.util.Mth.sign;

/**
 * Handler that implements and manages hotkeys for the orthographic camera.
 *
 * @author SoLegendary, adapted from Mineshot by Nico Bergemann <barracuda415 at yahoo.de>
 */
public class OrthoviewClientEvents {


    public enum LeafHideMethod {
        NONE, AROUND_UNITS_AND_CURSOR, // requires threaded video option
        ALL
    }

    public static boolean shouldHideLeaves() {
        return hideLeavesMethod != LeafHideMethod.NONE;
    }

    public static LeafHideMethod hideLeavesMethod = LeafHideMethod.NONE;
    public static int enabledCount = 0;
    public static boolean enabled = false;
    private static boolean cameraMovingByMouse = false; // excludes edgepanning

    private static final Minecraft MC = Minecraft.getInstance();
    private static final float ZOOM_STEP_KEY = 5;
    private static final float ZOOM_STEP_SCROLL = 1;
    private static final float ZOOM_MIN = 10;
    private static final float ZOOM_MAX = 90;
    private static final float CAMROTY_MAX = -20;
    private static final float CAMROTY_MIN = -90;
    private static final float CAMROT_MOUSE_SENSITIVITY = 0.12f;

    private static final float ZOOM_DEFAULT = 30;
    private static final float CAMROTX_DEFAULT = 135;
    private static final float CAMROTY_DEFAULT = -45;

    private static final int FORCE_PAN_TICKS_MAX = 20;
    private static int forcePanTicksLeft = 0;
    private static float forcePanTargetX = 0;
    private static float forcePanTargetZ = 0;
    private static float forcePanOriginalX = 0;
    private static float forcePanOriginalZ = 0;
    private static float forcePanOriginalZoom = 0;

    private static final int FORCE_ROT_FRAMES_MAX = 20;
    private static int forceRotFramesLeft = 0;
    private static float forceRotTargetX = 0;
    private static float forceRotOriginalX = 0;

    private static int cameraLockTicksLeft = 0;
    private static boolean cameraLocked = false;

    private static float zoom = 30; // * 2 = number of blocks in height (higher == zoomed out)
    private static float camRotX = 135; // left/right - should start northeast (towards -Z,+X)
    private static float camRotY = -45; // up/down
    private static float camRotAdjX = 0;
    private static float camRotAdjY = 0;
    private static float mouseRightDownX = 0;
    private static float mouseRightDownY = 0;
    private static float mouseLeftDownX = 0;
    private static float mouseLeftDownY = 0;
    public static final float MAX_PAN_SENSITIVITY = 3.0f;

    // by default orthoview players stay at BASE_Y, but can be raised to as high as MAX_Y if they are clipping terrain
    public static double orthoviewPlayerBaseY = 100;
    public static double orthoviewPlayerMaxY = 160;
    private static double minOrthoviewY = 0;

    public static void setMinOrthoviewY(double value) {
        minOrthoviewY = value;
        if (MC.level != null && MC.player != null && MC.player.getY() < value + 15) {
            MC.player.move(MoverType.SELF, new Vec3(0, minOrthoviewY - MC.player.getY() + 15, 0));
        }
    }

    private static float getEdgeCamPanSensitivity() {
        return (float) (Math.sqrt(getZoom()) / (Math.sqrt(ZOOM_MAX))) * getPanSensitivityMult();
    }
    public static float getPanSensitivityMult() {
        return (float) ReignOfNetherClientConfigs.CAMERA_SENSITIVITY.get() / 10f;
    }
    public static void adjustPanSensitivityMult(boolean increase) {
        if (increase && Math.round(getPanSensitivityMult() * 10) < (MAX_PAN_SENSITIVITY * 10))
            ReignOfNetherClientConfigs.CAMERA_SENSITIVITY.set(ReignOfNetherClientConfigs.CAMERA_SENSITIVITY.get() + 1);
        else if (!increase && Math.round(getPanSensitivityMult() * 10) > 1)
            ReignOfNetherClientConfigs.CAMERA_SENSITIVITY.set(ReignOfNetherClientConfigs.CAMERA_SENSITIVITY.get() - 1);
    }

    public static void updateOrthoviewY() {
        if (MC.player != null && MC.level != null) {
            BlockPos playerPos = MC.player.blockPosition();
            int radius = 10; // Defines the area around the player to sample heights
            int sumHeights = 0;
            int count = 0;

            // Iterate through a square area around the player
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int blockX = playerPos.getX() + x;
                    int blockZ = playerPos.getZ() + z;
                    int height = MC.level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ);
                    sumHeights += height;
                    count++;
                }
            }
            // Calculate the average height
            int avgHeight = count > 0 ? sumHeights / count : playerPos.getY();

            // Update ORTHOVIEW values based on the average height
            orthoviewPlayerBaseY = Math.max(avgHeight + 30, minOrthoviewY);
            orthoviewPlayerMaxY = avgHeight + 100;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isCameraMovingByMouse() {
        return cameraMovingByMouse;
    }

    public static float getZoom() {
        return zoom;
    }

    public static float getCamRotX() {
        return -camRotX - camRotAdjX;
    }

    public static float getCamRotY() {
        return -camRotY - camRotAdjY;
    }

    public static boolean isCameraLocked() {
        return cameraLockTicksLeft > 0 || cameraLocked;
    }

    public static void lockCam() {
        cameraLocked = true;
    }

    public static void unlockCam() {
        cameraLocked = false;
    }

    private static void reset() {
        zoom = ZOOM_DEFAULT;
        camRotX = CAMROTX_DEFAULT;
        camRotY = CAMROTY_DEFAULT;
    }

    public static void rotateCam(float x, float y) {
        if (isCameraLocked()) {
            return;
        }
        camRotX += x;
        if (camRotX >= 360) {
            camRotX -= 360;
        }
        if (camRotX <= -360) {
            camRotX += 360;
        }

        /*
        camRotY += y;
        if (camRotY > CAMROTY_MAX)
            camRotY = CAMROTY_MAX;
        if (camRotY < CAMROTY_MIN)
            camRotY = CAMROTY_MIN;
         */
    }

    public static void zoomCam(float zoomAdj) {
        if (isCameraLocked()) {
            return;
        }
        zoom += zoomAdj;
        if (zoom < ZOOM_MIN) {
            zoom = ZOOM_MIN;
        }
        if (zoom > ZOOM_MAX) {
            zoom = ZOOM_MAX;
        }
    }

    public static void panCam(float x, float y, float z) { // pan camera relative to rotation
        if (MC.player != null) {
            Vec2 XZRotated = MyMath.rotateCoords(x, z, -camRotX - camRotAdjX);
            MC.player.move(MoverType.SELF, new Vec3(XZRotated.x, y, XZRotated.y));
        }
    }

    // lock the camera and move it towards a location, remain locked for cameraLockTicks
    public static void forceMoveCam(int x, int z, int cameraLockTicks) {
        if (MC.player != null) {
            forcePanTicksLeft = FORCE_PAN_TICKS_MAX;
            forcePanTargetX = x;
            forcePanTargetZ = z;
            cameraLockTicksLeft = FORCE_PAN_TICKS_MAX + cameraLockTicks;
            forcePanOriginalX = MC.player.getOnPos().getX();
            forcePanOriginalZ = MC.player.getOnPos().getZ();
            forcePanOriginalZoom = zoom;
        }
    }

    public static void forceMoveCam(Vec3i pos, int cameraLockTicks) {
        forceMoveCam(pos.getX(), pos.getZ(), cameraLockTicks);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) {
            return;
        }

        if (isEnabled() && MC.gameMode != null && (
            MC.gameMode.getPlayerMode() == GameType.ADVENTURE || MC.gameMode.getPlayerMode() == GameType.SURVIVAL
        )) {
            toggleEnable();
        }

        if (cameraLockTicksLeft > 0) {
            cameraLockTicksLeft -= 1;
        }

        if (!OrthoviewClientEvents.isEnabled() || MC.player == null || MC.level == null) {
            forcePanTicksLeft = 0;
            return;
        }

        if (MiscUtil.isGroundBlock(MC.level, MC.player.blockPosition().offset(0, -5, 0))
            && MC.player.getOnPos().getY() <= orthoviewPlayerMaxY) {
            panCam(0, 1f, 0);
        }
        if (!MiscUtil.isGroundBlock(MC.level, MC.player.blockPosition().offset(0, -6, 0))
            && MC.player.getOnPos().getY() >= orthoviewPlayerBaseY) {
            panCam(0, -1f, 0);
        }

        if (forcePanTicksLeft > 0) {
            float xDiff = (forcePanTargetX - forcePanOriginalX) / FORCE_PAN_TICKS_MAX;
            float zDiff = (forcePanTargetZ - forcePanOriginalZ) / FORCE_PAN_TICKS_MAX;
            float zoomDiff = (ZOOM_DEFAULT - forcePanOriginalZoom) / FORCE_PAN_TICKS_MAX;
            zoom += zoomDiff;
            MC.player.move(MoverType.SELF, new Vec3(xDiff, 0, zDiff));
            forcePanTicksLeft -= 1;
        }
        updateOrthoviewY();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) throws NoSuchFieldException {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS ||
            !OrthoviewClientEvents.isEnabled()) {
            return;
        }
        if (forceRotFramesLeft > 0) {
            float xDiff = (forceRotTargetX - forceRotOriginalX) / FORCE_ROT_FRAMES_MAX;
            rotateCam(-xDiff, 0);
            forceRotFramesLeft -= 1;
            if (forceRotFramesLeft <= 0) {
                camRotX = -forceRotTargetX;
                rotateCam(0,0);
            }
        }
    }

    public static void toggleEnable() {
        if (MC.level == null || MC.player == null) {
            return;
        }

        for (BuildingPlacement building : BuildingClientEvents.getBuildings())
            if (building instanceof RangeIndicator ri)
                ri.updateBorderBps();

        enabled = !enabled;

        if (enabled) {
            MC.options.tutorialStep = TutorialSteps.NONE;
            MC.getTutorial().stop();
            enabledCount += 1;
            PlayerServerboundPacket.enableOrthoview();
            MinimapClientEvents.setMapCentre(MC.player.getX(), MC.player.getZ());
            PlayerServerboundPacket.teleportPlayer(MC.player.getX(), orthoviewPlayerBaseY, MC.player.getZ());
            TopdownGuiServerboundPacket.openTopdownGui(MC.player.getId());
            MC.options.cloudStatus().set(CloudStatus.OFF);
            MC.options.hideGui = false; // for some reason, when gui is hidden, shape rendering goes whack
            MC.options.setCameraType(CameraType.FIRST_PERSON);
            switchToEasyIfPeaceful();
        } else {
            PlayerServerboundPacket.disableOrthoview();
            TopdownGuiServerboundPacket.closeTopdownGui(MC.player.getId());
            if (StartPosClientEvents.hasReservedPos()) {
                StartPosClientEvents.selectedFaction = Faction.NONE;
                StartPosServerboundPacket.unreservePos(StartPosClientEvents.getPos().pos);
            }
        }
        TutorialClientEvents.updateStage();
    }

    public static void centreCameraOnPos(BlockPos bp) {
        centreCameraOnPos(new Vec3(bp.getX(), bp.getY(), bp.getZ()));
    }

    // moves the camera to the position such that x,z is at the centre of the screen
    public static void centreCameraOnPos(Vec3 pos) {
        if (MC.player == null) {
            return;
        }
        MinimapClientEvents.setMapCentre(pos.x, pos.z);
        // at 0deg by default camera is facing +Z and we want to move it backwards from this
        Vec2 XZRotated = MyMath.rotateCoords(0, -20, OrthoviewClientEvents.getCamRotX());

        float offset = (float) (Math.sqrt(getZoom()) / (Math.sqrt(ZOOM_MAX)));

        int yDiff = (int) (MC.player.getY() - pos.y) - 40;

        Vec2 XZRotatedOffset = MyMath.rotateCoords(0, -(offset * 35) - yDiff, -camRotX - camRotAdjX);

        PlayerServerboundPacket.teleportPlayer(
                pos.x + XZRotated.x + XZRotatedOffset.x, MC.player.getY(),
                pos.z + XZRotated.y + XZRotatedOffset.y
        );
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent evt) {
        if (isEnabled()) {
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent evt) {
        if (isEnabled()) {
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    // can't use ScreenEvent.KeyboardKeyPressedEvent as that only happens when a screen is up
    public static void onInput(InputEvent.Key evt) {
        // Prevent repeated key actions
        if (evt.getAction() == GLFW.GLFW_PRESS) {

            if (evt.getKey() == Keybindings.getFnum(12).key) {
                tryToToggleEnable();
            }
            else if (evt.getKey() == Keybindings.reset.key) {
                reset();
            }
        }
    }

    // rotates the camera to fixed points at -135, -45, 45 and 135 degrees
    public static void fixedRotateCam(boolean clockwise) {
        if (forceRotFramesLeft <= 0) {
            camRotAdjX = 0;
            int roundedRotX = (Math.round(getCamRotX() / 90.0f) * 90) + 45;

            if (roundedRotX > getCamRotX() && clockwise)
                forceRotTargetX = roundedRotX;
            else if (roundedRotX < getCamRotX() && !clockwise)
                forceRotTargetX = roundedRotX - 90;
            else if (clockwise)
                forceRotTargetX = roundedRotX;
            else
                forceRotTargetX = roundedRotX - 90;

            if (forceRotTargetX == getCamRotX() && clockwise)
                forceRotTargetX += 90;
            else if (forceRotTargetX == getCamRotX())
                forceRotTargetX -= 90;

            forceRotOriginalX = getCamRotX();
            forceRotFramesLeft = FORCE_ROT_FRAMES_MAX;
        }
    }

    public static void tryToToggleEnable() {
        if (!OrthoviewClientEvents.isCameraLocked() && MC.gameMode != null) {
            if (MC.player != null && (
                    MC.gameMode.getPlayerMode() == GameType.ADVENTURE
                            || MC.gameMode.getPlayerMode() == GameType.SURVIVAL
            )) {
                MC.player.sendSystemMessage(Component.literal(I18n.get("hud.orthoview.reignofnether.ortho_error")));
            } else {
                toggleEnable();
            }
        }
    }

    public static Button getLeavesHidingButton() {
        return new Button("Hide Leaves Method",
                14,
                switch(hideLeavesMethod) {
                    case NONE -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/leaves.png");
                    case AROUND_UNITS_AND_CURSOR -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/lime_stained_glass.png");
                    case ALL -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/glass.png");
                },
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                null,
                () -> false,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK) || !MinimapClientEvents.isLargeMap(),
                () -> true,
                () -> {
                    FogOfWarClientEvents.resetFogChunks();
                    UnitClientEvents.windowUpdateTicks = 0;
                    if (hideLeavesMethod == LeafHideMethod.NONE) {
                        hideLeavesMethod = LeafHideMethod.AROUND_UNITS_AND_CURSOR;
                    } else if (hideLeavesMethod == LeafHideMethod.AROUND_UNITS_AND_CURSOR) {
                        hideLeavesMethod = LeafHideMethod.ALL;
                    } else if (hideLeavesMethod == LeafHideMethod.ALL) {
                        hideLeavesMethod = LeafHideMethod.NONE;
                    }
                },
                null,
                List.of(
                        fcs(I18n.get("hud.orthoview.reignofnether.hiding_leaves_around"), hideLeavesMethod == LeafHideMethod.AROUND_UNITS_AND_CURSOR),
                        fcs(I18n.get("hud.orthoview.reignofnether.hiding_leaves_all"), hideLeavesMethod == LeafHideMethod.ALL),
                        fcs(I18n.get("hud.orthoview.reignofnether.disabled_hiding_leaves"), hideLeavesMethod == LeafHideMethod.NONE)
                )
        );
    }

    // Method to switch difficulty to Easy if it is currently set to Peaceful
    private static void switchToEasyIfPeaceful() {
        Minecraft minecraft = Minecraft.getInstance();

        // Ensure this only runs in single-player mode
        if (minecraft.getSingleplayerServer() != null) {
            Difficulty currentDifficulty = minecraft.level.getDifficulty();

            // If the current difficulty is Peaceful, switch to Easy
            if (currentDifficulty == Difficulty.PEACEFUL) {
                minecraft.getSingleplayerServer().setDifficulty(Difficulty.EASY, true);
                HudClientEvents.showTemporaryMessage(
                    "RTS units cannot spawn in Peaceful. Your difficulty has been set to Easy.");
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrolled evt) {
        if (!enabled || isCameraLocked()) {
            return;
        }
        if (Keybindings.altMod.isDown()) {
            zoomCam((float) sign(evt.getScrollDelta()) * -ZOOM_STEP_SCROLL);
        }
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render evt) {
        if (!enabled || !(evt.getScreen() instanceof TopdownGui)) {
            return;
        }

        // GLFW coords seem to be 2x vanilla coords, but use only them for consistency
        // since we need to use glfwSetCursorPos
        long glfwWindow = MC.getWindow().getWindow();
        int glfwWinWidth = MC.getWindow().getScreenWidth();
        int glfwWinHeight = MC.getWindow().getScreenHeight();

        DoubleBuffer glfwCursorX = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer glfwCursorY = BufferUtils.createDoubleBuffer(1);
        GLFW.glfwGetCursorPos(glfwWindow, glfwCursorX, glfwCursorY);
        double cursorX = glfwCursorX.get();
        double cursorY = glfwCursorY.get();

        // panCam when cursor is at edge of screen
        // remember that mouse (0,0) is top left of screen
        if (!Keybindings.altMod.isDown() && MC.isWindowActive() && !isCameraLocked()) {
            if (cursorX <= 0) {
                panCam(getEdgeCamPanSensitivity(), 0, 0);
                TutorialClientEvents.pannedLeft = true;
            } else if (cursorX >= glfwWinWidth) {
                panCam(-getEdgeCamPanSensitivity(), 0, 0);
                TutorialClientEvents.pannedRight = true;
            }
            if (cursorY <= 0) {
                panCam(0, 0, getEdgeCamPanSensitivity());
                TutorialClientEvents.pannedUp = true;
            } else if (cursorY >= glfwWinHeight) {
                panCam(0, 0, -getEdgeCamPanSensitivity());
                TutorialClientEvents.pannedDown = true;
            }
        }
        // lock mouse inside window
        if (cursorX >= glfwWinWidth) {
            GLFW.glfwSetCursorPos(glfwWindow, glfwWinWidth, cursorY);
        }
        if (cursorY >= glfwWinHeight) {
            GLFW.glfwSetCursorPos(glfwWindow, cursorX, glfwWinHeight);
        }
        if (cursorX <= 0) {
            GLFW.glfwSetCursorPos(glfwWindow, 0, cursorY);
        }
        if (cursorY <= 0) {
            GLFW.glfwSetCursorPos(glfwWindow, cursorX, 0);
        }

        Player player = MC.player;

        // zoom in/out with keys
        if (Keybindings.zoomIn.isDown()) {
            zoomCam(-ZOOM_STEP_KEY);
        }
        if (Keybindings.zoomOut.isDown()) {
            zoomCam(ZOOM_STEP_KEY);
        }

        float panKeyStep = 1.5f * (getZoom() / ZOOM_MAX);

        if (!isCameraLocked() && !Keybindings.altMod.isDown()) {
            // pan camera with keys
            if (Keybindings.panPlusX.isDown()) {
                panCam(getEdgeCamPanSensitivity(), 0, 0);
            } else if (Keybindings.panMinusX.isDown()) {
                panCam(-getEdgeCamPanSensitivity(), 0, 0);
            }
            if (Keybindings.panPlusZ.isDown()) {
                panCam(0, 0, getEdgeCamPanSensitivity());
            } else if (Keybindings.panMinusZ.isDown()) {
                panCam(0, 0, -getEdgeCamPanSensitivity());
            }
        }
        // note that we treat x and y rot as horizontal and vertical, but MC treats it the other way around...
        if (player != null) {
            player.setXRot(-camRotY - camRotAdjY);
            player.setYRot(-camRotX - camRotAdjX);
        }
    }

    // prevents stuff like fire and water effects being shown on your HUD
    @SubscribeEvent
    public static void onRenderBlockOverlay(RenderBlockScreenEffectEvent evt) {
        if (enabled) {
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Post evt) {
        if (!enabled || isCameraLocked()) {
            return;
        }

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            mouseLeftDownX = (float) evt.getMouseX();
            mouseLeftDownY = (float) evt.getMouseY();
        } else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            mouseRightDownX = (float) evt.getMouseX();
            mouseRightDownY = (float) evt.getMouseY();
        }
    }

    @SubscribeEvent
    public static void onMouseRelease(ScreenEvent.MouseButtonReleased evt) {
        if (!enabled || isCameraLocked()) {
            return;
        }

        // stop treating the rotation as adjustments and add them to the base amount
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            cameraMovingByMouse = false;
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            cameraMovingByMouse = false;
            rotateCam(camRotAdjX, camRotAdjY);
            camRotAdjX = 0;
            camRotAdjY = 0;
        }
    }

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged evt) {
        if (!enabled || isCameraLocked()) {
            return;
        }

        if ((evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_1 && Keybindings.altMod.isDown())
            || evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_3) {
            cameraMovingByMouse = true;

            float moveX = (float) evt.getDragX() * 0.20f * (zoom / ZOOM_MAX) * getPanSensitivityMult();
            float moveZ = (float) evt.getDragY() * 0.20f * (zoom / ZOOM_MAX) * getPanSensitivityMult();
            panCam(moveX, 0, moveZ);
        } else if (evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_2 && Keybindings.altMod.isDown()) {
            cameraMovingByMouse = true;
            camRotAdjX = (float) (evt.getMouseX() - mouseRightDownX) * CAMROT_MOUSE_SENSITIVITY;
            //camRotAdjY = (float) -(evt.getMouseY() - mouseRightDownY) * CAMROT_MOUSE_SENSITIVITY;

            /*
            if (camRotY + camRotAdjY > CAMROTY_MAX)
                camRotAdjY = CAMROTY_MAX - camRotY;
            if (camRotY + camRotAdjY < CAMROTY_MIN)
                camRotAdjY = CAMROTY_MIN - camRotY;
             */
        }
    }

    // don't let orthoview players see other orthoview players or themselves
    @SubscribeEvent
    public static void onPlayerRender(RenderPlayerEvent.Pre evt) {
        if (enabled && (evt.getEntity().isSpectator() || evt.getEntity().isCreative())) {
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFovModifier(ViewportEvent.ComputeFov evt) {
        if (enabled) {
            evt.setFOV(180);
        }
    }

    /*
    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getGuiGraphics(), MC.font, new String[] {
                "rotX: " + getCamRotX()
        });
    }
     */

    // OrthoViewMixin uses this to generate a customisation orthographic view to replace the usual view
    // shamelessly copied from ImmersivePortals 1.16
    public static Matrix4f getOrthographicProjection() {
        int width = MC.getWindow().getScreenWidth();
        int height = MC.getWindow().getScreenHeight();

        float near = -3000;
        float far = 3000;

        float zoomFinal = zoom;

        float wView = (zoomFinal / height) * width;
        float left = -wView / 2;
        float rgt = wView / 2;

        float top = zoomFinal / 2;
        float bot = -zoomFinal / 2;

        Matrix4f m1 = new Matrix4f(2.0f / (rgt - left), 0, 0,
                -(rgt + left) / (rgt - left), 0,
                2.0f / (top - bot), 0,
                -(top + bot) / (top - bot), 0, 0, -2.0f / (far - near), -(far + near) / (far - near), 0, 0, 0, 1);

        return m1;
    }
}