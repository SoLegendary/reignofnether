package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.ability.heroAbilities.wretchedwraith.BitterFrostPassive;
import com.solegendary.reignofnether.ability.heroAbilities.wretchedwraith.Blizzard;
import com.solegendary.reignofnether.ability.heroAbilities.wretchedwraith.ChillingScreech;
import com.solegendary.reignofnether.ability.heroAbilities.wretchedwraith.FrostBlink;
import com.solegendary.reignofnether.blocks.BlockServerEvents;
import com.solegendary.reignofnether.building.RangeIndicator;
import com.solegendary.reignofnether.entities.WraithSnowball;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.hero.HeroClientboundPacket;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.resources.BlockUtils;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.time.NightUtils;
import com.solegendary.reignofnether.unit.*;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.*;
import com.solegendary.reignofnether.unit.modelling.animations.WretchedWraithAnimations;
import com.solegendary.reignofnether.util.MiscUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import oshi.util.tuples.Pair;

import javax.annotation.Nullable;
import java.util.*;

public class WretchedWraithUnit extends Monster implements Unit, AttackerUnit, HeroUnit, KeyframeAnimated, RangeIndicator {
    public final Abilities ABILITIES = new Abilities(
        List.of(
            new Pair<>(new ChillingScreech(), Keybindings.keyQ),
            new Pair<>(new FrostBlink(), Keybindings.keyW),
            new Pair<>(new BitterFrostPassive(), Keybindings.keyE),
            new Pair<>(new Blizzard(), Keybindings.keyR)
        )
    );

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

    public Faction getFaction() {return Faction.MONSTERS;}
    public Abilities getAbilities() {return abilities;};
    public List<ItemStack> getItems() {return items;};
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public Goal getAttackBuildingGoal() {return attackBuildingGoal;}
    public Goal getAttackGoal() {return attackGoal;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}
    public MountGoal getMountGoal() {return mountGoal;}

    private EnemySearchBehaviour attackSearchBehaviour = EnemySearchBehaviour.NONE;
    public EnemySearchBehaviour getEnemySearchBehaviour() { return attackSearchBehaviour; }
    public void setEnemySearchBehaviour(EnemySearchBehaviour behaviour) { attackSearchBehaviour = behaviour; }

    private MoveToTargetBlockGoal moveGoal;
    private SelectedTargetGoal<? extends LivingEntity> targetGoal;
    private ReturnResourcesGoal returnResourcesGoal;
    public MountGoal mountGoal;

    private GenericUntargetedSpellGoal castChillingScreechGoal;
    public GenericUntargetedSpellGoal getCastChillingScreechGoal() {
        return castChillingScreechGoal;
    }
    public GenericTargetedSpellGoal castFrostblinkGoal;
    public GenericTargetedSpellGoal getCastFrostblinkGoal() {
        return castFrostblinkGoal;
    }
    private GenericUntargetedSpellGoal castBlizzardGoal;
    public GenericUntargetedSpellGoal getCastBlizzardGoal() {
        return castBlizzardGoal;
    }

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
            SynchedEntityData.defineId(WretchedWraithUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(WretchedWraithUnit.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
        this.entityData.define(scenarioRoleDataAccessor, -1);
    }

    // combat stats
    public boolean getWillRetaliate() {return !isBlizzardInProgress() && willRetaliate;}
    public float getAttackCooldown() {return ((20 / attacksPerSecond) * getAttackCooldownMultiplier());}
    public float getAttacksPerSecond() {return 20f / getAttackCooldown();}
    public float getBaseAttacksPerSecond() {return attacksPerSecond;}
    public float getAggroRange() {return aggroRange;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle && !isVehicle();}
    public float getAttackRange() {return attackRange;}
    public float getMovementSpeed() {return movementSpeed;}
    public float getUnitAttackDamage() {return attackDamage + (attackBonusPerLevel * getHeroLevel());}
    public float getUnitMaxHealth() {return maxHealth + (maxHealthBonusPerLevel * getHeroLevel());}

    @Nullable
    public ResourceCost getCost() {return ResourceCosts.WRETCHED_WRAITH;}
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
    private float baseMaxMana = 120;
    private float maxMana = baseMaxMana;
    private float mana = maxMana;
    private float manaRegenPerSecond = 1;
    private float manaBonusPerLevel = 10;
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

    final static public float attackDamage = 5.0f;
    final static public float attackBonusPerLevel = 0.5f;
    final static public float attacksPerSecond = 0.4f;
    final static public float maxHealth = 140.0f;
    final static public float maxHealthBonusPerLevel = 17.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.28f;
    final static public float attackRange = 3F; // only used by ranged units or melee building attackers
    final static public float aggroRange = 10;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;
    public int maxResources = 100;

    @Override public float getHealthBonusPerLevel() { return maxHealthBonusPerLevel; };
    @Override public float getAttackBonusPerLevel() { return attackBonusPerLevel; };
    @Override public float getBaseHealth() { return maxHealth; };
    @Override public float getBaseAttack() { return attackDamage; };

    public int fogRevealDuration = 0; // set > 0 for the client who is attacked by this unit
    public int getFogRevealDuration() { return fogRevealDuration; }
    public void setFogRevealDuration(int duration) { fogRevealDuration = duration; }

    private AbstractMeleeAttackUnitGoal attackGoal;
    private MeleeAttackBuildingGoal attackBuildingGoal;

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
    public int getAttackWindupTicks() {
        return 12;
    }

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
    public float getAnimationSpeed() {
        return animateSpeed;
    }
    public boolean animateScaleReducing = false;
    public void setAnimateTicksLeft(int ticks) { animateTicks = ticks; }
    public int getAnimateTicksLeft() { return animateTicks; }

    public void playSingleAnimation(UnitAnimationAction animAction) {
        animateScaleReducing = false;
        switch (animAction) {
            case ATTACK_UNIT, ATTACK_BUILDING -> {
                activeAnimDef = WretchedWraithAnimations.ATTACK;
                activeAnimState = attackAnimState;
                animateScale = 1.0f;
                animateSpeed = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CAST_SPELL -> {
                activeAnimDef = WretchedWraithAnimations.SPELL;
                activeAnimState = attackAnimState;
                animateScale = 1.0f;
                animateSpeed = 0.4f;
                startAnimation(activeAnimDef);
            }
            case TELEPORT -> {
                activeAnimDef = WretchedWraithAnimations.TELEPORT;
                activeAnimState = spellChargeAnimState;
                animateScale = 1.0f;
                animateSpeed = 1.0f;
                startAnimation(activeAnimDef);
            }
            case ULTIMATE -> {
                activeAnimDef = WretchedWraithAnimations.ULTIMATE;
                activeAnimState = spellActivateAnimState;
                animateScale = 1.0f;
                animateSpeed = 0.70f;
                startAnimation(activeAnimDef);
            }
            default -> {
                animateScaleReducing = true;
                animateSpeed = 1.0f;
            }
        }
    }

    private HashMap<BlockPos, Integer> snowToPlace = new HashMap<>(); // ticks before being placed
    private int frostblinkTicks = 0;
    private int frostblinkTicksMax = 0;
    private BlockPos frostblinkTarget = null;
    private boolean frostBlinkInProgress = false;
    public boolean isFrostBlinkInProgress() {
        return frostBlinkInProgress;
    }
    private int blizzardTicksLeft = 0;
    public boolean isBlizzardInProgress() {
        return blizzardTicksLeft > 0;
    }

    @Override
    public double getUnitPhysicalArmorPercentage() {
        double dmgAfterAbsorb = CombatRules.getDamageAfterAbsorb(
                1,
                getArmorValue() + (isBlizzardInProgress() ? 13 : 0),
                (float)getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        dmgAfterAbsorb += getDamageTakenIncrease();
        return Math.round((1 - dmgAfterAbsorb)/ 0.01d) * 0.01d;
    }

    public WretchedWraithUnit(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);

        updateAbilityButtons();
        setStatsForLevel();
    }

    // prevent vanilla logic for picking up items
    @Override
    protected void pickUpItem(ItemEntity pItemEntity) { }

    @Override
    public float getDamageAfterMagicAbsorb(DamageSource pSource, float pDamage) {
        pDamage = super.getDamageAfterMagicAbsorb(pSource, pDamage);
        if (pSource.is(DamageTypeTags.WITCH_RESISTANT_TO) || pSource.is(DamageTypes.ON_FIRE))
            pDamage *= 0.7F;
        return pDamage;
    }

    @Override
    public void resetBehaviours() {
        animateScaleReducing = true;
        this.castChillingScreechGoal.stop();
        this.castFrostblinkGoal.stop();
        this.castBlizzardGoal.stop();
        if (this.blizzardTicksLeft > 0) {
            blizzardTicksLeft = 0;
            if (!level().isClientSide()) {
                SoundClientboundPacket.stopSoundWithId(getId());
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, WretchedWraithUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, WretchedWraithUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, WretchedWraithUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, WretchedWraithUnit.armorValue)
                .add(Attributes.ATTACK_KNOCKBACK, 0f)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0f);
    }

    public void tick() {
        this.setCanPickUpLoot(true);
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);
        HeroUnit.tick(this);

        if (level().isClientSide() && animateTicks > 0) {
            animateTicks -= 1;
        }
        this.castChillingScreechGoal.tick();
        this.castFrostblinkGoal.tick();
        this.castBlizzardGoal.tick();

        if (!level().isClientSide()) {
            HashMap<BlockPos, Integer> newSnowQueue = new HashMap<>();
            for (BlockPos pos : snowToPlace.keySet()) {
                int ticksLeft = snowToPlace.get(pos);
                if (ticksLeft <= 0) {
                    BlockServerEvents.placeWraithSnow((ServerLevel) level(), pos, getId());
                } else {
                    newSnowQueue.put(pos, ticksLeft - 1);
                }
            }
            snowToPlace = newSnowQueue;
            // heal while on wraith snow
            int layers = BlockUtils.getWraithSnowLayers(level().getBlockState(getOnPos().above()));
            if (onGround() && layers > 0 && tickCount % (80 / layers) == 0) {
                heal(1);
            }
        }
        if (!level().isClientSide()) {
            tickFrostBlink();
        }
        tickBlizzard();
        if (level().isClientSide() && HudClientEvents.hudSelectedEntity == this) {
            if (!lastOnPos.equals(getOnPos())) {
                updateHighlightBps();
            }
            lastOnPos = getOnPos();
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
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.readUnitSaveData(pCompound);
    }

    @Override
    protected boolean isSunBurnTick() {
        return NightUtils.isSunBurnTick(this);
    }

    @Override
    public SunlightEffect getSunlightEffect() {
        return SunlightEffect.SLOWNESS_I;
    }

    @Override protected SoundEvent getAmbientSound() {
        return SoundRegistrar.WRETCHED_WRAITH_AMBIENT.get();
    }
    @Override protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundRegistrar.WRETCHED_WRAITH_HURT.get();
    }
    @Override protected SoundEvent getDeathSound() {
        return SoundRegistrar.WRETCHED_WRAITH_DEATH.get();
    }
    @Override protected void playStepSound(BlockPos pPos, BlockState pBlock) { }

    public SoundAction getAttackSound() {
        return SoundAction.WRETCHED_WRAITH_ATTACK_QUIET;
    }

    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, false);
        this.garrisonGoal = new GarrisonGoal(this);
        this.attackGoal = new MeleeWindupAttackUnitGoal(this, false);
        this.attackBuildingGoal = new MeleeWindupAttackBuildingGoal(this);
        this.returnResourcesGoal = new ReturnResourcesGoal(this);

        this.castChillingScreechGoal = new GenericUntargetedSpellGoal(
                this,
                20,
                this::ChillingScreech,
                UnitAnimationAction.CAST_SPELL,
                UnitAnimationAction.STOP,
                UnitAnimationAction.STOP
        );
        this.castFrostblinkGoal = new GenericTargetedSpellGoal(
                this,
                10,
                FrostBlink.RANGE_RANK_1,
                UnitAnimationAction.TELEPORT,
                null,
                this::frostBlink,
                null
        );
        this.castBlizzardGoal = new GenericUntargetedSpellGoal(
                this,
                0,
                this::blizzard,
                UnitAnimationAction.ULTIMATE,
                null,
                null
        );
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
    }

    public ChillingScreech getChillingScreech() {
        for (Ability ability : abilities.get())
            if (ability instanceof ChillingScreech)
                return (ChillingScreech) ability;
        return null;
    }

    public FrostBlink getFrostBlink() {
        for (Ability ability : abilities.get())
            if (ability instanceof FrostBlink)
                return (FrostBlink) ability;
        return null;
    }

    public BitterFrostPassive getBitterFrost() {
        for (Ability ability : abilities.get())
            if (ability instanceof BitterFrostPassive)
                return (BitterFrostPassive) ability;
        return null;
    }

    public Blizzard getBlizzard() {
        for (Ability ability : abilities.get())
            if (ability instanceof Blizzard)
                return (Blizzard) ability;
        return null;
    }

    public void ChillingScreech() {
        if (level().isClientSide) return;

        ChillingScreech chillingScreech = getChillingScreech();
        if (chillingScreech.getRank(this) > 0) {
            float radius = getChillingScreech().radius;
            int duration = getChillingScreech().duration;
            snowToPlace.putAll(BlockServerEvents.getSnowPositions(level(), this.getOnPos().above(), (int) radius));
            for (LivingEntity entity : MiscUtil.getEntitiesWithinRange(position(), radius, LivingEntity.class, level())) {
                Relationship rs = UnitServerEvents.getUnitToEntityRelationship(this, entity);
                if (rs != Relationship.FRIENDLY && rs != Relationship.OWNED)
                    entity.addEffect(new MobEffectInstance(MobEffectRegistrar.FROST_DAMAGE.get(), duration));
            }
        }
    }

    public void frostBlink(BlockPos targetPos) {
        if (level().isClientSide) return;
        ArrayList<BlockPos> snowPoses = new ArrayList<>();
        for (BlockPos pos : MiscUtil.getLine2D(getOnPos(), targetPos)) {
            snowPoses.add(MiscUtil.getHighestGroundBlock(level(), pos).above().above());
        }
        for (int i = 0; i < snowPoses.size(); i++) {
            snowToPlace.put(snowPoses.get(i), i);
        }
        HashMap<BlockPos, Integer> startSnowPoses = BlockServerEvents.getSnowPositions(level(), getOnPos().above(), 2);
        snowToPlace.putAll(startSnowPoses);
        HashMap<BlockPos, Integer> endSnowPoses = BlockServerEvents.getSnowPositions(level(), targetPos.above(), 2);
        for (BlockPos pos : endSnowPoses.keySet()) {
            endSnowPoses.replace(pos, endSnowPoses.get(pos) + snowPoses.size() + 1);
        }
        snowToPlace.putAll(endSnowPoses);
        frostBlinkInProgress = true;
        frostblinkTicksMax = snowPoses.size() + 1;
        frostblinkTarget = targetPos;
        SoundClientboundPacket.playSoundAtPos(SoundAction.WRETCHED_WRAITH_TELEPORT_START, blockPosition());
    }

    private void tickFrostBlink() {
        if (frostblinkTicksMax > 0 && frostBlinkInProgress) {
            frostblinkTicks += 1;
            if (frostblinkTicks > 3) {
                teleportTo(getX(), getY() - 20, getZ());
            }
            if (frostblinkTicks > frostblinkTicksMax && frostblinkTarget != null) {
                SoundClientboundPacket.playSoundAtPos(SoundAction.WRETCHED_WRAITH_TELEPORT_END, frostblinkTarget);
                frostBlinkInProgress = false;
                teleportTo(frostblinkTarget.above().getX(), frostblinkTarget.above().getY(), frostblinkTarget.above().getZ());
                frostblinkTicks = 0;
                frostblinkTicksMax = 0;
                frostblinkTarget = null;
            }
        }
    }

    private void freezeRandomNearbyEnemy() {
        ArrayList<LivingEntity> mobs = new ArrayList<>(MiscUtil.getEntitiesWithinRange(position(), Blizzard.RADIUS, LivingEntity.class, level()));
        Collections.shuffle(mobs);
        for (LivingEntity mob : mobs) {
            Relationship rs = UnitServerEvents.getUnitToEntityRelationship(this, mob);
            if (rs != Relationship.OWNED && rs != Relationship.FRIENDLY &&
                mob.onGround() && !(mob instanceof WretchedWraithUnit) &&
                !mob.hasEffect(MobEffectRegistrar.FREEZE.get())) {
                int duration = Blizzard.FREEZE_DURATION;
                BlockServerEvents.addTempBlock((ServerLevel) level(), mob.getOnPos().above(),
                        Blocks.PACKED_ICE.defaultBlockState(), Blocks.AIR.defaultBlockState(), duration, true);
                BlockServerEvents.addTempBlock((ServerLevel) level(), mob.getOnPos().above().above(),
                        Blocks.PACKED_ICE.defaultBlockState(), Blocks.AIR.defaultBlockState(), duration, true);
                BlockServerEvents.addTempBlock((ServerLevel) level(), mob.getOnPos().above().above().above(),
                        BlockRegistrar.WRAITH_SNOW_LAYER.get().defaultBlockState(), Blocks.AIR.defaultBlockState(), duration, true);
                snowToPlace.putAll(BlockServerEvents.getSnowPositions(level(), mob.getOnPos().above(), 2));
                mob.addEffect(new MobEffectInstance(MobEffectRegistrar.FREEZE.get(), duration));
                mob.addEffect(new MobEffectInstance(MobEffectRegistrar.FROST_DAMAGE.get(), duration));
                MiscUtil.addParticleExplosion(ParticleTypes.SNOWFLAKE, 10, level(), mob.position());
                break;
            }
        }
    }

    public void spawnSnowStorm() {
        int radius = Blizzard.RADIUS;
        for (int i = 0; i < 15; i++) {
            double x = getX() + random.nextDouble() * radius * 2 - radius;
            double y = getY() + random.nextDouble() * 15 + 5;
            double z = getZ() + random.nextDouble() * radius * 2 - radius;

            double vx = random.nextGaussian() * 0.01;
            double vy = -0.03 - random.nextDouble() * 0.02;
            double vz = random.nextGaussian() * 0.01;

            Vec2 stormPos2d = new Vec2((float) x, (float) z);
            Vec2 wraithPos2d = new Vec2((float) getX(), (float) getZ());

            if (stormPos2d.distanceToSqr(wraithPos2d) < Blizzard.RADIUS * Blizzard.RADIUS) {
                ((ServerLevel) level()).sendParticles(
                        ParticleTypes.ITEM_SNOWBALL,
                        x, y, z,
                        1,
                        vx, vy, vz,
                        0.0
                );
                if (i == 14) {
                    WraithSnowball snowball = EntityRegistrar.WRAITH_SNOWBALL.get().create(this.level());
                    if (snowball != null) {
                        snowball.moveTo(x, y, z);
                        snowball.setDeltaMovement(vx * 5, vy * 5, vz * 5);
                        snowball.setOwner(this);
                        level().addFreshEntity(snowball);
                    }
                }
            }
        }
    }

    private void tickBlizzard() {
        if (blizzardTicksLeft > 0) {
            blizzardTicksLeft -= 1;
        }
        if (!level().isClientSide()) {
            if (blizzardTicksLeft <= 0) {
                SoundClientboundPacket.stopSoundWithId(getId());
            }
            if (blizzardTicksLeft > 0 && blizzardTicksLeft < Blizzard.CHANNEL_DURATION - 20) {
                if (blizzardTicksLeft % 30 == 0) {
                    freezeRandomNearbyEnemy();
                }
                if (blizzardTicksLeft % 3 == 0) {
                    spawnSnowStorm();
                }
            }
        }
    }

    @Override
    public boolean isIdle() {
        boolean idleAttacker = getAttackMoveTarget() == null &&
                !hasLivingTarget() &&
                !AttackerUnit.isAttackingBuilding(this);

        // some larger mobs like bears get stuck near their movetarget so nav won't be done but it also won't be null
        boolean stationaryNearMoveTarget = false;
        if (this.getMoveGoal().getMoveTarget() != null) {
            double distToMoveTarget = this.distanceToSqr(this.getMoveGoal().getMoveTarget().getCenter());
            boolean stationary = this.getDeltaMovement().x == 0 || this.getDeltaMovement().z == 0;
            stationaryNearMoveTarget = stationary && distToMoveTarget < 4;
        }
        return (this.getMoveGoal().getMoveTarget() == null || stationaryNearMoveTarget) &&
                this.getFollowTarget() == null &&
                idleAttacker &&
                !isBlizzardInProgress() &&
                !isFrostBlinkInProgress();
    }

    public void blizzard() {
        blizzardTicksLeft = Blizzard.CHANNEL_DURATION;
        if (!level().isClientSide) {
            AbilityClientboundPacket.doAbility(getId(), UnitAction.BLIZZARD, blizzardTicksLeft);
            SoundClientboundPacket.playFadeableLoopingSoundAtPos(SoundAction.WRETCHED_WRAITH_BLIZZARD, blockPosition(), 1.0f, getId(), Blizzard.CHANNEL_DURATION);
        }
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity pEntity) {
        boolean result = super.doHurtTarget(pEntity);
        if (result && getBitterFrost().getRank(this) >= 1 && !level().isClientSide()) {
            snowToPlace.putAll(BlockServerEvents.getSnowPositions(level(), pEntity.getOnPos().above(), 1));
        }
        return result;
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        boolean result = super.hurt(pSource, pAmount);
        if (result && getBitterFrost().getRank(this) >= 2 && !level().isClientSide()) {
            if (pSource.getEntity() instanceof Mob mob) {
                BlockServerEvents.placeWraithSnow((ServerLevel) level(), mob.getOnPos().above(), getId());
            } else if (pSource.getEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Mob mob) {
                BlockServerEvents.placeWraithSnow((ServerLevel) level(), mob.getOnPos().above(), getId());
            }
        }
        return result;
    }

    @Override
    public float getBonusMeleeRange() {
        return 0.6f;
    }

    @Override
    public float getBonusMeleeRangeForAttackers() {
        return 0.3f;
    }

    @Override
    public AABB getInflatedSelectionBox() {
        AABB aabb = this.getBoundingBox().inflate(0.4f, 0, 0.4f);
        aabb.setMaxY(aabb.maxY + 1.0f);
        return aabb;
    }

    @Override
    public boolean isPushable() {
        return !isBlizzardInProgress() && !isFrostBlinkInProgress();
    }

    @Override
    public boolean uninterruptable() {
        return isBlizzardInProgress();
    }
}
