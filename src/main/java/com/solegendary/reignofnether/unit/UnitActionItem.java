package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

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

    public void action(ServerLevel level) {

        if (!level.isClientSide()) {
            if (action == UnitAction.STOP) {
                for (int id : unitIds) {
                    Unit unit = (Unit) level.getEntity(id);
                    if (unit != null)
                        unit.resetBehaviours();
                }
            }
            else if (action == UnitAction.HOLD) {
                for (int id : unitIds) {
                    Unit unit = (Unit) level.getEntity(id);
                    if (unit != null) {
                        unit.resetBehaviours();
                        unit.setHoldPosition(true);
                    }
                }
            }
            else if (action == UnitAction.MOVE) {
                for (int id : unitIds) {
                    Unit unit = (Unit) level.getEntity(id);
                    if (unit != null) {
                        unit.resetBehaviours();
                        unit.setMoveTarget(preselectedBlockPos);
                    }
                }
            }
            else if (action == UnitAction.ATTACK_MOVE) {
                for (int id : unitIds) {
                    Unit unit = (Unit) level.getEntity(id);
                    if (unit != null) {
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
                    Unit unit = (Unit) level.getEntity(id);
                    if (unit != null) {
                        unit.resetBehaviours();
                        // if the unit can't actually attack just treat this as a follow action
                        if (unit.canAttack())
                            unit.setAttackTarget((LivingEntity) level.getEntity(unitId));
                        else
                            unit.setFollowTarget((LivingEntity) level.getEntity(unitId));
                    }
                }
            }
            else if (action == UnitAction.BUILD_REPAIR) {
                for (int id : unitIds) {
                    Unit unit = (Unit) level.getEntity(id);
                    if (unit != null) {
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
            else if (action == UnitAction.FOLLOW) {
                for (int id : unitIds) {
                    Unit unit = (Unit) level.getEntity(id);
                    if (unit != null) {
                        unit.resetBehaviours();
                        unit.setFollowTarget((LivingEntity) level.getEntity(unitId));
                    }
                }
            }
        }
    }
}