package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.core.BlockPos;

public final class PathfinderConfig {
    private PathfinderConfig() {}

    public static final int MAX_RADIUS = 96;
    // Worst-case node budget per A* segment; only fully spent on hard detours / unreachable targets. High
    // enough to flood the reachable area within MAX_RADIUS so units round obstacles instead of giving up at
    // the near wall. Past ~200k the radius cap, not this, is the limit (raise MAX_RADIUS + MIN_DILATION).
    public static final int MAX_NODES = 250000;
    // Node budget for a search whose goal cell has NO standable spot nearby (eg. an airborne canopy leaf a
    // worker can never stand to mine). The goal is unreachable as an exact cell, so a normal search would
    // flood the full MAX_NODES proving it (~250ms). Cap it low: the worker still best-effort approaches, the
    // search returns in a few ms, and the gather goal drops the unreachable target. Chaining is also skipped
    // for this case (see dispatchToPool) since there's nothing to chain toward.
    public static final int MAX_NODES_UNREACHABLE_GOAL = 8000;
    public static final int MAX_CHAIN_SEGMENTS = 10;
    public static final int MIN_DILATION = 48;
    public static final int QUEUE_BACKPRESSURE_CAP = 500;
    // Walkability-grid building (getBlockState/getCollisionShape + the crowd[] precompute) must run on the main
    // thread, so a single cross-map move classifying its whole corridor in one tick spikes the TPS. Cap how many
    // cold (uncached) chunks the build queue classifies per server tick and defer the rest; cache hits are free
    // and don't count. Lower = smoother TPS but slower first path, higher = snappier first path but bigger
    // per-tick cost. Kept modest because each cold build now also bakes crowd[] (a ~5x5-neighbour scan per cell),
    // so a chunk costs noticeably more than it did before that was cached - a batch of fresh path orders (eg.
    // units just produced from a building) shouldn't classify a big batch of chunks in one tick.
    public static final int MAX_CHUNK_BUILDS_PER_TICK = 8;
    // A block change marks its chunk dirty (instead of evicting it) and records the changed-cell bbox; the
    // START-phase drain patches only that region via WalkabilityGridChunk.reclassifyRegion (a clone + a small
    // footprint reclassify, NOT a whole-chunk build). Cap how many distinct dirty chunks are patched per server
    // tick; the rest stay stale-but-walkable (never removed, so units are never stranded) and wait for a later
    // tick. Same order as MAX_CHUNK_BUILDS_PER_TICK, which the cold-build path already tolerates. Tunable.
    public static final int MAX_CHUNK_RECLASSIFY_PER_TICK = 8;
    // Cross-tick coalescing for the dirty-chunk drain. A chunk is only rebuilt once it has SETTLED (no new
    // block change for this many ticks), so a building placing 1 block/tick rebuilds ~once when it finishes
    // instead of every tick of its construction. The cached chunk stays stale-but-walkable in the meantime,
    // exactly as it already does between mark and drain.
    public static final int WALKABILITY_SETTLE_TICKS = 4;
    // Hard ceiling on the settle wait: a chunk that never goes quiet (active farm, a long mining job) is
    // force-reclassified once it has been continuously dirty this long, so staleness is bounded (~2s) even
    // under perpetual churn. Overrides WALKABILITY_SETTLE_TICKS.
    public static final int WALKABILITY_MAX_DEFER_TICKS = 40;
    // A* searches stay near the surface; only classify a Y band around the path rather than the full
    // world column. SLACK covers the Y+-1 step nodes and goal snapping that reach just outside the band.
    public static final int VERTICAL_RADIUS = 24;
    public static final int VERTICAL_WINDOW_SLACK = 8;

    // Clearance (unit height in cells) the crowding malus is PRECOMPUTED with, since the cached per-cell crowd[]
    // is shared by all units. 2 = the common/minimum unit clearance; taller units get a negligibly softer
    // steering malus, never a correctness change (wideFits/headBlocked stay exact and per-unit). See
    // WalkabilityGridChunk.crowd and GridNeighbors.crowdingMalus.
    public static final int MALUS_CLEARANCE = 2;

    // The grid's neighbour model only steps +-1 in Y, so it can't express a sheer drop bigger than one block -
    // a unit would stall at the lip of a 2+ block descent (a tall staircase) with no node to advance to, and a
    // wide body spins on the ledge. Vanilla allows multi-block falls; so do we, up to MAX_FALL_DROP blocks, with
    // a small per-block malus so a unit still prefers a gentle route when one is comparably short but commits to
    // the drop rather than fearing it. 3 matches vanilla's roughly-safe fall threshold.
    public static final int MAX_FALL_DROP = 3;
    public static final float FALL_COST_PER_BLOCK = 0.3f;

    // Bound the per-level walkability chunk cache so long games don't grow it without limit.
    public static final int MAX_CACHED_CHUNKS = 1024;

    // Cost multiplier a unit pays to path across a fire/magma/campfire cell when it is NOT fire-immune and
    // its DAMAGE_FIRE pathfinding malus marks fire as dangerous. Fire-immune units pay 1x instead (see
    // RtsPathfinder.fireCostFor), so they cross fire freely while everyone else routes around it.
    public static final float FIRE_AVOID_COST = 50.0f;
    public static final float SLIME_AVOID_COST = 10.0f;

    // A* over an immutable snapshot is embarrassingly parallel; scale workers with cores.
    public static final int WORKER_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

    // A unit within this squared distance of its move target is "arrived": separation stops pushing it and
    // the move goal stops recalculating, so a settled/formed-up group doesn't jitter on the spot. Shared by
    // UnitSeparation and MoveToTargetBlockGoal.getMinDistToRecalculateSqr so the two thresholds can't drift.
    public static final double ARRIVAL_SETTLE_SQ = 2.25; // 1.5 blocks

    // Per-tick separation steering between crowding, same-owner, moving units.
    public static final double SEPARATION_RADIUS = 1.6;
    public static final double SEPARATION_STRENGTH = 0.04;
    public static final int SEPARATION_MAX_PER_TICK = 400;
    public static final int SEPARATION_CELL_SIZE = 2;

    public static int dilationFor(BlockPos start, BlockPos target) {
        int manhattan = Math.abs(start.getX() - target.getX()) + Math.abs(start.getZ() - target.getZ());
        return Math.min(MAX_RADIUS, Math.max(MIN_DILATION, manhattan / 2));
    }
}
