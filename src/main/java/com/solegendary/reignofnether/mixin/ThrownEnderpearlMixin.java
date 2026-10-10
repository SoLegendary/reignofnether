package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin extends ThrowableItemProjectile {

    public ThrownEnderpearlMixin(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(
            method = "onHit",
            at = @At("HEAD")
    )
    protected void onHit(HitResult pResult, CallbackInfo ci) {
        if (!this.isRemoved() && this.getOwner() instanceof Unit) {
            this.level().playSound(null, this.xo, this.yo, this.zo, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0F, 1.0F);
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 3.0F, 1.0F);
        }
    }
}
