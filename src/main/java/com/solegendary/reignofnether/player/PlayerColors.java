package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.config.ReignOfNetherClientConfigs;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.unit.Relationship;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.List;

import static com.solegendary.reignofnether.unit.UnitClientEvents.getPlayerToPlayerRelationship;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class PlayerColors {
    /**
     * Indicates which color mode is used. This affects unit/buildings outlines, as well as some UI elements.
     * @return true if using player colors, false if using relation colors
     */
    public static boolean isUsingPlayerColors() {
        if (!PlayerClientEvents.isRTSPlayer())
            return true;
        return ReignOfNetherClientConfigs.USE_PLAYER_COLORS.get();
    }

    /**
     * Switch color mode between Player Colors and Relation Colors.
     */
    public static void toggleColorMode() {
        ReignOfNetherClientConfigs.USE_PLAYER_COLORS.set(!ReignOfNetherClientConfigs.USE_PLAYER_COLORS.get());
    }

    private static final HashMap<Integer, PlayerColor> mappedColors = new HashMap<Integer, PlayerColor>();

    public static class PlayerColor {
        private static int colourCount = 0;
        public final int mapColorId;
        public final int id;
        public final String name;
        public final int hexCode;
        public final ResourceLocation blockTexture;
        public final ResourceLocation bedIcon;

        public PlayerColor(int mapColorId, int hexCode, String name, ResourceLocation blockTexture, ResourceLocation bedIcon) {
            this.mapColorId = mapColorId;
            this.id = colourCount++;
            this.name = name;
            this.hexCode = hexCode;
            this.blockTexture = blockTexture;
            this.bedIcon = bedIcon;
            System.out.println("Created color '" + this.name + "' with index " + this.id);
            if (this.mapColorId != -1) {
                mappedColors.put(mapColorId, this);
            }
        }

        public static PlayerColor fromName(int mapColorId, int hexCode, String name) {
            return new PlayerColor(
                    mapColorId,
                    hexCode,
                    name,
                    ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/block/rts_start_block_" + name + ".png"),
                    ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/beds/" + name + ".png"));
        }
    }

    // colors available for players to choose - they all have an associated startBlock
    public static final PlayerColor COLOR_RED = PlayerColor.fromName(MapColor.COLOR_RED.id, 0xA12722, "red");
    public static final PlayerColor COLOR_BLUE = PlayerColor.fromName(MapColor.COLOR_BLUE.id, 0x35399D, "blue");
    public static final PlayerColor COLOR_CYAN = PlayerColor.fromName(MapColor.COLOR_CYAN.id, 0x158991, "cyan");
    public static final PlayerColor COLOR_PURPLE = PlayerColor.fromName(MapColor.COLOR_PURPLE.id, 0x792AAC, "purple");
    public static final PlayerColor COLOR_YELLOW = PlayerColor.fromName(MapColor.COLOR_YELLOW.id, 0xF8C627, "yellow");
    public static final PlayerColor COLOR_ORANGE = PlayerColor.fromName(MapColor.COLOR_ORANGE.id, 0xF07613, "orange");
    public static final PlayerColor COLOR_LIME = PlayerColor.fromName(MapColor.COLOR_LIGHT_GREEN.id, 0x70B919, "lime");
    public static final PlayerColor COLOR_PINK = PlayerColor.fromName(MapColor.COLOR_PINK.id, 0xED8DAC, "pink");
    public static final PlayerColor COLOR_MAGENTA = PlayerColor.fromName(MapColor.COLOR_MAGENTA.id, 0xBD44B3, "magenta");
    public static final PlayerColor COLOR_LIGHT_BLUE = PlayerColor.fromName(MapColor.COLOR_LIGHT_BLUE.id, 0x3AAFD9, "light_blue");
    public static final PlayerColor COLOR_GREEN = PlayerColor.fromName(MapColor.COLOR_GREEN.id, 0x546D1B, "green");
    public static final PlayerColor COLOR_BROWN = PlayerColor.fromName(MapColor.COLOR_BROWN.id, 0x724728, "brown");
    public static final int PLAYER_COLOR_COUNT = PlayerColor.colourCount;

    public static final PlayerColor COLOR_LIGHT_GREY = PlayerColor.fromName(MapColor.COLOR_LIGHT_GRAY.id, 0x8E8E86, "light_gray");
    public static final PlayerColor COLOR_GRAY = PlayerColor.fromName(MapColor.COLOR_GRAY.id, 0x3E4447, "gray");
    public static final PlayerColor COLOR_BLACK = PlayerColor.fromName(MapColor.COLOR_BLACK.id, 0x141519, "black");
    public static final PlayerColor COLOR_WHITE = PlayerColor.fromName(MapColor.SNOW.id, 0xE9ECEC, "white");

    public static final PlayerColor COLOR_OWNED = new PlayerColor(-1, 0x33FF33, "owned", ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/lime_wool.png"), COLOR_LIME.bedIcon);
    public static final PlayerColor COLOR_FRIENDLY = new PlayerColor(-1, 0x3333FF, "friendly", ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/blue_wool.png"), COLOR_BLUE.bedIcon);
    public static final PlayerColor COLOR_NEUTRAL = new PlayerColor(-1, 0xFFFF19, "neutral", ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/yellow_wool.png"), COLOR_YELLOW.bedIcon);
    public static final PlayerColor COLOR_HOSTILE = new PlayerColor(-1, 0xFF3333, "hostile", ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/red_wool.png"), COLOR_RED.bedIcon);
    public static final int PLAYER_COLOR_TOTAL_COUNT = PlayerColor.colourCount;

    public static final PlayerColor[] colors = new PlayerColor[]{
            COLOR_RED,
            COLOR_BLUE,
            COLOR_CYAN,
            COLOR_PURPLE,
            COLOR_YELLOW,
            COLOR_ORANGE,
            COLOR_LIME,
            COLOR_PINK,
            COLOR_MAGENTA,
            COLOR_LIGHT_BLUE,
            COLOR_GREEN,
            COLOR_BROWN,

            COLOR_LIGHT_GREY,
            COLOR_GRAY,
            COLOR_BLACK,
            COLOR_WHITE,

            COLOR_OWNED,
            COLOR_FRIENDLY,
            COLOR_NEUTRAL,
            COLOR_HOSTILE,
    };

    private static HashMap<String, Integer> playerColorId = new HashMap<>();
    public static int getPlayerColorId(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            // fallback color - white
            return COLOR_WHITE.id;
        }

        // check for a cached color id
        var cachedColorId = playerColorId.getOrDefault(playerName, -1);
        if (cachedColorId != -1) {
            return cachedColorId;
        }

        // find the colorId for the player and cache it
        var mapColorId = PlayerClientEvents.getPlayerMapColorId(playerName);
        if (mapColorId > 0 && mappedColors.containsKey(mapColorId)) {
            var color = mappedColors.get(mapColorId);
            playerColorId.put(playerName, color.id); // only cache a real id
            return color.id;
        }

        var playerId = PlayerClientEvents.getPlayerIndex(playerName);
        if (playerId != null) {
            playerId = playerId % PLAYER_COLOR_COUNT;
            playerColorId.put(playerName, playerId); // only cache a real id
            return playerId;
        }

        // fallback color - white
        return COLOR_WHITE.id;
    }

    public static void reset() {
        playerColorId.clear();
    }

    public static ResourceLocation getColorIcon(int colorIndex) {
        return colors[colorIndex % PLAYER_COLOR_TOTAL_COUNT].blockTexture;
    }

    public static int getColorHex(int colorIndex) {
        return colors[colorIndex % PLAYER_COLOR_TOTAL_COUNT].hexCode;
    }

    public static int getPlayerColorHex(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return COLOR_WHITE.hexCode;
        }

        int colorId = getPlayerColorId(playerName);
        return colors[colorId].hexCode;
    }

    public static ResourceLocation getPlayerColorIcon(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return COLOR_WHITE.blockTexture;
        }

        int colorId = getPlayerColorId(playerName);
        return colors[colorId].blockTexture;
    }

    public static ResourceLocation getPlayerColorBedIcon(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return COLOR_WHITE.bedIcon;
        }

        int colorId = getPlayerColorId(playerName);
        return colors[colorId].bedIcon;
    }

    public static int getPlayerDisplayColorHex(String playerName) {
        if (PlayerColors.isUsingPlayerColors()) {
            return PlayerColors.getPlayerColorHex(playerName);
        }

        // fall back on alliance color
        return getPlayerAllianceColorHex(playerName);
    }

    public static int getPlayerAllianceColorHex(String playerName) {
        Relationship unitRs = getPlayerToPlayerRelationship(playerName);
        return switch (unitRs) {
            case OWNED -> colors[ReignOfNetherClientConfigs.PLAYER_COLOR_SELF.get() % PLAYER_COLOR_TOTAL_COUNT].hexCode;
            case FRIENDLY ->
                    colors[ReignOfNetherClientConfigs.PLAYER_COLOR_ALLY.get() % PLAYER_COLOR_TOTAL_COUNT].hexCode;
            case NEUTRAL ->
                    colors[ReignOfNetherClientConfigs.PLAYER_COLOR_NEUTRAL.get() % PLAYER_COLOR_TOTAL_COUNT].hexCode;
            case HOSTILE ->
                    colors[ReignOfNetherClientConfigs.PLAYER_COLOR_ENEMY.get() % PLAYER_COLOR_TOTAL_COUNT].hexCode;
        };
    }

    public static int getPlayerPortraitDisplayColorHex(String playerName) {
        if (PlayerColors.isUsingPlayerColors()) {
            return 0x90000000 | PlayerColors.getPlayerColorHex(playerName);
        }

        // fall back on alliance color
        Relationship unitRs = getPlayerToPlayerRelationship(playerName);
        return switch (unitRs) {
            case OWNED -> 0x90000000;
            case FRIENDLY ->
                    0x90000000 | colors[ReignOfNetherClientConfigs.PLAYER_COLOR_ALLY.get() % PLAYER_COLOR_TOTAL_COUNT].hexCode;
            case NEUTRAL ->
                    0x90000000 | colors[ReignOfNetherClientConfigs.PLAYER_COLOR_NEUTRAL.get() % PLAYER_COLOR_TOTAL_COUNT].hexCode;
            case HOSTILE ->
                    0x90000000 | colors[ReignOfNetherClientConfigs.PLAYER_COLOR_ENEMY.get() % PLAYER_COLOR_TOTAL_COUNT].hexCode;
        };
    }

    public static Button getToggleTeamColorsButton() {
        return new Button(
                "Toggle alliance colours",
                14,
                isUsingPlayerColors()
                        ? ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/toggle_color_mode_players.png")
                        : ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/toggle_color_mode_relations.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                null,
                () -> false,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK) || !MinimapClientEvents.isLargeMap(),
                PlayerClientEvents::isRTSPlayer,
                PlayerColors::toggleColorMode,
                null,
                List.of(
                        fcs(I18n.get("hud.orthoview.reignofnether.using_player_team_color"), isUsingPlayerColors()),
                        fcs(I18n.get("hud.orthoview.reignofnether.using_relation_color"), !isUsingPlayerColors())
                )
        );
    }
}
