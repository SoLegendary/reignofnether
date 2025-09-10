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

    @Override
    protected void resetRecalcCooldown() { recalcCooldown = RECALC_COOLDOWN_MAX; }
}
