package com.solegendary.ageofcraft.units;

import com.solegendary.ageofcraft.units.goals.SelectedTargetGoal;
import com.solegendary.ageofcraft.units.goals.MoveToCursorBlockGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class ZombieUnit extends Zombie implements Unit {

    MoveToCursorBlockGoal moveGoal;
    SelectedTargetGoal targetGoal;

    private BlockPos attackMoveTarget = null;
    private final float attackMoveRange = 5;
    private BlockPos attackMoveAnchor = null;
    private LivingEntity followTarget = null;

    public ZombieUnit(EntityType<? extends Zombie> p_34271_, Level p_34272_) {
        super(p_34271_, p_34272_);
    }

    public Boolean isAttackMoving() { return attackMoveTarget != null; }
    public Boolean isFollowing() { return followTarget != null; }

    @Override
    protected void registerGoals() {
        this.moveGoal = new MoveToCursorBlockGoal(this, 1.0f);
        this.targetGoal = new SelectedTargetGoal(this, true, true);

        // TODO: extend zombie goal to always null target on end
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, moveGoal);
        this.goalSelector.addGoal(3, new ZombieAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(3, targetGoal);
    }

    public void resetTargets() {
        this.attackMoveTarget = null;
        targetGoal.setTarget(null);
        moveGoal.setMoveTarget(null);
        this.followTarget = null;
    }

    public void setMoveTarget(@Nullable BlockPos bp) {
        resetTargets();
        moveGoal.setMoveTarget(bp);
    }

    // target MUST be a serverside entity or else it cannot be attacked
    public void setAttackTarget(@Nullable LivingEntity target) {
        resetTargets();
        targetGoal.setTarget(target);
    }

    public void setAttackMoveTarget(@Nullable BlockPos bp) {
        resetTargets();
        this.attackMoveTarget = bp;
    }

    public void setFollowTarget(@Nullable LivingEntity target) {
        resetTargets();
        this.followTarget = target;
    }
}
