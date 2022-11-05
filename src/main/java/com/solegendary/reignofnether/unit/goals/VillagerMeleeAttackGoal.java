package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.villagers.VillagerUnit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Vindicator;

public class VillagerMeleeAttackGoal extends MeleeAttackGoal {
    public VillagerMeleeAttackGoal(VillagerUnit p_34123_) {
        super(p_34123_, 1.0D, false);
    }

    protected double getAttackReachSqr(LivingEntity p_34125_) {
        if (this.mob.getVehicle() instanceof Ravager) {
            float f = this.mob.getVehicle().getBbWidth() - 0.1F;
            return (double)(f * 2.0F * f * 2.0F + p_34125_.getBbWidth());
        } else {
            return super.getAttackReachSqr(p_34125_);
        }
    }
}
