package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.pathfinding.WalkabilityGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Invalidate the walkability column on any loaded-chunk block change, on both client and server.
// LevelChunk.setBlockState is the single chokepoint every such change funnels through (worldgen uses
// ProtoChunk, so this never fires during generation).
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Shadow public abstract Level getLevel();

    @Inject(method = "setBlockState", at = @At("TAIL"))
    private void reignofnether$invalidateWalkability(BlockPos pos, BlockState state, boolean isMoving,
                                                     CallbackInfoReturnable<BlockState> cir) {
        WalkabilityGrid.invalidateColumnIfPresent(getLevel(), pos);
    }
}
