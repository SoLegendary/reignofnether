package com.solegendary.reignofnether.unit.units.piglins;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.WitherCloud;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.piglins.BasaltSprings;
import com.solegendary.reignofnether.building.buildings.piglins.FlameSanctuary;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WitherSkeletonUnit extends WitherSkeleton implements Unit, AttackerUnit {
    public static final Abilities ABILITIES = new Abilities();
    static {
        ABILITIES.add(new WitherCloud(), Keybindings.keyQ);
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
            SynchedEntityData.defineId(WitherSkeletonUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(WitherSkeletonUnit.class, EntityDataSerializers.INT);

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
    public ResourceCost getCost() {return ResourceCosts.WITHER_SKELETON;}
    public boolean getWillRetaliate() {return willRetaliate;}
    public float getAttackCooldown() {return ((20 / attacksPerSecond) * getAttackCooldownMultiplier());}
    public float getAttacksPerSecond() {return 20f / getAttackCooldown();}
    public float getBaseAttacksPerSecond() {return attacksPerSecond;}
    public float getAggroRange() {return aggroRange;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle && !isVehicle();}
    public float getAttackRange() {return attackRange;}
    public float getUnitAttackDamage() {return attackDamage;}
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

    final static public float attackDamage = 3.0f;
    final static public float attacksPerSecond = 0.3f;
    final static public float attackRange = 2; // only used by ranged units or melee building attackers
    final static public float aggroRange = 10;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;

    final static public float maxHealth = 100.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.28f;
    public int maxResources = 100;

    public int deathCloudTicks = 0;

    private Abilities abilities = ABILITIES.clone();
    private final List<ItemStack> items = new ArrayList<>();

    public WitherSkeletonUnit(EntityType<? extends WitherSkeleton> entityType, Level level) {
        super(entityType, level);

        updateAbilityButtons();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        SpawnGroupData spawnGroupData = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(attackDamage);
        return spawnGroupData;
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, WitherSkeletonUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, WitherSkeletonUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, WitherSkeletonUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ATTACK_KNOCKBACK, 0.5f)
                .add(Attributes.ARMOR, WitherSkeletonUnit.armorValue)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5f);
    }

    @Override
    public void setRemainingFireTicks(int pRemainingFireTicks) {
        if (!level().isClientSide()) {
            boolean hasImmunityResearch = ResearchServerEvents.playerHasResearch(getOwnerName(), ProductionItems.RESEARCH_FIRE_RESISTANCE);
            if (hasImmunityResearch && !hasEffect(MobEffectRegistrar.SOULS_AFLAME.get()))
                pRemainingFireTicks = 0;
        }
        super.setRemainingFireTicks(pRemainingFireTicks);
    }

    @Override
    public void reassessWeaponGoal() { }
    @Override
    public boolean isSunBurnTick() { return false; }
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

    public void tick() {
        this.setCanPickUpLoot(true);
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);

        if (!level().isClientSide() && deathCloudTicks > 0 && deathCloudTicks % 20 == 0) {
            AreaEffectCloud aec = new AreaEffectCloud(level(), getX(), getY(), getZ());
            aec.setOwner(this);
            aec.setRadius(4.0F);
            aec.setRadiusOnUse(0);
            aec.setDurationOnUse(0);
            aec.setDuration(2 * 20); // cloud duration
            aec.setRadiusPerTick(-aec.getRadius() / (float)aec.getDuration());
            aec.addEffect(new MobEffectInstance(MobEffects.WITHER, 2 * 20, 1));
            level().addFreshEntity(aec);
        }
        if (deathCloudTicks > 0)
            deathCloudTicks -= 1;
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
        this.attackGoal = new MeleeAttackUnitGoal(this, false);
        this.returnResourcesGoal = new ReturnResourcesGoal(this);
        this.attackBuildingGoal = new MeleeAttackBuildingGoal(this);
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
        this.goalSelector.addGoal(3, moveGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundUnitGoal(this));
    }

    @Override
    public void setupEquipmentAndUpgradesClient() {

    }

    @Override
    public void setupEquipmentAndUpgradesServer() {
        ItemStack swordStack = new ItemStack(Items.NETHERITE_SWORD);
        AttributeModifier mod = new AttributeModifier(UUID.randomUUID().toString(), 0, AttributeModifier.Operation.ADDITION);
        swordStack.addAttributeModifier(Attributes.ATTACK_DAMAGE, mod, EquipmentSlot.MAINHAND);
        this.setItemSlot(EquipmentSlot.MAINHAND, swordStack);
    }

    public boolean hasNetheriteChestplate() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.CHEST);
        return itemStack.getItem() == Items.NETHERITE_CHESTPLATE;
    }

    public static final int WITHER_SECONDS = 6;
    public static final int WITHER_MAX_AMPLIFIER = 5; // amplifier starts at 0
    public static final int WITHER_MAX_AMPLIFIER_HERO = 3;

    public static void applyStackingWither(LivingEntity le) {
        int amplifier = 0;
        MobEffectInstance witherEffect = null;
        for (MobEffectInstance effect : (le).getActiveEffects())
            if (effect.getEffect() == MobEffects.WITHER)
                witherEffect = effect;

        if (witherEffect != null) {
            int maxAmp = WITHER_MAX_AMPLIFIER;
            if (le instanceof HeroUnit heroUnit)
                maxAmp = WITHER_MAX_AMPLIFIER_HERO;
            amplifier = Math.min(maxAmp, witherEffect.getAmplifier() + 1);
            le.removeEffect(MobEffects.WITHER);
        }
        le.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_SECONDS * 20, amplifier), null);
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity pEntity) {
        if (!super.doHurtTarget(pEntity)) {
            return false;
        } else {
            if (pEntity instanceof LivingEntity le)
                applyStackingWither(le);
            return true;
        }
    }

    @Override
    public boolean fireImmune() {
        BuildingPlacement bpl = BuildingUtils.findBuilding(level().isClientSide(), getOnPos());
        return super.fireImmune() || (bpl != null && (bpl.getBuilding() instanceof FlameSanctuary || bpl.getBuilding() instanceof BasaltSprings));
    }

    @Override
    public boolean canPickUpEquipment(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return (item == Items.GOLDEN_CHESTPLATE ||
                item == Items.GOLDEN_LEGGINGS ||
                item == Items.GOLDEN_BOOTS ||
                item == Items.GOLDEN_HELMET ||
                item == Items.NETHERITE_CHESTPLATE ||
                item == Items.NETHERITE_LEGGINGS ||
                item == Items.NETHERITE_BOOTS ||
                item == Items.NETHERITE_HELMET) &&
                getItemBySlot(getEquipmentSlotForItem(itemStack)).getItem() != item;
    }

    @Override
    public void onPickupEquipment(ItemStack itemStack) {
        setItemSlot(getEquipmentSlotForItem(itemStack), itemStack);
    }




}
