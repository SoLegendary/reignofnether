package com.solegendary.reignofnether.unit.goals;

import net.minecraft.world.entity.PathfinderMob;

public class MeleeAttackUnitGoal extends AbstractMeleeAttackUnitGoal {
    private final PathfinderMob mob;
    private int raiseArmTicks;

    public MeleeAttackUnitGoal(PathfinderMob mob, int attackInterval, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(mob, attackInterval, speedModifier, followingTargetEvenIfNotSeen);
        this.mob = mob;
    }

    public void start() {
        super.start();
        this.raiseArmTicks = 0;
    }

    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
    }

    public void tick() {
        super.tick();
        ++this.raiseArmTicks;
        if (this.raiseArmTicks >= 5 && this.getTicksUntilNextAttack() < this.getAttackInterval() / 2) {
            this.mob.setAggressive(true);
        } else {
            this.mob.setAggressive(false);
        }

    }
}
