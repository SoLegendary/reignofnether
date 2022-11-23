package com.solegendary.reignofnether.unit.units;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.Unit;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.villagers.VillagerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CreeperUnit extends Creeper implements Unit {
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
    public CreeperAttackUnitGoal attackGoal;

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
            SynchedEntityData.defineId(CreeperUnit.class, EntityDataSerializers.STRING);

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

    final static public float attackDamage = 20.0f;
    final static public float maxHealth = 20.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.25f;
    final static public float attackRange = 0; // only used by ranged units
    final static public int attackCooldown = 100; // not used by creepers anyway
    final static public float aggroRange = 10;
    final static public float sightRange = 10f;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = false;
    final static public int popCost = ResourceCosts.Creeper.POPULATION;
    final static public boolean canBuildAndRepair = false;
    final static public boolean canAttack = true;

    private final List<AbilityButton> abilities = new ArrayList<>();

    private boolean forceExplode = false;

    public CreeperUnit(EntityType<? extends Creeper> entityType, Level level) {
        super(entityType, level);
        if (level.isClientSide())
            this.abilities.add(new AbilityButton(
                "Explode",
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/tnt.png"),
                Keybinding.keyQ,
                () -> false,//CursorClientEvents.getLeftClickAction() == UnitAction.EXPLODE,
                () -> false,
                () -> true,
                () -> UnitClientEvents.sendUnitCommand(UnitAction.EXPLODE),//CursorClientEvents.setLeftClickAction(UnitAction.EXPLODE),
                List.of(
                    FormattedCharSequence.forward("Explode", Style.EMPTY)
                ),
                0, 0, 3
            ));
    }

    public boolean canExplodeOnTarget() {
        LivingEntity target = this.getTarget();
        if (target != null && target.position().distanceTo(this.position()) <= 2f)
            return true;
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, VillagerUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, VillagerUnit.maxHealth)
                .add(Attributes.ARMOR, VillagerUnit.armorValue);
    }

    public void tick() {
        super.tick();
        Unit.tick(this);
        if (forceExplode)
            this.setSwellDir(1);
        else if (!canExplodeOnTarget())
            this.setSwellDir(-1);
    }

    public void initialiseGoals() {
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 1.0f, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.attackGoal = new CreeperAttackUnitGoal(this, attackCooldown, 1.0f, false);
    }

    @Override
    public void resetBehaviours() {
        this.setAttackMoveTarget(null);
        this.getTargetGoal().setTarget(null);
        this.getMoveGoal().stopMoving();
        this.setFollowTarget(null);
        this.setHoldPosition(false);
        forceExplode = false;
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, attackGoal);
        this.goalSelector.addGoal(3, moveGoal);
        this.targetSelector.addGoal(4, targetGoal);
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    public void explode() {
        forceExplode = true;
    }
}
