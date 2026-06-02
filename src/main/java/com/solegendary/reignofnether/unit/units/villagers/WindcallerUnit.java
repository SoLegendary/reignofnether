package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.ToggleFlying;
import com.solegendary.reignofnether.building.RangeIndicator;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.entities.WindcallerProjectile;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientEvents;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.controls.FlyingUnitMoveControl;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.modelling.animations.WindcallerAnimations;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import com.solegendary.reignofnether.util.MiscUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WindcallerUnit extends Pillager implements Unit, AttackerUnit, RangedAttackerUnit, KeyframeAnimated, RangeIndicator {
    public static final Abilities ABILITIES = new Abilities();
    static {
        ABILITIES.add(new ToggleFlying(), Keybindings.abilitySlot1);
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

    public Faction getFaction() {return Faction.VILLAGERS;}
    public Abilities getAbilities() {return abilities;};
    public List<ItemStack> getItems() {return items;};
    public MoveToTargetBlockGoal getMoveGoal() {
        return isFlying() ? flyingMoveGoal : moveGoal;
    }
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public Goal getAttackBuildingGoal() {return null;}
    public Goal getAttackGoal() {return attackGoal;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}
    public MountGoal getMountGoal() {return mountGoal;}

    private EnemySearchBehaviour attackSearchBehaviour = EnemySearchBehaviour.NONE;
    public EnemySearchBehaviour getEnemySearchBehaviour() { return attackSearchBehaviour; }
    public void setEnemySearchBehaviour(EnemySearchBehaviour behaviour) { attackSearchBehaviour = behaviour; }

    private final MoveControl groundMoveControl;
    private final MoveControl flyingMoveControl;
    private MoveToTargetBlockGoal moveGoal;
    private FlyingMoveToTargetGoal flyingMoveGoal;
    private SelectedTargetGoal<? extends LivingEntity> targetGoal;
    private ReturnResourcesGoal returnResourcesGoal;
    public MountGoal mountGoal;

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
            SynchedEntityData.defineId(WindcallerUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(WindcallerUnit.class, EntityDataSerializers.INT);

    public boolean isFlying() { return this.entityData.get(isFlyingAccessor); }
    public void setFlying(boolean value) { this.entityData.set(isFlyingAccessor, value); }
    public static final EntityDataAccessor<Boolean> isFlyingAccessor =
            SynchedEntityData.defineId(WindcallerUnit.class, EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
        this.entityData.define(scenarioRoleDataAccessor, -1);
        this.entityData.define(isFlyingAccessor, false);
    }

    // combat stats
    public boolean getWillRetaliate() {return willRetaliate;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle && !isVehicle();}

    @Nullable
    public ResourceCost getCost() {return ResourceCosts.WINDCALLER;}
    public boolean canAttackBuildings() {return getAttackBuildingGoal() != null;}

    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    final static public int LEVITATE_TICKS = 80;

    final static public float attackDamage = 5.0f;
    final static public float attacksPerSecondFlying = 0.25f;
    final static public float attacksPerSecond = 0.30f;
    final static public float maxHealth = 40.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.25f;
    final static public float movementSpeedFlying = 0.30f;
    final static public float attackRange = 12.0F;
    final static public float attackRangeFlying = 14.0F;
    final static public float aggroRange = 12;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;
    public int maxResources = 100;

    public int fogRevealDuration = 0; // set > 0 for the client who is attacked by this unit
    public int getFogRevealDuration() { return fogRevealDuration; }
    public void setFogRevealDuration(int duration) { fogRevealDuration = duration; }

    private UnitRangedAttackGoal<? extends LivingEntity> attackGoal;

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

    // animation attack peak starts at 44% the way through, but we need to set it to 22% for some reason?
    final static private int ATTACK_WINDUP_TICKS = 6; // (int) (NecromancerAnimations.ATTACK.lengthInSeconds() * 20f * 0.22f);

    // when the windcaller is landing, issuing a move command to preserve its movement goal won't work so store it here until landing
    private BlockPos pendingGroundMoveTarget = null;

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
                activeAnimDef = WindcallerAnimations.ATTACK;
                activeAnimState = attackAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CHARGE_SPELL -> {
                activeAnimDef = WindcallerAnimations.LIFT;
                activeAnimState = spellChargeAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CAST_SPELL -> {
                activeAnimDef = WindcallerAnimations.SPELL;
                activeAnimState = spellChargeAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            default -> animateScaleReducing = true;
        }
    }

    public WindcallerUnit(EntityType<? extends Pillager> entityType, Level level) {
        super(entityType, level);
        this.groundMoveControl = this.moveControl;
        this.flyingMoveControl = new FlyingUnitMoveControl(this);
        updateAbilityButtons();
    }

    public void toggleFlying() {
        setFlying(!isFlying());
        updateFlyingStates(true);
    }

    public void updateFlyingStates(boolean doAnimationAndSound) {
        if (isFlying()) {
            this.navigation = new FlyingPathNavigation(this, level());
            BlockPos moveTarget = this.moveGoal.getMoveTarget();
            this.moveGoal.stopMoving();
            this.flyingMoveGoal.stopMoving();
            this.moveControl = flyingMoveControl;
            if (moveTarget != null)
                this.getMoveGoal().setMoveTarget(MiscUtil.getHighestGroundBlock(level(), moveTarget).above());
            else
                this.getMoveGoal().setMoveTarget(blockPosition());
            if (onGround())
                this.setDeltaMovement(0,1,0);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(movementSpeedFlying);
            this.getAttribute(AttributeRegistrar.ATTACKS_PER_SECOND.get()).setBaseValue(attacksPerSecondFlying);
            this.getAttribute(AttributeRegistrar.ATTACK_RANGE.get()).setBaseValue(attackRangeFlying);

            if (!level().isClientSide() && doAnimationAndSound) {
                UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.CHARGE_SPELL, this);
                SoundClientboundPacket.playSoundAtPos(SoundAction.WINDCALLER_LIFT, blockPosition());
            }
        } else {
            this.navigation = new GroundPathNavigation(this, level());
            BlockPos moveTarget = this.flyingMoveGoal.getMoveTarget();
            this.moveGoal.stopMoving();
            this.flyingMoveGoal.stopMoving();
            this.moveControl = groundMoveControl;
            if (moveTarget != null)
                pendingGroundMoveTarget = MiscUtil.getHighestGroundBlock(level(), moveTarget);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(movementSpeed);
            this.getAttribute(AttributeRegistrar.ATTACKS_PER_SECOND.get()).setBaseValue(attacksPerSecond);
            this.getAttribute(AttributeRegistrar.ATTACK_RANGE.get()).setBaseValue(attackRange);

            if (!level().isClientSide() && doAnimationAndSound)
                UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.STOP, this);
        }
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) { }

    @Override
    public void travel(Vec3 pTravelVector) {
        if (!isFlying()) {
            super.travel(pTravelVector);
        }
        else if (this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.02F, pTravelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.800000011920929));
            } else if (this.isInLava()) {
                this.moveRelative(0.02F, pTravelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            } else {
                BlockPos ground = this.getBlockPosBelowThatAffectsMyMovement();
                float f = 0.91F;
                if (this.onGround()) {
                    f = this.level().getBlockState(ground).getFriction(this.level(), ground, this) * 0.91F;
                }

                float f1 = 0.16277137F / (f * f * f);
                f = 0.91F;
                if (this.onGround()) {
                    f = this.level().getBlockState(ground).getFriction(this.level(), ground, this) * 0.91F;
                }

                this.moveRelative(this.onGround() ? 0.1F * f1 : 0.02F, pTravelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale((double)f));
            }
        }
        this.calculateEntityAnimation(false);
    }

    public boolean onClimbable() {
        if (isFlying())
            return false;
        else
            return super.onClimbable();
    }

    // prevent vanilla logic for picking up items
    @Override
    protected void pickUpItem(ItemEntity pItemEntity) { }

    @Override
    public void resetBehaviours() {
        animateScaleReducing = true;
        pendingGroundMoveTarget = null;
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, WindcallerUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, WindcallerUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, WindcallerUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, WindcallerUnit.armorValue)
                .add(AttributeRegistrar.BASE_MAX_HEALTH.get(), WindcallerUnit.maxHealth)
                .add(AttributeRegistrar.ATTACK_DAMAGE.get(), attackDamage)
                .add(AttributeRegistrar.ATTACKS_PER_SECOND.get(), attacksPerSecond)
                .add(AttributeRegistrar.ATTACK_RANGE.get(), attackRange)
                .add(AttributeRegistrar.AGGRO_RANGE.get(), aggroRange)
                .add(AttributeRegistrar.RANGED_DAMAGE_RESIST.get(), 0);
    }

    public void tick() {
        this.setCanPickUpLoot(true);
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);

        if (level().isClientSide() && animateTicks > 0) {
            animateTicks -= 1;
        }
        if (level().isClientSide() && HudClientEvents.hudSelectedEntity == this) {
            if (!lastOnPos.equals(getOnPos())) {
                updateHighlightBps();
            }
            lastOnPos = getOnPos();
        }
        updateRotation();

        if (level().isClientSide() && isFlying()) {
            MiscUtil.spawnFlyingCloudParticles(this);
        }

        // Apply deferred ground target once the windcaller has actually landed
        if (!isFlying() && onGround() && pendingGroundMoveTarget != null) {
            moveGoal.setMoveTarget(pendingGroundMoveTarget.above());
            pendingGroundMoveTarget = null;
        }
    }


    private Set<BlockPos> highlightBps = new HashSet<>();
    private BlockPos lastOnPos = new BlockPos(0,0,0);

    @Override public Set<BlockPos> getHighlightBps() { return highlightBps; }
    @Override public void setHighlightBps(Set<BlockPos> bps) { highlightBps = bps; }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        this.addUnitSaveData(pCompound);
        pCompound.putBoolean("isFlying", isFlying());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.readUnitSaveData(pCompound);
        if (pCompound.contains("isFlying")) {
            setFlying(pCompound.getBoolean("isFlying"));
            updateFlyingStates(false);
        }
    }

    @Override protected SoundEvent getAmbientSound() {
        return SoundRegistrar.WINDCALLER_AMBIENT.get();
    }
    @Override protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundRegistrar.WINDCALLER_HURT.get();
    }
    @Override protected SoundEvent getDeathSound() {
        return SoundRegistrar.WINDCALLER_DEATH.get();
    }

    public void updateRotation() {
        LivingEntity target = this.getTarget();
        Vec3 dMove = this.getDeltaMovement();
        double x = 0;
        double z = 0;

        if (target != null) {
            x = target.getX() - this.getX();
            z = target.getZ() - this.getZ();
        } else if (dMove.distanceTo(new Vec3(0,0,0)) > 0) {
            x = dMove.x();
            z = dMove.z();
        }

        if (Math.abs(x) > 0.05f || Math.abs(z) > 0.05f) {
            this.setYRot(-((float) Mth.atan2(x, z)) * 57.295776F);
            this.yBodyRot = this.getYRot();
        }
    }

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.flyingMoveGoal = new FlyingMoveToTargetGoal(this, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, false);
        this.garrisonGoal = new GarrisonGoal(this);
        this.attackGoal = new UnitRangedAttackGoal<>(this, ATTACK_WINDUP_TICKS);
        this.returnResourcesGoal = new ReturnResourcesGoal(this);
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
        this.goalSelector.addGoal(3, flyingMoveGoal);
    }

    public int getAttackerRangeBonus(Mob attacker) {
        Vec2 attackerPos = new Vec2((float) attacker.getX(), (float) attacker.getZ());
        Vec2 ghastPos = new Vec2((float) this.getX(), (float) this.getZ());
        double horizDist = Math.sqrt(attackerPos.distanceToSqr(ghastPos));
        double vertiDist = Math.max(0, this.getY() - attacker.getY());

        // if we're directly under the ghast, just allow anything to attack it
        if (horizDist < 4)
            return (int) (vertiDist * 0.5f);
        else
            return (int) (vertiDist * 0.25f);
    }

    @Override
    public void setupEquipmentAndUpgradesServer() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));

        if (ResearchServerEvents.playerHasResearch(getOwnerName(), ProductionItems.RESEARCH_UPGRADED_WINDCALLERS) && !isFlying()) {
            setFlying(true);
            updateFlyingStates(false);
        }
    }

    public int getPunchLevel() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        return itemStack.getEnchantmentLevel(Enchantments.PUNCH_ARROWS);
    }

    @Override
    public void performUnitRangedAttack(LivingEntity pTarget, float velocity) {
        if (pTarget == null)
            return;

        // Lead the shot by predicting where the target will be when the projectile arrives
        /*
        Vec3 shooterPos = this.getEyePosition();
        Vec3 targetPos = pTarget.getEyePosition().subtract(0,0.5,0);
        Vec3 targetDelta = pTarget.getDeltaMovement();

        if (pTarget.onGround())
            targetDelta = targetDelta.multiply(1,0,1);

        double distanceTicks = shooterPos.distanceTo(targetPos) * 1.5f;
        Vec3 predictedPos = targetPos.add(targetDelta.multiply(distanceTicks * 1.5f, distanceTicks, distanceTicks * 1.5f));

        double x = predictedPos.x() - shooterPos.x();
        double y = predictedPos.y() - shooterPos.y();
        double z = predictedPos.z() - shooterPos.z();
         */

        double x = pTarget.getX() - this.getX();
        double y = isFlying() ? (pTarget.getY() - this.getEyeY()) : (pTarget.getEyeY() - 0.5 - this.getEyeY());
        double z = pTarget.getZ() - this.getZ();

        WindcallerProjectile proj = new WindcallerProjectile(this.level(), this, x, y, z);
        proj.setPos(this.getEyePosition());

        level().addFreshEntity(proj);

        if (level().isClientSide())
            SoundClientEvents.playSoundAtPos(SoundAction.WINDCALLER_WIND_ATTACK, blockPosition(), 2.0F);

        if (!level().isClientSide() && pTarget instanceof Unit unit)
            FogOfWarClientboundPacket.revealRangedUnit(unit.getOwnerName(), this.getId());
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        return pSpawnData;
    }
}
