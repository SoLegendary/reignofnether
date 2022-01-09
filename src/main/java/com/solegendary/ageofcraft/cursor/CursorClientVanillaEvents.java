package com.solegendary.ageofcraft.cursor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3d;
import com.solegendary.ageofcraft.orthoview.OrthoviewClientVanillaEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.event.DrawSelectionEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
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

/**
 * Handler that implements and manages screen-to-world translations of the cursor and block/entity selection
 */
public class CursorClientVanillaEvents {

    private static boolean leftClickDown = false;
    private static boolean rightClickDown = false;

    // pos of block moused over
    private static BlockPos mousedBlockPos = new BlockPos(0,0,0);
    // pos of block selected by right click (to move units to)
    private static BlockPos selectedBlockPos = new BlockPos(0,0,0);
    // pos of cursor exactly on: first non-air block, last frame, near to screen, far from screen
    private static Vector3d cursorPos = new Vector3d(0,0,0);
    private static Vector3d cursorPosLast = new Vector3d(0,0,0);
    private static Vector3d cursorPosNear = new Vector3d(0,0,0);
    private static Vector3d cursorPosFar = new Vector3d(0,0,0);
    private static Vector3d lookVector = new Vector3d(0,0,0);
    // entity moused over, vs entity selected by clicking
    private static Chicken mousedEntity = null;
    private static Chicken selectedEntity = null;

    private static final Minecraft MC = Minecraft.getInstance();
    private static int winWidth = MC.getWindow().getGuiScaledWidth();
    private static int winHeight = MC.getWindow().getGuiScaledHeight();

    public static Vector3d getCursorPos() {
        return cursorPos;
    }
    public static Chicken getMousedEntity() {
        return mousedEntity;
    }
    public static Chicken getSelectedEntity() {
        return selectedEntity;
    }
    public static BlockPos getMousedBlockPos() {
        return mousedBlockPos;
    }
    public static BlockPos getSelectedBlockPos() {
        return selectedBlockPos;
    }

    @SubscribeEvent
    public static void onDrawScreen(GuiScreenEvent.DrawScreenEvent evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        winWidth = MC.getWindow().getGuiScaledWidth();
        winHeight = MC.getWindow().getGuiScaledHeight();

        float zoom = OrthoviewClientVanillaEvents.getZoom();
        int mouseX = evt.getMouseX();
        int mouseY = evt.getMouseY();

        if (MC.player == null) return;

        // ************************************
        // Calculate cursor position on screen
        // ************************************

        // at winHeight=240, zoom=10, screen is 20 blocks high, so PTB=240/20=24
        float pixelsToBlocks = winHeight / zoom;

        // make mouse coordinate origin centre of screen
        float x = (mouseX - (float) winWidth / 2) / pixelsToBlocks;
        float y = 0;
        float z = (mouseY - (float) winHeight / 2) / pixelsToBlocks;

        double camRotYRads = Math.toRadians(OrthoviewClientVanillaEvents.getCamRotY());
        z = z / (float) (Math.sin(-camRotYRads));

        // get look vector of the player (and therefore the camera)
        // calcs from https://stackoverflow.com/questions/65897792/3d-vector-coordinates-from-x-and-y-rotation
        float a = (float) Math.toRadians(MC.player.getYRot());
        float b = (float) Math.toRadians(MC.player.getXRot());
        lookVector = new Vector3d(-cos(b) * sin(a), -sin(b), cos(b) * cos(a));

        Vec2 XZRotated = OrthoviewClientVanillaEvents.rotateCoords(x, z);

        cursorPosLast = new Vector3d(
                cursorPos.x,
                cursorPos.y,
                cursorPos.z
        );
        // for some reason position is off by some y coord so just move it down manually
        cursorPos = new Vector3d(
                MC.player.xo - XZRotated.x,
                MC.player.yo + y + 1.5,
                MC.player.zo - XZRotated.y
        );

        // calc near and far cursorPos to get a cursor line vector
        Vector3d lookVectorNear = new Vector3d(0, 0, 0);
        lookVectorNear.set(lookVector);
        lookVectorNear.scale(-100);
        cursorPosNear.set(cursorPos);
        cursorPosNear.add(lookVectorNear);
        Vector3d lookVectorFar = new Vector3d(0, 0, 0);
        lookVectorFar.set(lookVector);
        lookVectorFar.scale(100);
        cursorPosFar.set(cursorPos);
        cursorPosFar.add(lookVectorFar);

        // ************************************************************
        // Find the first solid walkable block along the cursorPos ray
        // ************************************************************

        // only spend time doing calcs for cursorEntity if we actually moved the cursor
        if (cursorPos.x != cursorPosLast.x || cursorPos.y != cursorPosLast.y || cursorPos.z != cursorPosLast.z) {

            if (MC.level != null) {
                Vec3 vectorNear = new Vec3(cursorPosNear.x, cursorPosNear.y, cursorPosNear.z);
                Vec3 vectorFar = new Vec3(cursorPosFar.x, cursorPosFar.y, cursorPosFar.z);

                HitResult hitResult = MC.level.clip(new ClipContext(vectorNear, vectorFar, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, null));
                Vec3 hitPos = hitResult.getLocation().add(new Vec3(0,-0.001,0));
                mousedBlockPos = new BlockPos(hitPos);

                // clip() returns the point of clip, not the clipped block, so we get off-by-one errors
                // in some directions so try moving very slightly to get the properly clipped block
                if (!MC.level.getBlockState(mousedBlockPos).getMaterial().isSolidBlocking()) {
                    hitPos = hitPos.add(new Vec3(-0.001, 0, -0.001));
                    mousedBlockPos = new BlockPos(hitPos);
                    // if we clipped a non-solid block (eg. tall grass) search adjacent blocks for a next-best match
                    mousedBlockPos = refineBlockPos(mousedBlockPos);
                }
                cursorPos = new Vector3d(hitPos.x, hitPos.y, hitPos.z);
                // TODO: make this be CursorEntity and only show when moving a mob instead of following cursor
                //CursorCommonVanillaEvents.moveCursorEntity(cursorPos);
            }
        }

        // ****************************************
        // Find entity moused over and/or selected
        // ****************************************
        AABB aabb = new AABB(
                cursorPos.x - 5,
                cursorPos.y - 5,
                cursorPos.z - 5,
                cursorPos.x + 5,
                cursorPos.y + 5,
                cursorPos.z + 5
        );
        List<Chicken> entities = MC.level.getEntitiesOfClass(Chicken.class, aabb);

        mousedEntity = null;

        for (Chicken entity : entities) {
            // inflate by set amount to improve click accuracy
            AABB entityaabb = entity.getBoundingBox().inflate(0.1);

            if (rayIntersectsAABBCustom(cursorPosNear, lookVector, entityaabb)) {
                mousedEntity = entity;
            }
        }
    }

    @SubscribeEvent
    public static void onMobDeath(EntityLeaveWorldEvent evt) {
        if (mousedEntity != null && evt.getEntity().getId() == mousedEntity.getId())
            mousedEntity = null;
        if (selectedEntity != null && evt.getEntity().getId() == selectedEntity.getId())
            selectedEntity = null;
    }

    @SubscribeEvent
    public static void onMouseClick(GuiScreenEvent.MouseClickedEvent evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        // select a moused over entity by left clicking it
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            leftClickDown = true;
            if (mousedEntity != null)
                selectedEntity = mousedEntity;
            else
                selectedEntity = null;
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            rightClickDown = true;

            if (selectedEntity != null) {
                selectedBlockPos = mousedBlockPos;
                System.out.println("Moving chicken (id " + selectedEntity.getId() + ") to " + selectedBlockPos);
            }
        }
    }
    @SubscribeEvent
    public static void onMouseRelease(GuiScreenEvent.MouseReleasedEvent evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        // select a moused over entity by left clicking it
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            leftClickDown = false;
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
            if (mousedEntity != null)
                drawEntityOutline(evt.getMatrixStack(), mousedEntity, 0.5f);
            else if (!OrthoviewClientVanillaEvents.isCameraMovingByMouse()) {
                if (rightClickDown)
                    drawBlockOutline(evt.getMatrixStack(), mousedBlockPos, 1.0f);
                else
                    drawBlockOutline(evt.getMatrixStack(), mousedBlockPos, 0.5f);
            }

        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveWorldEvent evt) {
        Entity entity = evt.getEntity();

        if (mousedEntity != null && mousedEntity.getId() == entity.getId())
            mousedEntity = null;
        if (selectedEntity != null && selectedEntity.getId() == entity.getId())
            selectedEntity = null;
    }

    private static BlockPos refineBlockPos(BlockPos bp) {
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

        for (int i = 0; i < blocks.size(); i++) {
            BlockPos block = blocks.get(i);
            double dist = new Vec3(0,0,0)
                    .distanceTo(new Vec3(0,0,0));
            if (MC.level.getBlockState(block).getMaterial().isSolidBlocking()
                    && rayIntersectsAABBCustom(cursorPosNear, lookVector, new AABB(block))
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


