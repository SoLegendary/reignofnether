package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.ability.abilities.PromoteIllager;
import com.solegendary.reignofnether.ability.heroAbilities.villager.Avatar;
import com.solegendary.reignofnether.ability.heroAbilities.villager.BattleRagePassive;
import com.solegendary.reignofnether.ability.heroAbilities.villager.MaceSlam;
import com.solegendary.reignofnether.ability.heroAbilities.villager.TauntingCry;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.hero.HeroClientboundPacket;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.modelling.animations.RoyalGuardAnimations;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoyalGuardUnit extends Vindicator implements AttackerUnit, HeroUnit, KeyframeAnimated {
    public static final Abilities ABILITIES = new Abilities();
    static {
        ABILITIES.add(new MaceSlam());
        ABILITIES.add(new TauntingCry());
        ABILITIES.add(new BattleRagePassive());
        ABILITIES.add(new Avatar());
    }

    @Override
    public Object2ObjectArrayMap<HeroAbility, Integer> getHeroAbilityRanks() {
        return heroAbilityRanks;
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
    Object2ObjectArrayMap<HeroAbility, Integer> heroAbilityRanks = new Object2ObjectArrayMap<>();

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
    public Abilities getAbilities() {return abilities;}
    public List<ItemStack> getItems() {return items;};
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public Goal getAttackBuildingGoal() {return attackBuildingGoal;}
    public Goal getAttackGoal() {return attackGoal;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}

    private GenericUntargetedSpellGoal castTauntingCryGoal;
    public GenericUntargetedSpellGoal getCastTauntingCryGoal() { return castTauntingCryGoal; }
    private GenericTargetedSpellGoal castMaceSlamGoal;
    public GenericTargetedSpellGoal getCastMaceSlamGoal() { return castMaceSlamGoal; }
    private GenericUntargetedSpellGoal castAvatarGoal;
    public GenericUntargetedSpellGoal getCastAvatarGoal() { return castAvatarGoal; }

    private MoveToTargetBlockGoal moveGoal;
    private SelectedTargetGoal<? extends LivingEntity> targetGoal;
    private ReturnResourcesGoal returnResourcesGoal;
    private AbstractMeleeAttackUnitGoal attackGoal;
    private MeleeAttackBuildingGoal attackBuildingGoal;

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
            SynchedEntityData.defineId(RoyalGuardUnit.class, EntityDataSerializers.STRING);

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
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle && !isVehicle();}
    public float getAttackRange() {return attackRange;}
    public float getMovementSpeed() {return movementSpeed;}
    public float getUnitAttackDamage() {return attackDamage + (attackBonusPerLevel * getHeroLevel());}
    public float getUnitMaxHealth() {return maxHealth + (maxHealthBonusPerLevel * getHeroLevel());}

    @Nullable
    public ResourceCost getCost() {return ResourceCosts.ROYAL_GUARD;}
    public boolean canAttackBuildings() {return getAttackBuildingGoal() != null;}

    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    private int skillPoints = 1;
    private int experience = 0;
    private boolean rankUpMenuOpen = false;
    @Override public int getSkillPoints() { return skillPoints; }
    @Override public void setSkillPoints(int points) { skillPoints = points; }
    @Override public boolean isRankUpMenuOpen() { return rankUpMenuOpen; }
    @Override public void showRankUpMenu(boolean show) { rankUpMenuOpen = show; }
    @Override public int getExperience() { return experience; }
    @Override public void setExperience(int amount) {
        experience = amount;
        setStatsForLevel();
    }
    private float baseMaxMana = 100;
    private float maxMana = baseMaxMana;
    private float mana = maxMana;
    private float manaRegenPerSecond = 0.5f;
    private float manaBonusPerLevel = 6;
    @Override public float getBaseMaxMana() { return baseMaxMana; }
    @Override public float getMaxMana() { return maxMana; }
    @Override public void setMaxMana(float amount) {
        this.maxMana = amount;
        if (!level().isClientSide())
            HeroClientboundPacket.setMaxMana(getId(), amount);
    }
    @Override public float getMana() { return mana; }
    @Override public void setMana(float amount) {
        this.mana = Math.min(maxMana, amount);
        if (!level().isClientSide())
            HeroClientboundPacket.setMana(getId(), this.mana);
    }
    @Override public float getManaRegenPerSecond() { return manaRegenPerSecond; }
    @Override public float getManaBonusPerLevel() { return manaBonusPerLevel; }

    final static public float attackDamage = 6.0f;
    final static public float attackBonusPerLevel = 0.6f;
    final static public float attacksPerSecond = 0.5f;
    final static public float maxHealth = 125.0f;
    final static public float maxHealthBonusPerLevel = 15.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.28f;
    final static public float attackRange = 2; // only used by ranged units or melee building attackers
    final static public float aggroRange = 10;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;
    public int maxResources = 100;

    @Override public float getHealthBonusPerLevel() { return maxHealthBonusPerLevel; };
    @Override public float getAttackBonusPerLevel() { return attackBonusPerLevel; };
    @Override public float getBaseHealth() { return maxHealth; };
    @Override public float getBaseAttack() { return attackDamage; };

    private Abilities abilities = ABILITIES.clone();
    private final List<ItemStack> items = new ArrayList<>();

    public final AnimationState idleAnimState = new AnimationState();
    public final AnimationState walkAnimState = new AnimationState();
    public final AnimationState spellChargeAnimState = new AnimationState();
    public final AnimationState spellActivateAnimState = new AnimationState();
    public final AnimationState attackAnimState = new AnimationState();

    final static private int ATTACK_WINDUP_TICKS = 12;

    public int tauntingCryTicksLeft = 0;

    public boolean avatarScalingStarted = false;
    public int avatarTicksLeft = 0;
    public int avatarScaleTicks = 0; // at max, will be full sized
    public final int AVATAR_SCALE_TICKS_MAX = 40;
    private final float AVATAR_MAX_BONUS_SCALE = 0.6f;

    private static final double KNOCKBACK_RESISTANCE = 0.5d;

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
    public boolean animateScaleReducing = false;
    public void setAnimateTicksLeft(int ticks) { animateTicks = ticks; }
    public int getAnimateTicksLeft() { return animateTicks; }

    public void playSingleAnimation(UnitAnimationAction animAction) {
        animateScaleReducing = false;
        switch (animAction) {
            case ATTACK_UNIT, ATTACK_BUILDING -> {
                activeAnimDef = RoyalGuardAnimations.ATTACK;
                activeAnimState = attackAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CHARGE_SPELL -> {
                activeAnimDef = RoyalGuardAnimations.SPELL_CHARGE;
                activeAnimState = spellChargeAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CAST_SPELL -> {
                activeAnimDef = RoyalGuardAnimations.SPELL_ACTIVATE;
                activeAnimState = spellActivateAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            default -> animateScaleReducing = true;
        }
    }

    public RoyalGuardUnit(EntityType<? extends Vindicator> entityType, Level level) {
        super(entityType, level);

        updateAbilityButtons();
        setStatsForLevel();
    }

    @Override
    public float getDamageAfterMagicAbsorb(DamageSource pSource, float pDamage) {
        pDamage = super.getDamageAfterMagicAbsorb(pSource, pDamage);
        if (pSource.is(DamageTypeTags.WITCH_RESISTANT_TO))
            pDamage *= 0.7F;
        return pDamage;
    }

    @Override
    public void setStatsForLevel(boolean heal) {
        AttributeInstance aiMaxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        float newHealth = getBaseHealth() + ((getHeroLevel() - 1) * getHealthBonusPerLevel());
        if (avatarTicksLeft > 0)
            newHealth += Avatar.BONUS_HEALTH;
        if (aiMaxHealth != null)
            aiMaxHealth.setBaseValue(newHealth);
        AttributeInstance aiAttackDamage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (aiAttackDamage != null)
            aiAttackDamage.setBaseValue(getBaseAttack() + ((getHeroLevel() - 1) * getAttackBonusPerLevel()));
        this.setMaxMana(getBaseMaxMana() + ((getHeroLevel() - 1) * getManaBonusPerLevel()));
        if (heal)
            this.setHealth(this.getMaxHealth());
        if (getHealth() > getMaxHealth())
            setHealth(getMaxHealth());
    }

    private void updateKnockbackResistance() {
        AttributeInstance ai = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (ai != null) {
            if (avatarTicksLeft > 0 || tauntingCryTicksLeft > 0)
                ai.setBaseValue(KNOCKBACK_RESISTANCE);
            else
                ai.setBaseValue(0.5f);
        }
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (tauntingCryTicksLeft > 0)
            pAmount *= TauntingCry.DAMAGE_MULT;

        boolean result = super.hurt(pSource, pAmount);
        BattleRagePassive battleRage = getBattleRage();
        if (result && battleRage.getRank(this) > 0 &&
            pSource.getEntity() instanceof Unit unit &&
            !List.of(Relationship.OWNED, Relationship.FRIENDLY)
                    .contains(UnitServerEvents.getUnitToEntityRelationship(unit, this))) {
            setMana(mana + pAmount * battleRage.manaPerDmgTaken);
        }
        return result;
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        boolean result = super.doHurtTarget(pEntity);
        if (result && avatarTicksLeft > 0) {

            List<LivingEntity> hitEntities = MiscUtil.getEntitiesWithinRange(pEntity.getEyePosition(), Avatar.ATTACK_SPLASH_RADIUS, LivingEntity.class, level())
                    .stream()
                    .filter(e -> {
                        if (e instanceof Unit unit) {
                            return !List.of(Relationship.OWNED, Relationship.FRIENDLY)
                                    .contains(UnitServerEvents.getUnitToEntityRelationship(unit, this));
                        }
                        return true;
                    })
                    .toList();

            level().explode(null, null, null, pEntity.getX(), pEntity.getY(), pEntity.getZ(),
                    1.0f, false, Level.ExplosionInteraction.NONE);
            AttributeInstance ai = getAttribute(Attributes.ATTACK_DAMAGE);

            if (ai != null) {
                for (LivingEntity hitEntity : hitEntities) {
                    if (hitEntity == pEntity)
                        continue;
                    boolean hurt = hitEntity.hurt(this.damageSources().mobAttack(this), (float) ai.getValue() * Avatar.ATTACK_SPLASH_MULT);
                    if (hurt) {
                        float kb = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
                        hitEntity.knockback(kb * 0.5F, Mth.sin(this.getYRot() * 0.017453292F), -Mth.cos(this.getYRot() * 0.017453292F));
                        this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
                        this.setLastHurtMob(hitEntity);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, RoyalGuardUnit.movementSpeed)
                .add(Attributes.ATTACK_DAMAGE, RoyalGuardUnit.attackDamage)
                .add(Attributes.ARMOR, RoyalGuardUnit.armorValue)
                .add(Attributes.MAX_HEALTH, RoyalGuardUnit.maxHealth)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange());
    }

    public void tick() {
        this.setCanPickUpLoot(true);
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);
        HeroUnit.tick(this);
        PromoteIllager.checkAndApplyBuff(this);

        if (level().isClientSide() && animateTicks > 0) {
            animateTicks -= 1;
        }
        this.castMaceSlamGoal.tick();
        this.castTauntingCryGoal.tick();
        this.castAvatarGoal.tick();
        this.tickTauntingCry();
        this.tickBattleRage();
        this.tickAvatar();

        if (tickCount % 100 == 0)
            updateKnockbackResistance();

        BlockPos maceSlamBp = castMaceSlamGoal.getCastTarget();
        if (maceSlamBp != null && distanceToSqr(Vec3.atCenterOf(maceSlamBp)) < 3) {
            double x0 = maceSlamBp.getX() - this.getX();
            double z0 = maceSlamBp.getZ() - this.getZ();
            float f = (float) (Mth.atan2(z0, x0) * 57.2957763671875) - 90.0F;
            this.setYRot(this.rotlerp(this.getYRot(), f, 10f));
        } else if (getTarget() != null && distanceToSqr(getTarget().position()) < 3) {
            double x0 = getTarget().getX() - this.getX();
            double z0 = getTarget().getZ() - this.getZ();
            float f = (float) (Mth.atan2(z0, x0) * 57.2957763671875) - 90.0F;
            this.setYRot(this.rotlerp(this.getYRot(), f, 10f));
        }
    }

    private float rotlerp(float pAngle, float pTargetAngle, float pMaxIncrease) {
        float f = Mth.wrapDegrees(pTargetAngle - pAngle);
        if (f > pMaxIncrease) {
            f = pMaxIncrease;
        }
        if (f < -pMaxIncrease) {
            f = -pMaxIncrease;
        }
        return pAngle + f;
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

    public MaceSlam getMaceSlam() {
        for (Ability ability : abilities.get())
            if (ability instanceof MaceSlam)
                return (MaceSlam) ability;
        return null;
    }

    public TauntingCry getTauntingCry() {
        for (Ability ability : abilities.get())
            if (ability instanceof TauntingCry)
                return (TauntingCry) ability;
        return null;
    }

    public BattleRagePassive getBattleRage() {
        for (Ability ability : abilities.get())
            if (ability instanceof BattleRagePassive)
                return (BattleRagePassive) ability;
        return null;
    }

    public Avatar getAvatar() {
        for (Ability ability : abilities.get())
            if (ability instanceof Avatar)
                return (Avatar) ability;
        return null;
    }

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.garrisonGoal = new GarrisonGoal(this);
        this.attackGoal = new MeleeWindupAttackUnitGoal(this, false, ATTACK_WINDUP_TICKS);
        this.attackBuildingGoal = new MeleeWindupAttackBuildingGoal(this, ATTACK_WINDUP_TICKS);
        this.returnResourcesGoal = new ReturnResourcesGoal(this);
        this.castMaceSlamGoal = new GenericTargetedSpellGoal(
                this,
                14,
                MaceSlam.RANGE,
                UnitAnimationAction.ATTACK_UNIT,
                null,
                this::maceSlam,
                null
        );
        this.castTauntingCryGoal = new GenericUntargetedSpellGoal(
                this,
                0,
                this::tauntingCry,
                null,
                UnitAnimationAction.STOP,
                UnitAnimationAction.CAST_SPELL
        );
        this.castAvatarGoal = new GenericUntargetedSpellGoal(
                this,
                40,
                this::enableAvatar,
                UnitAnimationAction.CHARGE_SPELL,
                UnitAnimationAction.STOP,
                null
        );
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();
        this.goalSelector.addGoal(2, usePortalGoal);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, attackGoal);
        this.goalSelector.addGoal(2, attackBuildingGoal);
        this.goalSelector.addGoal(2, returnResourcesGoal);
        this.goalSelector.addGoal(2, garrisonGoal);
        this.targetSelector.addGoal(2, targetGoal);
        this.targetSelector.addGoal(3, moveGoal);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        return pSpawnData;
    }

    @Override
    public void resetBehaviours() {
        animateScaleReducing = true;
        this.castMaceSlamGoal.stop();
        this.castTauntingCryGoal.stop();
        this.castAvatarGoal.stop();
        if (avatarTicksLeft <= 0 && avatarScalingStarted) {
            disableAvatar();
            updateKnockbackResistance();
        }
    }

    public void maceSlam(BlockPos blockPos) {
        if (level().isClientSide())
            return;
        List<LivingEntity> hitEntities = MiscUtil.getEntitiesWithinRange(Vec3.atCenterOf(blockPos.above()), MaceSlam.RADIUS, LivingEntity.class, level())
                .stream()
                .filter(e -> {
                    if (e instanceof Unit unit) {
                        return !List.of(Relationship.OWNED, Relationship.FRIENDLY)
                                .contains(UnitServerEvents.getUnitToEntityRelationship(unit, this));
                    }
                    return true;
                })
                .toList();

        MaceSlam maceSlam = getMaceSlam();
        if (maceSlam != null && maceSlam.getRank(this) > 0) {
            level().explode(null, null, null, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                    2.0f, false, Level.ExplosionInteraction.NONE);

            Set<BuildingPlacement> buildings = new HashSet<>();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BuildingPlacement building = BuildingUtils.findBuilding(level().isClientSide(), blockPos.above().offset(x,y,z));
                        if (building != null)
                            buildings.add(building);
                    }
                }
            }
            for (BuildingPlacement building : buildings) {
                building.destroyRandomBlocks((int) (maceSlam.damage / 2));
            }

            for (LivingEntity hitEntity : hitEntities) {
                if (hitEntity instanceof Unit unit) {
                    Unit.fullResetBehaviours(unit);
                    hitEntity.addEffect(new MobEffectInstance(MobEffectRegistrar.STUN.get(), maceSlam.stunDuration));
                } else {
                    hitEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, maceSlam.stunDuration, 63));
                }

                boolean hurt = hitEntity.hurt(this.damageSources().mobAttack(this), maceSlam.damage);
                if (hurt) {
                    float kb = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
                    hitEntity.knockback(kb * 0.5F, Mth.sin(this.getYRot() * 0.017453292F), -Mth.cos(this.getYRot() * 0.017453292F));
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
                    this.setLastHurtMob(hitEntity);
                }
            }
        }
    }

    public void tauntingCry() {
        if (level().isClientSide())
            return;
        List<AttackerUnit> tauntableUnits = MiscUtil.getEntitiesWithinRange(position(), TauntingCry.RANGE, Mob.class, level())
                .stream()
                .filter(e -> e instanceof AttackerUnit unit &&
                        !List.of(Relationship.OWNED, Relationship.FRIENDLY)
                        .contains(UnitServerEvents.getUnitToEntityRelationship((Unit) unit, this)))
                .map(e -> (AttackerUnit) e)
                .toList();

        TauntingCry tauntingCry = getTauntingCry();
        if (tauntingCry != null && tauntingCry.getRank(this) > 0) {
            for (AttackerUnit unit : tauntableUnits) {
                Unit.fullResetBehaviours((Unit) unit);
                unit.setUnitAttackTargetForced(this);
                ((LivingEntity) unit).addEffect(new MobEffectInstance(MobEffectRegistrar.UNCONTROLLABLE.get(), tauntingCry.duration));
            }
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, tauntingCry.duration, 2));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, tauntingCry.duration, 2));
            tauntingCryTicksLeft = tauntingCry.duration;
            updateKnockbackResistance();
            SoundClientboundPacket.playSoundAtPos(SoundAction.HEROISM, this.getOnPos().above());
        }
    }

    public void tickTauntingCry() {
        if (tauntingCryTicksLeft > 0) {
            tauntingCryTicksLeft -= 1;
        }
        if (tauntingCryTicksLeft == 1) {
            updateKnockbackResistance();
        }
    }

    public void tickBattleRage() {
        if (tickCount % 20 != 0)
            return;
        BattleRagePassive battleRage = getBattleRage();
        if (battleRage != null && battleRage.getRank(this) > 0) {
            float percentRage = 1 - (getHealth() / getMaxHealth());
            heal(percentRage * battleRage.maxHpRegen);
            updateAbilityButtons();
        }
    }

    public float getScale() {
        return 1 + (AVATAR_MAX_BONUS_SCALE * ((float) avatarScaleTicks / AVATAR_SCALE_TICKS_MAX));
    }

    private void tickAvatar() {
        if (avatarTicksLeft > 0) {
            avatarTicksLeft -= 1;
            removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (avatarTicksLeft <= 0) {
                disableAvatar();
                setStatsForLevel();
            }
        }
        if (avatarScalingStarted && avatarScaleTicks < AVATAR_SCALE_TICKS_MAX) {
            avatarScaleTicks += 1;
            if (avatarScaleTicks == AVATAR_SCALE_TICKS_MAX * 0.75f) {
                animateScaleReducing = true;
            }
            this.reapplyPosition();
            this.refreshDimensions();
        } else if (!avatarScalingStarted && avatarScaleTicks > 0) {
            avatarScaleTicks -= 1;
            this.reapplyPosition();
            this.refreshDimensions();
        }
    }

    public void disableAvatar() {
        avatarScalingStarted = false;
        if (!level().isClientSide()) {
            HeroClientboundPacket.deactivateAbilityClientside(getId(), 3);
        }
    }

    public void enableAvatar() {
        avatarTicksLeft = Avatar.DURATION;
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, avatarTicksLeft, 0));
        updateKnockbackResistance();
        setStatsForLevel();
        heal(Avatar.BONUS_HEALTH);
        if (!level().isClientSide()) {
            HeroClientboundPacket.activateAbilityClientside(getId(), 4);
        }
    }

    @Override
    public void makeStuckInBlock(BlockState pState, Vec3 pMotionMultiplier) {
        if (avatarTicksLeft <= 0)
            super.makeStuckInBlock(pState, pMotionMultiplier);
    }

    @Override
    public void activateAbilityClientside(int abilityIndex) {
        if (level().isClientSide()) {
            if (abilityIndex == 3) {
                avatarScalingStarted = true;
            } else if (abilityIndex == 4) {
                enableAvatar();
            }
        }
    }

    @Override
    public void deactivateAbilityClientside(int abilityIndex) {
        if (level().isClientSide()) {
            if (abilityIndex == 3) {
                avatarScalingStarted = false;
            }
        }
    }
}
