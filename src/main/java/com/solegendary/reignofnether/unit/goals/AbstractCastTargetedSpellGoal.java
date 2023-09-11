package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.function.Consumer;

public abstract class AbstractCastTargetedSpellGoal extends MoveToTargetBlockGoal {

    private LivingEntity targetEntity = null;
    private Ability ability; // used for syncing cooldown with clientside
    protected int ticksCasting = 0; // how long have we spent trying to cast this spell
    public boolean isCasting() { return isCasting; }
    protected final int channelTicks; // max time required to cast a spell
    protected boolean isCasting = false;
    private BlockPos castTarget = null; // pos that the spell will be cast at
    private final int range;
    public Consumer onCast;
    boolean targetsGround;

    public AbstractCastTargetedSpellGoal(PathfinderMob mob, int channelTicks, int range, boolean targetsGround, Consumer onCast) {
        super(mob, false, 1.0f, 0);
        this.channelTicks = channelTicks;
        this.range = range;
        this.targetsGround = targetsGround; // if true, onCast provides a BlockPos, else it provides LivingEntity
        this.onCast = onCast;
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

    protected boolean isInRange() {
        if (moveTarget != null && MyMath.distance(
                this.mob.getX(), this.mob.getZ(),
                moveTarget.getX(), moveTarget.getZ()) <= range)
            return true;
        if (castTarget != null && MyMath.distance(
                this.mob.getX(), this.mob.getZ(),
                castTarget.getX(), castTarget.getZ()) <= range)
            return true;
        return false;
    }

    @Override
    public void tick() {
        // keep following the target
        if (this.targetEntity != null)
            this.setMoveTarget(this.targetEntity.getOnPos());

        if (moveTarget != null || castTarget != null) {

            if (isInRange()) {
                if (moveTarget != null)
                    castTarget = moveTarget;
                if (!isCasting)
                    startCasting();
                this.stopMoving();
            } else {
                this.stopCasting();
                this.setMoveTarget(moveTarget);
            }

            if (isCasting && castTarget != null) {
                this.mob.getLookControl().setLookAt(castTarget.getX(), castTarget.getY(), castTarget.getZ());
                ticksCasting += 1;
                if (ticksCasting >= channelTicks) {
                    if (targetsGround)
                        onCast.accept(castTarget);
                    else
                        onCast.accept(targetEntity);

                    if (this.ability != null && !this.mob.level.isClientSide())
                        AbilityClientboundPacket.sendSetCooldownPacket(this.mob.getId(), this.ability.action, this.ability.cooldownMax);
                    this.stop();
                }
            }
        }
    }

    public void startCasting() {
        this.isCasting = true;
        this.castTarget = moveTarget;
    }
    public void stopCasting() {
        this.isCasting = false;
        this.ticksCasting = 0;
        this.castTarget = null;
    }

    @Override
    public void stop() {
        this.stopMoving();
        this.setTarget((LivingEntity) null);
        this.stopCasting();
    }
}
