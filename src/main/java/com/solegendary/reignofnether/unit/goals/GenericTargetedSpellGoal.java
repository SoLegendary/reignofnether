package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class GenericTargetedSpellGoal extends MoveToTargetBlockGoal {

    protected LivingEntity targetEntity = null;
    protected Ability ability; // used for syncing cooldown with clientside
    protected int channelTicks = 0; // how long have we spent trying to cast this spell
    public boolean isCasting() { return isCasting; }
    protected final int channelTicksMax; // max time required to cast a spell
    protected boolean isCasting = false;
    protected BlockPos castTarget = null; // pos that the spell will be cast at
    public float range;
    public Consumer<LivingEntity> onEntityCast;
    public Consumer<BlockPos> onGroundCast;
    public Consumer<BuildingPlacement> onBuildingCast;
    public boolean hasKeyframeAnimations = false;
    protected float bonusChannelingRange = 0; // extra range added while channeling
    protected final UnitAnimationAction animationStart;
    private Consumer<BlockPos> onStartChanneling = null;
    public boolean instantLook = false;

    public GenericTargetedSpellGoal(Mob mob, int channelTicksMax, float range,
                                    UnitAnimationAction animationStart,
                                    @Nullable Consumer<LivingEntity> onEntityCast,
                                    @Nullable Consumer<BlockPos> onGroundCast,
                                    @Nullable Consumer<BuildingPlacement> onBuildingCast) {
        super(mob, false, 0);
        this.channelTicksMax = channelTicksMax;
        this.range = range;
        this.animationStart = animationStart;
        if (this.animationStart != null)
            this.hasKeyframeAnimations = true;
        this.onEntityCast = onEntityCast;
        this.onGroundCast = onGroundCast;
        this.onBuildingCast = onBuildingCast;
    }

    public GenericTargetedSpellGoal(Mob mob, int channelTicksMax, float range,
                                    @Nullable Consumer<LivingEntity> onEntityCast,
                                    @Nullable Consumer<BlockPos> onGroundCast,
                                    @Nullable Consumer<BuildingPlacement> onBuildingCast) {
        super(mob, false, 0);
        this.channelTicksMax = channelTicksMax;
        this.range = range;
        this.animationStart = null;
        this.onEntityCast = onEntityCast;
        this.onGroundCast = onGroundCast;
        this.onBuildingCast = onBuildingCast;
    }

    // if we set an entity target, on every tick we will follow that target
    // if we set a BlockPos as the target, remove any entity target
    // the user will only start casting once we reach the target
    public void setTarget(LivingEntity entity) {
        if (entity != this.mob) {
            this.targetEntity = entity;
        }
    }
    public void setTarget(BlockPos bpTarget) {
        this.setMoveTarget(bpTarget);
        this.setTarget((LivingEntity) null);
    }
    public void setTarget(BuildingPlacement building) {
        this.setMoveTarget(building.centrePos);
        this.setTarget((LivingEntity) null);
    }
    public int getChannelTicks() {
        return channelTicks;
    }
    public int getChannelTicksMax() {
        return channelTicksMax;
    }

    public LivingEntity getTargetEntity() {
        return targetEntity;
    }

    public void setAbility(Ability ability) {
        this.ability = ability;
    }

    public void setOnStartChanneling(Consumer<BlockPos> onStartChanneling) {
        this.onStartChanneling = onStartChanneling;
    }

    protected boolean isInRange() {
        float finalRange = range;
        if (isCasting())
            finalRange += bonusChannelingRange;

        if (moveTarget != null && MyMath.distance(
                this.mob.getX(), this.mob.getZ(),
                moveTarget.getX(), moveTarget.getZ()) <= finalRange)
            return true;
        if (castTarget != null && MyMath.distance(
                this.mob.getX(), this.mob.getZ(),
                castTarget.getX(), castTarget.getZ()) <= finalRange)
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
                if (!isCasting) {
                    startCasting();
                }
                this.stopMoving();
            } else {
                this.stopCasting();
                this.setMoveTarget(moveTarget);
            }

            if (isCasting && castTarget != null) {
                if (channelTicks == 1 && onStartChanneling != null) {
                    onStartChanneling.accept(castTarget);
                }
                if (!instantLook) {
                    this.mob.getLookControl().setLookAt(castTarget.getX(), castTarget.getY(), castTarget.getZ());
                } else {
                    double dx = castTarget.getX() + 0.5 - mob.getX();
                    double dy = castTarget.getY() + 0.5 - mob.getEyeY();
                    double dz = castTarget.getZ() + 0.5 - mob.getZ();

                    double horizontalDist = Math.sqrt(dx * dx + dz * dz);

                    float yaw = (float)(Math.atan2(dz, dx) * (180F / Math.PI)) - 90F;
                    float pitch = (float)-(Math.atan2(dy, horizontalDist) * (180F / Math.PI));

                    mob.setYRot(yaw);
                    mob.setXRot(pitch);

                    // Sync previous rotation so it doesn't lerp back
                    mob.yRotO = yaw;
                    mob.xRotO = pitch;
                    mob.yBodyRot = yaw;
                    mob.yHeadRot = yaw;
                    mob.yBodyRotO = yaw;
                    mob.yHeadRotO = yaw;
                }
                channelTicks += 1;
                if (channelTicks >= channelTicksMax) {
                    if (onEntityCast != null && targetEntity != null)
                        onEntityCast.accept(targetEntity);
                    else if (onGroundCast != null || onBuildingCast != null) {
                        BuildingPlacement targetBuilding = BuildingUtils.findBuilding(mob.level().isClientSide(), castTarget);
                        if (onBuildingCast != null && targetBuilding != null) {
                            onBuildingCast.accept(targetBuilding);
                        }
                        else if (onGroundCast != null)
                            onGroundCast.accept(castTarget);
                    }
                    if (this.ability != null && !this.mob.level().isClientSide()) {
                        if (!this.mob.level().isClientSide() && mob instanceof Unit unit) {
                            if (this.ability.isOffCooldown(unit)) {
                                AbilityClientboundPacket.sendSetCooldownPacket(this.mob.getId(), this.ability.action, this.ability.cooldownMax);
                            }
                            if (mob instanceof HeroUnit heroUnit && this.ability instanceof HeroAbility heroAbility) {
                                heroUnit.setMana(heroUnit.getMana() - heroAbility.manaCost);
                            }
                        }
                        else if (mob instanceof Unit unit && this.ability.isOffCooldown(unit)) {
                            this.ability.setToMaxCooldown(unit);
                        }
                    }
                    this.stopExceptAnimations();
                }
            }
        }
    }

    public BlockPos getCastTarget() {
        return castTarget;
    }

    public void startCasting() {
        this.isCasting = true;
        this.castTarget = moveTarget;
        if (!this.mob.level().isClientSide()) {
            if (!hasKeyframeAnimations) {
                UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.NON_KEYFRAME_START, this.mob);
            } else if (animationStart != null) {
                UnitAnimationClientboundPacket.sendBasicPacket(animationStart, this.mob);
            }
        }
    }
    public void stopCasting() {
        this.isCasting = false;
        this.channelTicks = 0;
        this.castTarget = null;
        if (!this.mob.level().isClientSide() && channelTicks < channelTicksMax) {
            if (!hasKeyframeAnimations) {
                UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.NON_KEYFRAME_STOP, this.mob);
            } else {
                UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.STOP, this.mob);
            }
        }
    }

    @Override
    public void stop() {
        this.stopMoving();
        this.setTarget((LivingEntity) null);
        this.stopCasting();
    }

    public void stopExceptAnimations() {
        this.stopMoving();
        this.setTarget((LivingEntity) null);
        this.isCasting = false;
        this.channelTicks = 0;
        this.castTarget = null;
    }
}
