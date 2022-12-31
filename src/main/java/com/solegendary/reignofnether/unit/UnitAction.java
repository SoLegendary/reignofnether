package com.solegendary.reignofnether.unit;

public enum UnitAction {
    // if a non-attack unit is asked to attack it will move instead
    // same for if a non-builder unit is asked to build or repair

    NONE, // basically null, but sending null to server crashes packetHandler
    ATTACK,
    ATTACK_BUILDING,
    STOP,
    HOLD,
    MOVE,
    ATTACK_MOVE, // enacted by attack button + left click on ground
    FOLLOW, // enacted by move button + left click on another entity
    BUILD_REPAIR, // build or repair the building at the targeted blockPos
    FARM, // sets the villager's target gather resource
    TOGGLE_GATHER_TARGET, // cycle between gathering nothing, food, wood or ore
    RETURN_RESOURCES, // drops off resources to the building
    RETURN_RESOURCES_TO_CLOSEST, // drops off resources to the nearest building that accepts resources
    DELETE, // instantly kills this unit

    // special abilities - these can also be assigned to cursor actions
    EXPLODE,
    CALL_LIGHTNING, // actually not from a unit, but we'll make an exception

}
