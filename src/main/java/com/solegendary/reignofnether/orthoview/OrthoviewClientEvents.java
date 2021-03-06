package com.solegendary.reignofnether.orthoview;

import com.solegendary.reignofnether.guiscreen.TopdownGuiServerboundPacket;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.client.Minecraft;
import com.solegendary.reignofnether.registrars.Keybinds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import com.mojang.math.Matrix4f;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import static net.minecraft.util.Mth.sign;

/**
 * Handler that implements and manages hotkeys for the orthographic camera.
 *
 * @author SoLegendary, adapted from Mineshot by Nico Bergemann <barracuda415 at yahoo.de>
 */
public class OrthoviewClientEvents {

    public static boolean enabled = false;
    private static boolean cameraMovingByMouse = false; // is the camera being moved using the mouse?

    private static final Minecraft MC = Minecraft.getInstance();
    private static final float ZOOM_STEP_KEY = 5;
    private static final float ZOOM_STEP_SCROLL = 1;
    private static final float ZOOM_MIN = 10;
    private static final float ZOOM_MAX = 90;
    private static final float PAN_KEY_STEP = 0.3f;
    private static final float EDGE_CAMPAM_SENSITIVITY = 0.8f;
    private static final float CAMROTY_MAX = -20;
    private static final float CAMROTY_MIN = -90;
    private static final float CAMROT_MOUSE_SENSITIVITY = 0.12f;
    private static final float CAMPAN_MOUSE_SENSITIVITY = 0.15f;

    private static float zoom = 30; // * 2 = number of blocks in height
    private static float camRotX = 135; // left/right - should start northeast (towards -Z,+X)
    private static float camRotY = -45; // up/down
    private static float camRotAdjX = 0;
    private static float camRotAdjY = 0;
    private static float mouseRightDownX = 0;
    private static float mouseRightDownY = 0;
    private static float mouseLeftDownX = 0;
    private static float mouseLeftDownY = 0;

    private static double prevPlayerY = 64;
    private static final double setPlayerY = 85;

    public static boolean isEnabled() {
        return enabled;
    }
    public static boolean isCameraMovingByMouse() { return cameraMovingByMouse; }
    public static float getZoom() { return zoom; }
    public static float getCamRotX() {
        return -camRotX - camRotAdjX;
    }
    public static float getCamRotY() { return -camRotY - camRotAdjY; }

    private static void reset() {
        zoom = 30;
        camRotX = 135;
        camRotY = -45;
    }
    public static void rotateCam(float x, float y) {
        camRotX += x;
        if (camRotX >= 360)
            camRotX -= 360;
        if (camRotX <= -360)
            camRotX += 360;

        camRotY += y;
        if (camRotY > CAMROTY_MAX)
            camRotY = CAMROTY_MAX;
        if (camRotY < CAMROTY_MIN)
            camRotY = CAMROTY_MIN;
    }
    public static void zoomCam(float zoomAdj) {
        zoom += zoomAdj;
        if (zoom < ZOOM_MIN)
            zoom = ZOOM_MIN;
        if (zoom > ZOOM_MAX)
            zoom = ZOOM_MAX;
    }

    public static void panCam(float x, float z) { // pan camera relative to rotation
        if (MC.player != null) {
            Vec2 XZRotated = MyMath.rotateCoords(x, z, -camRotX - camRotAdjX);
            MC.player.move(MoverType.SELF, new Vec3(XZRotated.x, 0, XZRotated.y));
        }
    }

    public static void toggleEnable() {
        enabled = !enabled;

        if (enabled) { // opening is done by TopdownGui world tick (which opens it whenever no other screen is open)
            //TopdownGuiServerboundPackets.openTopdownGui();
            MinimapClientEvents.setMapCentre(MC.player.getX(), MC.player.getZ());
            prevPlayerY = MC.player.getY();
            PlayerServerboundPacket.teleportPlayer(MC.player.getId(), MC.player.getX(), setPlayerY, MC.player.getX());
        }
        else {
            TopdownGuiServerboundPacket.closeTopdownGui(MC.player.getId());
            PlayerServerboundPacket.teleportPlayer(MC.player.getId(), MC.player.getX(), prevPlayerY, MC.player.getX());
        }
    }

    // ensure topdownGui is always open whenever Orthoview is enabled (if no other screens are open)
    // it takes a while for the packet to be received and processed so don't spam the server with it
    private static int sendPacketCooldown = 10;
    @SubscribeEvent
    public static void onWorldTick(TickEvent.ClientTickEvent evt) {
        if (sendPacketCooldown > 0) sendPacketCooldown -= 1;
        if (Minecraft.getInstance().screen == null && isEnabled() && sendPacketCooldown <= 0) {
            TopdownGuiServerboundPacket.openTopdownGui(MC.player.getId());
            sendPacketCooldown = 10;
        }
    }

    @SubscribeEvent
    // can't use ScreenEvent.KeyboardKeyPressedEvent as that only happens when a screen is up
    public static void onInput(InputEvent.KeyInputEvent evt) {

        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions
            if (evt.getKey() == Keybinds.toggle.getKey().getValue())
                toggleEnable();

            if (evt.getKey() == Keybinds.reset.getKey().getValue())
                reset();
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrollEvent evt) {
        if (!enabled) return;

        zoomCam((float) sign(evt.getScrollDelta()) * -ZOOM_STEP_SCROLL);
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.DrawScreenEvent evt) {
        String screenName = evt.getScreen().getTitle().getString();

        if (!enabled || !screenName.equals("topdowngui_container")) return;

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
        // mouse (0,0) is top left of screen

        // for one frame you can take the mouse outside of the window, so use this amount to adjust pan speed
        if (!Keybinds.altMod.isDown()) {
            if (cursorX <= 0)
                panCam(EDGE_CAMPAM_SENSITIVITY, 0);
            else if (cursorX >= glfwWinWidth)
                panCam(-EDGE_CAMPAM_SENSITIVITY, 0);
            if (cursorY <= 0)
                panCam(0, EDGE_CAMPAM_SENSITIVITY);
            else if (cursorY >= glfwWinHeight)
                panCam(0, -EDGE_CAMPAM_SENSITIVITY);
        }

        // lock mouse inside window
        if (cursorX >= glfwWinWidth)
            GLFW.glfwSetCursorPos(glfwWindow, glfwWinWidth, cursorY);
        if (cursorY >= glfwWinHeight)
            GLFW.glfwSetCursorPos(glfwWindow, cursorX, glfwWinHeight);
        if (cursorX <= 0)
            GLFW.glfwSetCursorPos(glfwWindow, 0, cursorY);
        if (cursorY <= 0)
            GLFW.glfwSetCursorPos(glfwWindow, cursorX, 0);
    }

    // prevents stuff like fire and water effects being shown on your HUD
    @SubscribeEvent
    public static void onRenderBlockOverlay(RenderBlockOverlayEvent evt) {
        if (enabled)
            evt.setCanceled(true);
    }
    
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseClickedEvent.Post evt) {
        if (!enabled) return;

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            mouseLeftDownX = (float) evt.getMouseX();
            mouseLeftDownY = (float) evt.getMouseY();
        }
        else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            mouseRightDownX = (float) evt.getMouseX();
            mouseRightDownY = (float) evt.getMouseY();
        }
    }
    @SubscribeEvent
    public static void onMouseRelease(ScreenEvent.MouseReleasedEvent evt) {
        if (!enabled) return;

        // stop treating the rotation as adjustments and add them to the base amount
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            cameraMovingByMouse = false;
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            cameraMovingByMouse = false;
            rotateCam(camRotAdjX,camRotAdjY);
            camRotAdjX = 0;
            camRotAdjY = 0;
        }
    }
    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragEvent evt) {
        if (!enabled) return;

        if (evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_1 && Keybinds.altMod.isDown()) {
            cameraMovingByMouse = true;
            float moveX = (float) evt.getDragX() * CAMPAN_MOUSE_SENSITIVITY * (zoom/ZOOM_MAX); //* winWidth/1920;
            float moveZ = (float) evt.getDragY() * CAMPAN_MOUSE_SENSITIVITY * (zoom/ZOOM_MAX); //* winHeight/1080;
            panCam(moveX, moveZ);
        }
        else if (evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_2 && Keybinds.altMod.isDown()) {
            cameraMovingByMouse = true;
            camRotAdjX = (float) (evt.getMouseX() - mouseRightDownX) * CAMROT_MOUSE_SENSITIVITY;
            camRotAdjY = (float) -(evt.getMouseY() - mouseRightDownY) * CAMROT_MOUSE_SENSITIVITY;

            if (camRotY + camRotAdjY > CAMROTY_MAX)
                camRotAdjY = CAMROTY_MAX - camRotY;
            if (camRotY + camRotAdjY < CAMROTY_MIN)
                camRotAdjY = CAMROTY_MIN - camRotY;
        }
    }

    // don't let orthoview players see other orthoview players
    @SubscribeEvent
    public static void onPlayerRender(RenderPlayerEvent.Pre evt) {
        if (enabled && evt.getPlayer().isSpectator())
            evt.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFovModifier(EntityViewRenderEvent.FieldOfView evt) {
        if (enabled)
            evt.setFOV(180);
    }

    // on each game render frame
    @SubscribeEvent
    public static void onFogDensity(EntityViewRenderEvent.RenderFogEvent evt) {
        if (!enabled)
            return;

        Player player = MC.player;

        // zoom in/out with keys
        if (Keybinds.zoomIn.isDown())
            zoomCam(-ZOOM_STEP_KEY);
        if (Keybinds.zoomOut.isDown())
            zoomCam(ZOOM_STEP_KEY);

        // pan camera with keys
        if (Keybinds.panPlusX.isDown())
            panCam(PAN_KEY_STEP,0);
        else if (Keybinds.panMinusX.isDown())
            panCam(-PAN_KEY_STEP,0);
        if (Keybinds.panPlusZ.isDown())
            panCam(0, PAN_KEY_STEP);
        else if (Keybinds.panMinusZ.isDown())
            panCam(0,-PAN_KEY_STEP);

        // note that we treat x and y rot as horizontal and vertical, but MC treats it the other way around...
        if (player != null) {
            player.setXRot(-camRotY - camRotAdjY);
            player.setYRot(-camRotX - camRotAdjX);
        }
    }

    // OrthoViewMixin uses this to generate a customisation orthographic view to replace the usual view
    // shamelessly copied from ImmersivePortals 1.16
    public static Matrix4f getOrthographicProjection() {
        int width = MC.getWindow().getScreenWidth();
        int height = MC.getWindow().getScreenHeight();

        float near = -3000;
        float far = 3000;

        float wView = (zoom / height) * width;
        float left = -wView / 2;
        float rgt = wView / 2;

        float top = zoom / 2;
        float bot = -zoom / 2;

        float[] arr = new float[]{
                2.0f/(rgt-left), 0,              0,                -(rgt+left)/(rgt-left),
                0,               2.0f/(top-bot), 0,                -(top+bot)/(top-bot),
                0,               0,              -2.0f/(far-near), -(far+near)/(far-near),
                0,               0,              0,                1
        };
        FloatBuffer fb = FloatBuffer.wrap(arr);
        Matrix4f m1 = new Matrix4f();
        m1.load(fb);

        return m1;
    }
}
