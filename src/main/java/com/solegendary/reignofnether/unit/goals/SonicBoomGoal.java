package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class SonicBoomGoal extends MoveToTargetBlockGoal {

    Ability ability;
    LivingEntity targetEntity;

    public SonicBoomGoal(PathfinderMob mob) {
        super(mob, false, 1.0f, 0);
    }

    // if we set an entity target, on every tick we will follow that target
    // if we set a BlockPos as the target, remove any entity target
    // the user will only start casting once we reach the target
    public void setTarget(LivingEntity entity) {
        this.targetEntity = entity;
    }
    public void setTarget(BlockPos bpTarget) {
        this.setMoveTarget(bpTarget);
        this.setTarget((LivingEntity) null);
    }

    public void setAbility(Ability ability) {
        this.ability = ability;
    }

    @Override
    public void tick() {

    }

    @Override
    public void stop() {

    }
}
