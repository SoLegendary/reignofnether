package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.debug.RtsDebugServerEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitPathClientboundPacket;
import com.solegendary.reignofnether.unit.pathfinding.MobilityClass;
import com.solegendary.reignofnether.unit.pathfinding.PathfinderConfig;
import com.solegendary.reignofnether.unit.pathfinding.RtsPathfinder;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class MoveToTargetBlockGoal extends Goal {

    protected final Mob mob;
    @Nullable protected BlockPos moveTarget = null;
    protected boolean persistent; // will keep trying to move back to the target if moved externally
    protected int moveReachRange = 0; // how far away from the target block to stop moving (manhattan distance)
    @Nullable public BlockPos lastSelectedMoveTarget = null; // ignores unit formations, used for reducing move actions sent to server

    protected final int RECALC_COOLDOWN_MAX = 20;
    protected static final int RECALC_COOLDOWN_CAP = 200; // ~10s cap for exponential backoff on stuck units
    protected int currentRecalcCooldown = RECALC_COOLDOWN_MAX;
    protected void backoffRecalcCooldown() {
        currentRecalcCooldown = Math.min(currentRecalcCooldown * 2, RECALC_COOLDOWN_CAP);
    }
    protected void resetRecalcBackoff() { currentRecalcCooldown = RECALC_COOLDOWN_MAX; }
    public boolean isInBackoff() { return currentRecalcCooldown > RECALC_COOLDOWN_MAX && moveTarget != null; }
    protected int recalcCooldown = 0; // limit start() used by canContinueToUse
    protected boolean pathPending = false; // true while an async RTS path request is in flight
    // Bumped on every new request/order/stop. The async callback captures it at request time and drops its
    // result if it no longer matches, so a slow stale path can't overwrite a newer order.
    protected int pathRequestSeq = 0;

    public MoveToTargetBlockGoal(Mob mob, boolean persistent, int reachRange) {
        this.mob = mob;
        this.persistent = persistent;
        this.moveReachRange = reachRange;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean isAtDestination() {
        if (moveTarget == null)
            return true;
        return mob.getNavigation().isDone();
    }

    public double getMinDistToRecalculateSqr() {
        // Floor the recalc radius at the separation settle distance, else there's a band where a unit settles
        // via separation but still triggers a recalc every tick - that mismatch is what makes arrived units dance.
        double dist = Math.max(Math.sqrt(PathfinderConfig.ARRIVAL_SETTLE_SQ), moveReachRange);
        return dist * dist;
    }

    // Whether this unit routes through the async RTS grid pathfinder. Overridden to false for units the grid
    // path doesn't suit (eg. jump-based slimes), keeping them on vanilla pathfinding.
    protected boolean useRtsPathfinding() {
        return UnitServerEvents.rtsPathfinding;
    }

    public boolean canUse() {
        if (this.mob instanceof Unit unit && unit.isFlyingUnit())
            return false;
        return moveTarget != null;
    }

    public boolean canContinueToUse() {
        // Keep the goal active (without recalculating) while an async RTS path is being computed.
        if (pathPending)
            return moveTarget != null;
        if (recalcCooldown > 0) {
            recalcCooldown -= 1;
            return true;
        }
        // PathNavigation has a max length, so once the unit finishes its current path but still isn't at the
        // target, restart to continue the journey (RTS delivers long routes in segments) or retry.
        if (this.mob.getNavigation().isDone() && moveTarget != null &&
            this.mob.getOnPos().distSqr(moveTarget) > getMinDistToRecalculateSqr()) {
            BlockPos oldFinalNode = getFinalNodePos();
            this.start();
            // start() is expensive and repeats every tick on a stuck mob (eg. targeting over water). Only the
            // synchronous vanilla path has a final node ready to compare for the backoff; the async RTS path
            // isn't ready yet (start() just fired the request), so it simply repaths next time it's done.
            if (!pathPending) {
                BlockPos newFinalNode = getFinalNodePos();
                if (oldFinalNode != null && oldFinalNode.equals(newFinalNode))
                    backoffRecalcCooldown();
                else
                    resetRecalcBackoff();
            }
            return true;
        }
        else if (moveTarget == null)
            return false;
        else if (this.mob.getNavigation().isDone()) {
            if (!persistent && !((Unit) this.mob).getHoldPosition()) {
                moveTarget = null;
            }
            return false;
        }
        return true;
    }

    public void start() {
        if (moveTarget == null) {
            this.mob.getNavigation().stop();
            return;
        }
        if (!(this.mob instanceof Unit u)) {
            this.mob.getNavigation().stop();
            return;
        }

        System.out.println("MoveToTargetBlockGoal start: " + mob.getName().getString() + "|" + mob.getId() + "|" + pathRequestSeq);

        // When the rtsPathfinding gamerule is on, route through the async grid A* pathfinder.
        if (useRtsPathfinding()) {
            this.mob.setMaxUpStep(1.0f);
            if (this.mob.getNavigation() instanceof GroundPathNavigation gpn) gpn.setCanFloat(true);
            this.mob.getNavigation().stop();
            MobilityClass mobility = MobilityClass.of(u);
            pathPending = true;
            final int seq = ++pathRequestSeq;
            RtsPathfinder.requestPath(this.mob, moveTarget, moveReachRange, mobility, path -> onPathReady(path, seq));
            return;
        }

        // Vanilla pathfinding: one createPath call. If the path ends up suboptimal, the backoff in
        // canContinueToUse handles retries / give-up.
        Path path = mob.getNavigation().createPath(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ(), moveReachRange);
        if (!this.mob.level().isClientSide()) RtsDebugServerEvents.debugPathCalcsThisSecond += 1;
        /*
        if (path == null) {
            AttributeInstance ai = mob.getAttribute(Attributes.FOLLOW_RANGE);
            if (ai != null && ai.getBaseValue() == FOLLOW_RANGE_IMPROVED) {
                // Fallback: long-range search bailed (eg. hit maxVisitedNodes). Retry with the short
                // range — vanilla A* may give up sooner and return a useful partial path.
                ai.setBaseValue(FOLLOW_RANGE);
                path = mob.getNavigation().createPath(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ(), moveReachRange);
                if (!this.mob.level().isClientSide()) RtsDebugServerEvents.debugPathCalcsThisSecond += 1;
                ai.setBaseValue(FOLLOW_RANGE_IMPROVED);
            }
        }
         */
        this.mob.getNavigation().moveTo(path, Unit.getSpeedModifier(u));
        // Broadcast the path so clients can render it briefly. Server-only — clients
        // that received the packet decide whether to render based on ownership/FOW.
        if (!this.mob.level().isClientSide()) {
            byte type = (path != null && !path.canReach()) ? RtsPathfinder.TYPE_FAILED : RtsPathfinder.TYPE_VANILLA;
            UnitPathClientboundPacket.sendPath(this.mob, path, type);
        }
    }

    // Callback for the async RTS pathfinder. Runs on the server thread once a path is ready.
    protected void onPathReady(@Nullable Path path, int seq) {
        // A newer order/stop superseded this request - drop the result. Leave pathPending alone; the newer
        // request is still in flight and owns it.
        if (seq != pathRequestSeq) return;
        pathPending = false;
        if (moveTarget == null) return;
        if (!(this.mob instanceof Unit u)) {
            this.mob.getNavigation().stop();
            return;
        }
        if (path == null) {
            this.mob.getNavigation().stop();
            return;
        }
        // Follow even a partial (unreachable) path so the unit still makes progress toward the target.
        this.mob.getNavigation().moveTo(path, Unit.getSpeedModifier(u));
        if (!this.mob.level().isClientSide()) {
            byte type = path.canReach() ? RtsPathfinder.TYPE_ASTAR : RtsPathfinder.TYPE_FAILED;
            UnitPathClientboundPacket.sendPath(this.mob, path, type);
        }
    }

    public void setMoveTarget(@Nullable BlockPos bp) {
        if (bp != null) {
            MiscUtil.addUnitCheckpoint((Unit) mob, bp, true);
        }
        // Only fire a fresh path on an actual target CHANGE. GatherResourcesGoal re-asserts the same block
        // target every tick for persistence; pathing on each would spam snap-and-fail requests at solid resource
        // blocks. A real change also bumps the request seq in start(), superseding any stale path still
        // computing for the old target. A stall on an unchanged target is re-pathed by canContinueToUse instead.
        boolean changed = !java.util.Objects.equals(bp, this.moveTarget);
        if (changed) {
            resetRecalcBackoff();
            recalcCooldown = 0;
        }
        this.moveTarget = bp;

        if (changed && !this.mob.level().isClientSide())
            this.start();
    }

    public BlockPos getMoveTarget() {
        return this.moveTarget;
    }

    @Nullable public BlockPos getFinalNodePos() {
        Path path = this.mob.getNavigation().getPath();
        if (path != null && !path.nodes.isEmpty())
            return path.nodes.get(path.nodes.size() - 1).asBlockPos();
        return null;
    }

    public void stopMoving() {
        recalcCooldown = 0;
        pathPending = false;
        pathRequestSeq++; // cancel any in-flight path so a late result can't restart movement after a stop.
        this.moveTarget = null;
        this.mob.getNavigation().stop();
        if (this.mob.isVehicle() && this.mob.getPassengers().get(0) instanceof Unit unit)
            unit.getMoveGoal().stopMoving();
    }
}
