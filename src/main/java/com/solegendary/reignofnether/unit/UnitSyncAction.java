package com.solegendary.reignofnether.unit;

public enum UnitSyncAction {
    LEAVE_LEVEL, // unit left server level so remove from client
    SYNC_STATS, // sync general stats like position, health, held item etc.
    SYNC_RESOURCES, // syncs resources held and targeted resource
    EVOKER_START_CASTING, // syncs an evoker's spell goals so other clients can see the casting animation
    EVOKER_STOP_CASTING
}