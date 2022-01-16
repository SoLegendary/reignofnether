package com.solegendary.ageofcraft.units;

import com.solegendary.ageofcraft.units.goals.MoveToCursorBlockGoal;
import com.solegendary.ageofcraft.units.goals.SelectedTargetGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class SkeletonUnit extends Skeleton implements Unit {

    public SkeletonUnit(EntityType<? extends Skeleton> p_33570_, Level p_33571_) {
        super(p_33570_, p_33571_);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MoveToCursorBlockGoal(this, 1.0f));
        this.goalSelector.addGoal(2, new RangedBowAttackGoal<>(this, 1.0D, 20, 15.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(2, new SelectedTargetGoal(this, true, false));
    }

    @Override
    public void setMoveToBlock(BlockPos bp) {
        for (WrappedGoal goal : this.goalSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof MoveToCursorBlockGoal) {
                MoveToCursorBlockGoal moveGoal = (MoveToCursorBlockGoal) goal.getGoal();
                moveGoal.setNewTargetBp(bp);
            }
        }
    }

    public void setAttackTarget(@Nullable LivingEntity target) {
        for (WrappedGoal goal : this.goalSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof SelectedTargetGoal) {
                SelectedTargetGoal targetGoal = (SelectedTargetGoal) goal.getGoal();
                targetGoal.setTarget(target);
            }
        }
        for (WrappedGoal goal : this.goalSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof ZombieAttackGoal) {
                ZombieAttackGoal targetGoal = (ZombieAttackGoal) goal.getGoal();
                targetGoal.start();
            }
        }
    }
}
