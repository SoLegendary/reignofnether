package com.solegendary.reignofnether.building;

// actions that can be done to Building objects themselves
public enum BuildingAction {
    PLACE, // provide serverside building data to client (on new placement and sync existing buildings for new logins)
    DESTROY, // WIP building cancelled by owner or destroyed by someone
    SYNC_BLOCKS, // sync client building health with server (client doesn't always have the blocks loaded to check natively)
    SET_RALLY_POINT,
    START_PRODUCTION, // start ProductionItem
    CANCEL_PRODUCTION, // remove ProductionItem from front of queue
    CANCEL_BACK_PRODUCTION, // remove ProductionItem from back of queue
    CHECK_STOCKPILE_CHEST // check stockpile chests for resources to consume
}
