package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.abilities.*;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.modelling.models.VillagerUnitModel;
import com.solegendary.reignofnether.faction.Faction;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EvokerUnit extends Evoker implements Unit, AttackerUnit, RangedAttackerUnit {
    public static final Abilities ABILITIES = new Abilities();
    static {
        ABILITIES.add(new SetFangsLine());
        ABILITIES.add(new SetFangsCircle());
        ABILITIES.add(new CastSummonVexes(), Keybindings.keyQ);
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
    public List<ItemStack> getItems() {return items;}
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public Goal getAttackGoal() {return attackGoal;}
    public Goal getAttackBuildingGoal() {return attackBuildingGoal;}

    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}

    private MoveToTargetBlockGoal moveGoal;
    private SelectedTargetGoal<? extends LivingEntity> targetGoal;
    public BuildRepairGoal buildRepairGoal;
    public GatherResourcesGoal gatherResourcesGoal;
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
            SynchedEntityData.defineId(EvokerUnit.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
    }

    // combat stats
    public boolean getWillRetaliate() {return willRetaliate;}
    public int getAttackCooldown() {
        return (int) (20 * (hasVigorEnchant() ? EnchantVigor.cooldownMultiplier : 1) / attacksPerSecond);
    }
    public float getAttacksPerSecond() {return 20f / (getAttackCooldown() + 25);}
    public float getAggroRange() {return aggroRange;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle && !isVehicle();}
    public float getAttackRange() {
        return isUsingLineFangs ? EvokerUnit.FANGS_RANGE_LINE : EvokerUnit.FANGS_RANGE_CIRCLE;
    }
    public float getUnitAttackDamage() {return attackDamage;}
    public boolean canAttackBuildings() {return getAttackBuildingGoal() != null;}

    public float getMovementSpeed() {return movementSpeed;}
    public float getUnitMaxHealth() {return maxHealth;}

    @Nullable
    public ResourceCost getCost() {return ResourceCosts.EVOKER;}

    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    private GenericTargetedSpellGoal castFangsGoal;
    public GenericTargetedSpellGoal getCastFangsGoal() {
        return castFangsGoal;
    }
    private GenericUntargetedSpellGoal castSummonVexesGoal;
    public GenericUntargetedSpellGoal getCastSummonVexesGoal() {
        return castSummonVexesGoal;
    }

    public static final int FANGS_RANGE_LINE = 10;
    public static final int FANGS_RANGE_CIRCLE = 3;
    public static final float FANGS_DAMAGE = 6f; // can sometimes be doubled or tripled due to overlapping fang hitboxes
    public static final int FANGS_CHANNEL_SECONDS = 1;
    public static final int SUMMON_VEXES_AMOUNT = 3;
    public static final int VEX_TARGET_RANGE = 20;
    public static final int VEX_TARGET_RANGE_GARRISON = 30;
    public static final int SUMMON_VEXES_CHANNEL_SECONDS = 3;

    final static public float attackDamage = FANGS_DAMAGE * 2;
    final static public float attacksPerSecond = 1f / (SetFangsLine.CD_MAX_SECONDS + FANGS_CHANNEL_SECONDS);
    final static public float aggroRange = FANGS_RANGE_LINE;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;

    final static public float maxHealth = 40.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.25f;

    public boolean isUsingLineFangs = true; // toggle between line and circular fangs
    public boolean lastCastedCircleFangs = false; // double damage to make up for circle fangs not overlapping

    public int maxResources = 100;

    public int fogRevealDuration = 0; // set > 0 for the client who is attacked by this unit
    public int getFogRevealDuration() { return fogRevealDuration; }
    public void setFogRevealDuration(int duration) { fogRevealDuration = duration; }

    private UnitBowAttackGoal<? extends LivingEntity> attackGoal;
    private MeleeAttackBuildingGoal attackBuildingGoal;

    private Abilities abilities = ABILITIES.clone();
    private final List<ItemStack> items = new ArrayList<>();

    public EvokerUnit(EntityType<? extends Evoker> entityType, Level level) {
        super(entityType, level);

        updateAbilityButtons();
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, EvokerUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, EvokerUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, EvokerUnit.armorValue);
    }

    @Override
    public void resetBehaviours() {
        this.castFangsGoal.stop();
        this.castSummonVexesGoal.stop();
        if (attackGoal != null && !this.abilities.get().isEmpty() && this.abilities.get().get(0).isOffCooldown(this))
            this.attackGoal.resetCooldown();
    }

    public void tick() {
        this.setCanPickUpLoot(true);
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);
        this.castFangsGoal.tick();
        this.castSummonVexesGoal.tick();
        PromoteIllager.checkAndApplyBuff(this);

        for (Ability ability : getAbilities().get()) {
            if (ability instanceof CastSummonVexes castSummonVexes) {
                if (castSummonVexes.isAutocasting(this) && !isCastingSpell() && castSummonVexes.isOffCooldown(this) && !level().isClientSide() && isIdle() &&
                    tickCount % 4 == 0 && ResearchServerEvents.playerHasResearch(getOwnerName(), ProductionItems.RESEARCH_EVOKER_VEXES)) {
                    this.castSummonVexesGoal.startCasting();
                }
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
        this.garrisonGoal = new GarrisonGoal(this);
        this.attackGoal = new UnitBowAttackGoal<>(this);
        this.returnResourcesGoal = new ReturnResourcesGoal(this);
        this.castFangsGoal = new GenericTargetedSpellGoal(this,
            FANGS_CHANNEL_SECONDS * ResourceCost.TICKS_PER_SECOND, FANGS_RANGE_LINE,
            this::createEvokerFangs, null, null
        );
        this.castSummonVexesGoal = new GenericUntargetedSpellGoal(
            this,
            SUMMON_VEXES_CHANNEL_SECONDS * ResourceCost.TICKS_PER_SECOND,
            this::summonVexes,
            UnitAnimationAction.NON_KEYFRAME_START,
            UnitAnimationAction.NON_KEYFRAME_STOP
        );
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

    // controls whether the evoker's arms are up or not
    @Override
    public boolean isCastingSpell() {
        for (Ability ability : getAbilities().get())
            if (ability instanceof SetFangsCircle || ability instanceof SetFangsLine) {
                if (ability.isOffCooldown(this) && this.getCastFangsGoal() != null && this.getCastFangsGoal().isCasting())
                    return true;
            } else if (ability instanceof CastSummonVexes) {
                if (ability.isOffCooldown(this) && this.getCastSummonVexesGoal() != null && this.getCastSummonVexesGoal().isCasting())
                    return true;
            }
        return false;
    }

    // starts channelling the fangs attack
    @Override
    public void performUnitRangedAttack(LivingEntity pTarget, float velocity) {
        if (isUsingLineFangs) {
            this.getCastFangsGoal().startCasting();
            this.getCastFangsGoal().setAbility(this.abilities.get().get(0));
            this.getCastFangsGoal().setTarget(pTarget);
        } else {
            this.getCastFangsGoal().startCasting();
            this.getCastFangsGoal().setAbility(this.abilities.get().get(1));
            this.getCastFangsGoal().setTarget(pTarget);
        }
        if (!level().isClientSide() && pTarget instanceof Unit unit)
            FogOfWarClientboundPacket.revealRangedUnit(unit.getOwnerName(), this.getId());
    }

    // actually performs the fangs attack
    public void createEvokerFangs(LivingEntity targetEntity) {
        if (isUsingLineFangs) {
            createEvokerFangsLine(targetEntity);
        } else {
            createEvokerFangsCircle();
        }
    }

    // based on Evoker.EvokerAttackSpellGoal.performSpellCasting
    public void createEvokerFangsLine(LivingEntity targetEntity) {
        double d0 = Math.min(targetEntity.getOnPos().getY(), this.getY());
        double d1 = Math.max(targetEntity.getOnPos().getY(), this.getY()) + 1.0;
        float f = (float)Mth.atan2(targetEntity.getZ() - this.getZ(), targetEntity.getX() - this.getX());
        int k;
        for(k = 0; k < FANGS_RANGE_LINE; ++k) {
            double d2 = 1.25 * (double)(k + 1);
            createEvokerFang(this.getX() + (double)Mth.cos(f) * d2, this.getZ() + (double)Mth.sin(f) * d2, d0, d1, f, k);
        }
        lastCastedCircleFangs = false;
    }

    // based on Evoker.EvokerAttackSpellGoal.performSpellCasting
    public void createEvokerFangsCircle() {
        int k;
        float f2;
        for(k = 0; k < 5; ++k) {
            f2 = (float)k * (float) Math.PI * 0.4F;
            createEvokerFang(this.getX() + (double)Mth.cos(f2) * 1.5, this.getZ() + (double)Mth.sin(f2) * 1.5, this.getY(), this.getY() + 1, f2, 0);
        }
        for(k = 0; k < 8; ++k) {
            f2 = (float)k * (float) Math.PI * 2.0F / 8.0F + 1.2566371F;
            createEvokerFang(this.getX() + (double)Mth.cos(f2) * 2.5, this.getZ() + (double)Mth.sin(f2) * 2.5, this.getY(), this.getY() + 1, f2, 3);
        }
        lastCastedCircleFangs = true;
    }

    // based on Evoker.EvokerAttackSpellGoal.createSpellEntity
    private void createEvokerFang(double pX, double pZ, double pMinY, double pMaxY, float pYRot, int pWarmupDelay) {
        BlockPos blockpos = new BlockPos((int) pX, (int) pMaxY, (int) pZ);
        boolean flag = false;
        double d0 = 0.0;

        do {
            BlockPos blockpos1 = blockpos.below();
            BlockState blockstate = this.level().getBlockState(blockpos1);
            if (blockstate.isFaceSturdy(this.level(), blockpos1, Direction.UP)) {
                if (!this.level().isEmptyBlock(blockpos)) {
                    BlockState blockstate1 = this.level().getBlockState(blockpos);
                    VoxelShape voxelshape = blockstate1.getCollisionShape(this.level(), blockpos);
                    if (!voxelshape.isEmpty())
                        d0 = voxelshape.max(Direction.Axis.Y);
                }
                flag = true;
                break;
            }
            blockpos = blockpos.below();
        } while(blockpos.getY() >= Mth.floor(pMinY) - 1);

        if (flag) {
            EvokerFangs fangs = new EvokerFangs(this.level(), pX, (double)blockpos.getY() + d0, pZ, pYRot, pWarmupDelay, this);
            this.level().addFreshEntity(fangs);
        }
    }

    public int getVexTargetRange() {
        if (GarrisonableBuilding.getGarrison(this) != null)
            return VEX_TARGET_RANGE_GARRISON;
        return VEX_TARGET_RANGE;
    }

    public void summonVexes() {
        if (this.level().isClientSide())
            return;

        for(int i = 0; i < SUMMON_VEXES_AMOUNT; ++i) {
            BlockPos blockpos = this.blockPosition().offset(-2 + this.random.nextInt(5), 1, -2 + this.random.nextInt(5));
            Vex vex = EntityType.VEX.create(this.level());
            if (vex != null) {
                vex.moveTo(blockpos, 0.0F, 0.0F);
                vex.finalizeSpawn((ServerLevel) this.level(), this.level().getCurrentDifficultyAt(blockpos), MobSpawnType.MOB_SUMMONED, null, null);
                vex.setOwner(this);
                vex.setBoundOrigin(blockpos);
                vex.setLimitedLife(CastSummonVexes.VEX_DURATION_SECONDS * ResourceCost.TICKS_PER_SECOND);
                ((ServerLevel) this.level()).addFreshEntityWithPassengers(vex);
            }
        }
        for (Ability ability : getAbilities().get())
            if (ability instanceof CastSummonVexes)
                AbilityClientboundPacket.sendSetCooldownPacket(getId(), UnitAction.CAST_SUMMON_VEXES, CastSummonVexes.CD_MAX_SECONDS * 20);

        if (getCastFangsGoal() != null)
            getCastFangsGoal().stopCasting();
    }

    public VillagerUnitModel.ArmPose getEvokerArmPose() {
        Entity targetEntity = getTargetGoal().getTarget();
        if (this.isCastingSpell() || (targetEntity != null &&
            this.distanceTo(targetEntity) <= getAttackRange()) &&
            this.getAbilities().get().get(0).isOffCooldown(this)) {
            return VillagerUnitModel.ArmPose.SPELLCASTING;
        }
        return VillagerUnitModel.ArmPose.CROSSED;
    }

    @Override
    public void setupEquipmentAndUpgradesClient() {

    }

    @Override
    public void setupEquipmentAndUpgradesServer() {
        if (hasAnyEnchant())
            return;

        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    public boolean hasAnyEnchant() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        return !itemStack.getAllEnchantments().isEmpty();
    }

    public boolean hasVigorEnchant() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        return itemStack.getAllEnchantments().containsKey(EnchantVigor.actualEnchantment);
    }

    public Enchantment getEnchant() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        Optional<Enchantment> enchant = itemStack.getAllEnchantments().keySet().stream().findFirst();
        return enchant.orElse(null);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        return pSpawnData;
    }

    @Override
    public boolean hasBonusAttackSpeed() {
        return hasVigorEnchant();
    }




}
