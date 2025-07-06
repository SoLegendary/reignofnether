package com.solegendary.reignofnether.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class AdjustablePrimedTnt extends PrimedTnt {

    @Nullable
    private LivingEntity owner;

    private float explosionPower;

    private final int DEFAULT_FUSE_TICKS = 60;

    public AdjustablePrimedTnt(EntityType<? extends PrimedTnt> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public AdjustablePrimedTnt(Level pLevel, double pX, double pY, double pZ, float explosionPower, @Nullable LivingEntity pOwner) {
        this(EntityType.TNT, pLevel);
        this.setPos(pX, pY, pZ);
        double $$5 = pLevel.random.nextDouble() * 6.2831854820251465;
        this.setDeltaMovement(-Math.sin($$5) * 0.02, 0.20000000298023224, -Math.cos($$5) * 0.02);
        this.setFuse(DEFAULT_FUSE_TICKS);
        this.xo = pX;
        this.yo = pY;
        this.zo = pZ;
        this.owner = pOwner;
        this.explosionPower = explosionPower;
    }

    protected void explode() {
        this.level().explode(this, this.getX(), this.getY(0.0625), this.getZ(), explosionPower, Level.ExplosionInteraction.TNT);
    }
}
