package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

// When the server resends a full EDGE chunk (circle growth, mixed multi-block update, promotion), the
// packet carries the current state of the WHOLE chunk - including columns outside the vision circle.
// Restore those fogged columns from the client's previous copy so out-of-circle changes never render;
// they only appear once the circle actually covers them (at which point the column is visible in the
// mask and takes the fresh data). Runs on the client main thread only (packet handler), so the
// capture-then-restore pair around the vanilla call needs no synchronisation.
@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {

    // captured old section states, indexed [section][(ly<<8)|(lx<<4)|lz]; a null section = it was all air
    private BlockState[][] ron_capturedSections = null;
    private long[] ron_mask = null;

    @Inject(method = "replaceWithPacketData", at = @At("HEAD"))
    private void reignofnether$captureFoggedColumns(
            int x, int z, FriendlyByteBuf buf, CompoundTag tag,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
            CallbackInfoReturnable<LevelChunk> cir
    ) {
        ron_capturedSections = null;
        ron_mask = null;
        long[] mask = FogOfWarClientEvents.getEdgeMask(x, z);
        if (mask == null) return; // not an edge chunk: apply the packet untouched
        LevelChunk old = ((ClientChunkCache) (Object) this).getChunk(x, z, ChunkStatus.FULL, false);
        if (old == null) return; // fresh load, nothing to preserve (server sent the snapshot)
        LevelChunkSection[] sections = old.getSections();
        BlockState[][] captured = new BlockState[sections.length][];
        for (int s = 0; s < sections.length; s++) {
            LevelChunkSection sec = sections[s];
            if (sec == null || sec.hasOnlyAir()) continue;
            BlockState[] arr = new BlockState[4096];
            for (int ly = 0; ly < 16; ly++)
                for (int lx = 0; lx < 16; lx++)
                    for (int lz = 0; lz < 16; lz++)
                        arr[(ly << 8) | (lx << 4) | lz] = sec.getBlockState(lx, ly, lz);
            captured[s] = arr;
        }
        ron_capturedSections = captured;
        ron_mask = mask;
    }

    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void reignofnether$restoreFoggedColumns(
            int x, int z, FriendlyByteBuf buf, CompoundTag tag,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
            CallbackInfoReturnable<LevelChunk> cir
    ) {
        BlockState[][] captured = ron_capturedSections;
        long[] mask = ron_mask;
        ron_capturedSections = null;
        ron_mask = null;
        if (captured == null || mask == null) return;
        LevelChunk chunk = cir.getReturnValue();
        if (chunk == null) return;
        BlockState air = Blocks.AIR.defaultBlockState();
        LevelChunkSection[] sections = chunk.getSections();
        for (int s = 0; s < sections.length; s++) {
            LevelChunkSection sec = sections[s];
            BlockState[] arr = (s < captured.length) ? captured[s] : null;
            if (sec == null || (arr == null && sec.hasOnlyAir())) continue; // both air: nothing to restore
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int i = (lx << 4) | lz;
                    if ((mask[i >> 6] & (1L << (i & 63))) != 0) continue; // visible column: keep live data
                    for (int ly = 0; ly < 16; ly++) {
                        BlockState want = (arr == null) ? air : arr[(ly << 8) | i];
                        if (sec.getBlockState(lx, ly, lz) != want)
                            sec.setBlockState(lx, ly, lz, want, false);
                    }
                }
            }
        }
    }
}
