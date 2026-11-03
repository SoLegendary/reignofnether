package com.solegendary.reignofnether.startpos;

public enum StartPosAction {
    ADD,
    ADD_DISABLED,
    REMOVE,
    RESERVE,
    UNRESERVE,
    RESET,
    SET_GAME_STARTING,
    UNSET_GAME_STARTING,
    PLAYER_READY,
    PLAYER_UNREADY,
    ENABLE,
    DISABLE,
    CLEAR,        // drop the seat list before a per-player resync (StartPosServerEvents.syncTo)
    HIDDEN_PLAYER // a player on another team whose seat is hidden by hideEnemyPicks: name and ready only
}
