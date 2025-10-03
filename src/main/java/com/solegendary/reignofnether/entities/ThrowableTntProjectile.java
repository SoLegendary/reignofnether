package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.ability.heroAbilities.piglin.ThrowTNT;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.unit.units.piglins.PiglinMerchantUnit;
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

    public ThrowableTntProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ThrowableTntProjectile(Level pLevel, LivingEntity livingEntity) {
        super(EntityRegistrar.THROWABLE_TNT_PROJECTILE.get(), livingEntity, pLevel);
    }

    @Override
    protected Item getDefaultItem() {
        return ItemRegistrar.THROWABLE_TNT.get();
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        if(!this.level().isClientSide() && this.getOwner() instanceof LivingEntity) {
            this.level().broadcastEntityEvent(this, ((byte) 3));

            float explosionPower = 4.0f;
            if (this.getOwner() instanceof PiglinMerchantUnit piglinMerchantUnit) {
                ThrowTNT throwTNT = piglinMerchantUnit.getThrowTNT();
                if (throwTNT != null)
                    explosionPower = throwTNT.explosionPower;
            }
            AdjustablePrimedTnt primedtnt = new AdjustablePrimedTnt(level(), getX(), getY(), getZ(), explosionPower, (LivingEntity) this.getOwner());
            level().addFreshEntity(primedtnt);
            level().playSound(null, primedtnt.getX(), primedtnt.getY(), primedtnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            level().gameEvent(this.getOwner(), GameEvent.PRIME_FUSE, pResult.getBlockPos());
        }

        this.discard();
        super.onHitBlock(pResult);
    }
}
