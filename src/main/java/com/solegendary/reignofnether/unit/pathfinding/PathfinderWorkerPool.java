package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.debug.RtsDebugServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.resources.ResourceIndex;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

@Mod.EventBusSubscriber(modid = ReignOfNether.MOD_ID)
public final class PathfinderWorkerPool {
    private PathfinderWorkerPool() {}

    private static volatile ExecutorService POOL;
    private static final ConcurrentLinkedQueue<Runnable> RESULTS = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger INFLIGHT = new AtomicInteger(0);

    // Worker-thread count is the live `pathfindingThreads` gamerule. currentThreads is the size the
    // running POOL was built with (main-thread only); desiredThreads is the latest requested size
    // (written from the gamerule command, applied at the next tick START). Hard-capped so a stray
    // value can't spawn a runaway pool.
    private static final int MAX_THREAD_CAP = 32;
    private static int currentThreads = 0;
    private static volatile int desiredThreads = 0;

    // Building a request's walkability grid (getBlockState/getCollisionShape) MUST happen on the main thread,
    // so classifying a whole corridor at once spikes the tick. Each request parks here and its cold chunks are
    // classified a budget at a time across ticks; once warm it dispatches to the pool. Main-thread-only.
    private static final ArrayDeque<PendingBuild> BUILD_QUEUE = new ArrayDeque<>();

    public static boolean isInitialised() {
        return POOL != null;
    }

    @SubscribeEvent
    public static void onServerStart(ServerStartingEvent evt) {
        if (POOL != null) return;
        int n = clampThreads(configuredThreads(evt.getServer()));
        POOL = newPool(n);
        currentThreads = n;
        desiredThreads = n;
        if (evt.getServer() != null && GameRuleRegistrar.PATHFINDING_CHUNK_BUILDS != null)
            setChunkBuildsPerTick(evt.getServer().getGameRules().getInt(GameRuleRegistrar.PATHFINDING_CHUNK_BUILDS));
        INFLIGHT.set(0);
        RESULTS.clear();
        BUILD_QUEUE.clear();
        ReignOfNether.LOGGER.info("RTS pathfinder pool started ({} thread{})", n, n == 1 ? "" : "s");
    }

    private static ExecutorService newPool(int n) {
        return Executors.newFixedThreadPool(n, r -> {
            Thread t = new Thread(r, "RtsPathfinderPool");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
    }

    private static int clampThreads(int n) {
        return Math.max(1, Math.min(n, MAX_THREAD_CAP));
    }

    // The pathfindingThreads gamerule value, falling back to the hardware-derived default if the rule
    // isn't available yet.
    private static int configuredThreads(MinecraftServer server) {
        if (server != null && GameRuleRegistrar.PATHFINDING_THREADS != null)
            return server.getGameRules().getInt(GameRuleRegistrar.PATHFINDING_THREADS);
        return PathfinderConfig.WORKER_THREADS;
    }

    // Requested by the gamerule command; the new size is applied at the next tick START (main thread).
    public static void requestResize(int n) {
        desiredThreads = clampThreads(n);
    }

    // Live cap on cold chunks warmed per tick (pathfindingChunkBuildsPerTick gamerule). Takes effect the next
    // tick; clamped so it can never stall warming (0) or spike TPS unbounded.
    public static void setChunkBuildsPerTick(int n) {
        PathfinderConfig.maxChunkBuildsPerTick = Math.max(1, Math.min(n, 64));
    }

    // Outstanding pathfinding work for the F7 "Queue" stat: requests parked for chunk warming plus those
    // running A* on the pool. This is the backlog that drives the "Path e2e" latency under load.
    // BUILD_QUEUE is main-thread-only, so call this from the main thread (the debug tick does).
    public static int queueDepth() {
        return BUILD_QUEUE.size() + INFLIGHT.get();
    }

    // Swap in a fresh pool of the requested size and let the old one drain gracefully. In-flight tasks
    // on the old pool still finish, post seq-guarded results to RESULTS, and decrement INFLIGHT, so we
    // leave INFLIGHT/RESULTS/BUILD_QUEUE untouched. Main-thread only (called from tick START).
    private static void applyPendingResize() {
        int target = desiredThreads;
        if (target <= 0 || target == currentThreads) return;
        ExecutorService old = POOL;
        POOL = newPool(target);
        currentThreads = target;
        if (old != null) old.shutdown();
        ReignOfNether.LOGGER.info("RTS pathfinder pool resized to {} thread{}", target, target == 1 ? "" : "s");
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppingEvent evt) {
        ExecutorService pool = POOL;
        POOL = null;
        if (pool != null) {
            pool.shutdownNow();
            try {
                pool.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        RESULTS.clear();
        BUILD_QUEUE.clear();
        WalkabilityGrid.clearDirty();
        ResourceIndex.clearAll();
        INFLIGHT.set(0);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        // START: rebuild settled dirty chunks, then classify a budget of cold corridor chunks and dispatch any
        // request now warm. Drain first so paths captured this tick see freshest data.
        if (evt.phase == TickEvent.Phase.START) {
            // apply a pending pathfindingThreads change before any dispatch this tick.
            applyPendingResize();
            // walkability caching + dispatch only run with the rtsPathfinding gamerule on (off = vanilla
            // pathfinding). the resource index is independent (used by gather goals either way), so always drains.
            if (UnitServerEvents.rtsPathfinding) {
                WalkabilityGrid.drainDirtyChunks(PathfinderConfig.MAX_CHUNK_RECLASSIFY_PER_TICK);
            }
            ResourceIndex.drainBuildQueue(ResourceIndex.MAX_RESOURCE_CHUNK_SCANS_PER_TICK);
            if (UnitServerEvents.rtsPathfinding) {
                processBuildQueue();
            }
            return;
        }
        // END: deliver finished paths back on the main thread.
        if (evt.phase != TickEvent.Phase.END) return;
        if (!UnitServerEvents.rtsPathfinding) return;
        Runnable r;
        int drained = 0;
        while (drained < 256 && (r = RESULTS.poll()) != null) {
            try { r.run(); } catch (Throwable t) { ReignOfNether.LOGGER.error("Path result handler failed", t); }
            drained++;
        }
    }

    public static void submit(Level level, BlockPos start, BlockPos target, int reach, MobilityClass mobility, int clearanceCells, int footprintRadius, float fireCost, boolean canClimb, int maxNodes, BooleanSupplier valid, PathCallback onReady) {
        if (POOL == null) {
            onReady.onPath(null, false);
            return;
        }
        // Queue saturated: deliver a busy signal (not a no-path), so the unit keeps its current path and
        // retries shortly instead of stopping and re-submitting every tick (the back-and-forth loop).
        if (BUILD_QUEUE.size() >= PathfinderConfig.QUEUE_BACKPRESSURE_CAP) {
            RESULTS.add(() -> onReady.onPath(null, true));
            return;
        }
        int dilation = PathfinderConfig.dilationFor(start, target);
        ChunkSnapshot.CaptureRegion region = ChunkSnapshot.regionFor(level, start, target, dilation);
        BUILD_QUEUE.add(new PendingBuild(level, start, target, reach, mobility, clearanceCells,
                footprintRadius, fireCost, canClimb, maxNodes, region, valid, onReady, System.nanoTime()));
    }

    // Classify up to maxChunkBuildsPerTick cold chunks across parked requests this tick. Cache hits are free
    // and don't spend budget, so already-warm corridors (eg. trailing units of a formation move) finish and
    // dispatch in the same tick they're queued.
    private static void processBuildQueue() {
        int budget = PathfinderConfig.maxChunkBuildsPerTick;
        while (budget > 0 && !BUILD_QUEUE.isEmpty()) {
            PendingBuild pb = BUILD_QUEUE.peekFirst();
            if (pb == null)
                break;
            // Drop work for units that died, were removed, or whose request was superseded by a newer order
            // (valid bundles isAlive + the goal's current seq), so stale entries don't burn chunk-build budget
            // and starve live requests. onPathReady discards the result via its own seq guard.
            if (pb.valid != null && !pb.valid.getAsBoolean()) {
                BUILD_QUEUE.pollFirst();
                pb.onReady.onPath(null, false);
                continue;
            }
            WalkabilityGrid grid = WalkabilityGrid.get(pb.level);
            ChunkSnapshot.CaptureRegion region = pb.region;
            int width = region.cz1 - region.cz0 + 1;
            int total = region.chunkCount();
            while (pb.cursor < total && budget > 0) {
                int cx = region.cx0 + pb.cursor / width;
                int cz = region.cz0 + pb.cursor % width;
                if (grid.isBuilt(cx, cz, region.wantMinY, region.wantMaxY)) {
                    pb.cursor++; // already cached - free
                } else {
                    try {
                        grid.getOrBuild(pb.level, cx, cz, region.wantMinY, region.wantMaxY);
                    } catch (Throwable t) {
                        ReignOfNether.LOGGER.error("Walkability build failed", t);
                    }
                    pb.cursor++;
                    budget--;
                }
            }
            if (pb.cursor >= total) {
                BUILD_QUEUE.pollFirst();
                dispatchToPool(pb); // corridor warm: capture (all hits) + run A* off-thread
            } else {
                break; // budget spent; resume this request next tick
            }
        }
    }

    // Assemble the (now fully cached) snapshot and run the chained A* on the worker pool.
    private static void dispatchToPool(PendingBuild pb) {
        ExecutorService pool = POOL;
        if (pool == null) {
            pb.onReady.onPath(null, false);
            return;
        }
        // Pool saturated: busy signal (see submit) so the unit doesn't stop/resubmit-loop under load.
        if (INFLIGHT.get() >= PathfinderConfig.QUEUE_BACKPRESSURE_CAP) {
            RESULTS.add(() -> pb.onReady.onPath(null, true));
            return;
        }

        ChunkSnapshot snapshot;
        try {
            int dilation = PathfinderConfig.dilationFor(pb.start, pb.target);
            snapshot = ChunkSnapshot.capture(pb.level, pb.start, pb.target, dilation, pb.mobility,
                    pb.clearanceCells, pb.footprintRadius, pb.fireCost, pb.canClimb);
        } catch (Throwable t) {
            ReignOfNether.LOGGER.error("ChunkSnapshot capture failed", t);
            pb.onReady.onPath(null, false);
            return;
        }

        final BlockPos start = pb.start;
        final BlockPos target = pb.target;
        final int reach = pb.reach;
        final int maxNodes = pb.maxNodes;
        final PathCallback onReady = pb.onReady;
        final long submitNanos = pb.submitNanos;
        INFLIGHT.incrementAndGet();
        pool.execute(() -> {
            Path result = null;
            long computeStart = System.nanoTime();
            try {
                // Chained A*: when the goal is outside the 96-block search radius, run a follow-up search from
                // where the last one ended rather than returning a partial path. Up to MAX_CHAIN_SEGMENTS hops,
                // then give up. An unreachable-goal (capped) request runs a single best-effort segment: nothing
                // to chain toward, and chaining would multiply its cheap budget.
                int segLimit = (maxNodes >= PathfinderConfig.MAX_NODES) ? PathfinderConfig.MAX_CHAIN_SEGMENTS : 1;
                ArrayList<BlockPos> combined = new ArrayList<>();
                BlockPos cur = start;
                boolean reached = false;
                for (int seg = 0; seg < segLimit; seg++) {
                    GridAStar.Result r = GridAStar.search(snapshot, cur, target, reach,
                            PathfinderConfig.MAX_RADIUS, maxNodes);
                    if (seg == 0) {
                        combined.addAll(r.waypoints);
                    } else {
                        // skip first waypoint (= cur, already last waypoint of previous segment)
                        for (int j = 1; j < r.waypoints.size(); j++) combined.add(r.waypoints.get(j));
                    }
                    if (r.reached) { reached = true; break; }
                    if (r.waypoints.size() < 2) break; // no forward progress, give up
                    BlockPos next = r.waypoints.get(r.waypoints.size() - 1);
                    if (next.getX() == cur.getX() && next.getZ() == cur.getZ()) break; // stuck
                    cur = next;
                }
                result = PathConverter.toMcPath(combined, target, reached, snapshot);
            } catch (Throwable t) {
                ReignOfNether.LOGGER.error("Worker A* failed", t);
            } finally {
                INFLIGHT.decrementAndGet();
                // worker-thread compute time (pure A*); accumulated thread-safely for the F7 "Path ms" stat.
                RtsDebugServerEvents.recordPathCompute(System.nanoTime() - computeStart);
            }
            final Path delivered = result;
            RESULTS.add(() -> {
                // end-to-end latency (submit -> delivered, incl. queue wait); recorded on the main thread
                // for the F7 "Path e2e" stat.
                RtsDebugServerEvents.recordPathE2e(System.nanoTime() - submitNanos);
                onReady.onPath(delivered, false);
            });
        });
    }

    // A parked path request: everything needed to warm its corridor and then run A*, plus a cursor over the
    // region's chunks (row-major, matching ChunkSnapshot.capture's iteration order).
    private static final class PendingBuild {
        final Level level;
        final BlockPos start;
        final BlockPos target;
        final int reach;
        final MobilityClass mobility;
        final int clearanceCells;
        final int footprintRadius;
        final float fireCost;
        final boolean canClimb;
        final int maxNodes;
        final ChunkSnapshot.CaptureRegion region;
        final BooleanSupplier valid;
        final PathCallback onReady;
        final long submitNanos;
        int cursor = 0;

        PendingBuild(Level level, BlockPos start, BlockPos target, int reach, MobilityClass mobility,
                     int clearanceCells, int footprintRadius, float fireCost, boolean canClimb, int maxNodes,
                     ChunkSnapshot.CaptureRegion region, BooleanSupplier valid, PathCallback onReady, long submitNanos) {
            this.level = level;
            this.start = start;
            this.target = target;
            this.reach = reach;
            this.mobility = mobility;
            this.clearanceCells = clearanceCells;
            this.footprintRadius = footprintRadius;
            this.fireCost = fireCost;
            this.canClimb = canClimb;
            this.maxNodes = maxNodes;
            this.region = region;
            this.valid = valid;
            this.onReady = onReady;
            this.submitNanos = submitNanos;
        }
    }
}
