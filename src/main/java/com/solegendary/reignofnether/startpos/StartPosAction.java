package com.solegendary.reignofnether.startpos;

public enum StartPosAction {
    ADD,
    ADD_DISABLED,
    REMOVE,
    RESERVE,
    RESERVE_BOT,  // host: put an AI bot on a seat, or change its faction or difficulty
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
    BOT_FACTION,   // a faction the AI bots can play right now (BotPlans); the list is resent after every CLEAR
    HIDDEN_PLAYER, // a player on another team whose seat is hidden by hideEnemyPicks: name and ready only
    JOIN_SOLDIER,        // a soldier on the team of the seat at blockPos
    JOIN_CO_COMMANDER,   // a co-commander sharing the seat at blockPos
    LEAVE_JOIN,          // drop the player's soldier/co-commander join
    ACCEPT_JOIN,         // the seat holder lets a pending soldier/co-commander in
    REFUSE_JOIN          // the seat holder sends a soldier/co-commander away
}
