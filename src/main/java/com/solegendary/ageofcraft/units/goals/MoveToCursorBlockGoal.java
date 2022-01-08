package com.solegendary.ageofcraft.units.goals;

import com.solegendary.ageofcraft.cursor.CursorClientVanillaEvents;
import com.solegendary.ageofcraft.units.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.Path;

public class MoveToCursorBlockGoal extends MoveToBlockGoal {

    // TODO: make work with actual units once implemented
    private final Chicken unit;

    public MoveToCursorBlockGoal(Chicken p_32409_) {
        // entity, speedModifier, searchRange, verticalSearchRange
        super(p_32409_, 1.0D, 8, 2);
        this.unit = p_32409_;
    }

    public boolean canUse() {
        return super.canUse();
    }
    public boolean canContinueToUse() {
        return super.canContinueToUse();
    }

    // TODO: try unit.getNavigation().createPath(targetbp,0); to go exactly to the target block (not sure if this has to be in a goal?)
    protected boolean isValidTarget(LevelReader levelReader, BlockPos bp) {
        BlockPos targetbp = CursorClientVanillaEvents.getSelectedBlockPos();

        int dist = targetbp.distManhattan(new Vec3i(bp.getX(), bp.getY(), bp.getZ()));

        boolean valid = bp.getX() == targetbp.getX() &&
                bp.getY() == targetbp.getY() &&
                bp.getZ() == targetbp.getZ() &&
                dist < 20;

        if (valid) System.out.println("Found valid blockpos!");


        return valid;
    }

    public void start() {
        super.start();
    }

    public void stop() { super.stop(); }
}
