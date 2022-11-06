package com.solegendary.reignofnether.building;

// actions that can be done to Building objects themselves
public enum BuildingAction {
    PLACE,
    CANCEL, // WIP building cancelled by owner
    SET_RALLY_POINT,
    START_PRODUCTION, // start ProductionItem
    CANCEL_PRODUCTION, // remove ProductionItem from front of queue
    CANCEL_BACK_PRODUCTION // remove ProductionItem from back of queue
}
