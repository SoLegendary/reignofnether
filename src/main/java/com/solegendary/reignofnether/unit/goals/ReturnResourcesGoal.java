package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.player.RTSPlayerScoresEnum;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;

// Move towards a building to build/repair it
// will continually try to move towards the building if too far away as long as this goal is being enacted

// if isBuilding(), stop the random lookaround goal (look at the next block to place instead)
// and run Player place block animations with arms shown

public class ReturnResourcesGoal extends MoveToTargetBlockGoal {

    private BuildingPlacement buildingTarget;

    public ReturnResourcesGoal(Mob mob) {
        super(mob, true, 0);
    }

    public void depositItems() {
        if (this.mob instanceof Unit unit && !this.mob.level().isClientSide()) {
            Resources res = Resources.getTotalResourcesFromItems(unit.getItems());
            if (res.getTotalValue() > 0) {
                res.ownerName = unit.getOwnerName();
                ResourcesServerEvents.addSubtractResources(res);
                ResourcesClientboundPacket.showFloatingText(res, this.moveTarget != null ? this.moveTarget : this.mob.getOnPos());

                RTSPlayer rtsPlayer = PlayerServerEvents.getRTSPlayer(res.ownerName);
                if (rtsPlayer != null) {
                    rtsPlayer.scores.addToScore(RTSPlayerScoresEnum.TOTAL_RESOURCES_HARVESTED, res.getTotalValue());
                    if (res.food > 0)
                        rtsPlayer.scores.addToScore(RTSPlayerScoresEnum.FOOD_HARVESTED, res.food);
                    else if (res.wood > 0)
                        rtsPlayer.scores.addToScore(RTSPlayerScoresEnum.WOOD_HARVESTED, res.wood);
                    else if (res.ore > 0)
                        rtsPlayer.scores.addToScore(RTSPlayerScoresEnum.ORES_HARVESTED, res.ore);
                }

                unit.getItems().clear();
                UnitSyncClientboundPacket.sendSyncResourcesPacket(unit);
                this.stopReturning();
            }
        }
    }

    public void tick() {
        if (buildingTarget != null) {
            calcMoveTarget();
            if (canDropOff() && this.mob instanceof Unit unit) {
                if (!this.mob.level().isClientSide()) {
                    this.depositItems();
                    if (this.mob instanceof WorkerUnit worker) {
                        unit.resetBehaviours();
                        WorkerUnit.resetBehaviours((WorkerUnit) unit);
                        GatherResourcesGoal goal = worker.getGatherResourceGoal();
                        if (goal != null && goal.saveData.hasData()) {
                            goal.loadState();
                            goal.saveData.delete();
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
            if (buildingTarget.isBuilt && buildingTarget.getBuilding().canAcceptResources &&
                BuildingServerEvents.getUnitToBuildingRelationship((Unit) this.mob, buildingTarget) == Relationship.OWNED &&
                BuildingServerEvents.getBuildings().contains(buildingTarget))
                return buildingTarget.isPosInsideBuilding(mob.getOnPos()) || MiscUtil.isMobInRangeOfPos(moveTarget, mob, 1.5f);
        return false;
    }

    public void returnToClosestBuilding() {
        if (this.mob.level().isClientSide())
            return;

        BlockPos pos = mob.getOnPos();
        BuildingPlacement closestBuilding = null;
        double closestDist = 99999;
        for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
            if (building.ownerName.equals(((Unit) mob).getOwnerName()) && building.getBuilding().canAcceptResources && building.isBuilt) {
                BlockPos bp = building.getClosestGroundPos(pos, 1);
                double dist = bp.distSqr(pos);
                if (dist < closestDist) {
                    closestBuilding = building;
                    closestDist = dist;
                }
            }
        }
        if (closestBuilding != null)
            this.setBuildingTarget(closestBuilding);
    }

    public void setBuildingTarget(@Nullable BuildingPlacement target) {
        if (target != null) {
            MiscUtil.addUnitCheckpoint((Unit) mob, target.centrePos, true);
        }
        this.buildingTarget = target;
        calcMoveTarget();
        this.start();
    }

    public BuildingPlacement getBuildingTarget() { return buildingTarget; }

    public void stopReturning() {
        buildingTarget = null;
        super.stopMoving();
    }
}
