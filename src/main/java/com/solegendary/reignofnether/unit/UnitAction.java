package com.solegendary.reignofnether.unit;

public enum UnitAction {
    // if a non-attack unit is asked to attack it will move instead
    // same for if a non-builder unit is asked to build or repair

    NONE, // basically null, but sending null to server crashes packetHandler
    ATTACK,
    STOP,
    HOLD,
    MOVE,
    ATTACK_MOVE, // enacted by attack button + left click on ground
    FOLLOW, // enacted by move button + left click on another entity
    BUILD_REPAIR, // build or repair the building at the targeted blockPos
    TOGGLE_GATHER_TARGET, // cycle between gathering nothing, food, wood or ore

    // special abilities
    EXPLODE,
}
