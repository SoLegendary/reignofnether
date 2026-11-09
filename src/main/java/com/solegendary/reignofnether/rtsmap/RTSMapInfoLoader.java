package com.solegendary.reignofnether.rtsmap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public class RTSMapInfoLoader {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String FILE_NAME = "rtsmap.json";

    public static RTSMapInfo load(ServerLevel level) {
        return load(level.getServer().getWorldPath(LevelResource.ROOT));
    }

    /** From the world's folder; the loading screen reads it before the server has started. */
    public static RTSMapInfo load(Path worldFolder) {
        worldFolder = worldFolder.toAbsolutePath();
        Path jsonPath = worldFolder.resolve(FILE_NAME);

        if (!Files.exists(jsonPath)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(jsonPath)) {
            return GSON.fromJson(reader, RTSMapInfo.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + FILE_NAME + " at " + jsonPath, e);
        }
    }
}
