package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.ability.abilities.PromoteIllager;
import com.solegendary.reignofnether.ability.heroAbilities.enchanter.*;
import com.solegendary.reignofnether.building.RangeIndicator;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.hero.HeroClientboundPacket;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.registrars.ParticleRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.*;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.modelling.animations.EnchanterAnimations;
import com.solegendary.reignofnether.unit.modelling.renderers.EnchanterRenderer;
import com.solegendary.reignofnether.unit.modelling.renderers.RoyalGuardRenderer;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import oshi.util.tuples.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnchanterUnit extends Vindicator implements AttackerUnit, HeroUnit, KeyframeAnimated, RangeIndicator {
    public final Abilities ABILITIES = new Abilities(
        List.of(
            new Pair<>(new CivilEnchantment(), Keybindings.keyQ),
            new Pair<>(new MartialEnchantment(), Keybindings.keyW),
            new Pair<>(new ProtectiveEnchantment(), Keybindings.keyE),
            new Pair<>(new MarchOfProgress(), Keybindings.keyR)
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

    public Faction getFaction() {return Faction.VILLAGERS;}
    public Abilities getAbilities() {return abilities;}
    public List<ItemStack> getItems() {return items;};
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public Goal getAttackBuildingGoal() {return attackBuildingGoal;}
    public Goal getAttackGoal() {return attackGoal;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}

    private EnemySearchBehaviour attackSearchBehaviour = EnemySearchBehaviour.NONE;
    public EnemySearchBehaviour getEnemySearchBehaviour() { return attackSearchBehaviour; }
    public void setEnemySearchBehaviour(EnemySearchBehaviour behaviour) { attackSearchBehaviour = behaviour; }

    private GenericTargetedSpellGoal castEnchantCivilGoal;
    public GenericTargetedSpellGoal getCastEnchantCivilGoal() { return castEnchantCivilGoal; }
    private GenericTargetedSpellGoal castEnchantMilitaryGoal;
    public GenericTargetedSpellGoal getCastEnchantMartialGoal() { return castEnchantMilitaryGoal; }
    private GenericTargetedSpellGoal castEnchantProtectiveGoal;
    public GenericTargetedSpellGoal getCastEnchantProtectiveGoal() { return castEnchantProtectiveGoal; }
    private GenericUntargetedSpellGoal castAuraGoal;
    public GenericUntargetedSpellGoal getCastAuraGoal() { return castAuraGoal; }

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
            SynchedEntityData.defineId(EnchanterUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(EnchanterUnit.class, EntityDataSerializers.INT);

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
    public ResourceCost getCost() {return ResourceCosts.ENCHANTER;}
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
    private float manaRegenPerSecond = 1f;
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
    final static public float attacksPerSecond = 0.5f;
    final static public float maxHealth = 100.0f;
    final static public float maxHealthBonusPerLevel = 10.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.25f;
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

    private float ageInTicksOffset = 0;
    public float getAgeInTicksOffset() { return ageInTicksOffset; }
    public void setAgeInTicksOffset(float ticks) { ageInTicksOffset = ticks; }

    @Override
    public int getAttackWindupTicks() {
        return 12;
    }

    private static final double KNOCKBACK_RESISTANCE = 0.5d;
    private static final float AUTOCAST_ENCHANT_RANGE = 15;

    public boolean auraEnabled = false;

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
                activeAnimDef = EnchanterAnimations.ATTACK;
                activeAnimState = attackAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CAST_SPELL -> {
                activeAnimDef = EnchanterAnimations.SPELL_FAST;
                activeAnimState = spellChargeAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case ULTIMATE -> {
                if (auraEnabled) {
                    activeAnimDef = EnchanterAnimations.ULTIMATE;
                    activeAnimState = spellActivateAnimState;
                    animateScale = 1.0f;
                    startAnimation(activeAnimDef);
                }
            }
            default -> animateScaleReducing = true;
        }
    }

    public EnchanterUnit(EntityType<? extends Vindicator> entityType, Level level) {
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
    public void setStatsForLevel(boolean heal) {
        AttributeInstance aiMaxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        float newHealth = getBaseHealth() + ((getHeroLevel() - 1) * getHealthBonusPerLevel());
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

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, EnchanterUnit.movementSpeed)
                .add(Attributes.ATTACK_DAMAGE, EnchanterUnit.attackDamage)
                .add(Attributes.ARMOR, EnchanterUnit.armorValue)
                .add(Attributes.MAX_HEALTH, EnchanterUnit.maxHealth)
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
        this.castEnchantMilitaryGoal.tick();
        this.castEnchantCivilGoal.tick();
        this.castEnchantProtectiveGoal.tick();
        this.castAuraGoal.tick();

        BlockPos targetBp = castEnchantMilitaryGoal.getCastTarget();
        if (targetBp != null && distanceToSqr(Vec3.atCenterOf(targetBp)) < 3) {
            double x0 = targetBp.getX() - this.getX();
            double z0 = targetBp.getZ() - this.getZ();
            float f = (float) (Mth.atan2(z0, x0) * 57.2957763671875) - 90.0F;
            this.setYRot(this.rotlerp(this.getYRot(), f, 10f));
        } else if (getTarget() != null && distanceToSqr(getTarget().position()) < 3) {
            double x0 = getTarget().getX() - this.getX();
            double z0 = getTarget().getZ() - this.getZ();
            float f = (float) (Mth.atan2(z0, x0) * 57.2957763671875) - 90.0F;
            this.setYRot(this.rotlerp(this.getYRot(), f, 10f));
        }

        if (tickCount % 30 == 0) {
            doAutocastEnchant();
        }
        if (auraEnabled && tickCount % 20 == 0) {
            setMana(getMana() - MarchOfProgress.MANA_COST_PER_SECOND);
            if (getMana() <= 0) {
                auraEnabled = false;
                if (!level().isClientSide)
                    SoundClientboundPacket.playSoundAtPos(SoundAction.BEACON_DEACTIVATE, blockPosition(), 1.5f);
            } else {
                if (!level().isClientSide) {
                    SoundClientboundPacket.playSoundAtPos(SoundAction.BEACON_AMBIENT, blockPosition(), 1.5f);
                    for (Mob mob : MiscUtil.getEntitiesWithinRange(position(), MarchOfProgress.RADIUS, Mob.class, level())) {
                        if (UnitServerEvents.getUnitToEntityRelationship(this, mob) == Relationship.FRIENDLY &&
                            mob instanceof Unit unit && unit.hasAnyEnchants()) {
                            mob.addEffect(new MobEffectInstance(MobEffectRegistrar.ENCHANTMENT_AMPLIFIER.get(), 30, 0, true, false));
                        }
                    }
                }
            }
        }

        if (auraEnabled && level().isClientSide && tickCount % 20 == 0) {
            List<Mob> mobs = MiscUtil.getEntitiesWithinRange(position(), MarchOfProgress.RADIUS, Mob.class, level());
            for (Mob mob : mobs) {
                if (mob.hasEffect(MobEffectRegistrar.ENCHANTMENT_AMPLIFIER.get()) ||
                    mob.hasEffect(MobEffectRegistrar.TEMPORARY_EFFICIENCY.get())) {
                    level().addParticle(
                            ParticleRegistrar.BIG_ENCHANT.get(),
                            mob.position().x,
                            mob.position().y + 2.0f,
                            mob.position().z,
                            position().x - mob.position().x,
                            position().y - mob.position().y,
                            position().z - mob.position().z
                    );
                }
            }
        }
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

    @Override public void updateHighlightBps() {
        if (level().isClientSide()) {
            highlightBps.clear();
            for (Ability ability : getAbilities().get()) {
                if (CursorClientEvents.getLeftClickAction() == ability.action) {
                    setHighlightBps(MiscUtil.getRangeIndicatorCircleBlocks(blockPosition(),
                            (int) (ability.range - 1),
                            level()
                    ));
                }
            }
            if (auraEnabled) {
                int radius = MarchOfProgress.RADIUS;
                this.highlightBps.addAll(MiscUtil.getRangeIndicatorCircleBlocks(blockPosition(),
                        radius - 1,
                        level()
                ));
            }
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

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.garrisonGoal = new GarrisonGoal(this);
        this.attackGoal = new MeleeWindupAttackUnitGoal(this, false);
        this.attackBuildingGoal = new MeleeWindupAttackBuildingGoal(this);
        this.returnResourcesGoal = new ReturnResourcesGoal(this);
        this.castEnchantCivilGoal = new GenericTargetedSpellGoal(
                this,
                0,
                10,
                UnitAnimationAction.CAST_SPELL,
                this::enchantCivilian,
                null,
                null
        );
        this.castEnchantMilitaryGoal = new GenericTargetedSpellGoal(
                this,
                0,
                10,
                UnitAnimationAction.CAST_SPELL,
                this::enchantMilitary,
                null,
                null
        );
        this.castEnchantProtectiveGoal = new GenericTargetedSpellGoal(
                this,
                0,
                10,
                UnitAnimationAction.CAST_SPELL,
                this::enchantArmour,
                null,
                null
        );
        this.castAuraGoal = new GenericUntargetedSpellGoal(
                this,
                0,
                this::toggleAura,
                null,
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
        this.castEnchantMilitaryGoal.stop();
        this.castEnchantCivilGoal.stop();
        this.castAuraGoal.stop();
    }

    private void playEnchantSound() {
        SoundClientboundPacket.playSoundAtPos(SoundAction.ENCHANT, blockPosition());
    }

    private void doAutocastEnchant() {
        if (!isIdle()) {
            return;
        }
        for (Ability ability : abilities.get()) {
            if (ability instanceof AbstractEnchantment enchantAbility &&
                    enchantAbility.isAutocasting(this) &&
                    enchantAbility.getRank(this) > 0 &&
                    enchantAbility.manaCost < getMana() &&
                    enchantAbility.isOffCooldown(this)) {

                for (Mob mob : MiscUtil.getEntitiesWithinRange(this.position(), AUTOCAST_ENCHANT_RANGE, Mob.class, level())) {
                    if (enchantAbility.canEnchant(mob) && mob instanceof Unit unit && unit.getOwnerName().equals(getOwnerName())) {
                        enchantAbility.use(level(), this, mob);
                    }
                }
            }
        }
    }

    public void enchantCivilian(LivingEntity entity) {
        if (level().isClientSide) return;

        if (entity.getMainHandItem().getItem() != Items.AIR)
            entity.getMainHandItem().enchant(Enchantments.BLOCK_EFFICIENCY, 1);
        entity.addEffect(new MobEffectInstance(MobEffectRegistrar.TEMPORARY_EFFICIENCY.get(), CivilEnchantment.DURATION_SECONDS * 20));
        playEnchantSound();
    }

    public void enchantMilitary(LivingEntity entity) {
        Enchantment enchantment = MartialEnchantment.getEnchantmentForUnit(entity);
        if (enchantment != null) {
            entity.getMainHandItem().enchant(enchantment, 1);
            if (entity instanceof MilitiaUnit militiaUnit) {
                if (militiaUnit.isUsingBow())
                    militiaUnit.bowEnchanted = true;
                else
                    militiaUnit.swordEnchanted = true;
            }
        }
        if (!level().isClientSide)
            playEnchantSound();
    }

    public void enchantArmour(LivingEntity entity) {
        if (level().isClientSide) return;

        entity.getItemBySlot(EquipmentSlot.CHEST).enchant(EnchantmentRegistrar.FORTYIFYING.get(), 1);
        playEnchantSound();
    }

    public void toggleAura() {
        auraEnabled = !auraEnabled;
        if (level().isClientSide) {
            updateHighlightBps();
        } else {
            if (auraEnabled) {
                AbilityClientboundPacket.doAbility(getId(), UnitAction.MARCH_OF_PROGRESS_SET, 1f);
                SoundClientboundPacket.playSoundAtPos(SoundAction.BEACON_ACTIVATE, blockPosition(), 1.5f);
            } else {
                AbilityClientboundPacket.doAbility(getId(), UnitAction.MARCH_OF_PROGRESS_SET, 0f);
                SoundClientboundPacket.playSoundAtPos(SoundAction.BEACON_DEACTIVATE, blockPosition(), 1.5f);
            }
        }
    }

    @Override
    public AABB getInflatedSelectionBox() {
        AABB aabb = this.getBoundingBox().inflate(0.15f, 0, 0.15f);
        aabb.setMaxY(aabb.maxY + 0.4875f);
        return aabb;
    }
}
