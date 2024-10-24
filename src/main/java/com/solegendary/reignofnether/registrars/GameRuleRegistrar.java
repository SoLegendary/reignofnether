package com.solegendary.reignofnether.registrars;

import net.minecraft.world.level.GameRules;

public class GameRuleRegistrar {

    public static GameRules.Key<GameRules.BooleanValue> LOG_FALLING;

    public static void init() {
        // do cut trees convert their logs into falling logs?
        LOG_FALLING = GameRules.register("doLogFalling", GameRules.Category.MISC,
                GameRules.BooleanValue.create(true)
        );
    }
}
