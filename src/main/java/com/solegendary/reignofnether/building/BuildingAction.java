package com.solegendary.reignofnether.building;

// actions that can be done to Building objects themselves
public enum BuildingAction {
    PLACE,
    CANCEL, // WIP building cancelled by owner
    REPAIR, // or continue build a WIP building
    START_PRODUCTION, // start producing something
    CANCEL_PRODUCTION, // stop producing from front of queue
    CANCEL_BACK_PRODUCTION // stop producing from back of queue
}
