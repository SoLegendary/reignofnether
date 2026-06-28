package com.solegendary.reignofnether.unit.pathfinding;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class WalkabilityGrid {
    private static final WeakHashMap<LevelAccessor, WalkabilityGrid> PER_LEVEL = new WeakHashMap<>();
    // Access-ordered (most-recent at head) so we can evict the least-recently-used tail.
    private final Long2ObjectLinkedOpenHashMap<WalkabilityGridChunk> chunks = new Long2ObjectLinkedOpenHashMap<>();

    // Columns awaiting deferred reclassify: chunkKey -> set of packed local columns ((lz<<4)|lx, 0..255).
    // A block change marks its column dirty instead of evicting the chunk, so reads keep getting the (briefly
    // stale) cached chunk rather than a BLOCKED hole that would strand units. Drained, budgeted, on the server
    // tick by drainDirtyColumns. The IntOpenHashSet dedups, so a placement/leaf-decay storm coalesces for free.
    private final Long2ObjectOpenHashMap<IntOpenHashSet> dirtyColumns = new Long2ObjectOpenHashMap<>();

    // Grids that currently have pending dirty work -> the Level needed to reclassify (the grid is keyed by
    // LevelAccessor and keeps no back-reference). The Level is held ONLY while work is pending (the entry is
    // removed when the grid's dirty set empties), so this transient strong ref cannot pin the PER_LEVEL weak
    // key long-term. Guarded by DIRTY_LOCK.
    private static final Map<WalkabilityGrid, Level> DIRTY_GRIDS = new IdentityHashMap<>();
    private static final Object DIRTY_LOCK = new Object();

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

    // Peek whether a cached chunk already covers [wantMinY, wantMaxY) WITHOUT building one. Lets the deferred
    // build queue tell a free cache hit from a cold build so it only spends its per-tick budget on the latter.
    public boolean isBuilt(int chunkX, int chunkZ, int wantMinY, int wantMaxY) {
        synchronized (chunks) {
            WalkabilityGridChunk c = chunks.get(ChunkPos.asLong(chunkX, chunkZ));
            return c != null && c.covers(wantMinY, wantMaxY);
        }
    }

    // Mark the column at bp dirty for deferred reclassify, WITHOUT creating a grid for a level that has none.
    // Called from the LevelChunk.setBlockState mixin on every in-game block change (mining, building,
    // explosions, leaf decay, commands). No-op when no grid exists for the level (eg. client side, where the
    // pathfinder never runs) or the chunk isn't cached. We deliberately do NOT evict: keeping the old chunk
    // live lets reads return stale-but-walkable data until the deferred drain swaps in the patched chunk,
    // instead of a removed chunk reading as KIND_BLOCKED and stranding units mid-update.
    public static void markColumnDirtyIfPresent(Level level, BlockPos bp) {
        if (level == null || bp == null) return;
        WalkabilityGrid grid;
        synchronized (WalkabilityGrid.class) { grid = PER_LEVEL.get(level); }
        if (grid != null) grid.markColumnDirty(level, bp);
    }

    private void markColumnDirty(Level level, BlockPos bp) {
        long key = ChunkPos.asLong(bp.getX() >> 4, bp.getZ() >> 4);
        WalkabilityGridChunk c;
        synchronized (chunks) { c = chunks.get(key); }
        if (c == null) return; // not cached -> nothing to patch; a later getOrBuild reads fresh world state.
        // A block at world y can only change cells whose feet/head/floor touch it: yIdx y-1, y, y+1. If that
        // whole [y-1, y+1] span is outside this chunk's band, reclassifying reproduces identical cells - skip.
        if (bp.getY() + 1 < c.minY() || bp.getY() - 1 >= c.maxY()) return;
        int packed = ((bp.getZ() & 15) << 4) | (bp.getX() & 15);
        synchronized (DIRTY_LOCK) {
            IntOpenHashSet set = dirtyColumns.get(key);
            if (set == null) { set = new IntOpenHashSet(); dirtyColumns.put(key, set); }
            set.add(packed);
            DIRTY_GRIDS.put(this, level);
        }
    }

    // Reclassify up to `budget` dirty columns across all levels on the server tick (called from
    // PathfinderWorkerPool.onServerTick START phase, before processBuildQueue). Copy-on-write: patch a clone
    // of the live chunk and atomically swap it in, so A* worker threads never see a half-updated chunk and
    // reads never hit a missing (BLOCKED) chunk. Cheap (~height cells per column) and coalesced.
    public static void drainDirtyColumns(int budget) {
        if (budget <= 0) return;
        List<Map.Entry<WalkabilityGrid, Level>> pending;
        synchronized (DIRTY_LOCK) {
            if (DIRTY_GRIDS.isEmpty()) return;
            // Snapshot into STABLE entries: processDirty removes from DIRTY_GRIDS while we iterate, and an
            // IdentityHashMap's own Entry views would go stale under that mutation.
            pending = new ArrayList<>(DIRTY_GRIDS.size());
            for (Map.Entry<WalkabilityGrid, Level> e : DIRTY_GRIDS.entrySet())
                pending.add(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()));
        }
        for (Map.Entry<WalkabilityGrid, Level> e : pending) {
            if (budget <= 0) break;
            budget -= e.getKey().processDirty(e.getValue(), budget);
        }
    }

    // Drain this grid's dirty columns up to `budget`; returns the number reclassified.
    private int processDirty(Level level, int budget) {
        int consumed = 0;
        while (consumed < budget) {
            long key;
            IntArrayList pulled = new IntArrayList();
            synchronized (DIRTY_LOCK) {
                if (dirtyColumns.isEmpty()) { DIRTY_GRIDS.remove(this); break; }
                key = dirtyColumns.keySet().iterator().nextLong();
                IntOpenHashSet set = dirtyColumns.get(key);
                IntIterator it = set.iterator();
                while (it.hasNext() && consumed + pulled.size() < budget) {
                    pulled.add(it.nextInt());
                    it.remove();
                }
                if (set.isEmpty()) dirtyColumns.remove(key);
                if (dirtyColumns.isEmpty()) DIRTY_GRIDS.remove(this);
            }
            if (pulled.isEmpty()) break;
            WalkabilityGridChunk original;
            synchronized (chunks) { original = chunks.get(key); }
            // null -> chunk was LRU-evicted while dirty; drop the columns, a later getOrBuild rebuilds fresh.
            if (original != null) {
                WalkabilityGridChunk replacement = original.reclassifyColumns(
                        level, new ChunkPos(ChunkPos.getX(key), ChunkPos.getZ(key)), pulled);
                synchronized (chunks) {
                    // CAS: only swap if no concurrent getOrBuild replaced it with a fresher/taller chunk in the
                    // meantime (that rebuild already read at-least-as-new world state, so drop our patch). Plain
                    // put, not putAndMoveToFirst, so a passive recompute doesn't perturb LRU order.
                    if (chunks.get(key) == original) chunks.put(key, replacement);
                }
            }
            consumed += pulled.size();
        }
        return consumed;
    }

    // Drop all pending dirty work (server stop), so the static DIRTY_GRIDS can't pin Levels across a restart.
    public static void clearDirty() {
        synchronized (DIRTY_LOCK) { DIRTY_GRIDS.clear(); }
    }

    // Snapshot of the currently-built (cached) chunk keys, for the debug overlay.
    public long[] builtChunkKeys() {
        synchronized (chunks) { return chunks.keySet().toLongArray(); }
    }
}
