package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.MountRavager;
import com.solegendary.reignofnether.ability.abilities.PromoteIllager;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.faction.Faction;

import com.solegendary.reignofnether.util.ParticleUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

// despite being a RangedAttackerUnit we don't implement performRangedAttack as we override the Pillager crossbow attack instead
// we just implement this for fog reveal methods
public class PillagerUnit extends Pillager implements Unit, AttackerUnit, RangedAttackerUnit {
    public static final Abilities ABILITIES = new Abilities();
    static {
        ABILITIES.add(new MountRavager(), Keybindings.abilitySlot1);
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

    UnitItemGoal itemGoal;
    @Override public UnitItemGoal getItemGoal() { return itemGoal; }

    UsePortalGoal usePortalGoal;
    public UsePortalGoal getUsePortalGoal() { return usePortalGoal; }
    public boolean canUsePortal() { return getUsePortalGoal() != null; }

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

    public String getOnDeathCommand() { return this.entityData.get(onDeathCommandDataAccessor); }
    public void setOnDeathCommand(String command) { this.entityData.set(onDeathCommandDataAccessor, command); }
    public static final EntityDataAccessor<String> onDeathCommandDataAccessor =
            SynchedEntityData.defineId(PillagerUnit.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
        this.entityData.define(scenarioRoleDataAccessor, -1);
        this.entityData.define(onDeathCommandDataAccessor, "");
    }

    // combat stats
    public boolean getWillRetaliate() { return willRetaliate; }
    public float getAttackCooldown() {return ((20 / AttackerUnit.super.getNonBaseAttacksPerSecond()) * getAttackCooldownMultiplier());}
    public float getBaseAttackCooldown() {return ((20 / AttackerUnit.super.getBaseAttacksPerSecond()) * getAttackCooldownMultiplier());}
    public float getAttacksPerSecond() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        return 20f / (getAttackCooldown() + (CrossbowItem.getChargeDuration(itemStack)));
    }
    public float getNonBaseAttacksPerSecond() {
        return 20f / (getAttackCooldown() + 35);
    }
    public float getBaseAttacksPerSecond() {
        return 20f / (getBaseAttackCooldown() + 35);
    }
    public boolean getAggressiveWhenIdle() { return aggressiveWhenIdle && !isVehicle(); }
    public float getUnitAttackDamage() {
        float baseDamage = AttackerUnit.super.getUnitAttackDamage();
        return isPassenger() ? baseDamage + 1 : baseDamage;
    }
    @Nullable
    public ResourceCost getCost() {return ResourceCosts.PILLAGER;}

    public boolean canAttackBuildings() { return getAttackBuildingGoal() != null && isPassenger(); }
    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    final static public float attackDamage = 8.0f;
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

    private RangedAttackGroundGoal<?> attackGroundGoal;
    @Override public RangedAttackGroundGoal<?> getRangedAttackGroundGoal() {
        return attackGroundGoal;
    }

    // ---- siege mode (driven by the ravager this pillager is riding) ----
    private static final UUID SIEGE_RANGE_UUID = UUID.fromString("5d1a7a3e-4c0e-4a7e-9b52-1f6a2c9e7b11");
    private static final double SIEGE_RANGE_BONUS = 1.0; // +100% of base = doubled

    public static final float SIEGE_MIN_ATTACK_RANGE = 6;

    @Override
    public float getMinAttackRange() {
        return isSieging() ? SIEGE_MIN_ATTACK_RANGE : 0;
    }

    public boolean isSieging() {
        return getVehicle() instanceof RavagerUnit r && r.getSiegeState() == RavagerUnit.SiegeState.SIEGED;
    }

    // attack range while sieging, whether or not currently sieging (eg. for previewing it)
    public float getSiegeAttackRange() {
        return isSieging() ? getAttackRange() : getAttackRange() + getBaseAttackRange() * (float) SIEGE_RANGE_BONUS;
    }

    private void updateSiegeModifiers() {
        boolean sieging = isSieging();
        for (Supplier<Attribute> sup : List.<Supplier<Attribute>>of(
                AttributeRegistrar.ATTACK_RANGE, AttributeRegistrar.AGGRO_RANGE, AttributeRegistrar.SIGHT_RANGE)) {
            AttributeInstance ai = getAttribute(sup.get());
            if (ai == null) continue;
            boolean has = ai.getModifier(SIEGE_RANGE_UUID) != null;
            if (sieging && !has)
                ai.addTransientModifier(new AttributeModifier(SIEGE_RANGE_UUID, "siege_mode",
                        sup == AttributeRegistrar.SIGHT_RANGE ? 0.5f : 1.0f,
                        AttributeModifier.Operation.MULTIPLY_BASE));
            else if (!sieging && has)
                ai.removeModifier(SIEGE_RANGE_UUID);
        }
    }

    // orange spark trail on siege arrows. Entity tags aren't synced to clients, so the pillager sends the particles
    private static final DustParticleOptions SIEGE_TRAIL_PARTICLE =
            new DustParticleOptions(new Vector3f(1.0f,1.0f,1.0f), 1.0f);
    private static final int SIEGE_TRAIL_MAX_TICKS = 10 * ResourceCost.TICKS_PER_SECOND;
    private final Map<Projectile, Vec3> siegeShots = new HashMap<>(); // arrow -> its position last tick

    // multishot extras are fired at -10/+10 degrees, so only the primary arrow (angle 0) counts as the siege shot
    private void markSiegeShot(Projectile p, float pProjectileAngle) {
        if (isSieging() && pProjectileAngle == 0.0F) {
            p.addTag(RangedAttackerUnit.SIEGE_ARROW_TAG);
            siegeShots.put(p, p.position());
        }
    }

    private void tickSiegeShotTrails() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        siegeShots.entrySet().removeIf(e -> {
            Projectile p = e.getKey();
            // stop once the arrow is gone or has landed (stopped moving)
            if (p.isRemoved() || p.tickCount > SIEGE_TRAIL_MAX_TICKS)
                return true;
            if (p.tickCount > 1 && p.position().equals(e.getValue()))
                return true;
            e.setValue(p.position());
            serverLevel.sendParticles(SIEGE_TRAIL_PARTICLE, p.getX(), p.getY(), p.getZ(), 3, 0.05, 0.05, 0.05, 0);
            return false;
        });
    }

    public PillagerUnit(EntityType<? extends Pillager> entityType, Level level) {
        super(entityType, level);
        updateAbilityButtons();
    }

    @Override
    public void setTarget(@Nullable LivingEntity pTarget) {
        super.setTarget(pTarget);
        if (pTarget != null && getRangedAttackGroundGoal() != null)
            getRangedAttackGroundGoal().stop();
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Unit.createDefaultAttributes()
                .add(Attributes.MOVEMENT_SPEED, PillagerUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, PillagerUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, PillagerUnit.armorValue)
                .add(AttributeRegistrar.ATTACK_DAMAGE.get(), attackDamage)
                .add(AttributeRegistrar.ATTACKS_PER_SECOND.get(), attacksPerSecond)
                .add(AttributeRegistrar.ATTACK_RANGE.get(), attackRange)
                .add(AttributeRegistrar.AGGRO_RANGE.get(), aggroRange)
                .add(AttributeRegistrar.SIGHT_RANGE.get(), 18)
                .add(AttributeRegistrar.RANGED_DAMAGE_RESIST.get(), 0)
                .add(AttributeRegistrar.MAGIC_DAMAGE_RESIST.get(), 0);
    }

    public void tick() {
        this.setCanPickUpLoot(true);
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);
        updateSiegeModifiers();
        tickSiegeShotTrails();
        this.mountGoal.tick();
        PromoteIllager.checkAndApplyBuff(this);
        this.attackGoal.tickChargeCrossbow();
        if (attackGroundGoal != null)
            attackGroundGoal.tick();
    }

    @Override
    public void remove(@NotNull RemovalReason pReason) {
        this.runOnDeathCommand(pReason);
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

    public void initialiseGoals() {
        this.usePortalGoal = new UsePortalGoal(this);
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, false);
        this.garrisonGoal = new GarrisonGoal(this);
        this.itemGoal = new UnitItemGoal(this);
        this.attackGoal = new UnitCrossbowAttackGoal<>(this, (int) getAttackCooldown());
        this.returnResourcesGoal = new ReturnResourcesGoal(this);
        this.mountGoal = new MountGoal(this);
        this.attackBuildingGoal = new RangedAttackBuildingGoal<>(this, this.attackGoal);
        this.attackGroundGoal = new RangedAttackGroundGoal<>(this, true, this.attackGoal);
    }

    @Override
    public void resetBehaviours() {
        this.mountGoal.stop();
        if (this.attackGoal != null)
            this.attackGoal.stop();
        if (this.attackGroundGoal != null)
            this.attackGroundGoal.stop();
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
        this.goalSelector.addGoal(2, attackGroundGoal);
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

    public void unloadCrossbow() {
        if (this.attackGoal != null)
            this.attackGoal.unloadCrossbow();
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
            shootExtraMultishotArrows(pUser, itemstack, pVelocity);
            playCrossbowShootSound();
        }
        this.onCrossbowAttackPerformed();
        getMainHandItem().setDamageValue(0);
    }

    // vanilla multishot always fires 3 arrows (0, -10, +10 degrees) regardless of level, so higher levels add a further
    // pair of arrows per level at +/-10 degrees more each, ie. level n fires 2n + 1 arrows
    private void shootExtraMultishotArrows(LivingEntity pUser, ItemStack crossbow, float pVelocity) {
        int multishotLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, crossbow);
        if (multishotLevel <= 1 || level().isClientSide())
            return;

        ItemStack ammo = pUser.getProjectile(crossbow);
        ArrowItem arrowItem = ammo.getItem() instanceof ArrowItem ai ? ai : (ArrowItem) Items.ARROW;
        int pierceLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, crossbow);

        for (int tier = 2; tier <= multishotLevel; tier++) {
            for (float angle : new float[]{-10.0F * tier, 10.0F * tier}) {
                AbstractArrow arrow = arrowItem.createArrow(level(), ammo.isEmpty() ? new ItemStack(Items.ARROW) : ammo, pUser);
                arrow.setShotFromCrossbow(true);
                arrow.setCritArrow(true);
                arrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                if (pierceLevel > 0)
                    arrow.setPierceLevel((byte) pierceLevel);
                shootCrossbowProjectile(pUser, getTarget(), arrow, angle, pVelocity);
                level().addFreshEntity(arrow);
            }
        }
    }

    @Override
    public void shootCrossbowProjectile(LivingEntity pUser, LivingEntity pTarget, Projectile pProjectile, float pProjectileAngle, float pVelocity) {
        RangedAttackerUnit.tagIfMultishot(pProjectile, pProjectileAngle);
        if (isPassenger())
            pProjectile.addTag(RangedAttackerUnit.EXPLOSIVE_ARROW_TAG);

        // bit of a hacky fix to attack buildings since this function is called from CrossbowItem
        try {
            if (pTarget == null) {
                if (this.getAttackBuildingGoal() instanceof RangedAttackBuildingGoal<?> rabg && rabg.getBuildingTarget() != null) {
                    shootCrossbowProjectileAtBuilding(pUser, rabg, pProjectile, pProjectileAngle, pVelocity);
                    return;
                }
                if (this.getRangedAttackGroundGoal() != null && this.getRangedAttackGroundGoal().getGroundTarget() != null) {
                    shootCrossbowProjectileAtGround(pUser, this.getRangedAttackGroundGoal(), pProjectile, pProjectileAngle, pVelocity);
                    return;
                }
            }
        } catch (NullPointerException e) {
            ReignOfNether.LOGGER.error("Caught NullPointerException in shootCrossbowProjectile", e);
        }
        if (pTarget == null)
            return;

        // siege mode: lob at units in an upward arc
        if (isSieging()) {
            double sdx = pTarget.getX() - pUser.getX();
            double sdz = pTarget.getZ() - pUser.getZ();
            Vector3f lob = this.getProjectileShotVector(pUser, new Vec3(sdx, 75, sdz), pProjectileAngle);
            markSiegeShot(pProjectile, pProjectileAngle);
            pProjectile.shoot(lob.x(), lob.y(), lob.z(), pVelocity * 1.09f, 0);
            ParticleUtil.addParticleExplosion(ParticleTypes.POOF, 5, level(), getEyePosition());
        } else {
            double d0 = pTarget.getX() - pUser.getX();
            double d1 = pTarget.getZ() - pUser.getZ();
            double d2 = Math.sqrt(d0 * d0 + d1 * d1);
            double d3 = pTarget.getY(0.3333333333333333) - pProjectile.getY() + d2 * 0.20000000298023224;
            if (pTarget.getEyeHeight() <= 1.0f)
                d1 -= (1.0f - pTarget.getEyeHeight());
            Vector3f vector3f = this.getProjectileShotVector(pUser, new Vec3(d0, d3, d1), pProjectileAngle);
            pProjectile.shoot(vector3f.x(), vector3f.y(), vector3f.z(), pVelocity, 0);
        }
        if (!level().isClientSide() && pTarget instanceof Unit unit)
            FogOfWarClientboundPacket.revealRangedUnit(unit.getOwnerName(), this.getId());
    }

    private void playCrossbowShootSound() {
        if (isSieging()) {
            this.playSound(SoundEvents.FIREWORK_ROCKET_SHOOT, 1.0f, 0);
        }
        this.playSound(SoundEvents.CROSSBOW_SHOOT, 3.0F, 0);
    }

    private void shootCrossbowProjectileAtBuilding(LivingEntity pUser, RangedAttackBuildingGoal<?> rabg, Projectile pProjectile, float pProjectileAngle, float pVelocity) {
        if (isSieging()) {
            double d0 = rabg.getBuildingTarget().centrePos.getX() - pUser.getX() + 0.5f;
            double d1 = rabg.getBuildingTarget().centrePos.getZ() - pUser.getZ() + 0.5f;

            Vector3f vector3f = this.getProjectileShotVector(pUser, new Vec3(d0, 75, d1), pProjectileAngle);
            markSiegeShot(pProjectile, pProjectileAngle);
            pProjectile.shoot(vector3f.x(), vector3f.y(), vector3f.z(), pVelocity * 1.09f, 0);
        } else {
            // outside of siege mode, shoot straight at a placed block of the building (with the same drop compensation
            // as unit shots). crossbow shots don't go through the goal's own retargeting, so pick a new block per volley
            // on the primary arrow - multishot extras then fan out around the same block
            if (pProjectileAngle == 0.0F)
                rabg.setNextBlockTarget();
            BlockPos blockTarget = rabg.getBlockTarget() != null ? rabg.getBlockTarget() : rabg.getBuildingTarget().centrePos;
            Vec3 targetPos = Vec3.atCenterOf(blockTarget);
            double d0 = targetPos.x - pUser.getX();
            double d1 = targetPos.z - pUser.getZ();
            double d2 = Math.sqrt(d0 * d0 + d1 * d1);
            double d3 = targetPos.y - pProjectile.getY() + d2 * 0.2;

            Vector3f vector3f = this.getProjectileShotVector(pUser, new Vec3(d0, d3, d1), pProjectileAngle);
            pProjectile.shoot(vector3f.x(), vector3f.y(), vector3f.z(), pVelocity, 0);
        }

        if (!level().isClientSide())
            FogOfWarClientboundPacket.revealRangedUnit(rabg.getBuildingTarget().ownerName, this.getId());
    }

    private void shootCrossbowProjectileAtGround(LivingEntity pUser, RangedAttackGroundGoal<?> ragg, Projectile pProjectile, float pProjectileAngle, float pVelocity) {
        double d0 = ragg.getGroundTarget().getX() - pUser.getX() + 0.5f;
        double d1 = ragg.getGroundTarget().getZ() - pUser.getZ() + 0.5f;

        Vector3f vector3f = this.getProjectileShotVector(pUser, new Vec3(d0, 75, d1), pProjectileAngle);
        markSiegeShot(pProjectile, pProjectileAngle);
        pProjectile.shoot(vector3f.x(), vector3f.y(), vector3f.z(), pVelocity * 1.09f, 0);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        return pSpawnData;
    }
}