package com.solegendary.reignofnether.unit.pathfinding;

public interface WalkabilityView {
    byte kindAt(int wx, int y, int wz);
    MobilityClass mobility();

    // per-request fire/magma cost for this unit (fire-immunity + DAMAGE_FIRE malus); see RtsPathfinder.fireCostFor.
    float fireCost();

    // the unit's height in whole cells (>=2). step-ups and destination cells need this much clearance, not the 2
    // the classifier assumes, so tall mobs don't wedge into a ceiling or fail to rise under an overhang.
    int clearanceCells();

    // tile footprint radius, vanilla style (Mth.floor(bbWidth + 1) - 1): 0 for a <=1-wide unit, scaling up for
    // wider mobs. see GridNeighbors.wideFits.
    int footprintRadius();

    // true if the block in this cell is solid-blocking. used to require headroom before a unit steps up a ledge.
    boolean solidAt(int wx, int y, int wz);

    // precomputed crowding malus for this cell (steering cost that keeps units off walls/edges/corners), baked
    // into the chunk at build time instead of rescanned per A* node. 0 for open cells / outside the view.
    float crowdAt(int wx, int y, int wz);

    // true if this unit may use vertical climb moves (cling to a climbable wall, no floor needed). false for
    // non-climbers, so they path as before. only spiders with wall-climbing toggled on set this.
    boolean canClimb();
}
