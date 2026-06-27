package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.BlockPos;
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
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = ReignOfNether.MOD_ID)
public final class PathfinderWorkerPool {
    private PathfinderWorkerPool() {}

    private static volatile ExecutorService POOL;
    private static final ConcurrentLinkedQueue<Runnable> RESULTS = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger INFLIGHT = new AtomicInteger(0);

    // Building a request's walkability grid (getBlockState/getCollisionShape) MUST happen on the main thread,
    // so a cross-map move that classifies its whole corridor at once spikes the tick. Instead each request is
    // parked here and its cold chunks are classified a budget at a time across ticks; once warm it's dispatched
    // to the worker pool. Main-thread-only (submit runs in the goal tick, draining runs in onServerTick).
    private static final ArrayDeque<PendingBuild> BUILD_QUEUE = new ArrayDeque<>();

    public static boolean isInitialised() {
        return POOL != null;
    }

    @SubscribeEvent
    public static void onServerStart(ServerStartingEvent evt) {
        if (POOL != null) return;
        int n = Math.max(1, PathfinderConfig.WORKER_THREADS);
        POOL = Executors.newFixedThreadPool(n, r -> {
            Thread t = new Thread(r, "RtsPathfinderPool");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        INFLIGHT.set(0);
        RESULTS.clear();
        BUILD_QUEUE.clear();
        ReignOfNether.LOGGER.info("RTS pathfinder pool started ({} thread{})", n, n == 1 ? "" : "s");
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
        INFLIGHT.set(0);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        // START: classify a budget of cold corridor chunks and dispatch any request that's now warm.
        if (evt.phase == TickEvent.Phase.START) {
            processBuildQueue();
            return;
        }
        // END: deliver finished paths back on the main thread.
        if (evt.phase != TickEvent.Phase.END) return;
        Runnable r;
        int drained = 0;
        while (drained < 256 && (r = RESULTS.poll()) != null) {
            try { r.run(); } catch (Throwable t) { ReignOfNether.LOGGER.error("Path result handler failed", t); }
            drained++;
        }
    }

    public static void submit(Level level, BlockPos start, BlockPos target, int reach, MobilityClass mobility, int clearanceCells, int footprintRadius, float fireCost, boolean canClimb, BooleanSupplier alive, Consumer<Path> onReady) {
        if (POOL == null) {
            onReady.accept(null);
            return;
        }
        if (BUILD_QUEUE.size() >= PathfinderConfig.QUEUE_BACKPRESSURE_CAP) {
            RESULTS.add(() -> onReady.accept(null));
            return;
        }
        int dilation = PathfinderConfig.dilationFor(start, target);
        ChunkSnapshot.CaptureRegion region = ChunkSnapshot.regionFor(start, target, dilation);
        BUILD_QUEUE.add(new PendingBuild(level, start, target, reach, mobility, clearanceCells,
                footprintRadius, fireCost, canClimb, region, alive, onReady));
    }

    // Classify up to MAX_CHUNK_BUILDS_PER_TICK cold chunks across the parked requests this tick. Cache hits are
    // free and don't spend budget, so warm/retry corridors (and the trailing units of a formation move) finish
    // their build in the same tick they're queued and dispatch immediately.
    private static void processBuildQueue() {
        int budget = PathfinderConfig.MAX_CHUNK_BUILDS_PER_TICK;
        while (budget > 0 && !BUILD_QUEUE.isEmpty()) {
            PendingBuild pb = BUILD_QUEUE.peekFirst();
            // Drop work for units that died/were removed while waiting, so they don't starve live requests.
            if (pb.alive != null && !pb.alive.getAsBoolean()) {
                BUILD_QUEUE.pollFirst();
                pb.onReady.accept(null);
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
            pb.onReady.accept(null);
            return;
        }
        if (INFLIGHT.get() >= PathfinderConfig.QUEUE_BACKPRESSURE_CAP) {
            RESULTS.add(() -> pb.onReady.accept(null));
            return;
        }

        ChunkSnapshot snapshot;
        try {
            int dilation = PathfinderConfig.dilationFor(pb.start, pb.target);
            snapshot = ChunkSnapshot.capture(pb.level, pb.start, pb.target, dilation, pb.mobility,
                    pb.clearanceCells, pb.footprintRadius, pb.fireCost, pb.canClimb);
        } catch (Throwable t) {
            ReignOfNether.LOGGER.error("ChunkSnapshot capture failed", t);
            pb.onReady.accept(null);
            return;
        }

        final BlockPos start = pb.start;
        final BlockPos target = pb.target;
        final int reach = pb.reach;
        final Consumer<Path> onReady = pb.onReady;
        INFLIGHT.incrementAndGet();
        pool.execute(() -> {
            Path result = null;
            try {
                // Chained A*: instead of returning a partial path when goal is outside the
                // 96-block search radius, run a follow-up search from where the previous one
                // ended. Up to MAX_CHAIN_SEGMENTS hops. Caps at "actually blocked" after that.
                ArrayList<BlockPos> combined = new ArrayList<>();
                BlockPos cur = start;
                boolean reached = false;
                for (int seg = 0; seg < PathfinderConfig.MAX_CHAIN_SEGMENTS; seg++) {
                    GridAStar.Result r = GridAStar.search(snapshot, cur, target, reach,
                            PathfinderConfig.MAX_RADIUS, PathfinderConfig.MAX_NODES);
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
            }
            final Path delivered = result;
            RESULTS.add(() -> onReady.accept(delivered));
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
        final ChunkSnapshot.CaptureRegion region;
        final BooleanSupplier alive;
        final Consumer<Path> onReady;
        int cursor = 0;

        PendingBuild(Level level, BlockPos start, BlockPos target, int reach, MobilityClass mobility,
                     int clearanceCells, int footprintRadius, float fireCost, boolean canClimb,
                     ChunkSnapshot.CaptureRegion region, BooleanSupplier alive, Consumer<Path> onReady) {
            this.level = level;
            this.start = start;
            this.target = target;
            this.reach = reach;
            this.mobility = mobility;
            this.clearanceCells = clearanceCells;
            this.footprintRadius = footprintRadius;
            this.fireCost = fireCost;
            this.canClimb = canClimb;
            this.region = region;
            this.alive = alive;
            this.onReady = onReady;
        }
    }
}
