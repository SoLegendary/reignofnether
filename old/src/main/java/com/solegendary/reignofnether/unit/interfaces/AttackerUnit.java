package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.units.monsters.PhantomSummon;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public interface AttackerUnit {

    public boolean getWillRetaliate();
    public int getAttackCooldown();
    public float getAttacksPerSecond();
    public float getAggroRange();
    public boolean getAggressiveWhenIdle();
    public float getAttackRange();
    public float getUnitAttackDamage();
    public BlockPos getAttackMoveTarget();
    public boolean canAttackBuildings();

    public Goal getAttackGoal(); // not necessarily the same goal, eg. could be melee or ranged
    public Goal getAttackBuildingGoal();

    // chase and attack the target ignoring all else until it is dead or out of sight
    public default void setUnitAttackTarget(@Nullable LivingEntity target) {
        if (target != null) {
            MiscUtil.addUnitCheckpoint(((Unit) this), target.getId(), false);
        }
        ((Unit) this).getTargetGoal().setTarget(target);
    }

    // chase and attack the target ignoring all else until it is dead or out of sight
    public default void setUnitAttackTargetForced(@Nullable LivingEntity target) {
        setUnitAttackTarget(target);
        if (target != null)
            ((Unit) this).getTargetGoal().forced = true;
    }

    // move to a building and start attacking it
    public default void setAttackBuildingTarget(BlockPos preselectedBlockPos) {
        if (this.canAttackBuildings()) {
            Goal attackBuildingGoal = this.getAttackBuildingGoal();
            if (attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rabg)
                rabg.setBuildingTarget(preselectedBlockPos);
            else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg)
                mabg.setBuildingTarget(preselectedBlockPos);
        } else {
            Level level = ((LivingEntity) this).level();
            BuildingPlacement building = BuildingUtils.findBuilding(level.isClientSide(), preselectedBlockPos);

            if (building != null) {
                BlockPos groundCentrePos = new BlockPos(building.centrePos.getX(), building.originPos.getY() + 1, building.centrePos.getZ());
                BlockPos targetPos = MyMath.getXZRangeLimitedBlockPos(
                        new BlockPos(groundCentrePos),
                        ((LivingEntity) this).getOnPos(),
                        getAttackRange() - 5
                );
                while (!level.getBlockState(targetPos.above()).isAir())
                    targetPos = targetPos.above();

                ((Unit) this).setMoveTarget(targetPos);
                if (((LivingEntity) this).level().isClientSide)
                    MiscUtil.addUnitCheckpoint((Unit) this, groundCentrePos, false);
            }
        }
    }

    public static void resetBehaviours(AttackerUnit unit) {
        unit.setUnitAttackTarget(null);
        unit.setAttackMoveTarget(null);

        Goal attackGoal = unit.getAttackGoal();
        if (attackGoal instanceof MeleeWindupAttackUnitGoal mwaug)
            mwaug.resetWindup();

        Goal attackBuildingGoal = unit.getAttackBuildingGoal();
        if (attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rabg)
            rabg.stop();
        else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg)
            mabg.stopAttacking();
    }

    // this setter sets a Unit field and so can't be defaulted
    // move to a block but chase/attack a target if there is one close by (for a limited distance)
    public void setAttackMoveTarget(@Nullable BlockPos bp);

    static boolean isAttackingBuilding(AttackerUnit attackerUnit) {
        boolean isAttackingBuilding = false;
        Goal attackBuildingGoal = attackerUnit.getAttackBuildingGoal();
        if (attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rabg)
            isAttackingBuilding = rabg.getBuildingTarget() != null;
        else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg)
            isAttackingBuilding = mabg.getBuildingTarget() != null;
        return isAttackingBuilding;
    }

    public static void tick(AttackerUnit attackerUnit) {
        Mob unitMob = (Mob) attackerUnit;
        Unit unit = (Unit) attackerUnit;

        if (!unitMob.level().isClientSide && !unit.isEatingFood()) {
            if (attackerUnit.getAttackGoal() instanceof AbstractMeleeAttackUnitGoal meleeAttackUnitGoal) {
                meleeAttackUnitGoal.tickAttackCooldown();
                // doesn't tick on its own for some reason?
                if (((Mob) attackerUnit).isVehicle())
                    meleeAttackUnitGoal.tick();
                if (meleeAttackUnitGoal instanceof MeleeWindupAttackUnitGoal goal)
                    goal.checkAndPerformAttackWithWindup();
            }
            else if (attackerUnit.getAttackGoal() instanceof UnitRangedAttackGoal rangedAttackGoal)
                rangedAttackGoal.tickAttackCooldown();
            else if (attackerUnit.getAttackGoal() instanceof UnitBowAttackGoal rangedAttackGoal)
                rangedAttackGoal.tickAttackCooldown();

            if (attackerUnit.getAttackBuildingGoal() != null && attackerUnit.canAttackBuildings())
                attackerUnit.getAttackBuildingGoal().tick();
        }
        else if (unit instanceof RangedAttackerUnit rangedAttackerUnit) {
            int revealDuration = rangedAttackerUnit.getFogRevealDuration();
            if (revealDuration > 0)
                rangedAttackerUnit.setFogRevealDuration(revealDuration - 1);
        }

        if (!unitMob.level().isClientSide && unitMob.tickCount % 4 == 0) {
            if (((LivingEntity) unit).getEffect(MobEffectRegistrar.STUN.get()) != null) {
                Unit.fullResetBehaviours(unit);
                return;
            }

            boolean isAttackingBuilding = isAttackingBuilding(attackerUnit);

            // enact attack moving
            // prioritises units and will chase them, resuming attack move once dead or out of range/sight
            if (attackerUnit.getAttackMoveTarget() != null && !unit.hasLivingTarget() && !isAttackingBuilding) {
                attackerUnit.attackClosestEnemy((ServerLevel) unitMob.level());

                if (unit.getTargetGoal().getTarget() == null &&
                    unit.getMoveGoal().getMoveTarget() == null &&
                    !isAttackingBuilding(attackerUnit))
                    unit.setMoveTarget(attackerUnit.getAttackMoveTarget());
            }

            boolean isCasting = unit.isCasting();

            // retaliate against a mob that damaged us UNLESS already on another command
            if (unitMob.getLastDamageSource() != null &&
                    attackerUnit.getWillRetaliate() &&
                    !isAttackingBuilding &&
                    unit.getTargetGoal().getTarget() == null &&
                    (unit.getMoveGoal().getMoveTarget() == null || unit.getHoldPosition()) &&
                    unit.getFollowTarget() == null &&
                    !isCasting) {

                Entity lastDSEntity = unitMob.getLastDamageSource().getEntity();

                boolean isMeleeAttackedByFlyingOrGarrisoned = false;
                if (lastDSEntity instanceof Unit unitDS &&
                    (unitDS.getMoveGoal() instanceof FlyingMoveToTargetGoal ||
                        GarrisonableBuilding.getGarrison(unitDS) != null ||
                        unitDS instanceof PhantomSummon ||
                        unitDS instanceof Vex) &&
                    attackerUnit.getAttackGoal() instanceof AbstractMeleeAttackUnitGoal) {
                    isMeleeAttackedByFlyingOrGarrisoned = true;
                }
                Relationship rs = UnitServerEvents.getUnitToEntityRelationship(unit, lastDSEntity);

                if (!isMeleeAttackedByFlyingOrGarrisoned &&
                    lastDSEntity instanceof LivingEntity &&
                    !(lastDSEntity instanceof Player player && player.isCreative()) &&
                    (rs == Relationship.NEUTRAL || rs == Relationship.HOSTILE)) {
                    attackerUnit.setUnitAttackTarget((LivingEntity) lastDSEntity);
                }
            }
            // enact aggression when idle
            if (unit.isIdle() && !isCasting && attackerUnit.getAggressiveWhenIdle())
                attackerUnit.attackClosestEnemy((ServerLevel) unitMob.level());

            // if attacking another unit as melee, retarget the closest unit periodically
            // unless targeting a building or targeting a specific unit
            if (!isAttackingBuilding &&
                !((Unit) attackerUnit).getTargetGoal().forced)
                attackerUnit.retargetToClosestUnit((ServerLevel) unitMob.level());
        }
    }

    // if the nearest target is closer than the current target, retarget to the nearest
    public default void retargetToClosestUnit(ServerLevel level) {
        float aggroRange = this.getAggroRange();
        GarrisonableBuilding garr = GarrisonableBuilding.getGarrison((Unit) this);
        if (garr != null)
            aggroRange  = garr.getAttackRange();

        LivingEntity closestTarget = MiscUtil.findClosestAttackableEntity((Mob) this, aggroRange, level);
        LivingEntity currentTarget = ((Mob) this).getTarget();

        if (closestTarget != null && currentTarget != null) {
            double distClosestTarget =  ((Mob) this).distanceToSqr(closestTarget.position());
            double distCurrentTarget =  ((Mob) this).distanceToSqr(currentTarget.position());

            if (distClosestTarget < distCurrentTarget) {
                ((Unit) this).getMoveGoal().stopMoving();
                setUnitAttackTarget(closestTarget);
            }
        }
    }

    public default void attackClosestEnemy(ServerLevel level) {
        float aggroRange = this.getAggroRange();
        GarrisonableBuilding garr = GarrisonableBuilding.getGarrison((Unit) this);
        if (garr != null)
            aggroRange  = garr.getAttackRange();

        LivingEntity entity = MiscUtil.findClosestAttackableEntity((Mob) this, aggroRange, level);
        if (entity != null) {
            ((Unit) this).getMoveGoal().stopMoving();
            setUnitAttackTarget(entity);
            return;
        }
        if (canAttackBuildings() && !(this instanceof RavagerUnit && ((LivingEntity) this).isVehicle()) &&
                (!(((Unit) this).getOwnerName()).isEmpty() || level.getGameRules().getRule(GameRuleRegistrar.NEUTRAL_AGGRO).get()))
        {
            BuildingPlacement closestBuilding = MiscUtil.findClosestAttackableBuilding((Mob) this, aggroRange, level);
            if (closestBuilding != null) {
                ((Unit) this).getMoveGoal().stopMoving();
                setAttackBuildingTarget(closestBuilding.originPos);
            }
        }
    }

    public static double getWeaponDamageModifier(AttackerUnit attackerUnit) {
        ItemStack itemStack = ((LivingEntity) attackerUnit).getItemBySlot(EquipmentSlot.MAINHAND);

        if (!itemStack.isEmpty())
            for(AttributeModifier attr : itemStack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE))
                if (attr.getOperation() == AttributeModifier.Operation.ADDITION)
                    return attr.getAmount();
        return 0;
    }
}
