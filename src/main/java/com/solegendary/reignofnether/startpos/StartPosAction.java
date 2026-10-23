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
    CLOSE_EMPTY,  // host: disable every enabled seat nobody is sitting in
    CLEAR,        // drop the seat list before a per-player resync (StartPosServerEvents.syncTo)
    HIDDEN_PLAYER, // a player on another team whose seat is hidden by hideEnemyPicks: name and ready only
    JOIN_SOLDIER,        // a soldier on the team of the seat at blockPos
    JOIN_CO_COMMANDER,   // a co-commander sharing the seat at blockPos
    LEAVE_JOIN           // drop the player's soldier/co-commander join
}
