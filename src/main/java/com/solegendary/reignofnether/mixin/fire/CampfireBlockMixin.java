package com.solegendary.reignofnether.mixin.fire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin extends BaseEntityBlock {

    private static final int DAMAGE_DELAY = 20; // higher == damage less often

    protected CampfireBlockMixin(Properties pProperties) {
        super(pProperties);
    }

    @Inject(
            method = "entityInside",
            at = @At("HEAD"),
            cancellable = true
    )
    public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity, CallbackInfo ci) {
        ci.cancel();
        if (pState.getValue(CampfireBlock.LIT) && pEntity instanceof LivingEntity && !EnchantmentHelper.hasFrostWalker((LivingEntity)pEntity) &&
            pEntity.tickCount % DAMAGE_DELAY == 0) {
            pEntity.hurt(pLevel.damageSources().inFire(), pState.getBlock() == Blocks.SOUL_CAMPFIRE ? 2 : 1);
        }
    }
}
