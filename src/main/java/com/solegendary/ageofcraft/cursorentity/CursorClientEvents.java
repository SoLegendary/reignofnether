package com.solegendary.ageofcraft.cursorentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3d;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.DrawSelectionEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.solegendary.ageofcraft.orthoview.OrthoViewClientEvents;

import static net.minecraft.util.Mth.cos;
import static net.minecraft.util.Mth.sin;

/**
 * Handler that implements and manages cursor block selection
 */
public class CursorClientEvents {

    private static BlockPos cursorBlockPos = new BlockPos(0,0,0);
    private static Vector3d cursorPos = new Vector3d(0,0,0);
    private static Vector3d cursorPosLast = new Vector3d(0,0,0);

    private static final Minecraft MC = Minecraft.getInstance();
    private static int winWidth = MC.getWindow().getGuiScaledWidth();
    private static int winHeight = MC.getWindow().getGuiScaledHeight();

    @SubscribeEvent
    public static void onDrawScreen(GuiScreenEvent.DrawScreenEvent evt) {
        if (!OrthoViewClientEvents.isTopdownGui(evt) || !OrthoViewClientEvents.isEnabled()) return;

        float zoom = OrthoViewClientEvents.getZoom();
        int mouseX = evt.getMouseX();
        int mouseY = evt.getMouseY();

        // get the block that is being moused over while on the topdown gui
        // TODO: do this for entities too
        if (MC.player != null) {
            // at winHeight=240, zoom=10, screen is 20 blocks high, so PTB=240/20=24
            float pixelsToBlocks = winHeight / zoom;

            // make mouse coordinate origin centre of screen
            float x = (mouseX - (float) winWidth / 2) / pixelsToBlocks;
            float y = 0;
            float z = (mouseY - (float) winHeight / 2) / pixelsToBlocks;

            double camRotYRads = Math.toRadians(OrthoViewClientEvents.getCamRotY());
            z = z / (float) (Math.sin(-camRotYRads));

            // get look vector of the player (and therefore the camera)
            // calcs from https://stackoverflow.com/questions/65897792/3d-vector-coordinates-from-x-and-y-rotation
            float a = (float) Math.toRadians(MC.player.getYRot());
            float b = (float) Math.toRadians(MC.player.getXRot());
            final Vector3d lookVector = new Vector3d(-cos(b) * sin(a), -sin(b), cos(b) * cos(a));

            Vec2 XZRotated = OrthoViewClientEvents.rotateCoords(x, z);

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

                // if we add a multiple of the lookVector, we can 'raytrace' back and forth from the camera without
                // changing the on-screen position of the cursorEntity
                boolean lastBlockSolid = false;
                double vectorScale = -50;
                Vector3d lookVectorScaled;
                BlockPos bp;
                BlockState bs;

                while (true) {
                    Vector3d searchVec = new Vector3d(0, 0, 0);
                    searchVec.set(cursorPos);

                    lookVectorScaled = new Vector3d(0, 0, 0);
                    lookVectorScaled.set(lookVector);
                    lookVectorScaled.scale(vectorScale); // has to be high enough to be at the 'front' of the screen
                    searchVec.add(lookVectorScaled);

                    bp = new BlockPos(searchVec.x, searchVec.y, searchVec.z);
                    bs = MC.level.getBlockState(bp);

                    // found the target block (or went too low)
                    if (!bs.isAir() || bp.getY() < -64) {
                        //System.out.println(bs.getBlock().getName());
                        cursorBlockPos = bp;
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
                CursorCommonEvents.moveCursorEntity(cursorPosFront);
            }
        }
    }

    // prevent moused over blocks being outlined in the usual way (ie. by raytracing from player to block)
    @SubscribeEvent
    public static void onHighlightBlockEvent(DrawSelectionEvent.HighlightBlock evt) {
        if (MC.level != null) {
            drawSelectionBox(evt.getMatrix());
            evt.setCanceled(true);
        }
    }

    static BlockPos testBlockPos = new BlockPos(346,64,-273);

    public static void drawSelectionBox(PoseStack matrix)
    {
        Entity camEntity = MC.getCameraEntity();
        BlockPos blockpos = testBlockPos;
        BlockState blockstate = MC.level.getBlockState(blockpos);
        double d0 = camEntity.getX();
        double d1 = camEntity.getY() + camEntity.getEyeHeight();
        double d2 = camEntity.getZ();

        if (!blockstate.isAir()) {
            VertexConsumer vertexConsumer = MC.renderBuffers().bufferSource().getBuffer(RenderType.lines());

            final double xIn = (double) blockpos.getX() - d0;
            final double yIn = (double) blockpos.getY() - d1;
            final double zIn = (double) blockpos.getZ() - d2;

            VoxelShape shapeIn = blockstate.getShape(MC.level, blockpos); // need to put back in ISelectionThingo?
            Matrix4f matrix4f = matrix.last().pose();

            shapeIn.forAllEdges((x0, y0, z0, x1, y1, z1) ->
            {
                vertexConsumer.vertex(matrix4f, (float) (x0 + xIn), (float) (y0 + yIn), (float) (z0 + zIn)).color(1.0f,1.0f,1.0f,1.0f).endVertex();
                vertexConsumer.vertex(matrix4f, (float) (x1 + xIn), (float) (y1 + yIn), (float) (z1 + zIn)).color(1.0f,1.0f,1.0f,1.0f).endVertex();
            });
        }
    }
}
