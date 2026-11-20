package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.debug.RtsDebugServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.pathfinding.FlyingPathfinder;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

// Flies straight at the target (FlyingUnitMoveControl), as before. With rtsPathfinding on it additionally plans an
// RTS flying path (FlyingPathfinder) whenever the straight line is blocked; the navigation then steers the unit
// along it (Mob.serverAiStep ticks navigation after goals, so an active path overrides the direct steering below).
// With rtsPathfinding off none of the path code runs and this behaves exactly as it always did.
public class FlyingMoveToTargetGoal extends MoveToTargetBlockGoal {

    // a retarget further than this from the old target is a new order: drop the old path and plan afresh.
    // Closer retargets (eg. a ranged unit chasing a moving target re-sets it every tick) keep the current path
    // and rely on the throttled repath once it runs out.
    private static final double NEW_ORDER_DIST_SQR = 36;
    // the straight-line check sweeps the body box in steps, so it's only worth doing for a short hop; anything
    // further always gets a path
    private static final double MAX_DIRECT_CHECK_DIST = 64;
    // vanilla stuck detection never fires for these flyers (FlyingUnitMoveControl never sets the mob's speed), so
    // a unit that moved less than this over a recalc interval while on a path is treated as wedged on something
    private static final double WEDGED_DIST_SQR = 0.25;

    // the nav path we planned, so we only ever cancel our own (bats' melee goal also uses the navigation)
    @Nullable private Path ownedPath = null;
    @Nullable private Vec3 wedgeCheckPos = null;

    public FlyingMoveToTargetGoal(Mob mob, int reachRange) {
        super(mob, false, reachRange);
    }

    @Override
    public boolean isAtDestination() {
        if (moveTarget == null)
            return true;
        return this.mob.distanceToSqr(moveTarget.getX() + 0.5f, moveTarget.getY() + 0.5f, moveTarget.getZ() + 0.5f) < 4;
    }

    @Override
    public boolean canUse() {
        if (this.mob instanceof Unit unit && !unit.isFlyingUnit())
            return false;
        return moveTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!isAtDestination() && moveTarget != null) {
            if (!this.mob.level().isClientSide() && useRtsPathfinding())
                maintainFlyPath();
            this.start();
            return true;
        }
        else if (moveTarget == null)
            return false;
        else if (isAtDestination()) {
            if (!persistent && !((Unit) this.mob).getHoldPosition()) {
                moveTarget = null;
                this.mob.getMoveControl().operation = MoveControl.Operation.WAIT;
                dropFlyPath();
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
            MiscUtil.addUnitCheckpoint((Unit) mob, bp, true);

            if (this.mob.level().getBlockState(bp).isAir())
                return;

            BlockPos bpGround = bp;
            int y = 1;
            while(!MiscUtil.isGroundBlock(this.mob.level(), bpGround)) {
                bpGround = bp.offset(0,-y,0);
                y += 1;
                if (y > 30)
                    break;
            }
            BlockPos targetBp = bpGround.offset(0,10,0);
            double maxHeight = this.mob.level().getGameRules().getRule(GameRuleRegistrar.FLYING_MAX_Y_LEVEL).get();
            if (targetBp.getY() > maxHeight)
                targetBp = new BlockPos(targetBp.getX(), (int) maxHeight, targetBp.getZ());

            BlockPos oldTarget = this.moveTarget;
            this.moveTarget = targetBp;
            if (oldTarget == null || oldTarget.distSqr(targetBp) > NEW_ORDER_DIST_SQR)
                dropFlyPath();

        } else {
            this.moveTarget = null;
            this.mob.getMoveControl().operation = MoveControl.Operation.WAIT;
            dropFlyPath();
        }
        this.start();
    }

    @Override
    public void stopMoving() {
        this.moveTarget = null;
        this.mob.getMoveControl().operation = MoveControl.Operation.WAIT;
        dropFlyPath();

        if (this.mob.isVehicle() && this.mob.getPassengers().get(0) instanceof Unit unit)
            unit.getMoveGoal().stopMoving();
    }

    @Override
    protected void onPathReady(@Nullable Path path, boolean busy, int seq) {
        boolean current = seq == pathRequestSeq; // super drops a superseded result without touching the navigation
        super.onPathReady(path, busy, seq);
        if (current && !busy) {
            ownedPath = this.mob.getNavigation().getPath();
            wedgeCheckPos = null;
        }
    }

    // Runs every tick while flying to the target, rate-limited by recalcCooldown. While the navigation is
    // following one of our paths it only watches for the unit wedging; otherwise it plans a path if the straight
    // line to the target is blocked (and keeps flying direct if it's clear).
    private void maintainFlyPath() {
        if (pathPending)
            return;
        if (recalcCooldown > 0) {
            recalcCooldown--;
            return;
        }
        if (!this.mob.getNavigation().isDone()) {
            recalcCooldown = RECALC_COOLDOWN_MAX;
            Vec3 pos = this.mob.position();
            if (wedgeCheckPos != null && pos.distanceToSqr(wedgeCheckPos) < WEDGED_DIST_SQR) {
                stopOwnedNavPath(); // next pass replans, or flies direct if the way is clear after all
                recalcCooldown = 0;
            }
            wedgeCheckPos = pos;
            return;
        }
        wedgeCheckPos = null;
        if (isFlightLineClear(moveTarget)) {
            recalcCooldown = RECALC_COOLDOWN_MAX;
            return;
        }
        // same repath bookkeeping as the ground goal: onPathReady backs off if we end up where we were
        pendingRepath = true;
        repathFromFinalNode = getFinalNodePos();
        requestFlyPath();
    }

    private void requestFlyPath() {
        if (moveTarget == null)
            return;
        pathPending = true;
        final int seq = ++pathRequestSeq;
        final BooleanSupplier valid = () -> this.mob.isAlive() && this.pathRequestSeq == seq;
        RtsDebugServerEvents.debugPathCalcsThisSecond += 1;
        // armed before the request: the callback can fire synchronously and may overwrite it with a backoff
        recalcCooldown = currentRecalcCooldown;
        FlyingPathfinder.requestPath(this.mob, moveTarget, 0, valid, (path, busy) -> onPathReady(path, busy, seq));
    }

    // Can the body fly straight from where it is to the target? Sweeps its real bounding box along the line, at
    // most half its width per step so a thin obstacle can't be stepped over.
    private boolean isFlightLineClear(BlockPos to) {
        Vec3 delta = new Vec3(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5).subtract(this.mob.position());
        double len = delta.length();
        if (len > MAX_DIRECT_CHECK_DIST)
            return false;
        AABB box = this.mob.getBoundingBox();
        int steps = Math.max(1, (int) Math.ceil(len / Math.max(0.5, this.mob.getBbWidth() / 2)));
        for (int i = 1; i <= steps; i++) {
            AABB at = box.move(delta.scale((double) i / steps));
            if (this.mob.level().getBlockCollisions(this.mob, at).iterator().hasNext())
                return false;
        }
        return true;
    }

    private void stopOwnedNavPath() {
        if (ownedPath != null && this.mob.getNavigation().getPath() == ownedPath)
            this.mob.getNavigation().stop();
        ownedPath = null;
    }

    // forget everything about the current plan: cancel an in-flight request and our nav path, and let the next
    // maintainFlyPath pass replan immediately
    private void dropFlyPath() {
        pathRequestSeq++;
        pathPending = false;
        pendingRepath = false;
        repathFromFinalNode = null;
        wedgeCheckPos = null;
        stopOwnedNavPath();
        resetRecalcBackoff();
        recalcCooldown = 0;
    }
}
