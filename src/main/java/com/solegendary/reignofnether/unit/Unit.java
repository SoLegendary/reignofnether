package com.solegendary.reignofnether.unit;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.unit.goals.BuildRepairGoal;
import com.solegendary.reignofnether.unit.goals.MoveToCursorBlockGoal;
import com.solegendary.reignofnether.unit.goals.SelectedTargetGoal;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// Defines method bodies for Units
// workaround for trying to have units inherit from both their base vanilla Mob class and a Unit class
// Note that we can't write any default methods if they need to use Unit fields without a getter/setter
// (including getters/setters themselves)

public interface Unit {

    public List<AbilityButton> getAbilities();

    // note that attackGoal is specific to unit types
    public MoveToCursorBlockGoal getMoveGoal();
    public void setMoveGoal(MoveToCursorBlockGoal moveGoal);
    public SelectedTargetGoal<?> getTargetGoal();
    public void setTargetGoal(SelectedTargetGoal<?> targetGoal);
    public BuildRepairGoal getBuildRepairGoal();
    public void setBuildRepairGoal(BuildRepairGoal buildRepairGoal);

    public boolean getWillRetaliate();
    public int getAttackCooldown();
    public float getAggroRange();
    public boolean getAggressiveWhenIdle();
    public float getAttackRange();
    public float getMovementSpeed();
    public float getAttackDamage();
    public float getUnitMaxHealth();
    public float getUnitArmorValue();
    public float getSightRange();
    public int getPopCost();
    public boolean canBuildAndRepair();
    public boolean canAttack();

    public BlockPos getAttackMoveTarget();
    public LivingEntity getFollowTarget();
    public boolean getHoldPosition();
    public void setHoldPosition(boolean holdPosition);

    public String getOwnerName();
    public void setOwnerName(String name);

    public static void tick(Unit unit) {
        Mob unitMob = (Mob) unit;

        if (!unitMob.level.isClientSide) {

            // sync target variables between goals and Mob
            if (unit.getTargetGoal().getTarget() == null || !unit.getTargetGoal().getTarget().isAlive() ||
                    unitMob.getTarget() == null || !unitMob.getTarget().isAlive()) {
                unitMob.setTarget(null);
                unit.getTargetGoal().setTarget(null);
            }

            // no iframes after being damaged so multiple units can attack at once
            unitMob.invulnerableTime = 0;

            // enact target-following, and stop followTarget being reset
            if (unit.getFollowTarget() != null)
                unit.setMoveTarget(unit.getFollowTarget().blockPosition());

            // enact attack moving - move to target but chase enemies, resuming move once dead or out of range/sight
            if (unit.getAttackMoveTarget() != null && !unit.hasLivingTarget()) {
                boolean attacked = unit.attackClosestEnemy((ServerLevel) unitMob.level);

                if (!attacked && unit.getMoveGoal().getMoveTarget() == null)
                    unit.setMoveTarget(unit.getAttackMoveTarget());

                else if (!attacked && !unit.getMoveGoal().canContinueToUse()) // finished attack-moving
                    unit.resetBehaviours();
            }

            // retaliate against a mob that damaged us UNLESS already on a move or follow command
            if (unitMob.getLastDamageSource() != null && unit.getWillRetaliate() &&
                unit.getMoveGoal().getMoveTarget() == null && unit.getFollowTarget() == null) {

                Entity lastDSEntity = unitMob.getLastDamageSource().getEntity();
                Relationship rs = UnitServerEvents.getUnitToEntityRelationship(unit, lastDSEntity);

                if (lastDSEntity instanceof PathfinderMob &&
                        (rs == Relationship.NEUTRAL || rs == Relationship.HOSTILE) &&
                        !unit.hasLivingTarget())
                    unit.setAttackTarget((PathfinderMob) lastDSEntity);
            }
            // enact aggression when idle
            if (unit.isIdle() && unit.getAggressiveWhenIdle())
                unit.attackClosestEnemy((ServerLevel) unitMob.level);
        }
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
            Relationship rs = UnitServerEvents.getUnitToEntityRelationship(this, mob);
            if (rs == Relationship.HOSTILE && mob.getId() != unitMob.getId())
                nearbyHostileMobs.add(mob);
        }
        // find the closest mob
        double closestDist = this.getAttackRange() + 1;
        PathfinderMob closestMob = null;
        for (PathfinderMob mob : nearbyHostileMobs) {
            double dist = unitMob.position().distanceTo(mob.position());
            if (dist < closestDist && dist < this.getAggroRange()) {
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

    public default boolean isIdle() {
        return this.getAttackMoveTarget() == null &&
                !this.hasLivingTarget() &&
                this.getMoveGoal().getMoveTarget() == null &&
                this.getFollowTarget() == null;
    }

    public default boolean hasLivingTarget() {
        Mob unitMob = (Mob) this;
        return unitMob.getTarget() != null && unitMob.getTarget().isAlive();
    }

    public default void resetBehaviours() {
        this.setAttackMoveTarget(null);
        this.getTargetGoal().setTarget(null);
        this.getMoveGoal().setMoveTarget(null);
        this.setFollowTarget(null);
        this.setHoldPosition(false);
    }

    // move to a block ignoring all else until reaching it
    public default void setMoveTarget(@Nullable BlockPos bp) {
        this.getMoveGoal().setMoveTarget(bp);
    }
    // chase and attack the target ignoring all else until it is dead or out of sight
    public default void setAttackTarget(@Nullable LivingEntity target) {
        this.getTargetGoal().setTarget(target);
    }
    // move to the selected building and start building/repairing it
    public default void setBuildRepairTarget(@Nullable Building building) {
        this.getBuildRepairGoal().setTarget(building);
    }

    // these two setters set a Unit field and so can't be defaulted

    // move to a block but chase/attack a target if there is one close by (for a limited distance)
    public void setAttackMoveTarget(@Nullable BlockPos bp);
    // continuously move to a target until told to do something else
    public void setFollowTarget(@Nullable LivingEntity target);

    // do one-off stuff to set up the mob when spawned by buildings
    // eg. equipment isn't provided automatically
    default void onBuildingSpawn() { }
}
