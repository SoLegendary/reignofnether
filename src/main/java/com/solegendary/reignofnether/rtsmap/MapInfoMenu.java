package com.solegendary.reignofnether.rtsmap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.solegendary.reignofnether.editor.EditorHistory;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.panels.SettingRows;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.SettingRow;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.MyEditBox;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.ui.EditorRows;
import com.solegendary.reignofnether.ui.EditorRows.Row;
import com.solegendary.reignofnether.ui.EditorRows.Style;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/** The sandbox dock's copy of ron/map.json (seats, modes, rules, info), sent to the server on Save. */
public class MapInfoMenu {

    private static final String KEY = "sandbox.reignofnether.map.";
    private static final int VANILLA_BORDER = 59999968;
    private static final int MAX_SEND_BYTES = 30000; // a serverbound packet can't be over 32767 bytes

    private static final String[] FIELDS = {"name", "version", "authors", "description"};
    private static final MyEditBox[] boxes = new MyEditBox[FIELDS.length];

    // seat and mode names, in their rows; remade from the map on every sync
    private static final Map<RTSMapInfo.Seat, MyEditBox> seatBoxes = new HashMap<>();
    private static final Map<String, MyEditBox> modeBoxes = new HashMap<>();
    private static final List<BiConsumer<String, String>> renameHooks = new ArrayList<>();

    static {
        EditorHistory.track(MapInfoMenu.HISTORY_MAP, MapInfoMenu::snapshotMap, MapInfoMenu::restoreMap);
    }

    @Nullable
    private static RTSMapInfo edit = null;
    private static boolean dirty = false; // changed since the last sync from the server
    // the server's value of each MAP_RULES gamerule, shown for the rules map.json doesn't list (only changed ones get saved)
    private static JsonObject currentRules = new JsonObject();

    public static void reset() {
        edit = null;
        dirty = false;
        currentRules = new JsonObject();
        clearBoxes();
        TextInputClientEvents.deregisterEditBox(centreX);
        TextInputClientEvents.deregisterEditBox(centreZ);
        centreX = centreZ = null;
    }

    // a new map.json from the server: unsaved changes keep everything but the seats (start blocks may have moved them)
    static void synced(RTSMapInfo info, JsonObject current) {
        currentRules = current;
        if (edit == null || !dirty) {
            edit = info;
            clearBoxes();
        } else {
            edit.getSeats().clear();
            edit.getSeats().addAll(info.getSeats());
            clearRowBoxes();
        }
        EditorHistory.rebase(HISTORY_MAP); // not an edit: nothing to undo
    }

    private static void clearBoxes() {
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i] != null)
                TextInputClientEvents.deregisterEditBox(boxes[i]);
            boxes[i] = null;
        }
        clearRowBoxes();
    }

    // emptied before deregistering: that defocuses a typed mode name, which renames from modeBoxes
    private static void clearRowBoxes() {
        List<MyEditBox> old = new ArrayList<>(seatBoxes.values());
        old.addAll(modeBoxes.values());
        seatBoxes.clear();
        modeBoxes.clear();
        old.forEach(TextInputClientEvents::deregisterEditBox);
    }

    // placed every frame by EditorRows.field
    private static MyEditBox rowBox(String value, @Nullable Consumer<String> responder, @Nullable Consumer<String> onDefocus) {
        MyEditBox box = new MyEditBox.Builder(0, 0, 120, 9)
                .bordered(false)
                .maxLength(32)
                .value(value)
                .responder(responder)
                .onDefocus(onDefocus)
                .build();
        TextInputClientEvents.registerEditBox(box);
        return box;
    }

    public static RTSMapInfo info() {
        if (edit == null)
            edit = new RTSMapInfo();
        return edit;
    }

    private static MyEditBox box(int i) {
        if (boxes[i] == null) {
            RTSMapInfo info = info();
            String value = switch (FIELDS[i]) {
                case "name" -> info.getName();
                case "version" -> info.getVersion();
                case "authors" -> String.join(", ", info.getAuthor());
                default -> info.getDescription();
            };
            // placed every frame by EditorRows.field
            boxes[i] = new MyEditBox.Builder(0, 0, 120, 9)
                    .bordered(false)
                    .maxLength(FIELDS[i].equals("description") ? 512 : 64)
                    .value(value)
                    .responder(v -> {
                        applyField(FIELDS[i], v);
                        changed();
                    })
                    .build();
            TextInputClientEvents.registerEditBox(boxes[i]);
        }
        return boxes[i];
    }

    // typed straight into the copy, so undo sees it; save() reads the boxes again
    private static void applyField(String field, String v) {
        RTSMapInfo info = info();
        switch (field) {
            case "name" -> info.setName(v.trim());
            case "version" -> info.setVersion(v.trim());
            case "authors" -> info.setAuthors(Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
            default -> info.setDescription(v.trim());
        }
    }

    /** The name, version, authors and description fields, one row each. */
    public static List<Row> infoRows() {
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < FIELDS.length; i++) {
            int index = i;
            rows.add((f, x, y, w) -> {
                Rect field = EditorRows.label(f, I18n.get(KEY + FIELDS[index]), null, x, y, w, EditorRows.fieldWidth(w));
                EditorRows.field(f, box(index), field);
            });
        }
        return rows;
    }

    /** Unsaved changes to map.json. */
    public static boolean isDirty() {
        return dirty;
    }

    // ------------------------------------------------------------------ undo (EditorHistory)

    public static final String HISTORY_MAP = "map.json";

    public static String snapshotMap() {
        return RTSMapInfoLoader.toJson(info());
    }

    public static void restoreMap(String json) {
        try {
            edit = RTSMapInfoLoader.fromJson(json);
        } catch (Exception e) {
            return;
        }
        clearBoxes();
        dirty = true;
    }

    /** Sends map.json. */
    public static void save() {
        RTSMapInfo info = info();
        renameModes(); // a name still being typed
        info.setName(box(0).getValue().trim());
        info.setVersion(box(1).getValue().trim());
        info.setAuthors(Arrays.stream(box(2).getValue().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        info.setDescription(box(3).getValue().trim());
        String json = RTSMapInfoLoader.toJson(info);
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_SEND_BYTES) {
            HudClientEvents.showTemporaryMessage(I18n.get(KEY + "too_big"));
            return;
        }
        dirty = false; // the server sends the saved map.json back
        RTSMapInfoServerboundPacket.saveMapJson(json);
    }

    public static void changed() {
        dirty = true;
    }

    // ------------------------------------------------------------------ seats

    public static String seatName(RTSMapInfo.Seat seat) {
        return seat.name != null && !seat.name.isEmpty() ? seat.name : I18n.get(KEY + "seat", seat.id);
    }

    /** Adds a seat where the player stands; returns its id, -1 without a player. */
    public static int addSeat(RTSMapInfo info) {
        if (Minecraft.getInstance().player == null)
            return -1;
        int id = info.getSeats().stream().mapToInt(s -> s.id).max().orElse(0) + 1;
        info.getSeats().add(new RTSMapInfo.Seat(id, Minecraft.getInstance().player.blockPosition()));
        changed();
        return id;
    }

    public static void removeSeat(RTSMapInfo info, RTSMapInfo.Seat seat) {
        info.getSeats().remove(seat);
        for (List<List<Integer>> teams : info.getModes().values())
            for (List<Integer> team : teams)
                team.remove((Integer) seat.id);
        changed();
    }

    public static List<String> factionOptions() {
        List<String> options = new ArrayList<>();
        options.add(null); // the player picks
        for (ResourceLocation rl : Factions.PLAYABLE_FACTIONS)
            options.add(rl.toString());
        return options;
    }

    public static String factionName(@Nullable String faction) {
        ResourceLocation rl = faction != null ? ResourceLocation.tryParse(faction) : null;
        return rl == null ? I18n.get(KEY + "faction.any") : I18n.get(String.format("hud.faction.%s.%s", rl.getNamespace(), rl.getPath()));
    }

    // the next (or last) item, wrapping round; also marks the map changed
    public static <T> T cycle(List<T> options, T current, int step) {
        changed();
        int i = Math.max(0, options.indexOf(current));
        return options.get(Math.floorMod(i + step, options.size()));
    }

    // ------------------------------------------------------------------ modes

    // 1 based, 0 = not in this mode
    public static int teamOf(List<List<Integer>> teams, int seatId) {
        for (int i = 0; i < teams.size(); i++)
            if (teams.get(i).contains(seatId))
                return i + 1;
        return 0;
    }

    // empty teams stay while editing so the numbers don't shift; the server drops them on save
    public static void setTeam(List<List<Integer>> teams, int seatId, int team) {
        for (List<Integer> t : teams)
            t.remove((Integer) seatId);
        while (teams.size() < team)
            teams.add(new ArrayList<>());
        if (team > 0)
            teams.get(team - 1).add(seatId);
        changed();
    }

    // the seats the classic lobby offers: not played by nobody
    private static List<Integer> seatIds(RTSMapInfo info) {
        return info.getSeats().stream().filter(s -> !s.getControl().equals("none")).map(s -> s.id).sorted().toList();
    }

    private static List<List<Integer>> teams(List<Integer> ids, Function<Integer, Integer> teamOfIndex, int teamCount) {
        List<List<Integer>> teams = new ArrayList<>();
        for (int t = 0; t < teamCount; t++)
            teams.add(new ArrayList<>());
        for (int i = 0; i < ids.size(); i++) {
            int t = teamOfIndex.apply(i);
            if (t >= 0)
                teams.get(t).add(ids.get(i));
        }
        return teams;
    }

    public static final int MAX_PLAYERS = 8;

    // 1v1, 2v2, 2v2v2, 2v2v2v2... and ffa_3, ffa_4... as far as the seats go; existing modes are kept.
    // coop_<players>_<difficulty> needs a difficulty, so it's only made by hand. Officially a match has 8 players at most
    public static void autoModes(RTSMapInfo info) {
        List<Integer> ids = seatIds(info);
        int n = Math.min(ids.size(), MAX_PLAYERS);
        for (int t = 2; t <= 4; t++) {
            int teamCount = t;
            // one seat per team past two teams is a free for all, named ffa_ below
            for (int k = t == 2 ? 1 : 2; k * t <= n; k++) {
                int size = k;
                info.getModes().putIfAbsent(String.join("v", Collections.nCopies(t, String.valueOf(k))),
                        teams(ids, i -> i < size * teamCount ? i / size : -1, teamCount));
            }
        }
        for (int m = 3; m <= n; m++) {
            int players = m;
            info.getModes().putIfAbsent("ffa_" + m, teams(ids, i -> i < players ? i : -1, m));
        }
        changed();
    }

    // a free for all of every seat, to be reshaped with the team buttons (1v3, coop...); rename it in map.json
    public static String addMode(RTSMapInfo info) {
        int i = 1;
        while (info.getModes().containsKey("mode_" + i))
            i++;
        List<Integer> ids = seatIds(info);
        info.getModes().put("mode_" + i, teams(ids, index -> index, ids.size()));
        changed();
        return "mode_" + i;
    }

    /** Runs with (old name, new name) when a typed mode name is taken. */
    public static void onModeRenamed(BiConsumer<String, String> hook) {
        renameHooks.add(hook);
    }

    /** A seat's name box (empty: no name), placed by EditorRows.field. */
    public static MyEditBox seatNameBox(RTSMapInfo.Seat seat) {
        return seatBoxes.computeIfAbsent(seat, s -> rowBox(s.name == null ? "" : s.name, v -> {
            s.name = v.isBlank() ? null : v.trim();
            changed();
        }, null));
    }

    /** A mode's name box: the name is taken when it loses focus (or on save), keeping the mode's teams and default. */
    public static MyEditBox modeNameBox(String name) {
        return modeBoxes.computeIfAbsent(name, n -> rowBox(n, null, v -> renameModes()));
    }

    // what's typed in the mode name boxes; a name that's empty, "none" or taken goes back. The mode keeps its teams
    // (and stays selected), map order is kept
    private static void renameModes() {
        RTSMapInfo info = info();
        for (Map.Entry<String, MyEditBox> e : new ArrayList<>(modeBoxes.entrySet())) {
            String from = e.getKey();
            MyEditBox box = e.getValue();
            String to = box.getValue().trim();
            if (to.equals(from) || !info.getModes().containsKey(from))
                continue;
            if (to.isEmpty() || to.equalsIgnoreCase("none") || info.getModes().containsKey(to)) {
                box.setValue(from);
                continue;
            }
            Map<String, List<List<Integer>>> renamed = new LinkedHashMap<>();
            info.getModes().forEach((name, teams) -> renamed.put(name.equals(from) ? to : name, teams));
            info.getModes().clear();
            info.getModes().putAll(renamed);
            if (info.getDefaultMode().equals(from))
                info.setDefaultMode(to);
            modeBoxes.remove(from);
            modeBoxes.put(to, box);
            changed();
            renameHooks.forEach(h -> h.accept(from, to));
        }
    }

    public static void removeMode(RTSMapInfo info, String name) {
        info.getModes().remove(name);
        if (info.getDefaultMode().equals(name))
            info.setDefaultMode("none");
        changed();
    }

    // ------------------------------------------------------------------ rules

    // gamerules whose lang keys don't follow their id
    private static final Map<String, String> RULE_KEYS = Map.of("doUnitGriefing", "unit_griefing",
            "doPlayerGriefing", "player_griefing", "slantedBuilding", "slanted_buildings");

    // the gamerule's id in the lang keys and GameruleClient
    private static String snake(String id) {
        return RULE_KEYS.getOrDefault(id, id.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT));
    }

    // map.json's rules but the match rules, which are on the Match defaults tab
    private static Set<String> mapRules() {
        Set<String> ids = new LinkedHashSet<>(info().getRules().keySet());
        ids.addAll(currentRules.keySet());
        ids.removeAll(RTSMapInfo.MATCH_RULES);
        return ids;
    }

    /**
     * A match rule's default on this map: map.json's value, else the live one, with a lock (map.json lockedRules) that
     * keeps the lobby host from changing it, and x to take both out of map.json.
     */
    public static Row matchDefaultRow(GameruleClient.Rule live) {
        RTSMapInfo info = info();
        String id = live.mapId();
        SettingRow row = live.withValue(() -> {
            JsonPrimitive saved = info.getRules().get(id);
            return saved == null ? live.get().get() : saved.isBoolean() ? (saved.getAsBoolean() ? 1 : 0) : saved.getAsInt();
        }, v -> {
            info.getRules().put(id, live.isToggle() ? new JsonPrimitive(v == 1) : new JsonPrimitive(v));
            changed();
        }).toRow(() -> true, "");
        return (f, x, y, w) -> {
            int bw = EditorRows.ROW_H - 4;
            boolean saved = info.getRules().containsKey(id) || info.getLockedRules().contains(id);
            boolean locked = info.getLockedRules().contains(id);
            int rw = w - bw - 2 - (saved ? bw + 2 : 0);
            SettingRows.renderRow(f, row, x, y, rw);
            Rect lock = new Rect(x + rw + 2, y + 2, bw, bw);
            Theme.button(f.g, lock.x(), lock.y(), bw, bw, f.hovered(lock));
            (locked ? Icon.LOCK : Icon.UNLOCK).drawIn(f.g, lock.x(), lock.y(), bw, bw, locked ? Theme.GOLD : Theme.FAINT);
            f.register(new ButtonBuilder("rule lock " + id).onLeftClick(() -> {
                if (!info.getLockedRules().remove(id)) {
                    info.getLockedRules().add(id);
                    info.getRules().putIfAbsent(id, live.isToggle() ? new JsonPrimitive(live.get().get() == 1) : new JsonPrimitive(live.get().get()));
                }
                changed();
            }).build(), lock, null, EditorRows.tip(I18n.get(KEY + (locked ? "rule_locked" : "rule_unlocked")), I18n.get(KEY + "rule_lock_tooltip")));
            if (saved)
                EditorRows.textButton(f, "rule clear " + id, "x", new Rect(x + w - bw, y + 2, bw, bw), Style.DANGER, () -> {
                    info.getRules().remove(id);
                    info.getLockedRules().remove(id);
                    changed();
                }, null, EditorRows.tip(row.name(), I18n.get(KEY + "rule_clear_tooltip")));
        };
    }

    /** A row per rule map.json can hold: its saved value, else the gamerule's current one. */
    public static List<Row> ruleRows() {
        RTSMapInfo info = info();
        List<Row> rows = new ArrayList<>();
        for (String id : mapRules()) {
            String snake = snake(id);
            String name = I18n.exists("gamerule.reignofnether.name." + snake) ? I18n.get("gamerule.reignofnether.name." + snake) : id;
            String desc = I18n.exists("commands.reignofnether.gamerule." + snake) ? I18n.get("commands.reignofnether.gamerule." + snake) : id;
            Consumer<JsonPrimitive> set = v -> {
                info.getRules().put(id, v);
                changed();
            };
            rows.add((f, x, y, w) -> {
                JsonPrimitive saved = info.getRules().get(id);
                JsonElement shown = saved != null ? saved : currentRules.get(id);
                if (shown == null || !shown.isJsonPrimitive())
                    return; // taken out of map.json and not a MAP_RULES gamerule: gone after the save
                JsonPrimitive value = shown.getAsJsonPrimitive();
                int clearW = EditorRows.ROW_H - 4;
                if (saved != null) // in map.json: x takes it out again (the gamerule keeps its value)
                    EditorRows.textButton(f, "rule clear " + id, "x", new Rect(x + w - clearW, y + 2, clearW, clearW), Style.DANGER, () -> {
                        info.getRules().remove(id);
                        changed();
                    }, null, EditorRows.tip(name, I18n.get(KEY + "rule_clear_tooltip")));
                int cw = w - clearW - 2;
                if (value.isBoolean())
                    EditorRows.toggle(f, name, EditorRows.tip(name, desc), value.getAsBoolean(),
                            () -> set.accept(new JsonPrimitive(!value.getAsBoolean())), x, y, cw);
                else if (value.isNumber())
                    EditorRows.stepper(f, name, EditorRows.tip(name, desc), value.getAsString(),
                            () -> set.accept(new JsonPrimitive(value.getAsInt() + (Keybindings.shiftMod.isDown() ? 10 : 1))),
                            () -> set.accept(new JsonPrimitive(value.getAsInt() - (Keybindings.shiftMod.isDown() ? 10 : 1))), x, y, cw);
                else // only by hand in map.json
                    EditorRows.label(f, name + ": " + value.getAsString(), EditorRows.tip(name, desc), x, y, cw, 0);
            });
        }
        return rows;
    }

    // ------------------------------------------------------------------ world border

    public static int borderSize() {
        var level = Minecraft.getInstance().level;
        return level == null ? VANILLA_BORDER : (int) level.getWorldBorder().getSize();
    }

    // the middle of the seats (map.json seats or start blocks), else where the player stands: where a border goes
    private static int[] seatsCentre() {
        var poses = StartPosClientEvents.startPoses;
        if (poses.isEmpty()) {
            var player = Minecraft.getInstance().player;
            return player == null ? new int[] { 0, 0 } : new int[] { player.getBlockX(), player.getBlockZ() };
        }
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (var sp : poses) {
            minX = Math.min(minX, sp.pos.getX());
            maxX = Math.max(maxX, sp.pos.getX());
            minZ = Math.min(minZ, sp.pos.getZ());
            maxZ = Math.max(maxZ, sp.pos.getZ());
        }
        return new int[] { (minX + maxX) / 2, (minZ + maxZ) / 2 };
    }

    /** A border of this size centred on the seats, so the whole map is inside it. */
    public static void fitBorder(int size) {
        int[] c = seatsCentre();
        RTSMapInfoServerboundPacket.setBorderCenter(c[0], c[1]);
        RTSMapInfoServerboundPacket.setBorderSize(size);
    }

    private static MyEditBox centreX = null, centreZ = null;

    // shows the border's centre; a number typed in is sent when you leave the field
    private static MyEditBox centreBox(boolean isX) {
        MyEditBox box = isX ? centreX : centreZ;
        if (box == null) {
            box = rowBox("", null, text -> {
                var level = Minecraft.getInstance().level;
                if (level == null || !text.strip().matches("-?\\d{1,8}"))
                    return;
                int v = Integer.parseInt(text.strip());
                int cx = (int) level.getWorldBorder().getCenterX(), cz = (int) level.getWorldBorder().getCenterZ();
                RTSMapInfoServerboundPacket.setBorderCenter(isX ? v : cx, isX ? cz : v);
            });
            if (isX) centreX = box; else centreZ = box;
        }
        var level = Minecraft.getInstance().level;
        if (!box.isFocused() && level != null)
            box.setValue(String.valueOf((int) (isX ? level.getWorldBorder().getCenterX() : level.getWorldBorder().getCenterZ())));
        return box;
    }

    /** Centre X and Z you can type. */
    public static List<Row> borderCentreRows() {
        return List.of(centreRow(true), centreRow(false));
    }

    private static Row centreRow(boolean isX) {
        String name = I18n.get(KEY + (isX ? "border_centre_x" : "border_centre_z"));
        return (f, x, y, w) -> {
            Rect field = EditorRows.label(f, name, EditorRows.tip(name, I18n.get(KEY + "border_centre_tooltip")), x, y, w, EditorRows.fieldWidth(w));
            EditorRows.field(f, centreBox(isX), field);
        };
    }

    /** The world border's size, which the server changes at once (not on Save). */
    public static Row borderRow() {
        String size = I18n.get(KEY + "border_size");
        return (f, x, y, w) -> EditorRows.stepper(f, size, EditorRows.tip(size, I18n.get(KEY + "border_size_tooltip")), String.valueOf(borderSize()),
                () -> RTSMapInfoServerboundPacket.setBorderSize(borderSize() + (Keybindings.shiftMod.isDown() ? 128 : 16)),
                () -> RTSMapInfoServerboundPacket.setBorderSize(Math.max(16, borderSize() - (Keybindings.shiftMod.isDown() ? 128 : 16))), x, y, w);
    }
}
