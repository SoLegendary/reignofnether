//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.goals;

import java.util.EnumSet;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;

// FloatGoal but excludes lava, since striders can walk on lava anyway
public class StriderFloatGoal extends Goal {
    private final Mob mob;

    public StriderFloatGoal(Mob pMob) {
        this.mob = pMob;
        this.setFlags(EnumSet.of(Flag.JUMP));
        pMob.getNavigation().setCanFloat(true);
    }

    public boolean canUse() {
        return this.mob.isInWater() && this.mob.getFluidHeight(FluidTags.WATER) > this.mob.getFluidJumpThreshold() || this.mob.isInFluidType(
            (fluidType, height) -> this.mob.canSwimInFluidType(fluidType) && height > this.mob.getFluidJumpThreshold()
        );
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if (this.mob.getRandom().nextFloat() < 0.8F) {
            this.mob.getJumpControl().jump();
        }
    }
}
