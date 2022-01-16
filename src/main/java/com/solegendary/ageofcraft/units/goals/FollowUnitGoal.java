package com.solegendary.ageofcraft.units.goals;

import net.minecraft.world.entity.ai.goal.Goal;

// TODO
// Follows a friendly unit right clicked until told to do something else

public class FollowUnitGoal extends Goal {

    @Override
    public boolean canUse() {
        return false;
    }
}
