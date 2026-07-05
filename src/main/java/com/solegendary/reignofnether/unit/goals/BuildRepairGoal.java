package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.FarmPlacement;
import com.solegendary.reignofnether.building.buildings.shared.AbstractBridge;
import com.solegendary.reignofnether.building.buildings.villagers.OakStockpile;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// Move towards a building to build/repair it
// will continually try to move towards the building if too far away as long as this goal is being enacted

// if isBuilding(), stop the random lookaround goal (look at the next block to place instead)
// and run Player place block animations with arms shown

public class BuildRepairGoal extends MoveToTargetBlockGoal {

    public boolean ignoreNextCheckpoint = false;
    public final List<BuildingPlacement> queuedBuildings = new ArrayList<>();
    private BuildingPlacement buildingTarget;

    private Boolean isBuildingServerside = false;

    public boolean autocastRepair = false;

    public BuildRepairGoal(Mob mob) {
        super(mob, true, 0);
    }

    // isBuilding() accepts the worker as arrived at range 2, but moveReachRange is 0 so the base recalc
    // threshold is the 1.5-block settle distance. That leaves a 1.5-2 block band where a worker is building
    // yet still "too far -> repath" - which loops it on the spot. Match the threshold to the build range so a
    // worker in build range never triggers a recalc.
    @Override
    public double getMinDistToRecalculateSqr() {
        return Math.max(super.getMinDistToRecalculateSqr(), 2.0 * 2.0);
    }

    public void setIsBuildingServerside(boolean isBuilding) {
        this.isBuildingServerside = isBuilding;
    }

    public boolean startNextQueuedBuilding() {
        queuedBuildings.removeIf(b -> !BuildingUtils.isBuildingBuildable(this.mob.level().isClientSide(), b));
        if (queuedBuildings.size() > 0) {
            setBuildingTarget(queuedBuildings.get(0));
            return true;
        }
        return false;
    }

    public void tick() {
        if (this.mob.tickCount % 5 != 0)
            return;

        if (buildingTarget == null) {
            if (!this.mob.level().isClientSide() && WorkerUnit.isIdle((WorkerUnit) this.mob) && autocastRepair) {
                BuildingPlacement building = BuildingUtils.findClosestBuilding(
                        this.mob.level().isClientSide(),
                        this.mob.getEyePosition(),
                        b -> b.getBlocksPlaced() < b.getBlocksTotal() &&
                        BuildingServerEvents.getUnitToBuildingRelationship((Unit) this.mob, b) != Relationship.HOSTILE
                );
                if (building != null)
                    setBuildingTarget(building);
            }
            return;
        }
        if (!BuildingUtils.isBuildingBuildable(this.mob.level().isClientSide(), buildingTarget)) {
            if (!startNextQueuedBuilding()) {
                if (buildingTarget instanceof FarmPlacement && mob instanceof WorkerUnit) {
                    ((WorkerUnit) mob).getGatherResourceGoal().setTargetResourceName(ResourceName.FOOD);
                    ((WorkerUnit) mob).getGatherResourceGoal().setTargetFarm(buildingTarget);
                }
                stopBuilding();
            }
            return;
        }
        calcMoveTarget();
        if (isBuilding() && buildingTarget != null) {
            BlockPos bp = buildingTarget.centrePos;
            this.mob.getLookControl().setLookAt(bp.getX(), bp.getY(), bp.getZ());
            mob.getLookControl().lookAtCooldown = 20;
        }
    }

    private void calcMoveTarget() {
        if (this.buildingTarget == null)
            return;
        // Hold ONE approach cell instead of recomputing the closest perimeter cell from the unit's CURRENT
        // position every tick. That recompute made the target flip between two near-equidistant cells as the
        // unit moved, so it oscillated around the boundary ("back and forth") instead of committing to a path.
        // Like the gather goal holding a fixed block, keep the chosen cell until it's null or built over, then
        // re-pick. setBuildingTarget nulls moveTarget so a NEW building still gets a fresh approach cell.
        if (this.moveTarget != null && !isApproachInvalid(this.moveTarget))
            return;
        this.moveTarget = this.buildingTarget.getClosestGroundPos(mob.getOnPos(), 1, true);
    }

    // Re-pick the approach cell only if it got built over (non-bridge), mirroring getClosestGroundPos's own
    // exclusion. Bridges have special over-water geometry, so their cell is held until the target changes.
    private boolean isApproachInvalid(BlockPos bp) {
        return !(buildingTarget.getBuilding() instanceof AbstractBridge)
                && BuildingUtils.isPosInsideAnyBuilding(this.mob.level().isClientSide(), bp);
    }

    // only count as building if in range of the target - building is actioned in Building.tick()
    public boolean isBuilding() {
        if (this.mob.level().isClientSide())
            return isBuildingServerside;

        if (buildingTarget != null && this.moveTarget != null)
            if (BuildingServerEvents.getUnitToBuildingRelationship((Unit) this.mob, buildingTarget) == Relationship.OWNED ||
                buildingTarget.getBuilding() instanceof AbstractBridge)
                return MiscUtil.isMobInRangeOfPos(moveTarget, mob, 2); // buildingTarget.isPosInsideBuilding(mob.getOnPos())
        return false;
    }

    public void setBuildingTarget(@Nullable BuildingPlacement target) {
        if (target != null && !BuildingUtils.isBuildingBuildable(this.mob.level().isClientSide(), target))
            return;

        if (target != null) {
            if (ignoreNextCheckpoint)
                ignoreNextCheckpoint = false;
            else {
                MiscUtil.addUnitCheckpoint((Unit) mob, new BlockPos(
                        target.centrePos.getX(),
                        target.originPos.getY() + 1,
                        target.centrePos.getZ()),
                        true
                );
            }
        }
        this.buildingTarget = target;
        this.moveTarget = null; // force a fresh approach cell for the new target (calcMoveTarget then holds it)
        calcMoveTarget();
        this.start();
    }

    public BuildingPlacement getBuildingTarget() { return buildingTarget; }

    // if we override stop() it for some reason is called after start() and we can never begin this goal...
    public void stopBuilding() {
        queuedBuildings.clear();
        buildingTarget = null;
        super.stopMoving();
    }
}
