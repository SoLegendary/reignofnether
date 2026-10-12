package com.solegendary.reignofnether.mixin.fire;

import com.solegendary.reignofnether.blocks.BlockUtils;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {

    public WalkNodeEvaluatorMixin() {
    }

    // 1.21.1 reworked the walker around PathfindingContext: getBlockPathType(BlockGetter,int,int,int)
    // became getPathType(PathfindingContext,int,int,int), and the BlockGetter is now reachable
    // through the context, which also carries the mob being routed.
    @Inject(
            method = "getPathType(Lnet/minecraft/world/level/pathfinder/PathfindingContext;III)Lnet/minecraft/world/level/pathfinder/PathType;",
            at = @At("HEAD"),
            cancellable = true
    )
    public void getPathType(PathfindingContext pContext, int pX, int pY, int pZ, CallbackInfoReturnable<PathType> cir) {
        // PathfindingContext keeps the mob private, but NodeEvaluator already holds the one prepare()
        // was called with, and getPathType is only reached through it.
        Mob mob = this.mob;
        if (!(mob instanceof Unit))
            return;

        BlockPos pos = new BlockPos(pX, pY, pZ);
        BlockState blockStateBelow = pContext.getBlockState(pos.below());
        Block blockBelow = blockStateBelow.getBlock();
        Block block = pContext.getBlockState(pos).getBlock();

        // allow units to walk on fire and magma but not leaves (to prevent workers getting stuck in trees)
        if (block == Blocks.FIRE || blockBelow == Blocks.FIRE ||
            block == Blocks.MAGMA_BLOCK || blockBelow == Blocks.MAGMA_BLOCK)
            cir.setReturnValue(PathType.WALKABLE);
        else if (block == Blocks.POINTED_DRIPSTONE || blockBelow == Blocks.POINTED_DRIPSTONE)
            cir.setReturnValue(PathType.UNPASSABLE_RAIL);
        else if (BlockUtils.isLeafBlock(blockStateBelow))
            cir.setReturnValue(PathType.DAMAGE_FIRE);
        else {
            cir.setReturnValue(WalkNodeEvaluator.getPathTypeStatic(pContext, new BlockPos.MutableBlockPos(pX, pY, pZ)));
        }
    }
}
