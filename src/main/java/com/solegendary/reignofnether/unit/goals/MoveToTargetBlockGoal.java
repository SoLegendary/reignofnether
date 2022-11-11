package com.solegendary.reignofnether.unit.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class MoveToTargetBlockGoal extends Goal {

    protected final PathfinderMob mob;
    protected final double speedModifier;
    protected BlockPos moveTarget = null;
    protected boolean persistent; // will keep trying to move back to the target if moved externally
    protected int reachRange = 0; // how far away from the target block to stop moving (manhattan distance)

    public MoveToTargetBlockGoal(PathfinderMob mob, boolean persistent, double speedModifier, int reachRange) {
        this.mob = mob;
        this.persistent = persistent;
        this.speedModifier = speedModifier;
        this.reachRange = reachRange;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        return moveTarget != null;
    }

    public boolean canContinueToUse() {
        if (this.mob.getNavigation().isDone() || moveTarget == null) {
            if (!persistent)
                moveTarget = null;
            return false;
        }
        return true;
    }

    public void start() {
        if (moveTarget != null) {
            // move to exact goal instead of 1 block away
            Path path = mob.getNavigation().createPath(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ(), reachRange);
            this.mob.getNavigation().moveTo(path, speedModifier);
        }
        else
            this.mob.getNavigation().stop();
    }

    // TODO: can sometimes fail to go back uphill when knocked downhill by attacks
    public void setTarget(@Nullable BlockPos bp) {
        this.moveTarget = bp;
        this.start();
    }

    public BlockPos getTarget() {
        return this.moveTarget;
    }

    public void stop() {
        this.moveTarget = null;
        this.mob.getNavigation().stop();
    }
}
