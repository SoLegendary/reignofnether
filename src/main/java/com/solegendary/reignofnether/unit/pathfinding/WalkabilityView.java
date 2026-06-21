package com.solegendary.reignofnether.unit.pathfinding;

public interface WalkabilityView {
    byte kindAt(int wx, int y, int wz);
    MobilityClass mobility();

    // True if the block in this cell is solid-blocking. Used to require headroom above a unit before it
    // steps up onto a 1-block-higher ledge (a low ceiling would otherwise trap a 2-tall unit at the wall).
    boolean solidAt(int wx, int y, int wz);
}
