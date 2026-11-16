package com.solegendary.reignofnether.rtsmap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RTSMapInfoClientEvents {

    static {
        WorldState.onClientReset(RTSMapInfoClientEvents::reset);
    }

    public static ArrayList<String> modeNames = new ArrayList<>();
    public static String selectedMode = "";
    public static boolean teamMode = false;
    public static String mapName = "";
    public static String description = "";
    public static Set<String> authors = new HashSet<>();
    public static String version = "";
    public static Set<String> lockedRules = new HashSet<>(); // map.json lockedRules: match rules the lobby can't change
    static int format = RTSMapInfoServerEvents.SUPPORTED_FORMAT; // synced by the server on join and after a Convert

    // the map's format: 1 = old (nothing loaded, Convert offered to mapmakers), SUPPORTED_FORMAT = playable and
    // editable, higher = made with a newer mod version (nothing loaded)
    public static int mapFormat() {
        return format;
    }

    public static boolean isEditable() {
        return format == RTSMapInfoServerEvents.SUPPORTED_FORMAT;
    }

    // why no match can start here, null when one can (the server refuses too, see RTSMapInfoServerEvents.playRefusal)
    @Nullable
    public static String playRefusal() {
        if (isEditable())
            return null;
        return I18n.get(format < RTSMapInfoServerEvents.SUPPORTED_FORMAT ? "map.reignofnether.format.play_old" : "map.reignofnether.format.play_newer");
    }

    public static Button getCycleModeButton() {
        return new ButtonBuilder("Cycle Mode")
                .iconResource(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/block/rts_start_block_white.png"))
                .isHidden(() -> modeNames.size() <= 1 || Minecraft.getInstance().player == null || Minecraft.getInstance().player.getPermissionLevel() < 2)
                .onLeftClick(() -> {
                    if (!modeNames.contains(selectedMode))
                        return;
                    int index = modeNames.indexOf(selectedMode);
                    if (index < 0)
                        index = 0;
                    String mode = modeNames.get((index + 1) % modeNames.size());
                    RTSMapInfoServerboundPacket.setStartingMode(mode);
                })
                .onRightClick(() -> {
                    if (!modeNames.contains(selectedMode))
                        return;
                    int index = modeNames.indexOf(selectedMode);
                    if (index < 0)
                        index = 0;
                    String mode = modeNames.get(
                            (index - 1 + modeNames.size()) % modeNames.size()
                    );
                    RTSMapInfoServerboundPacket.setStartingMode(mode);
                })
                .tooltipLines(List.of(
                        Component.translatable("message.reignofnether.rts_map_info_mode", selectedMode).getVisualOrderText()
                ))
                .build();
    }

    // the whole map.json (MapInfoMenu edits a copy); also keeps the mode list in step after an Editor save
    public static void setMapJson(String json) {
        RTSMapInfo info;
        JsonObject currentRules;
        try {
            info = RTSMapInfoLoader.fromJson(json);
            currentRules = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject(RTSMapInfoLoader.CURRENT_RULES);
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("Bad map.json from the server: " + e.getMessage());
            return;
        }
        modeNames = new ArrayList<>(info.getModeNames());
        selectedMode = info.getDefaultMode();
        mapName = info.getName();
        description = info.getDescription();
        authors = new HashSet<>(info.getAuthor());
        version = info.getVersion();
        lockedRules = new HashSet<>(info.getLockedRules());
        MapInfoMenu.synced(info, currentRules != null ? currentRules : new JsonObject());
    }

    // the map's own lang files (ron/lang), by locale
    private static JsonObject mapLang = new JsonObject();

    static void setLang(String json) {
        try {
            mapLang = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("Bad map lang from the server: " + e.getMessage());
        }
    }

    /** A key in the map's lang files (ron/lang, the player's language else en_us), else the mod's, else the text itself. */
    public static String mapText(String keyOrText) {
        String locale = Minecraft.getInstance().getLanguageManager().getSelected();
        for (String l : new String[] { locale, "en_us" }) {
            JsonObject lang = mapLang.getAsJsonObject(l);
            if (lang != null && lang.has(keyOrText))
                return lang.get(keyOrText).getAsString();
        }
        return I18n.exists(keyOrText) ? I18n.get(keyOrText) : keyOrText; // plain text isn't formatted: a % would throw
    }

    public static void reset() {
        mapLang = new JsonObject();
        MapInfoMenu.reset();
        modeNames = new ArrayList<>();
        selectedMode = "";
        teamMode = false;
        mapName = "";
        description = "";
        authors = new HashSet<>();
        version = "";
        lockedRules = new HashSet<>();
        format = RTSMapInfoServerEvents.SUPPORTED_FORMAT;
    }
}
