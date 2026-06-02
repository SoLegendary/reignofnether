package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.Fear;
import com.solegendary.reignofnether.ability.abilities.Possess;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.time.NightUtils;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.modelling.animations.WraithAnimations;
import com.solegendary.reignofnether.util.MiscUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class WraithUnit extends Monster implements Unit, AttackerUnit, KeyframeAnimated {
    public static final Abilities ABILITIES = new Abilities();
    static {
        ABILITIES.add(new Fear(), Keybindings.abilitySlot1);
        ABILITIES.add(new Possess(), Keybindings.abilitySlot2);
    }

    //region
    @Override
    public void updateAbilityButtons() {
        abilities = ABILITIES.clone();
    }
    Object2ObjectArrayMap<Ability, Float> cooldowns = Unit.createCooldownMap();
    Object2ObjectArrayMap<Ability, Integer> charges = new Object2ObjectArrayMap<>();
    @Override public Object2ObjectArrayMap<Ability, Float> getCooldowns() { return cooldowns; }
    @Override public boolean hasAutocast(Ability ability) { return autocast == ability; }
    @Override public void setAutocast(Ability autocast) { this.autocast = autocast; }
    @Override public Object2ObjectArrayMap<Ability, Integer> getCharges() { return charges; }

    Ability autocast;

    private int eatingTicksLeft = 0;
    public void setEatingTicksLeft(int amount) { eatingTicksLeft = amount; }
    public int getEatingTicksLeft() { return eatingTicksLeft; }

    private BlockPos anchorPos = new BlockPos(0,0,0);
    public void setAnchor(BlockPos bp) { anchorPos = bp; }
    public BlockPos getAnchor() { return anchorPos; }

    private final ArrayList<Checkpoint> checkpoints = new ArrayList<>();
    public ArrayList<Checkpoint> getCheckpoints() { return checkpoints; };

    GarrisonGoal garrisonGoal;
    public GarrisonGoal getGarrisonGoal() { return garrisonGoal; }
    public boolean canGarrison() { return getGarrisonGoal() != null; }

    UsePortalGoal usePortalGoal;
    public UsePortalGoal getUsePortalGoal() { return usePortalGoal; }
    public boolean canUsePortal() { return getUsePortalGoal() != null; }

    public Faction getFaction() {return Faction.MONSTERS;}
    public Abilities getAbilities() {return abilities;}
    public List<ItemStack> getItems() {return items;};
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}

    private MoveToTargetBlockGoal moveGoal;
    private SelectedTargetGoal<? extends LivingEntity> targetGoal;
    private ReturnResourcesGoal returnResourcesGoal;
    private AbstractMeleeAttackUnitGoal attackGoal;
    private MeleeAttackBuildingGoal attackBuildingGoal;
    private GenericTargetedSpellGoal fearGoal;
    public GenericTargetedSpellGoal getFearGoal() { return fearGoal; }
    private GenericTargetedSpellGoal possessGoal;
    public GenericTargetedSpellGoal getPossessGoal() { return possessGoal; }

    @Nullable
    public Fear getFearAbility() {
        for (Ability ability : this.getAbilities().get())
            if (ability instanceof Fear fear)
                return fear;
        return null;
    }

    @Nullable
    public Possess getPossessAbility() {
        for (Ability ability : this.getAbilities().get())
            if (ability instanceof Possess possess)
                return possess;
        return null;
    }

    public LivingEntity getFollowTarget() { return followTarget; }
    public boolean getHoldPosition() { return holdPosition; }
    public void setHoldPosition(boolean holdPosition) { this.holdPosition = holdPosition; }

    // if true causes moveGoal and attackGoal to work together to allow attack moving
    // moves to a block but will chase/attack nearby monsters in range up to a certain distance away
    private LivingEntity followTarget = null; // if nonnull, continuously moves to the target
    private boolean holdPosition = false;
    private BlockPos attackMoveTarget = null;

    // which player owns this unit? this format ensures its synched to client without having to use packets
    public String getOwnerName() { return this.entityData.get(ownerDataAccessor); }
    public void setOwnerName(String name) { this.entityData.set(ownerDataAccessor, name); }
    public static final EntityDataAccessor<String> ownerDataAccessor =
            SynchedEntityData.defineId(WraithUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(WraithUnit.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
        this.entityData.define(scenarioRoleDataAccessor, -1);
    }

    @Nullable
    public ResourceCost getCost() {return ResourceCosts.WRAITH;}
    public boolean getWillRetaliate() {return willRetaliate;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle && !isVehicle();}
    public BlockPos getAttackMoveTarget() { return attackMoveTarget; }
    public boolean canAttackBuildings() {return getAttackBuildingGoal() != null;}
    public Goal getAttackGoal() { return attackGoal; }
    public Goal getAttackBuildingGoal() { return attackBuildingGoal; }
    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    private EnemySearchBehaviour attackSearchBehaviour = EnemySearchBehaviour.NONE;
    public EnemySearchBehaviour getEnemySearchBehaviour() { return attackSearchBehaviour; }
    public void setEnemySearchBehaviour(EnemySearchBehaviour behaviour) { attackSearchBehaviour = behaviour; }

    // endregion

    final static public float attackDamage = 5.0f;
    final static public float attacksPerSecond = 0.5f;
    final static public float attackRange = 2; // only used by ranged units or melee building attackers
    final static public float aggroRange = 10;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;
    final static public float maxHealth = 50.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.28f;

    public int maxResources = 100;

    private Abilities abilities = ABILITIES.clone();
    private final List<ItemStack> items = new ArrayList<>();

    public final AnimationState idleAnimState = new AnimationState();
    public final AnimationState walkAnimState = new AnimationState();
    public final AnimationState spellChargeAnimState = new AnimationState();
    public final AnimationState spellActivateAnimState = new AnimationState();
    public final AnimationState attackAnimState = new AnimationState();

    private float ageInTicksOffset = 0;
    public float getAgeInTicksOffset() { return ageInTicksOffset; }
    public void setAgeInTicksOffset(float ticks) { ageInTicksOffset = ticks; }

    @Override
    public int getAttackWindupTicks() { return 8; }

    @Override
    public float getAnimationSpeed() { return animateSpeed; }

    // non-looping animations
    public AnimationDefinition activeAnimDef = null;
    public AnimationState activeAnimState = null;

    public void stopAllAnimations() {
        idleAnimState.stop();
        walkAnimState.stop();
        spellChargeAnimState.stop();
        spellActivateAnimState.stop();
        attackAnimState.stop();
    }
    public int animateTicks = 0;
    public float animateScale = 1.0f;
    public float animateSpeed = 1.0f;
    public boolean animateScaleReducing = false;
    public void setAnimateTicksLeft(int ticks) { animateTicks = ticks; }
    public int getAnimateTicksLeft() { return animateTicks; }

    public void playSingleAnimation(UnitAnimationAction animAction) {
        animateScaleReducing = false;
        switch (animAction) {
            case ATTACK_UNIT, ATTACK_BUILDING -> {
                if (getFearAbility() != null && getFearAbility().isAutocasting(this) && getFearAbility().isOffCooldown(this))
                    activeAnimDef = WraithAnimations.FEAR;
                else
                    activeAnimDef = WraithAnimations.ATTACK;
                activeAnimState = attackAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CAST_SPELL -> {
                activeAnimDef = WraithAnimations.FEAR;
                activeAnimState = attackAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CAST_SPELL_ALT -> {
                activeAnimDef = WraithAnimations.POSSESS;
                activeAnimState = attackAnimState;
                animateScale = 1.0f;
                animateSpeed = 0.15f;
                startAnimation(activeAnimDef);
            }
            default -> {
                animateScaleReducing = true;
                animateSpeed = 1.0f;
            }
        }
    }

    public WraithUnit(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        updateAbilityButtons();
        this.autocast = getAbilities().get().get(0); // Fear
    }

    @Override
    public void kill() {
        if (!level().isClientSide()) {
            SoundClientboundPacket.stopSoundWithId(getId());
        }
        super.kill();
    }

    @Override
    protected boolean onSoulSpeedBlock() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, WraithUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, WraithUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, WraithUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, WraithUnit.armorValue)
                .add(Attributes.ATTACK_KNOCKBACK, 0f)
                .add(Attributes.KNOCKBACK_RESISTANCE, 9999.0f)
                .add(AttributeRegistrar.ATTACK_DAMAGE.get(), attackDamage)
                .add(AttributeRegistrar.ATTACKS_PER_SECOND.get(), attacksPerSecond)
                .add(AttributeRegistrar.ATTACK_RANGE.get(), attackRange)
                .add(AttributeRegistrar.AGGRO_RANGE.get(), aggroRange)
                .add(AttributeRegistrar.RANGED_DAMAGE_RESIST.get(), 0)
                .add(AttributeRegistrar.MAGIC_DAMAGE_RESIST.get(), 0);
    }

    @Override
    public boolean isLeftHanded() { return false; }
    @Override // prevent vanilla logic for picking up items
    protected void pickUpItem(ItemEntity pItemEntity) { }
    @Override
    protected void customServerAiStep() { }
    @Override
    public LivingEntity getTarget() {
        return this.targetGoal.getTarget();
    }

    @Override
    public void resetBehaviours() {
        this.getMoveGoal().setMoveTarget(this.getOnPos());
        if (getFearGoal() != null)
            getFearGoal().stop();
        if (getPossessGoal() != null)
            getPossessGoal().stop();
    }

    public void tick() {
        this.setCanPickUpLoot(true);
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);

        if (getFearGoal() != null)
            getFearGoal().tick();
        if (getPossessGoal() != null)
            getPossessGoal().tick();

        if (level().isClientSide() && animateTicks > 0) {
            animateTicks -= 1;
        }
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity pEntity) {
        if (super.doHurtTarget(pEntity) && pEntity instanceof LivingEntity le) {
            Ability ability = getFearAbility();
            if (ability != null && ability.isAutocasting(this))
                ability.use(this.level(), this, le);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        this.addUnitSaveData(pCompound);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.readUnitSaveData(pCompound);
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    protected boolean isSunBurnTick() {
        return NightUtils.isSunBurnTick(this);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (isSunBurnTick())
            this.setSecondsOnFire(8);
    }

    @Override
    public SunlightEffect getSunlightEffect() {
        if (hasItemInSlot(EquipmentSlot.HEAD) && getItemBySlot(EquipmentSlot.HEAD).getItem() != Items.CARVED_PUMPKIN) {
            return SunlightEffect.SLOWNESS_II;
        } else {
            return SunlightEffect.FIRE;
        }
    }
    @Override protected SoundEvent getAmbientSound() {
        return SoundRegistrar.WRAITH_AMBIENT.get();
    }
    @Override protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundRegistrar.WRAITH_HURT.get();
    }
    @Override protected SoundEvent getDeathSound() {
        return SoundRegistrar.WRAITH_DEATH.get();
    }

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.garrisonGoal = new GarrisonGoal(this);
        this.attackGoal = new MeleeWindupAttackUnitGoal(this, false);
        this.attackBuildingGoal = new MeleeWindupAttackBuildingGoal(this);
        this.returnResourcesGoal = new ReturnResourcesGoal(this);
        this.fearGoal = new GenericTargetedSpellGoal(this,
                0,
                Fear.RANGE,
                UnitAnimationAction.CAST_SPELL,
                this::onCastFear,
                null,
                null
        );
        this.possessGoal = new PossessSpellGoal(this,
                Possess.BASE_CHANNEL_TICKS,
                Possess.RANGE,
                UnitAnimationAction.CAST_SPELL_ALT,
                this::onCastPossess
        );
        this.possessGoal.instantLook = true;
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();
        this.goalSelector.addGoal(2, usePortalGoal);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, attackGoal);
        this.goalSelector.addGoal(2, returnResourcesGoal);
        this.goalSelector.addGoal(2, garrisonGoal);
        this.targetSelector.addGoal(2, targetGoal);
        this.goalSelector.addGoal(3, moveGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundUnitGoal(this));
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void doPush(Entity entity) {
        if (entity.getClass() == this.getClass()) {
            super.doPush(entity);
        }
    }
    @Override
    public void push(Entity entity) {
        if (entity.getClass() == this.getClass()) {
            super.push(entity);
        }
    }

    public void onCastFear(LivingEntity targetEntity) {
        if (level().isClientSide())
            return;
        if (!(targetEntity instanceof Unit targetUnit))
            return;
        if (targetUnit.uninterruptable())
            return;

        if (!this.level().isClientSide()) {
            SoundClientboundPacket.playSoundAtPos(SoundAction.WRAITH_FEAR, blockPosition());
        }

        // Calculate a flee position 5 blocks directly away from this wraith
        Vec3 toTarget = targetEntity.position().subtract(this.position()).normalize();
        Vec3 fleePos = targetEntity.position().add(toTarget.scale(Fear.DURATION_SECONDS * 2));
        BlockPos fleeBp = new BlockPos((int) fleePos.x, (int) fleePos.y, (int) fleePos.z);

        Unit.fullResetBehaviours(targetUnit);
        targetUnit.getMoveGoal().setMoveTarget(fleeBp);
        targetEntity.addEffect(new MobEffectInstance(MobEffectRegistrar.UNCONTROLLABLE.get(), Fear.DURATION_SECONDS * 20, 0, true, false));
        targetEntity.addEffect(new MobEffectInstance(MobEffectRegistrar.FEARFUL.get(), Fear.DURATION_SECONDS * 20, 0, true, false));
    }


    public void onCastPossess(LivingEntity targetEntity) {
        MobEffectInstance mei = targetEntity.getEffect(MobEffectRegistrar.PARTIALLY_POSSESSED.get());
        int amp = 0;
        if (mei != null) {
            amp = mei.getAmplifier() + 1;
        }
        // play possess sound
        kill();
        if (targetEntity instanceof Unit unit && unit.getCost().population <= (amp + 1) * Possess.POP_PER_WRAITH) {
            targetEntity.removeEffect(MobEffectRegistrar.PARTIALLY_POSSESSED.get());
            unit.setOwnerName(this.getOwnerName());
            unit.setAnchor(null);
            MiscUtil.addParticleExplosion(ParticleTypes.SCULK_SOUL, 40, level(), targetEntity.getEyePosition(), 0.15f);
            if (!this.level().isClientSide()) {
                Unit.fullResetBehaviours(unit); // stop the unit's actions and stop all allied units from attacking it
                for (LivingEntity entity : UnitServerEvents.getAllUnits())
                    if (entity instanceof Unit unit1 && unit1.getTargetGoal().getTarget() == unit && unit1.getOwnerName().equals(unit.getOwnerName()))
                        Unit.fullResetBehaviours(unit1);
                SoundClientboundPacket.playSoundAtPos(SoundAction.WRAITH_POSSESS_FULL, targetEntity.blockPosition());
            }
        } else {
            targetEntity.addEffect(new MobEffectInstance(
                    MobEffectRegistrar.PARTIALLY_POSSESSED.get(),
                    Possess.PARTIAL_POSSESS_DURATION_SECONDS * 20,
                    amp,
                    false,
                    true
            ));
            MiscUtil.addParticleExplosion(ParticleTypes.SCULK_SOUL, 10, level(), targetEntity.getEyePosition(), 0.10f);
            if (!this.level().isClientSide())
                SoundClientboundPacket.playSoundAtPos(SoundAction.WRAITH_POSSESS_PARTIAL, targetEntity.blockPosition());
        }
    }
}
