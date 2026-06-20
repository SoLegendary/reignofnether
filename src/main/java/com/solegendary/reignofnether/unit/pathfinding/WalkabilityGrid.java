package com.solegendary.reignofnether.unit.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.WeakHashMap;

public final class WalkabilityGrid {
    private static final WeakHashMap<LevelAccessor, WalkabilityGrid> PER_LEVEL = new WeakHashMap<>();
    private final Long2ObjectMap<WalkabilityGridChunk> chunks = new Long2ObjectOpenHashMap<>();

    private WalkabilityGrid() {}

    public static synchronized WalkabilityGrid get(LevelAccessor level) {
        return PER_LEVEL.computeIfAbsent(level, k -> new WalkabilityGrid());
    }

    public WalkabilityGridChunk getOrBuild(Level level, int chunkX, int chunkZ) {
        long key = ChunkPos.asLong(chunkX, chunkZ);
        WalkabilityGridChunk existing;
        synchronized (chunks) { existing = chunks.get(key); }
        if (existing != null) return existing;
        WalkabilityGridChunk built = WalkabilityGridChunk.build(level, new ChunkPos(chunkX, chunkZ));
        synchronized (chunks) {
            WalkabilityGridChunk concurrent = chunks.get(key);
            if (concurrent != null) return concurrent;
            chunks.put(key, built);
        }
        return built;
    }

    public void invalidateColumn(BlockPos bp) {
        synchronized (chunks) { chunks.remove(ChunkPos.asLong(bp.getX() >> 4, bp.getZ() >> 4)); }
    }

    public int chunkCount() {
        synchronized (chunks) { return chunks.size(); }
    }

    public static int totalChunksCached() {
        synchronized (PER_LEVEL) {
            int total = 0;
            for (WalkabilityGrid g : PER_LEVEL.values()) total += g.chunkCount();
            return total;
        }
    }
}
