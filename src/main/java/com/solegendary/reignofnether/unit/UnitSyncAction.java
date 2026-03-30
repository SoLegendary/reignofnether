package com.solegendary.reignofnether.unit;

public enum UnitSyncAction {
    LEAVE_LEVEL, // unit left server level so remove from client
    SYNC_STATS, // sync general stats like position, health, held item etc.
    SYNC_RESOURCES, // syncs resources held and targeted resource
    SYNC_OWNERNAME,
    SYNC_SCENARIO_ROLE_INDEX,
    MAKE_VILLAGER_VETERAN,
    SYNC_ANCHOR_POS,
    REMOVE_ANCHOR_POS,

    SYNC_ABILITIES,
    REQUEST_SYNC_ABILITIES, // ask the server to sync cooldowns
}