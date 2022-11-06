package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.EnumSet;

// similar to MoveToCursorBlockGoal but used for builder units to move towards a building to build/repair it
// will continually try to move towards the building if too far away as long as this goal is being enacted

// if isBuilding(), stop the random lookaround goal (look at the next block to place instead)
// and run Player place block animations with arms shown

public class BuildRepairGoal extends Goal {

    private final PathfinderMob mob;
    private final double speedModifier;
    private BlockPos moveTarget = null;
    private boolean restartFlag = false; // if true, rerun start()
    private Building buildingTarget;

    public BuildRepairGoal(PathfinderMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    // only count as building if in range of the target,
    public boolean isBuilding() {
        if (this.buildingTarget != null) {
            BlockPos closestPos = this.buildingTarget.getClosestGroundPos(mob.getOnPos(), 0);
            double distToBuilding = Math.sqrt(closestPos.distSqr(new Vec3i(mob.getX(), mob.getY(), mob.getZ())));
            return distToBuilding < 2;
        }
        return false;
    }
    public double getDistToMoveTarget() {
        return Math.sqrt(moveTarget.distSqr(new Vec3i(mob.getX(), mob.getY(), mob.getZ())));
    }

    // move towards target - and keep trying to move towards it if knocked away
    public void tick() {
        if (buildingTarget != null) {
            if (buildingTarget.getBlocksPlaced() >= buildingTarget.getBlocksTotal())
                setTarget(null);
            if (moveTarget != null &&
                this.mob.getNavigation().isDone() &&
                    getDistToMoveTarget() >= 1)
                start();
        }
        if (isBuilding()) {
            BlockPos bp = BuildingUtils.getCentrePos(buildingTarget.getBlocks());
            this.mob.getLookControl().setLookAt(bp.getX(), bp.getY(), bp.getZ());
        }
    }

    public boolean canUse() {
        return moveTarget != null;
    }

    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone();
    }

    public void start() {
        if (buildingTarget != null) {
            // find closest BlockPos adjacent the target building
            this.moveTarget = buildingTarget.getClosestGroundPos(mob.getOnPos(), 0);
            Path path = mob.getNavigation().createPath(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ(), 0);
            this.mob.getNavigation().moveTo(path, speedModifier);
        }
        else
            this.mob.getNavigation().stop();
    }

    public void setTarget(@Nullable Building target) {
        this.buildingTarget = target;
        this.start();
    }

    public Building getTarget() { return buildingTarget; }
}
