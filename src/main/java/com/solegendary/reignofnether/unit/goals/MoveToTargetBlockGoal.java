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
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.BooleanSupplier;

public class MoveToTargetBlockGoal extends Goal {

    protected final Mob mob;
    @Nullable protected BlockPos moveTarget = null;
    protected boolean persistent; // will keep trying to move back to the target if moved externally
    protected int moveReachRange = 0; // how far away from the target block to stop moving (manhattan distance)
    // True while this is a manual "disengage" MOVE order (plain move command), as opposed to attack-move,
    // gather, build, etc. While set, AttackerUnit auto-aggro (idle-aggression, retaliation, retarget) is
    // suppressed so a re-acquired enemy can't silently override the order the player gave - the attack goal
    // outranks the move goal, so without this a re-aggro hijacks the move. Cleared on arrival, stopMoving,
    // or any fresh target (a new attack/attack-move order goes through fullResetBehaviours -> stopMoving).
    protected boolean manualMove = false;
    public boolean isManualMove() { return manualMove && moveTarget != null; }
    public void setManualMove(boolean manualMove) { this.manualMove = manualMove; }

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
    // Mirrors the goal's active state (set in start(), cleared in stop()). The GoalSelector calls
    // start() itself on the idle -> active transition, so setMoveTarget only needs to fire start()
    // manually while already running - otherwise an idle unit would path twice for one order.
    protected boolean isRunning = false;
    // Bumped on every new request/order/stop. The async callback captures it at request time and drops its
    // result if it no longer matches, so a slow stale path can't overwrite a newer order.
    protected int pathRequestSeq = 0;
    // Set when the in-flight async request is a repath (not a fresh order). The async path isn't ready when
    // start() fires, so the stuck-backoff decision (same final node = still stuck -> grow; changed = progress
    // -> reset) is deferred to onPathReady, which compares the new path against repathFromFinalNode.
    protected boolean pendingRepath = false;
    @Nullable protected BlockPos repathFromFinalNode = null;

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
            // start() is expensive and repeats every tick on a stuck mob (eg. targeting over water), so it must
            // be throttled. The synchronous vanilla path has its new final node ready, so decide the backoff
            // and arm the cooldown gate right here. The async RTS path isn't ready yet (start() just fired the
            // request), so flag it and let onPathReady do the same once the new path is in.
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
                recalcCooldown = currentRecalcCooldown;
            } else {
                pendingRepath = true;
                repathFromFinalNode = oldFinalNode;
            }
            return true;
        }
        else if (moveTarget == null)
            return false;
        else if (this.mob.getNavigation().isDone()) {
            if (!persistent && !((Unit) this.mob).getHoldPosition()) {
                moveTarget = null;
                manualMove = false; // arrived: drop hold-fire so normal aggression resumes
            }
            return false;
        }
        return true;
    }

    public void start() {
        isRunning = true;
        // Cleared on every start(); canContinueToUse re-sets it after this call when it fires an async repath,
        // so a fresh order (started directly here) never inherits a previous repath's deferred backoff.
        pendingRepath = false;
        repathFromFinalNode = null;
        if (moveTarget == null) {
            this.mob.getNavigation().stop();
            return;
        }
        if (!(this.mob instanceof Unit u)) {
            this.mob.getNavigation().stop();
            return;
        }

        // When the rtsPathfinding gamerule is on, route through the async grid A* pathfinder.
        if (useRtsPathfinding()) {
            this.mob.setMaxUpStep(1.0f);
            if (this.mob.getNavigation() instanceof GroundPathNavigation gpn) gpn.setCanFloat(true);
            this.mob.getNavigation().stop();
            MobilityClass mobility = MobilityClass.of(u);
            pathPending = true;
            final int seq = ++pathRequestSeq;
            // Validity check run on the main thread before this request warms its chunks: drop it if the
            // mob died or a newer order bumped pathRequestSeq past this seq, so superseded requests don't
            // burn chunk-build budget (pathRequestSeq is only read/written on the main thread).
            final BooleanSupplier valid = () -> this.mob.isAlive() && this.pathRequestSeq == seq;
            // Count the async request too, so the F7 "Paths/s" meter reflects RTS pathfinding load (the
            // vanilla branch below already counts createPath calls); else the meter reads ~0 under
            // rtsPathfinding while async repaths flood.
            if (!this.mob.level().isClientSide()) RtsDebugServerEvents.debugPathCalcsThisSecond += 1;
            RtsPathfinder.requestPath(this.mob, moveTarget, moveReachRange, mobility, valid,
                    (path, busy) -> onPathReady(path, busy, seq));
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
    protected void onPathReady(@Nullable Path path, boolean busy, int seq) {
        // A newer order/stop superseded this request - drop the result. Leave pathPending alone; the newer
        // request is still in flight and owns it.
        if (seq != pathRequestSeq) return;
        pathPending = false;
        // Request was dropped under load (queue/pool saturated), not computed. Keep the current nav path
        // and retry after a short cooldown - do NOT stop() or back off. Stopping here is exactly what made
        // units shuffle back and forth: they'd stop, re-submit next tick, get dropped again, repeat.
        if (busy) {
            pendingRepath = false;
            repathFromFinalNode = null;
            recalcCooldown = RECALC_COOLDOWN_MAX;
            return;
        }
        // The deferred half of the repath throttle (see canContinueToUse): now that the async path is in we
        // can compare its final node to the pre-repath one. Same node (or no path at all) = still stuck, grow
        // the backoff; a changed node = progress, reset it. Then arm the cooldown gate so the next repath is
        // throttled. wasRepath is false for a fresh order, which must never back off.
        boolean wasRepath = pendingRepath;
        pendingRepath = false;
        if (moveTarget == null) { repathFromFinalNode = null; return; }
        if (!(this.mob instanceof Unit u)) {
            this.mob.getNavigation().stop();
            return;
        }
        if (path == null) {
            this.mob.getNavigation().stop();
            if (wasRepath) {
                backoffRecalcCooldown();
                recalcCooldown = currentRecalcCooldown;
            }
            repathFromFinalNode = null;
            return;
        }
        // Follow even a partial (unreachable) path so the unit still makes progress toward the target.
        this.mob.getNavigation().moveTo(path, Unit.getSpeedModifier(u));
        // The RTS pathfinder is async: node 0 is the unit's position when the request was SUBMITTED, a few
        // ticks ago. By delivery the unit has often drifted off it (crowd/formation separation, momentum),
        // and vanilla navigation always starts following at node 0 - so it would steer the unit BACK to the
        // stale start node before going forward, the "snap back to start" oscillation. Drop the leading nodes
        // already behind the unit by advancing to the node nearest its current position.
        snapPathToMob();
        if (wasRepath) {
            if (java.util.Objects.equals(repathFromFinalNode, getFinalNodePos()))
                backoffRecalcCooldown();
            else
                resetRecalcBackoff();
            recalcCooldown = currentRecalcCooldown;
        }
        repathFromFinalNode = null;
        if (!this.mob.level().isClientSide()) {
            byte type = path.canReach() ? RtsPathfinder.TYPE_ASTAR : RtsPathfinder.TYPE_FAILED;
            UnitPathClientboundPacket.sendPath(this.mob, path, type);
        }
    }

    // Advance the active nav path's next-node pointer to the node closest to the mob's CURRENT position,
    // trimming leading nodes it has already passed (or drifted away from). Reads the path back from the
    // navigation rather than trusting the passed-in reference, since vanilla moveTo can keep the prior path
    // when the new one is sameAs() it. Only searches forward from the current index, so the unit is never
    // rewound and a cell legitimately revisited later in the route isn't skipped.
    private void snapPathToMob() {
        Path p = this.mob.getNavigation().getPath();
        if (p == null || p.isDone())
            return;
        int best = p.getNextNodeIndex();
        double bestDistSqr = Double.MAX_VALUE;
        for (int i = best; i < p.getNodeCount(); i++) {
            Node n = p.getNode(i);
            double dx = (n.x + 0.5) - this.mob.getX();
            double dy = n.y - this.mob.getY();
            double dz = (n.z + 0.5) - this.mob.getZ();
            double distSqr = dx * dx + dy * dy + dz * dz;
            if (distSqr < bestDistSqr) {
                bestDistSqr = distSqr;
                best = i;
            }
        }
        p.setNextNodeIndex(best);
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
            manualMove = false; // a fresh target defaults to non-manual; the MOVE command re-flags it after
        }
        this.moveTarget = bp;

        // The engine's GoalSelector calls start() itself when this goal goes idle -> active, so only
        // fire start() manually when the goal is already running (re-issuing a target to a moving unit,
        // where canContinueToUse stays true and the engine won't re-path). Avoids double-pathing one order.
        if (changed && !this.mob.level().isClientSide() && isRunning)
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

    @Override
    public void stop() {
        isRunning = false;
    }

    public void stopMoving() {
        recalcCooldown = 0;
        pathPending = false;
        pendingRepath = false;
        repathFromFinalNode = null;
        manualMove = false;
        pathRequestSeq++; // cancel any in-flight path so a late result can't restart movement after a stop.
        this.moveTarget = null;
        this.mob.getNavigation().stop();
        if (this.mob.isVehicle() && this.mob.getPassengers().get(0) instanceof Unit unit)
            unit.getMoveGoal().stopMoving();
    }
}
