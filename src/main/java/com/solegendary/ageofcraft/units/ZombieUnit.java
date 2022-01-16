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

    public ZombieUnit(EntityType<? extends Zombie> p_34271_, Level p_34272_) {
        super(p_34271_, p_34272_);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MoveToCursorBlockGoal(this, 1.0f));
        this.goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(2, new SelectedTargetGoal(this, true, true));
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
    }
}
