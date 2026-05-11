package com.solegendary.reignofnether.unit;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class FormationDragMove {

    private static BlockPos dragStartBp = null;
    private static BlockPos dragEndBp = null;
    private static List<BlockPos> formationTargets = new ArrayList<>();
    private static boolean dragging = false;

    public static boolean isDragging() {
        return dragging;
    }

    public static void startDrag(BlockPos bp) {
        dragStartBp = bp;
        dragEndBp = bp;
        dragging = true;
        formationTargets.clear();
    }

    public static void updateDrag(BlockPos currentBp, int unitCount, Level level) {
        if (!dragging) return;
        dragEndBp = currentBp;
        calculateTargets(unitCount, level);
    }

    public static List<BlockPos> getFormationTargets() {
        return formationTargets;
    }

    public static Vec3 getLineStart() {
        if (dragStartBp == null) return Vec3.ZERO;
        return new Vec3(dragStartBp.getX() + 0.5, dragStartBp.getY() + 1.0, dragStartBp.getZ() + 0.5);
    }

    public static Vec3 getLineEnd() {
        if (dragEndBp == null) return Vec3.ZERO;
        return new Vec3(dragEndBp.getX() + 0.5, dragEndBp.getY() + 1.0, dragEndBp.getZ() + 0.5);
    }

    public static List<Pair<LivingEntity, BlockPos>> endDrag(List<LivingEntity> units) {
        List<Pair<LivingEntity, BlockPos>> result = assignUnitsToPositions(units);
        dragStartBp = null;
        dragEndBp = null;
        formationTargets.clear();
        dragging = false;
        return result;
    }

    public static void cancelDrag() {
        dragStartBp = null;
        dragEndBp = null;
        formationTargets.clear();
        dragging = false;
    }

    private static void calculateTargets(int unitCount, Level level) {
        formationTargets.clear();
        if (dragStartBp == null || dragEndBp == null || unitCount <= 0) return;

        if (unitCount == 1) {
            formationTargets.add(getHeightAdjustedPos(level, dragEndBp));
            return;
        }

        Vec3 start = Vec3.atCenterOf(dragStartBp);
        Vec3 end = Vec3.atCenterOf(dragEndBp);

        for (int i = 0; i < unitCount; i++) {
            double t = (i + 0.5) / unitCount;
            double x = start.x + (end.x - start.x) * t;
            double z = start.z + (end.z - start.z) * t;
            BlockPos bp = new BlockPos((int) Math.floor(x), dragStartBp.getY(), (int) Math.floor(z));
            formationTargets.add(getHeightAdjustedPos(level, bp));
        }
    }

	private static BlockPos getHeightAdjustedPos(Level level, BlockPos bp) {
		int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bp.getX(), bp.getZ()) - 1;
		BlockPos checkBp = new BlockPos(bp.getX(), groundY, bp.getZ());
		while (groundY > level.getMinBuildHeight()) {
			BlockState bs = level.getBlockState(checkBp);
			boolean isLeaves = bs.getBlock() instanceof LeavesBlock;
			boolean isPassable = !MiscUtil.isSolidBlocking(level, checkBp) && bs.getFluidState().isEmpty();
			if (!isLeaves && !isPassable) break;
			groundY--;
			checkBp = new BlockPos(bp.getX(), groundY, bp.getZ());
		}
		return new BlockPos(bp.getX(), groundY, bp.getZ());
	}

    private static List<Pair<LivingEntity, BlockPos>> assignUnitsToPositions(List<LivingEntity> units) {
        List<Pair<LivingEntity, BlockPos>> result = new ArrayList<>();
        if (units.isEmpty() || formationTargets.isEmpty()) return result;

        List<LivingEntity> unassigned = new ArrayList<>(units);

        for (BlockPos target : formationTargets) {
            LivingEntity closest = null;
            double closestDist = Double.MAX_VALUE;
            for (LivingEntity unit : unassigned) {
                double dist = unit.getOnPos().distSqr(target);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = unit;
                }
            }
            if (closest != null) {
                result.add(new Pair<>(closest, target));
                unassigned.remove(closest);
            }
        }
        return result;
    }
}