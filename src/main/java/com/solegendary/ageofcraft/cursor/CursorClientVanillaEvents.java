package com.solegendary.ageofcraft.cursor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3d;
import com.solegendary.ageofcraft.orthoview.OrthoviewClientVanillaEvents;
import com.solegendary.ageofcraft.registrars.Keybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.event.DrawSelectionEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.systems.RenderSystem;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.minecraft.util.Mth.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Handler that implements and manages screen-to-world translations of the cursor and block/entity selection
 */
public class CursorClientVanillaEvents {

    private static boolean leftClickDown = false;
    private static boolean rightClickDown = false;

    // pos of block moused over or inside a box select
    private static BlockPos preselectedBlockPos = new BlockPos(0,0,0);
    // pos of block selected by right click (to move units to)
    private static BlockPos selectedBlockPos = new BlockPos(0,0,0);
    // pos of cursor exactly on: first non-air block, last frame, near to screen, far from screen
    private static Vector3d cursorWorldPos = new Vector3d(0,0,0);
    private static Vector3d cursorWorldPosLast = new Vector3d(0,0,0);
    // pos of cursor on screen for box selections
    private static Vec2 cursorLeftClickPos = new Vec2(0,0);
    private static Vec2 cursorPos = new Vec2(0,0);
    // entity moused over, vs entity selected by clicking
    private static Chicken preselectedEntity = null;
    private static Chicken selectedEntity = null;

    private static AABB boxSelectAABB = new AABB(0,0,0,0,0,0);

    private static final Minecraft MC = Minecraft.getInstance();

    public static Vector3d getCursorWorldPos() {
        return cursorWorldPos;
    }
    public static Chicken getPreselectedEntity() {
        return preselectedEntity;
    }
    public static Chicken getSelectedEntity() {
        return selectedEntity;
    }
    public static BlockPos getPreselectedBlockPos() {
        return preselectedBlockPos;
    }
    public static BlockPos getSelectedBlockPos() {
        return selectedBlockPos;
    }

    @SubscribeEvent
    public static void onDrawScreen(GuiScreenEvent.DrawScreenEvent evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        cursorPos = new Vec2((float) evt.getMouseX(), (float) evt.getMouseY());

        if (MC.player == null || MC.level == null) return;

        // ***********************************************
        // Convert cursor on-screen 2d pos to world 3d pos
        // ***********************************************
        cursorWorldPosLast = new Vector3d(
                cursorWorldPos.x,
                cursorWorldPos.y,
                cursorWorldPos.z
        );
        cursorWorldPos = convertScreenPosToWorldPos(evt.getMouseX(), evt.getMouseY());

        // calc near and far cursorWorldPos to get a cursor line vector
        Vector3d lookVector = getPlayerLookVector();
        Vector3d cursorWorldPosNear = addVector3d(cursorWorldPos, lookVector, -100);
        Vector3d cursorWorldPosFar = addVector3d(cursorWorldPos, lookVector, 100);

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
        //CursorCommonVanillaEvents.moveCursorEntity(cursorWorldPos);

        // ****************************************
        // Find entity moused over and/or selected
        // ****************************************
        AABB aabb = new AABB(
                cursorWorldPos.x - 5,
                cursorWorldPos.y - 5,
                cursorWorldPos.z - 5,
                cursorWorldPos.x + 5,
                cursorWorldPos.y + 5,
                cursorWorldPos.z + 5
        );
        List<Chicken> entities = MC.level.getEntitiesOfClass(Chicken.class, aabb);

        preselectedEntity = null;

        for (Chicken entity : entities) {
            // inflate by set amount to improve click accuracy
            AABB entityaabb = entity.getBoundingBox().inflate(0.1);

            if (rayIntersectsAABBCustom(cursorWorldPosNear, getPlayerLookVector(), entityaabb)) {
                preselectedEntity = entity;
            }
        }
    }

    // draw box selection rectangle
    @SubscribeEvent
    public static void renderOverlay(RenderGameOverlayEvent.Post evt) {
        if (leftClickDown && !Keybinds.shiftMod.isDown()) {
            GuiComponent.fill(evt.getMatrixStack(), // x1,y1, x2,y2,
                    Math.round(cursorLeftClickPos.x),
                    Math.round(cursorLeftClickPos.y),
                    Math.round(cursorPos.x),
                    Math.round(cursorPos.y),
                    0x0341e868); //ARGB(hex); note that alpha ranges between ~0-16, not 0-255
        }
    }

    @SubscribeEvent
    public static void onMobDeath(EntityLeaveWorldEvent evt) {
        if (preselectedEntity != null && evt.getEntity().getId() == preselectedEntity.getId())
            preselectedEntity = null;
        if (selectedEntity != null && evt.getEntity().getId() == selectedEntity.getId())
            selectedEntity = null;
    }

    @SubscribeEvent
    public static void onMouseClick(GuiScreenEvent.MouseClickedEvent evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        // select a moused over entity by left clicking it
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            cursorLeftClickPos = new Vec2((float) evt.getMouseX(), (float) evt.getMouseY());
            leftClickDown = true;
            if (preselectedEntity != null)
                selectedEntity = preselectedEntity;
            else
                selectedEntity = null;
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            rightClickDown = true;

            if (selectedEntity != null) {
                selectedBlockPos = preselectedBlockPos;
                System.out.println("Moving chicken (id " + selectedEntity.getId() + ") to " + selectedBlockPos);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseDrag(GuiScreenEvent.MouseDragEvent evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        // box select update AABB vertices
        if (evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_1 && !Keybinds.shiftMod.isDown()) {

            Vector3d lookVector = getPlayerLookVector();

            System.out.println(cursorLeftClickPos.x + " " + cursorLeftClickPos.y);
            System.out.println(cursorPos.x + " " + cursorPos.y);

            // TODO: for some reason the AABB doesnt get assigned properly

            Vector3d startWorldPos = convertScreenPosToWorldPos((int) cursorLeftClickPos.x, (int) cursorLeftClickPos.y);
            startWorldPos = addVector3d(startWorldPos, lookVector, -100);
            boxSelectAABB.setMaxX(startWorldPos.x);
            boxSelectAABB.setMaxY(startWorldPos.y);
            boxSelectAABB.setMaxZ(startWorldPos.z);

            Vector3d endWorldPos = convertScreenPosToWorldPos((int) cursorPos.x, (int) cursorPos.y);
            endWorldPos = addVector3d(endWorldPos, lookVector, 100);
            boxSelectAABB.setMinX(endWorldPos.x);
            boxSelectAABB.setMinY(endWorldPos.y);
            boxSelectAABB.setMinZ(endWorldPos.z);


            System.out.println(boxSelectAABB);
        }
    }

    @SubscribeEvent
    public static void onMouseRelease(GuiScreenEvent.MouseReleasedEvent evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        // select a moused over entity by left clicking it
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            leftClickDown = false;
            cursorLeftClickPos = new Vec2(0,0);

            // TODO: enact box select

        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            rightClickDown = false;
        }
    }

    // prevent moused over blocks being outlined in the usual way (ie. by raytracing from player to block)
    @SubscribeEvent
    public static void onHighlightBlockEvent(DrawSelectionEvent.HighlightBlock evt) {
        if (MC.level != null && OrthoviewClientVanillaEvents.isEnabled())
            evt.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldLastEvent evt) {
        if (MC.level != null && OrthoviewClientVanillaEvents.isEnabled()) {
            if (selectedEntity != null)
                drawEntityOutline(evt.getMatrixStack(), selectedEntity, 1.0f);
            if (preselectedEntity != null)
                drawEntityOutline(evt.getMatrixStack(), preselectedEntity, 0.5f);
            else if (!OrthoviewClientVanillaEvents.isCameraMovingByMouse() && shouldDrawBlockOutline()) {
                if (rightClickDown)
                    drawBlockOutline(evt.getMatrixStack(), preselectedBlockPos, 1.0f);
                else
                    drawBlockOutline(evt.getMatrixStack(), preselectedBlockPos, 0.5f);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveWorldEvent evt) {
        Entity entity = evt.getEntity();

        if (preselectedEntity != null && preselectedEntity.getId() == entity.getId())
            preselectedEntity = null;
        if (selectedEntity != null && selectedEntity.getId() == entity.getId())
            selectedEntity = null;
    }

    // gets the unit vector in the direction of player facing (same as camera)
    // calcs from https://stackoverflow.com/questions/65897792/3d-vector-coordinates-from-x-and-y-rotation
    public static Vector3d getPlayerLookVector() {
        float a = (float) Math.toRadians(MC.player.getYRot());
        float b = (float) Math.toRadians(MC.player.getXRot());
        return new Vector3d(-cos(b) * sin(a), -sin(b), cos(b) * cos(a));
    }

    // converts
    public static Vector3d convertScreenPosToWorldPos(int mouseX, int mouseY) {
        int winWidth = MC.getWindow().getGuiScaledWidth();
        int winHeight = MC.getWindow().getGuiScaledHeight();

        // at winHeight=240, zoom=10, screen is 20 blocks high, so PTB=240/20=24
        float pixelsToBlocks = winHeight / OrthoviewClientVanillaEvents.getZoom();

        // make mouse coordinate origin centre of screen
        float x = (mouseX - (float) winWidth / 2) / pixelsToBlocks;
        float y = 0;
        float z = (mouseY - (float) winHeight / 2) / pixelsToBlocks;

        double camRotYRads = Math.toRadians(OrthoviewClientVanillaEvents.getCamRotY());
        z = z / (float) (Math.sin(-camRotYRads));

        Vec2 XZRotated = OrthoviewClientVanillaEvents.rotateCoords(x, z);

        // for some reason position is off by some y coord so just move it down manually
        return new Vector3d(
                MC.player.xo - XZRotated.x,
                MC.player.yo + y + 1.5,
                MC.player.zo - XZRotated.y
        );
    }

    // returns vec3d with a set amount of the given unit vector added to it
    private static Vector3d addVector3d(Vector3d vec3d, Vector3d unitVec3d, float scale) {
        Vector3d unitVec3dLocal = new Vector3d(0,0,0);
        unitVec3dLocal.set(unitVec3d);
        unitVec3dLocal.scale(scale);
        Vector3d vec3dLocal = new Vector3d(0,0,0);
        vec3dLocal.set(vec3d);
        vec3dLocal.add(unitVec3dLocal);
        return vec3dLocal;
    }

    // returns the exact spot that the cursorWorldPos ray meets a solid block
    private static Vec3 getRefinedCursorWorldPos(Vector3d cursorWorldPosNear, Vector3d cursorWorldPosFar) {
        Vec3 vectorNear = new Vec3(cursorWorldPosNear.x, cursorWorldPosNear.y, cursorWorldPosNear.z);
        Vec3 vectorFar = new Vec3(cursorWorldPosFar.x, cursorWorldPosFar.y, cursorWorldPosFar.z);

        // clip() returns the point of clip, not the clipped block giving off-by-one errors so move slightly to compensate
        HitResult hitResult = MC.level.clip(new ClipContext(vectorNear, vectorFar, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, null));
        return hitResult.getLocation().add(new Vec3(-0.001,-0.001,-0.001));
    }

    // don't draw block outline if we are box selecting and only if we have an entity selected
    private static boolean shouldDrawBlockOutline() {
        return !leftClickDown && selectedEntity != null;
    }

    private static BlockPos getRefinedBlockPos(BlockPos bp, Vector3d cursorWorldPosNear) {
        ArrayList<BlockPos> blocks = new ArrayList<BlockPos>();

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

        for (int i = 0; i < blocks.size(); i++) {
            BlockPos block = blocks.get(i);
            double dist = new Vec3(block.getX(), block.getY(), block.getZ())
                    .distanceTo(new Vec3(cursorWorldPosNear.x, cursorWorldPosNear.y, cursorWorldPosNear.z));
            if (MC.level.getBlockState(block).getMaterial().isSolidBlocking()
                    && rayIntersectsAABBCustom(cursorWorldPosNear, lookVector, new AABB(block))
                    && dist < smallestDist ) {
                smallestDist = dist;
                bestBp = block;
            }
        }
        return bestBp;
    }

    private static boolean rayIntersectsAABBCustom(Vector3d origin, Vector3d rayVector, AABB aabb) {
        // r.dir is unit direction vector of ray
        Vector3d dirfrac = new Vector3d(
                1.0f / rayVector.x,
                1.0f / rayVector.y,
                1.0f / rayVector.z
        );
        // lb is the corner of AABB with minimal coordinates - left bottom, rt is maximal corner
        // r.org is origin of ray
        float t1 = (float) ((aabb.minX - origin.x) * dirfrac.x);
        float t2 = (float) ((aabb.maxX - origin.x) * dirfrac.x);
        float t3 = (float) ((aabb.minY - origin.y) * dirfrac.y);
        float t4 = (float) ((aabb.maxY - origin.y) * dirfrac.y);
        float t5 = (float) ((aabb.minZ - origin.z) * dirfrac.z);
        float t6 = (float) ((aabb.maxZ - origin.z) * dirfrac.z);

        float tmin = max(max(min(t1, t2), min(t3, t4)), min(t5, t6));
        float tmax = min(min(max(t1, t2), max(t3, t4)), max(t5, t6));

        // if tmax < 0, ray (line) is intersecting AABB, but the whole AABB is behind us
        // if (tmax < 0) return false;
        // if tmin > tmax, ray doesn't intersect AABB
        if (tmin > tmax) return false;

        return true;
    }

    public static void drawBlockOutline(PoseStack matrixStack, BlockPos blockpos, float alpha) {
        AABB aabb = new AABB(blockpos).move(0,0.01,0);
        drawOutline(matrixStack, aabb, alpha);
    }

    public static void drawEntityOutline(PoseStack matrixStack, Entity entity, float alpha) {
        drawOutline(matrixStack, entity.getBoundingBox(), alpha);
    }

    public static void drawOutline(PoseStack matrixStack, AABB aabb, float alpha) {
        Entity camEntity = MC.getCameraEntity();
        double d0 = camEntity.getX();
        double d1 = camEntity.getY() + camEntity.getEyeHeight();
        double d2 = camEntity.getZ();

        RenderSystem.depthMask(false); // disable showing lines through blocks
        VertexConsumer vertexConsumer = MC.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        matrixStack.pushPose();
        matrixStack.translate(-d0, -d1, -d2); // because we start at 0,0,0 relative to camera
        LevelRenderer.renderLineBox(matrixStack, vertexConsumer, aabb, 1.0f, 1.0f, 1.0f, alpha);
        matrixStack.popPose();

    }
}


