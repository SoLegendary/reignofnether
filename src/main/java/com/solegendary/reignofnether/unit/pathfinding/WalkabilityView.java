package com.solegendary.reignofnether.unit.pathfinding;

public interface WalkabilityView {
    byte kindAt(int wx, int y, int wz);
    MobilityClass mobility();

    // Per-request fire/magma cost for THIS unit (fire-immunity + DAMAGE_FIRE malus); see RtsPathfinder.fireCostFor.
    float fireCost();

    // The unit's height in whole cells (>=2); step-ups and destination cells need this much vertical clearance,
    // not the 2 the classifier assumes, so tall mobs don't wedge into a ceiling or fail to rise under an overhang.
    int clearanceCells();

    // Tile footprint radius, vanilla style (Mth.floor(bbWidth + 1) - 1): 0 for a <=1-wide unit, 1 for a bear's
    // 2x2 box, scaling up. See GridNeighbors.wideFits.
    int footprintRadius();

    // True if the block in this cell is solid-blocking. Used to require headroom before a unit steps up a ledge.
    boolean solidAt(int wx, int y, int wz);

    // True if this unit may use vertical climb moves (cling to a climbable wall, no floor needed). False for
    // non-climbers, so they path exactly as before. Only spiders with wall-climbing toggled on set this.
    boolean canClimb();
}
