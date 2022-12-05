package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ZombieUnit extends Zombie implements Unit, AttackerUnit {
    // region
    public Faction getFaction() {return Faction.MONSTERS;}
    public List<AbilityButton> getAbilities() {return abilities;};
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public AttackBuildingGoal getAttackBuildingGoal() {return attackBuildingGoal;}

    public MoveToTargetBlockGoal moveGoal;
    public SelectedTargetGoal<? extends LivingEntity> targetGoal;

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
            SynchedEntityData.defineId(ZombieUnit.class, EntityDataSerializers.STRING);

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
    public boolean canAttackBuildings() {return canAttackBuildings;}

    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    final static public float attackDamage = 3.0f;
    final static public float maxHealth = 20.0f;
    final static public float armorValue = 2.0f;
    final static public float movementSpeed = 0.25f;
    final static public float attackRange = 0; // only used by ranged units
    final static public int attackCooldown = 30;
    final static public float aggroRange = 10;
    final static public float sightRange = 10f;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = false;
    final static public int popCost = ResourceCosts.Zombie.POPULATION;
    final static public boolean canAttackBuildings = true;

    public ZombieAttackUnitGoal attackUnitGoal;
    public AttackBuildingGoal attackBuildingGoal;

    private static final List<AbilityButton> abilities = new ArrayList<>();

    public ZombieUnit(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, ZombieUnit.movementSpeed)
                .add(Attributes.ATTACK_DAMAGE, ZombieUnit.attackDamage)
                .add(Attributes.ARMOR, ZombieUnit.armorValue)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0); // needs to be added for parent to work
    }

    public void tick() {
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);
    }

    public void resetBehaviours() {
        Unit.resetBehaviours(this);
        AttackerUnit.resetBehaviours(this);
    }

    public void initialiseGoals() {
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 1.0f, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.attackUnitGoal = new ZombieAttackUnitGoal(this, attackCooldown, 1.0D, false);
        this.attackBuildingGoal = new AttackBuildingGoal(this, 1.0D);
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, moveGoal);
        this.goalSelector.addGoal(3, attackUnitGoal);
        this.goalSelector.addGoal(3, attackBuildingGoal);
        this.targetSelector.addGoal(3, targetGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }
}
