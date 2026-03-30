package com.solegendary.reignofnether.unit.units.piglins;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.AttackGround;
import com.solegendary.reignofnether.entities.GhastUnitFireball;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.controls.GhastUnitMoveControl;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class GhastUnit extends Ghast implements Unit, AttackerUnit, RangedAttackerUnit {
    final static public float attackRange = 30; // only used by ranged units or melee building attackers

    public static final Abilities ABILITIES = new Abilities();
    static {
        ABILITIES.add(new AttackGround(attackRange), Keybindings.keyQ);
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

    MoveToTargetBlockGoal usePortalGoal;
    public MoveToTargetBlockGoal getUsePortalGoal() { return usePortalGoal; }
    public boolean canUsePortal() { return getUsePortalGoal() != null; }

    public Faction getFaction() {return Faction.PIGLINS;}
    public Abilities getAbilities() {return abilities;}
    public List<ItemStack> getItems() {return items;};
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}
    public MountGoal getMountGoal() {return mountGoal;}

    private MoveToTargetBlockGoal moveGoal;
    private SelectedTargetGoal<? extends LivingEntity> targetGoal;
    private ReturnResourcesGoal returnResourcesGoal;
    public MountGoal mountGoal;

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
            SynchedEntityData.defineId(GhastUnit.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(GhastUnit.class, EntityDataSerializers.INT);

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
    public ResourceCost getCost() {return ResourceCosts.GHAST;}
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

    private UnitBowAttackGoal<? extends LivingEntity> attackGoal;

    // endregion

    final static public float attackDamage = 8.0f;
    final static public float attacksPerSecond = 0.15f;
    final static public float aggroRange = 30;
    final static public boolean willRetaliate = true; // will attack when hurt by an enemy
    final static public boolean aggressiveWhenIdle = true;

    final static public float maxHealth = 50.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.22f;
    public int maxResources = 100;

    public int fogRevealDuration = 0; // set > 0 for the client who is attacked by this unit
    public int getFogRevealDuration() { return fogRevealDuration; }
    public void setFogRevealDuration(int duration) { fogRevealDuration = duration; }

    private Abilities abilities = ABILITIES.clone();
    private final List<ItemStack> items = new ArrayList<>();

    public static final int EXPLOSION_POWER = 2;
    public static final int FIREBALL_FIRE_BLOCKS = 2;

    private static final int SHOOTING_FACE_TICKS_MAX = 14;
    private int shootingFaceTicksLeft = 0;
    public boolean isShooting() { return shootingFaceTicksLeft > 0; }
    public void showShootingFace() { shootingFaceTicksLeft = SHOOTING_FACE_TICKS_MAX; }

    private RangedFlyingAttackGroundGoal<?> attackGroundGoal;
    @Override public RangedFlyingAttackGroundGoal<?> getRangedAttackGroundGoal() {
        return attackGroundGoal;
    }
    private RangedAttackBuildingGoal<?> attackBuildingGoal;

    public GhastUnit(EntityType<? extends Ghast> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new GhastUnitMoveControl(this);

        updateAbilityButtons();
    }

    @Override
    public float getDamageAfterMagicAbsorb(DamageSource pSource, float pDamage) {
        pDamage = super.getDamageAfterMagicAbsorb(pSource, pDamage);
        if (pSource.is(DamageTypeTags.WITCH_RESISTANT_TO) || pSource.is(DamageTypes.ON_FIRE))
            pDamage *= 0.5F;
        return pDamage;
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, GhastUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, GhastUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, GhastUnit.maxHealth)
                .add(Attributes.ARMOR, GhastUnit.armorValue);
    }

    @Override // prevent vanilla logic for picking up items
    protected void pickUpItem(ItemEntity pItemEntity) { }

    @Override // destroy touching leaves, copied from Ravager AI
    protected void customServerAiStep() {
        if (this.isAlive() && this.horizontalCollision && ForgeEventFactory.getMobGriefingEvent(this.level(), this)) {
            boolean flag = false;
            AABB aabb = this.getBoundingBox().inflate(0.2);
            Iterator var8 = BlockPos.betweenClosed(Mth.floor(aabb.minX), Mth.floor(aabb.minY), Mth.floor(aabb.minZ), Mth.floor(aabb.maxX), Mth.floor(aabb.maxY), Mth.floor(aabb.maxZ)).iterator();

            label62:
            while(true) {
                BlockPos blockpos;
                Block block;
                do {
                    if (!var8.hasNext())
                        break label62;
                    blockpos = (BlockPos)var8.next();
                    BlockState blockstate = this.level().getBlockState(blockpos);
                    block = blockstate.getBlock();
                } while(!(block instanceof LeavesBlock));

                flag = this.level().destroyBlock(blockpos, true, this) || flag;
            }
        }
    }
    @Override
    public LivingEntity getTarget() {
        return this.targetGoal.getTarget();
    }

    public void tick() {
        if (tickCount % 10 == 0) {
            BlockState lowestBs = level().getBlockState(MiscUtil.getHighestNonAirBlock(level(), blockPosition(), false, false));
            if (lowestBs.isAir() || lowestBs.getBlock() == Blocks.STRUCTURE_VOID) {
                addEffect(new MobEffectInstance(MobEffectRegistrar.DISARM.get(), 15, 1, true, false));
            }
        }

        this.setCanPickUpLoot(false);
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);

        if (attackGroundGoal != null)
            attackGroundGoal.tick();

        if (shootingFaceTicksLeft > 0)
            shootingFaceTicksLeft -= 1;

        updateRotation();
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

    public void updateRotation() {
        LivingEntity target = this.getTarget();
        BlockPos groundTarget = this.getRangedAttackGroundGoal().getGroundTarget();
        Vec3 dMove = this.getDeltaMovement();
        double x = 0;
        double z = 0;

        if (target != null) {
            x = target.getX() - this.getX();
            z = target.getZ() - this.getZ();
        } else if (groundTarget != null) {
            x = groundTarget.getX() - this.getX();
            z = groundTarget.getZ() - this.getZ();
        } else if (dMove.distanceTo(new Vec3(0,0,0)) > 0) {
            x = dMove.x();
            z = dMove.z();
        }

        if (Math.abs(x) > 0.05f || Math.abs(z) > 0.05f) {
            this.setYRot(-((float)Mth.atan2(x, z)) * 57.295776F);
            this.yBodyRot = this.getYRot();
        }
    }

    public void initialiseGoals() {
        this.usePortalGoal = new FlyingUsePortalGoal(this);
        this.moveGoal = new FlyingMoveToTargetGoal(this, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.attackGoal = new UnitBowAttackGoal<>(this);
        this.attackGroundGoal = new RangedFlyingAttackGroundGoal<>(this, this.attackGoal);
        this.attackBuildingGoal = new RangedAttackBuildingGoal<>(this, this.attackGoal);
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();
        this.goalSelector.addGoal(2, usePortalGoal);
        this.goalSelector.addGoal(2, attackBuildingGoal);
        this.goalSelector.addGoal(2, attackGroundGoal);
        this.goalSelector.addGoal(2, attackGoal);
        this.targetSelector.addGoal(2, targetGoal);
        this.goalSelector.addGoal(3, moveGoal);
    }

    // for some reason this.getNavigation().stop(); doesn't stop spider units from moving
    @Override
    public void resetBehaviours() {
        this.getRangedAttackGroundGoal().stop();
    }

    @Override
    public void performUnitRangedAttack(LivingEntity pTarget, float velocity) {
        double x = pTarget.getX();
        double y = pTarget.getY();
        double z = pTarget.getZ();
        performUnitRangedAttack(x, y, z, velocity);

        if (!level().isClientSide() && pTarget instanceof Unit unit)
            FogOfWarClientboundPacket.revealRangedUnit(unit.getOwnerName(), this.getId());
    }

    @Override
    public void performUnitRangedAttack(double x, double y, double z, float velocity) {
        if (this.hasEffect(MobEffectRegistrar.DISARM.get()))
            return;

        Vec3 viewVec = this.getViewVector(1.0F);
        double tx = x - (this.getX() + viewVec.x * 4.0);
        double ty = y - (0.5 + this.getY(0.5));
        double tz = z - (this.getZ() + viewVec.z * 4.0);
        if (!this.isSilent()) {
            this.level().levelEvent(null, 1016, this.blockPosition(), 0);
        }
        LargeFireball fireball = new GhastUnitFireball(this.level(), this, tx, ty, tz, EXPLOSION_POWER);
        fireball.setInvulnerable(true);
        fireball.setPos(this.getX() + viewVec.x * 4.0, this.getY(0.5) + 0.5, fireball.getZ() + viewVec.z * 4.0);
        this.playSound(SoundEvents.GHAST_WARN, 3.0F, 1.0F);
        this.level().addFreshEntity(fireball);
        UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.NON_KEYFRAME_START, this);
    }

    // range bonus that an attacker gets when targeting this ghast, so that we can't just float high up out of range
    public int getAttackerRangeBonus(Mob attacker) {
        Vec2 attackerPos = new Vec2((float) attacker.getX(), (float) attacker.getZ());
        Vec2 ghastPos = new Vec2((float) this.getX(), (float) this.getZ());
        double horizDist = Math.sqrt(attackerPos.distanceToSqr(ghastPos));
        double vertiDist = Math.max(0, this.getY() - attacker.getY());

        // if we're directly under the ghast, just allow anything to attack it
        if (horizDist < 4)
            return (int) vertiDist;
        else
            return (int) (vertiDist * 0.5f);
    }

    @Override
    public void setupEquipmentAndUpgradesClient() {

    }

    @Override
    public void setupEquipmentAndUpgradesServer() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    @Override
    public List<FormattedCharSequence> getAttackDamageStatTooltip() {
        return List.of(
                fcs(I18n.get("unitstats.reignofnether.attack_damage"), true),
                fcs(I18n.get("unitstats.reignofnether.attack_damage_bonus_buildings", "300%"))
        );
    }
    @Override
    public boolean hasBonusDamage() {
        return true;
    }




}
