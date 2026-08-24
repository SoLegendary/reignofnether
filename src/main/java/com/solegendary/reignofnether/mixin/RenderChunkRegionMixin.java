package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(RenderChunkRegion.class)
public abstract class RenderChunkRegionMixin {

    @Unique
    private boolean reignofnether$inLeafLookup = false;

    @Inject(
            method = "getBlockState",
            at = @At("RETURN"),
            cancellable = true
    )
    private void getBlockState(BlockPos pPos, CallbackInfoReturnable<BlockState> cir) {
        if (this.reignofnether$inLeafLookup) return;   // nested below() lookup: leave it untouched
        if (!OrthoviewClientEvents.shouldHideLeaves()) return;

        BlockState state = cir.getReturnValue();
        if (state == null) return;

        Block block = state.getBlock();
        BlockState replacementBs = null;

        if (block == BlockRegistrar.DECAYABLE_NETHER_WART_BLOCK.get()) {
            replacementBs = Blocks.RED_STAINED_GLASS.defaultBlockState();
        } else if (block instanceof LeavesBlock) {
            replacementBs = Blocks.GREEN_STAINED_GLASS.defaultBlockState();
        } else if (MiscUtil.isSnowLayerBlock(block)) {
            if (reignofnether$below(pPos).getBlock() instanceof LeavesBlock)
                replacementBs = Blocks.AIR.defaultBlockState();
        }

        if (replacementBs == null) return;

        if (OrthoviewClientEvents.hideLeavesMethod == OrthoviewClientEvents.LeafHideMethod.ALL) {
            cir.setReturnValue(replacementBs);
            return;
        }
        synchronized (UnitClientEvents.unitWindowVecs) {
            for (ArrayList<Vec3> vecs : UnitClientEvents.unitWindowVecs) {
                if (MyMath.isPointInsideRect3d(vecs, Vec3.atCenterOf(pPos))) {
                    cir.setReturnValue(replacementBs);
                    return;
                }
            }
        }
    }

    // Read through the region (i.e. the main-thread palette copies), not the live level.
    @Unique
    private BlockState reignofnether$below(BlockPos pPos) {
        this.reignofnether$inLeafLookup = true;
        try {
            return ((RenderChunkRegion) (Object) this).getBlockState(pPos.below());
        } finally {
            this.reignofnether$inLeafLookup = false;
        }
    }
}
