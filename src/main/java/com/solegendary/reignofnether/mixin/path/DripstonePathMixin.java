package com.solegendary.reignofnether.mixin.path;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public class DripstonePathMixin {

    @Inject(method = "getBlockPathTypeRaw",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void avoidDripstone(
            BlockGetter level, BlockPos pos,
            CallbackInfoReturnable<BlockPathTypes> cir
    ) {
        if (level.getBlockState(pos).is(Blocks.POINTED_DRIPSTONE)) {
            cir.setReturnValue(BlockPathTypes.BLOCKED);
        }
    }
}
