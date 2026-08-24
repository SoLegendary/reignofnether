package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.registrars.EntityRegistrar;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;

public class NecromancerProjectile extends AbstractMagicProjectile {

    public NecromancerProjectile(EntityType<? extends AbstractHurtingProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel, ParticleTypes.WITCH);
    }

    public NecromancerProjectile(Level pLevel, LivingEntity pShooter, double offsetX, double offsetY, double offsetZ) {
        super(EntityRegistrar.NECROMANCER_PROJECTILE.get(), pShooter, offsetX, offsetY, offsetZ, pLevel, ParticleTypes.WITCH);
    }
}
