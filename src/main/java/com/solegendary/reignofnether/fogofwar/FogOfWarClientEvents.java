package com.solegendary.reignofnether.fogofwar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.solegendary.reignofnether.cursor.CursorClientEvents.getPlayerLookVector;
import static com.solegendary.reignofnether.cursor.CursorClientEvents.getRefinedCursorWorldPos;

public class FogOfWarClientEvents {

    static final Minecraft MC = Minecraft.getInstance();
    static ArrayList<Pair<BlockPos, Direction>> foggedBlocks = new ArrayList<>(); // x/z coords that are in fog of war (to darken on the minimap)

    private static final int REFRESH_TICKS_MAX = 10;
    private static int refreshTicksCurrent = 0;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        if (!OrthoviewClientEvents.isEnabled() || MC.level == null)
            return;

        /*
        refreshTicksCurrent -= 1;
        if (refreshTicksCurrent <= 0) {
            refreshTicksCurrent = REFRESH_TICKS_MAX;
            updateFogOfWar();
        }
        renderFogOfWar(evt.getPoseStack());
         */
    }

    public static void updateFogOfWar() {

        LivingEntity selEntity = HudClientEvents.hudSelectedEntity;
        foggedBlocks = new ArrayList<>();

        ArrayList<Vec3> uvwp = MyMath.prepIsPointInsideRect3d(MC,
                0, 0,
                0, MC.getWindow().getGuiScaledHeight(),
                MC.getWindow().getGuiScaledWidth(), MC.getWindow().getGuiScaledHeight()
        );

        // get world position of corners of the screen
        Vector3d tl = MiscUtil.screenPosToWorldPos(MC, 0, 0);
        Vector3d bl = MiscUtil.screenPosToWorldPos(MC, 0, MC.getWindow().getGuiScaledHeight());
        Vector3d br = MiscUtil.screenPosToWorldPos(MC, MC.getWindow().getGuiScaledWidth(), MC.getWindow().getGuiScaledHeight());
        Vector3d tr = MiscUtil.screenPosToWorldPos(MC, MC.getWindow().getGuiScaledWidth(), 0);

        Vector3d[] corners = new Vector3d[]{tl, bl, br, tr};
        Vec3[] cornersRef = new Vec3[4];

        Vector3d lookVector = getPlayerLookVector();
        for (int i = 0; i <= 3; i++) {
            Vector3d ptNear = MyMath.addVector3d(corners[i], lookVector, -200);
            Vector3d ptFar = MyMath.addVector3d(corners[i], lookVector, 200);
            cornersRef[i] = getRefinedCursorWorldPos(ptNear, ptFar);
        }
        double maxX = Collections.max(Arrays.asList(cornersRef[0].x, cornersRef[1].x, cornersRef[2].x, cornersRef[3].x));
        double minX = Collections.min(Arrays.asList(cornersRef[0].x, cornersRef[1].x, cornersRef[2].x, cornersRef[3].x));
        double maxZ = Collections.max(Arrays.asList(cornersRef[0].z, cornersRef[1].z, cornersRef[2].z, cornersRef[3].z));
        double minZ = Collections.min(Arrays.asList(cornersRef[0].z, cornersRef[1].z, cornersRef[2].z, cornersRef[3].z));

        // iterate over a larger x/z aligned rect and only darken those inside the view rect
        for (int z = (int) minZ; z < maxZ; z++) {
            for (int x = (int) minX; x < maxX; x++) {
                int yMax = MC.level.getChunkAt(new BlockPos(x, 0, z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

                for (int y = 0; y <= yMax; y++) {
                    Vec3 m = new Vec3(x, y, z);
                    if (MyMath.isPointInsideRect3d(uvwp, m)) {
                        BlockPos bp = new BlockPos(x, y, z);
                        BlockState bs = MC.level.getBlockState(bp);

                        if (bs.getMaterial().isSolid() &&
                            selEntity instanceof Unit &&
                            selEntity.getEyePosition().distanceTo(new Vec3(x, y, z)) > (((Unit) selEntity).getSightRange())) {

                            if (!isSolidBlocking(bp.above()))
                                foggedBlocks.add(new Pair(bp, Direction.UP));
                            if (!isSolidBlocking(bp.north()))
                                foggedBlocks.add(new Pair(bp, Direction.NORTH));
                            if (!isSolidBlocking(bp.south()))
                                foggedBlocks.add(new Pair(bp, Direction.SOUTH));
                            if (!isSolidBlocking(bp.east()))
                                foggedBlocks.add(new Pair(bp, Direction.EAST));
                            if (!isSolidBlocking(bp.west()))
                                foggedBlocks.add(new Pair(bp, Direction.WEST));
                        }
                    }
                }
            }
        }
    }

    public static void renderFogOfWar(PoseStack poseStack) {
        for (Pair<BlockPos, Direction> bpd : foggedBlocks)
            MyRenderer.drawBlackBlockFace(poseStack, bpd.getSecond(), bpd.getFirst(), 0.5f);
    }

    // checks whether a block is a complete cube or not
    private static boolean isSolidBlocking(BlockPos bp) {
        BlockState bs = MC.level.getBlockState(bp);
        return bs.getMaterial().isSolidBlocking();
    }
}
