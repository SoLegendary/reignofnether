package com.solegendary.reignofnether.units.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Predicate;

// Requirements:
// - top priority
// - follow and attack always even if out of LOS (first go to last seen position then retry)
// - don't distinguish friend from foe, only do that in the cursor options (attack allies if A clicked)

public class SelectedTargetGoal<T extends LivingEntity> extends TargetGoal {
    protected LivingEntity target;
    protected TargetingConditions targetConditions;

    public SelectedTargetGoal(Mob mob, boolean mustSee, boolean mustReach) {
        this(mob, mustSee, mustReach, null);
    }

    public SelectedTargetGoal(Mob mob, boolean mustSee, boolean mustReach, @Nullable Predicate<LivingEntity> condition) {
        super(mob, mustSee, mustReach);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        this.targetConditions = TargetingConditions.forCombat().range(this.getFollowDistance()).selector(condition);
    }

    public boolean canUse() {
        return this.target != null;
    }

    public void start() {
        this.mob.setTarget(this.target);
        super.start();
    }

    public void setTarget(@Nullable LivingEntity target) {
        this.target = target;
        this.start();
    }

    public LivingEntity getTarget() {
        return this.target;
    }
}