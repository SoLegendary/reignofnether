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

public class CastFangsLineGoal extends MoveToTargetBlockGoal {

    private LivingEntity targetEntity = null;
    private Ability ability; // used for syncing cooldown with clientside
    private int ticksCasting = 0; // how long have we spent trying to cast this spell
    public boolean isCasting() { return isCasting; }
    public final static int TICKS_CASTING_MAX = 1 * ResourceCost.TICKS_PER_SECOND; // max time required to cast a spell
    private boolean isCasting = false;
    private BlockPos castTarget = null;

    public CastFangsLineGoal(PathfinderMob mob) {
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
        if (this.targetEntity != null)
            this.setMoveTarget(this.targetEntity.getOnPos());

        if (moveTarget != null || castTarget != null) {

            if (isCasting) {
                if (castTarget == null) {
                    this.castTarget = moveTarget;
                    this.stopMoving();
                    this.setTarget((LivingEntity) null);
                }
                else {
                    this.mob.getLookControl().setLookAt(castTarget.getX(), castTarget.getY(), castTarget.getZ());
                    ticksCasting += 1;
                    if (ticksCasting >= TICKS_CASTING_MAX) {
                        // cast at move target
                        ((EvokerUnit) this.mob).createEvokerFangsLine(castTarget);

                        if (this.ability != null && !this.mob.level.isClientSide())
                            AbilityClientboundPacket.sendSetCooldownPacket(this.mob.getId(), this.ability.action, this.ability.cooldownMax);
                        this.stop();
                    }
                }
            }
            else if (moveTarget != null && MyMath.distance(
                    this.mob.getX(), this.mob.getZ(),
                    moveTarget.getX(), moveTarget.getZ()) <= EvokerUnit.getFangsRange()) {
                if (moveTarget != null)
                    startCasting();
            }
        }
    }

    public void startCasting() {
        this.isCasting = true;
        if (!this.mob.level.isClientSide())
            UnitSyncClientboundPacket.sendSyncEvokerCastingPacket(this.mob, true);
    }

    @Override
    public void stop() {
        this.ticksCasting = 0;
        this.isCasting = false;
        this.castTarget = null;
        this.stopMoving();
        this.setTarget((LivingEntity) null);
        if (!this.mob.level.isClientSide())
            UnitSyncClientboundPacket.sendSyncEvokerCastingPacket(this.mob, false);
    }
}
