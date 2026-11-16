package com.solegendary.reignofnether.loading;

import com.solegendary.reignofnether.minimap.MapGrid;
import com.solegendary.reignofnether.util.WorldState;

/**
 * What the server is doing while a world loads, after vanilla's spawn chunks: drawing the map, building the navmesh
 * and taking the fog snapshot. Written by the server thread; in singleplayer and for a P2P host the loading screen
 * reads it from the same game, which is the only time anyone can see it (players joining see the connection instead).
 */
public final class LoadProgress {

    private LoadProgress() { }

    public enum Step { MAP, NAVMESH, FOG }

    public static volatile Step step = null; // null: not started, or vanilla still preparing the spawn
    public static volatile int done = 0;
    public static volatile int total = 0;
    public static volatile boolean finished = false;
    // the map as it is drawn, so the loading screen paints it in as it is built
    public static volatile MapGrid map = null;
    static {
        WorldState.onServerReset(LoadProgress::reset);
    }

    public static void begin(Step s, int of) {
        done = 0;
        total = Math.max(1, of);
        step = s;
    }

    public static void reset() {
        step = null;
        done = 0;
        total = 0;
        finished = false;
        map = null;
    }
}
