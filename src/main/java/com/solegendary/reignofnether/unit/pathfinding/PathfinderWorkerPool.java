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

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = ReignOfNether.MOD_ID)
public final class PathfinderWorkerPool {
    private PathfinderWorkerPool() {}

    private static volatile ExecutorService POOL;
    private static final ConcurrentLinkedQueue<Runnable> RESULTS = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger INFLIGHT = new AtomicInteger(0);

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
        INFLIGHT.set(0);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;
        Runnable r;
        int drained = 0;
        while (drained < 256 && (r = RESULTS.poll()) != null) {
            try { r.run(); } catch (Throwable t) { ReignOfNether.LOGGER.error("Path result handler failed", t); }
            drained++;
        }
    }

    public static void submit(Level level, BlockPos start, BlockPos target, int reach, MobilityClass mobility, Consumer<Path> onReady) {
        ExecutorService pool = POOL;
        if (pool == null) {
            onReady.accept(null);
            return;
        }
        if (INFLIGHT.get() >= PathfinderConfig.QUEUE_BACKPRESSURE_CAP) {
            RESULTS.add(() -> onReady.accept(null));
            return;
        }

        ChunkSnapshot snapshot;
        try {
            int dilation = PathfinderConfig.dilationFor(start, target);
            snapshot = ChunkSnapshot.capture(level, start, target, dilation, mobility);
        } catch (Throwable t) {
            ReignOfNether.LOGGER.error("ChunkSnapshot capture failed", t);
            onReady.accept(null);
            return;
        }

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
}
