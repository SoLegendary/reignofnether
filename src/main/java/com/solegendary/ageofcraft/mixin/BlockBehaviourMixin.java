package com.solegendary.ageofcraft.mixin;

import com.solegendary.ageofcraft.orthoview.OrthoViewClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
    @Inject(
            method = "getShadeBrightness(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void getShadeBrightness(
            BlockState p_60472_, BlockGetter p_60473_, BlockPos p_60474_,
            CallbackInfoReturnable<Float> cir
    ) {
        // this is only called once on initial render so probably not a good solution...
        /*
        BlockPos mop = OrthoViewClientEvents.mousedOverPos;
        if (p_60474_.getX() == mop.getX() && p_60474_.getY() == mop.getY() && p_60474_.getZ() == mop.getZ()) {
            System.out.println("set!");
            cir.setReturnValue(0f);
        }*/
    }
}