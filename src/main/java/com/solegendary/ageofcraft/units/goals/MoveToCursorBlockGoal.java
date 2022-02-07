package com.solegendary.ageofcraft.units.goals;

import com.solegendary.ageofcraft.units.Unit;
import com.solegendary.ageofcraft.units.UnitCommonVanillaEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;

public class MoveToCursorBlockGoal extends Goal {

    private final PathfinderMob mob;
    private final double speedModifier;
    private final int maxDist = 20;
    private BlockPos moveTarget = null;

    public MoveToCursorBlockGoal(PathfinderMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    // only use if the target pos is close enough and the mob is selected
    public boolean canUse() {
        ArrayList<Integer> selectedUnitIds = UnitCommonVanillaEvents.getSelectedUnitIds();

        for (int unitId : selectedUnitIds) {
            if (unitId == mob.getId() && moveTarget != null) {
                BlockPos mobbp = this.mob.blockPosition();
                int dist = moveTarget.distManhattan(new Vec3i(mobbp.getX(), mobbp.getY()-1, mobbp.getZ()));
                return dist <= maxDist;
            }
        }
        return false;
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

    public void setMoveTarget(@Nullable BlockPos bp) {
        this.moveTarget = bp;
        this.start();
    }

    public BlockPos getMoveTarget() {
        return this.moveTarget;
    }
}
