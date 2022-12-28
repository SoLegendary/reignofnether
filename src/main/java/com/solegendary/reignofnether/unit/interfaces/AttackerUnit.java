package com.solegendary.reignofnether.unit.interfaces;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.AttackBuildingGoal;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
    public AttackBuildingGoal getAttackBuildingGoal();

    // chase and attack the target ignoring all else until it is dead or out of sight
    public default void setAttackTarget(@Nullable LivingEntity target) {
        ((Unit) this).getTargetGoal().setTarget(target);
    }
    // move to a building and start attacking it
    public default void setAttackBuildingTarget(BlockPos preselectedBlockPos) {
        AttackBuildingGoal attackBuildingGoal = this.getAttackBuildingGoal();
        if (attackBuildingGoal != null)
            attackBuildingGoal.setBuildingTarget(preselectedBlockPos);
    }

    public static void resetBehaviours(AttackerUnit unit) {
        unit.setAttackTarget(null);
        unit.setAttackMoveTarget(null);
        AttackBuildingGoal attackBuildingGoal = unit.getAttackBuildingGoal();
        if (unit.canAttackBuildings() && attackBuildingGoal != null)
            attackBuildingGoal.stopAttacking();
    }

    // this setter sets a Unit field and so can't be defaulted
    // move to a block but chase/attack a target if there is one close by (for a limited distance)
    public void setAttackMoveTarget(@Nullable BlockPos bp);

    public static void tick(AttackerUnit attackerUnit) {
        Mob unitMob = (Mob) attackerUnit;
        Unit unit = (Unit) attackerUnit;

        if (!unitMob.level.isClientSide) {

            if (attackerUnit.getAttackBuildingGoal() != null && attackerUnit.canAttackBuildings())
                attackerUnit.getAttackBuildingGoal().tick();

            // enact attack moving - move to target but chase enemies, resuming move once dead or out of range/sight
            if (attackerUnit.getAttackMoveTarget() != null && !unit.hasLivingTarget()) {
                boolean attacked = attackerUnit.attackClosestEnemy((ServerLevel) unitMob.level);

                if (!attacked && unit.getMoveGoal().getMoveTarget() == null)
                    unit.setMoveTarget(attackerUnit.getAttackMoveTarget());

                else if (!attacked && !unit.getMoveGoal().canContinueToUse()) // finished attack-moving
                    AttackerUnit.resetBehaviours(attackerUnit);
            }

            // retaliate against a mob that damaged us UNLESS already on a move or follow command (unless holding position)
            if (unitMob.getLastDamageSource() != null &&
                    attackerUnit.getWillRetaliate() &&
                    (unit.getMoveGoal().getMoveTarget() == null || unit.getHoldPosition()) &&
                    unit.getFollowTarget() == null) {

                Entity lastDSEntity = unitMob.getLastDamageSource().getEntity();
                Relationship rs = UnitServerEvents.getUnitToEntityRelationship(unit, lastDSEntity);

                if (lastDSEntity instanceof PathfinderMob &&
                    (rs == Relationship.NEUTRAL || rs == Relationship.HOSTILE)) {
                    attackerUnit.setAttackTarget((PathfinderMob) lastDSEntity);
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
    public default boolean attackClosestEnemy(ServerLevel level) {
        Mob unitMob = (Mob) this;

        List<PathfinderMob> nearbyMobs = MiscUtil.getEntitiesWithinRange(
                new Vector3d(unitMob.position().x, unitMob.position().y, unitMob.position().z),
                this.getAggroRange(),
                PathfinderMob.class,
                level);

        List<PathfinderMob> nearbyHostileMobs = new ArrayList<>();

        for (PathfinderMob mob : nearbyMobs) {
            Relationship rs = UnitServerEvents.getUnitToEntityRelationship((Unit) this, mob);
            if (rs == Relationship.HOSTILE && mob.getId() != unitMob.getId())
                nearbyHostileMobs.add(mob);
        }
        // find the closest mob
        double closestDist = getAggroRange();
        PathfinderMob closestMob = null;
        for (PathfinderMob mob : nearbyHostileMobs) {
            double dist = unitMob.position().distanceTo(mob.position());
            if (dist < closestDist) {
                closestDist = unitMob.position().distanceTo(mob.position());
                closestMob = mob;
            }
        }
        if (closestMob != null && unitMob.hasLineOfSight(closestMob)) {
            setAttackTarget(closestMob);
            return true;
        }
        return false;
    }

    default double getWeaponDamageModifier() { return 0; }
}
