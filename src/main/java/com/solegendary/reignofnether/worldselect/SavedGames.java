package com.solegendary.reignofnether.worldselect;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * RTS maps are never played directly: opening one plays a copy, a saved game, so the map stays as it was made.
 * A saved game is a normal world with a ron-save.json naming the map it came from.
 */
public final class SavedGames {

    private SavedGames() { }

    public static final String MARKER = "ron-save.json";

    public record Info(String map, String mapName, long created) { }

    /** The saved game's info, or null when the world isn't one. */
    public static Info read(Path world) {
        Path marker = world.resolve(MARKER);
        if (!Files.isRegularFile(marker))
            return null;
        try {
            JsonObject o = JsonParser.parseString(Files.readString(marker, StandardCharsets.UTF_8)).getAsJsonObject();
            return new Info(o.get("map").getAsString(), o.get("mapName").getAsString(), o.get("created").getAsLong());
        } catch (Exception e) {
            return new Info("", "", 0);
        }
    }

    /** The name a saved game of this map gets, e.g. "Oceanborn · 26 Oct 21:40". */
    public static String saveName(String mapName, long time) {
        return mapName + " · " + new SimpleDateFormat("d MMM HH:mm", Locale.ROOT).format(new Date(time));
    }

    /**
     * Copies the map's world folder into a new saved game and returns its folder name.
     * The lock file of a map that is open elsewhere is left behind.
     */
    public static String cloneMap(Path saves, String mapFolder, String mapName, long time) throws IOException {
        Path source = saves.resolve(mapFolder);
        // folder names can't hold ':' or other characters some systems refuse
        String base = saveName(mapName, time).replace(':', '-').replaceAll("[\\\\/*?\"<>|]", "_");
        String folder = base;
        for (int i = 2; Files.exists(saves.resolve(folder)); i++)
            folder = base + " (" + i + ")";
        Path dest = saves.resolve(folder);

        List<Path> paths;
        try (Stream<Path> walk = Files.walk(source)) {
            paths = walk.toList();
        }
        try {
            for (Path p : paths) {
                Path rel = source.relativize(p);
                if (rel.toString().equals("session.lock") || rel.getFileName().toString().equals(MARKER))
                    continue;
                Path target = dest.resolve(rel.toString());
                if (Files.isDirectory(p))
                    Files.createDirectories(target);
                else
                    Files.copy(p, target);
            }
            JsonObject marker = new JsonObject();
            marker.addProperty("map", mapFolder);
            marker.addProperty("mapName", mapName);
            marker.addProperty("created", time);
            Files.writeString(dest.resolve(MARKER), marker.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            deleteQuietly(dest); // never leave half a world in the list
            throw e;
        }
        return folder;
    }

    private static void deleteQuietly(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }
}
