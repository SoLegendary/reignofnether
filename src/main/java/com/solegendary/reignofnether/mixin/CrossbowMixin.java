package com.solegendary.reignofnether.mixin;

import javax.annotation.Nullable;

import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public class CrossbowMixin {

    @Inject(
            method = "getChargeDuration",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void getChargeDuration(ItemStack pCrossbowStack, LivingEntity pShooter, CallbackInfoReturnable<Integer> cir) {
        int i = EnchantmentHelper.getItemEnchantmentLevel(EnchantmentRegistrar.vanilla(Enchantments.QUICK_CHARGE), pCrossbowStack);
        cir.setReturnValue(i == 0 ? 35 : 35 - 5 * i);
    }

    /**
     * 1.20.1 had {@code Pillager#shootCrossbowProjectile} and CrossbowItem called into it; 1.21.1
     * dropped that hook and inlined the aiming maths. PillagerUnit relies on the hook to aim at
     * buildings and ground, so redirect the whole method for PillagerUnit shooters.
     */
    @Inject(
            method = "shootProjectile",
            at = @At("HEAD"),
            cancellable = true
    )
    private void shootProjectile(
            LivingEntity pShooter,
            Projectile pProjectile,
            int pIndex,
            float pVelocity,
            float pInaccuracy,
            float pAngle,
            @Nullable LivingEntity pTarget,
            CallbackInfo ci
    ) {
        if (pShooter instanceof PillagerUnit pillagerUnit) {
            pillagerUnit.ron$shootCrossbowProjectile(pShooter, pTarget, pProjectile, pAngle, pVelocity);
            ci.cancel();
        }
    }
}
