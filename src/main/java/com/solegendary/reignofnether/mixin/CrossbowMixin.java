package com.solegendary.reignofnether.mixin;

import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public class CrossbowMixin {

    @Inject(
            method = "getChargeDuration",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void getChargeDuration(ItemStack pCrossbowStack, CallbackInfoReturnable<Integer> cir) {
        int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, pCrossbowStack);
        cir.setReturnValue(i == 0 ? 35 : 35 - 5 * i);
    }
}