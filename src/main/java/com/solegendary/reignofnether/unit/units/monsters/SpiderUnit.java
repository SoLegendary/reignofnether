package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.abilities.Eject;
import com.solegendary.reignofnether.ability.abilities.SpiderClimbing;
import com.solegendary.reignofnether.ability.abilities.SpinWebs;
import com.solegendary.reignofnether.blocks.BlockServerEvents;
import com.solegendary.reignofnether.building.RangeIndicator;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.ConvertableUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpiderUnit extends Spider implements Unit, AttackerUnit, ConvertableUnit, RangeIndicator {
    public static final Abilities ABILITIES = new Abilities();
    static {
        ABILITIES.add(new SpiderClimbing(), Keybindings.keyQ);
        ABILITIES.add(new Eject(), Keybindings.keyW);
        ABILITIES.add(new SpinWebs(), Keybindings.keyE);
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

    public GarrisonGoal getGarrisonGoal() { return null; }
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

    private EnemySearchBehaviour attackSearchBehaviour = EnemySearchBehaviour.NONE;
    public EnemySearchBehaviour getEnemySearchBehaviour() { return attackSearchBehaviour; }
    public void setEnemySearchBehaviour(EnemySearchBehaviour behaviour) { attackSearchBehaviour = behaviour; }

    private MoveToTargetBlockGoal moveGoal;
    private SelectedTargetGoal<? extends LivingEntity> targetGoal;
    private ReturnResourcesGoal returnResourcesGoal;

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
            SynchedEntityData.defineId(SpiderUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(SpiderUnit.class, EntityDataSerializers.INT);

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
    public float getUnitAttackDamage() {return attackDamage;}
    public float getUnitMaxHealth() {return maxHealth;}

    @Nullable
    public ResourceCost getCost() {return ResourceCosts.SPIDER;}
    public boolean canAttackBuildings() {return getAttackBuildingGoal() != null;}

    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // ConvertableUnit
    private boolean shouldDiscard = false;
    public boolean shouldDiscard() { return shouldDiscard; }
    public void setShouldDiscard(boolean discard) { this.shouldDiscard = discard; }

    // endregion

    final static public float attackDamage = 5.0f;
    final static public float attacksPerSecond = 0.6f;
    final static public float maxHealth = 30.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.34f;
    final static public float attackRange = 2; // only used by ranged units or melee building attackers
    final static public float aggroRange = 10;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;

    private boolean wallClimbing = true;

    public int maxResources = 100;

    private AbstractMeleeAttackUnitGoal attackGoal;
    private MeleeAttackBuildingGoal attackBuildingGoal;
    private GenericTargetedSpellGoal webGoal;
    public GenericTargetedSpellGoal getWebGoal() { return webGoal; }

    @Nullable
    public SpinWebs getWebAbility() {
        for (Ability ability : this.getAbilities().get())
            if (ability instanceof SpinWebs spinWebs)
                return spinWebs;
        return null;
    }

    protected Abilities abilities = ABILITIES.clone();
    private final List<ItemStack> items = new ArrayList<>();

    public SpiderUnit(EntityType<? extends Spider> entityType, Level level) {
        super(entityType, level);
        this.setMaxUpStep(1.0F);
    }

    public boolean isWallClimbing() { return wallClimbing; }

    public boolean toggleWallClimbing() {
        wallClimbing = !wallClimbing;
        if (wallClimbing) {
            this.navigation = new WallClimberNavigation(this, level());
        } else {
            this.navigation = new GroundPathNavigation(this, level());
        }
        return wallClimbing;
    }

    @Override
    public boolean isClimbing() {
        if (wallClimbing)
            return super.isClimbing();
        else
            return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, SpiderUnit.movementSpeed)
                .add(Attributes.ATTACK_DAMAGE, SpiderUnit.attackDamage)
                .add(Attributes.ARMOR, SpiderUnit.armorValue)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.MAX_HEALTH, SpiderUnit.maxHealth);
    }

    // for some reason this.getNavigation().stop(); doesn't stop spider units from moving
    @Override
    public void resetBehaviours() {
        this.getMoveGoal().setMoveTarget(this.getOnPos());
        if (getWebGoal() != null)
            getWebGoal().stop();
    }

    public void tick() {
        if (shouldDiscard) {
            this.discard();
        } else {
            this.setCanPickUpLoot(false);
            super.tick();
            Unit.tick(this);
            AttackerUnit.tick(this);

            if (getWebGoal() != null)
                getWebGoal().tick();
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
    public SunlightEffect getSunlightEffect() {
        return SunlightEffect.SLOWNESS_II;
    }

    @Override
    public void kill() {
        super.kill();
    }

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.attackGoal = new MeleeAttackUnitGoal(this, false);
        this.attackBuildingGoal = new MeleeAttackBuildingGoal(this);
        this.webGoal = new GenericTargetedSpellGoal(this, 0, SpinWebs.RANGE, this::onEntityCastWeb, this::onGroundCastWeb, null);
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();
        this.goalSelector.addGoal(2, usePortalGoal);

        // movegoal must be lower priority than attacks so that attack-moving works correctly
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, attackGoal);
        this.goalSelector.addGoal(2, attackBuildingGoal);
        this.targetSelector.addGoal(2, targetGoal);
        this.goalSelector.addGoal(2, webGoal);
        this.goalSelector.addGoal(3, moveGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundUnitGoal(this));
    }

    // removes vanilla spider jockey spawn and random effects
    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        return pSpawnData;
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity pEntity) {
        if (super.doHurtTarget(pEntity)) {
            if (getWebAbility() != null && getWebAbility().isAutocasting(this))
                getWebAbility().use(this.level(), this, pEntity.getOnPos());
            return true;
        } else {
            return false;
        }
    }

    public void onEntityCastWeb(LivingEntity targetEntity) {
        onGroundCastWeb(targetEntity.getOnPos());
    }

    public void onGroundCastWeb(BlockPos targetBp) {
        SpinWebs spinWebs = getWebAbility();
        if (spinWebs == null)
            return;

        if (!level().isClientSide() && !isVehicle()) {
            BlockPos limitedBp = MyMath.getXZRangeLimitedBlockPos(getOnPos(), targetBp, spinWebs.range);

            BlockPos originBp = MiscUtil.getHighestNonAirBlock(level(), limitedBp, true);
            List<Vec2> vec2s = List.of(
                    new Vec2(0,0),
                    new Vec2(1,1),
                    new Vec2(-1,-1),
                    new Vec2(1,-1),
                    new Vec2(-1,1)
            );
            for (Vec2 vec2 : vec2s) {
                BlockPos bp = MiscUtil.getHighestNonAirBlock(level(), limitedBp.offset((int) vec2.x, 0, (int) vec2.y), true);
                if (distanceToSqr(Vec3.atCenterOf(bp)) < (spinWebs.range * 2) * (spinWebs.range * 2))
                    BlockServerEvents.addTempBlock((ServerLevel) level(), bp.above().above(), Blocks.COBWEB.defaultBlockState(),
                            Blocks.AIR.defaultBlockState(), SpinWebs.DURATION_SECONDS * 20);
            }
            resetBehaviours();
        } else if (level().isClientSide()) {
            if (isVehicle()) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.spin_webs.error1"));
                return;
            }
        }
        if (!isVehicle()) {
            spinWebs.setToMaxCooldown(this);
            if (!level().isClientSide())
                AbilityClientboundPacket.sendSetCooldownPacket(getId(), spinWebs.action, spinWebs.cooldownMax);
        }
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource == damageSources().fall())
            return false;
        return super.hurt(pSource, pAmount);
    }
}
