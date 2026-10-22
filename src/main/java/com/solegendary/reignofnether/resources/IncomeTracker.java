package com.solegendary.reignofnether.resources;

import java.util.ArrayDeque;

/** Resources the local player dropped off in the last minute, for the "+N/MIN" line on the HUD. */
public final class IncomeTracker {

    private IncomeTracker() { }

    private static final long WINDOW_MS = 60_000;

    private record Deposit(long time, int food, int wood, int ore) { }

    private static final ArrayDeque<Deposit> deposits = new ArrayDeque<>();

    public static void record(int food, int wood, int ore) {
        deposits.addLast(new Deposit(System.currentTimeMillis(), food, wood, ore));
    }

    public static int perMinute(ResourceName resource) {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        while (!deposits.isEmpty() && deposits.peekFirst().time < cutoff)
            deposits.removeFirst();
        int total = 0;
        for (Deposit d : deposits)
            total += switch (resource) {
                case FOOD -> d.food;
                case WOOD -> d.wood;
                case ORE -> d.ore;
                default -> 0;
            };
        return total;
    }

    public static void reset() {
        deposits.clear();
    }
}
