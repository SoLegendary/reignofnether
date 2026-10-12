package com.solegendary.reignofnether.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;

@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin extends Projectile {

    // 1.20.1's ThrownTrident#tridentItem is gone. The trident is an AbstractArrow in 1.21.1, not a
    // ThrowableItemProjectile, and the stack it was thrown with is reachable through
    // getWeaponItem() (the same accessor vanilla uses for its enchantment effects).
    @Shadow public abstract ItemStack getWeaponItem();
    @Shadow private boolean dealtDamage;

    protected ThrownTridentMixin(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    // replace bounce logic (on hitting an enemy at the time as another trident) with pierce logic instead
    @Inject(
            method = "onHitEntity",
            at = @At("HEAD"),
            cancellable = true
    )
    protected void onHitEntity(EntityHitResult pResult, CallbackInfo ci) {
        ci.cancel();

        Entity $$1 = pResult.getEntity();
        float $$2 = 8.0F;
        if ($$1 instanceof LivingEntity $$3) {
            // 1.21.1 moved Impaling out of EnchantmentHelper and into the enchantment's
            // data-driven value effects, which this mixin's hand-rolled damage bypasses.
            int impaling = EnchantmentHelper.getItemEnchantmentLevel(
                    EnchantmentRegistrar.vanilla(Enchantments.IMPALING), this.getWeaponItem());
            if (impaling > 0) {
                MobCategory category = $$3.getType().getCategory();
                if (category == MobCategory.UNDERGROUND_WATER_CREATURE
                        || category == MobCategory.WATER_CREATURE
                        || category == MobCategory.AXOLOTLS) {
                    $$2 += impaling * 2.5F;
                } else if (category == MobCategory.MONSTER) {
                    $$2 += impaling;
                }
            }
        }
        Entity $$4 = this.getOwner();
        DamageSource $$5 = this.damageSources().trident(this, $$4 == null ? this : $$4);
        SoundEvent $$6 = SoundEvents.TRIDENT_HIT;
        if ($$1.hurt($$5, $$2)) {
            if ($$1.getType() == EntityType.ENDERMAN) {
                return;
            }
            if ($$1 instanceof LivingEntity $$7) {
                if ($$4 instanceof LivingEntity) {
                    this.dealtDamage = true;
                    this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01, -0.1, -0.01));
                }
            }
        }

        this.playSound($$6, 1.0F, 1.0F);
    }
}

