package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.ability.heroAbilities.wildfire.MoltenBomb;
import com.solegendary.reignofnether.blocks.BlockServerEvents;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.WildfireUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;

import java.util.HashMap;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.isSolidBlocking;

public class MoltenBombProjectile extends Fireball {

    private int maxTicks = 200;
    private int explosionRadius = MoltenBomb.RADIUS_RANK_1;

    public MoltenBombProjectile(EntityType<? extends Fireball> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public MoltenBombProjectile(Level pLevel, LivingEntity pShooter, double offsetX, double offsetY, double offsetZ) {
        super(EntityRegistrar.MOLTEN_BOMB_PROJECTILE.get(), pShooter, offsetX, offsetY, offsetZ, pLevel);
        if (!(getOwner() instanceof WildfireUnit))
            return;
        MoltenBomb moltenBomb = ((WildfireUnit) getOwner()).getMoltenBomb();
        this.explosionRadius = (int) moltenBomb.radius;
    }

    public void setMaxTicks(int ticks) {
        this.maxTicks = ticks;
    }

    @Override
    public void tick() {
        setDeltaMovement(getDeltaMovement().normalize().scale(0.5f));
        super.tick();
        if (tickCount > maxTicks)
            this.detonate();
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        if (this.level().isClientSide() || this.getOwner() == null)
            return;
        Relationship rl = UnitServerEvents.getUnitToEntityRelationship((Unit) this.getOwner(), pResult.getEntity());
        if (rl == Relationship.NEUTRAL || rl == Relationship.HOSTILE)
            detonate();
    }

    @Override
    public boolean ignoreExplosion() {
        return true;
    }

    protected void detonate() {
        level().explode(null, null, null, this.getX(), this.getEyeY(), this.getZ(),
                2.0f, false, Level.ExplosionInteraction.NONE);

        HashMap<BlockPos, Double> bpAndDists = new HashMap<>();

        for (int x = -explosionRadius; x < explosionRadius; x++)
            for (int y = -explosionRadius; y < explosionRadius; y++)
                for (int z = -explosionRadius; z < explosionRadius; z++) {
                    BlockPos bp = blockPosition().offset(x, y, z);
                    double distSqr = bp.distSqr(blockPosition());
                    double radiusSqr = explosionRadius * explosionRadius;
                    if (distSqr <= radiusSqr && level().getBlockState(bp).isSolid())
                        bpAndDists.put(bp, distSqr / radiusSqr);
                }

        for (BlockPos bp : bpAndDists.keySet()) {
            if (random.nextFloat() > bpAndDists.get(bp)) {
                BlockState fireState = Blocks.FIRE.defaultBlockState();
                if (getOwner() instanceof Blaze blaze && blaze.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get())) {
                    fireState = BlockRegistrar.UNEXTINGUISHABLE_SOUL_FIRE.get().defaultBlockState();
                }
                BlockServerEvents.addTempBlock(
                        (ServerLevel) level(),
                        bp,
                        BlockRegistrar.WALKABLE_MAGMA_BLOCK.get().defaultBlockState(),
                        level().getBlockState(bp),
                        random.nextInt(MoltenBomb.MIN_MAGMA_DURATION, MoltenBomb.MAX_MAGMA_DURATION),
                        true,
                        random.nextFloat() < 0.333f && !isSolidBlocking(level(), bp.above()) ? fireState : null
                );
            }
        }
        List<Mob> mobs = MiscUtil.getEntitiesWithinRange(position(), explosionRadius, Mob.class, level());
        for (Mob mob : mobs) {
            mob.hurt(damageSources().mobProjectile(this, (LivingEntity) this.getOwner()), 0.5f);
            if (random.nextBoolean())
                mob.setSecondsOnFire(5);
            if (this.getOwner() instanceof LivingEntity le && le.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get())) {
                mob.addEffect(new MobEffectInstance(MobEffectRegistrar.SOULS_AFLAME.get(), 120, 0, false, false));
            }
        }
        MiscUtil.addParticleExplosion(ParticleTypes.LAVA, explosionRadius * 3, level(), position());
        discard();
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SMOKE;
    }
}
