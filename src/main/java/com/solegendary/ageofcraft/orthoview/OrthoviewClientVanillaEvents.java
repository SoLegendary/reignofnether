package com.solegendary.ageofcraft.orthoview;

import com.solegendary.ageofcraft.gui.TopdownGuiCommonVanillaEvents;
import com.solegendary.ageofcraft.util.MyMath;
import net.minecraft.client.Minecraft;
import com.solegendary.ageofcraft.registrars.Keybinds;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import com.solegendary.ageofcraft.gui.TopdownGuiContainer;
import com.mojang.math.Matrix4f;

import java.nio.FloatBuffer;

import static net.minecraft.util.Mth.sign;

/**
 * Handler that implements and manages hotkeys for the orthographic camera.
 *
 * @author SoLegendary, adapted from Mineshot by Nico Bergemann <barracuda415 at yahoo.de>
 */
public class OrthoviewClientVanillaEvents {

    public static boolean enabled = false;
    private static boolean cameraMovingByMouse = false; // is the camera being moved using the mouse?

    private static final Minecraft MC = Minecraft.getInstance();
    private static final float ZOOM_STEP_KEY = 5;
    private static final float ZOOM_STEP_SCROLL = 1;
    private static final float ZOOM_MIN = 10;
    private static final float ZOOM_MAX = 90;
    private static final float PAN_KEY_STEP = 0.3f;
    private static final float PAN_MOUSE_STEP = 0.3f;
    private static final float PAN_MOUSE_EDGE_BUFFER = 20; // if mouse < this distance from screen edge: start panning
    private static final float ROTATE_STEP = 0.35f;
    private static final float CAMROTY_MAX = -20;
    private static final float CAMROTY_MIN = -90;
    private static final float CAMROT_MOUSE_SENSITIVITY = 0.12f;
    private static final float CAMPAN_MOUSE_SENSITIVITY = 0.12f;

    private static float zoom = 30; // * 2 = number of blocks in height
    private static float camRotX = 45;
    private static float camRotY = -45;
    private static float camRotAdjX = 0;
    private static float camRotAdjY = 0;
    private static float mouseRightDownX = 0;
    private static float mouseRightDownY = 0;
    private static float mouseLeftDownX = 0;
    private static float mouseLeftDownY = 0;
    private static int winWidth = MC.getWindow().getGuiScaledWidth();
    private static int winHeight = MC.getWindow().getGuiScaledHeight();
    private static int screenWidth = MC.getWindow().getScreenWidth();
    private static int screenHeight = MC.getWindow().getScreenHeight();

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
        camRotX = 45;
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

    // are we on the top-down gui screen?
    public static boolean isTopdownGui(GuiScreenEvent evt) {
        if (evt.getGui() != null)
            return evt.getGui().getTitle().getString().equals(TopdownGuiContainer.TITLE.getString());
        else
            return false;
    }

    public static void toggleEnable() {
        enabled = !enabled;

        if (enabled) {
            TopdownGuiCommonVanillaEvents.openTopdownGui();
        }
        else {
            TopdownGuiCommonVanillaEvents.closeTopdownGui();
        }
    }

    @SubscribeEvent
    public static void onInput(InputEvent.KeyInputEvent evt) {

        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions
            if (evt.getKey() == Keybinds.toggle.getKey().getValue())
                toggleEnable();

            if (evt.getKey() == Keybinds.reset.getKey().getValue())
                reset();

            if (evt.getKey() == Keybinds.keyP.getKey().getValue())
                TopdownGuiCommonVanillaEvents.openPauseMenu();
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(GuiScreenEvent.MouseScrollEvent evt) {
        if (!enabled) return;

        zoomCam((float) sign(evt.getScrollDelta()) * -ZOOM_STEP_SCROLL);
    }

    @SubscribeEvent
    public static void onDrawScreen(GuiScreenEvent.DrawScreenEvent evt) {
        if (!enabled) return;

        if (Keybinds.altMod.isDown()) return;

        // no idea why but mouse x and y are half of what's expected
        int mouseX = evt.getMouseX();
        int mouseY = evt.getMouseY();

        // panCam when cursor is at edge of screen
        // mouse (0,0) is top left of screen
        /*
        if (mouseX < PAN_MOUSE_EDGE_BUFFER)
            panCam(PAN_KEY_STEP * 2, 0);
        else if (mouseX > winWidth - PAN_MOUSE_EDGE_BUFFER)
            panCam(-PAN_KEY_STEP * 2, 0);
        if (mouseY < PAN_MOUSE_EDGE_BUFFER)
            panCam(0, PAN_KEY_STEP * 2);
        else if (mouseY > winHeight - PAN_MOUSE_EDGE_BUFFER)
            panCam(0, -PAN_KEY_STEP * 2);
         */
    }

    // prevents stuff like fire and water effects being shown on your HUD
    @SubscribeEvent
    public static void onRenderBlockOverlay(RenderBlockOverlayEvent evt) {
        if (enabled)
            evt.setCanceled(true);
    }
    
    @SubscribeEvent
    public static void onMouseClick(GuiScreenEvent.MouseClickedEvent evt) {
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
    public static void onMouseRelease(GuiScreenEvent.MouseReleasedEvent evt) {
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
    public static void onMouseDrag(GuiScreenEvent.MouseDragEvent evt) {
        if (!enabled) return;

        if (evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_1 && Keybinds.altMod.isDown()) {
            cameraMovingByMouse = true;
            float moveX = (float) evt.getDragX() * CAMPAN_MOUSE_SENSITIVITY * (zoom/ZOOM_MAX) * ((float) screenWidth / winWidth);
            float moveZ = (float) evt.getDragY() * CAMPAN_MOUSE_SENSITIVITY * (zoom/ZOOM_MAX) * ((float) screenHeight / winHeight);
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

    @SubscribeEvent
    public static void onFovModifier(EntityViewRenderEvent.FOVModifier evt) {
        if (enabled)
            evt.setFOV(180);
    }

    // on each game render frame
    @SubscribeEvent
    public static void onFogDensity(EntityViewRenderEvent.FogDensity evt) {
        if (!enabled)
            return;

        winWidth = MC.getWindow().getGuiScaledWidth();
        winHeight = MC.getWindow().getGuiScaledHeight();
        screenWidth = MC.getWindow().getScreenWidth();
        screenHeight = MC.getWindow().getScreenHeight();

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
            player.setXRot((float) -camRotY - camRotAdjY);
            player.setYRot((float) -camRotX - camRotAdjX);
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
