package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.util.AttributeHelpers;
import com.solegendary.reignofnether.util.MobEffectHelpers;
import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.Explode;
import com.solegendary.reignofnether.hud.TooltipColours;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import net.minecraft.world.level.GameRules;

public class CreeperUnit extends Creeper implements Unit, AttackerUnit {
    public static final Abilities ABILITIES = new Abilities();
    static {
        ABILITIES.add(new Explode());
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
    private CreeperAttackUnitGoal attackGoal;
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
            SynchedEntityData.defineId(CreeperUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(CreeperUnit.class, EntityDataSerializers.INT);
    
    public String getOnDeathCommand() { return this.entityData.get(onDeathCommandDataAccessor); }
    public void setOnDeathCommand(String command) { this.entityData.set(onDeathCommandDataAccessor, command); }
    public static final EntityDataAccessor<String> onDeathCommandDataAccessor =
        SynchedEntityData.defineId(CreeperUnit.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
                builder.define(ownerDataAccessor, "");
        builder.define(scenarioRoleDataAccessor, -1);
        builder.define(onDeathCommandDataAccessor, "");
    }

    // combat stats
    public boolean getWillRetaliate() {return willRetaliate;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle && !isVehicle();}

    @Nullable //Defined after CommonSetup, as this value is loaded from configuration
    public ResourceCost getCost() {return ResourceCosts.CREEPER;}
    public boolean canAttackBuildings() {return getAttackBuildingGoal() != null;}

    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    final static public float attackDamage = 20.0f;
    final static public float attacksPerSecond = 1f;
    final static public float maxHealth = 20.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.28f;
    final static public float attackRange = 2; // only used by ranged units or melee building attackers
    final static public float aggroRange = 10;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = false;

    final static public float EXPLOSION_RADIUS = 3;
    final static public float CHARGED_EXPLOSION_RADIUS = 5;
    final static public float CHARGED_DAMAGE_MULT = 1.7f;

    final static public int maxResources = 0;

    private MeleeAttackBuildingGoal attackBuildingGoal;

    private Abilities abilities = ABILITIES.clone();
    private final List<ItemStack> items = new ArrayList<>();

    private boolean forceSwelling = false;

    public CreeperUnit(EntityType<? extends Creeper> entityType, Level level) {
        super(entityType, level);

        Explode explodeAbility = new Explode();
        this.abilities.add(explodeAbility);
    }

    // 1.21.1 made Creeper#explodeCreeper and #spawnLingeringCloud private, so the unit keeps its
    // own copy rather than overriding; the body is vanilla's, with the mob-griefing game rule
    // this mod has always honoured instead of Level.ExplosionInteraction.MOB.
    public void explodeCreeper() {
        if (!this.level().isClientSide) {
            Level.ExplosionInteraction explosion$blockinteraction = this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
            float radius = this.isPowered() ? CHARGED_EXPLOSION_RADIUS : EXPLOSION_RADIUS;
            this.dead = true;
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), radius, explosion$blockinteraction);
            this.discard();
            this.spawnLingeringCloud();
        }
    }

    private void spawnLingeringCloud() {
        Collection<MobEffectInstance> effects = this.getActiveEffects();
        if (!effects.isEmpty()) {
            AreaEffectCloud cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
            cloud.setRadius(2.5F);
            cloud.setRadiusOnUse(-0.5F);
            cloud.setWaitTime(10);
            cloud.setDuration(cloud.getDuration() / 2);
            cloud.setRadiusPerTick(-cloud.getRadius() / (float)cloud.getDuration());

            for (MobEffectInstance effect : effects)
                cloud.addEffect(new MobEffectInstance(effect));

            this.level().addFreshEntity(cloud);
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public boolean canExplodeOnTarget() {
        LivingEntity target = this.getTarget();
        if (target != null && target.position().distanceTo(this.position()) <= 2f)
            return true;
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Unit.createDefaultAttributes()
                .add(Attributes.MOVEMENT_SPEED, CreeperUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, CreeperUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, CreeperUnit.armorValue)
                .add(AttributeHelpers.holder(AttributeRegistrar.ATTACK_DAMAGE.get()), attackDamage)
                .add(AttributeHelpers.holder(AttributeRegistrar.ATTACKS_PER_SECOND.get()), attacksPerSecond)
                .add(AttributeHelpers.holder(AttributeRegistrar.ATTACK_RANGE.get()), attackRange)
                .add(AttributeHelpers.holder(AttributeRegistrar.AGGRO_RANGE.get()), aggroRange)
                .add(AttributeHelpers.holder(AttributeRegistrar.SIGHT_RANGE.get()), Unit.DEFAULT_SIGHT_RANGE)
                .add(AttributeHelpers.holder(AttributeRegistrar.RANGED_DAMAGE_RESIST.get()), 0)
                .add(AttributeHelpers.holder(AttributeRegistrar.MAGIC_DAMAGE_RESIST.get()), 0);
    }

    public void tick() {
        this.setCanPickUpLoot(false);

        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);

        if (forceSwelling)
            this.setSwellDir(1);
        else if (!canExplodeOnTarget())
            this.setSwellDir(-1);
    }

    @Override
    public void remove(@NotNull RemovalReason pReason) {
	    if (this.level() instanceof ServerLevel serverLevel) {
            String command = this.getOnDeathCommand();
            if (command != null && !command.isEmpty()) {
                CommandSourceStack source;
                source = serverLevel.getServer()
                    .createCommandSourceStack()
                    .withEntity(this)
                    .withPosition(this.position())
                    .withLevel(serverLevel)
                    .withPermission(2);
                serverLevel.getServer().getCommands().performPrefixedCommand(source, command);
            }
        }
        super.remove(pReason);
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

    @Override
    public SunlightEffect getSunlightEffect() {
        return SunlightEffect.SLOWNESS_II;
    }

    @Override
    public void resetBehaviours() {
        forceSwelling = false;
    }

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.garrisonGoal = new GarrisonGoal(this);
        this.attackGoal = new CreeperAttackUnitGoal(this, (int) getAttackCooldown(), false);
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();
        this.goalSelector.addGoal(2, usePortalGoal);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, attackGoal);
        this.goalSelector.addGoal(2, garrisonGoal);
        this.targetSelector.addGoal(2, targetGoal);
        this.goalSelector.addGoal(3, moveGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundUnitGoal(this));
    }

    public void startToExplode() {
        forceSwelling = true;
    }

    @Override
    public void thunderHit(ServerLevel pLevel, LightningBolt pLightning) {
        super.thunderHit(pLevel, pLightning);
        this.addEffect(MobEffectHelpers.instance(MobEffects.MOVEMENT_SPEED, 30 * 20, 0));
    }

    @Override
    public List<FormattedCharSequence> getAttackDamageStatTooltip() {
        return List.of(
                fcs(I18n.get("unitstats.reignofnether.attack_damage"), true),
                fcs(I18n.get("unitstats.reignofnether.attack_damage_bonus_buildings", "300%"))
        );
    }
    @Override
    public int getDamageTooltipColour() {
        return TooltipColours.GREEN;
    }
}
