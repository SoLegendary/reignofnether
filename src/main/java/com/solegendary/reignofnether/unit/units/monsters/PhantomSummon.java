package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ability.heroAbilities.necromancer.InsomniaCurse;
import com.solegendary.reignofnether.time.NightUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

import static com.solegendary.reignofnether.ability.heroAbilities.necromancer.InsomniaCurse.PHANTOM_MAX_ATTACKS;

// still not a unit, but has overrides to make

// TODO:
// - Must be able to set a specific entitity to attack
//      - Once that unit dies, the phantom disappears
// - Better control range and height of flight
// - Increase size of the phantoms according to spell level, which also scales damage

public class PhantomSummon extends Phantom {

    final static public float maxHealth = 50.0f;
    public int attacksLeft = PHANTOM_MAX_ATTACKS; // at 0, dies after a few seconds
    public int ticksToDie = 60; // starts counting down after attacksLeft <= 0
    public int tickCountUntilDeath = 1200; // hard cap on lifespan
    public int attackCooldown = 0;
    public final int ATTACK_COOLDOWN_MAX = 120;

    public LivingEntity entityTarget = null;

    public PhantomSummon(EntityType<? extends Phantom> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, InsomniaCurse.PHANTOM_DAMAGE)
                .add(Attributes.MAX_HEALTH, PhantomSummon.maxHealth);
    }

    @Override
    public void setPhantomSize(int pPhantomSize) {
        this.entityData.set(ID_SIZE, Mth.clamp(pPhantomSize, 0, 64));
    }

    @Override
    public void tick() {
        noPhysics = true;
        if (entityTarget != null && hasLineOfSight(entityTarget))
            anchorPoint = entityTarget.blockPosition();
        super.tick();

        if (getTarget() != entityTarget)
            setTarget(entityTarget);

        if (attacksLeft <= 0 || getTarget() == null || getTarget().isDeadOrDying() || getTarget().isRemoved())
            ticksToDie -= 1;
        if (ticksToDie <= 0 || tickCount > tickCountUntilDeath)
            kill();

        if (attackCooldown > 0)
            attackCooldown -= 1;
    }

    @Override
    public boolean hasLineOfSight(Entity pEntity) {
        if (attackCooldown > 0)
            return false;
        if (pEntity == entityTarget)
            return true;
        return super.hasLineOfSight(pEntity);
    }

    /*
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PhantomAttackStrategyGoal());
        this.goalSelector.addGoal(2, new PhantomSweepAttackGoal());
        this.goalSelector.addGoal(3, new PhantomCircleAroundAnchorGoal());
        this.targetSelector.addGoal(1, new PhantomAttackPlayerTargetGoal());
    }
     */

    @Override
    protected boolean isSunBurnTick() {
        return NightUtils.isSunBurnTick(this);
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        if (super.doHurtTarget(pEntity)) {
            attackCooldown = ATTACK_COOLDOWN_MAX;
            attacksLeft -= 1;
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        return pSpawnData;
    }
}
