package com.solegendary.reignofnether.rtsmap;

import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// ron/map.json (MapConverter reads the older rtsmap.json into it, renaming "author" and "startPositions")
public class RTSMapInfo {

    // what the lobby host picks each match (GameruleClient.rules); map.json "rules" holds the map's default and
    // "lockedRules" the ones the host can't change. fogOfWar isn't a gamerule, applyRules turns fog on or off
    public static final List<String> MATCH_RULES = List.of("fogOfWar", "maxPopulation", "allowedHeroes",
            "randomItemDrops", "neutralAggro", "beaconWinMinutes", "hideEnemyPicks", "lockAlliances", "allChat",
            "coCommanders", "soldiersPerTeam", "soldierRespawnSeconds");

    // every start position open, each its own team until players pick teams in the lobby; offered on every map
    public static final String CUSTOM_MODE = "custom";

    private int format = 1; // map format, see RTSMapInfoServerEvents.SUPPORTED_FORMAT; files without one are 1
    private String name = "";
    @SerializedName(value = "authors", alternate = "author")
    private List<String> authors = new ArrayList<>();
    private String description = "";
    private String version = "";
    @SerializedName(value = "seats", alternate = "startPositions")
    private List<Seat> seats = new ArrayList<>();
    private String defaultMode = "none";
    transient String selectedMode = null; // the lobby's pick for this session, never saved; null = defaultMode
    private Map<String, List<List<Integer>>> modes = new LinkedHashMap<>();
    // gamerules set when the world opens: facts about the map (groundYLevel, allowBeacons...) and defaults for the
    // match rules (maxPopulation, fogOfWar...), which the lobby host can still change unless lockedRules has them
    private Map<String, JsonPrimitive> rules = new LinkedHashMap<>();
    // match rules the lobby host can't change (only ops), by their "rules" id, e.g. ["fogOfWar"]
    private List<String> lockedRules = new ArrayList<>();
    // solo maps: the world opens straight into the match, its host in the first open seat (StartPosServerEvents.autoStart)
    private boolean autoStart = false;

    // a place a player starts at: { "id": 1, "x": 100, "y": 64, "z": 200 }, the rest is optional
    public static class Seat {
        public int id;
        public int x, y, z;
        public String name;     // shown instead of the player's name, e.g. "Rebels"
        public String faction;  // fixed faction (e.g. "reignofnether:villagers"), null = the player picks
        public Integer food, wood, ore; // fixed starting resources, null = the normal amount
        public String control;  // "player" (default), "bot" or "none" (units stay put, nobody plays it)
        public Integer role;    // the v1 scenario role it was converted from: that role's old units join it (MapConverter)
        // what the match gives it at its position: "capitol" (default: foundations, workers, scout), "units" (workers
        // and scout), "empty" (nothing, scripts give it units) or "soldier" (the player fights in first person for it)
        public String start;

        public Seat() { }

        public Seat(int id, BlockPos pos) {
            this.id = id;
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }

        public BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }

        public String getControl() {
            return control == null ? "player" : control;
        }

        public String getStart() {
            return start == null ? "capitol" : start;
        }

        // "villagers" or "reignofnether:villagers", null when the player picks
        public ResourceLocation getFactionKey() {
            if (faction == null || faction.isBlank())
                return null;
            return ResourceLocation.tryParse(faction.contains(":") ? faction : ReignOfNether.MOD_ID + ":" + faction);
        }
    }

    // --- Getters ---
    public int getFormat() { return format; }
    public void setFormat(int format) { this.format = format; }
    public String getName() { return name; }
    public List<String> getAuthor() { return authors; }
    public String getDescription() { return description; }
    public String getVersion() { return version; }
    public String getDefaultMode() { return defaultMode; }
    public void setDefaultMode(String defaultMode) { this.defaultMode = defaultMode; }
    public String getSelectedMode() { return selectedMode != null && supportsMode(selectedMode) ? selectedMode : defaultMode; }
    public void setSelectedMode(String mode) { this.selectedMode = mode; }
    public Map<String, List<List<Integer>>> getModes() { return modes; }
    public List<Seat> getSeats() { return seats; }
    public Map<String, JsonPrimitive> getRules() { return rules; }
    public List<String> getLockedRules() { return lockedRules; }
    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }

    // --- Setters, for the Editor ---
    public void setName(String name) { this.name = name; }
    public void setAuthors(List<String> authors) { this.authors = new ArrayList<>(authors); }
    public void setDescription(String description) { this.description = description; }
    public void setVersion(String version) { this.version = version; }

    // gson leaves a field null when the json has it as null
    void fillMissing() {
        if (name == null) name = "";
        if (authors == null) authors = new ArrayList<>();
        if (description == null) description = "";
        if (version == null) version = "";
        if (seats == null) seats = new ArrayList<>();
        if (defaultMode == null) defaultMode = "none";
        if (modes == null) modes = new LinkedHashMap<>();
        if (rules == null) rules = new LinkedHashMap<>();
        if (lockedRules == null) lockedRules = new ArrayList<>();
    }

    // drops what would break a match (a hand edit, a seat removed): empty seat entries, team ids of missing seats,
    // empty teams and modes; an unknown defaultMode becomes "none"
    void sanitize() {
        List<String> dropped = new ArrayList<>();
        if (seats.removeIf(Objects::isNull))
            dropped.add("empty seat entries");
        Set<Integer> seatIds = seats.stream().map(s -> s.id).collect(Collectors.toSet());
        for (Map.Entry<String, List<List<Integer>>> mode : modes.entrySet()) {
            List<List<Integer>> teams = new ArrayList<>();
            for (List<Integer> team : mode.getValue() == null ? List.<List<Integer>>of() : mode.getValue()) {
                List<Integer> kept = team == null ? List.of() : team.stream().filter(seatIds::contains).toList();
                if (team != null && kept.size() < team.size())
                    dropped.add("mode " + mode.getKey() + ": seat ids " + team.stream().filter(id -> !seatIds.contains(id)).toList());
                if (!kept.isEmpty())
                    teams.add(new ArrayList<>(kept));
            }
            mode.setValue(teams);
        }
        modes.entrySet().removeIf(mode -> {
            if (!mode.getValue().isEmpty())
                return false;
            dropped.add("empty mode " + mode.getKey());
            return true;
        });
        if (!supportsMode(defaultMode)) {
            dropped.add("defaultMode " + defaultMode);
            defaultMode = "none";
        }
        if (!dropped.isEmpty())
            ReignOfNether.LOGGER.info("map.json: dropped " + String.join(", ", dropped));
    }

    public Seat getSeat(int id) {
        return seats.stream().filter(s -> s.id == id).findFirst().orElse(null);
    }

    public Seat getSeat(BlockPos pos) {
        return seats.stream().filter(s -> s.toBlockPos().equals(pos)).findFirst().orElse(null);
    }

    // the selected mode's teams; empty for "none" (every seat is its own team)
    public List<List<BlockPos>> getTeams() {
        String mode = getSelectedMode();
        if (isCustom())
            return lobbySeats().stream().map(seat -> List.of(seat.toBlockPos())).collect(Collectors.toList());
        List<List<Integer>> teamSeatIds = mode.equalsIgnoreCase("none") ? null : modes.get(mode);
        if (teamSeatIds == null)
            return List.of();

        List<List<BlockPos>> teams = new ArrayList<>();
        for (List<Integer> teamSeatId : teamSeatIds) {
            List<BlockPos> team = teamSeatId.stream().map(this::getSeat).filter(Objects::nonNull)
                    .map(Seat::toBlockPos).collect(Collectors.toList());
            if (!team.isEmpty())
                teams.add(team);
        }
        return teams;
    }

    // teams fixed by the map (1v1, 2v2...), not a free for all where everyone is their own team
    public boolean isTeamMode() {
        if (isCustom())
            return false;
        List<List<BlockPos>> teams = getTeams();
        if (teams.size() < 2)
            return false;
        return teams.size() == 2 || teams.stream().anyMatch(team -> team.size() > 1);
    }

    public boolean supportsMode(String mode) {
        return mode.equalsIgnoreCase("none") || modes != null && modes.containsKey(mode) || mode.equals(CUSTOM_MODE) && offersCustom();
    }

    // the map's own "custom" mode, if it has one, wins
    private boolean isCustom() {
        return CUSTOM_MODE.equals(getSelectedMode()) && !modes.containsKey(CUSTOM_MODE);
    }

    private boolean offersCustom() {
        return lobbySeats().size() >= 2;
    }

    // seats the classic lobby can offer: not "none"
    private List<Seat> lobbySeats() {
        return seats.stream().filter(seat -> !seat.getControl().equals("none")).toList();
    }

    /** The map's modes, then Custom. */
    public List<String> getModeNames() {
        List<String> names = new ArrayList<>(modes.keySet());
        if (offersCustom() && !names.contains(CUSTOM_MODE))
            names.add(CUSTOM_MODE);
        return names;
    }
}
