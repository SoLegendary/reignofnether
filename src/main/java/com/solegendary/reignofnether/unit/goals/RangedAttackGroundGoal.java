package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

// allows use of the AttackGroundAbility
// dependent on the unit having a UnitBowAttackGoal

public class RangedAttackGroundGoal<T extends Mob> extends Goal {
    private final T mob;
    private BlockPos groundTarget = null;
    private UnitBowAttackGoal<?> bowAttackGoal = null;
    private UnitCrossbowAttackGoal<?> cbowAttackGoal = null;
    private final boolean attackWhileMoving;
    private boolean reachedTarget = false;

    public RangedAttackGroundGoal(T mob, boolean attackWhileMoving, UnitBowAttackGoal<?> bowAttackGoal) {
        this.mob = mob;
        this.attackWhileMoving = attackWhileMoving;
        this.bowAttackGoal = bowAttackGoal;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public RangedAttackGroundGoal(T mob, boolean attackWhileMoving, UnitCrossbowAttackGoal<?> cbowAttackGoal) {
        this.mob = mob;
        this.attackWhileMoving = attackWhileMoving;
        this.cbowAttackGoal = cbowAttackGoal;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
        return this.groundTarget != null;
    }

    public boolean canContinueToUse() {
        if (!this.canUse() && this.isDoneMoving())
            return false;
        return true;
    }

    public void start() {
        super.start();
        this.mob.setAggressive(true);
    }

    public void stop() {
        super.stop();
        this.setGroundTarget(null);
        this.mob.setAggressive(false);
        this.reachedTarget = false;
    }

    public BlockPos getGroundTarget() {
        return this.groundTarget;
    }

    public void setGroundTarget(BlockPos groundTarget) {
        this.groundTarget = groundTarget;
        if (groundTarget != null) {
            MiscUtil.addUnitCheckpoint(((Unit) mob), groundTarget, false);
        } else {
            reachedTarget = false;
        }
        if (groundTarget != null) {
            ((Unit) mob).getTargetGoal().setTarget(null);
        }
    }

    public void tick() {
        if (groundTarget != null) {
            float tx = groundTarget.getX() + 0.5f;
            float ty = groundTarget.getY() + 1.0f;
            float tz = groundTarget.getZ() + 0.5f;

            this.mob.getLookControl().setLookAt(tx, ty, tz);

            if (this.mob.level().isClientSide())
                return;

            float attackRange = ((AttackerUnit) this.mob).getAttackRange();

            double distToTarget = Math.sqrt(this.mob.distanceToSqr(tx, ty, tz));

            if (distToTarget <= attackRange) {
                if (!reachedTarget) {
                    this.stopMoving();
                }
                reachedTarget = true;
            }

            if (!attackWhileMoving || !reachedTarget) {
                if ((distToTarget > attackRange) &&
                        !((Unit) this.mob).getHoldPosition()) {
                    this.moveTo(this.groundTarget);
                } else if (!attackWhileMoving) {
                    this.stopMoving();
                }
            }
            if (distToTarget <= attackRange) { // start drawing bowstring
                if (bowAttackGoal != null) {
                    if (bowAttackGoal.getAttackCooldown() <= 0) {
                        if (mob instanceof RangedAttackerUnit rangedAttackerUnit)
                            rangedAttackerUnit.performUnitRangedAttack(tx, ty, tz, 20);
                        bowAttackGoal.setToMaxAttackCooldown();
                    }
                }
            }
            // handle crossbow attacks (ie. pillager artillery attacking grounding) in UnitCrossbowAttackGoal
            // because we can't directly use mob.performUnitRangedAttack()
        }
    }

    // moveGoal controllers
    private boolean isDoneMoving() {
        Unit unit = (Unit) this.mob;
        if (unit.getMoveGoal() instanceof FlyingMoveToTargetGoal flyingMoveGoal)
            return flyingMoveGoal.isAtDestination();
        else
            return this.mob.getNavigation().isDone();
    }

    private void stopMoving() {
        Unit unit = (Unit) this.mob;
        if (unit.getMoveGoal() instanceof FlyingMoveToTargetGoal flyingMoveGoal)
            flyingMoveGoal.stopMoving();
        else
            this.mob.getNavigation().stop();
    }

    private void moveTo(BlockPos bp) {
        Unit unit = (Unit) this.mob;
        if (unit.getMoveGoal() instanceof FlyingMoveToTargetGoal flyingMoveGoal)
            flyingMoveGoal.setMoveTarget(bp);
        else
            this.mob.getNavigation().moveTo(bp.getX(), bp.getY(), bp.getZ(), 1.0f);
    }
}
