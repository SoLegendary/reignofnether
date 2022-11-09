package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.unit.units.CreeperUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class UnitActionItem {
    private final UnitAction action;
    private final int unitId;
    private final int[] unitIds;
    private final BlockPos preselectedBlockPos;

    public UnitActionItem(
            UnitAction action,
            int unitId,
            int[] unitIds,
            BlockPos preselectedBlockPos) {

        this.action = action;
        this.unitId = unitId;
        this.unitIds = unitIds;
        this.preselectedBlockPos = preselectedBlockPos;
    }

    // can be done server or clientside - but only serverside will have an effect on the world
    // clientside actions are purely for tracking data
    public void action(Level level) {

        if (action == UnitAction.STOP) {
            for (int id : unitIds)
                if (level.getEntity(id) instanceof Unit unit)
                    unit.resetBehaviours();
        }
        else if (action == UnitAction.HOLD) {
            for (int id : unitIds) {
                if (level.getEntity(id) instanceof Unit unit) {
                    unit.resetBehaviours();
                    unit.setHoldPosition(true);
                }
            }
        }
        else if (action == UnitAction.MOVE) {
            for (int id : unitIds) {
                if (level.getEntity(id) instanceof Unit unit) {
                    unit.resetBehaviours();
                    unit.setMoveTarget(preselectedBlockPos);
                }
            }
        }
        else if (action == UnitAction.ATTACK_MOVE) {
            for (int id : unitIds) {
                if (level.getEntity(id) instanceof Unit unit) {
                    unit.resetBehaviours();
                    // if the unit can't actually attack just treat this as a move action
                    if (unit.canAttack())
                        unit.setAttackMoveTarget(preselectedBlockPos);
                    else
                        unit.setMoveTarget(preselectedBlockPos);
                }
            }
        }
        else if (action == UnitAction.ATTACK) {
            for (int id : unitIds) {
                if (level.getEntity(id) instanceof Unit unit) {
                    unit.resetBehaviours();
                    // if the unit can't actually attack just treat this as a follow action
                    if (unit.canAttack())
                        unit.setAttackTarget((LivingEntity) level.getEntity(unitId));
                    else
                        unit.setFollowTarget((LivingEntity) level.getEntity(unitId));
                }
            }
        }
        else if (action == UnitAction.FOLLOW) {
            for (int id : unitIds) {
                if (level.getEntity(id) instanceof Unit unit) {
                    unit.resetBehaviours();
                    unit.setFollowTarget((LivingEntity) level.getEntity(unitId));
                }
            }
        }
        else if (action == UnitAction.EXPLODE) {
            for (int id : unitIds) {
                if (level.getEntity(id) instanceof CreeperUnit unit) {
                    unit.resetBehaviours();
                    unit.explode();
                }
            }
        }
        else if (action == UnitAction.BUILD_REPAIR) {
            for (int id : unitIds) {
                if (level.getEntity(id) instanceof Unit unit) {
                    unit.resetBehaviours();
                    // if the unit can't actually build/repair just treat this as a move action
                    if (unit.isWorker()) {
                        Building building = BuildingUtils.findBuilding(BuildingServerEvents.getBuildings(), preselectedBlockPos);
                        unit.getBuildRepairGoal().setBuildingTarget(building);
                    }
                    else
                        unit.setMoveTarget(preselectedBlockPos);
                }
            }
        }
        else if (action == UnitAction.TOGGLE_GATHER_TARGET) {
            for (int id : unitIds) {
                if (level.getEntity(id) instanceof Unit unit) {
                    if (unit.isWorker())
                        unit.getGatherResourceGoal().toggleTargetResource();
                }
            }
        }
    }
}