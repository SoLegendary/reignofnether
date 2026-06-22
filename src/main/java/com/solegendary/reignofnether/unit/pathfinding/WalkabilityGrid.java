package com.solegendary.reignofnether.unit.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.WeakHashMap;

public final class WalkabilityGrid {
    private static final WeakHashMap<LevelAccessor, WalkabilityGrid> PER_LEVEL = new WeakHashMap<>();
    // Access-ordered (most-recent at head) so we can evict the least-recently-used tail.
    private final Long2ObjectLinkedOpenHashMap<WalkabilityGridChunk> chunks = new Long2ObjectLinkedOpenHashMap<>();

    private WalkabilityGrid() {}

    public static synchronized WalkabilityGrid get(LevelAccessor level) {
        return PER_LEVEL.computeIfAbsent(level, k -> new WalkabilityGrid());
    }

    // Returns a chunk whose classified Y band covers [wantMinY, wantMaxY). If a cached chunk exists but
    // its band is too small, it is rebuilt over the union of the old and requested bands so repeated
    // requests at different Y converge to one taller band rather than thrashing.
    public WalkabilityGridChunk getOrBuild(Level level, int chunkX, int chunkZ, int wantMinY, int wantMaxY) {
        long key = ChunkPos.asLong(chunkX, chunkZ);
        WalkabilityGridChunk existing;
        synchronized (chunks) { existing = chunks.getAndMoveToFirst(key); }
        if (existing != null && existing.covers(wantMinY, wantMaxY)) return existing;

        int buildMinY = wantMinY;
        int buildMaxY = wantMaxY;
        if (existing != null) {
            buildMinY = Math.min(buildMinY, existing.minY());
            buildMaxY = Math.max(buildMaxY, existing.maxY());
        }
        WalkabilityGridChunk built = WalkabilityGridChunk.build(level, new ChunkPos(chunkX, chunkZ), buildMinY, buildMaxY);
        synchronized (chunks) {
            WalkabilityGridChunk concurrent = chunks.get(key);
            if (concurrent != null && concurrent.covers(wantMinY, wantMaxY)) {
                chunks.getAndMoveToFirst(key);
                return concurrent;
            }
            chunks.putAndMoveToFirst(key, built);
            while (chunks.size() > PathfinderConfig.MAX_CACHED_CHUNKS) chunks.removeLast();
        }
        return built;
    }

    public void invalidateColumn(BlockPos bp) {
        synchronized (chunks) { chunks.remove(ChunkPos.asLong(bp.getX() >> 4, bp.getZ() >> 4)); }
    }
}
