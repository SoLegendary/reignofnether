package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import net.minecraft.world.entity.Mob;

public class MeleeWindupAttackBuildingGoal extends MeleeAttackBuildingGoal {

    private KeyframeAnimated kfa;
    private int windupTicksLeft;

    public MeleeWindupAttackBuildingGoal(KeyframeAnimated kfa) {
        super((Mob) kfa);
        this.kfa = kfa;
        this.windupTicksLeft = kfa.getAttackWindupTicks();
    }

    @Override
    public void tick() {
        super.tick();
        if (isAttacking() && ticksToNextBlockBreak <= 0 && windupTicksLeft > 0) {
            if (windupTicksLeft == kfa.getAttackWindupTicks() &&
                    mob instanceof KeyframeAnimated && !mob.level().isClientSide()) {
                UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.ATTACK_UNIT, mob);
                if (mob instanceof AttackerUnit attackerUnit &&
                        attackerUnit.getAttackSound() != null) {
                    SoundClientboundPacket.playSoundAtPos(attackerUnit.getAttackSound(), mob.blockPosition());
                }
            }
            windupTicksLeft -= 1;
        }
    }

    @Override
    public void doBuildingAttack() {
        if (windupTicksLeft <= 0) {
            super.doBuildingAttack();
            windupTicksLeft = kfa.getAttackWindupTicks();
        }
    }
}
