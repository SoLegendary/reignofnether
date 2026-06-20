package com.solegendary.reignofnether.unit.pathfinding;

import java.util.concurrent.atomic.AtomicLong;

public final class PathfinderMetrics {
    private PathfinderMetrics() {}

    private static final int WINDOW = 256;
    private static final long[] LATENCIES_NS = new long[WINDOW];
    private static volatile int writeIdx = 0;
    private static final AtomicLong TOTAL_COMPLETED = new AtomicLong(0);
    private static final AtomicLong TOTAL_FAILED = new AtomicLong(0);

    public static void recordCompletion(long ns) {
        LATENCIES_NS[writeIdx & (WINDOW - 1)] = ns;
        writeIdx++;
        TOTAL_COMPLETED.incrementAndGet();
    }

    public static void recordFailed() { TOTAL_FAILED.incrementAndGet(); }
    public static long totalCompleted() { return TOTAL_COMPLETED.get(); }
    public static long totalFailed() { return TOTAL_FAILED.get(); }

    public static double avgLatencyMs() {
        long sum = 0; int n = 0;
        for (long v : LATENCIES_NS) if (v > 0) { sum += v; n++; }
        return n == 0 ? 0.0 : (sum / (double) n) / 1_000_000.0;
    }

    public static double p99LatencyMs() {
        long[] copy = new long[WINDOW];
        int n = 0;
        for (long v : LATENCIES_NS) if (v > 0) copy[n++] = v;
        if (n == 0) return 0.0;
        java.util.Arrays.sort(copy, 0, n);
        int p99 = Math.max(0, (int) (n * 0.99) - 1);
        return copy[p99] / 1_000_000.0;
    }
}
