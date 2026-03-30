package com.solegendary.reignofnether.unit.units.piglins;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.abilities.Bloodlust;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.piglins.BasaltSprings;
import com.solegendary.reignofnether.building.buildings.piglins.FlameSanctuary;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.*;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.modelling.animations.MarauderAnimations;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MarauderUnit extends PiglinBrute implements Unit, AttackerUnit, KeyframeAnimated {
    public static final Abilities ABILITIES = new Abilities();
    static {
        ABILITIES.add(new Bloodlust(), Keybindings.keyQ);
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

    public Faction getFaction() {return Faction.PIGLINS;}
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
            SynchedEntityData.defineId(MarauderUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(MarauderUnit.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
        this.entityData.define(scenarioRoleDataAccessor, -1);
    }

    // combat stats
    public float getMovementSpeed() {return movementSpeed;}
    public float getUnitMaxHealth() {return maxHealth;}

    @Nullable
    public ResourceCost getCost() {return ResourceCosts.MARAUDER;}
    public boolean getWillRetaliate() {return willRetaliate;}
    public float getAttacksPerSecond() {return 20f / getAttackCooldown();}
    public float getBaseAttacksPerSecond() {return attacksPerSecond;}
    public float getAggroRange() {return aggroRange;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle && !isVehicle();}
    public float getAttackRange() {return attackRange;}
    public float getUnitAttackDamage() {
        if (useCleavingHitDamage) {
            return cleavingHitDamage;
        }
        return isNextHitBig() ? bigHitDamage : attackDamage;}
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

    public float getAttackCooldown() {return ((20 / attacksPerSecond) * getAttackCooldownMultiplier());}

    final static public float attackDamage = 7.0f;
    final static public float bigHitDamage = 10.0f;
    final static public float cleavingHitDamage = 5f;
    final static public float attacksPerSecond = 0.4f;
    final static public float attackRange = 2; // only used by ranged units or melee building attackers
    final static public float aggroRange = 10;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;
    final static public float maxHealth = 140.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.28f;
    final static public float rangedDamageResist = 0.0f;

    public int maxResources = 100;

    private final int ATTACKS_TO_BIG_HIT_MAX = 2;
    public int attacksToNextBigHit = 2;
    private boolean useCleavingHitDamage = false;

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
        return hasEffectWithDuration(MobEffectRegistrar.BLOODLUST.get()) ? 10 : 16;
    }

    @Override
    public float getAnimationSpeed() {
        return hasEffectWithDuration(MobEffectRegistrar.BLOODLUST.get()) ? 2.0f : 1.2f;
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
    public boolean animateScaleReducing = false;
    public void setAnimateTicksLeft(int ticks) { animateTicks = ticks; }
    public int getAnimateTicksLeft() { return animateTicks; }

    public void playSingleAnimation(UnitAnimationAction animAction) {
        animateScaleReducing = false;
        switch (animAction) {
            case ATTACK_UNIT, ATTACK_BUILDING -> {
                activeAnimDef = isNextHitBig() ? MarauderAnimations.ATTACK_SWING : MarauderAnimations.ATTACK_SLAM;
                activeAnimState = attackAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            default -> animateScaleReducing = true;
        }
    }

    public MarauderUnit(EntityType<? extends PiglinBrute> entityType, Level level) {
        super(entityType, level);
        updateAbilityButtons();
    }

    private boolean isNextHitBig() {
        return attacksToNextBigHit == 0;
    }

    @Override
    public double getUnitRangedArmorPercentage() {
        return rangedDamageResist;
    }

    @Override
    protected boolean onSoulSpeedBlock() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, MarauderUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, MarauderUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, MarauderUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, MarauderUnit.armorValue)
                .add(Attributes.ATTACK_KNOCKBACK, 0f)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.66f);
    }

    @Override
    public boolean isLeftHanded() { return false; }
    @Override // prevent vanilla logic for picking up items
    protected void pickUpItem(ItemEntity pItemEntity) { }
    @Override
    public boolean isConverting() { return false; }
    @Override
    protected void customServerAiStep() { }
    @Override
    public LivingEntity getTarget() {
        return this.targetGoal.getTarget();
    }

    public void tick() {
        this.setCanPickUpLoot(true);
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);

        if (level().isClientSide() && animateTicks > 0) {
            animateTicks -= 1;
        }
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity pEntity) {
        boolean result;
        if (isNextHitBig()) {
            this.getAttribute(Attributes.ATTACK_KNOCKBACK).addTransientModifier(new AttributeModifier("knockback", 1.5f, AttributeModifier.Operation.ADDITION));
            result = super.doHurtTarget(pEntity);
            if (pEntity instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffectRegistrar.STUN.get(), 40));
            }
            this.getAttribute(Attributes.ATTACK_KNOCKBACK).removeModifiers();
            decrementAttacks();

            if (!level().isClientSide() && ResearchServerEvents.playerHasResearch(getOwnerName(), ProductionItems.RESEARCH_CLEAVING_FLAILS)) {
                this.useCleavingHitDamage = true;
                List<Mob> nearbyMobs = MiscUtil.getEntitiesWithinRange(new Vector3d(
                            pEntity.position().x,
                            pEntity.position().y,
                            pEntity.position().z
                        ), 2, Mob.class, level()
                );
                List<Mob> closestMobs = nearbyMobs.stream().sorted(Comparator.comparing(m -> m.distanceToSqr(position()))).toList();
                int extraHitsLeft = 2;
                for (Mob mob : closestMobs) {
                    if (UnitServerEvents.getUnitToEntityRelationship(this, mob) != Relationship.FRIENDLY && mob.getId() != pEntity.getId()) {
                        super.doHurtTarget(mob);
                        mob.addEffect(new MobEffectInstance(MobEffectRegistrar.STUN.get(), 20));
                        extraHitsLeft -= 1;
                        if (extraHitsLeft <= 0) {
                            break;
                        }
                    }
                }
                this.useCleavingHitDamage = false;
            }
        } else {
            result = super.doHurtTarget(pEntity);
            decrementAttacks();
        }
        return result;
    }

    public void decrementAttacks() {
        if (isNextHitBig())
            attacksToNextBigHit = ATTACKS_TO_BIG_HIT_MAX;
        else
            attacksToNextBigHit -= 1;
        if (!level().isClientSide())
            AbilityClientboundPacket.doAbility(getId(), UnitAction.SET_ATTACK_COUNT, attacksToNextBigHit);
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
    public void setupEquipmentAndUpgradesClient() {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.AIR));
    }

    @Override
    public void setupEquipmentAndUpgradesServer() {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.AIR));
    }

    @Override
    public boolean fireImmune() {
        BuildingPlacement bpl = BuildingUtils.findBuilding(level().isClientSide(), getOnPos());
        return super.fireImmune() || (bpl != null && (bpl.getBuilding() instanceof FlameSanctuary || bpl.getBuilding() instanceof BasaltSprings));
    }

    public boolean hasNetheriteChestplate() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.CHEST);
        return itemStack.getItem() == Items.NETHERITE_CHESTPLATE;
    }

    @Override
    public boolean canPickUpEquipment(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return item == Items.NETHERITE_CHESTPLATE &&
                !hasItemInSlot(EquipmentSlot.CHEST);
    }

    @Override
    public void onPickupEquipment(ItemStack itemStack) {
        setItemSlot(getEquipmentSlotForItem(itemStack), itemStack);
    }

    @Override
    public AABB getInflatedSelectionBox() {
        AABB aabb = this.getBoundingBox().inflate(0.5f, 0, 0.5f);
        aabb.setMaxY(aabb.maxY + 0.8f);
        return aabb;
    }

    @Override
    public float getBonusMeleeRange() {
        return isNextHitBig() ? 1.2f : 0.6f;
    }

    @Override
    public float getBonusMeleeRangeForAttackers() {
        return 0.4f;
    }

    @Override
    public boolean hasBonusDamage() {
        return isNextHitBig();
    }
}
