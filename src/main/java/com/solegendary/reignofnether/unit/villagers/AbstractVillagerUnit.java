package com.solegendary.reignofnether.unit.villagers;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;

// Based on AbstractIllager
public abstract class AbstractVillagerUnit extends Raider {
    protected AbstractVillagerUnit(EntityType<? extends AbstractVillagerUnit> p_32105_, Level p_32106_) {
        super(p_32105_, p_32106_);
    }

    protected void registerGoals() {
        super.registerGoals();
    }

    public MobType getMobType() {
        return MobType.ILLAGER;
    }

    public AbstractVillagerUnit.VillagerUnitArmPose getArmPose() {
        return AbstractVillagerUnit.VillagerUnitArmPose.CROSSED;
    }

    public boolean canAttack(LivingEntity p_186270_) {
        return p_186270_ instanceof AbstractVillager && p_186270_.isBaby() ? false : super.canAttack(p_186270_);
    }

    public enum VillagerUnitArmPose {
        CROSSED,
        ATTACKING,
        SPELLCASTING,
        BOW_AND_ARROW,
        CROSSBOW_HOLD,
        CROSSBOW_CHARGE,
        CELEBRATING,
        NEUTRAL;
    }

    protected class RaiderOpenDoorGoal extends OpenDoorGoal {
        public RaiderOpenDoorGoal(Raider p_32128_) {
            super(p_32128_, false);
        }

        public boolean canUse() {
            return super.canUse() && AbstractVillagerUnit.this.hasActiveRaid();
        }
    }
}