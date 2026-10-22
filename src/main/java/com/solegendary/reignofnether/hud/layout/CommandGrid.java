package com.solegendary.reignofnether.hud.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Places commands and abilities in the fixed 5x3 command card. The slot decides the hotkey:
 * <pre>
 *   Q W E R T    0  1  2  3  4   abilities, production, buildings
 *   A S D F G    5  6  7  8  9   commands (attack, stop, hold, garrison, gather...)
 *   Z X C V B   10 11 12 13 14   more abilities
 * </pre>
 * Commands go to their preferred slot when it is free, otherwise to the next free command slot.
 * Abilities fill the top row, then the bottom row, then any command slot left free. When there are
 * more abilities than free slots, B (or the last free slot) becomes a page button.
 */
public final class CommandGrid {

    public static final int COLS = 5;
    public static final int ROWS = 3;
    public static final int SLOTS = COLS * ROWS;

    public static final int SLOT_Q = 0, SLOT_A = 5, SLOT_S = 6, SLOT_D = 7, SLOT_F = 8, SLOT_G = 9, SLOT_B = 14;
    public static final int NO_PREFERENCE = -1;

    private static final int[] ABILITY_ORDER = { 0, 1, 2, 3, 4, 10, 11, 12, 13, 14, 5, 6, 7, 8, 9 };

    public record Command<T>(T item, int preferredSlot) { }

    /**
     * @param slots     what sits in each of the 15 slots (null = empty)
     * @param pageSlot  slot of the page button, or -1 when everything fits
     * @param pageCount number of pages (1 when everything fits)
     */
    public record Layout<T>(List<T> slots, int pageSlot, int pageCount) { }

    public static <T> Layout<T> assign(List<Command<T>> commands, List<T> abilities, int page) {
        Object[] grid = new Object[SLOTS];
        List<T> spill = new ArrayList<>();

        List<Command<T>> later = new ArrayList<>();
        for (Command<T> c : commands) {
            int pref = c.preferredSlot();
            if (pref >= 0 && pref < SLOTS && grid[pref] == null)
                grid[pref] = c.item();
            else
                later.add(c);
        }
        for (Command<T> c : later) {
            int free = firstFree(grid, SLOT_A, SLOT_G);
            if (free >= 0)
                grid[free] = c.item();
            else
                spill.add(c.item());
        }

        List<T> all = new ArrayList<>(spill);
        all.addAll(abilities);

        List<Integer> free = new ArrayList<>();
        for (int slot : ABILITY_ORDER)
            if (grid[slot] == null)
                free.add(slot);

        int pageSlot = -1;
        int pageCount = 1;
        int perPage = free.size();
        if (all.size() > free.size() && !free.isEmpty()) {
            // the page button sits on B when it can, so it is always in the same place
            pageSlot = free.contains(SLOT_B) ? SLOT_B : free.get(free.size() - 1);
            free.remove(Integer.valueOf(pageSlot));
            perPage = free.size();
            pageCount = perPage == 0 ? 1 : (all.size() + perPage - 1) / perPage;
        }
        int clampedPage = Math.floorMod(page, pageCount);
        int start = clampedPage * perPage;
        for (int i = 0; i < perPage && start + i < all.size(); i++)
            grid[free.get(i)] = all.get(start + i);

        @SuppressWarnings("unchecked")
        List<T> slots = (List<T>) Arrays.asList(grid);
        return new Layout<>(slots, pageSlot, pageCount);
    }

    private static int firstFree(Object[] grid, int from, int to) {
        for (int i = from; i <= to; i++)
            if (grid[i] == null)
                return i;
        return -1;
    }

    public static int row(int slot) { return slot / COLS; }

    public static int col(int slot) { return slot % COLS; }
}
