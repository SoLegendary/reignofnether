package com.solegendary.reignofnether.hud;

public enum ActionName {
    NONE, // basically null, but sending null to server crashes packetHandler
    ATTACK,
    STOP,
    HOLD,
    MOVE,

    // special abilities
    EXPLODE,
}
