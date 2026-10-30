package com.solegendary.reignofnether.rtsmap;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RTSMapInfo {

    // every start position open, each its own team until players pick teams in the lobby; offered on every map
    public static final String CUSTOM_MODE = "custom";

    private String name;
    private List<String> author;
    private String description;
    private String version;
    private List<MapInfoStartPos> startPositions;
    private String defaultMode; // this is also the actual selected mode, but is the same as the default for the Gson to work
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
    public void setDefaultMode(String defaultMode) { this.defaultMode = defaultMode; }
    public Map<String, List<List<Integer>>> getModes() { return modes; }

    private MapInfoStartPos getEntryById(int id) {
        return startPositions.stream()
                .filter(s -> s.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No spawn with id: " + id));
    }

    public List<List<BlockPos>> getTeams() {
        if (getDefaultMode().equalsIgnoreCase("none"))
            return List.of();

        List<List<Integer>> teamSpawnIds = modes != null ? modes.get(defaultMode) : null;
        if (teamSpawnIds == null && isCustom())
            return startPositions.stream().map(sp -> List.of(sp.toBlockPos())).collect(Collectors.toList());
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

    // teams fixed by the map (1v1, 2v2...), not a free for all where everyone is their own team
    public boolean isTeamMode() {
        if (isCustom())
            return false;
        List<List<BlockPos>> teams = getTeams();
        if (teams == null || teams.size() < 2)
            return false;
        return teams.size() == 2 || teams.stream().anyMatch(team -> team.size() > 1);
    }

    public boolean supportsMode(String mode) {
        return mode.equalsIgnoreCase("none") || modes != null && modes.containsKey(mode) || mode.equals(CUSTOM_MODE) && offersCustom();
    }

    // the map's own "custom" mode, if it has one, wins
    private boolean isCustom() {
        return CUSTOM_MODE.equals(defaultMode) && (modes == null || !modes.containsKey(CUSTOM_MODE));
    }

    private boolean offersCustom() {
        return startPositions != null && startPositions.size() >= 2;
    }

    /** The map's modes, then Custom. */
    public List<String> getModeNames() {
        List<String> names = new java.util.ArrayList<>(modes != null ? modes.keySet() : java.util.Set.of());
        if (offersCustom() && !names.contains(CUSTOM_MODE))
            names.add(CUSTOM_MODE);
        return names;
    }
}