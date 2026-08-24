package com.solegendary.reignofnether.debug;

import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.pathfinding.PathfinderWorkerPool;
import com.solegendary.reignofnether.unit.pathfinding.WalkabilityGrid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.concurrent.atomic.LongAdder;

public class RtsDebugServerEvents {

    private static final long[] UNLOADED = new long[]{0L};

    private static int updateTicks = 0;

    // Debug counter: incremented each time a goal calls mob.getNavigation().createPath().
    // Sampled and reset once per second by the rts-debug stats tick handler.
    public static int debugPathCalcsThisSecond = 0;

    // 5-second rolling buffers for the debug overlay. Indexed mod STATS_WINDOW.
    private static final int STATS_WINDOW = 5;
    private static final int[] pathsHistory = new int[STATS_WINDOW];
    private static final int[] queueHistory = new int[STATS_WINDOW];
    private static final int[] stuckHistory = new int[STATS_WINDOW];
    private static int statsIndex = 0;
    // Queue is sampled every tick (cheap, captures bursts) and averaged at second boundaries.
    private static long queueSumThisSecond = 0;
    private static int queueSamplesThisSecond = 0;

    // Path-timing accumulators, averaged once per second for the F7 "Path ms" / "Path e2e" stats.
    // compute = pure A* time on the worker threads (written concurrently -> LongAdder). e2e = submit ->
    // delivered wall time incl. queue wait (recorded only on the main thread at result delivery).
    private static final LongAdder pathComputeNanos = new LongAdder();
    private static final LongAdder pathComputeCount = new LongAdder();
    private static long pathE2eNanos = 0;
    private static int pathE2eCount = 0;

    public static void recordPathCompute(long nanos) {
        pathComputeNanos.add(nanos);
        pathComputeCount.increment();
    }

    // Main-thread only (called from the END-phase result drain).
    public static void recordPathE2e(long nanos) {
        pathE2eNanos += nanos;
        pathE2eCount += 1;
    }

    // logic borrowed from net.minecraftforge.server.command.TPSCommand
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        // Sample the pathfinder backlog (parked + in-flight requests) every tick (cheap, captures bursts);
        // averaged at the once-per-second boundary below. This is the queue that backs up under load and
        // drives "Path e2e" - the formation dispatch queue drains ~instantly and read ~0.
        queueSumThisSecond += PathfinderWorkerPool.queueDepth();
        queueSamplesThisSecond += 1;

        MinecraftServer server = evt.getServer();
        long[] times = server.getTickTime(Level.OVERWORLD);
        if (times == null)
            times = UNLOADED;

        double worldTickTime = (double)mean(times) * 1.0E-6;

        updateTicks += 1;
        if (updateTicks < 20) return;
        updateTicks = 0;
        int paths = debugPathCalcsThisSecond;
        debugPathCalcsThisSecond = 0;
        int stuck = 0;
        for (LivingEntity e : UnitServerEvents.getAllUnits()) {
            if (e instanceof Unit u) {
                var mg = u.getMoveGoal();
                if (mg != null && mg.isInBackoff()) stuck += 1;
            }
        }
        int avgQueueThisSec = queueSamplesThisSecond > 0 ? (int) (queueSumThisSecond / queueSamplesThisSecond) : 0;
        queueSumThisSecond = 0;
        queueSamplesThisSecond = 0;
        pathsHistory[statsIndex] = paths;
        queueHistory[statsIndex] = avgQueueThisSec;
        stuckHistory[statsIndex] = stuck;
        statsIndex = (statsIndex + 1) % STATS_WINDOW;

        long computeNanos = pathComputeNanos.sumThenReset();
        long computeCount = pathComputeCount.sumThenReset();
        double avgComputeMs = computeCount > 0 ? (computeNanos / (double) computeCount) * 1.0E-6 : 0.0;
        double avgE2eMs = pathE2eCount > 0 ? (pathE2eNanos / (double) pathE2eCount) * 1.0E-6 : 0.0;
        pathE2eNanos = 0;
        pathE2eCount = 0;

        RtsDebugStatsClientboundPacket.broadcast(
                avg(pathsHistory), avg(queueHistory), avg(stuckHistory), worldTickTime, avgComputeMs, avgE2eMs);
        // Built navmesh chunks (overworld) so the debug overlay can show what's cached.
        if (server.overworld() != null)
            RtsDebugChunksClientboundPacket.broadcast(WalkabilityGrid.get(server.overworld()).builtChunkKeys());
    }

    private static long mean(long[] values) {
        long sum = 0L;
        for (long v : values)
            sum += v;
        return sum / (long)values.length;
    }

    private static int avg(int[] buf) {
        int sum = 0;
        for (int v : buf) sum += v;
        return sum / buf.length;
    }
}
