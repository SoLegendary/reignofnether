package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;

// Move a villager towards a building to become a militia

public class CallToArmsGoal extends MoveToTargetBlockGoal {

    private BuildingPlacement buildingTarget;

    public CallToArmsGoal(Mob mob) {
        super(mob, true, 0);
    }

    public void tick() {
        if (buildingTarget == null)
            return;
        calcMoveTarget();
        if (this.mob.tickCount % 20 == 0)
            start();

        if (isInRange() && buildingTarget != null && !this.mob.level().isClientSide())
            if (this.mob instanceof VillagerUnit villagerUnit)
                villagerUnit.convertToMilitia();
    }

    private void calcMoveTarget() {
        if (this.buildingTarget != null)
            this.moveTarget = this.buildingTarget.getClosestGroundPos(mob.getOnPos(), 1);
    }

    public boolean isInRange() {
        if (buildingTarget != null && this.moveTarget != null)
            if (BuildingServerEvents.getUnitToBuildingRelationship((Unit) this.mob, buildingTarget) == Relationship.OWNED)
                return MiscUtil.isMobInRangeOfPos(moveTarget, mob, 2);
        return false;
    }

    public void setNearestTownCentreAsTarget() {
        BuildingPlacement building = BuildingUtils.findClosestBuilding(mob.level().isClientSide(), this.mob.getEyePosition(),
                (b) -> b.isBuilt && b.ownerName.equals(((Unit) mob).getOwnerName()) && b.getBuilding() instanceof TownCentre);
        if (building != null && building.getBuilding() instanceof TownCentre)
            setBuildingTarget(building);
    }

    private void setBuildingTarget(@Nullable BuildingPlacement target) {
        if (target != null) {
            MiscUtil.addUnitCheckpoint((Unit) mob, new BlockPos(
                    target.centrePos.getX(),
                    target.originPos.getY() + 1,
                    target.centrePos.getZ()),
                    true
            );
        }
        this.buildingTarget = target;
        calcMoveTarget();
        this.start();
    }

    public BuildingPlacement getBuildingTarget() { return buildingTarget; }

    @Override
    public void stop() {
        buildingTarget = null;
        super.stop();
    }
}
