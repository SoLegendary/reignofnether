package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.monsters.Mausoleum;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.Ability;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.Faction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ZombieUnit extends Zombie implements Unit, AttackerUnit {
    // region
    public Faction getFaction() {return Faction.MONSTERS;}
    public List<AbilityButton> getAbilityButtons() {return abilityButtons;};
    public List<Ability> getAbilities() {return abilities;};
    public List<ItemStack> getItems() {return items;};
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public AttackBuildingGoal getAttackBuildingGoal() {return attackBuildingGoal;}
    public Goal getAttackGoal() {return attackGoal;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}

    public MoveToTargetBlockGoal moveGoal;
    public SelectedTargetGoal<? extends LivingEntity> targetGoal;
    public ReturnResourcesGoal returnResourcesGoal;

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
    public int getAttackCooldown() {return (int) (20 / attacksPerSecond);}
    public float getAttacksPerSecond() {return attacksPerSecond;}
    public float getAggroRange() {return aggroRange;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle;}
    public float getAttackRange() {return attackRange;}
    public float getMovementSpeed() {return movementSpeed;}
    public float getUnitAttackDamage() {return attackDamage;}
    public float getUnitMaxHealth() {return maxHealth;}
    public float getUnitArmorValue() {return armorValue;}
    public float getSightRange() {return sightRange;}
    public int getPopCost() {return popCost;}
    public boolean canAttackBuildings() {return canAttackBuildings;}

    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    final static public float attackDamage = 3.0f;
    final static public float attacksPerSecond = 0.6f;
    final static public float maxHealth = 20.0f;
    final static public float armorValue = 2.0f;
    final static public float movementSpeed = 0.27f;
    final static public float attackRange = 2; // only used by ranged units or melee building attackers
    final static public float aggroRange = 10;
    final static public float sightRange = 10f;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;
    final static public int popCost = ResourceCosts.Zombie.POPULATION;
    final static public boolean canAttackBuildings = true;
    public int maxResources = 100;

    public MeleeAttackUnitGoal attackGoal;
    public AttackBuildingGoal attackBuildingGoal;

    private final List<AbilityButton> abilityButtons = new ArrayList<>();
    private final List<Ability> abilities = new ArrayList<>();
    private final List<ItemStack> items = new ArrayList<>();

    public ZombieUnit(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, ZombieUnit.movementSpeed)
                .add(Attributes.ATTACK_DAMAGE, ZombieUnit.attackDamage)
                .add(Attributes.ARMOR, ZombieUnit.armorValue)
                .add(Attributes.MAX_HEALTH, ZombieUnit.maxHealth)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0); // needs to be added for parent to work
    }

    private final ObjectArrayList<AABB> renderChunksInFrustum = new ObjectArrayList<>(10000);

    public void tick() {
        this.setCanPickUpLoot(true);

        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);

        Predicate<Building> condition = (b) -> {
            BlockPos bp = BuildingUtils.getCentrePos(b.getBlocks());
            Vec3 buildingPos = new Vec3(bp.getX(), bp.getY(), bp.getZ());
            return b.name.equals(Mausoleum.buildingName) &&
                this.position().distanceToSqr(buildingPos) < Math.pow(Mausoleum.sunScreenDist, 2);
        };
        if (!this.level.isClientSide() && this.isOnFire()) {
            Building mausoleumInRange = BuildingUtils.findClosestBuilding(BuildingServerEvents.getBuildings(), this.position(), condition);
            if (mausoleumInRange != null)
                this.setRemainingFireTicks(0);
        }
    }

    public void initialiseGoals() {
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 1.0f, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.attackGoal = new MeleeAttackUnitGoal(this, getAttackCooldown(), 1.0D, false);
        this.attackBuildingGoal = new AttackBuildingGoal(this, 1.0D);
        this.returnResourcesGoal = new ReturnResourcesGoal(this, 1.0f);
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();

        // movegoal must be lower priority than attacks so that attack-moving works correctly
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, attackGoal);
        this.goalSelector.addGoal(2, attackBuildingGoal);
        this.goalSelector.addGoal(2, returnResourcesGoal);
        this.targetSelector.addGoal(2, targetGoal);
        this.goalSelector.addGoal(3, moveGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }
}
