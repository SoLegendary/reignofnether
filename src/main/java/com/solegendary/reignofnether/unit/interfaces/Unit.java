package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.unit.UnitClientboundPacket;
import com.solegendary.reignofnether.unit.goals.GatherResourcesGoal;
import com.solegendary.reignofnether.unit.goals.MoveToTargetBlockGoal;
import com.solegendary.reignofnether.unit.goals.ReturnResourcesGoal;
import com.solegendary.reignofnether.unit.goals.SelectedTargetGoal;
import com.solegendary.reignofnether.unit.Ability;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

// Defines method bodies for Units
// workaround for trying to have units inherit from both their base vanilla Mob class and a Unit class
// Note that we can't write any default methods if they need to use Unit fields without a getter/setter
// (including getters/setters themselves)

public interface Unit {

    public Faction getFaction();

    public List<AbilityButton> getAbilityButtons();
    public List<Ability> getAbilities();
    public List<ItemStack> getItems();
    public int getMaxResources();

    // note that attackGoal is specific to unit types
    public MoveToTargetBlockGoal getMoveGoal();
    public SelectedTargetGoal<?> getTargetGoal();
    public ReturnResourcesGoal getReturnResourcesGoal();

    public float getMovementSpeed();
    public float getUnitMaxHealth();
    public float getUnitArmorValue();
    public float getSightRange();
    public int getPopCost();

    public LivingEntity getFollowTarget();
    public boolean getHoldPosition();
    public void setHoldPosition(boolean holdPosition);

    public String getOwnerName();
    public void setOwnerName(String name);

    public static void tick(Unit unit) {
        Mob unitMob = (Mob) unit;

        for (Ability ability : unit.getAbilities())
            ability.tickCooldown();

        if (!unitMob.level.isClientSide) {

            int totalRes = Resources.getTotalResourcesFromItems(unit.getItems()).getTotalValue();
            if (unitMob.canPickUpLoot()) {
                for (ItemEntity itementity : unitMob.level.getEntitiesOfClass(ItemEntity.class, unitMob.getBoundingBox().inflate(1,0,1))) {
                    if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && unitMob.isAlive()) {

                        if (!Unit.atMaxResources(unit)) {
                            ItemStack itemstack = itementity.getItem();
                            ResourceSource resBlock = ResourceSources.getFromItem(itemstack.getItem());
                            if (resBlock != null) {
                                unitMob.onItemPickup(itementity);
                                unitMob.take(itementity, itemstack.getCount());
                                itementity.discard();
                                unit.getItems().add(itemstack);
                                UnitClientboundPacket.sendSyncResourcesPacket(unitMob);
                            }
                            if (Unit.atMaxResources(unit) && unit instanceof WorkerUnit workerUnit) {
                                GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
                                if (goal != null && goal.getTargetResourceName() != ResourceName.NONE)
                                    goal.saveAndReturnResources();
                            }
                        }
                    }
                }
            }

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
        }
    }

    public static boolean atMaxResources(Unit unit) {
        return Resources.getTotalResourcesFromItems(unit.getItems()).getTotalValue() >= unit.getMaxResources();
    }

    public default boolean hasLivingTarget() {
        Mob unitMob = (Mob) this;
        return unitMob.getTarget() != null && unitMob.getTarget().isAlive();
    }

    public static void resetBehaviours(Unit unit) {
        unit.getTargetGoal().setTarget(null);
        unit.getMoveGoal().stopMoving();
        if (unit.getReturnResourcesGoal() != null)
            unit.getReturnResourcesGoal().stopReturning();
        unit.setFollowTarget(null);
        unit.setHoldPosition(false);
    }

    // can be overridden in the Unit's class to do additional logic on a reset
    public default void resetBehaviours() { }

    // this setter sets a Unit field and so can't be defaulted
    // move to a block ignoring all else until reaching it
    public default void setMoveTarget(@Nullable BlockPos bp) {
        this.getMoveGoal().setMoveTarget(bp);
    }


    // continuously move to a target until told to do something else
    public void setFollowTarget(@Nullable LivingEntity target);

    public void initialiseGoals();
}
