package com.solegendary.reignofnether.config;

import com.solegendary.reignofnether.player.PlayerColors;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.ForgeConfigSpec;

public class ReignOfNetherClientConfigs {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Integer> PLAYER_COLOR_SELF;
    public static final ForgeConfigSpec.ConfigValue<Integer> PLAYER_COLOR_ALLY;
    public static final ForgeConfigSpec.ConfigValue<Integer> PLAYER_COLOR_NEUTRAL;
    public static final ForgeConfigSpec.ConfigValue<Integer> PLAYER_COLOR_ENEMY;

    public static final ForgeConfigSpec.ConfigValue<Boolean> USE_PLAYER_COLORS;
    public static final ForgeConfigSpec.ConfigValue<Integer> CAMERA_SENSITIVITY;

    static {
        BUILDER.push("Configuration File");
        BUILDER.pop();
        BUILDER.comment("Player colors");
        for (PlayerColors.PlayerColor color : PlayerColors.colors) {
            BUILDER.comment("- " + color.name + ": " + color.id);
        }
        PLAYER_COLOR_SELF = BUILDER.define("player_color_self", PlayerColors.COLOR_OWNED.id);
        PLAYER_COLOR_ALLY = BUILDER.define("player_color_ally", PlayerColors.COLOR_FRIENDLY.id);
        PLAYER_COLOR_NEUTRAL = BUILDER.define("player_color_neutral", PlayerColors.COLOR_NEUTRAL.id);
        PLAYER_COLOR_ENEMY = BUILDER.define("player_color_enemy", PlayerColors.COLOR_HOSTILE.id);
        USE_PLAYER_COLORS = BUILDER.define("use_player_colors", false);
        CAMERA_SENSITIVITY = BUILDER.define("camera_sensitivity", 10);
        SPEC = BUILDER.build();
    }

    public static ConfigScreenHandler.ConfigScreenFactory createConfigScreen() {
        return new ConfigScreenHandler.ConfigScreenFactory(ReignOfNetherClientConfigs::buildConfigScreen);
    }

    private static Screen buildConfigScreen(Screen screen) {
        return new ReignOfNetherConfigScreen(screen);
    }
}
