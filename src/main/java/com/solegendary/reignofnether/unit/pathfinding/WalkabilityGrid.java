package com.solegendary.reignofnether.unit.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
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

    // Chunks awaiting a deferred reclassify: chunkKey -> dirty entry (tick bounds to coalesce ACROSS ticks +
    // the bbox of the cells that changed). A block change marks its chunk dirty instead of evicting it, so reads
    // keep getting the (briefly stale) cached chunk rather than a BLOCKED hole that would strand units. Drained,
    // budgeted, on the server tick by drainDirtyChunks, which patches ONLY the changed region (reclassifyRegion)
    // - not all 256 columns over the full band. The crowd[] precompute is a ~5x5-neighbour scan per cell, so a
    // whole-chunk rebuild on a small footprint (eg. a worker placing a building's blocks a tick at a time) spiked
    // the main thread; region scope + the settle/ceiling coalescing keeps that cheap and rare.
    private final Long2ObjectOpenHashMap<DirtyChunk> dirtyChunks = new Long2ObjectOpenHashMap<>();

    // Per dirty chunk: the tick it first went dirty (anchors the hard-defer ceiling), the tick of its most
    // recent change (anchors the settle check), and the bbox of the cells that actually changed - local
    // columns [minLX..maxLX]/[minLZ..maxLZ] (0..15) and the WORLD-Y span [minY..maxY] (inclusive). The drain
    // reclassifies only that region (reclassifyRegion) instead of the whole chunk, so a building's footprint
    // churn doesn't rebuild all 256 columns over the full band.
    private static final class DirtyChunk {
        final long firstTick;
        long lastTick;
        int minLX = Integer.MAX_VALUE, maxLX = Integer.MIN_VALUE;
        int minLZ = Integer.MAX_VALUE, maxLZ = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        DirtyChunk(long now) { firstTick = now; lastTick = now; }
        void include(int lx, int lz, int yLo, int yHi) {
            if (lx < minLX) minLX = lx;
            if (lx > maxLX) maxLX = lx;
            if (lz < minLZ) minLZ = lz;
            if (lz > maxLZ) maxLZ = lz;
            if (yLo < minY) minY = yLo;
            if (yHi > maxY) maxY = yHi;
        }
    }

    // Monotonic server-tick clock, bumped once per drainDirtyChunks call (the drain runs once per server tick
    // START). Read by markChunkDirty to stamp dirty entries; the absolute value is irrelevant, only deltas.
    private static volatile long tickClock;

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

    // Mark the chunk containing bp dirty for a deferred rebuild, WITHOUT creating a grid for a level that has
    // none. Called from the LevelChunk.setBlockState mixin on every in-game block change (mining, building,
    // explosions, leaf decay, commands). No-op when no grid exists for the level (eg. client side, where the
    // pathfinder never runs) or the chunk isn't cached. We deliberately do NOT evict: keeping the old chunk
    // live lets reads return stale-but-walkable data until the deferred drain swaps in the rebuilt chunk,
    // instead of a removed chunk reading as KIND_BLOCKED and stranding units mid-update.
    public static void markChunkDirtyIfPresent(Level level, BlockPos bp) {
        if (level == null || bp == null) return;
        WalkabilityGrid grid;
        synchronized (WalkabilityGrid.class) { grid = PER_LEVEL.get(level); }
        if (grid != null) grid.markChunkDirty(level, bp);
    }

    private void markChunkDirty(Level level, BlockPos bp) {
        long key = ChunkPos.asLong(bp.getX() >> 4, bp.getZ() >> 4);
        WalkabilityGridChunk c;
        synchronized (chunks) { c = chunks.get(key); }
        if (c == null) return; // not cached -> nothing to patch; a later getOrBuild reads fresh world state.
        // A block at world y can only change cells whose feet/head/floor touch it: yIdx y-1, y, y+1. If that
        // whole [y-1, y+1] span is outside this chunk's band, a rebuild reproduces identical cells - skip.
        if (bp.getY() + 1 < c.minY() || bp.getY() - 1 >= c.maxY()) return;
        long now = tickClock;
        synchronized (DIRTY_LOCK) {
            DirtyChunk entry = dirtyChunks.get(key);
            if (entry == null) { entry = new DirtyChunk(now); dirtyChunks.put(key, entry); }
            entry.lastTick = now; // bump the settle window on every change; firstTick stays the ceiling anchor
            // Grow the changed-cell bbox by the same [y-1, y+1] span the early-out above reasons about (a block
            // at y can only change cells whose feet/head/floor touch it).
            entry.include(bp.getX() & 15, bp.getZ() & 15, bp.getY() - 1, bp.getY() + 1);
            DIRTY_GRIDS.put(this, level);
        }
    }

    // Rebuild up to `budget` SETTLED dirty chunks across all levels on the server tick (called from
    // PathfinderWorkerPool.onServerTick START phase, before processBuildQueue). Copy-on-write: build a fresh
    // chunk over the live world and atomically swap it in, so A* worker threads never see a half-updated chunk
    // and reads never hit a missing (BLOCKED) chunk. A chunk is rebuilt only once it has SETTLED (no change
    // for WALKABILITY_SETTLE_TICKS) or hit the WALKABILITY_MAX_DEFER_TICKS ceiling, so a building dripping
    // 1 block/tick coalesces into a single rebuild instead of one per tick.
    public static void drainDirtyChunks(int budget) {
        long now = ++tickClock; // one tick elapsed; stamps the settle/ceiling comparisons below
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
            budget -= e.getKey().processDirty(e.getValue(), budget, now);
        }
    }

    // Rebuild this grid's eligible (settled / ceiling-hit) dirty chunks, up to `budget` chunks; returns how
    // many were rebuilt. Chunks not yet settled stay in place (their stale-but-walkable cached chunk keeps
    // serving reads) and are revisited next tick.
    private int processDirty(Level level, int budget, long now) {
        // Pick the eligible chunk keys under the lock, then rebuild OUTSIDE it: build() reads the world and
        // must not hold DIRTY_LOCK while A* threads concurrently mark fresh chunks dirty.
        LongArrayList ready = new LongArrayList();
        List<DirtyChunk> readyEntries = new ArrayList<>(); // the bbox per ready key (it.remove() drops the entry)
        synchronized (DIRTY_LOCK) {
            LongIterator it = dirtyChunks.keySet().iterator();
            while (it.hasNext() && ready.size() < budget) {
                long key = it.nextLong();
                DirtyChunk d = dirtyChunks.get(key);
                boolean settled = (now - d.lastTick) >= PathfinderConfig.WALKABILITY_SETTLE_TICKS;
                boolean ceiling = (now - d.firstTick) >= PathfinderConfig.WALKABILITY_MAX_DEFER_TICKS;
                if (settled || ceiling) { ready.add(key); readyEntries.add(d); it.remove(); }
            }
            if (dirtyChunks.isEmpty()) DIRTY_GRIDS.remove(this);
        }
        // A block change arriving after we removed a key (but before/while we rebuild) re-marks the chunk with
        // a fresh entry, so it's rebuilt again next settle - nothing is lost, and reclassify reads current state.
        for (int i = 0; i < ready.size(); i++) {
            long key = ready.getLong(i);
            DirtyChunk d = readyEntries.get(i);
            WalkabilityGridChunk original;
            synchronized (chunks) { original = chunks.get(key); }
            // null -> chunk was LRU-evicted while dirty; drop it, a later getOrBuild rebuilds fresh.
            if (original == null) continue;
            ChunkPos cp = new ChunkPos(ChunkPos.getX(key), ChunkPos.getZ(key));
            // Patch only the changed region. A bbox spanning all 16x16 columns saves nothing over build(), so
            // degrade to a full rebuild there (also the exact-crowd path); otherwise reclassify just the footprint.
            boolean wholeChunk = d.minLX <= 0 && d.maxLX >= 15 && d.minLZ <= 0 && d.maxLZ >= 15;
            WalkabilityGridChunk replacement = wholeChunk
                    ? WalkabilityGridChunk.build(level, cp, original.minY(), original.maxY())
                    : original.reclassifyRegion(level, cp, d.minLX, d.maxLX, d.minLZ, d.maxLZ, d.minY, d.maxY);
            if (replacement == original) continue; // region fully outside the band -> nothing changed
            synchronized (chunks) {
                // CAS: only swap if no concurrent getOrBuild replaced it with a fresher/taller chunk in the
                // meantime (that rebuild already read at-least-as-new world state, so drop ours). Plain put,
                // not putAndMoveToFirst, so a passive rebuild doesn't perturb LRU order.
                if (chunks.get(key) == original) chunks.put(key, replacement);
            }
        }
        return ready.size();
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
