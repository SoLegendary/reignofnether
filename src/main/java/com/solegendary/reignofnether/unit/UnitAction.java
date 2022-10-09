package com.solegendary.reignofnether.unit;

public enum UnitAction {
    NONE, // basically null, but sending null to server crashes packetHandler
    ATTACK,
    STOP,
    HOLD,
    MOVE,

    // special abilities
    EXPLODE,
}
