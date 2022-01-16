package com.solegendary.ageofcraft.units.goals;

import net.minecraft.world.entity.ai.goal.Goal;

// TODO Requirements:
// - Move to goal
// - if any enemies appear in a radius, then attack it
// - only chase said units for a particular distance

public class AttackMoveGoal extends Goal {

    @Override
    public boolean canUse() {
        return false;
    }
}
