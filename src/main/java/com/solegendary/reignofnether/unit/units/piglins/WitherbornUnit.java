package com.solegendary.reignofnether.unit.units.piglins;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.ability.heroAbilities.witherborn.Castigation;
import com.solegendary.reignofnether.ability.heroAbilities.witherborn.EssenceDrain;
import com.solegendary.reignofnether.ability.heroAbilities.witherborn.WitheringMark;
import com.solegendary.reignofnether.ability.heroAbilities.witherborn.WorldOfRot;
import com.solegendary.reignofnether.blocks.RangeIndicator;
import com.solegendary.reignofnether.entities.ThrowableTntProjectile;
import com.solegendary.reignofnether.hero.HeroClientboundPacket;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.modelling.animations.WitherbornAnimations;
import com.solegendary.reignofnether.util.MiscUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import oshi.util.tuples.Pair;

import javax.annotation.Nullable;
import java.util.*;

public class WitherbornUnit extends WitherSkeleton implements Unit, AttackerUnit, HeroUnit, KeyframeAnimated, RangeIndicator {
    public final Abilities ABILITIES = new Abilities(
            List.of(
                    new Pair<>(new EssenceDrain(), Keybindings.abilitySlot1),
                    new Pair<>(new WitheringMark(), Keybindings.abilitySlot2),
                    new Pair<>(new Castigation(), Keybindings.abilitySlot3),
                    new Pair<>(new WorldOfRot(), Keybindings.abilitySlot4)
            )
    );

    boolean needsStatSync = false;
    @Override public boolean needsStatSync() { return needsStatSync; }
    @Override public void setNeedsStatSync(boolean value) { needsStatSync = value; }

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
    @Override public Object2ObjectArrayMap<Ability, Float> getAbilityCooldowns() { return cooldowns; }
    @Override public boolean hasAutocast(Ability ability) { return autocast == ability; }
    @Override public void setAutocast(Ability autocast) { this.autocast = autocast; }
    @Override public Object2ObjectArrayMap<Ability, Integer> getAbilityCharges() { return charges; }
    Object2ObjectArrayMap<HeroAbility, Integer> heroAbilityRanks = new Object2ObjectArrayMap<>();

    Ability autocast;


    private int eatingTicksLeft = 0;
    public void setEatingTicksLeft(int amount) { eatingTicksLeft = amount; }
    public int getEatingTicksLeft() { return eatingTicksLeft; }
    private BlockPos anchorPos = new BlockPos(0,0,0);
    public void setAnchor(BlockPos bp) { anchorPos = bp; }
    public BlockPos getAnchor() { return anchorPos; }

    private final ArrayList<Checkpoint> checkpoints = new ArrayList<>();
    public ArrayList<Checkpoint> getCheckpoints() { return checkpoints; }

    public GarrisonGoal getGarrisonGoal() { return null; }
    public boolean canGarrison() { return getGarrisonGoal() != null; }

    UnitItemGoal itemGoal;
    @Override public UnitItemGoal getItemGoal() { return itemGoal; }

    UsePortalGoal usePortalGoal;
    public UsePortalGoal getUsePortalGoal() { return usePortalGoal; }
    public boolean canUsePortal() { return getUsePortalGoal() != null; }

    public Abilities getAbilities() {return abilities;}
    public List<ItemStack> getItems() {return items;};
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}

    private GenericTargetedSpellGoal castEssenceDrainGoal;
    public GenericTargetedSpellGoal getCastEssenceDrainGoal() {
        return castEssenceDrainGoal;
    }
    private GenericTargetedSpellGoal castWitheringMarkGoal;
    public GenericTargetedSpellGoal getCastWitheringMarkGoal() {
        return castWitheringMarkGoal;
    }
    private GenericUntargetedSpellGoal castCastigationGoal;
    public GenericUntargetedSpellGoal getCastCastigationGoal() {
        return castCastigationGoal;
    }
    private GenericUntargetedSpellGoal castWorldOfRotGoal;
    public GenericUntargetedSpellGoal getCastWorldOfRotGoal() {
        return castWorldOfRotGoal;
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
    // Refreshed by local writes and incoming entity data; hot reads avoid the data lock.
    private volatile String cachedOwnerName = "";

    public String getOwnerName() { return cachedOwnerName; }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (ownerDataAccessor.equals(accessor)) {
            cachedOwnerName = this.entityData.get(ownerDataAccessor);
        }
        super.onSyncedDataUpdated(accessor);
    }
    public void setOwnerName(String name) { this.entityData.set(ownerDataAccessor, name); }
    public static final EntityDataAccessor<String> ownerDataAccessor =
            SynchedEntityData.defineId(WitherbornUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(WitherbornUnit.class, EntityDataSerializers.INT);

    public String getOnDeathCommand() { return this.entityData.get(onDeathCommandDataAccessor); }
    public void setOnDeathCommand(String command) { this.entityData.set(onDeathCommandDataAccessor, command); }
    public static final EntityDataAccessor<String> onDeathCommandDataAccessor =
            SynchedEntityData.defineId(WitherbornUnit.class, EntityDataSerializers.STRING);

    public boolean isCastigationEnabled() { return this.entityData.get(castigationEnabledAccessor); }
    public void setCastigationEnabled(boolean value) { this.entityData.set(castigationEnabledAccessor, value); }
    public static final EntityDataAccessor<Boolean> castigationEnabledAccessor =
            SynchedEntityData.defineId(WitherbornUnit.class, EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
        this.entityData.define(scenarioRoleDataAccessor, -1);
        this.entityData.define(onDeathCommandDataAccessor, "");
        this.entityData.define(castigationEnabledAccessor, false);
    }

    @Nullable
    public ResourceCost getCost() {return ResourceCosts.WITHERBORN;}
    public boolean getWillRetaliate() {return willRetaliate;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle && !isVehicle();}
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

    private int skillPoints = 1;
    private int experience = 0;
    private boolean rankUpMenuOpen = false;
    @Override public int getSkillPoints() { return skillPoints; }
    @Override public void setSkillPoints(int points) { skillPoints = points; }
    @Override public boolean isRankUpMenuOpen() { return rankUpMenuOpen || (Keybindings.ctrlMod.isDown() && getSkillPoints() > 0); }
    @Override public void showRankUpMenu(boolean show) { rankUpMenuOpen = show; }
    @Override public int getExperience() { return experience; }
    @Override public void setExperience(int amount) {
        experience = amount;
        setStatsForLevel();
    }
    final static private float baseMaxMana = 125;
    private float maxMana = baseMaxMana;
    private float mana = maxMana;
    final static private float manaRegenPerSecond = 0.8f;
    final static private float manaBonusPerLevel = 8;
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

    final static public float attackDamage = 8.0f;
    final static public float attackBonusPerLevel = 0.7f;
    final static public float attacksPerSecond = 0.35f;
    final static public float maxHealth = 150.0f;
    final static public float maxHealthBonusPerLevel = 10.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.28f;
    final static public float attackRange = 2; // only used by ranged units or melee building attackers
    final static public float aggroRange = 10;
    final static public double magicDamageResist = 0.3d;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;
    public int maxResources = 100;

    private Abilities abilities = ABILITIES.clone();
    private final List<ItemStack> items = new ArrayList<>();

    public final AnimationState idleAnimState = new AnimationState();
    public final AnimationState walkAnimState = new AnimationState();
    public final AnimationState attackAnimState = new AnimationState();
    public final AnimationState essenceDrainAnimState = new AnimationState();
    public final AnimationState witheringMarkAnimState = new AnimationState();
    public final AnimationState worldOfRotAnimState = new AnimationState();

    private float ageInTicksOffset = 0;
    public float getAgeInTicksOffset() { return ageInTicksOffset; }
    public void setAgeInTicksOffset(float ticks) { ageInTicksOffset = ticks; }

    @Override
    public int getAttackWindupTicks() {
        return 16;
    }

    // non-looping animations
    public AnimationDefinition activeAnimDef = null;
    public AnimationState activeAnimState = null;

    public void stopAllAnimations() {
        idleAnimState.stop();
        walkAnimState.stop();
        attackAnimState.stop();
        essenceDrainAnimState.stop();
        witheringMarkAnimState.stop();
        worldOfRotAnimState.stop();
    }
    public int animateTicks = 0;
    public float animateSpeed = 1.0f;
    public boolean stopActiveAnimation = false;
    public void setAnimateTicksLeft(int ticks) { animateTicks = ticks; }
    public int getAnimateTicksLeft() { return animateTicks; }

    public void playSingleAnimation(UnitAnimationAction animAction) {
        stopActiveAnimation = false;
        switch (animAction) {
            case ATTACK_UNIT, ATTACK_BUILDING -> {
                activeAnimDef = WitherbornAnimations.ATTACK;
                activeAnimState = attackAnimState;
                animateSpeed = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CAST_SPELL -> {
                activeAnimDef = WitherbornAnimations.ESSENCE_DRAIN;
                activeAnimState = essenceDrainAnimState;
                animateSpeed = 1.0f;
                startAnimation(activeAnimDef);
            }
            case CAST_SPELL_ALT -> {
                activeAnimDef = WitherbornAnimations.WITHERING_MARK;
                activeAnimState = witheringMarkAnimState;
                animateSpeed = 1.0f;
                startAnimation(activeAnimDef);
            }
            case ULTIMATE -> {
                activeAnimDef = WitherbornAnimations.WORLD_OF_ROT;
                activeAnimState = worldOfRotAnimState;
                animateSpeed = 1.0f;
                startAnimation(activeAnimDef);
            }
            default ->  {
                stopActiveAnimation = true;
                animateSpeed = 1.0f;
            }
        }
    }

    public WitherbornUnit(EntityType<? extends WitherSkeleton> entityType, Level level) {
        super(entityType, level);

        updateAbilityButtons();
        setStatsForLevel();
    }

    @Override
    public float getDamageAfterMagicAbsorb(DamageSource pSource, float pDamage) {
        pDamage = super.getDamageAfterMagicAbsorb(pSource, pDamage);
        if (MiscUtil.isMagicDamage(pSource))
            pDamage *= (1 - getUnitMagicArmorPercentage());
        return pDamage;
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return HeroUnit.createDefaultAttributes()
                .add(Attributes.ATTACK_DAMAGE, WitherbornUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, WitherbornUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, WitherbornUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, WitherbornUnit.armorValue)
                .add(AttributeRegistrar.BASE_MAX_HEALTH.get(), WitherbornUnit.maxHealth)
                .add(AttributeRegistrar.ATTACK_DAMAGE.get(), attackDamage)
                .add(AttributeRegistrar.ATTACKS_PER_SECOND.get(), attacksPerSecond)
                .add(AttributeRegistrar.ATTACK_RANGE.get(), attackRange)
                .add(AttributeRegistrar.AGGRO_RANGE.get(), aggroRange)
                .add(AttributeRegistrar.SIGHT_RANGE.get(), HeroUnit.DEFAULT_SIGHT_RANGE)
                .add(AttributeRegistrar.MAGIC_DAMAGE_RESIST.get(), magicDamageResist)
                .add(AttributeRegistrar.BASE_MAX_MANA.get(), baseMaxMana)
                .add(AttributeRegistrar.MANA_REGEN_PER_SECOND.get(), manaRegenPerSecond)
                .add(AttributeRegistrar.MAX_MANA_BONUS_PER_LEVEL.get(), manaBonusPerLevel)
                .add(AttributeRegistrar.MAX_HEALTH_BONUS_PER_LEVEL.get(), maxHealthBonusPerLevel)
                .add(AttributeRegistrar.ATTACK_DAMAGE_BONUS_PER_LEVEL.get(), attackBonusPerLevel);
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
        HeroUnit.tick(this);

        if (level().isClientSide() && animateTicks > 0) {
            animateTicks -= 1;
        }
        castEssenceDrainGoal.tick();
        castWitheringMarkGoal.tick();
        castCastigationGoal.tick();
        castWorldOfRotGoal.tick();

        if (level().isClientSide() && HudClientEvents.hudSelectedEntity == this) {
            if (!lastOnPos.equals(getOnPos())) {
                updateHighlightBps(level());
            }
            lastOnPos = getOnPos();
        }
    }

    @Override
    public void remove(@NotNull RemovalReason pReason) {
        this.runOnDeathCommand(pReason);
        super.remove(pReason);
    }

    private Set<BlockPos> highlightBps = new HashSet<>();
    private BlockPos lastOnPos = new BlockPos(0,0,0);

    @Override public Set<BlockPos> getHighlightBps() { return highlightBps; }
    @Override public void setHighlightBps(Set<BlockPos> bps) { highlightBps = bps; }

    @Override
    public void aiStep() {
        super.aiStep();

        // smooth travel up 1-high blocks from Ravager aiStep()s
        if (this.horizontalCollision) {
            boolean flag = false;
            AABB aabb = this.getBoundingBox().inflate(0.2);
            Iterator<BlockPos> var8 = BlockPos.betweenClosed(Mth.floor(aabb.minX), Mth.floor(aabb.minY), Mth.floor(aabb.minZ), Mth.floor(aabb.maxX), Mth.floor(aabb.maxY), Mth.floor(aabb.maxZ)).iterator();
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
                    blockpos = var8.next();
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
        this.setNeedsStatSync(true);
    }

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.attackGoal = new MeleeWindupAttackUnitGoal(this, false);
        this.attackBuildingGoal = new MeleeWindupAttackBuildingGoal(this);
        this.itemGoal = new UnitItemGoal(this);
        this.castEssenceDrainGoal = new GenericTargetedSpellGoal(
                this,
                EssenceDrain.CHANNEL_TICKS,
                EssenceDrain.RANGE,
                UnitAnimationAction.CAST_SPELL,
                null,
                this::essenceDrain,
                null
        );
        this.castWitheringMarkGoal = new GenericTargetedSpellGoal(
                this,
                WitheringMark.CAST_TICKS,
                WitheringMark.RANGE,
                UnitAnimationAction.CAST_SPELL_ALT,
                null,
                (BlockPos bp) -> this.witheringMark(),
                null
        );
        this.castCastigationGoal = new GenericUntargetedSpellGoal(
                this,
                0,
                this::toggleCastigation,
                null,
                UnitAnimationAction.STOP,
                null
        );
        this.castWorldOfRotGoal = new GenericUntargetedSpellGoal(
                this,
                WorldOfRot.CAST_TICKS,
                this::worldOfRot,
                UnitAnimationAction.ULTIMATE,
                UnitAnimationAction.STOP,
                null
        );
        this.returnResourcesGoal = new ReturnResourcesGoal(this);
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();
        this.goalSelector.addGoal(2, usePortalGoal);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, returnResourcesGoal);
        this.goalSelector.addGoal(2, attackGoal);
        this.goalSelector.addGoal(2, attackBuildingGoal);
        this.goalSelector.addGoal(2, itemGoal);
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
        this.castEssenceDrainGoal.stop();
        this.castWitheringMarkGoal.stop();
        this.castCastigationGoal.stop();
        this.castWorldOfRotGoal.stop();
    }

    public EssenceDrain getEssenceDrain() {
        for (Ability ability : abilities.get())
            if (ability instanceof EssenceDrain)
                return (EssenceDrain) ability;
        return null;
    }

    public WitheringMark getWitheringMark() {
        for (Ability ability : abilities.get())
            if (ability instanceof WitheringMark)
                return (WitheringMark) ability;
        return null;
    }

    public Castigation getCastigation() {
        for (Ability ability : abilities.get())
            if (ability instanceof Castigation)
                return (Castigation) ability;
        return null;
    }

    public WorldOfRot getWorldOfRot() {
        for (Ability ability : abilities.get())
            if (ability instanceof WorldOfRot)
                return (WorldOfRot) ability;
        return null;
    }

    public void essenceDrain(BlockPos targetBp) {

    }

    public void witheringMark() {

    }

    public void toggleCastigation() {
        setCastigationEnabled(!isCastigationEnabled());
        if (!level().isClientSide()) {
            AbilityClientboundPacket.doAbility(getId(), UnitAction.CASTIGATION_SET, isCastigationEnabled());
        }
    }

    public void worldOfRot() {

    }

    @Override
    public AABB getInflatedSelectionBox() {
        AABB aabb = this.getBoundingBox().inflate(0.3f, 0, 0.3f);
        aabb.setMaxY(aabb.maxY + 0.8f);
        return aabb;
    }
}
