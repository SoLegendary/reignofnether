package com.solegendary.reignofnether.unit.pathfinding;

import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;

// Result of an async path request. `busy` distinguishes a load-drop (queue/pool saturated, the
// request was never computed) from a genuine result (a path, or null = no path exists). A busy
// drop must NOT stop the unit: it should keep its current path and retry shortly, else the
// stop-then-resubmit loop under load makes units shuffle back and forth.
@FunctionalInterface
public interface PathCallback {
    void onPath(@Nullable Path path, boolean busy);

    default void accept(@Nullable Path path) { onPath(path, false); }
}
