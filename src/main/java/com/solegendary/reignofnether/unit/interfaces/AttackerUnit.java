package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.building.GarrisonableBuilding;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.goals.FlyingMoveToTargetGoal;
import com.solegendary.reignofnether.unit.goals.MeleeAttackUnitGoal;
import com.solegendary.reignofnether.unit.goals.RangedFlyingAttackBuildingGoal;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

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
            MiscUtil.addUnitCheckpoint(((Unit) this), target.getId());
            ((Unit) this).setIsCheckpointGreen(false);
        }
        ((Unit) this).getTargetGoal().setTarget(target);
    }
    // move to a building and start attacking it
    public default void setAttackBuildingTarget(BlockPos preselectedBlockPos) {
        Goal attackBuildingGoal = this.getAttackBuildingGoal();
        if (attackBuildingGoal instanceof RangedFlyingAttackBuildingGoal<?> rabg)
            rabg.setBuildingTarget(preselectedBlockPos);
        else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg)
            mabg.setBuildingTarget(preselectedBlockPos);
    }

    public static void resetBehaviours(AttackerUnit unit) {
        unit.setUnitAttackTarget(null);
        unit.setAttackMoveTarget(null);

        Goal attackBuildingGoal = unit.getAttackBuildingGoal();
        if (attackBuildingGoal instanceof RangedFlyingAttackBuildingGoal<?> rabg)
            rabg.stop();
        else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg)
            mabg.stopAttacking();
    }

    // this setter sets a Unit field and so can't be defaulted
    // move to a block but chase/attack a target if there is one close by (for a limited distance)
    public void setAttackMoveTarget(@Nullable BlockPos bp);

    public static void tick(AttackerUnit attackerUnit) {
        Mob unitMob = (Mob) attackerUnit;
        Unit unit = (Unit) attackerUnit;

        if (!unitMob.level.isClientSide) {
            if (attackerUnit.getAttackGoal() instanceof MeleeAttackUnitGoal meleeAttackUnitGoal)
                meleeAttackUnitGoal.tickAttackCooldown();

            if (attackerUnit.getAttackBuildingGoal() != null && attackerUnit.canAttackBuildings())
                attackerUnit.getAttackBuildingGoal().tick();

            // enact attack moving - move to target but chase enemies, resuming move once dead or out of range/sight
            if (attackerUnit.getAttackMoveTarget() != null && !unit.hasLivingTarget()) {
                attackerUnit.attackClosestEnemy((ServerLevel) unitMob.level);

                if (unit.getTargetGoal().getTarget() == null && unit.getMoveGoal().getMoveTarget() == null)
                    unit.setMoveTarget(attackerUnit.getAttackMoveTarget());
            }

            boolean isAttackingBuilding = false;
            Goal attackBuildingGoal = attackerUnit.getAttackBuildingGoal();
            if (attackBuildingGoal instanceof RangedFlyingAttackBuildingGoal<?> rabg)
                isAttackingBuilding = rabg.getBuildingTarget() != null;
            else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg)
                isAttackingBuilding = mabg.getBuildingTarget() != null;

            // retaliate against a mob that damaged us UNLESS already on another command
            if (unitMob.getLastDamageSource() != null &&
                    attackerUnit.getWillRetaliate() &&
                    !isAttackingBuilding &&
                    unit.getTargetGoal().getTarget() == null &&
                    (unit.getMoveGoal().getMoveTarget() == null || unit.getHoldPosition()) &&
                    unit.getFollowTarget() == null) {

                Entity lastDSEntity = unitMob.getLastDamageSource().getEntity();

                boolean isMeleeAttackedByFlying = false;
                if (lastDSEntity instanceof Unit unitDS &&
                    unitDS.getMoveGoal() instanceof FlyingMoveToTargetGoal &&
                    attackerUnit.getAttackGoal() instanceof MeleeAttackUnitGoal) {
                    isMeleeAttackedByFlying = true;
                }

                Relationship rs = UnitServerEvents.getUnitToEntityRelationship(unit, lastDSEntity);

                if (!isMeleeAttackedByFlying &&
                    lastDSEntity instanceof LivingEntity &&
                    (rs == Relationship.NEUTRAL || rs == Relationship.HOSTILE)) {
                    attackerUnit.setUnitAttackTarget((LivingEntity) lastDSEntity);
                }
            }
            // enact aggression when idle
            if (attackerUnit.isIdle() && attackerUnit.getAggressiveWhenIdle())
                attackerUnit.attackClosestEnemy((ServerLevel) unitMob.level);
        }
    }

    public default boolean isIdle() {
        Unit unit = (Unit) this;
        return this.getAttackMoveTarget() == null &&
                !unit.hasLivingTarget() &&
                unit.getMoveGoal().getMoveTarget() == null &&
                unit.getFollowTarget() == null;
    }

    // returns true and attacks the closest enemy OR
    // returns false and does nothing if none are found
    public default void attackClosestEnemy(ServerLevel level) {
        float aggroRange = this.getAggroRange();
        GarrisonableBuilding garr = GarrisonableBuilding.getGarrison((Unit) this);
        if (garr != null)
            aggroRange += garr.getAttackRangeBonus();

        Mob closestMob = MiscUtil.findClosestAttackableEnemy((Mob) this, aggroRange, level);
        if (closestMob != null) {
            ((Unit) this).getMoveGoal().stopMoving();
            setUnitAttackTarget(closestMob);
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
