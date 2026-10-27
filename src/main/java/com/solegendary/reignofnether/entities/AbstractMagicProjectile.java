package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.ParticleUtil;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public abstract class AbstractMagicProjectile extends AbstractHurtingProjectile {

    private static final int MAX_TICKS = 60;
    private static final double SPEED = 1.0d; // blocks per tick
    private static final double MAX_TURN_PER_TICK = 0.2d; // radians
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(AbstractMagicProjectile.class, EntityDataSerializers.INT);
    private final SimpleParticleType particleType;

    public AbstractMagicProjectile(EntityType<? extends AbstractHurtingProjectile> pEntityType, Level pLevel, SimpleParticleType particleType) {
        super(pEntityType, pLevel);
        this.particleType = particleType;
    }

    public AbstractMagicProjectile(EntityType<? extends AbstractHurtingProjectile> pEntityType,
                                   LivingEntity pShooter, double offsetX, double offsetY, double offsetZ,
                                   Level pLevel, SimpleParticleType particleType) {
        super(pEntityType, pShooter, offsetX, offsetY, offsetZ, pLevel);
        this.particleType = particleType;
        // vanilla only stores the launch direction as an acceleration, so start moving along it right away
        setDeltaMovement(new Vec3(xPower, yPower, zPower).normalize().scale(SPEED));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TARGET_ID, -1);
    }

    public void setTarget(@Nullable LivingEntity target) {
        this.entityData.set(DATA_TARGET_ID, target == null ? -1 : target.getId());
    }

    @Nullable
    public LivingEntity getTarget() {
        if (level().getEntity(this.entityData.get(DATA_TARGET_ID)) instanceof LivingEntity le && le.isAlive())
            return le;
        return null;
    }

    @Override
    public void tick() {
        // AbstractHurtingProjectile constantly accelerates along the launch direction (xPower etc.), which would
        // fight any steering, so remove it and fully control the velocity ourselves each tick
        xPower = 0;
        yPower = 0;
        zPower = 0;

        Vec3 heading = getDeltaMovement().normalize();
        LivingEntity target = getTarget();
        if (target != null && heading.lengthSqr() > 0)
            heading = turnTowards(heading, target.getBoundingBox().getCenter().subtract(position()), MAX_TURN_PER_TICK);
        setDeltaMovement(heading.scale(SPEED));

        super.tick();
        if (tickCount > MAX_TICKS)
            this.discard();
    }

    // rotates the unit vector 'from' towards the direction 'to' by at most maxAngle radians
    private static Vec3 turnTowards(Vec3 from, Vec3 to, double maxAngle) {
        if (to.lengthSqr() < 1.0E-6)
            return from;
        Vec3 toDir = to.normalize();
        double dot = Mth.clamp(from.dot(toDir), -1, 1);
        double angle = Math.acos(dot);
        if (angle <= maxAngle)
            return toDir;
        Vec3 perp = toDir.subtract(from.scale(dot));
        if (perp.lengthSqr() < 1.0E-6) // directly opposite, no unique turning plane
            return from;
        return from.scale(Math.cos(maxAngle)).add(perp.normalize().scale(Math.sin(maxAngle))).normalize();
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        if (!this.level().isClientSide && this.getOwner() instanceof AttackerUnit aUnit) {
            pResult.getEntity().hurt(damageSources().mobProjectile(this, (LivingEntity) this.getOwner()), aUnit.getUnitAttackDamage());
            ParticleUtil.addParticleExplosion(particleType, 10, level(), position());
            discard();
        }
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return particleType;
    }


    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        return false;
    }
}
