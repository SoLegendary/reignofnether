package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.goals.AttackBuildingGoal;
import com.solegendary.reignofnether.unit.goals.MeleeAttackUnitGoal;
import com.solegendary.reignofnether.unit.goals.MoveToTargetBlockGoal;
import com.solegendary.reignofnether.unit.goals.SelectedTargetGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.Ability;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class IronGolemUnit extends IronGolem implements Unit, AttackerUnit {
    // region
    public Faction getFaction() {return Faction.VILLAGERS;}
    public List<AbilityButton> getAbilityButtons() {return abilityButtons;};
    public List<Ability> getAbilities() {return abilities;}
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public AttackBuildingGoal getAttackBuildingGoal() {return attackBuildingGoal;}
    public Goal getAttackGoal() {return attackGoal;}

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
            SynchedEntityData.defineId(IronGolemUnit.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
    }

    // combat stats
    public boolean getWillRetaliate() {return willRetaliate;}
    public int getAttackCooldown() {return (int) (20 / attacksPerSecond);}
    public float getAttacksPerSecond() {return attacksPerSecond;}
    public float getAggroRange() {return aggroRange;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle;}
    public float getAttackRange() {return attackRange;}
    @Override
    public float getAttackDamage() {return attackDamage;}
    public float getMovementSpeed() {return movementSpeed;}
    public float getUnitMaxHealth() {return maxHealth;}
    public float getUnitArmorValue() {return armorValue;}
    public float getSightRange() {return sightRange;}
    public int getPopCost() {return popCost;}
    public boolean canAttackBuildings() {return canAttackBuildings;}

    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    final static public float attackDamage = 9.0f;
    final static public float attacksPerSecond = 0.4f;
    final static public float maxHealth = 50.0f;
    final static public float armorValue = 4.0f;
    final static public float movementSpeed = 0.20f;
    final static public float attackRange = 3; // only used by ranged units or melee building attackers
    final static public float aggroRange = 10;
    final static public float sightRange = 10f;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = false;
    final static public int popCost = ResourceCosts.IronGolem.POPULATION;
    final static public boolean canAttackBuildings = true;

    public MeleeAttackUnitGoal attackGoal;
    public AttackBuildingGoal attackBuildingGoal;

    private static final List<AbilityButton> abilityButtons = new ArrayList<>();
    private final List<Ability> abilities = new ArrayList<>();

    public IronGolemUnit(EntityType<? extends IronGolem> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, IronGolemUnit.movementSpeed)
                .add(Attributes.ATTACK_DAMAGE, IronGolemUnit.attackDamage)
                .add(Attributes.ARMOR, IronGolemUnit.armorValue)
                .add(Attributes.MAX_HEALTH, IronGolemUnit.maxHealth)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void tick() {
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);
    }

    public void initialiseGoals() {
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 1.0f, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.attackGoal = new MeleeAttackUnitGoal(this, getAttackCooldown(), 1.0D, false);
        this.attackBuildingGoal = new AttackBuildingGoal(this, 1.0D);
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, moveGoal);
        this.goalSelector.addGoal(3, attackGoal);
        this.goalSelector.addGoal(3, attackBuildingGoal);
        this.targetSelector.addGoal(3, targetGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }
}
