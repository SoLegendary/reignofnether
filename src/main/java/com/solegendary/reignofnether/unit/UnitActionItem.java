package com.solegendary.reignofnether.unit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;

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

        System.out.println("Resolving action: " + action);
        System.out.println(unitId);
        System.out.println(Arrays.toString(unitIds));

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
                        unit.setAttackMoveTarget(preselectedBlockPos);
                    }
                }
            }
            else if (action == UnitAction.ATTACK) {
                for (int id : unitIds) {
                    Unit unit = (Unit) level.getEntity(id);
                    if (unit != null) {
                        unit.resetBehaviours();
                        unit.setAttackTarget((LivingEntity) level.getEntity(unitId));
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