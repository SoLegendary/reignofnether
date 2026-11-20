package com.solegendary.reignofnether.rtsmap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class RTSMapInfoLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_NAME = "map.json";
    // sent to the Editor next to map.json: the current value of each MAP_RULES gamerule, never saved
    public static final String CURRENT_RULES = "_currentRules";

    public static Path path(MinecraftServer server) {
        return RonFiles.dir(server).resolve(FILE_NAME);
    }

    private static Path worldFolder(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT);
    }

    private static Path jsonPath(Path worldFolder) {
        return worldFolder.toAbsolutePath().normalize().resolve("ron").resolve(FILE_NAME);
    }

    // the file as it is on disk now; null when there's none, throws when it's broken
    @Nullable
    private static JsonObject readRaw(Path worldFolder) throws IOException {
        Path jsonPath = jsonPath(worldFolder);
        return Files.exists(jsonPath) ? JsonParser.parseString(Files.readString(jsonPath)).getAsJsonObject() : null;
    }

    // The world's map format (see RTSMapInfoServerEvents.SUPPORTED_FORMAT): ron/map.json's "format", 1 when it has none.
    // Without map.json, a world holding old format data (see MapConverter.hasOldData) is format 1, and one with none of it
    // is a new map: format 2, its map.json written on its first save.
    // A broken map.json counts as 1, so nothing writes over it. Reads files only, so the world list can use it too
    public static int mapFormat(Path worldFolder) {
        worldFolder = worldFolder.toAbsolutePath().normalize();
        Path jsonPath = worldFolder.resolve("ron").resolve(FILE_NAME);
        if (Files.exists(jsonPath)) {
            try {
                JsonElement format = JsonParser.parseString(Files.readString(jsonPath)).getAsJsonObject().get("format");
                return format == null ? 1 : format.getAsInt();
            } catch (Exception e) {
                ReignOfNether.LOGGER.error("Failed to read the format of " + jsonPath + ": " + e.getMessage());
                return 1;
            }
        }
        return MapConverter.hasOldData(worldFolder) ? 1 : RTSMapInfoServerEvents.SUPPORTED_FORMAT;
    }

    // a broken file is logged and the world opens as a map without info, instead of crashing
    public static RTSMapInfo load(ServerLevel level) {
        return load(worldFolder(level.getServer()));
    }

    /** From the world's folder; the loading screen reads it before the server has started. */
    public static RTSMapInfo load(Path worldFolder) {
        try {
            JsonObject json = readRaw(worldFolder);
            return json == null ? null : fromJson(json);
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("Failed to read " + jsonPath(worldFolder) + ": " + e.getMessage());
            return null;
        }
    }

    // throws on broken json; also used for the copy the Editor sends back and forth
    public static RTSMapInfo fromJson(String text) {
        return fromJson(JsonParser.parseString(text).getAsJsonObject());
    }

    private static RTSMapInfo fromJson(JsonObject json) {
        RTSMapInfo info = GSON.fromJson(json, RTSMapInfo.class);
        info.fillMissing();
        info.sanitize();
        return info;
    }

    public static JsonObject toTree(RTSMapInfo info) {
        return GSON.toJsonTree(info).getAsJsonObject();
    }

    public static String toJson(Object info) {
        return GSON.toJson(info);
    }

    // merges into ron/map.json as it is on disk now: only what differs from before
    // (the map as last read or written) is written, so hand edits made since and keys the mod doesn't know (_comment...) stay.
    // Returns the map as saved, null when it couldn't (a broken file on disk is left alone)
    @Nullable
    public static RTSMapInfo save(MinecraftServer server, RTSMapInfo info, JsonObject before) {
        Path jsonPath = path(server);
        try {
            JsonObject disk = readRaw(worldFolder(server));
            if (disk == null)
                disk = new JsonObject();
            merge(disk, before, toTree(info));
            Files.createDirectories(jsonPath.getParent());
            Files.writeString(jsonPath, GSON.toJson(disk));
            return fromJson(disk);
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("Failed to write " + jsonPath + ": " + e.getMessage());
            return null;
        }
    }

    // objects (rules, modes) merge one key at a time, anything else is replaced whole
    private static void merge(JsonObject disk, JsonObject before, JsonObject now) {
        for (String key : List.copyOf(before.keySet()))
            if (!now.has(key))
                disk.remove(key);
        for (Map.Entry<String, JsonElement> entry : now.entrySet()) {
            JsonElement old = before.get(entry.getKey());
            if (entry.getValue().equals(old))
                continue;
            if (old instanceof JsonObject oldObj && entry.getValue() instanceof JsonObject nowObj && disk.get(entry.getKey()) instanceof JsonObject diskObj)
                merge(diskObj, oldObj, nowObj);
            else
                disk.add(entry.getKey(), entry.getValue());
        }
    }
}
