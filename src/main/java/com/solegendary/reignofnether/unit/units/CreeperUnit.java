package com.solegendary.reignofnether.unit.units;

import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.unit.PopulationCosts;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.Unit;
import com.solegendary.reignofnether.unit.goals.MoveToCursorBlockGoal;
import com.solegendary.reignofnether.unit.goals.SelectedTargetGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CreeperUnit extends Creeper implements Unit {
    // region
    public List<AbilityButton> getAbilities() {return abilities;};

    public MoveToCursorBlockGoal getMoveGoal() {return moveGoal;}
    public void setMoveGoal(MoveToCursorBlockGoal moveGoal) {this.moveGoal = moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public void setTargetGoal(SelectedTargetGoal<? extends LivingEntity> targetGoal) {this.targetGoal = targetGoal;}

    public MoveToCursorBlockGoal moveGoal;
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
    public float getSpeedModifier() {return speedModifier;}
    public float getDamage() {return damage;}
    public float getSightRange() {return sightRange;}
    public int getPopCost() {return popCost;}

    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    final public float damage = 20.0f;
    final public float speedModifier = 1.0f;
    final public float attackRange = 0; // only used by ranged units
    final public int attackCooldown = 100; // not used by creepers anyway
    final public float aggroRange = 10;
    final public float sightRange = 10f;
    final public boolean willRetaliate = true; // will attack when hurt by an enemy
    final public boolean aggressiveWhenIdle = false;
    final public int popCost = PopulationCosts.CREEPER;

    public MeleeAttackGoal attackGoal;

    private final List<AbilityButton> abilities = new ArrayList<>();

    public CreeperUnit(EntityType<? extends Creeper> entityType, Level level) {
        super(entityType, level);
        if (level.isClientSide())
            this.abilities.add(new AbilityButton(
                "Explode",
                14,
                "textures/icons/blocks/tnt.png",
                Keybinding.keyQ,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.EXPLODE,
                () -> false,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.EXPLODE),
                0, 0, 3
            ));
    }

    public void tick() {
        super.tick();
        Unit.tick(this);
    }

    @Override
    protected void registerGoals() {
        this.moveGoal = new MoveToCursorBlockGoal(this, speedModifier);
        this.targetGoal = new SelectedTargetGoal(this, true, true);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        // TODO: extend this to make it also compatible with the Explode ability
        this.goalSelector.addGoal(2, new SwellGoal(this));
        this.goalSelector.addGoal(3, moveGoal);
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        // TODO: this doesn't cause the creeper to move to the target before attacking
        this.targetSelector.addGoal(4, targetGoal);
    }

    // TODO: specifically ground target explode ability; for targeting a mob just set target for regular attack goal
    public void explode(BlockPos targetPos) {
    }
}
