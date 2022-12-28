package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.unit.*;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import javax.annotation.Nullable;

// Move towards a building to build/repair it
// will continually try to move towards the building if too far away as long as this goal is being enacted

// if isBuilding(), stop the random lookaround goal (look at the next block to place instead)
// and run Player place block animations with arms shown

public class ReturnResourcesGoal extends MoveToTargetBlockGoal {

    private Building buildingTarget;


    public ReturnResourcesGoal(PathfinderMob mob, double speedModifier) {
        super(mob, true, speedModifier, 0);
    }

    public void tick() {
        if (buildingTarget != null) {
            calcMoveTarget();
            if (canDropOff() && this.mob instanceof Unit unit) {
                if (!this.mob.level.isClientSide()) {
                    Resources res = Resources.getTotalResourcesFromItems(unit.getItems());
                    res.ownerName = unit.getOwnerName();
                    ResourcesServerEvents.addSubtractResources(res);
                    ResourcesClientboundPacket.showFloatingText(res, this.moveTarget);
                    unit.getItems().clear();
                    UnitClientboundPacket.sendSyncResourcesPacket(this.mob);
                    this.stopReturning();

                    if (this.mob instanceof WorkerUnit worker) {
                        unit.resetBehaviours();
                        WorkerUnit.resetBehaviours((WorkerUnit) unit);
                        GatherResourcesGoal goal = worker.getGatherResourceGoal();
                        if (goal != null && goal.hasSavedData()) {
                            goal.loadState();
                            goal.deleteSavedState();
                        }
                    }
                }
            }
        }
        else
            this.moveTarget = null;
    }

    private void calcMoveTarget() {
        if (this.buildingTarget != null)
            this.moveTarget = this.buildingTarget.getClosestGroundPos(mob.getOnPos(), 1);
    }

    // only count as building if in range of the target - building is actioned in Building.tick()
    public boolean canDropOff() {
        if (buildingTarget != null && this.moveTarget != null)
            if (buildingTarget.isBuilt && buildingTarget.canAcceptResources &&
                BuildingServerEvents.getUnitToBuildingRelationship((Unit) this.mob, buildingTarget) == Relationship.OWNED)
                return buildingTarget.isPosInsideBuilding(mob.getOnPos()) ||
                        Math.sqrt(moveTarget.distSqr(new Vec3i(mob.getX(), mob.getY(), mob.getZ()))) < 2;
        return false;
    }

    public void returnToClosestBuilding() {
        if (this.mob.level.isClientSide())
            return;

        BlockPos pos = mob.getOnPos();
        Building closestBuilding = null;
        double closestDist = 9999;
        for (Building building : BuildingServerEvents.getBuildings()) {
            if (building.ownerName.equals(((Unit) mob).getOwnerName()) && building.canAcceptResources && building.isBuilt) {
                BlockPos bp = building.getClosestGroundPos(pos, 1);
                double dist = bp.distSqr(pos);
                if (bp.distSqr(pos) < closestDist) {
                    closestBuilding = building;
                    closestDist = dist;
                }
            }
        }
        if (closestBuilding != null)
            this.setBuildingTarget(closestBuilding);
    }

    public void setBuildingTarget(@Nullable Building target) {
        this.buildingTarget = target;
        calcMoveTarget();
        this.start();
    }

    public Building getBuildingTarget() { return buildingTarget; }

    public void stopReturning() {
        buildingTarget = null;
        super.stopMoving();
    }
}
