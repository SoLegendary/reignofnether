package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

// The server always serialises the *live* server chunk into ClientboundLevelChunkWithLightPacket, so the
// only copy of the terrain the player is supposed to remember is the client's own LevelChunk. This mixin
// keeps that remembered copy authoritative for every column the server says is not currently visible:
//
//   - chunk not in brightChunks        -> fully fogged, every column frozen at its remembered state
//   - chunk in brightChunks + mask     -> edge chunk, per-column: visible columns take the fresh packet
//   - chunk in brightChunks, no mask   -> fully visible, packet applied untouched
//
// Because the client's LevelChunk is thrown away when the player walks off (ClientboundForgetLevelChunk ->
// drop()), the remembered state is also snapshotted into ron_fogMemory on the way out and read back the
// next time the server re-sends that chunk. Without that, a walk-away-and-return produced a chunk with no
// previous copy to preserve, and the whole packet - fogged columns included - was applied verbatim.
//
// Everything here runs on the client main thread (packet handler / chunk drop), so the capture-then-restore
// pair around the vanilla call and the memory map need no synchronisation.
@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {

    // Remembered terrain for chunks that have left the cache, as palette snapshots per section
    // (a null entry in the array = that section was all air). LinkedHashMap so the oldest entry can be
    // evicted once the cap is hit; cleared whenever a new ClientChunkCache is built (level change).
    @Unique
    private static Map<ChunkPos, PalettedContainer<BlockState>[]> ron_fogMemory;

    // ~8k chunks of palette snapshots is a few tens of MB worst case; an RTS-optimised map is well under
    // this, so in practice nothing is ever evicted.
    @Unique
    private static final int RON_FOG_MEMORY_MAX_CHUNKS = 8192;

    // state handed from the HEAD hook to the RETURN hook for the current replaceWithPacketData call
    @Unique
    private PalettedContainer<BlockState>[] ron_capturedSections = null;
    @Unique
    private long[] ron_mask = null;

    @Unique
    private static Map<ChunkPos, PalettedContainer<BlockState>[]> ron_memory() {
        if (ron_fogMemory == null) ron_fogMemory = new LinkedHashMap<>();
        return ron_fogMemory;
    }

    // Cheap palette-level copy of a chunk's block states. Independent of the live chunk once taken.
    @Unique
    @SuppressWarnings("unchecked")
    private static PalettedContainer<BlockState>[] ron_snapshot(LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        PalettedContainer<BlockState>[] snapshot = new PalettedContainer[sections.length];
        for (int s = 0; s < sections.length; s++) {
            LevelChunkSection sec = sections[s];
            if (sec == null || sec.hasOnlyAir()) continue; // leave null = remembered as air
            snapshot[s] = sec.getStates().copy();
        }
        return snapshot;
    }

    // null = nothing to hide (fog off, or a fully visible chunk). An all-zero mask = hide everything.
    @Unique
    private static long[] ron_visibilityMask(int x, int z) {
        if (!FogOfWarClientEvents.isEnabled()) return null;
        if (!FogOfWarClientEvents.brightChunks.contains(new ChunkPos(x, z))) return new long[4];
        return FogOfWarClientEvents.getEdgeMask(x, z);
    }

    // A new ClientChunkCache means a new ClientLevel (join, respawn, dimension change): the old memory is
    // for a different world. Note updateViewRadius() reuses the cache and only swaps Storage, so changing
    // render distance does not wipe fog memory.
    @Inject(method = "<init>", at = @At("RETURN"))
    private void reignofnether$clearFogMemory(ClientLevel level, int viewDistance, CallbackInfo ci) {
        ron_memory().clear();
    }

    // Snapshot the remembered terrain before the client forgets the chunk. The live client chunk already
    // holds the masked view (this mixin has been maintaining it), so capturing it verbatim is correct.
    @Inject(method = "drop", at = @At("HEAD"))
    private void reignofnether$rememberDroppedChunk(int x, int z, CallbackInfo ci) {
        if (!FogOfWarClientEvents.isEnabled()) return;
        LevelChunk chunk = ((ClientChunkCache) (Object) this).getChunk(x, z, ChunkStatus.FULL, false);
        if (chunk == null || chunk.isEmpty()) return;
        ChunkPos cpos = new ChunkPos(x, z);
        Map<ChunkPos, PalettedContainer<BlockState>[]> memory = ron_memory();
        memory.remove(cpos); // re-insert so iteration order stays newest-last
        memory.put(cpos, ron_snapshot(chunk));
        for (Iterator<ChunkPos> it = memory.keySet().iterator();
             memory.size() > RON_FOG_MEMORY_MAX_CHUNKS && it.hasNext(); ) {
            it.next();
            it.remove();
        }
    }

    @Inject(method = "replaceWithPacketData", at = @At("HEAD"))
    private void reignofnether$captureFoggedColumns(
            int x, int z, FriendlyByteBuf buf, CompoundTag tag,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
            CallbackInfoReturnable<LevelChunk> cir
    ) {
        ron_capturedSections = null;
        ron_mask = null;
        long[] mask = ron_visibilityMask(x, z);
        if (mask == null) return; // fog off or fully visible: apply the packet untouched

        LevelChunk old = ((ClientChunkCache) (Object) this).getChunk(x, z, ChunkStatus.FULL, false);
        FogOfWarClientEvents.setChunkFreshlyLoaded(x, z, old == null);

        // Still cached -> snapshot it live. Not cached -> fall back to what we stored when it was dropped.
        // Peek rather than remove: replaceWithPacketData can still bail out (out of view range) and we
        // would rather keep the memory than lose it. The entry is dropped in the RETURN hook on success.
        PalettedContainer<BlockState>[] captured = (old != null)
                ? ron_snapshot(old)
                : ron_memory().get(new ChunkPos(x, z));
        if (captured == null) return; // genuinely never seen: nothing to remember, take the packet as-is

        ron_capturedSections = captured;
        ron_mask = mask;
    }

    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void reignofnether$restoreFoggedColumns(
            int x, int z, FriendlyByteBuf buf, CompoundTag tag,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
            CallbackInfoReturnable<LevelChunk> cir
    ) {
        PalettedContainer<BlockState>[] captured = ron_capturedSections;
        long[] mask = ron_mask;
        ron_capturedSections = null;
        ron_mask = null;
        if (captured == null || mask == null) return;
        LevelChunk chunk = cir.getReturnValue();
        if (chunk == null) return; // vanilla refused the packet; memory stays put for the next attempt

        BlockState air = Blocks.AIR.defaultBlockState();
        LevelChunkSection[] sections = chunk.getSections();
        for (int s = 0; s < sections.length; s++) {
            LevelChunkSection sec = sections[s];
            if (sec == null) continue;
            PalettedContainer<BlockState> remembered = (s < captured.length) ? captured[s] : null;
            if (remembered == null && sec.hasOnlyAir()) continue; // remembered air, still air: nothing to do
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int i = (lx << 4) | lz;
                    if ((mask[i >> 6] & (1L << (i & 63))) != 0) continue; // visible column: keep live data
                    for (int ly = 0; ly < 16; ly++) {
                        BlockState want = (remembered == null) ? air : remembered.get(lx, ly, lz);
                        if (sec.getBlockState(lx, ly, lz) != want)
                            sec.setBlockState(lx, ly, lz, want, false);
                    }
                }
            }
        }
        // The cache now holds the remembered state again; drop() will re-snapshot if it leaves once more.
        ron_memory().remove(chunk.getPos());
    }
}