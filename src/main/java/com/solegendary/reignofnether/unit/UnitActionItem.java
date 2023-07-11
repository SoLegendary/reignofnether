package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.goals.GatherResourcesGoal;
import com.solegendary.reignofnether.unit.goals.ReturnResourcesGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

public class UnitActionItem {
    private final String ownerName;
    private final UnitAction action;
    private final int unitId; // preselected unit (usually the target)
    private final int[] unitIds; // selected unit(s)
    private final BlockPos preselectedBlockPos;
    private final BlockPos selectedBuildingPos;

    public UnitActionItem(
            String ownerName,
            UnitAction action,
            int unitId,
            int[] unitIds,
            BlockPos preselectedBlockPos,
            BlockPos selectedBuildingPos) {

        this.ownerName = ownerName;
        this.action = action;
        this.unitId = unitId;
        this.unitIds = unitIds;
        this.preselectedBlockPos = preselectedBlockPos;
        this.selectedBuildingPos = selectedBuildingPos;
    }

    public void resetBehaviours(Unit unit) {
        unit.resetBehaviours();
        Unit.resetBehaviours(unit);
        if (unit instanceof WorkerUnit workerUnit)
            WorkerUnit.resetBehaviours(workerUnit);
        if (unit instanceof AttackerUnit attackerUnit)
            AttackerUnit.resetBehaviours(attackerUnit);
    }

    // can be done server or clientside - but only serverside will have an effect on the world
    // clientside actions are purely for tracking data
    public void action(Level level) {

        // filter out unowned units and non-unit entities
        ArrayList<Unit> actionableUnits = new ArrayList<>();
        for (int id : unitIds)
            if (level.getEntity(id) instanceof Unit unit && unit.getOwnerName().equals(this.ownerName))
                actionableUnits.add(unit);

        for (Unit unit : actionableUnits) {

            // have to do this before resetBehaviours so we can assign the correct resourceName first
            if (action == UnitAction.TOGGLE_GATHER_TARGET) {
                if (unit instanceof WorkerUnit workerUnit) {
                    GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
                    ResourceName targetResourceName = goal.getTargetResourceName();
                    resetBehaviours(unit);
                    if (goal != null) {
                        switch (targetResourceName) {
                            case NONE -> goal.setTargetResourceName(ResourceName.FOOD);
                            case FOOD -> goal.setTargetResourceName(ResourceName.WOOD);
                            case WOOD -> goal.setTargetResourceName(ResourceName.ORE);
                            case ORE -> goal.setTargetResourceName(ResourceName.NONE);
                        }
                    }
                }
            }
            else
                resetBehaviours(unit);

            switch (action) {
                case STOP -> { }
                case HOLD -> {
                    unit.setHoldPosition(true);
                }
                case MOVE -> {
                    ResourceName resName = ResourceSources.getBlockResourceName(preselectedBlockPos, level);
                    if (unit instanceof WorkerUnit workerUnit && resName != ResourceName.NONE) {
                        workerUnit.getGatherResourceGoal().setTargetResourceName(resName);
                        workerUnit.getGatherResourceGoal().setMoveTarget(preselectedBlockPos);
                    }
                    else
                        unit.setMoveTarget(preselectedBlockPos);
                }
                case ATTACK_MOVE -> {
                    // if the unit can't actually attack just treat this as a move action
                    if (unit instanceof AttackerUnit attackerUnit)
                        attackerUnit.setAttackMoveTarget(preselectedBlockPos);
                    else
                        unit.setMoveTarget(preselectedBlockPos);
                }
                case ATTACK -> {
                    // if the unit can't actually attack just treat this as a follow action
                    if (unit instanceof AttackerUnit attackerUnit)
                        attackerUnit.setAttackTarget((LivingEntity) level.getEntity(unitId));
                    else
                        unit.setFollowTarget((LivingEntity) level.getEntity(unitId));
                }
                case ATTACK_BUILDING -> {
                    // if the unit can't actually attack just treat this as a move action
                    if (unit instanceof AttackerUnit attackerUnit && attackerUnit.canAttackBuildings())
                        attackerUnit.setAttackBuildingTarget(preselectedBlockPos);
                    else
                        unit.setMoveTarget(preselectedBlockPos);
                }
                case FOLLOW -> {
                    unit.setFollowTarget((LivingEntity) level.getEntity(unitId));
                }
                case BUILD_REPAIR -> {
                    // if the unit can't actually build/repair just treat this as a move action
                    if (unit instanceof WorkerUnit workerUnit) {
                        Building building = BuildingUtils.findBuilding(BuildingServerEvents.getBuildings(), preselectedBlockPos);
                        if (building != null)
                            workerUnit.getBuildRepairGoal().setBuildingTarget(building);
                    }
                    else
                        unit.setMoveTarget(preselectedBlockPos);
                }
                case FARM -> {
                    if (unit instanceof WorkerUnit workerUnit) {
                        GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
                        if (goal != null) {
                            goal.setTargetResourceName(ResourceName.FOOD);
                            goal.setMoveTarget(preselectedBlockPos);
                            Building building = BuildingUtils.findBuilding(BuildingServerEvents.getBuildings(), preselectedBlockPos);
                            if (building != null && building.name.contains(" Farm"))
                                goal.setTargetFarm(building);
                        }
                    }
                }
                case RETURN_RESOURCES -> {
                    if (unit instanceof WorkerUnit workerUnit) { // if we manually did this, ignore automated return to gather
                        GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
                        if (goal != null)
                            goal.deleteSavedState();
                    }
                    ReturnResourcesGoal returnResourcesGoal = unit.getReturnResourcesGoal();
                    Building building = BuildingUtils.findBuilding(BuildingServerEvents.getBuildings(), preselectedBlockPos);
                    if (returnResourcesGoal != null && building != null)
                        returnResourcesGoal.setBuildingTarget(building);
                }
                case RETURN_RESOURCES_TO_CLOSEST -> {
                    if (unit instanceof WorkerUnit workerUnit) { // if we manually did this, ignore automated return to gather
                        GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
                        if (goal != null)
                            goal.deleteSavedState();
                    }
                    ReturnResourcesGoal returnResourcesGoal = unit.getReturnResourcesGoal();
                    if (returnResourcesGoal != null)
                        returnResourcesGoal.returnToClosestBuilding();
                }
                case DELETE -> {
                    ((LivingEntity) unit).kill();
                }

                // any other Ability not explicitly defined here
                default -> {
                    for (Ability ability : unit.getAbilities()) {
                        if (ability.action == action) {
                            if (ability.canTargetEntities && this.unitId > 0)
                                ability.use(level, unit, (LivingEntity) level.getEntity(unitId));
                            else
                                ability.use(level, unit, preselectedBlockPos);
                        }
                    }
                }
            }
        }
        if (this.selectedBuildingPos.equals(new BlockPos(0, 0, 0)))
            return;

        Building actionableBuilding;
        if (level.isClientSide())
            actionableBuilding = BuildingUtils.findBuilding(BuildingClientEvents.getBuildings(), this.selectedBuildingPos);
        else
            actionableBuilding = BuildingUtils.findBuilding(BuildingServerEvents.getBuildings(), this.selectedBuildingPos);

        if (actionableBuilding != null) {
            for (Ability ability : actionableBuilding.getAbilities())
                if (ability.action == action)
                    ability.use(level, actionableBuilding, preselectedBlockPos);
        }
    }
}