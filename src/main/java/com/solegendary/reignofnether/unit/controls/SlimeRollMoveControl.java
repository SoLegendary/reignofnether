package com.solegendary.reignofnether.unit.controls;

import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.goals.MeleeAttackSlimeUnitGoal;
import com.solegendary.reignofnether.unit.units.monsters.SlimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

public class SlimeRollMoveControl extends MoveControl {

    private final SlimeUnit slime;

    /** How long the slide burst itself lasts, in ticks. */
    private int burstDurationTicks = 6;

    /** Rest time between bursts, in ticks. */
    private int restDurationTicks = 12;

    /** How long the "wind up" squish-contraction lasts before the burst fires, in ticks. */
    private int windUpDurationTicks = 4;

    /** Multiplier applied to the mob's movement speed attribute while actively sliding. */
    private double burstSpeedMultiplier = 1;

    /** How far negative (contracted) the squish target goes during wind-up. Negative = squashed flat. */
    private float windUpSquishTarget = 0.45F;

    /** How far positive (stretched) the squish target goes during the burst itself. Positive = stretched tall/long. */
    private float burstSquishTarget = -0.6F;

    /** Blend rate used when easing squish back to neutral during rest (0-1, higher = snappier). */
    private float restSquishDecay = 0.35F;

    // ---------------------------------------------------------------------
    // Internal state
    // ---------------------------------------------------------------------

    private enum SlidePhase {
        RESTING,
        WINDING_UP,
        BURSTING
    }

    private SlidePhase phase = SlidePhase.RESTING;
    private int phaseTicksRemaining = 0;

    public SlimeRollMoveControl(SlimeUnit pSlime) {
        super(pSlime);
        this.slime = pSlime;
        this.phaseTicksRemaining = this.restDurationTicks;
    }

    // distance to the target we can consider close enough to stop
    private float getGoalDistSqr() {
        float base = slime.getBbWidth();
        return base * base;
    }

    @Override
    public void tick() {
        BlockPos targetPos = slime.getMoveTarget();
        double distSqr = 999;
        if (targetPos != null) {
            distSqr = mob.distanceToSqr(targetPos.getCenter());
        }
        if (distSqr < getGoalDistSqr() || targetPos == null) {
            this.operation = Operation.WAIT;
        }

        if (this.operation != Operation.MOVE_TO) {
            this.mob.setZza(0.0F);
            decaySquishTowardsNeutral();
            return;
        }

        switch (this.phase) {
            case RESTING -> tickResting();
            case WINDING_UP -> tickWindingUp();
            case BURSTING -> tickBursting();
        }
    }

    private void tickResting() {
        // Not moving yet, just decaying squish back to neutral and waiting out the cooldown.
        this.mob.setSpeed(0.0F);
        this.mob.setZza(0.0F);
        decaySquishTowardsNeutral();

        if (--this.phaseTicksRemaining <= 0) {
            this.phase = SlidePhase.WINDING_UP;
            this.phaseTicksRemaining = this.windUpDurationTicks;
        }
    }

    private void tickWindingUp() {
        // Contract in place before the burst - telegraphs the movement.
        this.mob.setSpeed(0.0F);
        this.mob.setZza(0.0F);
        this.slime.targetSquish = this.windUpSquishTarget;

        if (--this.phaseTicksRemaining <= 0) {
            this.phase = SlidePhase.BURSTING;
            this.phaseTicksRemaining = this.burstDurationTicks;
            this.slime.targetSquish = this.burstSquishTarget;
        }
    }

    private void tickBursting() {
        // Face the target and rocket toward it for the duration of the burst.
        double dx = this.wantedX - this.mob.getX();
        double dz = this.wantedZ - this.mob.getZ();

        float targetYRot = (float) (Mth.atan2(dz, dx) * 57.2957763671875) - 90.0F;
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYRot, 90.0F));
        this.mob.yBodyRot = this.mob.getYRot();
        this.mob.yHeadRot = this.mob.getYRot();

        double speed = this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
        this.mob.setSpeed((float) speed);
        this.mob.setZza(1.0F);

        if (--this.phaseTicksRemaining <= 0) {
            endBurstAndRest();
        }
    }

    private void endBurstAndRest() {
        this.mob.setZza(0.0F);
        this.mob.setSpeed(0.0F);
        this.phase = SlidePhase.RESTING;
        this.phaseTicksRemaining = this.restDurationTicks;
        // Let the natural squish decay (below) ease it back to neutral over the rest period
        // rather than snapping, so the "landing" reads as a soft flatten-then-settle.
        this.slime.targetSquish = -this.burstSquishTarget * 0.5F;
    }

    private void decaySquishTowardsNeutral() {
        this.slime.targetSquish += (0.0F - this.slime.targetSquish) * this.restSquishDecay;
    }
}