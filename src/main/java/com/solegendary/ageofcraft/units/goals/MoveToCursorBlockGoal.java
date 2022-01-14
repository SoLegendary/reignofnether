package com.solegendary.ageofcraft.units.goals;

import com.solegendary.ageofcraft.cursor.CursorClientVanillaEvents;
import com.solegendary.ageofcraft.units.UnitCommonVanillaEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.EnumSet;

public class MoveToCursorBlockGoal extends Goal {

    private final PathfinderMob mob;
    private final double speedModifier;
    private final Level level;
    private final int maxDist = 20;
    private BlockPos targetBp = null;

    public MoveToCursorBlockGoal(PathfinderMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.level = mob.level;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    // only use if the target pos is close enough and the mob is selected
    public boolean canUse() {
        ArrayList<PathfinderMob> selectedUnits = UnitCommonVanillaEvents.getSelectedUnits();

        for (PathfinderMob unit : selectedUnits) {
            if (unit.getId() == mob.getId() && targetBp != null) {
                BlockPos mobbp = this.mob.blockPosition();
                int dist = targetBp.distManhattan(new Vec3i(mobbp.getX(), mobbp.getY()-1, mobbp.getZ()));
                return dist <= maxDist;
            }
        }
        return false;
    }

    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone();
    }

    public void start() {
        if (targetBp != null) {
            // move to exact goal instead of 1 block away
            Path path = mob.getNavigation().createPath(targetBp.getX(), targetBp.getY(), targetBp.getZ(), 0);
            this.mob.getNavigation().moveTo(path, speedModifier);
        }
    }

    public void setNewTargetBp(BlockPos bp) {
        this.targetBp = bp;
    }
}
