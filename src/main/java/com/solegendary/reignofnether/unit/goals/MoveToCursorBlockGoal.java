package com.solegendary.reignofnether.unit.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class MoveToCursorBlockGoal extends Goal {

    private final PathfinderMob mob;
    private final double speedModifier;
    private BlockPos moveTarget = null;

    public MoveToCursorBlockGoal(PathfinderMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        return moveTarget != null;
    }

    public boolean canContinueToUse() {
        if (this.mob.getNavigation().isDone()) {
            moveTarget = null;
            return false;
        }
        return true;
    }

    public void start() {
        if (moveTarget != null) {
            // move to exact goal instead of 1 block away
            Path path = mob.getNavigation().createPath(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ(), 0);
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
}
