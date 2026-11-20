package com.solegendary.reignofnether.resources;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Resources each player dropped off and spent in the last minute, for the "+N/MIN" lines and tooltips on the HUD. */
public final class IncomeTracker {

    private IncomeTracker() { }

    private static final long WINDOW_MS = 60_000;

    private record Deposit(long time, int food, int wood, int ore) { }

    private static final Map<String, ArrayDeque<Deposit>> deposits = new HashMap<>();
    // ponytail: refunds (cancelled production) aren't taken off, so spend can read a bit high
    private static final Map<String, ArrayDeque<Deposit>> spends = new HashMap<>();

    public static synchronized void record(String owner, int food, int wood, int ore) {
        add(deposits, owner, food, wood, ore);
    }

    /** From a resource change: its negative parts are spending. */
    public static synchronized void recordSpend(String owner, int food, int wood, int ore) {
        if (food < 0 || wood < 0 || ore < 0)
            add(spends, owner, -Math.min(0, food), -Math.min(0, wood), -Math.min(0, ore));
    }

    public static synchronized int perMinute(String owner, ResourceName resource) {
        return sum(deposits, owner, resource);
    }

    public static synchronized int spentPerMinute(String owner, ResourceName resource) {
        return sum(spends, owner, resource);
    }

    private static void add(Map<String, ArrayDeque<Deposit>> map, String owner, int food, int wood, int ore) {
        map.computeIfAbsent(owner, k -> new ArrayDeque<>()).addLast(new Deposit(System.currentTimeMillis(), food, wood, ore));
    }

    private static int sum(Map<String, ArrayDeque<Deposit>> map, String owner, ResourceName resource) {
        ArrayDeque<Deposit> list = map.get(owner);
        if (list == null)
            return 0;
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        while (!list.isEmpty() && list.peekFirst().time < cutoff)
            list.removeFirst();
        int total = 0;
        for (Deposit d : list)
            total += switch (resource) {
                case FOOD -> d.food;
                case WOOD -> d.wood;
                case ORE -> d.ore;
                default -> 0;
            };
        return total;
    }

    public static synchronized void reset() {
        deposits.clear();
        spends.clear();
    }
}
