package com.solegendary.reignofnether.rtsmap;

import net.minecraft.core.BlockPos;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RTSMapInfo {

    private String name;
    private List<String> author;
    private String description;
    private String version;
    private List<MapInfoStartPos> startPositions;
    private String defaultMode;
    private Map<String, List<List<Integer>>> modes;

    /**
     * Intermediate deserialization class — Gson can't construct BlockPos directly.
     * Maps directly to your JSON spawn objects: { "id": 1, "x": 100, "y": 64, "z": 200 }
     */
    private static class MapInfoStartPos {
        int id;
        int x, y, z;

        BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }

    // --- Getters ---
    public String getName() { return name; }
    public List<String> getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getVersion() { return version; }
    public String getDefaultMode() { return defaultMode; }
    public void setDefaultMode(String mode) { defaultMode = mode; }
    public Map<String, List<List<Integer>>> getModes() { return modes; }

    private MapInfoStartPos getEntryById(int id) {
        return startPositions.stream()
                .filter(s -> s.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No spawn with id: " + id));
    }

    public List<List<BlockPos>> getTeams() {
        List<List<Integer>> teamSpawnIds = modes.get(defaultMode);
        if (teamSpawnIds == null) return null;

        List<List<BlockPos>> teams = new java.util.ArrayList<>();
        for (List<Integer> teamSpawnId : teamSpawnIds) {
            List<BlockPos> team = teamSpawnId.stream()
                    .map(id -> getEntryById(id).toBlockPos())
                    .collect(Collectors.toList());
            teams.add(team);
        }
        return teams;
    }

    public boolean supportsMode(String mode) {
        return modes != null && modes.containsKey(mode);
    }
}