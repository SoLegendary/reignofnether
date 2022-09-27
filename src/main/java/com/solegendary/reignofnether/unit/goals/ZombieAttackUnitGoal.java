package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.units.ZombieUnit;

public class ZombieAttackUnitGoal extends MeleeAttackUnitGoal {
    private final ZombieUnit zombieUnit;
    private int raiseArmTicks;

    public ZombieAttackUnitGoal(ZombieUnit zombieUnit, int attackInterval, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(zombieUnit, attackInterval, speedModifier, followingTargetEvenIfNotSeen);
        this.zombieUnit = zombieUnit;
    }

    public void start() {
        super.start();
        this.raiseArmTicks = 0;
    }

    public void stop() {
        super.stop();
        this.zombieUnit.setAggressive(false);
    }

    public void tick() {
        super.tick();
        ++this.raiseArmTicks;
        if (this.raiseArmTicks >= 5 && this.getTicksUntilNextAttack() < this.getAttackInterval() / 2) {
            this.zombieUnit.setAggressive(true);
        } else {
            this.zombieUnit.setAggressive(false);
        }

    }
}
