package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    @Inject(
            method = "entityInside",
            at = @At("HEAD")
    )
    public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity, CallbackInfo ci) {
        if (pEntity instanceof Unit) {
            pLevel.setBlockAndUpdate(pPos, Blocks.SNOW_BLOCK.defaultBlockState());
            if (pLevel.getBlockState(pPos.below()).getBlock() == Blocks.POWDER_SNOW)
                pLevel.setBlockAndUpdate(pPos.below(), Blocks.SNOW_BLOCK.defaultBlockState());
        }
    }
}
