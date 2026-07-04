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
    // access-ordered (most-recent at head) so we evict the LRU tail
    private final Long2ObjectLinkedOpenHashMap<WalkabilityGridChunk> chunks = new Long2ObjectLinkedOpenHashMap<>();

    // chunks awaiting a deferred reclassify. a block change marks its chunk dirty instead of evicting it, so reads
    // keep returning the briefly-stale cached chunk rather than a BLOCKED hole that strands units. drained budgeted
    // per server tick by drainDirtyChunks, which patches only the changed region (not the whole 256-column band) -
    // the crowd[] precompute is a ~5x5 scan per cell, so whole-chunk rebuilds on a small footprint spiked the main
    // thread; region scope + settle/ceiling coalescing keeps it cheap.
    private final Long2ObjectOpenHashMap<DirtyChunk> dirtyChunks = new Long2ObjectOpenHashMap<>();

    // per dirty chunk: firstTick (anchors the hard-defer ceiling), lastTick (anchors the settle check), and the
    // bbox of changed cells - local columns [minLX..maxLX]/[minLZ..maxLZ] (0..15) and WORLD-Y span [minY..maxY]
    // (inclusive). the drain reclassifies only this region instead of the whole chunk.
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

    // monotonic tick clock, bumped once per drainDirtyChunks (once per server tick). only deltas matter.
    private static volatile long tickClock;

    // grids with pending dirty work -> the Level needed to reclassify (grids are keyed by LevelAccessor and hold no
    // back-reference). the Level is held only while work is pending (entry removed when the dirty set empties), so
    // this transient strong ref can't pin the PER_LEVEL weak key. guarded by DIRTY_LOCK.
    private static final Map<WalkabilityGrid, Level> DIRTY_GRIDS = new IdentityHashMap<>();
    private static final Object DIRTY_LOCK = new Object();

    private WalkabilityGrid() {}

    public static synchronized WalkabilityGrid get(LevelAccessor level) {
        return PER_LEVEL.computeIfAbsent(level, k -> new WalkabilityGrid());
    }

    // returns a chunk whose Y band covers [wantMinY, wantMaxY). if a cached chunk's band is too small it's rebuilt
    // over the union of old+requested bands, so requests at different Y converge to one taller band instead of thrashing.
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

    // peek whether a cached chunk already covers [wantMinY, wantMaxY) without building one - lets the deferred
    // build queue tell a free cache hit from a cold build and spend its per-tick budget only on the latter.
    public boolean isBuilt(int chunkX, int chunkZ, int wantMinY, int wantMaxY) {
        synchronized (chunks) {
            WalkabilityGridChunk c = chunks.get(ChunkPos.asLong(chunkX, chunkZ));
            return c != null && c.covers(wantMinY, wantMaxY);
        }
    }

    // mark the chunk containing bp dirty for a deferred rebuild, without creating a grid for a level that has none.
    // called from the LevelChunk.setBlockState mixin on every block change. no-op if the level has no grid (eg.
    // client side) or the chunk isn't cached. we don't evict: keeping the old chunk live lets reads stay
    // stale-but-walkable until the drain swaps in the rebuild, vs. a missing chunk reading as KIND_BLOCKED.
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
        if (c == null) return; // not cached -> nothing to patch; a later getOrBuild reads fresh state
        // a block at y only changes cells whose feet/head/floor touch it: y-1, y, y+1. if that span is outside
        // this chunk's band, a rebuild reproduces identical cells - skip.
        if (bp.getY() + 1 < c.minY() || bp.getY() - 1 >= c.maxY()) return;
        long now = tickClock;
        synchronized (DIRTY_LOCK) {
            DirtyChunk entry = dirtyChunks.get(key);
            if (entry == null) { entry = new DirtyChunk(now); dirtyChunks.put(key, entry); }
            entry.lastTick = now; // bump the settle window; firstTick stays the ceiling anchor
            // grow the changed-cell bbox by that same [y-1, y+1] span
            entry.include(bp.getX() & 15, bp.getZ() & 15, bp.getY() - 1, bp.getY() + 1);
            DIRTY_GRIDS.put(this, level);
        }
    }

    // rebuild up to `budget` settled dirty chunks across all levels (called from PathfinderWorkerPool.onServerTick
    // START, before processBuildQueue). copy-on-write: build a fresh chunk over the live world and atomically swap
    // it in, so A* threads never see a half-updated chunk and reads never hit a missing (BLOCKED) one. a chunk is
    // rebuilt only once settled (no change for WALKABILITY_SETTLE_TICKS) or past the WALKABILITY_MAX_DEFER_TICKS
    // ceiling, so a building dripping 1 block/tick coalesces into a single rebuild.
    public static void drainDirtyChunks(int budget) {
        long now = ++tickClock; // one tick elapsed; stamps the settle/ceiling comparisons below
        if (budget <= 0) return;
        List<Map.Entry<WalkabilityGrid, Level>> pending;
        synchronized (DIRTY_LOCK) {
            if (DIRTY_GRIDS.isEmpty()) return;
            // snapshot into stable entries: processDirty removes from DIRTY_GRIDS while we iterate, and an
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

    // rebuild this grid's eligible (settled / ceiling-hit) dirty chunks, up to `budget`; returns how many were
    // rebuilt. not-yet-settled chunks stay in place (stale-but-walkable cache keeps serving) and are revisited next tick.
    private int processDirty(Level level, int budget, long now) {
        // pick eligible keys under the lock, then rebuild outside it: build() reads the world and must not hold
        // DIRTY_LOCK while A* threads concurrently mark fresh chunks dirty.
        LongArrayList ready = new LongArrayList();
        List<DirtyChunk> readyEntries = new ArrayList<>(); // bbox per ready key (it.remove() drops the entry)
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
        // a block change arriving after we removed a key (but before/while we rebuild) re-marks the chunk with a
        // fresh entry, rebuilt again next settle - nothing is lost, and reclassify reads current state.
        for (int i = 0; i < ready.size(); i++) {
            long key = ready.getLong(i);
            DirtyChunk d = readyEntries.get(i);
            WalkabilityGridChunk original;
            synchronized (chunks) { original = chunks.get(key); }
            // null -> LRU-evicted while dirty; drop it, a later getOrBuild rebuilds fresh
            if (original == null) continue;
            ChunkPos cp = new ChunkPos(ChunkPos.getX(key), ChunkPos.getZ(key));
            // a bbox spanning all 16x16 columns saves nothing over build(), so full-rebuild there; otherwise
            // reclassify just the footprint.
            boolean wholeChunk = d.minLX <= 0 && d.maxLX >= 15 && d.minLZ <= 0 && d.maxLZ >= 15;
            WalkabilityGridChunk replacement = wholeChunk
                    ? WalkabilityGridChunk.build(level, cp, original.minY(), original.maxY())
                    : original.reclassifyRegion(level, cp, d.minLX, d.maxLX, d.minLZ, d.maxLZ, d.minY, d.maxY);
            if (replacement == original) continue; // region fully outside the band -> nothing changed
            synchronized (chunks) {
                // CAS: only swap if no concurrent getOrBuild already replaced it with a fresher/taller chunk
                // (that rebuild read at-least-as-new state, so drop ours). plain put, not putAndMoveToFirst,
                // so a passive rebuild doesn't perturb LRU order.
                if (chunks.get(key) == original) chunks.put(key, replacement);
            }
        }
        return ready.size();
    }

    // drop all pending dirty work (server stop) so static DIRTY_GRIDS can't pin Levels across a restart
    public static void clearDirty() {
        synchronized (DIRTY_LOCK) { DIRTY_GRIDS.clear(); }
    }

    // snapshot of currently-cached chunk keys, for the debug overlay
    public long[] builtChunkKeys() {
        synchronized (chunks) { return chunks.keySet().toLongArray(); }
    }
}
