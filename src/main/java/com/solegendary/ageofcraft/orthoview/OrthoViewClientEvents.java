package com.solegendary.ageofcraft.orthoview;

import com.solegendary.ageofcraft.cursorentity.CursorEntityCommonEvents;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fmlclient.registry.ClientRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import com.solegendary.ageofcraft.gui.TopdownGuiContainer;

import static net.minecraft.util.Mth.cos;
import static net.minecraft.util.Mth.sin;
import static net.minecraft.util.Mth.sign;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;

/**
 * Handler that implements and manages hotkeys for the orthographic camera.
 *
 * @author SoLegendary, adapted from Mineshot by Nico Bergemann <barracuda415 at yahoo.de>
 */
public class OrthoViewClientEvents {

    private static final String topdownGuiName = "topdowngui_container";
    private static final Minecraft MC = Minecraft.getInstance();
    private static final String KEY_CATEGORY = "key.categories.ageofcraft";
    private static final float ZOOM_STEP_KEY = 5;
    private static final float ZOOM_STEP_SCROLL = 1;
    private static final float ZOOM_MIN = 10;
    private static final float ZOOM_MAX = 90;
    private static final float PAN_KEY_STEP = 0.5f;
    private static final float PAN_MOUSE_STEP = 0.5f;
    private static final float PAN_MOUSE_EDGE_BUFFER = 20; // if mouse < this distance from screen edge: start panning
    private static final float ROTATE_STEP = 0.35f;
    private static final float CAMROTY_MAX = 70;
    private static final float CAMROTY_MIN = 0;
    private static final float CAMROT_MOUSE_SENSITIVITY = 0.12f;
    private static final float CAMPAN_MOUSE_SENSITIVITY = 0.2f;

    private static final KeyMapping keyBindToggle = new KeyMapping("key.ageofcraft.orthoview.toggle", GLFW.GLFW_KEY_KP_5, KEY_CATEGORY);
    private static final KeyMapping keyBindZoomIn = new KeyMapping("key.ageofcraft.orthoview.zoomIn", GLFW.GLFW_KEY_KP_ADD, KEY_CATEGORY);
    private static final KeyMapping keyBindZoomOut = new KeyMapping("key.ageofcraft.orthoview.zoomOut", GLFW.GLFW_KEY_KP_SUBTRACT, KEY_CATEGORY);
    private static final KeyMapping keyBindRotPlusX = new KeyMapping("key.ageofcraft.orthoview.rotPlusY", GLFW.GLFW_KEY_LEFT, KEY_CATEGORY);
    private static final KeyMapping keyBindRotMinusX = new KeyMapping("key.ageofcraft.orthoview.rotMinusY", GLFW.GLFW_KEY_RIGHT, KEY_CATEGORY);
    private static final KeyMapping keyBindRotPlusY = new KeyMapping("key.ageofcraft.orthoview.rotPlusX", GLFW.GLFW_KEY_UP, KEY_CATEGORY);
    private static final KeyMapping keyBindRotMinusY = new KeyMapping("key.ageofcraft.orthoview.rotMinusX", GLFW.GLFW_KEY_DOWN, KEY_CATEGORY);
    private static final KeyMapping keyBindPanPlusX = new KeyMapping("key.ageofcraft.orthoview.panPlusZ", GLFW.GLFW_KEY_A, KEY_CATEGORY);
    private static final KeyMapping keyBindPanMinusX = new KeyMapping("key.ageofcraft.orthoview.panMinusZ", GLFW.GLFW_KEY_D, KEY_CATEGORY);
    private static final KeyMapping keyBindPanPlusZ = new KeyMapping("key.ageofcraft.orthoview.panPlusX", GLFW.GLFW_KEY_W, KEY_CATEGORY);
    private static final KeyMapping keyBindPanMinusZ = new KeyMapping("key.ageofcraft.orthoview.panMinusX", GLFW.GLFW_KEY_S, KEY_CATEGORY);
    private static final KeyMapping keyBindReset = new KeyMapping("key.ageofcraft.orthoview.reset", GLFW.GLFW_KEY_RIGHT_CONTROL, KEY_CATEGORY);
    private static final KeyMapping keyBindShiftMod = new KeyMapping("key.ageofcraft.orthoview.shiftMod", GLFW.GLFW_KEY_LEFT_SHIFT, KEY_CATEGORY);
    private static final KeyMapping keyBindCtrlMod = new KeyMapping("key.ageofcraft.orthoview.ctrlMod", GLFW.GLFW_KEY_LEFT_CONTROL, KEY_CATEGORY);
    public static boolean enabled = false;
    private static float zoom = 30; // * 2 = number of blocks in height
    private static float camRotX = 0;
    private static float camRotY = 45;
    private static float camRotAdjX = 0;
    private static float camRotAdjY = 0;
    private static boolean mouseLeftDown = false;
    private static boolean mouseRightDown = false;
    private static float mouseRightDownX = 0;
    private static float mouseRightDownY = 0;
    private static float mouseLeftDownX = 0;
    private static float mouseLeftDownY = 0;
    private static int winWidth = 0;
    private static int winHeight = 0;
    private static final int screenWidth = MC.getWindow().getScreenWidth();
    private static final int screenHeight = MC.getWindow().getScreenHeight();

    public static void init() {
        ClientRegistry.registerKeyBinding(keyBindToggle);
        ClientRegistry.registerKeyBinding(keyBindZoomIn);
        ClientRegistry.registerKeyBinding(keyBindZoomOut);
        ClientRegistry.registerKeyBinding(keyBindRotPlusX);
        ClientRegistry.registerKeyBinding(keyBindRotMinusX);
        ClientRegistry.registerKeyBinding(keyBindRotPlusY);
        ClientRegistry.registerKeyBinding(keyBindRotMinusY);
        ClientRegistry.registerKeyBinding(keyBindReset);
        ClientRegistry.registerKeyBinding(keyBindShiftMod);
        ClientRegistry.registerKeyBinding(keyBindCtrlMod);
    }

    private static void reset() {
        zoom = 30;
        camRotX = 0;
        camRotY = 45;
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
            Vec2 XZRotated = rotateCoords(x,z);
            MC.player.move(MoverType.SELF, new Vec3(XZRotated.x, 0, XZRotated.y));
        }
    }

    private static Vec2 rotateCoords(float x, float y) {
        float camXRotRads = (float) Math.toRadians(-camRotX - camRotAdjX);
        float moveXRotated = (x * cos(camXRotRads)) - (y * sin(camXRotRads));
        float moveyRotated = (y * cos(camXRotRads)) + (x * sin(camXRotRads));
        return new Vec2(moveXRotated, moveyRotated);
    }

    private static boolean isTopdownGui(GuiScreenEvent evt) {
        return evt.getGui().getTitle().getString().equals(TopdownGuiContainer.TITLE.getString());
    }

    public static boolean isEnabled() {
        return enabled;
    }

    // only fires on key down, only when a gui screen is up
    @SubscribeEvent
    public static void onKeyPressed (GuiScreenEvent.KeyboardKeyPressedEvent.Pre evt) {
        Integer keyPressed = evt.getKeyCode();

        // can't use keyBindToggle.isDown() as it doesn't happen on the same tick as this event
        if (keyPressed.equals(keyBindToggle.getKey().getValue()))
            enabled = !enabled;

        if (keyBindReset.isDown())
            reset();
    }
    @SubscribeEvent
    public static void onMouseScroll(GuiScreenEvent.MouseScrollEvent evt) {
        if (!isTopdownGui(evt) || !enabled) return;

        zoomCam((float) sign(evt.getScrollDelta()) * -ZOOM_STEP_SCROLL);
    }
    @SubscribeEvent
    public static void onDrawScreen(GuiScreenEvent.DrawScreenEvent evt) {
        if (!isTopdownGui(evt) || !enabled) return;

        if (keyBindShiftMod.isDown()) return;

        // no idea why but mouse x and y are half of what's expected
        int mouseX = evt.getMouseX();
        int mouseY = evt.getMouseY();

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

        if (MC.player != null) {

            // at winHeight=240, zoom=10, screen is 20 blocks high, so PTB=240/20=24
            float pixelsToBlocks = winHeight / zoom; // then /2 for blocks from centre of screen

            // make mouse coordinate origin centre of screen
            float x = (mouseX - (float) winWidth / 2) / (pixelsToBlocks / 2);
            float y = 0;
            float z = (mouseY - (float) winHeight / 2) / (pixelsToBlocks / 2);

            double camRotYRads = Math.toRadians(camRotY);
            //z += (z * Math.tan(camRotYRads) / 2);
            z =  z / (float) (Math.cos(camRotYRads));
            //z += zAdj;

            System.out.println(z);

            Vec2 XZRotated = rotateCoords(x,z);

            double xFinal = MC.player.xo - XZRotated.x;
            double yFinal = MC.player.yo + y;
            double zFinal = MC.player.zo - XZRotated.y;
            CursorEntityCommonEvents.moveCursorEntity(xFinal, yFinal, zFinal);

            //System.out.println(xFinal + " " + yFinal + " " + zFinal);
        }
    }
    @SubscribeEvent
    public static void onMouseClick(GuiScreenEvent.MouseClickedEvent evt) {
        if (!isTopdownGui(evt) || !enabled) return;

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
        if (!isTopdownGui(evt) || !enabled) return;

        // stop treating the rotation as adjustments and add them to the base amount
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            mouseLeftDown = false;
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            mouseRightDown = false;
            rotateCam(camRotAdjX,0);
            rotateCam(0,camRotAdjY);
            camRotAdjX = 0;
            camRotAdjY = 0;
        }
    }
    @SubscribeEvent
    public static void onMouseDrag(GuiScreenEvent.MouseDragEvent evt) {
        if (!isTopdownGui(evt) || !enabled) return;

        if (evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_1 && keyBindShiftMod.isDown()) {
            float moveX = (float) evt.getDragX() * CAMPAN_MOUSE_SENSITIVITY * (zoom/ZOOM_MAX) * ((float) screenWidth / winWidth);
            float moveZ = (float) evt.getDragY() * CAMPAN_MOUSE_SENSITIVITY * (zoom/ZOOM_MAX) * ((float) screenHeight / winHeight);
            panCam(moveX, moveZ);
        }
        if (evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_2 && keyBindShiftMod.isDown()) {
            camRotAdjX = (float) (evt.getMouseX() - mouseRightDownX) * CAMROT_MOUSE_SENSITIVITY;
            camRotAdjY = (float) -(evt.getMouseY() - mouseRightDownY) * CAMROT_MOUSE_SENSITIVITY;

            if (camRotY + camRotAdjY > CAMROTY_MAX)
                camRotAdjY = CAMROTY_MAX - camRotY;
            if (camRotY + camRotAdjY < CAMROTY_MIN)
                camRotAdjY = CAMROTY_MIN - camRotY;
        }
    }

    //public static final FloatBuffer projection = MemoryTracker.createFloatBuffer(16);
    //public static final FloatBuffer modelview = MemoryTracker.createFloatBuffer(16);
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
    @SubscribeEvent
    public static void onFovModifier(EntityViewRenderEvent.FOVModifier evt) {
        if (enabled)
            evt.setFOV(180);
    }
    // on what I assume is whenever the view distance fog changes (ie. whenever camera moves or rotates at all)
    @SubscribeEvent
    public static void onFogDensity(EntityViewRenderEvent.FogDensity evt) {
        if (!enabled)
            return;

        winWidth = MC.getWindow().getGuiScaledWidth();
        winHeight = MC.getWindow().getGuiScaledHeight();

        // zoom in/out with keys
        if (keyBindZoomIn.isDown())
            zoomCam(-ZOOM_STEP_KEY);
        if (keyBindZoomOut.isDown())
            zoomCam(ZOOM_STEP_KEY);

        // rotate with keys
        if (keyBindRotPlusX.isDown())
            rotateCam(ROTATE_STEP,0);
        else if (keyBindRotMinusX.isDown())
            rotateCam(-ROTATE_STEP,0);
        if (keyBindRotPlusY.isDown())
            rotateCam(0,ROTATE_STEP);
        else if (keyBindRotMinusY.isDown())
            rotateCam(0,-ROTATE_STEP);

        // pan camera with keys
        if (keyBindPanPlusX.isDown())
            panCam(PAN_KEY_STEP,0);
        else if (keyBindPanMinusX.isDown())
            panCam(-PAN_KEY_STEP,0);
        if (keyBindPanPlusZ.isDown())
            panCam(0, PAN_KEY_STEP);
        else if (keyBindPanMinusZ.isDown())
            panCam(0,-PAN_KEY_STEP);

        float width = zoom * (winWidth / (float) winHeight);
        float height = zoom;

        // override projection matrix
        //RenderSystem.matrixMode(GL_PROJECTION);
        //RenderSystem.loadIdentity();

        RenderSystem.projectionMatrix.setIdentity();

        // actually apply the orthogonal camera settings on the GL_PROJECTION matrix
        //GL11.glTranslated(0, 0, 0);
        GL11.glScaled(1, 1, 1);
        GL11.glOrtho(-width, width, -height, height,-9999, 9999);

        // the actual rendering camera is no longer tied to this client entity camera (ie. the player 1st person view)
        // but frustum culling is so to solve that we set FOV to max (180deg), point the viewing entity (player) looking
        // directly down so we render as much as possible, then manually rotate the camera separately.
        GL11.glRotated(-camRotY - camRotAdjY, 1,0,0);

        Player player = MC.player;

        // rotate the player instead of GL11.glRotate so we still move with WASD in the expected directions
        // note that we treat x and y rot as horizontal and vertical, but MC treats it the other way around...
        if (player != null) {
            player.xRotO = 90;
            player.yRotO = (float) -camRotX - camRotAdjX;
        }
    }
}
