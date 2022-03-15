package com.solegendary.reignofnether.units.goals;

import com.solegendary.reignofnether.units.unit.ZombieUnit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

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
