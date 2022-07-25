package com.solegendary.reignofnether.building;

// actions that can be done to Building objects themselves
public enum BuildingAction {
    PLACE,
    CANCEL, // WIP building cancelled by owner
    DESTROY, // Building explodes and is deleted from serverside memory
    REPAIR // damage is done by directly removing blocks
}
