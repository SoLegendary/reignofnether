package com.solegendary.reignofnether.config;

import com.solegendary.reignofnether.blocks.NightCircleMode;
import com.solegendary.reignofnether.hud.panels.SettingsMenu;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
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
    public static final ForgeConfigSpec.ConfigValue<Boolean> SEEN_NETWORK_NOTICE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SQUARE_MINIMAP;
    public static final ForgeConfigSpec.ConfigValue<Boolean> LARGER_SMALL_TEXT;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SOLID_PANELS;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOW_MISSING_COST;
    public static final ForgeConfigSpec.ConfigValue<Boolean> QUEUE_OWN_ROW;
    public static final ForgeConfigSpec.ConfigValue<Boolean> HIGHLIGHT_ANIMALS;
    public static final ForgeConfigSpec.ConfigValue<Boolean> UNDERLINES;
    public static final ForgeConfigSpec.EnumValue<OrthoviewClientEvents.LeafHideMethod> HIDE_LEAVES;
    public static final ForgeConfigSpec.EnumValue<NightCircleMode> NIGHT_CIRCLES;

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
        SEEN_NETWORK_NOTICE = BUILDER.define("seen_network_notice", false);
        SQUARE_MINIMAP = BUILDER.define("square_minimap", false);
        LARGER_SMALL_TEXT = BUILDER.define("larger_small_text", false);
        SOLID_PANELS = BUILDER.define("solid_panels", false);
        SHOW_MISSING_COST = BUILDER.define("show_missing_cost", true);
        QUEUE_OWN_ROW = BUILDER.define("queue_own_row", false);
        HIGHLIGHT_ANIMALS = BUILDER.define("highlight_animals", false);
        UNDERLINES = BUILDER.define("underlines", true);
        HIDE_LEAVES = BUILDER.defineEnum("hide_leaves", OrthoviewClientEvents.LeafHideMethod.NONE);
        NIGHT_CIRCLES = BUILDER.defineEnum("night_circles", NightCircleMode.NO_OVERLAPS);
        SPEC = BUILDER.build();
    }

    public static ConfigScreenHandler.ConfigScreenFactory createConfigScreen() {
        return new ConfigScreenHandler.ConfigScreenFactory(ReignOfNetherClientConfigs::buildConfigScreen);
    }

    // Forge's mod list "Config" button opens the same settings screen as the pause menu
    private static Screen buildConfigScreen(Screen screen) {
        return SettingsMenu.settingsScreen(screen);
    }
}
