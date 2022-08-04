package com.solegendary.reignofnether.building;

// actions that can be done to Building objects themselves
public enum BuildingAction {
    PLACE,
    DESTROY, // WIP building cancelled by owner
    REPAIR, // damage is done by directly removing blocks
    PLACE_BLOCK,
    DESTROY_BLOCK
}
