package com.solegendary.reignofnether.unit.goals;

import java.util.EnumSet;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

// modified version of RangedBowAttackGoal which:
// - has an attack cooldown parameter in the constructor
// - has no pathfinding delay
// - stops when the target is dead

public class UnitCrossbowAttackGoal<T extends Monster & RangedAttackMob & CrossbowAttackMob> extends Goal {
    private final T mob;
    private UnitCrossbowAttackGoal.CrossbowState crossbowState = UnitCrossbowAttackGoal.CrossbowState.UNCHARGED;
    private final float attackRadiusSqr;
    private int seeTime;
    private int attackCooldown;
    private int attackCooldownMax;

    public UnitCrossbowAttackGoal(T mob, int attackCooldown, float attackRadius) {
        this.mob = mob;
        this.attackCooldownMax = attackCooldown;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        return this.isValidTarget() && this.isHoldingCrossbow();
    }

    private boolean isHoldingCrossbow() {
        return this.mob.isHolding(is -> is.getItem() instanceof CrossbowItem);
    }

    public boolean canContinueToUse() {
        return this.isValidTarget() && (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingCrossbow();
    }

    private boolean isValidTarget() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }

    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.mob.setTarget((LivingEntity)null);
        this.seeTime = 0;
        if (this.mob.isUsingItem()) {
            this.mob.stopUsingItem();
            this.mob.setChargingCrossbow(false);
            CrossbowItem.setCharged(this.mob.getUseItem(), false);
        }

    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target != null && target.isAlive()) {
            boolean flag = this.mob.getSensing().hasLineOfSight(target);
            boolean flag1 = this.seeTime > 0;
            if (flag != flag1) {
                this.seeTime = 0;
            }

            if (flag) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            double d0 = this.mob.distanceToSqr(target);
            boolean flag2 = (d0 > (double)this.attackRadiusSqr || this.seeTime < 5) && this.attackCooldown == 0;
            if (flag2 && !((Unit) this.mob).getHoldPosition()) {
                this.mob.getNavigation().moveTo(target, 1.0f);
            } else {
                this.mob.getNavigation().stop();
            }

            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.crossbowState == UnitCrossbowAttackGoal.CrossbowState.UNCHARGED) {
                if (!flag2) {
                    this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof CrossbowItem));
                    this.crossbowState = UnitCrossbowAttackGoal.CrossbowState.CHARGING;
                    this.mob.setChargingCrossbow(true);
                }
            } else if (this.crossbowState == UnitCrossbowAttackGoal.CrossbowState.CHARGING) {
                if (!this.mob.isUsingItem()) {
                    this.crossbowState = UnitCrossbowAttackGoal.CrossbowState.UNCHARGED;
                }

                int i = this.mob.getTicksUsingItem();
                ItemStack itemstack = this.mob.getUseItem();
                if (i >= CrossbowItem.getChargeDuration(itemstack)) {
                    this.mob.releaseUsingItem();
                    this.crossbowState = UnitCrossbowAttackGoal.CrossbowState.CHARGED;
                    this.attackCooldown = attackCooldownMax;
                    this.mob.setChargingCrossbow(false);
                }
            } else if (this.crossbowState == UnitCrossbowAttackGoal.CrossbowState.CHARGED) {
                --this.attackCooldown;
                if (this.attackCooldown == 0) {
                    this.crossbowState = UnitCrossbowAttackGoal.CrossbowState.READY_TO_ATTACK;
                }
            } else if (this.crossbowState == UnitCrossbowAttackGoal.CrossbowState.READY_TO_ATTACK && flag) {
                this.mob.performRangedAttack(target, 1.0F);
                ItemStack itemstack1 = this.mob.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof CrossbowItem));
                CrossbowItem.setCharged(itemstack1, false);
                this.crossbowState = UnitCrossbowAttackGoal.CrossbowState.UNCHARGED;
            }

        }
    }

    private boolean canRun() {
        return this.crossbowState == UnitCrossbowAttackGoal.CrossbowState.UNCHARGED;
    }

    enum CrossbowState {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK;
    }
}
