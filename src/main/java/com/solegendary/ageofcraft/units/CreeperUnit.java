package com.solegendary.ageofcraft.units;

import com.solegendary.ageofcraft.units.goals.MoveToCursorBlockGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public class CreeperUnit extends Creeper {

    public CreeperUnit(EntityType<? extends Creeper> p_32278_, Level p_32279_) {
        super(p_32278_, p_32279_);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MoveToCursorBlockGoal(this, 1.0f));
    }
}
