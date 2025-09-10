package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.EnumSet;

import static com.solegendary.reignofnether.unit.interfaces.Unit.FOLLOW_RANGE;
import static com.solegendary.reignofnether.unit.interfaces.Unit.FOLLOW_RANGE_IMPROVED;

public class MoveToTargetBlockGoal extends Goal {

    protected final Mob mob;
    @Nullable protected BlockPos moveTarget = null;
    protected boolean persistent; // will keep trying to move back to the target if moved externally
    protected int moveReachRange = 0; // how far away from the target block to stop moving (manhattan distance)
    @Nullable public BlockPos lastSelectedMoveTarget = null; // ignores unit formations, used for reducing move actions sent to server

    protected final int RECALC_COOLDOWN_MAX = 20;
    protected void resetRecalcCooldown() { recalcCooldown = RECALC_COOLDOWN_MAX; }
    protected int recalcCooldown = 0; // limit start() used by canContinueToUse

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
        double dist = Math.max(1, moveReachRange);
        return dist * dist;
    }

    public boolean canUse() {
        return moveTarget != null;
    }

    public boolean canContinueToUse() {
        if (recalcCooldown > 0) {
            recalcCooldown -= 1;
            return true;
        }
        // PathNavigation seems to have a max length so restart it if we haven't actually reached the target yet
        if (this.mob.getNavigation().isDone() && moveTarget != null &&
            this.mob.getOnPos().distSqr(moveTarget) > getMinDistToRecalculateSqr()) {
            //BlockPos oldFinalNode = getFinalNodePos();
            this.start();
            //BlockPos newFinalNode = getFinalNodePos();
            // start() is very expensive, and it repeats every tick if the mob is stuck, eg. targeting over water
            //if (oldFinalNode != null && oldFinalNode.equals(newFinalNode))
            //    stopMoving();
            resetRecalcCooldown();
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
        if (moveTarget != null) {
            AttributeInstance ai = mob.getAttribute(Attributes.FOLLOW_RANGE);
            boolean improvedPathfinding = ai != null && ai.getBaseValue() == FOLLOW_RANGE_IMPROVED;
            Path bestPath;
            if (improvedPathfinding) {
                ai.setBaseValue(FOLLOW_RANGE);
                Path shortPath = mob.getNavigation().createPath(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ(), moveReachRange);
                BlockPos shortFinalPos = getFinalNodePos(shortPath);
                ai.setBaseValue(FOLLOW_RANGE_IMPROVED);
                Path longPath = mob.getNavigation().createPath(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ(), moveReachRange);
                BlockPos longFinalPos = getFinalNodePos(longPath);
                bestPath = longPath;
                if (shortFinalPos != null && longFinalPos != null) {
                    BlockPos moveTargetXZ = new BlockPos(moveTarget.getX(), 0, moveTarget.getZ());
                    double shortXZDist = new BlockPos(shortFinalPos.getX(), 0, shortFinalPos.getZ()).distSqr(moveTargetXZ);
                    double longXZDist = new BlockPos(longFinalPos.getX(), 0, longFinalPos.getZ()).distSqr(moveTargetXZ);
                    if (shortXZDist < longXZDist)
                        bestPath = shortPath;
                }
            } else {
                bestPath = mob.getNavigation().createPath(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ(), moveReachRange);
            }
            this.mob.getNavigation().moveTo(bestPath, Unit.getSpeedModifier((Unit) this.mob));
        }
        else
            this.mob.getNavigation().stop();
    }

    public void setMoveTarget(@Nullable BlockPos bp) {
        if (bp != null) {
            MiscUtil.addUnitCheckpoint((Unit) mob, bp, true);
        }
        this.moveTarget = bp;

        if (!this.mob.level().isClientSide())
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

    @Nullable public BlockPos getFinalNodePos(Path path) {
        if (path != null && !path.nodes.isEmpty())
            return path.nodes.get(path.nodes.size() - 1).asBlockPos();
        return null;
    }

    public void stopMoving() {
        recalcCooldown = 0;
        this.moveTarget = null;
        this.mob.getNavigation().stop();
        if (this.mob.isVehicle() && this.mob.getPassengers().get(0) instanceof Unit unit)
            unit.getMoveGoal().stopMoving();
    }
}
