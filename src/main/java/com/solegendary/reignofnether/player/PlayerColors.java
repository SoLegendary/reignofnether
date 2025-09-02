package com.solegendary.reignofnether.player;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.unit.Relationship;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

import static com.solegendary.reignofnether.unit.UnitClientEvents.getPlayerToPlayerRelationship;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class PlayerColors {

    private static final Minecraft MC = Minecraft.getInstance();

    public static boolean usePlayerTeamColor = true;

    private static class PlayerColor {
        private final int hexCode;
        private final ResourceLocation blockTexture;
        private final ResourceLocation bedIcon;

        public PlayerColor(int hexCode, String name) {
            this.hexCode = hexCode;
            this.blockTexture = new ResourceLocation(ReignOfNether.MOD_ID, "textures/block/rts_start_block_" + name + ".png");
            this.bedIcon = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/beds/" + name + ".png");
        }
    }

    public static final PlayerColor[] colors = new PlayerColor[]{
            new PlayerColor(0xA12722, "red"),
            new PlayerColor(0x35399D, "blue"),
            new PlayerColor(0x158991, "cyan"),
            new PlayerColor(0x792AAC, "purple"),
            new PlayerColor(0xF8C627, "yellow"),
            new PlayerColor(0xF07613, "orange"),
            new PlayerColor(0x70B919, "lime"),
            new PlayerColor(0xED8DAC, "pink"),
            new PlayerColor(0x8E8E86, "light_gray"),
            new PlayerColor(0x3AAFD9, "light_blue"),
            new PlayerColor(0x546D1B, "green"),
            new PlayerColor(0x724728, "brown"),
            new PlayerColor(0x3E4447, "gray"),
            new PlayerColor(0xBD44B3, "magenta"),
            new PlayerColor(0x141519, "black"),
            // the last color is the fallback color, used when team color can't be determined
            new PlayerColor(0xE9ECEC, "white"),
    };

    public static int nextColorId = 0;
    public static HashMap<String, Integer> playerColorId = new HashMap<>();

    private static int getPlayerTeamColorId(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            // fallback color - white
            return colors.length - 1;
        }

        // check for a cached color id
        var cachedColorId = playerColorId.getOrDefault(playerName, -1);
        if (cachedColorId >= 0) {
            return cachedColorId;
        }

        // if the color id is not yet cached for the player,
        // we take the next color id and increment.
        // TODO: use the starting block color instead
        var colorId = nextColorId;
        nextColorId = (nextColorId + 1) % (colors.length - 1); // -1 to skip white
        playerColorId.put(playerName, colorId);
        return colorId;
    }

    public static void reset() {
        playerColorId.clear();
        nextColorId = 0;
    }

    public static int getPlayerTeamColor(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return colors[colors.length - 1].hexCode;
        }

        int colorId = getPlayerTeamColorId(playerName);
        return colors[colorId].hexCode;
    }

    public static ResourceLocation getPlayerTeamColorIcon(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return colors[colors.length - 1].blockTexture;
        }

        int colorId = getPlayerTeamColorId(playerName);
        return colors[colorId].blockTexture;
    }

    public static ResourceLocation getPlayerTeamColorBedIcon(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return colors[colors.length - 1].bedIcon;
        }

        int colorId = getPlayerTeamColorId(playerName);
        return colors[colorId].bedIcon;
    }

    public static int getPlayerDisplayColor(String playerName) {
        if (PlayerColors.usePlayerTeamColor) {
            return PlayerColors.getPlayerTeamColor(playerName);
        }

        // fall back on alliance color
        Relationship unitRs = getPlayerToPlayerRelationship(playerName);
        return switch (unitRs) {
            case OWNED -> 0x33FF33;
            case FRIENDLY -> 0x3333FF;
            case HOSTILE -> 0xFF3333;
            default -> 0xFFFF19;
        };
    }

    public static Button getToggleTeamColorsButton() {
        return new Button(I18n.get("hud.orthoview.reignofnether.toggle_team_colors"),
                14,
                usePlayerTeamColor
                        ? PlayerColors.getPlayerTeamColorIcon(MC.player.getName().getString())
                        : new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/sword_and_bow.png"),
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                null,
                () -> false,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK) || !MinimapClientEvents.isLargeMap(),
                () -> true,
                () ->
                {
                    usePlayerTeamColor = !usePlayerTeamColor;
                },
                null,
                List.of(
                        fcs(I18n.get("hud.orthoview.reignofnether.using_player_team_color"), usePlayerTeamColor),
                        fcs(I18n.get("hud.orthoview.reignofnether.using_relation_color"), !usePlayerTeamColor),
                        fcs(I18n.get("hud.orthoview.reignofnether.color_type_toggle"), false)
                )
        );
    }
}
