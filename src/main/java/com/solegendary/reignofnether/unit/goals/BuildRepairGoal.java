package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.Building;
import net.minecraft.world.entity.ai.goal.Goal;

public class BuildRepairGoal extends Goal {
    public void setTarget(Building target) {
    }

    public boolean canUse() {
        return false;
    }
}
