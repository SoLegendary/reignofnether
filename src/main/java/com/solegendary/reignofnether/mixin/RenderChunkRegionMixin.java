package com.solegendary.reignofnether.mixin;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(RenderChunkRegion.class)
public abstract class RenderChunkRegionMixin {

    @Inject(
            method = "getBlockState",
            at = @At("HEAD"),
            cancellable = true
    )
    private void getBlockState(BlockPos pPos, CallbackInfoReturnable<BlockState> cir) {
        Level level = Minecraft.getInstance().level;
        if (level != null && OrthoviewClientEvents.isEnabled() && OrthoviewClientEvents.shouldHideLeaves()) {
            Block block = level.getBlockState(pPos).getBlock();
            BlockState bs = block == BlockRegistrar.DECAYABLE_NETHER_WART_BLOCK.get() ?
                    Blocks.RED_STAINED_GLASS.defaultBlockState() :
                    Blocks.GREEN_STAINED_GLASS.defaultBlockState();

            if (level.getBlockState(pPos).getBlock() instanceof LeavesBlock) {
                if (OrthoviewClientEvents.hideLeavesMethod == OrthoviewClientEvents.LeafHideMethod.ALL) {
                    cir.setReturnValue(bs);
                    return;
                }
                synchronized (UnitClientEvents.unitWindowVecs) {
                    for (ArrayList<Vec3> vecs : UnitClientEvents.unitWindowVecs) {
                        if (MyMath.isPointInsideRect3d(vecs, Vec3.atCenterOf(pPos))) {
                            cir.setReturnValue(bs);
                            return;
                        }
                    }
                }
            }
        }
    }
}
