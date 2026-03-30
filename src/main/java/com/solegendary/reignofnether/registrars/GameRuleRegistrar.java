package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.world.level.GameRules;

public class GameRuleRegistrar {

    public static GameRules.Key<GameRules.BooleanValue> LOG_FALLING;
    public static GameRules.Key<GameRules.BooleanValue> NEUTRAL_AGGRO;
    public static GameRules.Key<GameRules.IntegerValue> MAX_POPULATION;
    public static GameRules.Key<GameRules.BooleanValue> DO_UNIT_GRIEFING;
    public static GameRules.Key<GameRules.BooleanValue> DO_PLAYER_GRIEFING;
    public static GameRules.Key<GameRules.BooleanValue> IMPROVED_PATHFINDING;
    public static GameRules.Key<GameRules.IntegerValue> GROUND_Y_LEVEL;
    public static GameRules.Key<GameRules.IntegerValue> FLYING_MAX_Y_LEVEL;
    public static GameRules.Key<GameRules.BooleanValue> ALLOW_BEACONS;
    public static GameRules.Key<GameRules.IntegerValue> BEACON_WIN_MINUTES;
    public static GameRules.Key<GameRules.BooleanValue> PVP_MODES_ONLY;
    public static GameRules.Key<GameRules.BooleanValue> SLANTED_BUILDING;
    public static GameRules.Key<GameRules.IntegerValue> ALLOWED_HEROES;
    public static GameRules.Key<GameRules.BooleanValue> LOCK_ALLIANCES;
    public static GameRules.Key<GameRules.BooleanValue> SCENARIO_MODE;

    public static void init() {
        // do cut trees convert their logs into falling logs?
        LOG_FALLING = GameRules.register("doLogFalling", GameRules.Category.MISC,
                GameRules.BooleanValue.create(true)
        );
        // treat neutral units as enemies? this includes auto attacks, right clicks and attack moving
        NEUTRAL_AGGRO = GameRules.register("neutralAggro", GameRules.Category.MOBS,
                GameRules.BooleanValue.create(true)
        );
        // set hard cap on population (max even with infinite houses)
        MAX_POPULATION = GameRules.register("maxPopulation", GameRules.Category.MISC,
                GameRules.IntegerValue.create(ResourceCosts.DEFAULT_MAX_POPULATION)
        );
        // allow units to damage blocks (separate from doMobGriefing which is only for vanilla mobs)
        DO_UNIT_GRIEFING = GameRules.register("doUnitGriefing", GameRules.Category.MOBS,
                GameRules.BooleanValue.create(false)
        );
        // allow players to break blocks other than buildings and resource blocks
        DO_PLAYER_GRIEFING = GameRules.register("doPlayerGriefing", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(true)
        );
        // increase pathfinding accuracy in exchange for increased CPU usage
        IMPROVED_PATHFINDING = GameRules.register("improvedPathfinding", GameRules.Category.MOBS,
                GameRules.BooleanValue.create(true)
        );
        // sets the minimum Y level for the camera so it doesn't fall into the void
        GROUND_Y_LEVEL = GameRules.register("groundYLevel", GameRules.Category.PLAYER,
                GameRules.IntegerValue.create(-320)
        );
        // locks the camera to a specific Y level instead of it being calculated dynamically
        FLYING_MAX_Y_LEVEL = GameRules.register("flyingMaxYLevel", GameRules.Category.MOBS,
                GameRules.IntegerValue.create(320)
        );
        // allow beacons to be built by workers as a win condition
        ALLOW_BEACONS = GameRules.register("allowBeacons", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(true)
        );
        // allow only classic/king of the beacon gamemodes
        PVP_MODES_ONLY = GameRules.register("pvpModesOnly", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false)
        );
        // ticks to win with a beacon
        BEACON_WIN_MINUTES = GameRules.register("beaconWinMinutes", GameRules.Category.PLAYER,
                GameRules.IntegerValue.create(10)
        );
        // buildings ignore ground flatness
        SLANTED_BUILDING = GameRules.register("slantedBuilding", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false)
        );
        // enable heroes in all gamemodes
        ALLOWED_HEROES = GameRules.register("allowedHeroes", GameRules.Category.PLAYER,
                GameRules.IntegerValue.create(2)
        );
        // only allow alliances to be made/broken with non-RTS players (ie. before a game starts)
        LOCK_ALLIANCES = GameRules.register("lockAlliances", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false)
        );
        // map is set to be played as a scenario by the player that opens it
        SCENARIO_MODE = GameRules.register("scenarioMode", GameRules.Category.MISC,
                GameRules.BooleanValue.create(false)
        );
    }
}
