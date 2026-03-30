package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.ability.heroAbilities.necromancer.BloodMoon;
import com.solegendary.reignofnether.ability.heroAbilities.necromancer.InsomniaCurse;
import com.solegendary.reignofnether.ability.heroAbilities.necromancer.RaiseDead;
import com.solegendary.reignofnether.ability.heroAbilities.necromancer.SoulSiphonPassive;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.RangeIndicator;
import com.solegendary.reignofnether.entities.NecromancerProjectile;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.hero.HeroClientboundPacket;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.time.NightUtils;
import com.solegendary.reignofnether.time.TimeServerEvents;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.*;
import com.solegendary.reignofnether.unit.modelling.animations.NecromancerAnimations;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import oshi.util.tuples.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NecromancerUnit extends Skeleton implements Unit, AttackerUnit, RangedAttackerUnit, HeroUnit, KeyframeAnimated, RangeIndicator {
    public final Abilities ABILITIES = new Abilities(
        List.of(
            new Pair<>(new RaiseDead(), Keybindings.keyQ),
            new Pair<>(new InsomniaCurse(), Keybindings.keyW),
            new Pair<>(new SoulSiphonPassive(), Keybindings.keyE),
            new Pair<>(new BloodMoon(), Keybindings.keyR)
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

    private GenericUntargetedSpellGoal castRaiseDeadGoal;
    public GenericUntargetedSpellGoal getCastRaiseDeadGoal() {
        return castRaiseDeadGoal;
    }
    private GenericTargetedSpellGoal castPhantomGoal;
    public GenericTargetedSpellGoal getCastPhantomGoal() {
        return castPhantomGoal;
    }
    private GenericTargetedSpellGoal castBloodMoonGoal;
    public GenericTargetedSpellGoal getCastBloodMoonGoal() {
        return castBloodMoonGoal;
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
            SynchedEntityData.defineId(NecromancerUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(NecromancerUnit.class, EntityDataSerializers.INT);

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
    public ResourceCost getCost() {return ResourceCosts.NECROMANCER;}
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
    private float baseMaxMana = 150;
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

    final static public float attackDamage = 4.0f;
    final static public float attackBonusPerLevel = 0.4f;
    final static public float attacksPerSecond = 0.35f;
    final static public float maxHealth = 100.0f;
    final static public float maxHealthBonusPerLevel = 12.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.25f;
    final static public float attackRange = 12.0F; // only used by ranged units or melee building attackers
    final static public float aggroRange = 12;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;
    public int maxResources = 100;

    public int souls = 0;

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

    // animation attack peak starts at 44% the way through, but we need to set it to 22% for some reason?
    final static private int ATTACK_WINDUP_TICKS = 6; // (int) (NecromancerAnimations.ATTACK.lengthInSeconds() * 20f * 0.22f);

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
                activeAnimDef = NecromancerAnimations.ATTACK;
                activeAnimState = attackAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CHARGE_SPELL -> {
                activeAnimDef = NecromancerAnimations.SPELL_CHARGE;
                activeAnimState = spellChargeAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CAST_SPELL -> {
                activeAnimDef = NecromancerAnimations.SPELL_ACTIVATE;
                activeAnimState = spellActivateAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            default -> animateScaleReducing = true;
        }
    }

    public NecromancerUnit(EntityType<? extends Skeleton> entityType, Level level) {
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
        this.castRaiseDeadGoal.stop();
        this.castPhantomGoal.stop();
        this.castBloodMoonGoal.stop();
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, NecromancerUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, NecromancerUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, NecromancerUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, NecromancerUnit.armorValue);
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
        this.castRaiseDeadGoal.tick();
        this.castPhantomGoal.tick();
        this.castBloodMoonGoal.tick();

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

    public RaiseDead getRaiseDead() {
        for (Ability ability : abilities.get())
            if (ability instanceof RaiseDead)
                return (RaiseDead) ability;
        return null;
    }

    public SoulSiphonPassive getSoulSiphon() {
        for (Ability ability : abilities.get())
            if (ability instanceof SoulSiphonPassive)
                return (SoulSiphonPassive) ability;
        return null;
    }

    @Override
    protected boolean isSunBurnTick() {
        return NightUtils.isSunBurnTick(this);
    }

    @Override
    public SunlightEffect getSunlightEffect() {
        return SunlightEffect.SLOWNESS_I;
    }

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, false);
        this.garrisonGoal = new GarrisonGoal(this);
        this.attackGoal = new UnitRangedAttackGoal<>(this, ATTACK_WINDUP_TICKS);
        this.returnResourcesGoal = new ReturnResourcesGoal(this);
        this.castRaiseDeadGoal = new GenericUntargetedSpellGoal(
                this,
                RaiseDead.CHANNEL_TICKS,
                this::raiseDead,
                UnitAnimationAction.CHARGE_SPELL,
                UnitAnimationAction.STOP,
                UnitAnimationAction.CAST_SPELL
        );
        this.castPhantomGoal = new GenericTargetedSpellGoal(
                this,
                0,
                InsomniaCurse.RANGE,
                UnitAnimationAction.CAST_SPELL,
                this::summonPhantomEntity,
                null,
                this::summonPhantomBuilding
        );
        this.castBloodMoonGoal = new GenericTargetedSpellGoal(
                this,
                BloodMoon.CHANNEL_TICKS,
                999999,
                UnitAnimationAction.CAST_SPELL,
                null,
                null,
                this::doBloodMoon
        );
            /*= new GenericUntargetedSpellGoal(
            this,
            BloodMoon.CHANNEL_TICKS,
            this::doBloodMoon,
            UnitAnimationAction.CHARGE_SPELL,
            UnitAnimationAction.STOP,
            UnitAnimationAction.CAST_SPELL
           );
             */
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

    @Override
    public void setupEquipmentAndUpgradesServer() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PAPER)); // prevent burning in sunlight
    }

    // override to make inaccuracy 0
    @Override
    public void performUnitRangedAttack(LivingEntity pTarget, float velocity) {

        double x = pTarget.getX() - this.getX();
        double y = pTarget.getY(0.5) - this.getY(0.5);
        double z = pTarget.getZ() - this.getZ();
        NecromancerProjectile proj = new NecromancerProjectile(this.level(), this, x, y, z);
        proj.setPos(this.getEyePosition());

        level().addFreshEntity(proj);

        this.playSound(SoundEvents.SHULKER_SHOOT, 3.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level().isClientSide() && pTarget instanceof Unit unit)
            FogOfWarClientboundPacket.revealRangedUnit(unit.getOwnerName(), this.getId());
    }

    // returns rank of soul siphon if any souls were consumed
    public int consumeSoulsAndGetSoulRank() {
        SoulSiphonPassive soulSiphon = getSoulSiphon();
        if (soulSiphon != null) {
            if (soulSiphon.consumeSoulsForCast(this)) {
                return soulSiphon.getRank(this);
            }
        }
        return 0;
    }

    public void raiseDead() {
        if (this.level().isClientSide())
            return;

        int soulRank = consumeSoulsAndGetSoulRank();
        int raiseDeadRank = getRaiseDead().getRank(this);

        for(int i = 0; i < 2; ++i) {
            BlockPos blockpos = this.blockPosition().offset(-2 + this.random.nextInt(5), 1, -2 + this.random.nextInt(5));
            ZombieUnit zombieUnit = EntityRegistrar.ZOMBIE_UNIT.get().create(this.level());
            if (zombieUnit != null) {
                zombieUnit.moveTo(blockpos, 0.0F, 0.0F);
                zombieUnit.setOwnerName(this.getOwnerName());
                this.level().addFreshEntity(zombieUnit);

                ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
                ItemStack chestPlate = new ItemStack(Items.LEATHER_CHESTPLATE);
                ItemStack leggings = new ItemStack(Items.LEATHER_LEGGINGS);
                ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
                ItemStack sword = new ItemStack(Items.WOODEN_SWORD);
                if (raiseDeadRank == 2) {
                    helmet = new ItemStack(Items.CHAINMAIL_HELMET);
                    chestPlate = new ItemStack(Items.CHAINMAIL_CHESTPLATE);
                    leggings = new ItemStack(Items.CHAINMAIL_LEGGINGS);
                    boots = new ItemStack(Items.CHAINMAIL_BOOTS);
                    sword = new ItemStack(Items.STONE_SWORD);
                } else if (raiseDeadRank >= 3) {
                    helmet = new ItemStack(Items.IRON_HELMET);
                    chestPlate = new ItemStack(Items.IRON_CHESTPLATE);
                    leggings = new ItemStack(Items.IRON_LEGGINGS);
                    boots = new ItemStack(Items.IRON_BOOTS);
                    sword = new ItemStack(Items.IRON_SWORD);
                }
                if (soulRank >= 1)
                    chestPlate.enchant(Enchantments.THORNS, 3);
                if (soulRank >= 2)
                    leggings.enchant(Enchantments.THORNS, 3);
                if (soulRank >= 3) {
                    boots.enchant(Enchantments.THORNS, 2);
                    helmet.enchant(Enchantments.THORNS, 2);
                }
                zombieUnit.setItemSlot(EquipmentSlot.HEAD, helmet);
                zombieUnit.setItemSlot(EquipmentSlot.CHEST, chestPlate);
                zombieUnit.setItemSlot(EquipmentSlot.LEGS, leggings);
                zombieUnit.setItemSlot(EquipmentSlot.FEET, boots);
                zombieUnit.setItemSlot(EquipmentSlot.MAINHAND, sword);
            }
        }
    }

    public void summonPhantomEntity(LivingEntity targetEntity) {
        if (this.level().isClientSide())
            return;
        PhantomSummon phantom = summonPhantom();
        if (phantom != null) {
            phantom.entityTarget = targetEntity;
        }
    }

    public void summonPhantomBuilding(BuildingPlacement targetBuilding) {
        if (this.level().isClientSide())
            return;
        PhantomSummon phantom = summonPhantom();
        if (phantom != null) {
            if (targetBuilding.getTargetStand() != null)
                phantom.entityTarget = targetBuilding.getTargetStand();
        }
    }

    public PhantomSummon summonPhantom() {
        BlockPos blockpos = this.blockPosition().offset(-2 + this.random.nextInt(5), 1, -2 + this.random.nextInt(5));
        PhantomSummon phantom = EntityRegistrar.PHANTOM_SUMMON.get().create(this.level());
        if (phantom != null) {
            phantom.moveTo(blockpos.offset(0,5,0), 0.0F, 0.0F);
            int soulRank = consumeSoulsAndGetSoulRank();
            phantom.setPhantomSize(soulRank == 0 ? 0 : soulRank + 1);
            AttributeInstance ai = phantom.getAttribute(Attributes.ATTACK_DAMAGE);
            if (ai != null) {
                ai.setBaseValue(InsomniaCurse.PHANTOM_DAMAGE + (soulRank * InsomniaCurse.PHANTOM_DAMAGE_BONUS_PER_SOUL_RANK));
            }
            this.level().addFreshEntity(phantom);
            return phantom;
        }
        return null;
    }

    public void doBloodMoon(BuildingPlacement bpl) {
        if (level().isClientSide())
            return;
        if (TimeServerEvents.isBloodMoonActive())
            return;

        int soulRank = consumeSoulsAndGetSoulRank();
        int bonusDuration = BloodMoon.BONUS_DURATION_PER_SOUL_RANK * soulRank;

        TimeServerEvents.startBloodMoon(BloodMoon.DURATION + bonusDuration, this, bpl.centrePos);
        AbilityClientboundPacket.doAbility(this.getId(), UnitAction.BLOOD_MOON, BloodMoon.DURATION + bonusDuration, bpl.centrePos);
    }
}
