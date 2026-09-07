package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.units.neutral.PandaUnit;
import net.minecraft.world.entity.Mob;

public class MeleeAttackUnitGoal extends AbstractMeleeAttackUnitGoal {
    private final Mob mob;
    private int raiseArmTicks; // for zombies

    public MeleeAttackUnitGoal(Mob mob, boolean followingTargetEvenIfNotSeen) {
        super(mob, followingTargetEvenIfNotSeen);
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
        if (this.mob instanceof PandaUnit pandaUnit && this.mob.getTarget() != null && ticksUntilNextAttack <= 0 &&
                this.mob.distanceTo(this.mob.getTarget()) < 4) {
            pandaUnit.roll(true);
        }

        super.tick();
        ++this.raiseArmTicks;
        if (this.raiseArmTicks >= 5 && this.getTicksUntilNextAttack() < this.getAttackInterval() / 2) {
            this.mob.setAggressive(true);
        } else {
            this.mob.setAggressive(false);
        }
    }
}
