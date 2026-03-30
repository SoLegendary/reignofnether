package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.BackToWorkUnit;
import com.solegendary.reignofnether.ability.abilities.PromoteIllager;
import com.solegendary.reignofnether.ability.abilities.WeaponSwapBow;
import com.solegendary.reignofnether.ability.abilities.WeaponSwapSword;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.TargetResourcesSave;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.ConvertableUnit;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import com.solegendary.reignofnether.unit.packets.UnitConvertClientboundPacket;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.solegendary.reignofnether.survival.SurvivalServerEvents.ENEMY_OWNER_NAME;

public class MilitiaUnit extends Vindicator implements Unit, AttackerUnit, RangedAttackerUnit, VillagerDataHolder, ConvertableUnit {
    public static final Abilities ABILITIES = new Abilities();
    static {
        ABILITIES.add(new BackToWorkUnit(), Keybindings.build);
        ABILITIES.add(new WeaponSwapBow(), Keybindings.keyQ);
        ABILITIES.add(new WeaponSwapSword(), Keybindings.keyQ);
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
    public Abilities getAbilities() {return abilities;}
    public List<ItemStack> getItems() {return items;};
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public BuildRepairGoal getBuildRepairGoal() {return null;}
    public GatherResourcesGoal getGatherResourceGoal() {return null;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}

    private MoveToTargetBlockGoal moveGoal;
    private SelectedTargetGoal<? extends LivingEntity> targetGoal;
    private ReturnResourcesGoal returnResourcesGoal;
    private AbstractMeleeAttackUnitGoal attackGoal;
    private UnitBowAttackGoal<? extends LivingEntity> rangedAttackGoal;
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
            SynchedEntityData.defineId(MilitiaUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(MilitiaUnit.class, EntityDataSerializers.INT);

    // combat stats
    public float getMovementSpeed() {return isUsingBow() ? rangedMovementSpeed : movementSpeed;}
    public float getUnitMaxHealth() {return maxHealth;}

    public ResourceCost getCost() {return ResourceCosts.MILITIA;}
    public boolean getWillRetaliate() {return willRetaliate;}
    public float getAttackCooldown() {return ((20 / (isUsingBow() ? rangedAttacksPerSecond : attacksPerSecond)) * getAttackCooldownMultiplier());}
    public float getAttacksPerSecond() {return 20f / getAttackCooldown();}
    public float getBaseAttacksPerSecond() {return isUsingBow() ? rangedAttacksPerSecond : attacksPerSecond;}
    public float getAggroRange() {return aggroRange;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle && !isVehicle();}
    public float getAttackRange() {return isUsingBow() ? attackRange : 2;}
    public float getUnitAttackDamage() {
        if (isUsingBow()) {
            return rangedAttackDamage + getPowerLevel();
        } else {
            return attackDamage + getSharpnessLevel();
        }
    }
    public BlockPos getAttackMoveTarget() { return attackMoveTarget; }
    public boolean canAttackBuildings() {return getAttackBuildingGoal() != null && !isUsingBow();}
    public Goal getAttackGoal() { return isUsingBow() ? rangedAttackGoal : attackGoal; }
    public Goal getAttackBuildingGoal() { return attackBuildingGoal; }
    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    private EnemySearchBehaviour attackSearchBehaviour = EnemySearchBehaviour.NONE;
    public EnemySearchBehaviour getEnemySearchBehaviour() { return attackSearchBehaviour; }
    public void setEnemySearchBehaviour(EnemySearchBehaviour behaviour) { attackSearchBehaviour = behaviour; }

    // ConvertableUnit
    public boolean converted = false;
    private boolean shouldDiscard = false;
    public boolean shouldDiscard() { return shouldDiscard; }
    public void setShouldDiscard(boolean discard) { this.shouldDiscard = discard; }

    public int fogRevealDuration = 0; // set > 0 for the client who is attacked by this unit
    public int getFogRevealDuration() { return fogRevealDuration; }
    public void setFogRevealDuration(int duration) { fogRevealDuration = duration; }

    // endregion

    // for going back to work as a villager
    public TargetResourcesSave resourcesSaveData = null;
    public VillagerProfession profession = VillagerProfession.NONE;
    public boolean isVeteran = false;
    public int farmerExp = 0;
    public int lumberjackExp = 0;
    public int minerExp = 0;
    public int masonExp = 0;
    public int hunterExp = 0;
    public boolean swordEnchanted = false;
    public boolean bowEnchanted = false;

    final static public float attackDamage = 3.0f;
    final static public float rangedAttackDamage = 3.0f;
    final static public float attacksPerSecond = 0.5f;
    final static public float rangedAttacksPerSecond = 0.3f;
    final static public float attackRange = 10; // only used by ranged units or melee building attackers
    final static public float aggroRange = 10;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;

    final static public float maxHealth = 30.0f;
    final static public float armorValue = 5.5f;
    final static public float rangedArmorValue = 0.0f;
    final static public float movementSpeed = 0.28f;
    final static public float rangedMovementSpeed = 0.25f;
    public int maxResources = 100;

    public boolean isCaptain = false;

    private Abilities abilities = ABILITIES.clone();
    private final List<ItemStack> items = new ArrayList<>();

    public MilitiaUnit(EntityType<? extends Vindicator> entityType, Level level) {
        super(entityType, level);
        updateAbilityButtons();
    }

    public boolean isUsingBow() {
        return getItemBySlot(EquipmentSlot.MAINHAND).is(Items.BOW);
    }

    public void swapWeapons(boolean useBow) {
        Item weapon = useBow ? Items.BOW : Items.STONE_SWORD;
        int damageMod = 0;
        ItemStack weaponStack = new ItemStack(weapon);
        if (weapon == Items.STONE_SWORD && swordEnchanted) {
            weaponStack.enchant(Enchantments.SHARPNESS, 1);
        } else if (weapon == Items.BOW && bowEnchanted) {
            weaponStack.enchant(Enchantments.POWER_ARROWS, 1);
        }
        AttributeModifier mod = new AttributeModifier(UUID.randomUUID().toString(), damageMod, AttributeModifier.Operation.ADDITION);
        weaponStack.addAttributeModifier(Attributes.ATTACK_DAMAGE, mod, EquipmentSlot.MAINHAND);
        this.setItemSlot(EquipmentSlot.MAINHAND, weaponStack);
        AttributeInstance ai1 = getAttribute(Attributes.ATTACK_DAMAGE);
        if (ai1 != null)
            ai1.setBaseValue(useBow ? rangedAttackDamage : attackDamage);
        AttributeInstance ai2 = getAttribute(Attributes.MOVEMENT_SPEED);
        if (ai2 != null)
            ai2.setBaseValue(useBow ? rangedMovementSpeed : movementSpeed);
        AttributeInstance ai3 = getAttribute(Attributes.ARMOR);
        if (ai3 != null)
            ai3.setBaseValue(useBow ? rangedArmorValue : armorValue);

        if (rangedAttackGoal != null && attackGoal != null) {
            if (useBow) {
                this.goalSelector.removeGoal(attackGoal);
                this.goalSelector.addGoal(2, rangedAttackGoal);
            } else {
                this.goalSelector.removeGoal(rangedAttackGoal);
                this.goalSelector.addGoal(2, attackGoal);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    // all for animation syncing...
    @Override
    public void setUnitAttackTarget(@Nullable LivingEntity target) {
        AttackerUnit.super.setUnitAttackTarget(target);
        if (!this.level().isClientSide()) {
            if (target != null)
                UnitAnimationClientboundPacket.sendEntityPacket(UnitAnimationAction.NON_KEYFRAME_START, this, target);
            else
                UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.NON_KEYFRAME_STOP, this);
        }
    }
    @Override
    public void setAttackBuildingTarget(BlockPos preselectedBlockPos) {
        AttackerUnit.super.setAttackBuildingTarget(preselectedBlockPos);
        if (!this.level().isClientSide())
            UnitAnimationClientboundPacket.sendBlockPosPacket(UnitAnimationAction.NON_KEYFRAME_START, this, preselectedBlockPos);
    }
    @Override
    public void resetBehaviours() {
        if (!this.level().isClientSide())
            UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.NON_KEYFRAME_STOP, this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, MilitiaUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, MilitiaUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, MilitiaUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, MilitiaUnit.armorValue);
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.VILLAGER_AMBIENT;}
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.VILLAGER_DEATH; }
    @Override
    protected SoundEvent getHurtSound(DamageSource p_34103_) { return SoundEvents.VILLAGER_HURT; }
    @Override
    public boolean isLeftHanded() { return false; }
    @Override // prevent vanilla logic for picking up items
    protected void pickUpItem(ItemEntity pItemEntity) { }

    public void tick() {
        if (shouldDiscard)
            this.discard();
        else {
            this.setCanPickUpLoot(true);
            super.tick();
            Unit.tick(this);
            AttackerUnit.tick(this);
            PromoteIllager.checkAndApplyBuff(this);

            this.isCaptain = getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof BannerItem;

            if (!this.isCaptain && this.tickCount > 100 && this.tickCount % 10 == 0 && !converted &&
                !level().isClientSide() && !getOwnerName().equals(ENEMY_OWNER_NAME)) {

                BuildingPlacement building = BuildingUtils.findClosestBuilding(level().isClientSide(), this.getEyePosition(),
                        (b) -> b.isBuilt && b.ownerName.equals(getOwnerName()) && b.getBuilding() instanceof TownCentre);

                int range = TownCentre.MILITIA_RANGE;
                if (building != null &&
                    distanceToSqr(building.centrePos.getX(), building.centrePos.getY(), building.centrePos.getZ()) > range * range) {
                    convertToVillager();
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("farmerExp", this.farmerExp);
        pCompound.putInt("lumberjackExp", this.lumberjackExp);
        pCompound.putInt("minerExp", this.minerExp);
        pCompound.putInt("masonExp", this.masonExp);
        pCompound.putInt("hunterExp", this.hunterExp);
        pCompound.putBoolean("isVeteran", this.isVeteran);
        pCompound.putBoolean("swordEnchanted", this.swordEnchanted);
        pCompound.putBoolean("bowEnchanted", this.bowEnchanted);
        this.addUnitSaveData(pCompound);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("farmerExp"))
            this.farmerExp = pCompound.getInt("farmerExp");
        if (pCompound.contains("lumberjackExp"))
            this.lumberjackExp = pCompound.getInt("lumberjackExp");
        if (pCompound.contains("minerExp"))
            this.minerExp = pCompound.getInt("minerExp");
        if (pCompound.contains("masonExp"))
            this.masonExp = pCompound.getInt("masonExp");
        if (pCompound.contains("hunterExp"))
            this.hunterExp = pCompound.getInt("hunterExp");
        if (pCompound.contains("isVeteran"))
            this.isVeteran = pCompound.getBoolean("isVeteran");
        if (pCompound.contains("swordEnchanted"))
            this.swordEnchanted = pCompound.getBoolean("swordEnchanted");
        if (pCompound.contains("bowEnchanted"))
            this.bowEnchanted = pCompound.getBoolean("bowEnchanted");
        this.readUnitSaveData(pCompound);
    }

    public void convertToVillager() {
        if (!converted) {
            LivingEntity newEntity = this.convertToUnit(EntityRegistrar.VILLAGER_UNIT.get());
            if (newEntity instanceof VillagerUnit vUnit) {
                if (resourcesSaveData != null) {
                    vUnit.getGatherResourceGoal().saveData = resourcesSaveData;
                    vUnit.getGatherResourceGoal().loadState();
                }
                vUnit.setProfession(this.profession);
                vUnit.isVeteran = this.isVeteran;
                vUnit.farmerExp = this.farmerExp;
                vUnit.lumberjackExp = this.lumberjackExp;
                vUnit.minerExp = this.minerExp;
                vUnit.masonExp = this.masonExp;
                vUnit.hunterExp = this.hunterExp;
                vUnit.chestplate = this.getItemBySlot(EquipmentSlot.CHEST).getItem();
                vUnit.chestplateEnchanted = this.getItemBySlot(EquipmentSlot.CHEST).isEnchanted();
                vUnit.swordEnchanted = this.swordEnchanted;
                vUnit.bowEnchanted = this.bowEnchanted;

                UnitConvertClientboundPacket.syncConvertedUnits(getOwnerName(), List.of(getId()), List.of(newEntity.getId()));
                converted = true;
            }
        }
    }

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.garrisonGoal = new GarrisonGoal(this);
        this.attackGoal = new MeleeAttackUnitGoal(this, true);
        this.rangedAttackGoal = new UnitBowAttackGoal<>(this);
        this.attackBuildingGoal = new MeleeAttackBuildingGoal(this);
        this.returnResourcesGoal = new ReturnResourcesGoal(this);
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
        swapWeapons(isUsingBow());
    }

    @Override
    public void setupEquipmentAndUpgradesServer() {
        swapWeapons(isUsingBow());
    }

    protected AbstractArrow getArrow(ItemStack pArrowStack, float pVelocity) {
        return ProjectileUtil.getMobArrow(this, pArrowStack, pVelocity);
    }

    @Override
    public void performUnitRangedAttack(LivingEntity pTarget, float velocity) {
        ItemStack itemstack = this.getProjectile(this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this,
                (item) -> item instanceof BowItem
        )));
        AbstractArrow abstractarrow = this.getArrow(itemstack, velocity);
        if (this.getMainHandItem().getItem() instanceof BowItem) {
            abstractarrow = ((BowItem)this.getMainHandItem().getItem()).customArrow(abstractarrow);
        }
        double d0 = pTarget.getX() - this.getX();
        double d1 = pTarget.getY(0.3333333333333333) - abstractarrow.getY();
        double d2 = pTarget.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        if (pTarget.getEyeHeight() <= 1.0f)
            d1 -= (1.0f - pTarget.getEyeHeight());

        abstractarrow.shoot(d0, d1 + d3 * 0.20000000298023224, d2, 1.6F, 0);
        this.playSound(SoundEvents.SKELETON_SHOOT, 3.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(abstractarrow);

        if (!level().isClientSide() && pTarget instanceof Unit unit)
            FogOfWarClientboundPacket.revealRangedUnit(unit.getOwnerName(), this.getId());
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        return pSpawnData;
    }

    private static final EntityDataAccessor<VillagerData> VILLAGER_DATA;

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
        this.entityData.define(scenarioRoleDataAccessor, -1);
        this.entityData.define(VILLAGER_DATA, new VillagerData(VillagerType.PLAINS, VillagerProfession.ARMORER, 1));
    }

    @Override
    public VillagerData getVillagerData() {
        return this.entityData.get(VILLAGER_DATA);
    }

    @Override
    public void setVillagerData(VillagerData p_35437_) {
        VillagerData villagerdata = this.getVillagerData();
        this.entityData.set(VILLAGER_DATA, p_35437_);
    }

    static {
        VILLAGER_DATA = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.VILLAGER_DATA);
    }

    public int getSharpnessLevel() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        return itemStack.getEnchantmentLevel(Enchantments.SHARPNESS);
    }

    public int getPowerLevel() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        return itemStack.getEnchantmentLevel(Enchantments.POWER_ARROWS);
    }

    @Override
    public boolean hasBonusDamage() {
        return (isUsingBow() && getPowerLevel() > 0) ||
                (!isUsingBow() && getSharpnessLevel() > 0);
    }
}
