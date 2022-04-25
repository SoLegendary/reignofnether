package com.solegendary.reignofnether.cursor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.hud.ActionButtons;
import com.solegendary.reignofnether.hud.ActionName;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.units.Unit;
import com.solegendary.reignofnether.units.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.event.DrawSelectionEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import static net.minecraft.util.Mth.*;


import java.util.ArrayList;
import java.util.List;

/**
 * Handler that implements and manages screen-to-world translations of the cursor and block/entity selection
 */
public class CursorClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    private static boolean leftClickDown = false;
    private static boolean rightClickDown = false;

    // pos of block moused over or inside a box select
    // currently blocks are not fully selectable
    private static BlockPos preselectedBlockPos = new BlockPos(0,0,0);
    // pos of cursor exactly on: first non-air block, last frame, near to screen, far from screen
    private static Vector3d cursorWorldPos = new Vector3d(0,0,0);
    private static Vector3d cursorWorldPosLast = new Vector3d(0,0,0);
    // pos of cursor on screen for box selections
    private static Vec2 cursorLeftClickDownPos = new Vec2(-1,-1);
    private static Vec2 cursorLeftClickDragPos = new Vec2(-1,-1);
    // attack that is performed on the next left click
    private static ActionName leftClickAction = null;

    public static Vector3d getCursorWorldPos() {
        return cursorWorldPos;
    }
    public static BlockPos getPreselectedBlockPos() {
        return preselectedBlockPos;
    }
    public static ActionName getLeftClickAction() {
        return leftClickAction;
    }
    public static void setLeftClickAction(ActionName actionName) {
        if (UnitClientEvents.getSelectedUnitIds().size() > 0)
            leftClickAction = actionName;
    }

    private static final ResourceLocation TEXTURE_CURSOR = new ResourceLocation("reignofnether", "textures/cursors/customcursor.png");
    private static final ResourceLocation TEXTURE_HAND = new ResourceLocation("reignofnether", "textures/cursors/customcursor_hand.png");
    private static final ResourceLocation TEXTURE_HAND_GRAB = new ResourceLocation("reignofnether", "textures/cursors/customcursor_hand_grab.png");
    private static final ResourceLocation TEXTURE_SWORD = new ResourceLocation("reignofnether", "textures/cursors/customcursor_sword.png");
    private static final ResourceLocation TEXTURE_CROSS = new ResourceLocation("reignofnether", "textures/cursors/customcursor_cross.png");

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.DrawScreenEvent evt) {

        String screenName = evt.getScreen().getTitle().getString();
        long window = MC.getWindow().getWindow();

        if (!OrthoviewClientEvents.isEnabled() || !screenName.equals("topdowngui_container")) {
            if (GLFW.glfwRawMouseMotionSupported()) // raw mouse increases sensitivity massively for some reason
                GLFW.glfwSetInputMode(window, GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_TRUE);
            return;
        }
        if (MC.player == null || MC.level == null) return;

        // ************************************
        // Manage cursor icons based on actions
        // ************************************

        // hides default cursor and locks it to the window to allow edge panning
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        // raw mouse increases sensitivity massively for some reason
        GLFW.glfwSetInputMode(window, GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_FALSE);

        // blitting like this will cause it to be rendered 1 frame behind realtime (this hopefully shouldn't be noticeable...)
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // draw at edge of screen even if mouse is off it
        int cursorDrawX = Math.min(evt.getMouseX(), MC.getWindow().getGuiScaledWidth() - 5);
        int cursorDrawY = Math.min(evt.getMouseY(), MC.getWindow().getGuiScaledHeight() - 5);
        cursorDrawX = Math.max(0,cursorDrawX);
        cursorDrawY = Math.max(0,cursorDrawY);

        if (Keybinds.altMod.isDown() && (leftClickDown || rightClickDown))
            RenderSystem.setShaderTexture(0, TEXTURE_HAND_GRAB);
        else if (Keybinds.altMod.isDown())
            RenderSystem.setShaderTexture(0, TEXTURE_HAND);
        else if (leftClickAction != null && leftClickAction.equals(ActionName.ATTACK))
            RenderSystem.setShaderTexture(0, TEXTURE_SWORD);
        else if (leftClickAction != null) {
            RenderSystem.setShaderTexture(0, TEXTURE_CROSS);
            cursorDrawX -= 8;
            cursorDrawY -= 8;
        }
        else
            RenderSystem.setShaderTexture(0, TEXTURE_CURSOR);

        GuiComponent.blit(evt.getPoseStack(),
                cursorDrawX, cursorDrawY,
                16,
                16, 16,
                16, 16,
                16,16
        );

        // ***********************************************
        // Convert cursor on-screen 2d pos to world 3d pos
        // ***********************************************
        cursorWorldPosLast = new Vector3d(
                cursorWorldPos.x,
                cursorWorldPos.y,
                cursorWorldPos.z
        );
        cursorWorldPos = MiscUtil.screenPosToWorldPos(MC, evt.getMouseX(), evt.getMouseY());

        // calc near and far cursorWorldPos to get a cursor line vector
        Vector3d lookVector = getPlayerLookVector();
        Vector3d cursorWorldPosNear = MyMath.addVector3d(cursorWorldPos, lookVector, -200);
        Vector3d cursorWorldPosFar = MyMath.addVector3d(cursorWorldPos, lookVector, 200);

        // only spend time doing refining calcs for if we actually moved the cursor (or if this is the first time)
        if (cursorWorldPos.x != cursorWorldPosLast.x || cursorWorldPos.y != cursorWorldPosLast.y || cursorWorldPos.z != cursorWorldPosLast.z) {

            Vec3 hitPos = getRefinedCursorWorldPos(cursorWorldPosNear, cursorWorldPosFar);
            cursorWorldPos = new Vector3d(hitPos.x, hitPos.y, hitPos.z);
            preselectedBlockPos = new BlockPos(hitPos);

            // if we clipped a non-solid block (eg. tall grass) search adjacent blocks for a next-best match
            if (!MC.level.getBlockState(preselectedBlockPos).getMaterial().isSolidBlocking()) {
                preselectedBlockPos = getRefinedBlockPos(preselectedBlockPos, cursorWorldPosNear);
            }
        }

        // TODO: make this be CursorEntity and only show when moving a mob instead of following cursor
        //CursorServerEvents.moveCursorEntity(cursorWorldPos);

        // ****************************************
        // Find entity moused over and/or selected
        // ****************************************
        List<PathfinderMob> entities = MiscUtil.getEntitiesWithinRange(cursorWorldPos, 100, PathfinderMob.class, MC.level);

        UnitClientEvents.setPreselectedUnitIds(new ArrayList<>());

        for (PathfinderMob entity : entities) {
            // inflate by set amount to improve click accuracy
            AABB entityaabb = entity.getBoundingBox().inflate(0.1);

            if (MyMath.rayIntersectsAABBCustom(cursorWorldPosNear, getPlayerLookVector(), entityaabb)) {
                UnitClientEvents.addPreselectedUnitId(entity.getId());
                break; // only allow one moused unit at a time
            }
        }

        // *******************************
        // Calculate box-preselected units
        // *******************************
        // weird bug when downPos == dragPos makes random entities get selected by this algorithm
        float dist = cursorLeftClickDownPos.distanceToSqr(cursorLeftClickDragPos);

        if (leftClickDown && dist > 0 && !Keybinds.altMod.isDown()) {

            // can't use AABB here as it's always axis-aligned (ie. no camera-rotation)
            // instead, improvise our own quad
            // https://math.stackexchange.com/questions/1472049/check-if-a-point-is-inside-a-rectangular-shaped-area-3d

            // calculate 4 vertices
            Vector3d worldPosTL = MiscUtil.screenPosToWorldPos(MC, (int) cursorLeftClickDownPos.x, (int) cursorLeftClickDownPos.y); // top-left
            Vector3d worldPosBL = MiscUtil.screenPosToWorldPos(MC, (int) cursorLeftClickDownPos.x, (int) cursorLeftClickDragPos.y); // bottom-left
            Vector3d worldPosBR = MiscUtil.screenPosToWorldPos(MC, (int) cursorLeftClickDragPos.x, (int) cursorLeftClickDragPos.y); // bottom-right

            Vector3d vp5 = MyMath.addVector3d(worldPosTL, lookVector, -200);
            Vector3d vp1 = MyMath.addVector3d(worldPosBL, lookVector, -200);
            Vector3d vp4 = MyMath.addVector3d(worldPosBR, lookVector, -200);
            Vector3d vp2 = MyMath.addVector3d(worldPosBL, lookVector, 200);

            // convert all to Vec3s so we can do math without modifying in-place
            Vec3 p5 = new Vec3(vp5.x, vp5.y, vp5.z);
            Vec3 p1 = new Vec3(vp1.x, vp1.y, vp1.z);
            Vec3 p4 = new Vec3(vp4.x, vp4.y, vp4.z);
            Vec3 p2 = new Vec3(vp2.x, vp2.y, vp2.z);

            Vec3 u = p1.subtract(p4).cross(p1.subtract(p5));
            Vec3 v = p1.subtract(p2).cross(p1.subtract(p5));
            Vec3 w = p1.subtract(p2).cross(p1.subtract(p4));

            for (PathfinderMob entity : entities) {
                Vec3 x = entity.getBoundingBox().getCenter();

                double ux = u.dot(x);
                double vx = v.dot(x);
                double wx = w.dot(x);

                if (MyMath.isBetween(u.dot(p1), ux, u.dot(p2)) &&
                    MyMath.isBetween(v.dot(p1), vx, v.dot(p4)) &&
                    MyMath.isBetween(w.dot(p1), wx, w.dot(p5))) {
                    UnitClientEvents.addPreselectedUnitId(entity.getId());
                }
            }
        }
    }

    public static boolean isBoxSelecting() {
        return cursorLeftClickDownPos.x >= 0 &&
                cursorLeftClickDownPos.y >= 0 &&
                cursorLeftClickDragPos.x >= 0 &&
                cursorLeftClickDragPos.y >= 0;
    }

    // draw box selection rectangle
    @SubscribeEvent
    public static void renderOverlay(RenderGameOverlayEvent.Post evt) {

        if (leftClickDown && !Keybinds.altMod.isDown()) {
            GuiComponent.fill(evt.getMatrixStack(), // x1,y1, x2,y2,
                    Math.round(cursorLeftClickDownPos.x),
                    Math.round(cursorLeftClickDownPos.y),
                    Math.round(cursorLeftClickDragPos.x),
                    Math.round(cursorLeftClickDragPos.y),
                    0x0341e868); //ARGB(hex); note that alpha ranges between ~0-16, not 0-255
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseClickedEvent.Post evt) {
        // don't box selecrt
        if (!OrthoviewClientEvents.isEnabled() ||
            MinimapClientEvents.isPointInsideMinimap(evt.getMouseX(), evt.getMouseY()))
            return;

        // select a moused over entity by left clicking it
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            cursorLeftClickDownPos = new Vec2(floor(evt.getMouseX()), floor(evt.getMouseY()));
            cursorLeftClickDragPos = new Vec2(floor(evt.getMouseX()), floor(evt.getMouseY()));
            leftClickDown = true;
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            rightClickDown = true;
        }
    }

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragEvent.Pre evt) {
        if (!OrthoviewClientEvents.isEnabled() ||
            (cursorLeftClickDownPos.x < 0 && cursorLeftClickDownPos.y < 0))
            return;

        cursorLeftClickDragPos = new Vec2(floor(evt.getMouseX()), floor(evt.getMouseY()));
    }

    @SubscribeEvent
    public static void onMouseRelease(ScreenEvent.MouseReleasedEvent.Post evt) {
        if (!OrthoviewClientEvents.isEnabled()) return;

        // select a moused over entity by left clicking it
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            leftClickDown = false;

            // enact box selection, excluding non-unit mobs
            // for single-click selection, see UnitCommonVanillaEvents
            // except if attack-moving or nothing is preselected (to prevent deselection)
            ArrayList<Integer> preselectedUnitIds = UnitClientEvents.getPreselectedUnitIds();
            if (preselectedUnitIds.size() > 0 && !cursorLeftClickDownPos.equals(cursorLeftClickDragPos)) {
                if (!Keybinds.shiftMod.isDown())
                    UnitClientEvents.setSelectedUnitIds(new ArrayList<>());
                for (int unitId : preselectedUnitIds) {
                    Entity entity = MC.level.getEntity(unitId);
                    if (entity instanceof Unit)
                        UnitClientEvents.addSelectedUnitId(entity.getId());
                }
            }
            cursorLeftClickDownPos = new Vec2(-1,-1);
            cursorLeftClickDragPos = new Vec2(-1,-1);
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            rightClickDown = false;
        }
    }

    // prevent moused over blocks being outlined in the usual way (ie. by raytracing from player to block)
    @SubscribeEvent
    public static void onHighlightBlockEvent(DrawSelectionEvent.HighlightBlock evt) {
        if (MC.level != null && OrthoviewClientEvents.isEnabled())
            evt.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelLastEvent evt) {
        if (MC.level != null && OrthoviewClientEvents.isEnabled()) {

            if (!OrthoviewClientEvents.isCameraMovingByMouse() && !leftClickDown &&
                    UnitClientEvents.getSelectedUnitIds().size() > 0 &&
                    UnitClientEvents.getPreselectedUnitIds().size() <= 0) {
                MyRenderer.drawBlockOutline(evt.getPoseStack(), preselectedBlockPos, rightClickDown ? 1.0f : 0.5f);
            }
        }
    }

    // gets the unit vector in the direction of player facing (same as camera)
    // calcs from https://stackoverflow.com/questions/65897792/3d-vector-coordinates-from-x-and-y-rotation
    public static Vector3d getPlayerLookVector() {
        float a = (float) Math.toRadians(MC.player.getYRot());
        float b = (float) Math.toRadians(MC.player.getXRot());
        return new Vector3d(-cos(b) * sin(a), -sin(b), cos(b) * cos(a));
    }

    // returns the exact spot that the cursorWorldPos ray meets a solid block
    public static Vec3 getRefinedCursorWorldPos(Vector3d cursorWorldPosNear, Vector3d cursorWorldPosFar) {
        Vec3 vectorNear = new Vec3(cursorWorldPosNear.x, cursorWorldPosNear.y, cursorWorldPosNear.z);
        Vec3 vectorFar = new Vec3(cursorWorldPosFar.x, cursorWorldPosFar.y, cursorWorldPosFar.z);

        // clip() returns the point of clip, not the clipped block giving off-by-one errors so move slightly to compensate
        HitResult hitResult = MC.level.clip(new ClipContext(vectorNear, vectorFar, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, null));
        return hitResult.getLocation().add(new Vec3(-0.001,-0.001,-0.001));
    }

    private static BlockPos getRefinedBlockPos(BlockPos bp, Vector3d cursorWorldPosNear) {
        ArrayList<BlockPos> blocks = new ArrayList<>();

        blocks.add(bp);
        blocks.add(bp.north());
        blocks.add(bp.south());
        blocks.add(bp.east());
        blocks.add(bp.west());
        blocks.add(bp.north().east());
        blocks.add(bp.south().east());
        blocks.add(bp.north().west());
        blocks.add(bp.south().west());

        blocks.add(bp.above());
        blocks.add(bp.above().north());
        blocks.add(bp.above().south());
        blocks.add(bp.above().east());
        blocks.add(bp.above().west());
        blocks.add(bp.above().north().east());
        blocks.add(bp.above().south().east());
        blocks.add(bp.above().north().west());
        blocks.add(bp.above().south().west());

        blocks.add(bp.below());
        blocks.add(bp.below().north());
        blocks.add(bp.below().south());
        blocks.add(bp.below().east());
        blocks.add(bp.below().west());
        blocks.add(bp.below().north().east());
        blocks.add(bp.below().south().east());
        blocks.add(bp.below().north().west());
        blocks.add(bp.below().south().west());

        BlockPos bestBp = bp;
        double smallestDist = 10000;
        Vector3d lookVector = getPlayerLookVector();

        for (BlockPos block : blocks) {
            double dist = new Vec3(block.getX(), block.getY(), block.getZ())
                    .distanceTo(new Vec3(cursorWorldPosNear.x, cursorWorldPosNear.y, cursorWorldPosNear.z));
            if (MC.level.getBlockState(block).getMaterial().isSolidBlocking()
                    && MyMath.rayIntersectsAABBCustom(cursorWorldPosNear, lookVector, new AABB(block))
                    && dist < smallestDist ) {
                smallestDist = dist;
                bestBp = block;
            }
        }
        return bestBp;
    }
}


