package com.solegendary.reignofnether.building;

// actions that can be done to Building objects themselves
public enum BuildingAction {
    PLACE,
    CANCEL, // WIP building cancelled by owner
    REPAIR // damage is done by directly removing blocks
}
