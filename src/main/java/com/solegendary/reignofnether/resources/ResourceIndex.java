package com.solegendary.reignofnether.resources;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Predicate;

// Per-chunk spatial index of resource block positions, so a worker's resource search iterates only the
// actual resources in nearby chunks instead of brute-force scanning a range^3 cube (the BlockPos
// .findClosestMatch hotspot). Used and maintained entirely on the SERVER MAIN THREAD (the gather goal,
// the LevelChunk.setBlockState mixin, and the server tick), so - unlike WalkabilityGrid, which is read by
// A* worker threads - there is NO cross-thread access and block changes mutate the live chunk set in place.
// Deferral (the budgeted build queue) is needed only for the expensive cold full-chunk scan.
public final class ResourceIndex {
    public static final int MAX_INDEXED_CHUNKS = 2048;            // LRU cap; a ResourceChunk is light (longs only)
    public static final int MAX_RESOURCE_CHUNK_SCANS_PER_TICK = 8; // cold-scan budget per server tick

    private static final WeakHashMap<LevelAccessor, ResourceIndex> PER_LEVEL = new WeakHashMap<>();

    // Cold-scan queue: chunks awaiting a budgeted full scan. Global across levels, main-thread only; holds a
    // strong Level ref only while a scan is pending. Dedup is guarded per-index by `queued`.
    private record PendingScan(Level level, long chunkKey) {}
    private static final ArrayDeque<PendingScan> BUILD_QUEUE = new ArrayDeque<>();

    // Access-ordered LRU (head = most-recently-used). A present value means the chunk has been scanned.
    private final Long2ObjectLinkedOpenHashMap<ResourceChunk> chunks = new Long2ObjectLinkedOpenHashMap<>();
    // Chunk keys currently sitting in BUILD_QUEUE for this index, so the same chunk is never enqueued twice.
    private final LongOpenHashSet queued = new LongOpenHashSet();

    private ResourceIndex() {}

    public static synchronized ResourceIndex get(LevelAccessor level) {
        return PER_LEVEL.computeIfAbsent(level, k -> new ResourceIndex());
    }

    // Peek without creating an index - used by the client mixin path so the client never builds one.
    private static synchronized ResourceIndex peek(LevelAccessor level) {
        return PER_LEVEL.get(level);
    }

    // Synchronous O(1) maintenance on every server-side loaded-chunk block change (from LevelChunkMixin).
    // No-op on the client (no index) and for not-yet-scanned chunks (their eventual cold scan reads live
    // state). Classifies the NEW state: resource -> ensure it's in the right bucket; otherwise drop it.
    public static void onBlockChange(Level level, BlockPos pos, BlockState newState) {
        if (level == null || pos == null) return;
        ResourceIndex idx = peek(level);
        if (idx == null) return;
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        ResourceChunk rc = idx.chunks.get(chunkKey);
        if (rc == null) return; // chunk not indexed -> ignore; the cold scan captures current state later
        long packed = pos.asLong();
        ResourceSource src = ResourceSources.getFromBlockState(newState); // type-only; blockStateTest at query
        rc.removeAll(packed); // drop any stale entry first (mine-to-air, or a resource->resource type swap)
        if (src != null && src.resourceName != ResourceName.NONE)
            rc.add(src.resourceName, packed);
    }

    // Closest valid resource of `type` within the [-range,range] box around origin (matching the old
    // findClosestMatch(origin, range, range, cond) bounding box), filtered by `condition`. Lazily queues any
    // in-range un-indexed chunk for a cold scan and proceeds with whatever IS indexed this tick.
    public Optional<BlockPos> findClosest(Level level, BlockPos origin, int range,
                                          ResourceName type, Predicate<BlockPos> condition) {
        if (type == null || type == ResourceName.NONE) return Optional.empty();

        final int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        final int cx0 = (ox - range) >> 4, cx1 = (ox + range) >> 4;
        final int cz0 = (oz - range) >> 4, cz1 = (oz + range) >> 4;

        LongArrayList cand = new LongArrayList();
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                long ckey = ChunkPos.asLong(cx, cz);
                ResourceChunk rc = chunks.getAndMoveToFirst(ckey);
                if (rc == null) { enqueueIfNeeded(level, ckey); continue; }
                LongOpenHashSet set = rc.positions(type);
                if (set == null || set.isEmpty()) continue;
                for (LongIterator it = set.iterator(); it.hasNext();) {
                    long p = it.nextLong();
                    if (Math.abs(BlockPos.getX(p) - ox) > range
                            || Math.abs(BlockPos.getY(p) - oy) > range
                            || Math.abs(BlockPos.getZ(p) - oz) > range) continue;
                    cand.add(p);
                }
            }
        }
        if (cand.isEmpty()) return Optional.empty();

        // Sort nearest-first so condition runs in increasing-distance order (preserving findClosestMatch's
        // "first passing wins", and the altSearchPos side-effect in BLOCK_CONDITION firing on the NEAREST
        // contested block). Candidate count is small (resources in a handful of chunks).
        long[] arr = cand.toLongArray();
        LongArrays.quickSort(arr, (a, b) -> Long.compare(distSq(a, ox, oy, oz), distSq(b, ox, oy, oz)));
        for (long p : arr) {
            BlockPos bp = new BlockPos(BlockPos.getX(p), BlockPos.getY(p), BlockPos.getZ(p));
            if (condition.test(bp)) return Optional.of(bp);
        }
        return Optional.empty();
    }

    private void enqueueIfNeeded(Level level, long chunkKey) {
        if (chunks.containsKey(chunkKey) || queued.contains(chunkKey)) return;
        queued.add(chunkKey);
        BUILD_QUEUE.add(new PendingScan(level, chunkKey));
    }

    // Drop the chunk's index + pending scan on unload, so stale positions can't linger and the queue can't
    // pin an unloaded chunk's Level. A reload re-scans fresh.
    public static void onChunkUnload(Level level, ChunkPos cp) {
        if (level == null || level.isClientSide()) return;
        ResourceIndex idx = peek(level);
        if (idx == null) return;
        long key = cp.toLong();
        idx.chunks.remove(key);
        idx.queued.remove(key);
    }

    // Budgeted cold scans, called from PathfinderWorkerPool.onServerTick START phase.
    public static void drainBuildQueue(int budget) {
        int done = 0;
        while (done < budget && !BUILD_QUEUE.isEmpty()) {
            PendingScan ps = BUILD_QUEUE.poll();
            ResourceIndex idx = peek(ps.level());
            if (idx == null) continue;
            if (!idx.queued.remove(ps.chunkKey())) continue;     // unloaded/cancelled since enqueue
            if (idx.chunks.containsKey(ps.chunkKey())) continue; // already scanned via another path
            LevelChunk chunk = loadedChunkOrNull(ps.level(), ps.chunkKey());
            if (chunk == null) continue;                          // unloaded mid-queue
            ResourceChunk rc = scanChunk(chunk);
            idx.chunks.putAndMoveToFirst(ps.chunkKey(), rc);
            while (idx.chunks.size() > MAX_INDEXED_CHUNKS) idx.chunks.removeLast();
            done++;
        }
    }

    // Scan one loaded chunk's resources, palette-aware. Most sections (stone/air) die at the palette check;
    // only resource-bearing sections walk their 4096 cells. Captures worldgen resources (the setBlockState
    // mixin never fires during worldgen, which uses ProtoChunk).
    private static ResourceChunk scanChunk(LevelChunk chunk) {
        ResourceChunk rc = new ResourceChunk();
        final int baseX = chunk.getPos().getMinBlockX();
        final int baseZ = chunk.getPos().getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();
        for (int si = 0; si < sections.length; si++) {
            LevelChunkSection section = sections[si];
            if (section == null || section.hasOnlyAir()) continue;
            if (!section.getStates().maybeHas(ResourceIndex::isResource)) continue; // palette-only pre-check
            int sectionBaseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(si));
            for (int ly = 0; ly < 16; ly++)
                for (int lz = 0; lz < 16; lz++)
                    for (int lx = 0; lx < 16; lx++) {
                        BlockState bs = section.getBlockState(lx, ly, lz);
                        ResourceSource s = ResourceSources.getFromBlockState(bs);
                        if (s == null || s.resourceName == ResourceName.NONE) continue;
                        rc.add(s.resourceName, BlockPos.asLong(baseX + lx, sectionBaseY + ly, baseZ + lz));
                    }
        }
        return rc;
    }

    private static boolean isResource(BlockState bs) {
        ResourceSource s = ResourceSources.getFromBlockState(bs);
        return s != null && s.resourceName != ResourceName.NONE;
    }

    private static LevelChunk loadedChunkOrNull(Level level, long key) {
        if (!(level instanceof ServerLevel sl)) return null;
        return sl.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)); // never force-loads
    }

    private static long distSq(long p, int ox, int oy, int oz) {
        long dx = BlockPos.getX(p) - ox, dy = BlockPos.getY(p) - oy, dz = BlockPos.getZ(p) - oz;
        return dx * dx + dy * dy + dz * dz;
    }

    // Server stop: drop the queue + per-level indices so static state can't pin Levels across a restart.
    public static void clearAll() {
        BUILD_QUEUE.clear();
        synchronized (ResourceIndex.class) { PER_LEVEL.clear(); }
    }
}
