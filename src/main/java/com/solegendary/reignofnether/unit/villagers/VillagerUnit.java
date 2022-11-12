package com.solegendary.reignofnether.unit.villagers;

import com.solegendary.reignofnether.building.buildings.Graveyard;
import com.solegendary.reignofnether.building.buildings.VillagerHouse;
import com.solegendary.reignofnether.building.buildings.VillagerTower;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.Unit;
import com.solegendary.reignofnether.unit.goals.BuildRepairGoal;
import com.solegendary.reignofnether.unit.goals.GatherResourcesGoal;
import com.solegendary.reignofnether.unit.goals.SelectedTargetGoal;
import com.solegendary.reignofnether.unit.goals.MoveToTargetBlockGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class VillagerUnit extends Vindicator implements Unit {
    // region
    public List<AbilityButton> getAbilities() {return abilities;};

    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public BuildRepairGoal getBuildRepairGoal() {return buildRepairGoal;}
    public GatherResourcesGoal getGatherResourceGoal() {return gatherResourcesGoal;}

    public MoveToTargetBlockGoal moveGoal;
    public SelectedTargetGoal<? extends LivingEntity> targetGoal;
    public BuildRepairGoal buildRepairGoal;
    public GatherResourcesGoal gatherResourcesGoal;

    public BlockPos getAttackMoveTarget() { return attackMoveTarget; }
    public LivingEntity getFollowTarget() { return followTarget; }
    public boolean getHoldPosition() { return holdPosition; }
    public void setHoldPosition(boolean holdPosition) { this.holdPosition = holdPosition; }

    // if true causes moveGoal and attackGoal to work together to allow attack moving
    // moves to a block but will chase/attack nearby monsters in range up to a certain distance away
    private BlockPos attackMoveTarget = null;
    private LivingEntity followTarget = null; // if nonnull, continuously moves to the target
    private boolean holdPosition = false;

    // which player owns this unit? this format ensures its synched to client without having to use packets
    public String getOwnerName() { return this.entityData.get(ownerDataAccessor); }
    public void setOwnerName(String name) { this.entityData.set(ownerDataAccessor, name); }
    public static final EntityDataAccessor<String> ownerDataAccessor =
            SynchedEntityData.defineId(VillagerUnit.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
    }

    // combat stats
    public boolean getWillRetaliate() {return willRetaliate;}
    public int getAttackCooldown() {return attackCooldown;}
    public float getAggroRange() {return aggroRange;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle;}
    public float getAttackRange() {return attackRange;}
    public float getMovementSpeed() {return movementSpeed;}
    public float getAttackDamage() {return attackDamage;}
    public float getUnitMaxHealth() {return maxHealth;}
    public float getUnitArmorValue() {return armorValue;}
    public float getSightRange() {return sightRange;}
    public int getPopCost() {return popCost;}
    public boolean isWorker() {return canBuildAndRepair;}
    public boolean canAttack() {return canAttack;}

    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    final static public float attackDamage = 0.0f;
    final static public float maxHealth = 10.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.25f;
    final static public float attackRange = 0; // only used by ranged units
    final static public int attackCooldown = 20;
    final static public float aggroRange = 10;
    final static public float sightRange = 10f;
    final static public boolean willRetaliate = false; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = false;
    final static public int popCost = ResourceCosts.Villager.POPULATION;
    final static public boolean canBuildAndRepair = true;
    final static public boolean canAttack = false;

    private final List<AbilityButton> abilities = new ArrayList<>();

    public VillagerUnit(EntityType<? extends Vindicator> entityType, Level level) {
        super(entityType, level);
        if (level.isClientSide()) {
            this.abilities.add(VillagerTower.getBuildButton());
            this.abilities.add(VillagerHouse.getBuildButton());
            this.abilities.add(Graveyard.getBuildButton());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, VillagerUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, VillagerUnit.maxHealth)
                .add(Attributes.ATTACK_DAMAGE, VillagerUnit.movementSpeed)
                .add(Attributes.ARMOR, VillagerUnit.armorValue);
    }

    // TODO: use player arm poses and animations
    public enum ArmPose {
        CROSSED,
        ATTACKING,
        SPELLCASTING,
        BOW_AND_ARROW,
        CROSSBOW_HOLD,
        CROSSBOW_CHARGE,
        CELEBRATING,
        NEUTRAL;
    }

    public ArmPose getVillagerUnitArmPose() {
        if (this.buildRepairGoal != null && this.buildRepairGoal.isBuilding())
            return ArmPose.ATTACKING;
        else if (this.gatherResourcesGoal != null && this.gatherResourcesGoal.isGathering())
            return ArmPose.CELEBRATING;
        return ArmPose.CROSSED;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource p_34103_) {
        return SoundEvents.VILLAGER_HURT;
    }


    public void tick() {
        super.tick();
        Unit.tick(this);

        // TODO: run Player place block animations with arms shown when building
    }

    public void initialiseGoals() {
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 1.0f, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.buildRepairGoal = new BuildRepairGoal(this, 1.0f);
        this.gatherResourcesGoal = new GatherResourcesGoal(this, 1.0f);
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, moveGoal);
        this.goalSelector.addGoal(3, buildRepairGoal);
        this.goalSelector.addGoal(3, gatherResourcesGoal);
        this.targetSelector.addGoal(3, targetGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }
}
