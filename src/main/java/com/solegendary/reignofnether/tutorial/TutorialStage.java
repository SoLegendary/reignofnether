package com.solegendary.reignofnether.tutorial;

public enum TutorialStage {

    INTRO { // just intro msgs, progresses on a delay
        @Override
        public TutorialStage prev() {
            return this;
        };
    },

    PAN_CAMERA, // make player pan in all 4 directions before continuing - UNLOCK CAM
    CAMERA_TIPS, // just a delayed msg to explain you can use arrow keys
    MINIMAP_CLICK,
    MINIMAP_TIPS, // mention minimise/maximise button, explain units and buildings appear on it

    PLACE_WORKERS_A, // don't highlight button while cursor has the action to place - LOCK CAM
    PLACE_WORKERS_B,
    SELECT_UNIT,
    MOVE_UNIT,
    BOX_SELECT_UNITS,
    MOVE_UNITS,
    UNIT_TIPS, // double click to select multiple, control groups, F1 to deselect, following etc.

    BUILD_INTRO,
    BUILD_TOWN_CENTRE, // forcePanCam back to spawn, ask player to select workers, don't highlight button while cursor has the action to place
    BUILDING_TIPS, // explain you can order multiple workers to help build together
    TRAIN_WORKER, // progress once worker is built, while building explain rally points and cancelling

    GATHER_RESOURCES, // explain resources, highlight some nearby trees and ores; progress once a resource is dropped
    HUNT_ANIMALS, // Spawn in some pigs nearby, explain to take a villager and right click it
    RETURN_FOOD,
    BUILD_BASE, // give free resources, open options to build houses, farms and barracks with a msg for each when built
    BUILD_ARMY, // progress once 3 units are built
    DEFEND_BASE, // set time to night, send in two zombies to fight - if the player loses somehow set time to day early
    DEFEND_BASE_AGAIN, // send in more zombies targeting town centre, but set time to day to kill them with explanation
    REPAIR_BUILDING,
    BUILD_BRIDGE, // once completed, spawn in free army including iron golems
    ATTACK_ENEMY_BASE, // completed once enemy capitol is destroyed
    OUTRO;

    public TutorialStage prev() {
        return values()[ordinal() - 1];
    }
    public TutorialStage next() {
        return values()[ordinal() + 1];
    }
}
