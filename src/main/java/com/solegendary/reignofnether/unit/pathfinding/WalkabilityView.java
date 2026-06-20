package com.solegendary.reignofnether.unit.pathfinding;

public interface WalkabilityView {
    byte kindAt(int wx, int y, int wz);
    MobilityClass mobility();
}
