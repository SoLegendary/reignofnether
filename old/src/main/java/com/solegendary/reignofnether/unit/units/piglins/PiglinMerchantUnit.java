package com.solegendary.reignofnether.unit.units.piglins;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.heroAbilities.piglin.FancyFeast;
import com.solegendary.reignofnether.ability.heroAbilities.piglin.GreedIsGoodPassive;
import com.solegendary.reignofnether.ability.heroAbilities.piglin.LootExplosion;
import com.solegendary.reignofnether.ability.heroAbilities.piglin.ThrowTNT;
import com.solegendary.reignofnether.entities.ThrowableTntProjectile;
import com.solegendary.reignofnether.hero.HeroClientboundPacket;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.modelling.animations.PiglinMerchantAnimations;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PiglinMerchantUnit extends Piglin implements Unit, AttackerUnit, HeroUnit, KeyframeAnimated {
    // region
    private int eatingTicksLeft = 0;
    public void setEatingTicksLeft(int amount) { eatingTicksLeft = amount; }
    public int getEatingTicksLeft() { return eatingTicksLeft; }
    private BlockPos anchorPos = new BlockPos(0,0,0);
    public void setAnchor(BlockPos bp) { anchorPos = bp; }
    public BlockPos getAnchor() { return anchorPos; }

    private final ArrayList<Checkpoint> checkpoints = new ArrayList<>();
    public ArrayList<Checkpoint> getCheckpoints() { return checkpoints; };

    public GarrisonGoal getGarrisonGoal() { return null; }
    public boolean canGarrison() { return getGarrisonGoal() != null; }

    UsePortalGoal usePortalGoal;
    public UsePortalGoal getUsePortalGoal() { return usePortalGoal; }
    public boolean canUsePortal() { return getUsePortalGoal() != null; }

    public Faction getFaction() {return Faction.PIGLINS;}
    public List<AbilityButton> getAbilityButtons() {return abilityButtons;};
    public List<Ability> getAbilities() {return abilities;}
    public List<ItemStack> getItems() {return items;};
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}

    private GenericTargetedSpellGoal castFancyFeastGoal;
    public GenericTargetedSpellGoal getCastFancyFeastGoal() {
        return castFancyFeastGoal;
    }
    private GenericTargetedSpellGoal castTNTGoal;
    public GenericTargetedSpellGoal getCastTNTGoal() {
        return castTNTGoal;
    }
    private GenericUntargetedSpellGoal castLootExplosionGoal;
    public GenericUntargetedSpellGoal getCastLootExplosionGoal() {
        return castLootExplosionGoal;
    }

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
            SynchedEntityData.defineId(PiglinMerchantUnit.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
    }

    // combat stats
    public float getMovementSpeed() {return movementSpeed;}
    public float getUnitAttackDamage() {return attackDamage + (attackBonusPerLevel * getHeroLevel());}
    public float getUnitMaxHealth() {return maxHealth + (maxHealthBonusPerLevel * getHeroLevel());}

    @Nullable
    public ResourceCost getCost() {return ResourceCosts.PIGLIN_MERCHANT;}
    public boolean getWillRetaliate() {return willRetaliate;}
    public int getAttackCooldown() {return (int) (20 / attacksPerSecond);}
    public float getAttacksPerSecond() {return attacksPerSecond;}
    public float getAggroRange() {return aggroRange;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle && !isVehicle();}
    public float getAttackRange() {return attackRange;}
    public BlockPos getAttackMoveTarget() { return attackMoveTarget; }
    public boolean canAttackBuildings() {return getAttackBuildingGoal() != null;}
    public Goal getAttackGoal() { return attackGoal; }
    public Goal getAttackBuildingGoal() { return attackBuildingGoal; }
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
    private float baseMaxMana = 125;
    private float maxMana = baseMaxMana;
    private float mana = maxMana;
    private float manaRegenPerSecond = 0.8f;
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

    final static public float attackDamage = 8.0f;
    final static public float attackBonusPerLevel = 0.7f;
    final static public float attacksPerSecond = 0.35f;
    final static public float maxHealth = 150.0f;
    final static public float maxHealthBonusPerLevel = 10.0f;
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

    private final List<AbilityButton> abilityButtons = new ArrayList<>();
    private final List<Ability> abilities = new ArrayList<>();
    private final List<ItemStack> items = new ArrayList<>();

    public final AnimationState idleAnimState = new AnimationState();
    public final AnimationState walkAnimState = new AnimationState();
    public final AnimationState spellChargeAnimState = new AnimationState();
    public final AnimationState spellActivateAnimState = new AnimationState();
    public final AnimationState attackAnimState = new AnimationState();

    final static private int ATTACK_WINDUP_TICKS = 32;

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
                activeAnimDef = PiglinMerchantAnimations.ATTACK;
                activeAnimState = attackAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CAST_SPELL -> {
                activeAnimDef = PiglinMerchantAnimations.SPELL_FULL;
                activeAnimState = spellActivateAnimState;
                animateScale = 1.0f;
                startAnimation(activeAnimDef);
            }
            default -> animateScaleReducing = true;
        }
    }

    public PiglinMerchantUnit(EntityType<? extends Piglin> entityType, Level level) {
        super(entityType, level);
        this.abilities.add(new ThrowTNT(this));
        this.abilities.add(new FancyFeast(this));
        this.abilities.add(new GreedIsGoodPassive(this));
        this.abilities.add(new LootExplosion(this));
        updateAbilityButtons();
        setStatsForLevel();
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, PiglinMerchantUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, PiglinMerchantUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, PiglinMerchantUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, PiglinMerchantUnit.armorValue);
    }

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
        this.setCanPickUpLoot(false);
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);
        HeroUnit.tick(this);

        if (level().isClientSide() && animateTicks > 0) {
            animateTicks -= 1;
        }
        castTNTGoal.tick();
        castFancyFeastGoal.tick();
        castLootExplosionGoal.tick();
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // smooth travel up 1-high blocks from Ravager aiStep()s
        if (this.horizontalCollision) {
            boolean flag = false;
            AABB aabb = this.getBoundingBox().inflate(0.2);
            Iterator var8 = BlockPos.betweenClosed(Mth.floor(aabb.minX), Mth.floor(aabb.minY), Mth.floor(aabb.minZ), Mth.floor(aabb.maxX), Mth.floor(aabb.maxY), Mth.floor(aabb.maxZ)).iterator();
            label62:
            while(true) {
                BlockPos blockpos;
                Block block;
                do {
                    if (!var8.hasNext()) {
                        if (!flag && this.onGround())
                            this.jumpFromGround();
                        break label62;
                    }
                    blockpos = (BlockPos)var8.next();
                    BlockState blockstate = this.level().getBlockState(blockpos);
                    block = blockstate.getBlock();
                } while(!(block instanceof LeavesBlock));
            }
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

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.attackGoal = new MeleeWindupAttackUnitGoal(this, false, ATTACK_WINDUP_TICKS);
        this.attackBuildingGoal = new MeleeWindupAttackBuildingGoal(this, ATTACK_WINDUP_TICKS);
        this.castTNTGoal = new GenericTargetedSpellGoal(
                this,
                32,
                ThrowTNT.RANGE,
                UnitAnimationAction.ATTACK_UNIT,
                null,
                this::throwTNT,
                null
        );
        this.castFancyFeastGoal = new GenericTargetedSpellGoal(
                this,
                32,
                FancyFeast.RANGE,
                UnitAnimationAction.ATTACK_UNIT,
                null,
                this::fancyFeast,
                null
        );
        this.castLootExplosionGoal = new GenericUntargetedSpellGoal(
                this,
                60,
                this::lootExplosion,
                UnitAnimationAction.CAST_SPELL,
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
        this.targetSelector.addGoal(2, targetGoal);
        this.goalSelector.addGoal(3, moveGoal);
        //this.goalSelector.addGoal(4, new RandomLookAroundUnitGoal(this));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        return pSpawnData;
    }

    @Override
    public void resetBehaviours() {
        animateScaleReducing = true;
        this.castTNTGoal.stop();
        this.castFancyFeastGoal.stop();
        this.castLootExplosionGoal.stop();
    }

    public ThrowTNT getThrowTNT() {
        for (Ability ability : abilities)
            if (ability instanceof ThrowTNT)
                return (ThrowTNT) ability;
        return null;
    }

    public FancyFeast getFancyFeast() {
        for (Ability ability : abilities)
            if (ability instanceof FancyFeast)
                return (FancyFeast) ability;
        return null;
    }

    public GreedIsGoodPassive getGreedIsGood() {
        for (Ability ability : abilities)
            if (ability instanceof GreedIsGoodPassive)
                return (GreedIsGoodPassive) ability;
        return null;
    }

    public LootExplosion getLootExplosion() {
        for (Ability ability : abilities)
            if (ability instanceof LootExplosion)
                return (LootExplosion) ability;
        return null;
    }

    public void throwTNT(BlockPos targetBp) {
        if (level().isClientSide())
            return;

        ThrowableTntProjectile tnt = new ThrowableTntProjectile(level(), this);
        tnt.setItem(new ItemStack(ItemRegistrar.THROWABLE_TNT.get()));
        Vec3 dMove = Vec3.atCenterOf(targetBp).subtract(this.getEyePosition())
                .multiply(1,0,1)
                .scale(0.04)
                .add(0,0.5,0);
        tnt.setDeltaMovement(dMove);
        level().addFreshEntity(tnt);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.EGG_THROW,
                SoundSource.NEUTRAL, 0.5F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));

        GreedIsGoodPassive greedIsGood = getGreedIsGood();
        int resourceBonus = 0;
        if (greedIsGood.isAutocasting())
            resourceBonus = greedIsGood.spendResourcesAndGet100sSpent(ResourceName.WOOD);

        ThrowTNT throwTNT = getThrowTNT();
        throwTNT.setCooldown(throwTNT.cooldownMax - (resourceBonus * ThrowTNT.LESS_COOLDOWN_PER_100_RESOURCES));
        AbilityClientboundPacket.sendSetCooldownPacket(getId(), throwTNT.action, throwTNT.getCooldown());
        setMana(getMana() + (resourceBonus * ThrowTNT.LESS_MANA_PER_100_RESOURCES));
    }

    public void fancyFeast(BlockPos targetBp) {
        Vec3 pos = getEyePosition();

        GreedIsGoodPassive greedIsGood = getGreedIsGood();
        int resourceBonus = 0;
        if (greedIsGood.isAutocasting())
            resourceBonus = greedIsGood.spendResourcesAndGet100sSpent(ResourceName.FOOD);

        int numItems = FancyFeast.BASE_ITEMS + (FancyFeast.BONUS_ITEMS_PER_100_RESOURCES * resourceBonus);

        for (int i = 0; i < numItems; i++) {
            ItemEntity foodEntity = new ItemEntity(level(), pos.x, pos.y, pos.z, new ItemStack(getFancyFeast().getFoodItem()));
            foodEntity.setThrower(getUUID());
            Vec3 dMove = Vec3.atCenterOf(targetBp).subtract(pos)
                    .multiply(1,0,1)
                    .scale(0.04)
                    .add(
                            (random.nextFloat() - 0.5f) / 4,
                            0.5,
                            (random.nextFloat() - 0.5f) / 4
                    );
            foodEntity.setDeltaMovement(dMove);
            level().addFreshEntity(foodEntity);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.EGG_THROW,
                    SoundSource.NEUTRAL, 0.5F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
        }
    }

    // give at least one rare item per unit
    private List<ItemStack> getRandomLoot(int amount) {
        ArrayList<ItemStack> items = new ArrayList<>();
        ArrayList<LivingEntity> units = new ArrayList<>(UnitServerEvents.getAllUnits()
                .stream()
                .filter(u -> u instanceof Unit unit &&
                        unit.getOwnerName().equals(this.getOwnerName()) &&
                        unit instanceof AttackerUnit attackerUnit &&
                        !(unit instanceof GhastUnit))
                .toList());
        Collections.shuffle(units);

        for (int n = 0; n < amount; n++) {
            if (units.size() > n) {
                int i = random.nextInt(100);
                LivingEntity unit = units.get(n);

                if (unit instanceof BruteUnit) {
                    if (i >= 50)
                        items.add( new ItemStack(Items.NETHERITE_CHESTPLATE));
                    else if (i > 0) {
                        ItemStack itemStack = new ItemStack(Items.NETHERITE_SWORD);
                        itemStack.enchant(Enchantments.FIRE_ASPECT, 1);
                        items.add(itemStack);
                    }
                }
                else if (unit instanceof HeadhunterUnit) {
                    if (i >= 50)
                        items.add(new ItemStack(Items.NETHERITE_CHESTPLATE));
                    else if (i > 0) {
                        ItemStack itemStack = new ItemStack(Items.TRIDENT);
                        itemStack.enchant(Enchantments.FIRE_ASPECT, 1);
                        itemStack.enchant(Enchantments.UNBREAKING, 1);
                        items.add(itemStack);
                    }
                }
                else if (unit instanceof HoglinUnit ||
                        unit instanceof WitherSkeletonUnit) {
                    items.add(new ItemStack(Items.NETHERITE_CHESTPLATE));
                } else {
                    items.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
                }
            } else {
                items.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
            }
        }
        return items;
    }

    public void lootExplosion() {
        if (level().isClientSide())
            return;

        Vec3 pos = getEyePosition();

        GreedIsGoodPassive greedIsGood = getGreedIsGood();
        int resourceBonus = 0;
        if (greedIsGood.isAutocasting())
            resourceBonus = greedIsGood.spendResourcesAndGet100sSpent(ResourceName.ORE);

        int numItems = LootExplosion.BASE_ITEMS + (LootExplosion.BONUS_ITEMS_PER_100_RESOURCES * resourceBonus);
        List<ItemStack> items = getRandomLoot(numItems);

        for (ItemStack itemStack : items) {
            ItemEntity item = new ItemEntity(level(), pos.x, pos.y, pos.z, itemStack);
            item.setThrower(getUUID());
            Vec3 dMove = new Vec3(
                    (random.nextFloat() - 0.5f) / 2,
                    0.5,
                    (random.nextFloat() - 0.5f) / 2
            ).scale(1.25f);
            item.setDeltaMovement(dMove);
            level().addFreshEntity(item);
        }
        level().explode(null, null, null, getX(), getY(), getZ(),
                2.0f, false, Level.ExplosionInteraction.NONE);
    }
}
