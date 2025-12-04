package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class ThrowableTntProjectile extends ThrowableItemProjectile {

    private float explosionPower = 2.0f;

    public ThrowableTntProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ThrowableTntProjectile(Level pLevel, LivingEntity livingEntity, float explosionPower) {
        super(EntityRegistrar.THROWABLE_TNT_PROJECTILE.get(), livingEntity, pLevel);
        this.explosionPower = explosionPower;
    }

    @Override
    protected Item getDefaultItem() {
        return ItemRegistrar.THROWABLE_TNT.get();
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        if(!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, ((byte) 3));
            AdjustablePrimedTnt primedtnt = new AdjustablePrimedTnt(level(), getX(), getY(), getZ(), explosionPower, (LivingEntity) this.getOwner());
            level().addFreshEntity(primedtnt);
            level().playSound(null, primedtnt.getX(), primedtnt.getY(), primedtnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            level().gameEvent(this.getOwner(), GameEvent.PRIME_FUSE, pResult.getBlockPos());
        }

        this.discard();
        super.onHitBlock(pResult);
    }
}
