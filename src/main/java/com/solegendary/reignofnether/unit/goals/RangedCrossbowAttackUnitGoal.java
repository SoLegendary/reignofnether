package com.solegendary.reignofnether.unit.goals;

import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;

public class RangedCrossbowAttackUnitGoal<T extends Monster & RangedAttackMob & CrossbowAttackMob> extends RangedCrossbowAttackGoal {
    public RangedCrossbowAttackUnitGoal(Monster monster, float range, float attackCooldown, float attackRadius) {
        super(monster, 1.0f, range);
    }
}
