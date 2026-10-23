package com.solegendary.reignofnether.resources;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Resources each player dropped off in the last minute, for the "+N/MIN" lines on the HUD. */
public final class IncomeTracker {

    private IncomeTracker() { }

    private static final long WINDOW_MS = 60_000;

    private record Deposit(long time, int food, int wood, int ore) { }

    private static final Map<String, ArrayDeque<Deposit>> deposits = new HashMap<>();

    public static synchronized void record(String owner, int food, int wood, int ore) {
        deposits.computeIfAbsent(owner, k -> new ArrayDeque<>()).addLast(new Deposit(System.currentTimeMillis(), food, wood, ore));
    }

    public static synchronized int perMinute(String owner, ResourceName resource) {
        ArrayDeque<Deposit> list = deposits.get(owner);
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
    }
}
