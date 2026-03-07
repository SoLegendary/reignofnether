package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class MeleeWindupAttackUnitGoal extends AbstractMeleeAttackUnitGoal {
    private final KeyframeAnimated kfa;
    private int windupTicksLeft;


    public MeleeWindupAttackUnitGoal(KeyframeAnimated kfaMob, boolean followingTargetEvenIfNotSeen) {
        super((Mob) kfaMob, followingTargetEvenIfNotSeen);
        this.kfa = kfaMob;
        this.windupTicksLeft = kfaMob.getAttackWindupTicks();
    }

    public void tick() {
        super.tick();
    }

    // ensure that animation syncs with actual attack ticks by calling this from mob.tick()
    public void checkAndPerformAttackWithWindup() {
        LivingEntity target = mob.getTarget();
        if (target != null) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());

            double d = this.getAttackReachSqr(target);
            if ((distSqr <= d || windupTicksLeft < kfa.getAttackWindupTicks()) && this.ticksUntilNextAttack <= 0) {
                if (windupTicksLeft == kfa.getAttackWindupTicks() &&
                        mob instanceof KeyframeAnimated && !mob.level().isClientSide()) {
                    UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.ATTACK_UNIT, mob);

                    if (mob instanceof AttackerUnit attackerUnit &&
                        attackerUnit.getAttackSound() != null) {
                        SoundClientboundPacket.playSoundAtPos(attackerUnit.getAttackSound(), mob.blockPosition());
                    }
                }
                windupTicksLeft -= 1;
                if (windupTicksLeft <= 0) {
                    this.ticksUntilNextAttack = this.adjustedTickDelay(getAttackInterval());
                    this.mob.swing(InteractionHand.MAIN_HAND);
                    this.mob.doHurtTarget(target);
                    windupTicksLeft = kfa.getAttackWindupTicks();
                }
            }
        }
    }

    public void resetWindup() {
        this.windupTicksLeft = kfa.getAttackWindupTicks();
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distSqr) {

    }
}
