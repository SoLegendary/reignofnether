package com.solegendary.ageofcraft.orthoview;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.Arrays;

import static net.minecraft.util.math.MathHelper.cos;
import static net.minecraft.util.math.MathHelper.sin;
import static net.minecraft.util.math.MathHelper.sign;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;

/**
 * Handler that implements and manages hotkeys for the orthographic camera.
 *
 * @author SoLegendary, adapted from Mineshot by Nico Bergemann <barracuda415 at yahoo.de>
 */
public class OrthoViewClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final String KEY_CATEGORY = "key.categories.ageofcraft";
    private static final float ZOOM_STEP_KEY = 5;
    private static final float ZOOM_STEP_SCROLL = 1;
    private static final float ZOOM_MIN = 10;
    private static final float ZOOM_MAX = 90;
    private static final float ROTATE_STEP_X = 0.35f;
    private static final float ROTATE_STEP_Y = 0.35f;
    private static final float CAMROTY_MAX = 70;
    private static final float CAMROTY_MIN = 0;
    private static final float CAMROT_MOUSE_SENSITIVITY_X = 0.12f;
    private static final float CAMROT_MOUSE_SENSITIVITY_Y = 0.12f;
    private static final float CAM_ENTITY_XY_STEP = 1;
    private static final float CAMPAN_MOUSE_SENSITIVITY_X = 0.2f;
    private static final float CAMPAN_MOUSE_SENSITIVITY_Z = 0.2f;

    private static final KeyBinding keyBindToggle = new KeyBinding("key.ageofcraft.orthoview.toggle", GLFW.GLFW_KEY_KP_5, KEY_CATEGORY);
    private static final KeyBinding keyBindZoomIn = new KeyBinding("key.ageofcraft.orthoview.zoom_in", GLFW.GLFW_KEY_KP_ADD, KEY_CATEGORY);
    private static final KeyBinding keyBindZoomOut = new KeyBinding("key.ageofcraft.orthoview.zoom_out", GLFW.GLFW_KEY_KP_SUBTRACT, KEY_CATEGORY);
    private static final KeyBinding keyBindPlusY = new KeyBinding("key.ageofcraft.orthoview.plusX", GLFW.GLFW_KEY_UP, KEY_CATEGORY);
    private static final KeyBinding keyBindMinusY = new KeyBinding("key.ageofcraft.orthoview.minusX", GLFW.GLFW_KEY_DOWN, KEY_CATEGORY);
    private static final KeyBinding keyBindPlusX = new KeyBinding("key.ageofcraft.orthoview.plusY", GLFW.GLFW_KEY_LEFT, KEY_CATEGORY);
    private static final KeyBinding keyBindMinusX = new KeyBinding("key.ageofcraft.orthoview.minusY", GLFW.GLFW_KEY_RIGHT, KEY_CATEGORY);
    private static final KeyBinding keyBindReset = new KeyBinding("key.ageofcraft.orthoview.reset", GLFW.GLFW_KEY_RIGHT_CONTROL, KEY_CATEGORY);

    private static boolean isRegistered = false;

    private static void registerKeybinds() {
        ClientRegistry.registerKeyBinding(keyBindToggle);
        ClientRegistry.registerKeyBinding(keyBindZoomIn);
        ClientRegistry.registerKeyBinding(keyBindZoomOut);
        ClientRegistry.registerKeyBinding(keyBindPlusX);
        ClientRegistry.registerKeyBinding(keyBindMinusX);
        ClientRegistry.registerKeyBinding(keyBindPlusY);
        ClientRegistry.registerKeyBinding(keyBindMinusY);
        ClientRegistry.registerKeyBinding(keyBindReset);
        isRegistered = true;
    }

    private static boolean enabled;
    private static float zoom = 30;
    private static float camRotX = 0;
    private static float camRotY = 45;
    private static float camRotAdjX = 0;
    private static float camRotAdjY = 0;

    private static final Integer keyToggle = GLFW.GLFW_KEY_KP_5;
    private static final Integer keyZoomIn = GLFW.GLFW_KEY_KP_ADD;
    private static final Integer keyZoomOut = GLFW.GLFW_KEY_KP_SUBTRACT;
    private static final Integer keyPlusY = GLFW.GLFW_KEY_UP;
    private static final Integer keyMinusY = GLFW.GLFW_KEY_DOWN;
    private static final Integer keyPlusX = GLFW.GLFW_KEY_LEFT;
    private static final Integer keyMinusX = GLFW.GLFW_KEY_RIGHT;
    private static final Integer keyReset = GLFW.GLFW_KEY_RIGHT_CONTROL;

    private static void reset() {
        zoom = 30;
        camRotX = 0;
        camRotY = 45;
    }

    @SubscribeEvent
    public static void onFovModifier(EntityViewRenderEvent.FOVModifier evt) {
        if (enabled) {
            evt.setFOV(180);
        }
    }

    // only fires on key down, only when a gui screen is up
    @SubscribeEvent
    public static void onKeyPressed (GuiScreenEvent.KeyboardKeyPressedEvent.Pre evt) {
        Integer keyPressed = evt.getKeyCode();

        PlayerEntity player = MC.player;
        if (keyPressed.equals(keyToggle)) {
            enabled = !enabled;
        }
        if (keyPressed.equals(keyReset)) {
            reset();
        }
    }

    public static void rotateCamX(float x) {
        camRotX += x;
        if (camRotX >= 360)
            camRotX -= 360;
        if (camRotX <= -360)
            camRotX += 360;
    }
    public static void rotateCamY(float y) {
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

    static boolean mouseLeftDown = false;
    static boolean mouseRightDown = false;
    static float mouseRightDownX = 0;
    static float mouseRightDownY = 0;
    static float mouseLeftDownX = 0;
    static float mouseLeftDownY = 0;

    @SubscribeEvent
    public static void onMouseScroll(GuiScreenEvent.MouseScrollEvent evt) {
        zoomCam((float) sign(evt.getScrollDelta()) * -ZOOM_STEP_SCROLL);
    }
    @SubscribeEvent
    public static void onMouseClick(GuiScreenEvent.MouseClickedEvent evt) {
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            mouseLeftDown = true;
            mouseLeftDownX = (float) evt.getMouseX();
            mouseLeftDownY = (float) evt.getMouseY();
        }
        else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            mouseRightDown = true;
            mouseRightDownX = (float) evt.getMouseX();
            mouseRightDownY = (float) evt.getMouseY();
        }
    }
    @SubscribeEvent
    public static void onMouseRelease(GuiScreenEvent.MouseReleasedEvent evt) {
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            mouseLeftDown = false;
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            mouseRightDown = false;
            rotateCamX(camRotAdjX);
            rotateCamY(camRotAdjY);
            camRotAdjX = 0;
            camRotAdjY = 0;
        }
    }
    @SubscribeEvent
    public static void onMouseDrag(GuiScreenEvent.MouseDragEvent evt) {
        if (evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_1) {

            if (MC.player != null) {
                float camXRotRads = (float) Math.toRadians(-camRotX - camRotAdjX);

                // formula for rotating XY coords (where f is degree of rotation)
                // x' = x cos f - y sin f
                // y' = y cos f + x sin f
                float moveX = (float) evt.getDragX() * CAMPAN_MOUSE_SENSITIVITY_X * (zoom/ZOOM_MAX);
                float moveZ = (float) evt.getDragY() * CAMPAN_MOUSE_SENSITIVITY_Z * (zoom/ZOOM_MAX);
                float moveXRotated = (moveX * cos(camXRotRads)) - (moveZ * sin(camXRotRads));
                float moveZRotated = (moveZ * cos(camXRotRads)) + (moveX * sin(camXRotRads));

                MC.player.move(MoverType.SELF, new Vector3d(moveXRotated, 0, moveZRotated));
            }
        }
        if (evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            camRotAdjX = (float) (evt.getMouseX() - mouseRightDownX) * CAMROT_MOUSE_SENSITIVITY_X;
            camRotAdjY = (float) -(evt.getMouseY() - mouseRightDownY) * CAMROT_MOUSE_SENSITIVITY_Y;

            if (camRotY + camRotAdjY > CAMROTY_MAX)
                camRotAdjY = CAMROTY_MAX - camRotY;
            if (camRotY + camRotAdjY < CAMROTY_MIN)
                camRotAdjY = CAMROTY_MIN - camRotY;
        }
    }

    // fires on key down once, then repeats if held after a delay like when typing, only when a gui screen is up
    // can use screen.keyPressed() to
    @SubscribeEvent
    public static void onKeyPressed(GuiScreenEvent.KeyboardKeyPressedEvent evt) {
        Integer keyPressed = evt.getKeyCode();

        // update stepped rotation/zoom controls
        if (keyPressed.equals(keyZoomIn))
            zoomCam(-ZOOM_STEP_KEY);
        if (keyPressed.equals(keyZoomOut))
            zoomCam(ZOOM_STEP_KEY);

        /*
        if (keyPressed.equals(keyPlusX))
            rotateCamX(ROTATE_STEP_X);
        else if (keyPressed.equals(keyMinusX))
            rotateCamX(-ROTATE_STEP_X);
        if (keyPressed.equals(keyPlusY))
            rotateCamY(ROTATE_STEP_Y);
        else if (keyPressed.equals(keyMinusY))
            rotateCamY(-ROTATE_STEP_Y);
         */

        // push is equivalent of addVelocity in older versions
        /*
        if (keyPressed.equals(keyPlusX))
            MC.player.push(1,0,0);
        else if (keyPressed.equals(keyMinusX))
            MC.player.push(-1,0,0);
        if (keyPressed.equals(keyPlusY))
            MC.player.push(0,0,1);
        else if (keyPressed.equals(keyMinusY))
            MC.player.push(0,0,-1);
         */
    }

    public static final FloatBuffer projection = GLAllocation.createFloatBuffer(16);
    public static final FloatBuffer modelview = GLAllocation.createFloatBuffer(16);

    // In EntityViewRenderEvent.CameraSetup compute whatever frustum you want and set MC.levelRenderer.capturedFrustum
    // to it. Then the game will use that frustum instead of whatever it computes based on player position.
    @SubscribeEvent
    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup evt) {

        //Matrix4f projectionMatrix = new Matrix4f(projection.asReadOnlyBuffer().array());
        //Matrix4f modelViewMatrix = new Matrix4f(modelview.asReadOnlyBuffer().array());

        //System.out.println(projectionMatrix);
        //System.out.println(modelViewMatrix);

        //ClippingHelper orthoFrustum = new ClippingHelper(null, null);
        //MC.levelRenderer.capturedFrustum = orthoFrustum;
    }

    // on what I assume is whenever the view distance fog changes (ie. whenever camera moves or rotates at all)
    @SubscribeEvent
    public static void onFogDensity(EntityViewRenderEvent.FogDensity evt) {
        if (!enabled)
            return;
        if (!isRegistered)
            registerKeybinds();

        float width = zoom * (MC.getWindow().getWidth() / (float) MC.getWindow().getHeight());
        float height = zoom;

        // override projection matrix
        RenderSystem.matrixMode(GL_PROJECTION);
        RenderSystem.loadIdentity();

        // actually apply the orthogonal camera settings on the GL_PROJECTION matrix
        //GL11.glTranslated(0, 0, 0);
        GL11.glScaled(1, 1, 1);
        GL11.glOrtho(-width, width, -height, height,-9999, 9999);

        // the actual rendering camera is no longer tied to this client entity camera (ie. the player 1st person view)
        // but frustum culling is so to solve that we set FOV to max (180deg), point the viewing entity (player) looking
        // directly down so we render as much as possible, then manually rotate the camera separately.
        GL11.glRotated(-camRotY - camRotAdjY, 1,0,0);

        PlayerEntity player = MC.player;

        // rotate the player instead of GL11.glRotate so we still move with WASD in the expected directions
        // note that we treat x and y rot as horizontal and vertical, but MC treats it the other way around...
        if (player != null) {
            player.xRot = 90;
            player.yRot = (float) -camRotX - camRotAdjX;
        }


    }
}
