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

    public ZombieUnit(EntityType<? extends Zombie> p_34271_, Level p_34272_) {
        super(p_34271_, p_34272_);
    }

    @Override
    protected void registerGoals() {
        this.moveGoal = new MoveToCursorBlockGoal(this, 1.0f);
        this.targetGoal = new SelectedTargetGoal(this, true, true);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, moveGoal);
        this.goalSelector.addGoal(3, new ZombieAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(3, targetGoal);
    }

    public void setMoveToBlock(@Nullable BlockPos bp) {
        targetGoal.setTarget(null);
        moveGoal.setNewTargetBp(bp);
    }

    // target MUST be a serverside entity or else it cannot be attacked
    public void setAttackTarget(@Nullable LivingEntity target) {
        moveGoal.setNewTargetBp(null);
        targetGoal.setTarget(target);
    }
}
