package com.solegendary.reignofnether.sandbox;

import com.mojang.blaze3d.platform.InputConstants;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.ui.CodeEditor;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.resources.ResourceLocation;
import com.solegendary.reignofnether.editor.CodeEditorPopup;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.hud.MyEditBox;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.hud.panels.SettingRows;
import com.solegendary.reignofnether.hud.panels.SettingsMenu;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.matchstart.MatchStartScreen;
import com.solegendary.reignofnether.rtsmap.MapInfoMenu;
import com.solegendary.reignofnether.rtsmap.RTSMapInfo;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoClientEvents;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerboundPacket;
import com.solegendary.reignofnether.rtsmap.RonEvents;
import com.solegendary.reignofnether.rtsmap.RonFiles;
import com.solegendary.reignofnether.rtsmap.RonFilesClient;
import com.solegendary.reignofnether.ui.EditorRows;
import com.solegendary.reignofnether.ui.EditorRows.Group;
import com.solegendary.reignofnether.ui.EditorRows.Row;
import com.solegendary.reignofnether.ui.EditorRows.Style;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.lwjgl.glfw.GLFW;

/**
 * The dock's sheets that edit the map's files: Rules, Seats (with a seat's and a mode's detail view), Map and
 * Scripts (the ron/ file tree, with the event list). They edit MapInfoMenu's copy of ron/map.json, saved by the
 * sheet's Save footer; script files open in the CodeEditorPopup.
 */
public final class MapSheets {

    private MapSheets() { }

    /** What a sheet shows: its header (◀ ▶ when nav isn't null, back to the list), rows and Save footer. */
    record Sheet(String title, String file, List<Group> groups, @Nullable IntConsumer nav, boolean back, boolean save) { }

    enum Sub { SEAT, MODE, EVENTS }

    private static final Minecraft MC = Minecraft.getInstance();
    private static final String KEY = "sandbox.reignofnether.";
    private static final String MAP_KEY = "sandbox.reignofnether.map.";
    private static final String PLAYERS = "editor.reignofnether.players.";
    private static final String LOGIC = "editor.reignofnether.logic.";
    private static final String MAP_JSON = "ron/map.json";
    private static final List<String> CONTROLS = List.of("player", "bot", "none");
    private static final List<String> STARTS = List.of("capitol", "units", "empty", "soldier");
    private static final int TEXT_W = HudLayout.DOCK_SHEET_W - 7 * 2 - 4;
    private static final int DOT = 10;
    private static final int BUTTON_W = 44;
    private static final int STEP = 50;
    private static final int BIG_STEP = 500;

    @Nullable
    static Sub sub = null; // the detail view open in its tab, null: the tab's list
    static String subKey = "";

    static {
        // a renamed mode stays open
        MapInfoMenu.onModeRenamed((from, to) -> {
            if (sub == Sub.MODE && subKey.equals(from))
                subKey = to;
        });
    }

    static void open(@Nullable Sub view, String key) {
        sub = view;
        subKey = key;
        SandboxPanel.scrollTop();
    }

    static boolean optimised() {
        return FogOfWarClientEvents.isOnRtsOptimisedMap();
    }

    // ------------------------------------------------------------------ rules

    // Map: map-only rules. Match defaults: each match rule's default on this map and whether the lobby can change it.
    // Both are saved in map.json; without it (not an op, or an old or newer format map) the live match rules show
    enum RulesTab { MAP, DEFAULTS }

    static RulesTab rulesTab = RulesTab.MAP; // map rules first: what the mapmaker opened the tab for

    static Sheet rules(boolean op) {
        boolean mapRules = op && RTSMapInfoClientEvents.isEditable(); // an old or newer format map has none loaded
        if (!mapRules)
            rulesTab = RulesTab.DEFAULTS;

        List<Group> groups = new ArrayList<>();
        if (mapRules) {
            List<SandboxPanel.Chip> tabs = new ArrayList<>();
            for (RulesTab t : RulesTab.values())
                tabs.add(new SandboxPanel.Chip(I18n.get(KEY + "rules.tab." + t.name().toLowerCase(Locale.ROOT)), null,
                        rulesTab == t, () -> {
                            rulesTab = t;
                            SandboxPanel.scrollTop();
                        }));
            groups.add(new Group(null, List.of((f, x, y, w) -> SandboxPanel.chips(f, tabs, x, y, w))));
        }

        if (rulesTab == RulesTab.MAP)
            groups.add(new Group(null, MapInfoMenu.ruleRows()));
        else if (mapRules || SettingsMenu.canOpenGamerules()) {
            for (boolean advanced : new boolean[] { false, true }) {
                List<Row> rows = new ArrayList<>();
                for (GameruleClient.Rule rule : GameruleClient.rules())
                    if (rule.advanced() == advanced)
                        rows.add(mapRules ? MapInfoMenu.matchDefaultRow(rule) : (f, x, y, w) -> SettingRows.renderRow(f, rule.toRow(), x, y, w));
                groups.add(new Group(GameruleClient.groupName(advanced), rows));
            }
        }
        if (!optimised())
            groups.add(new Group(null, warnRows(I18n.get(KEY + "rts.fog"))));
        return new Sheet(I18n.get(KEY + "tab.rules"), mapRules ? I18n.get(KEY + "rules.file") : "", groups, null, false, mapRules);
    }

    // ------------------------------------------------------------------ seats

    static Sheet seats() {
        RTSMapInfo info = MapInfoMenu.info();
        if (sub == Sub.SEAT) {
            RTSMapInfo.Seat seat = seatById(info, subKey);
            if (seat != null)
                return seat(info, seat);
        }
        if (sub == Sub.MODE && info.getModes().containsKey(subKey))
            return mode(info, subKey);
        sub = null;

        String mode = shownMode(info);
        List<Row> seats = new ArrayList<>();
        for (RTSMapInfo.Seat seat : info.getSeats()) {
            String detail = I18n.get(MAP_KEY + "control." + seat.getControl()) + (seat.faction != null ? " · " + MapInfoMenu.factionName(seat.faction) : "");
            int team = mode == null ? 0 : MapInfoMenu.teamOf(info.getModes().get(mode), seat.id);
            seats.add(listRow(MapInfoMenu.seatName(seat), detail, Theme.LABEL, teamColour(team), String.valueOf(seat.id),
                    I18n.get(KEY + "custom_buildings.edit"), () -> open(Sub.SEAT, String.valueOf(seat.id))));
        }
        seats.add(0, headerRow(I18n.get(PLAYERS + "seats", seats.size()), List.of(new Action(I18n.get(PLAYERS + "add_here"), () -> {
            int id = MapInfoMenu.addSeat(info);
            if (id >= 0)
                open(Sub.SEAT, String.valueOf(id));
        }))));

        List<Row> modes = new ArrayList<>();
        modes.add(headerRow(I18n.get(PLAYERS + "modes"), EditorRows.tip(I18n.get(PLAYERS + "modes"), I18n.get(PLAYERS + "modes_note")), List.of(
                new Action(I18n.get(PLAYERS + "auto"), () -> MapInfoMenu.autoModes(info)),
                new Action("+", () -> open(Sub.MODE, MapInfoMenu.addMode(info))))));
        for (String name : info.getModes().keySet()) {
            boolean isDefault = name.equals(info.getDefaultMode());
            modes.add(listRow(name, isDefault ? "★ " + I18n.get(PLAYERS + "default") : "", Theme.GOLD, 0, "",
                    I18n.get(KEY + "custom_buildings.edit"), () -> open(Sub.MODE, name)));
        }

        String autoLabel = I18n.get(PLAYERS + "auto_start");
        List<Row> lobby = List.of((f, x, y, w) -> EditorRows.toggle(f, autoLabel, EditorRows.tip(autoLabel, I18n.get(PLAYERS + "auto_start_tooltip")),
                info.isAutoStart(), () -> {
                    info.setAutoStart(!info.isAutoStart());
                    MapInfoMenu.changed();
                }, x, y, w));

        List<Group> groups = new ArrayList<>(List.of(new Group(null, seats), new Group(null, modes), new Group(null, lobby)));
        return new Sheet(I18n.get(KEY + "tab.seats"), I18n.get(KEY + "seats.file"), groups, null, false, true);
    }

    @Nullable
    private static RTSMapInfo.Seat seatById(RTSMapInfo info, String id) {
        try {
            return info.getSeat(Integer.parseInt(id));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // the mode whose teams colour the seats: the default one, else the first
    @Nullable
    private static String shownMode(RTSMapInfo info) {
        if (info.getModes().containsKey(info.getDefaultMode()))
            return info.getDefaultMode();
        return info.getModes().keySet().stream().findFirst().orElse(null);
    }

    private static int teamColour(int team) {
        return team == 0 ? Theme.SECONDARY : MatchStartScreen.TEAM_COLOURS[(team - 1) % MatchStartScreen.TEAM_COLOURS.length];
    }

    // steps through the list, wrapping round
    private static <T> IntConsumer stepper(List<T> items, T current, Consumer<T> show) {
        return d -> {
            int i = Math.max(0, items.indexOf(current));
            show.accept(items.get(Math.floorMod(i + d, items.size())));
        };
    }

    private static Sheet seat(RTSMapInfo info, RTSMapInfo.Seat seat) {
        String name = MapInfoMenu.seatName(seat);
        List<Row> rows = new ArrayList<>();
        rows.add(fieldRow(I18n.get(PLAYERS + "name"), MapInfoMenu.seatNameBox(seat), I18n.get(MAP_KEY + "seat", seat.id)));
        rows.add((f, x, y, w) -> EditorRows.stepper(f, I18n.get(MAP_KEY + "faction"), EditorRows.tip(name, I18n.get(MAP_KEY + "faction_tooltip")),
                MapInfoMenu.factionName(seat.faction),
                () -> seat.faction = MapInfoMenu.cycle(MapInfoMenu.factionOptions(), seat.faction, 1),
                () -> seat.faction = MapInfoMenu.cycle(MapInfoMenu.factionOptions(), seat.faction, -1), x, y, w));
        rows.add((f, x, y, w) -> EditorRows.stepper(f, I18n.get(PLAYERS + "played_by"), EditorRows.tip(name, I18n.get(MAP_KEY + "control_tooltip")),
                I18n.get(MAP_KEY + "control." + seat.getControl()),
                () -> seat.control = MapInfoMenu.cycle(CONTROLS, seat.getControl(), 1),
                () -> seat.control = MapInfoMenu.cycle(CONTROLS, seat.getControl(), -1), x, y, w));
        if (seat.getControl().equals("bot") && !optimised())
            rows.addAll(warnRows(I18n.get(KEY + "rts.bot")));
        rows.add((f, x, y, w) -> EditorRows.stepper(f, I18n.get(PLAYERS + "start"), EditorRows.tip(name, I18n.get(PLAYERS + "start_tooltip")),
                I18n.get(PLAYERS + "start." + seat.getStart()),
                () -> seat.start = MapInfoMenu.cycle(STARTS, seat.getStart(), 1),
                () -> seat.start = MapInfoMenu.cycle(STARTS, seat.getStart(), -1), x, y, w));

        List<Row> end = new ArrayList<>(noteRows(seat.getControl().equals("none")
                ? I18n.get(PLAYERS + "nobody_note", name) : I18n.get(PLAYERS + "command_note", name.replace(' ', '_'))));
        end.add(dangerRow(I18n.get(PLAYERS + "remove_seat"), () -> {
            MapInfoMenu.removeSeat(info, seat);
            open(null, "");
        }));
        List<Group> groups = List.of(
                new Group(null, rows),
                new Group(I18n.get(PLAYERS + "group.resources"), List.of(
                        resourceRow(I18n.get(PLAYERS + "food"), () -> seat.food, v -> seat.food = v),
                        resourceRow(I18n.get(PLAYERS + "wood"), () -> seat.wood, v -> seat.wood = v),
                        resourceRow(I18n.get(PLAYERS + "ore"), () -> seat.ore, v -> seat.ore = v))),
                new Group(null, end));
        List<String> ids = info.getSeats().stream().map(s -> String.valueOf(s.id)).toList();
        return new Sheet(name, MAP_JSON + " · " + I18n.get(MAP_KEY + "seat", seat.id), groups,
                stepper(ids, String.valueOf(seat.id), id -> open(Sub.SEAT, id)), true, true);
    }

    // null = the normal starting amount; Shift for bigger steps
    private static Row resourceRow(String label, Supplier<Integer> get, Consumer<Integer> set) {
        return (f, x, y, w) -> {
            Integer v = get.get();
            int step = Keybindings.shiftMod.isDown() ? BIG_STEP : STEP;
            EditorRows.stepper(f, label, EditorRows.tip(label, I18n.get(PLAYERS + "resource_tooltip", BIG_STEP)),
                    v == null ? I18n.get(PLAYERS + "default") : String.valueOf(v),
                    () -> {
                        set.accept((v == null ? 0 : v) + step);
                        MapInfoMenu.changed();
                    },
                    () -> {
                        set.accept(v == null || v <= 0 ? null : Math.max(0, v - step));
                        MapInfoMenu.changed();
                    }, x, y, w);
        };
    }

    private static Sheet mode(RTSMapInfo info, String mode) {
        List<List<Integer>> teams = info.getModes().get(mode);
        String defaultLabel = I18n.get(PLAYERS + "default_in_lobby");
        List<Row> rows = new ArrayList<>();
        rows.add(fieldRow(I18n.get(PLAYERS + "name"), MapInfoMenu.modeNameBox(mode), mode));
        rows.add((f, x, y, w) -> {
            boolean on = mode.equals(info.getDefaultMode());
            EditorRows.toggle(f, defaultLabel, EditorRows.tip(defaultLabel, I18n.get(MAP_KEY + "default_mode_tooltip")), on, () -> {
                info.setDefaultMode(on ? "none" : mode);
                MapInfoMenu.changed();
            }, x, y, w);
        });

        List<Row> teamRows = new ArrayList<>();
        List<RTSMapInfo.Seat> seats = info.getSeats();
        int n = seats.size();
        for (RTSMapInfo.Seat seat : seats) {
            String name = MapInfoMenu.seatName(seat);
            teamRows.add((f, x, y, w) -> {
                int team = MapInfoMenu.teamOf(teams, seat.id);
                EditorRows.stepper(f, name, EditorRows.tip(name, I18n.get(MAP_KEY + "team_tooltip")),
                        team == 0 ? "–" : I18n.get(MAP_KEY + "team", team),
                        () -> MapInfoMenu.setTeam(teams, seat.id, (team + 1) % (n + 1)),
                        () -> MapInfoMenu.setTeam(teams, seat.id, (team + n) % (n + 1)), x, y, w);
            });
        }
        teamRows.addAll(noteRows(I18n.get(PLAYERS + "teams_note")));
        teamRows.add(dangerRow(I18n.get(PLAYERS + "remove_mode"), () -> {
            MapInfoMenu.removeMode(info, mode);
            open(null, "");
        }));
        List<String> names = new ArrayList<>(info.getModes().keySet());
        return new Sheet(mode, MAP_JSON + " · " + mode, List.of(new Group(null, rows), new Group(I18n.get(PLAYERS + "group.teams"), teamRows)),
                stepper(names, mode, name -> open(Sub.MODE, name)), true, true);
    }

    // ------------------------------------------------------------------ map

    static Sheet map() {
        int border = MapInfoMenu.borderSize();
        int max = WorldBorderServerEvents.RTS_OPTIMIZED_BORDER;
        List<Row> rts = new ArrayList<>();
        if (optimised()) {
            rts.add(textRow(I18n.get(MAP_KEY + "border_n", border), I18n.get(MAP_KEY + "yes"), Theme.OK));
            rts.addAll(noteRows(I18n.get(MAP_KEY + "optimised_note")));
        } else {
            rts.addAll(warnRows(I18n.get(MAP_KEY + "not_optimised", border, max)));
            rts.add(buttonRow(I18n.get(KEY + "rts.fix", max), Style.PRIMARY, () -> MapInfoMenu.fitBorder(max)));
        }
        return new Sheet(I18n.get(KEY + "tab.map"), MAP_JSON, List.of(
                new Group(I18n.get(MAP_KEY + "rts_optimised"), rts),
                new Group(I18n.get(MAP_KEY + "group.border"), Stream.concat(Stream.of(MapInfoMenu.borderRow()), MapInfoMenu.borderCentreRows().stream()).toList()),
                new Group(I18n.get(KEY + "tab.map"), MapInfoMenu.infoRows())), null, false, true);
    }

    // ------------------------------------------------------------------ scripts

    // events/<event>/<type> files the game also fires, with the types each can have
    private static final Map<String, Supplier<List<String>>> TYPED = Map.of(
            "unit_trained", CodeEditor::unitIds, "unit_selected", CodeEditor::unitIds,
            "building_built", MapSheets::buildingTypes, "building_destroyed", MapSheets::buildingTypes,
            "building_selected", MapSheets::buildingTypes, "research_done", CodeEditor::researchIds,
            "resources_returned", () -> List.of("food", "wood", "ore"),
            "order_given", () -> Arrays.stream(UnitAction.values()).filter(a -> a != UnitAction.NONE)
                    .map(a -> a.name().toLowerCase(Locale.ROOT)).toList());
    public static Map<String, Supplier<List<String>>> typedEvents() {
        return TYPED;
    }

    private static final Set<String> openFolders = new HashSet<>(Set.of("events", "src"));
    @Nullable
    private static MyEditBox newPathBox = null;

    // the file tree: main and map.json pinned, a field that makes a file, then folders (folded or open) and files the
    // way a code editor's explorer shows them. Rows have no buttons: a click opens, hovering shows what else there is
    static Sheet scripts() {
        if (sub == Sub.EVENTS)
            return events();
        sub = null;

        List<Row> pinned = new ArrayList<>();
        pinned.add(treeRow("main", 0, false, false, I18n.get(LOGIC + "runs.main"), Theme.ORANGE, null, null,
                () -> CodeEditorPopup.open("main", "main", runsWhen("main"))));
        pinned.add(treeRow("map.json", 0, false, false, I18n.get(LOGIC + "settings"), 0xFF4D9BFF, null, null, SandboxPanel::openMap));
        List<Row> rows = fileRows();
        List<Row> end = new ArrayList<>();
        end.add(scriptsFooterRow());
        return new Sheet(I18n.get(KEY + "tab.scripts"), I18n.get(KEY + "scripts.file"),
                List.of(new Group(null, pinned), new Group(I18n.get(LOGIC + "section.files"), rows), new Group(null, end)), null, false, false);
    }

    // the new file field, then the folders and files below main and map.json
    private static List<Row> fileRows() {
        List<String> paths = new ArrayList<>(RonFilesClient.files.keySet());
        paths.addAll(RonFilesClient.others);
        paths.removeIf(p -> p.equals("main") || p.equals("map.json"));
        if (paths.stream().noneMatch(p -> p.startsWith("events/")))
            paths.add("events/"); // the folder + Event lives on, even with no event files yet
        paths.sort(MapSheets::compareTree);
        List<Row> rows = new ArrayList<>();
        rows.add(newFileRow());
        Set<String> shownFolders = new HashSet<>();
        for (String path : paths) {
            String[] parts = path.split("/");
            String folder = "";
            boolean hidden = false;
            for (int i = 0; i < parts.length - 1 && !hidden; i++) {
                folder = folder.isEmpty() ? parts[i] : folder + "/" + parts[i];
                if (shownFolders.add(folder))
                    rows.add(folderRow(folder, i, paths));
                hidden = !openFolders.contains(folder);
            }
            if (!hidden && !path.endsWith("/"))
                rows.add(RonFilesClient.files.containsKey(path) ? scriptRow(path, parts.length - 1) : otherRow(path, parts.length - 1));
        }
        return rows;
    }

    // the open folder link and the triggers note
    private static Row scriptsFooterRow() {
        return (f, x, y, w) -> {
            String note = I18n.get(KEY + "scripts.triggers_short");
            if (MC.getSingleplayerServer() != null) {
                String open = I18n.get(KEY + "scripts.open_folder_link");
                Rect link = new Rect(x, y, Text.width(open) + 4, EditorRows.ROW_H);
                Text.draw(f.g, open, x, y + 4, f.hovered(link) ? Theme.GOLD : Theme.ORANGE);
                f.register(new ButtonBuilder("sheet open folder").onLeftClick(MapSheets::openFolder).build(), link, null, null);
            }
            Text.draw(f.g, Text.fit(note, w / 2), x + w - Math.min(Text.width(note), w / 2), y + 4, Theme.DIM);
        };
    }

    /**
     * A row of the tree: a ▸/▾ for a folder, an optional dot, the name, then a grey hint at the right; a hover action
     * (orange, like "+ New") shows in place of the hint while the row is hovered. The whole row clicks.
     */
    private static Row treeRow(String name, int depth, boolean folder, boolean open, String hint, int dot,
                               @Nullable String hoverAction, @Nullable Runnable onHoverAction, Runnable click) {
        return (f, x, y, w) -> {
            int h = EditorRows.ROW_H;
            Rect r = new Rect(x, y, w, h);
            boolean over = f.hovered(r);
            if (over)
                f.g.fill(x, y, x + w, y + h, Theme.HOVER);
            int tx = x + 4 + depth * 9;
            if (folder) {
                Text.draw(f.g, open ? "▾" : "▸", tx, y + 4, Theme.LABEL);
                tx += 9;
            } else if (dot != 0) {
                f.g.fill(tx, y + 7, tx + 4, y + 11, dot);
                tx += 9;
            } else
                tx += 9;
            f.register(new ButtonBuilder("sheet tree " + name + depth).onLeftClick(click).build(), r, null, null);
            int right = x + w - 4;
            if (over && hoverAction != null && onHoverAction != null) {
                int aw = Text.width(hoverAction);
                Rect a = new Rect(right - aw - 2, y, aw + 4, h);
                Text.draw(f.g, hoverAction, right - aw, y + 4, f.hovered(a) ? Theme.GOLD : Theme.ORANGE);
                f.register(new ButtonBuilder("sheet tree action " + name + depth).onLeftClick(onHoverAction).build(), a, null, null);
                right -= aw + 8;
            } else if (!hint.isEmpty()) {
                int room = Math.max(0, (right - tx) / 2);
                String shown = Text.fit(hint, room);
                Text.draw(f.g, shown, right - Text.width(shown), y + 4, Theme.DIM);
                right -= Text.width(shown) + 8;
            }
            Text.draw(f.g, Text.fit(name, right - tx), tx, y + 4, folder ? Theme.BODY : Theme.TEXT);
        };
    }

    // segment by segment, a folder before the files beside it
    private static int compareTree(String a, String b) {
        String[] pa = a.split("/"), pb = b.split("/");
        for (int i = 0; i < Math.min(pa.length, pb.length); i++) {
            if (pa[i].equals(pb[i]))
                continue;
            boolean folderA = i < pa.length - 1, folderB = i < pb.length - 1;
            if (folderA != folderB)
                return folderA ? -1 : 1;
            return pa[i].compareToIgnoreCase(pb[i]);
        }
        return Integer.compare(pa.length, pb.length);
    }

    private static Row folderRow(String folder, int depth, List<String> paths) {
        boolean open = openFolders.contains(folder);
        long count = paths.stream().filter(p -> p.startsWith(folder + "/") && !p.endsWith("/")).count();
        boolean events = folder.equals("events");
        return treeRow(folder.substring(folder.lastIndexOf('/') + 1) + "/", depth, true, open,
                count == 0 ? I18n.get(LOGIC + "empty") : I18n.get(LOGIC + "file_count", count), 0,
                I18n.get(LOGIC + (events ? "add_event_link" : "new_link")), events ? () -> open(Sub.EVENTS, "") : () -> {
                    newPathBox().setValue(folder + "/");
                    newPathBox().setFocused(true);
                }, () -> {
                    if (!openFolders.remove(folder))
                        openFolders.add(folder);
                });
    }

    // the hint shows on hover only: a long list of the same "when called" says nothing
    private static Row scriptRow(String path, int depth) {
        return hoverHint(treeRow(path.substring(path.lastIndexOf('/') + 1), depth, false, false, runsWhen(path), 0, null, null,
                () -> CodeEditorPopup.open(path, path, runsWhen(path))), treeRow(path.substring(path.lastIndexOf('/') + 1), depth,
                false, false, "", 0, null, null, () -> CodeEditorPopup.open(path, path, runsWhen(path))));
    }

    private static Row hoverHint(Row withHint, Row without) {
        return (f, x, y, w) -> (f.hovered(new Rect(x, y, w, EditorRows.ROW_H)) ? withHint : without).draw(f, x, y, w);
    }

    // a custom building's files open its editor; anything else the folder (singleplayer)
    private static Row otherRow(String path, int depth) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        CustomBuilding building = path.startsWith("buildings/") && path.indexOf('/', "buildings/".length()) < 0
                ? CustomBuildingClientEvents.customBuildings.stream()
                    .filter(b -> path.equals("buildings/" + b.id + ".json") || path.equals("buildings/" + b.id + ".nbt"))
                    .findFirst().orElse(null)
                : null;
        if (building != null)
            return treeRow(name, depth, false, false, building.name, 0, null, null, () -> CustomBuildingClientEvents.setCustomBuildingToEdit(building));
        return treeRow(name, depth, false, false, "", 0, null, null, MapSheets::openFolder);
    }

    private static MyEditBox newPathBox() {
        if (newPathBox == null) {
            newPathBox = new MyEditBox.Builder(0, 0, 120, 9).bordered(false).maxLength(200).value("")
                    .onDefocus(text -> {
                        if (!text.isBlank() && InputConstants.isKeyDown(MC.getWindow().getWindow(), GLFW.GLFW_KEY_ENTER))
                            newFile();
                    }).build();
            TextInputClientEvents.registerEditBox(newPathBox);
        }
        return newPathBox;
    }

    // a path to type (folders are made as needed), Enter or + makes it; empty + makes src/script_N
    private static Row newFileRow() {
        return (f, x, y, w) -> {
            Rect field = new Rect(x, y + 2, w, EditorRows.ROW_H - 4);
            Rect plus = new Rect(field.x() + 1, field.y(), 12, field.h());
            EditorRows.field(f, newPathBox(), new Rect(field.x() + 12, field.y(), field.w() - 12, field.h()));
            f.g.fill(field.x(), field.y(), field.x() + 12, field.bottom(), Theme.SUNKEN);
            Theme.outline(f.g, field.x(), field.y(), field.w(), field.h(), 1, newPathBox().isFocused() ? Theme.ORANGE : Theme.INK);
            Text.draw(f.g, "+", plus.x() + 3, plus.y() + 2, f.hovered(plus) ? Theme.GOLD : Theme.ORANGE);
            f.register(new ButtonBuilder("sheet new file").onLeftClick(MapSheets::newFile).build(), plus, null,
                    EditorRows.tip(I18n.get(LOGIC + "create"), I18n.get(LOGIC + "new_path_tip")));
            if (newPathBox().getValue().isEmpty() && !newPathBox().isFocused())
                Text.draw(f.g, I18n.get(LOGIC + "new_path"), field.x() + 15, field.centerY() - 4, Theme.DIM);
        };
    }

    private static void newFile() {
        String path = RonFilesClient.normalize(newPathBox().getValue());
        if (path.isEmpty() || path.endsWith("/")) {
            int k = 1;
            while (RonFilesClient.exists(path + (path.isEmpty() ? "src/" : "") + "script_" + k))
                k++;
            path = path + (path.isEmpty() ? "src/" : "") + "script_" + k;
        }
        newPathBox().setValue("");
        newPathBox().setFocused(false);
        CodeEditorPopup.open(path, path, runsWhen(path));
    }

    // every event the game fires, grouped; a typed one opens its list of types
    private static Sheet events() {
        if (TYPED.containsKey(subKey)) {
            String event = subKey;
            List<Row> rows = new ArrayList<>();
            rows.add(eventRow("events/" + event, I18n.get(LOGIC + "any"), I18n.get(LOGIC + "runs." + event), false));
            for (String type : TYPED.get(event).get())
                rows.add(eventRow("events/" + event + "/" + type, type, "", false));
            return new Sheet(event, "ron/events/" + event + "/", List.of(new Group(null, rows)), null, true, false);
        }
        List<Group> groups = new ArrayList<>();
        groups.add(new Group(null, noteRows(I18n.get(LOGIC + "events_intro"))));
        for (var group : RonEvents.GROUPS.entrySet()) {
            List<Row> rows = new ArrayList<>();
            for (String event : group.getValue())
                rows.add(eventRow("events/" + event, event, I18n.get(LOGIC + "runs." + event), TYPED.containsKey(event)));
            groups.add(new Group(I18n.get(LOGIC + "group." + group.getKey()), rows));
        }
        groups.add(new Group(I18n.get(LOGIC + "group.zones"), noteRows(I18n.get(LOGIC + "events_note"))));
        return new Sheet(I18n.get(LOGIC + "add_event"), "ron/events/", groups, null, true, false);
    }

    // a dot (green: the file exists), the name, what sets it off, then its command count, › for its types, or + on hover
    private static Row eventRow(String path, String name, String desc, boolean typed) {
        String event = path.substring("events/".length());
        return (f, x, y, w) -> {
            int h = EditorRows.ROW_H;
            Rect r = new Rect(x, y, w, h);
            boolean over = f.hovered(r);
            Integer count = RonFilesClient.files.get(path);
            long typedFiles = typed ? RonFilesClient.files.keySet().stream().filter(p -> p.startsWith(path + "/")).count() : 0;
            boolean has = count != null || typedFiles > 0;
            if (over)
                f.g.fill(x, y, x + w, y + h, Theme.HOVER);
            f.g.fill(x + 4, y + 7, x + 8, y + 11, has ? Theme.OK : Theme.FAINT);
            String state = typed ? (typedFiles > 0 ? typedFiles + " ›" : "›") : count != null ? I18n.get(LOGIC + "commands_sub", count) : over ? "+" : "";
            int sw = Text.width(state);
            Text.draw(f.g, state, x + w - 4 - sw, y + 4, count != null || typedFiles > 0 ? Theme.OK_TEXT : over ? Theme.ORANGE : Theme.BODY);
            int nameW = Math.min(Text.width(name), (w - 20) / 2);
            Text.draw(f.g, Text.fit(name, nameW), x + 13, y + 4, has || typed ? Theme.TEXT : Theme.LABEL);
            int dx = x + 13 + Math.max(nameW, Math.min(96, (w - 20) / 2)) + 6;
            if (!desc.isEmpty())
                Text.draw(f.g, Text.fit(desc, x + w - 12 - sw - dx), dx, y + 4, Theme.DIM);
            f.register(new ButtonBuilder("sheet event " + path).onLeftClick(typed ? () -> open(Sub.EVENTS, event)
                    : () -> CodeEditorPopup.open(path, path, runsWhen(path))).build(), r, null, null);
        };
    }

    public static List<String> buildingTypes() {
        List<String> types = new ArrayList<>();
        for (ResourceLocation id : ReignOfNetherRegistries.BUILDING.keySet())
            types.add(id.getPath());
        for (CustomBuilding custom : CustomBuildingClientEvents.customBuildings)
            types.add("custom_" + custom.id);
        types.sort(null);
        return types;
    }

    public static String runsWhen(String path) {
        if (path.equals("main"))
            return I18n.get(LOGIC + "runs.main");
        if (path.startsWith("events/")) {
            String event = path.substring("events/".length());
            String base = event.contains("/") ? event.substring(0, event.indexOf('/')) : event;
            if (RonEvents.ALL.contains(base))
                return I18n.get(LOGIC + "runs." + base) + (base.equals(event) ? "" : " · " + event.substring(base.length() + 1));
        }
        if (path.startsWith("buildings/"))
            return I18n.get(LOGIC + "runs.building");
        return I18n.get(LOGIC + "runs.called", path);
    }

    // the world's own ron/ folder: only on the world you're hosting
    private static void openFolder() {
        var server = MC.getSingleplayerServer();
        if (server == null)
            return;
        Path dir = RonFiles.dir(server);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            ReignOfNether.LOGGER.warn("Couldn't make {}", dir, e);
            return;
        }
        Util.getPlatform().openFile(dir.toFile());
    }

    // ------------------------------------------------------------------ rows

    record Action(String label, Runnable click) { }

    /** A paragraph wrapped to the sheet, a row per line. */
    static List<Row> noteRows(String text) {
        List<Row> rows = new ArrayList<>();
        for (FormattedCharSequence line : Text.wrap(text, TEXT_W))
            rows.add((f, x, y, w) -> Text.draw(f.g, line, x, y + 4, Theme.DESC));
        return rows;
    }

    /** A red warning strip (like Theme.banner), a row per wrapped line. */
    static List<Row> warnRows(String text) {
        return stripRows(text, Theme.ERROR);
    }

    private static List<Row> stripRows(String text, int edge) {
        List<Row> rows = new ArrayList<>();
        for (FormattedCharSequence line : Text.wrap(text, TEXT_W - 10))
            rows.add((f, x, y, w) -> {
                f.g.fill(x, y, x + w, y + EditorRows.ROW_H, (edge & 0x00FFFFFF) | 0x1A000000);
                f.g.fill(x, y, x + 2, y + EditorRows.ROW_H, edge);
                Text.draw(f.g, line, x + 7, y + 4, Theme.BODY);
            });
        return rows;
    }

    // a small-caps label with small buttons at its right end
    private static Row headerRow(String label, List<Action> actions) {
        return headerRow(label, null, actions);
    }

    // with a tooltip on the label
    private static Row headerRow(String label, @Nullable Supplier<Tooltip> tip, List<Action> actions) {
        return (f, x, y, w) -> {
            Text.small(f.g, label.toUpperCase(), x, y + 6, Theme.LABEL);
            if (tip != null)
                f.register(new ButtonBuilder("sheet header " + label).build(), new Rect(x, y, Text.width(label.toUpperCase()) + 4, EditorRows.ROW_H), null, tip);
            int rx = x + w;
            for (int i = actions.size() - 1; i >= 0; i--) {
                Action a = actions.get(i);
                int bw = Text.mediumWidth(a.label()) + 10;
                rx -= bw;
                EditorRows.textButton(f, "sheet action " + label + " " + a.label(), a.label(),
                        new Rect(rx, y + 3, bw, EditorRows.ROW_H - 5), Style.SECONDARY, a.click(), null, null);
                rx -= 3;
            }
        };
    }

    // an optional colour square with a mark in it, the name, a grey value and a button; the whole row clicks it
    private static Row listRow(String name, String value, int valueColour, int dot, String mark, String button, Runnable click) {
        return listRow(name, value, valueColour, dot, mark, button, click, click);
    }

    // the button does buttonClick instead
    private static Row listRow(String name, String value, int valueColour, int dot, String mark, String button, Runnable click, Runnable buttonClick) {
        return (f, x, y, w) -> {
            int h = EditorRows.ROW_H;
            Rect r = new Rect(x, y, w, h);
            f.g.fill(x, y + h - 1, x + w, y + h, Theme.RAISED);
            if (f.hovered(r))
                f.g.fill(x, y, x + w, y + h, Theme.HOVER);
            int tx = x + 2;
            if (dot != 0) {
                int dy = y + (h - DOT) / 2;
                Theme.outline(f.g, tx - 1, dy - 1, DOT + 2, DOT + 2, 1, Theme.INK);
                f.g.fill(tx, dy, tx + DOT, dy + DOT, dot);
                if (!mark.isEmpty())
                    Text.drawCentered(f.g, mark, tx + DOT / 2 + 1, dy + 1, Theme.TEXT);
                tx += DOT + 5;
            }
            f.register(new ButtonBuilder("sheet row " + name).onLeftClick(click).build(), r, null, null);
            int bw = Math.max(BUTTON_W, Text.mediumWidth(button) + 10);
            Rect b = new Rect(x + w - bw, y + 2, bw, h - 4);
            EditorRows.textButton(f, "sheet edit " + name, button, b, Style.SECONDARY, buttonClick, null, null);
            int valueW = value.isEmpty() ? 0 : Math.min(Text.width(value), (b.x() - tx) / 2);
            if (valueW > 0) {
                String shown = Text.fit(value, valueW);
                Text.draw(f.g, shown, b.x() - 4 - Text.width(shown), y + 4, valueColour);
            }
            Text.draw(f.g, Text.fit(name, b.x() - 6 - valueW - tx), tx, y + 4, Theme.TEXT);
        };
    }

    private static Row fieldRow(String label, MyEditBox box, String placeholder) {
        return (f, x, y, w) -> {
            Rect field = EditorRows.label(f, label, null, x, y, w, EditorRows.fieldWidth(w));
            EditorRows.field(f, box, field);
            if (box.getValue().isEmpty() && !box.isFocused())
                Text.draw(f.g, placeholder, field.x() + 3, field.centerY() - 4, Theme.DIM);
        };
    }

    private static Row textRow(String label, String value, int colour) {
        return (f, x, y, w) -> {
            int vw = Math.min(Text.width(value), w / 2);
            Rect c = EditorRows.label(f, label, null, x, y, w, vw);
            Text.draw(f.g, Text.fit(value, vw), c.x(), c.y() + 3, colour);
        };
    }

    private static Row buttonRow(String label, Style style, Runnable click) {
        return (f, x, y, w) -> EditorRows.textButton(f, "sheet " + label, label, new Rect(x, y + 2, w, EditorRows.ROW_H - 4),
                style, click, null, null);
    }

    private static Row dangerRow(String label, Runnable click) {
        return buttonRow(label, Style.DANGER, click);
    }
}
