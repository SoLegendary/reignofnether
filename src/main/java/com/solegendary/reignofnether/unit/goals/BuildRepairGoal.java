package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.Farm;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.PathfinderMob;
import javax.annotation.Nullable;

// Move towards a building to build/repair it
// will continually try to move towards the building if too far away as long as this goal is being enacted

// if isBuilding(), stop the random lookaround goal (look at the next block to place instead)
// and run Player place block animations with arms shown

public class BuildRepairGoal extends MoveToTargetBlockGoal {

    private Building buildingTarget;

    public BuildRepairGoal(PathfinderMob mob, double speedModifier) {
        super(mob, true, speedModifier, 0);
    }

    public void tick() {
        if (buildingTarget != null) {
            calcMoveTarget();
            if (buildingTarget.getBlocksPlaced() >= buildingTarget.getBlocksTotal()) {
                if (buildingTarget instanceof Farm)
                    ((Unit) mob).getGatherResourceGoal().setTargetResourceName(ResourceName.FOOD);
                stopBuilding();
            }
            if (isBuilding()) {
                BlockPos bp = BuildingUtils.getCentrePos(buildingTarget.getBlocks());
                this.mob.getLookControl().setLookAt(bp.getX(), bp.getY(), bp.getZ());
            }
        }
        else
            this.moveTarget = null;
    }

    private void calcMoveTarget() {
        if (this.buildingTarget != null)
            this.moveTarget = this.buildingTarget.getClosestGroundPos(mob.getOnPos(), 0);
    }

    // only count as building if in range of the target - building is actioned in Building.tick()
    public boolean isBuilding() {
        if (buildingTarget != null && this.moveTarget != null)
            if (BuildingServerEvents.getUnitToBuildingRelationship((Unit) this.mob, buildingTarget) == Relationship.OWNED)
                return Math.sqrt(moveTarget.distSqr(new Vec3i(mob.getX(), mob.getY(), mob.getZ()))) < 2;
        return false;
    }

    public void setBuildingTarget(@Nullable Building target) {
        this.buildingTarget = target;
        calcMoveTarget();
        this.start();
    }

    public Building getBuildingTarget() { return buildingTarget; }

    // if we override stop() it for some reason is called after start() and we can never begin this goal...
    public void stopBuilding() {
        buildingTarget = null;
        super.stopMoving();
    }
}
