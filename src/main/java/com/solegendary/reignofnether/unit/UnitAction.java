package com.solegendary.reignofnether.unit;

public enum UnitAction {
    NONE, // basically null, but sending null to server crashes packetHandler
    ATTACK,
    STOP,
    HOLD,
    MOVE,
    ATTACK_MOVE, // enacted by attack button + left click on ground
    FOLLOW, // enacted by move button + left click on another entity

    // special abilities
    EXPLODE,
}
