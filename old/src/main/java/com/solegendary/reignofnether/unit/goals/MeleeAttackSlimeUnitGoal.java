package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SlimeUnit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;

// slimes attack by landing a jump near enemies
public class MeleeAttackSlimeUnitGoal extends AbstractMeleeAttackUnitGoal {
    public boolean landedJump = false;

    public MeleeAttackSlimeUnitGoal(Mob mob, boolean followingTargetEvenIfNotSeen) {
        super(mob, followingTargetEvenIfNotSeen);
    }

    public void landedJump() {
        landedJump = true;
        tick();
        landedJump = false;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());

            if (!((Unit) this.mob).getHoldPosition()) {
                if (ticksUntilNextPathRecalculation <= 0) {
                    Path path = mob.getNavigation().createPath(target.getX(), target.getY(), target.getZ(), 0);
                    this.mob.getNavigation().moveTo(path, Unit.getSpeedModifier((Unit) this.mob));
                    if (distSqr < 16)
                        ticksUntilNextPathRecalculation = tickPathRecalcMax;
                    else if (distSqr < 64)
                        ticksUntilNextPathRecalculation = tickPathRecalcMax * 2;
                    else
                        ticksUntilNextPathRecalculation = tickPathRecalcMax * 4;
                } else {
                    ticksUntilNextPathRecalculation -= 1;
                }
            }
            this.checkAndPerformAttack(target, distSqr);
        }
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distSqr) {
        if (landedJump && this.mob instanceof SlimeUnit slimeUnit && slimeUnit.pushAttackCd <= 0) {
            ticksUntilNextAttack = 0;
            super.checkAndPerformAttack(target, distSqr);
            slimeUnit.pushAttackCd = slimeUnit.PUSH_ATTACK_CD_MAX;

            if (target instanceof SlimeUnit targetSlime)
                if (targetSlime.getAttackGoal() instanceof MeleeAttackSlimeUnitGoal goal &&
                    targetSlime.pushAttackCd <= 0 && targetSlime.getTarget() == this.mob) {
                    goal.landedJump = true;
                    goal.checkAndPerformAttack(this.mob, distSqr);
                    goal.landedJump = false;
                }
        }
    }
}
