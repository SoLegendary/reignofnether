package com.solegendary.reignofnether.unit;

public enum UnitSyncAction {
    LEAVE_LEVEL, // unit left server level so remove from client
    SYNC_STATS, // sync general stats like position, health, held item etc.
    SYNC_RESOURCES, // syncs resources held and targeted resource
}