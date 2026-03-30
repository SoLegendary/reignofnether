package com.solegendary.reignofnether.unit.units.piglins;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.ability.abilities.FirewallShot;
import com.solegendary.reignofnether.ability.heroAbilities.wildfire.IntenseHeatPassive;
import com.solegendary.reignofnether.ability.heroAbilities.wildfire.MoltenBomb;
import com.solegendary.reignofnether.ability.heroAbilities.wildfire.ScorchingGaze;
import com.solegendary.reignofnether.ability.heroAbilities.wildfire.SoulsAflame;
import com.solegendary.reignofnether.building.RangeIndicator;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.entities.BlazeUnitFireball;
import com.solegendary.reignofnether.entities.MoltenBombProjectile;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.hero.HeroClientboundPacket;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.registrars.ParticleRegistrar;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.*;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.*;
import com.solegendary.reignofnether.unit.modelling.animations.WildfireAnimations;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import oshi.util.tuples.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class WildfireUnit extends Blaze implements Unit, AttackerUnit, RangedAttackerUnit, HeroUnit, KeyframeAnimated, RangeIndicator {
    public final Abilities ABILITIES = new Abilities(
            List.of(
                    new Pair<>(new MoltenBomb(), Keybindings.keyQ),
                    new Pair<>(new ScorchingGaze(), Keybindings.keyW),
                    new Pair<>(new IntenseHeatPassive(), Keybindings.keyE),
                    new Pair<>(new SoulsAflame(), Keybindings.keyR)
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

    public Faction getFaction() {return Faction.PIGLINS;}
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

    private GenericTargetedSpellGoal castMoltenBombGoal;
    public GenericTargetedSpellGoal getCastMoltenBombGoal() {
        return castMoltenBombGoal;
    }
    private GenericTargetedSpellGoal castScorchingGazeGoal;
    public GenericTargetedSpellGoal getCastScorchingGazeGoal() {
        return castScorchingGazeGoal;
    }
    private GenericUntargetedSpellGoal castSoulsAflameGoal;
    public GenericUntargetedSpellGoal getCastSoulsAflameGoal() {
        return castSoulsAflameGoal;
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
            SynchedEntityData.defineId(WildfireUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(WildfireUnit.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
        this.entityData.define(scenarioRoleDataAccessor, -1);
    }

    // combat stats
    public boolean getWillRetaliate() {return willRetaliate;}
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
    public ResourceCost getCost() {return ResourceCosts.WILDFIRE;}
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
    private float manaRegenPerSecond = 0.6f;
    private float manaBonusPerLevel = 8;
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

    final static public float attackDamage = 2.0f;
    final static public float attackBonusPerLevel = 0.25f;
    final static public float attacksPerSecond = 0.6f;
    final static public float maxHealth = 120.0f;
    final static public float maxHealthBonusPerLevel = 12.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.25f;
    final static public float attackRange = 12.0F; // only used by ranged units or melee building attackers
    final static public float aggroRange = 12;
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

    private UnitRangedAttackGoal<? extends LivingEntity> attackGoal;
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
    public float getAnimationSpeed() {
        return animateSpeed;
    }

    // animation attack peak starts at 44% the way through, but we need to set it to 22% for some reason?
    final static private int ATTACK_WINDUP_TICKS = 6;

    // non-looping animations
    public AnimationDefinition activeAnimDef = null;
    public AnimationState activeAnimState = null;

    // smoother look controls
    private float targetYaw;
    private float targetPitch;
    private int rotateTicksRemaining = 0;

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
                activeAnimDef = WildfireAnimations.ATTACK;
                activeAnimState = attackAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CAST_SPELL -> {
                activeAnimDef = WildfireAnimations.SPELL_SLAM;
                activeAnimState = spellActivateAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CAST_SPELL_ALT -> {
                activeAnimDef = WildfireAnimations.SPELL_STARE;
                activeAnimState = spellActivateAnimState;
                animateScale = 1.0f;
                animateSpeed = 0.75f;
                startAnimation(activeAnimDef);
            }
            case ULTIMATE -> {
                activeAnimDef = WildfireAnimations.ULTIMATE;
                activeAnimState = spellActivateAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            default -> {
                animateScaleReducing = true;
                animateSpeed = 1.0f;
            }
        }
    }

    public WildfireUnit(EntityType<? extends Blaze> entityType, Level level) {
        super(entityType, level);

        updateAbilityButtons();
        setStatsForLevel();
    }

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
        this.castMoltenBombGoal.stop();
        this.castScorchingGazeGoal.stop();
        this.castSoulsAflameGoal.stop();
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, WildfireUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, WildfireUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, WildfireUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, WildfireUnit.armorValue)
                .add(Attributes.ATTACK_KNOCKBACK, 0);
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
        this.castMoltenBombGoal.tick();
        this.castScorchingGazeGoal.tick();
        this.castSoulsAflameGoal.tick();

        if (level().isClientSide() && HudClientEvents.hudSelectedEntity == this) {
            if (!lastOnPos.equals(getOnPos()) || !lastCursorPos.equals(CursorClientEvents.getPreselectedBlockPos())) {
                updateHighlightBps();
            }
            lastOnPos = getOnPos();
            lastCursorPos = CursorClientEvents.getPreselectedBlockPos();
        }
        if (rotateTicksRemaining > 0) {
            float yawStep = Mth.wrapDegrees(targetYaw - getYRot()) / rotateTicksRemaining;
            float pitchStep = (targetPitch - getXRot()) / rotateTicksRemaining;
            setYRot(getYRot() + yawStep);
            setXRot(getXRot() + pitchStep);
            yRotO = getYRot();
            xRotO = getXRot();
            yBodyRot = getYRot();
            yHeadRot = getYRot();
            rotateTicksRemaining--;
        }
        if (!level().isClientSide() && getIntenseHeatPassive().getRank(this) > 0 && tickCount % 10 == 0) {
            List<Mob> nearbyMobs = MiscUtil.getEntitiesWithinRange(position(), IntenseHeatPassive.MAX_RANGE, Mob.class, level());
            ArrayList<Mob> nearbyEnemies = new ArrayList<>();
            for (Mob mob : nearbyMobs)
                if (UnitServerEvents.getUnitToEntityRelationship(this, mob) != Relationship.FRIENDLY)
                    nearbyEnemies.add(mob);
            int maxAmp = getIntenseHeatPassive().getMaxAmp();

            for (Mob mob : nearbyEnemies) {
                double distSqr = distanceToSqr(mob);
                double minSqr = IntenseHeatPassive.MIN_RANGE * IntenseHeatPassive.MIN_RANGE;
                double maxSqr = IntenseHeatPassive.MAX_RANGE * IntenseHeatPassive.MAX_RANGE;
                double t = (distSqr - minSqr) / (maxSqr - minSqr);
                t = 1 - Mth.clamp(t, 0.0, 1.0);
                int amp = (int) Math.round(t * maxAmp);
                mob.addEffect(new MobEffectInstance(MobEffectRegistrar.INTENSE_HEAT.get(), 15, amp, true, true));
            }
        }
    }

    private Set<BlockPos> highlightBps = new HashSet<>();
    private BlockPos lastOnPos = new BlockPos(0,0,0);
    private BlockPos lastCursorPos = new BlockPos(0,0,0);

    @Override public Set<BlockPos> getHighlightBps() { return highlightBps; }
    @Override public void setHighlightBps(Set<BlockPos> bps) { highlightBps = bps; }

    @Override public void updateHighlightBps() {
        if (!level().isClientSide())
            return;
        this.highlightBps.clear();
        if (CursorClientEvents.getLeftClickAction() == UnitAction.MOLTEN_BOMB) {
            BlockPos limitedBp = MyMath.getXZRangeLimitedBlockPos(getOnPos(), CursorClientEvents.getPreselectedBlockPos(), MoltenBomb.RANGE);
            for (BlockPos pos : MiscUtil.getLine2D(getOnPos(), limitedBp)) {
                this.highlightBps.add(MiscUtil.getHighestGroundBlock(level(), pos).above());
            }
            this.highlightBps.addAll(MiscUtil.getRangeIndicatorFilledCircleBlocks(limitedBp, (int) getMoltenBomb().radius - 1, level()));
        } else if (CursorClientEvents.getLeftClickAction() == UnitAction.SCORCHING_GAZE) {
            setHighlightBps(MiscUtil.getRangeIndicatorCircleBlocks(blockPosition(),
                    ScorchingGaze.RANGE - 1,
                    level()
            ));
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

    @Override protected SoundEvent getAmbientSound() {
        return SoundRegistrar.WILDFIRE_AMBIENT.get();
    }
    @Override protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundRegistrar.WILDFIRE_HURT.get();
    }
    @Override protected SoundEvent getDeathSound() {
        return SoundRegistrar.WILDFIRE_DEATH.get();
    }
    @Override protected void playStepSound(BlockPos pPos, BlockState pBlock) { }

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, false);
        this.garrisonGoal = new GarrisonGoal(this);
        this.attackGoal = new UnitRangedAttackGoal<>(this, ATTACK_WINDUP_TICKS);
        this.returnResourcesGoal = new ReturnResourcesGoal(this);
        this.castMoltenBombGoal = new GenericTargetedSpellGoal(
                this,
                20,
                Integer.MAX_VALUE, // cast without moving to the target location first
                UnitAnimationAction.CAST_SPELL,
                null,
                this::doMoltenBomb,
                null
        );
        this.castMoltenBombGoal.setOnStartChanneling((BlockPos bp) -> {
            if (!this.level().isClientSide()) {
                SoundClientboundPacket.playSoundAtPos(SoundAction.WILDFIRE_MOLTEN_BOMB, blockPosition());
            }
            setSmoothLookAtTarget(bp, 10);
        });
        this.castScorchingGazeGoal = new GenericTargetedSpellGoal(
                this,
                40,
                ScorchingGaze.RANGE,
                UnitAnimationAction.CAST_SPELL_ALT,
                this::scorchingGaze,
                null,
                null
        );
        this.castScorchingGazeGoal.setOnStartChanneling((BlockPos bp) -> {
            if (!this.level().isClientSide()) {
                SoundClientboundPacket.playSoundAtPos(SoundAction.WILDFIRE_SCORCHING_GAZE_START, blockPosition(), 1.5f);
            }
            setSmoothLookAtTarget(bp, 10);
        });
        this.castScorchingGazeGoal.instantLook = true;
        this.castSoulsAflameGoal = new GenericUntargetedSpellGoal(
                this,
                54,
                this::soulsAflame,
                UnitAnimationAction.ULTIMATE,
                null,
                null
        );
        this.castSoulsAflameGoal.setOnStartChanneling(() -> {
            if (!this.level().isClientSide()) {
                SoundClientboundPacket.playSoundAtPos(SoundAction.WILDFIRE_SOULS_AFLAME, blockPosition());
            }
        });
    }

    private void setSmoothLookAtTarget(BlockPos bp, int ticks) {
        double dx = bp.getX() + 0.5 - getX();
        double dy = bp.getY() + 0.5 - getEyeY();
        double dz = bp.getZ() + 0.5 - getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        targetYaw = (float)(Math.atan2(dz, dx) * (180F / Math.PI)) - 90F;
        targetPitch = (float)-(Math.atan2(dy, horiz) * (180F / Math.PI));
        rotateTicksRemaining = 10;
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

    public MoltenBomb getMoltenBomb() {
        for (Ability ability : abilities.get())
            if (ability instanceof MoltenBomb)
                return (MoltenBomb) ability;
        return null;
    }

    public ScorchingGaze getScorchingGaze() {
        for (Ability ability : abilities.get())
            if (ability instanceof ScorchingGaze)
                return (ScorchingGaze) ability;
        return null;
    }

    public IntenseHeatPassive getIntenseHeatPassive() {
        for (Ability ability : abilities.get())
            if (ability instanceof IntenseHeatPassive)
                return (IntenseHeatPassive) ability;
        return null;
    }

    public SoulsAflame getSoulsAflame() {
        for (Ability ability : abilities.get())
            if (ability instanceof SoulsAflame)
                return (SoulsAflame) ability;
        return null;
    }

    @Override
    public void performUnitRangedAttack(LivingEntity pTarget, float velocity) {
        if (!this.abilities.isEmpty() &&
                this.abilities.get().get(0) instanceof FirewallShot firewallShot &&
                !firewallShot.isOffCooldown(this))
            return;

        LivingEntity target = this.getTarget();
        if (target != null) {
            double x = target.getX() - this.getX();
            double y = target.getY(0.5) - this.getY(0.5);
            double z = target.getZ() - this.getZ();
            BlazeUnitFireball fireball = new BlazeUnitFireball(this.level(), this, x, y, z, false);
            fireball.setPos(fireball.getX(), this.getY(0.5) + 0.5, fireball.getZ());
            this.playSound(SoundEvents.BLAZE_SHOOT, 3.0F, 1.0F);
            this.level().addFreshEntity(fireball);

            if (!level().isClientSide() && target instanceof Unit unit)
                FogOfWarClientboundPacket.revealRangedUnit(unit.getOwnerName(), this.getId());
        }
    }

    public void doMoltenBomb(BlockPos bp) {
        if (level().isClientSide())
            return;

        bp = MyMath.getXZRangeLimitedBlockPos(this.blockPosition(), bp, getMoltenBomb().range);

        double x = bp.getCenter().x() - this.getX();
        double y = bp.getCenter().y() - this.getY(0.5);
        double z = bp.getCenter().z() - this.getZ();
        MoltenBombProjectile proj = new MoltenBombProjectile(this.level(), this, x, y, z);
        proj.setMaxTicks((int) (bp.getCenter().distanceTo(position())) * 2);
        proj.setPos(this.getEyePosition());

        level().addFreshEntity(proj);
        proj.setInvulnerable(true);
        this.level().addFreshEntity(proj);
    }

    public void scorchingGaze(LivingEntity targetEntity) {
        if (!this.level().isClientSide()) {
            SoundClientboundPacket.playSoundAtPos(SoundAction.WILDFIRE_SCORCHING_GAZE_END, blockPosition());
        }
        int durationTicks = getScorchingGaze().durationSeconds * 20;
        targetEntity.addEffect(new MobEffectInstance(MobEffectRegistrar.SCORCHING_FIRE.get(), durationTicks, getScorchingGaze().durationSeconds));
        targetEntity.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks,0, true, true));
        if (hasEffect(MobEffectRegistrar.SOULS_AFLAME.get())) {
            targetEntity.addEffect(new MobEffectInstance(MobEffectRegistrar.SCORCHING_FIRE.get(), durationTicks + 20, 0, true, true));
        }
        targetEntity.setSecondsOnFire(getScorchingGaze().durationSeconds);
    }

    public void soulsAflame() {
        if (hasEffect(MobEffectRegistrar.SOULS_AFLAME.get()))
            return;

        MiscUtil.addParticleExplosion(ParticleRegistrar.BIG_SOUL_FLAME.get(), 15, level(), position().add(0,2,0));
        addEffect(new MobEffectInstance(MobEffectRegistrar.SOULS_AFLAME.get(), SoulsAflame.DURATION, 0, true, true));
        if (!level().isClientSide()) {
            convertNearbyBlazes();
            convertNearbyFires();
        }
    }

    private void convertNearbyBlazes() {
        List<LivingEntity> nearbyUnits = MiscUtil.getEntitiesWithinRange(position(), SoulsAflame.RANGE, LivingEntity.class, level());
        for (LivingEntity le : nearbyUnits) {
            if (le instanceof BlazeUnit blazeUnit && UnitServerEvents.getUnitToEntityRelationship(this, blazeUnit) == Relationship.FRIENDLY) {
                le.addEffect(new MobEffectInstance(MobEffectRegistrar.SOULS_AFLAME.get(), SoulsAflame.DURATION, 0, true, true));
            }
        }
    }

    private void convertNearbyFires() {
        int range = SoulsAflame.RANGE;
        int x0 = blockPosition().getX();
        int y0 = blockPosition().getY();
        int z0 = blockPosition().getZ();
        for (int x = -range + x0; x < range + x0; x++) {
            for (int y = -range + y0; y < range + y0; y++) {
                for (int z = -range + z0; z < range + z0; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (blockPosition().distToCenterSqr(pos.getCenter()) < range * range) {
                        System.out.println(z);
                        BlockState bs = level().getBlockState(pos);
                        if (bs.getBlock() == Blocks.FIRE) {
                            level().setBlockAndUpdate(pos, BlockRegistrar.UNEXTINGUISHABLE_SOUL_FIRE.get().defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    // prevent vanilla logic for picking up items
    @Override
    protected void pickUpItem(ItemEntity pItemEntity) { }

    @Override
    public void setupEquipmentAndUpgradesServer() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    @Override
    public AABB getInflatedSelectionBox() {
        AABB aabb = this.getBoundingBox().inflate(0.6f, 0, 0.6f);
        aabb.setMaxY(aabb.maxY + 1.2f);
        return aabb;
    }

    @Override
    public List<FormattedCharSequence> getAttackDamageStatTooltip() {
        return List.of(
                fcs(I18n.get("unitstats.reignofnether.attack_damage"), true),
                fcs(I18n.get("unitstats.reignofnether.attack_damage_bonus_fire_damage", BlazeUnitFireball.FIRE_SECONDS, BlazeUnitFireball.FIRE_SECONDS))
        );
    }

    @Override
    public boolean hasBonusDamage() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        // disallow rising into the air (sometimes out of melee range) when attacking a nearby target)
    }
}
