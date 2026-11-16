package com.solegendary.reignofnether.rtsmap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// the events the game fires as ron/events/<event>.mcfunction (RonFiles.fire), for both sides: the editor lists them,
// the server checks client input against them and rtsapi wait suggests them
public final class RonEvents {

    private RonEvents() { }

    // grouped as the editor's Add event sheet lists them
    public static final Map<String, List<String>> GROUPS = new LinkedHashMap<>();
    static {
        GROUPS.put("match", List.of("map_loaded", "player_joined", "match_start", "second", "second_player", "match_end",
                "player_defeated", "dialogue_end"));
        GROUPS.put("units", List.of("unit_trained", "unit_damaged", "unit_died", "order_given", "rally_set", "resources_returned"));
        GROUPS.put("buildings", List.of("building_built", "building_destroyed", "research_done"));
        GROUPS.put("input", List.of("camera_moved", "minimap_clicked", "unit_selected", "building_selected", "box_selected", "key_pressed"));
    }

    public static final List<String> ALL = GROUPS.values().stream().flatMap(List::stream).toList();

    // only the client can see these happen, so it sends them (GuideServer.input) while the map listens for them
    public static final List<String> INPUT = GROUPS.get("input");

    // what rtsapi wait for can wait on: not the ones that happen once before any file could be waiting, plus the zones
    public static final List<String> WAITABLE = Stream.concat(
            ALL.stream().filter(e -> !List.of("map_loaded", "player_joined", "match_start", "second_player", "match_end").contains(e)),
            Stream.of("zone_entered", "zone_left")).toList();
}
