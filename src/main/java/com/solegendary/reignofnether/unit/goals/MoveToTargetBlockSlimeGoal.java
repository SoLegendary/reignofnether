package com.solegendary.reignofnether.unit.goals;

import net.minecraft.world.entity.Mob;

public class MoveToTargetBlockSlimeGoal extends MoveToTargetBlockGoal {

    protected final int RECALC_COOLDOWN_MAX = 10;

    public MoveToTargetBlockSlimeGoal(Mob mob, boolean persistent, int reachRange) {
        super(mob, persistent, reachRange);
    }

    @Override
    public double getMinDistToRecalculateSqr() {
        return 4D;
    }

    // Slimes move in discrete jumps, not smooth walking, so the grid A* path (block-step waypoints)
    // makes them stutter and turn poorly. Keep them on vanilla pathfinding (see SlimeUnitMoveControl).
    @Override
    protected boolean useRtsPathfinding() {
        return false;
    }

    @Override
    protected void resetRecalcCooldown() { recalcCooldown = RECALC_COOLDOWN_MAX; }
}
