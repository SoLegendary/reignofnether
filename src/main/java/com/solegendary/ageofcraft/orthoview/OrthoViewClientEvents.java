package com.solegendary.ageofcraft.orthoview;

import com.mojang.math.Vector3d;
import com.solegendary.ageofcraft.cursorentity.CursorEntityCommonEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.fmlclient.registry.ClientRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import com.solegendary.ageofcraft.gui.TopdownGuiContainer;
import com.mojang.math.Matrix4f;

import java.nio.FloatBuffer;

import static net.minecraft.util.Mth.cos;
import static net.minecraft.util.Mth.sin;
import static net.minecraft.util.Mth.sign;

/**
 * Handler that implements and manages hotkeys for the orthographic camera.
 *
 * @author SoLegendary, adapted from Mineshot by Nico Bergemann <barracuda415 at yahoo.de>
 */
public class OrthoViewClientEvents {

    public static boolean enabled = false;
    public static BlockPos mouseoverPos = new BlockPos(0,0,0);

    private static Vector3d cursorPos = new Vector3d(0,0,0);
    private static Vector3d cursorPosLast = new Vector3d(0,0,0);

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
    private static final float CAMROTY_MAX = -20;
    private static final float CAMROTY_MIN = -90;
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

    private static float zoom = 30; // * 2 = number of blocks in height
    private static float camRotX = 0;
    private static float camRotY = -45;
    private static float camRotAdjX = 0;
    private static float camRotAdjY = 0;
    private static boolean mouseLeftDown = false;
    private static boolean mouseRightDown = false;
    private static float mouseRightDownX = 0;
    private static float mouseRightDownY = 0;
    private static float mouseLeftDownX = 0;
    private static float mouseLeftDownY = 0;
    private static int winWidth = MC.getWindow().getGuiScaledWidth();
    private static int winHeight = MC.getWindow().getGuiScaledHeight();
    private static int screenWidth = MC.getWindow().getScreenWidth();
    private static int screenHeight = MC.getWindow().getScreenHeight();

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
        if (evt.getGui() != null)
            return evt.getGui().getTitle().getString().equals(TopdownGuiContainer.TITLE.getString());
        else
            return false;
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

        if (MC.player != null) {

            // at winHeight=240, zoom=10, screen is 20 blocks high, so PTB=240/20=24
            float pixelsToBlocks = winHeight / zoom;

            // make mouse coordinate origin centre of screen
            float x = (mouseX - (float) winWidth / 2) / pixelsToBlocks;
            float y = 0;
            float z = (mouseY - (float) winHeight / 2) / pixelsToBlocks;

            double camRotYRads = Math.toRadians(camRotY);
            z = z / (float) (Math.sin(-camRotYRads));

            // get look vector of the player (and therefore the camera)
            // calcs from https://stackoverflow.com/questions/65897792/3d-vector-coordinates-from-x-and-y-rotation
            float a = (float) Math.toRadians(MC.player.getYRot());
            float b = (float) Math.toRadians(MC.player.getXRot());
            final Vector3d lookVector = new Vector3d(-cos(b) * sin(a), -sin(b), cos(b) * cos(a));

            Vec2 XZRotated = rotateCoords(x, z);

            cursorPosLast = new Vector3d(
                    cursorPos.x,
                    cursorPos.y,
                    cursorPos.z
            );
            cursorPos = new Vector3d(
                    MC.player.xo - XZRotated.x,
                    MC.player.yo + y,
                    MC.player.zo - XZRotated.y
            );

            // only spend time doing calcs if we actually moved the cursor
            if (cursorPos.x != cursorPosLast.x || cursorPos.y != cursorPosLast.y || cursorPos.z != cursorPosLast.z) {

                System.out.println(cursorPos);
                System.out.println(cursorPosLast);

                // if we add a multiple of the lookVector, we can 'raytrace' back and forth from the camera without
                // changing the on-screen position of the cursorEntity
                boolean lastBlockSolid = false;
                double vectorScale = -50;
                Vector3d lookVectorScaled;
                BlockPos bp;
                BlockState bs;

                while (true) {
                    Vector3d searchVec = new Vector3d(0,0,0);
                    searchVec.set(cursorPos);

                    lookVectorScaled = new Vector3d(0,0,0);
                    lookVectorScaled.set(lookVector);
                    lookVectorScaled.scale(vectorScale); // has to be high enough to be at the 'front' of the screen
                    searchVec.add(lookVectorScaled);

                    bp = new BlockPos(searchVec.x, searchVec.y, searchVec.z);
                    bs = MC.level.getBlockState(bp);

                    if (!bs.isAir()) {
                        mouseoverPos = bp;
                        //System.out.println("Set new position:");
                        //System.out.println(mouseoverPos.toShortString());
                        break;
                    }
                    vectorScale += 1;
                }

                // subtract to have the cursorentity always show at the front of the screen
                Vector3d cursorPosFront = new Vector3d(
                        cursorPos.x,
                        cursorPos.y,
                        cursorPos.z
                );
                cursorPosFront.add(lookVectorScaled);
                CursorEntityCommonEvents.moveCursorEntity(cursorPosFront);
            }
        }
    }

    // prevents stuff like fire and water effects being shown on your HUD
    @SubscribeEvent
    public static void onRenderBlockOverlay(RenderBlockOverlayEvent evt) {
        if (enabled)
            evt.setCanceled(true);
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
            rotateCam(camRotAdjX,camRotAdjY);
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
