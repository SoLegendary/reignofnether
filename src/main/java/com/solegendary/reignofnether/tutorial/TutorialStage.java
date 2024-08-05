package com.solegendary.reignofnether.tutorial;

public enum TutorialStage {

    INTRO { // just intro msgs, progresses on a delay
        @Override
        public TutorialStage prev() {
            return this;
        };
    },
    PAN_CAMERA, // make player pan in all 4 directions before continuing
    PAN_CAMERA_TIPS, // just a delayed msg to explain you can use arrow keys
    ZOOM_CAMERA,
    ROTATE_CAMERA,
    MINIMAP_MAXIMISE, // before this step, hide the map and expand button
    MINIMAP_CLICK,
    MINIMAP_SHIFT_CLICK,
    MINIMAP_MINIMISE,
    MINIMAP_TIPS, // explaining units and buildings appear on it
    OBSERVER_TIPS, // explaining you start in observer mode
    PLACE_VILLAGERS, // don't highlight button while cursor has the action to place
    SELECT_UNIT,
    BOX_SELECT_UNITS,
    MOVE_UNITS,
    MOVING_TIPS, // double click to select multiple, control groups, following etc.
    BUILD_TOWN_CENTRE, // don't highlight button while cursor has the action to place
    BUILDING_TIPS, // explain you can order multiple workers to help build together
    TRAIN_WORKER, // while building, explain you can right click to set rally point; progress once worker is built
    GATHER_RESOURCES, // explain resources, highlight some nearby trees and ores; progress once a resource is dropped
    HUNT_ANIMALS, // Spawn in some pigs nearby, explain to take a villager and right click it
    RETURN_FOOD,
    BUILD_BASE, // give free resources, open options to build houses, farms and barracks with a msg for each when built
    BUILD_ARMY, // progress once 3 units are built
    DEFEND_BASE, // set time to night, send in two zombies to fight - if the player loses somehow set time to day early
    DEFEND_BASE_AGAIN, // send in more zombies targeting town centre, but set time to day to kill them with explanation
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
