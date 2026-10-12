package com.solegendary.reignofnether.config;

import com.solegendary.reignofnether.player.PlayerColors;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ReignOfNetherClientConfigs {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Integer> PLAYER_COLOR_SELF;
    public static final ModConfigSpec.ConfigValue<Integer> PLAYER_COLOR_ALLY;
    public static final ModConfigSpec.ConfigValue<Integer> PLAYER_COLOR_NEUTRAL;
    public static final ModConfigSpec.ConfigValue<Integer> PLAYER_COLOR_ENEMY;

    public static final ModConfigSpec.ConfigValue<Boolean> USE_PLAYER_COLORS;
    public static final ModConfigSpec.ConfigValue<Integer> CAMERA_SENSITIVITY;
    public static final ModConfigSpec.ConfigValue<Boolean> SQUARE_MINIMAP;

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
        SQUARE_MINIMAP = BUILDER.define("square_minimap", false);
        SPEC = BUILDER.build();
    }

    // NeoForge 21.1 replaced ConfigScreenHandler with the IConfigScreenFactory extension point.
    // Its callback takes the container and the screen the user came from, instead of a factory
    // object wrapping a one-argument builder.
    public static IConfigScreenFactory createConfigScreen() {
        // passed as an instance rather than a method reference: ModContainer#registerExtensionPoint
        // is overloaded on (Class, T) and (Class, Supplier<T>), which makes a bare method
        // reference ambiguous between the two.
        return ReignOfNetherClientConfigs::buildConfigScreen;
    }

    private static Screen buildConfigScreen(ModContainer container, Screen screen) {
        return new ReignOfNetherConfigScreen(screen);
    }
}
