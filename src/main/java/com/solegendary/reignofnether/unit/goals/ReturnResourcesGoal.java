package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.UnitServerboundPacket;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
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
                    Resources res = ResourceSources.getTotalResourcesFromItems(unit.getItems());
                    res.ownerName = unit.getOwnerName();
                    ResourcesServerEvents.addSubtractResources(res);
                    ResourcesClientboundPacket.showFloatingText(res, this.moveTarget);
                    unit.getItems().clear();
                    UnitClientEvents.syncUnitResources(this.mob.getId(), new Resources("", 0,0,0));
                    this.stopReturning();
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
