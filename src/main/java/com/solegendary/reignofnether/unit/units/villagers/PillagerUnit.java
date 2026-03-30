package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.MountRavager;
import com.solegendary.reignofnether.ability.abilities.PromoteIllager;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// despite being a RangedAttackerUnit we don't implement performRangedAttack as we override the Pillager crossbow attack instead
// we just implement this for fog reveal methods
public class PillagerUnit extends Pillager implements Unit, AttackerUnit, RangedAttackerUnit {
    public static final Abilities ABILITIES = new Abilities();
    static {
        ABILITIES.add(new MountRavager(), Keybindings.keyQ);
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

    public Faction getFaction() { return Faction.VILLAGERS; }

    public Abilities getAbilities() { return abilities; }
    public List<ItemStack> getItems() { return items; }

    public MoveToTargetBlockGoal getMoveGoal() { return moveGoal; }
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() { return targetGoal; }
    public Goal getAttackBuildingGoal() { return attackBuildingGoal; }
    public Goal getAttackGoal() { return attackGoal; }
    public ReturnResourcesGoal getReturnResourcesGoal() { return returnResourcesGoal; }
    public int getMaxResources() { return maxResources; }
    public MountGoal getMountGoal() { return mountGoal; }

    private EnemySearchBehaviour attackSearchBehaviour = EnemySearchBehaviour.NONE;
    public EnemySearchBehaviour getEnemySearchBehaviour() { return attackSearchBehaviour; }
    public void setEnemySearchBehaviour(EnemySearchBehaviour behaviour) { attackSearchBehaviour = behaviour; }

    private MoveToTargetBlockGoal moveGoal;
    private SelectedTargetGoal<? extends LivingEntity> targetGoal;
    private ReturnResourcesGoal returnResourcesGoal;
    public MountGoal mountGoal;

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
    public String getOwnerName() {
        return this.entityData.get(ownerDataAccessor);
    }

    public void setOwnerName(String name) {
        this.entityData.set(ownerDataAccessor, name);
    }

    public static final EntityDataAccessor<String> ownerDataAccessor =
            SynchedEntityData.defineId(PillagerUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(PillagerUnit.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
        this.entityData.define(scenarioRoleDataAccessor, -1);
    }

    // combat stats
    public boolean getWillRetaliate() { return willRetaliate; }
    public float getAttackCooldown() {return ((20 / attacksPerSecond) * getAttackCooldownMultiplier());}
    public float getAttacksPerSecond() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        return 20f / (getAttackCooldown() + (CrossbowItem.getChargeDuration(itemStack)));
    }
    public float getBaseAttacksPerSecond() {
        return 20f / (getAttackCooldown() + 35);
    }
    public float getAggroRange() { return aggroRange; }
    public boolean getAggressiveWhenIdle() { return aggressiveWhenIdle && !isVehicle(); }
    public float getAttackRange() { return attackRange; }
    public float getMovementSpeed() { return movementSpeed; }
    public float getUnitAttackDamage() {
        return isPassenger() ? attackDamage + 1 : attackDamage;
    }
    public float getUnitMaxHealth() { return maxHealth; }
    @Nullable
    public ResourceCost getCost() {return ResourceCosts.PILLAGER;}

    public boolean canAttackBuildings() { return getAttackBuildingGoal() != null && isPassenger(); }
    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    final static public float attackDamage = 7.0f;
    final static public float attacksPerSecond = 0.632f; // excludes crossbow charge time
    final static public float maxHealth = 45.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.24f;
    final static public float attackRange = 16.0F; // only used by ranged units or melee building attackers
    final static public float aggroRange = 16;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;

    public int maxResources = 100;

    public int fogRevealDuration = 0; // set > 0 for the client who is attacked by this unit

    public int getFogRevealDuration() {
        return fogRevealDuration;
    }

    public void setFogRevealDuration(int duration) {
        fogRevealDuration = duration;
    }

    private UnitCrossbowAttackGoal<? extends LivingEntity> attackGoal;
    private RangedAttackBuildingGoal<?> attackBuildingGoal;

    private Abilities abilities = ABILITIES.clone();
    private final List<ItemStack> items = new ArrayList<>();

    public PillagerUnit(EntityType<? extends Pillager> entityType, Level level) {
        super(entityType, level);

        updateAbilityButtons();
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, PillagerUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, PillagerUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, PillagerUnit.armorValue);
    }

    public void tick() {
        this.setCanPickUpLoot(true);
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);
        this.mountGoal.tick();
        PromoteIllager.checkAndApplyBuff(this);
        this.attackGoal.tickChargeCrossbow();
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
        this.targetGoal = new SelectedTargetGoal<>(this, true, false);
        this.garrisonGoal = new GarrisonGoal(this);
        this.attackGoal = new UnitCrossbowAttackGoal<>(this, (int) getAttackCooldown());
        this.returnResourcesGoal = new ReturnResourcesGoal(this);
        this.mountGoal = new MountGoal(this);
        this.attackBuildingGoal = new RangedAttackBuildingGoal<>(this, this.attackGoal);
    }

    @Override
    public void resetBehaviours() {
        this.mountGoal.stop();
        if (this.attackGoal != null) {
            this.attackGoal.stop();
        }
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();
        this.goalSelector.addGoal(2, usePortalGoal);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, attackGoal);
        this.goalSelector.addGoal(2, returnResourcesGoal);
        this.goalSelector.addGoal(2, mountGoal);
        this.goalSelector.addGoal(2, garrisonGoal);
        this.targetSelector.addGoal(2, targetGoal);
        this.goalSelector.addGoal(3, moveGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundUnitGoal(this));
    }

    @Override
    public void setupEquipmentAndUpgradesServer() {
        if (!getMainHandItem().getAllEnchantments().isEmpty())
            return;

        ItemStack cbowStack = new ItemStack(Items.CROSSBOW);
        this.setItemSlot(EquipmentSlot.MAINHAND, cbowStack);
    }

    public Enchantment getEnchant() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        Optional<Enchantment> enchant = Optional.empty();
        for (Enchantment enchantment : itemStack.getAllEnchantments().keySet()) {
            enchant = Optional.of(enchantment);
            break;
        }
        return enchant.orElse(null);
    }

    // override to make inaccuracy 0
    @Override
    public void performCrossbowAttack(LivingEntity pUser, float pVelocity) {
        InteractionHand interactionhand = ProjectileUtil.getWeaponHoldingHand(pUser, (item) ->
                item instanceof CrossbowItem
        );
        ItemStack itemstack = pUser.getItemInHand(interactionhand);
        if (pUser.isHolding((is) -> is.getItem() instanceof CrossbowItem)) {
            CrossbowItem.performShooting(pUser.level(), pUser, interactionhand, itemstack, pVelocity, 0);
            this.playSound(SoundEvents.CROSSBOW_SHOOT, 3.0F, 0);
        }
        this.onCrossbowAttackPerformed();
        getMainHandItem().setDamageValue(0);
    }

    @Override
    public void shootCrossbowProjectile(LivingEntity pUser, LivingEntity pTarget, Projectile pProjectile, float pProjectileAngle, float pVelocity) {
        // bit of a hacky fix to attack buildings since this function is called from CrossbowItem
        try {
            if (pTarget == null && this.getAttackBuildingGoal() instanceof RangedAttackBuildingGoal<?> rabg) {
                shootCrossbowProjectileAtBuilding(pUser, rabg, pProjectile, pProjectileAngle, pVelocity);
                return;
            }
        } catch (NullPointerException e) {
            System.out.println("Caught NullPointerException in shootCrossbowProjectile: " + e.getMessage());
        }
        if (pTarget == null)
            return;

        double d0 = pTarget.getX() - pUser.getX();
        double d1 = pTarget.getZ() - pUser.getZ();
        double d2 = Math.sqrt(d0 * d0 + d1 * d1);
        double d3 = pTarget.getY(0.3333333333333333) - pProjectile.getY() + d2 * 0.20000000298023224;

        if (pTarget.getEyeHeight() <= 1.0f)
            d1 -= (1.0f - pTarget.getEyeHeight());

        Vector3f vector3f = this.getProjectileShotVector(pUser, new Vec3(d0, d3, d1), pProjectileAngle);
        pProjectile.shoot(vector3f.x(), vector3f.y(), vector3f.z(), pVelocity, 0);
        pUser.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (pUser.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level().isClientSide() && pTarget instanceof Unit unit)
            FogOfWarClientboundPacket.revealRangedUnit(unit.getOwnerName(), this.getId());
    }

    private void shootCrossbowProjectileAtBuilding(LivingEntity pUser, RangedAttackBuildingGoal<?> rabg, Projectile pProjectile, float pProjectileAngle, float pVelocity) {
        if (rabg.getBuildingTarget() == null) {
            return;
        }
        double d0 = rabg.getBuildingTarget().centrePos.getX() - pUser.getX();
        double d1 = rabg.getBuildingTarget().centrePos.getZ() - pUser.getZ();

        Vector3f vector3f = this.getProjectileShotVector(pUser, new Vec3(d0, 75, d1), pProjectileAngle);
        pProjectile.shoot(vector3f.x(), vector3f.y(), vector3f.z(), pVelocity, 0);
        pUser.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (pUser.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level().isClientSide())
            FogOfWarClientboundPacket.revealRangedUnit(rabg.getBuildingTarget().ownerName, this.getId());
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        return pSpawnData;
    }
}