package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;

import javax.annotation.Nullable;

public class FlyingMoveToTargetGoal extends MoveToTargetBlockGoal {

    public FlyingMoveToTargetGoal(Mob mob, boolean persistent, int reachRange) {
        super(mob, persistent, reachRange);
    }

    private boolean isAtDestination() {
        if (moveTarget == null)
            return true;
        return this.mob.distanceToSqr(moveTarget.getX() + 0.5f, moveTarget.getY() + 0.5f, moveTarget.getZ() + 0.5f) < 4;
    }

    @Override
    public boolean canContinueToUse() {
        if (!isAtDestination() && moveTarget != null) {
            this.start();
            return true;
        }
        else if (moveTarget == null)
            return false;
        else if (isAtDestination()) {
            if (!persistent && !((Unit) this.mob).getHoldPosition()) {
                moveTarget = null;
                this.mob.getMoveControl().operation = MoveControl.Operation.WAIT;
            }
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        if (moveTarget != null) {
            this.mob.getLookControl().setLookAt(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ());
            this.mob.getMoveControl().setWantedPosition(moveTarget.getX() + 0.5f, moveTarget.getY() + 0.5f, moveTarget.getZ() + 0.5f, 1.0f);
        }
        else
            this.mob.getMoveControl().operation = MoveControl.Operation.WAIT;
    }

    @Override
    public void setMoveTarget(@Nullable BlockPos bp) {
        if (bp != null) {
            MiscUtil.addUnitCheckpoint((Unit) mob, bp);
            ((Unit) mob).setIsCheckpointGreen(true);
            this.moveTarget = bp.offset(0,10,0);
        } else {
            this.moveTarget = null;
            this.mob.getMoveControl().operation = MoveControl.Operation.WAIT;
        }
        this.start();
    }

    @Override
    public void stopMoving() {
        this.moveTarget = null;
        this.mob.getMoveControl().operation = MoveControl.Operation.WAIT;

        if (this.mob.isVehicle() && this.mob.getPassengers().get(0) instanceof Unit unit)
            unit.getMoveGoal().stopMoving();
    }
}
