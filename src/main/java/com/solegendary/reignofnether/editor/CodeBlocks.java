package com.solegendary.reignofnether.editor;

import com.mojang.blaze3d.platform.InputConstants;
import com.solegendary.reignofnether.commands.rtsapi.GuideCommands;
import com.solegendary.reignofnether.commands.rtsapi.RonCommands;
import com.solegendary.reignofnether.hud.MyEditBox;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.rtsmap.RonEvents;
import com.solegendary.reignofnether.rtsmap.RonFilesClient;
import com.solegendary.reignofnether.sandbox.MapSheets;
import com.solegendary.reignofnether.ui.Clip;
import com.solegendary.reignofnether.ui.CodeEditor;
import com.solegendary.reignofnether.ui.EditorRows;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The code popup's Blocks view: each line of the file as a block with its values in fields, read from the text and
 * written straight back to it, so the Code view and a hand edit always see the same file. A line no block knows
 * stays a Command block holding its text. Fields with a world or HUD target pick it with CodePicker.
 */
public final class CodeBlocks {

    private static final String KEY = "editor.reignofnether.blocks.";
    private static final int ROW = 13;
    private static final int CHIP_H = 11;
    private static final int PAD = 4;
    private static final int TOOLS_W = 52; // ▲ ▼ ⧉ ≡ ✕ at a row's right
    private static final int INDENT = 30; // an if's command under it, after "then"
    private static final int MAX_OPTIONS = 10;

    enum Cat {
        DIALOGUE(0xFFB07CFF), HUD(0xFFFFB347), CAMERA(0xFF5CC8FF), WORLD(0xFF8FE03C), PLAYER(0xFFF2C94C),
        IF(0xFFFF6A9A), VAR(0xFFC7A6FF), GOAL(0xFF7FD1B9), WAIT(0xFFF19A0F), FLOW(0xFF4D9BFF), OTHER(0xFF9498B5);

        final int colour;

        Cat(int colour) {
            this.colour = colour;
        }
    }

    // how a field reads its tokens: one word, a word that may be "quoted", the rest of the line, x y z, two corners,
    // a unit or building, a HUD target (button "name" or a region), or a whole command (an if's then)
    enum Kind { WORD, NUM, QUOTED, REST, OPT_REST, POS, CAMERA, BOX, UNIT, BUILDING, HUD, LINE }

    record Slot(String name, Kind kind, String options, String fallback) { }

    // a template is the command with {name:kind[:options][=default]} where its values go
    record Type(String id, Cat cat, String template, List<Object> parts) {
        Type(String id, Cat cat, String template) {
            this(id, cat, template, compile(template));
        }
    }

    // a {slot} is one token even when its default has spaces ({line:rest=say hi})
    private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\{[^}]*}|\\S+");
    private static final Pattern SLOT = Pattern.compile("\\{(\\w+):(\\w+)(?::(\\w+))?(?:=([^}]*))?}");

    private static List<Object> compile(String template) {
        List<Object> parts = new ArrayList<>();
        Matcher tok = TEMPLATE_TOKEN.matcher(template);
        while (tok.find()) {
            String token = tok.group();
            Matcher m = SLOT.matcher(token);
            parts.add(m.matches() ? new Slot(m.group(1), Kind.valueOf(m.group(2).toUpperCase(Locale.ROOT)),
                    m.group(3) == null ? "" : m.group(3), m.group(4) == null ? "" : m.group(4)) : token);
        }
        return parts;
    }

    private static final String WHO = "{who:word:player=@a}";
    private static final String OWNER = "{who:word:player=$owner}";
    private static final String THEN = "{then:line=rtsapi say @a none Guide Done!}";

    // most specific first: the first one that reads the whole line is the line's block
    static final List<Type> TYPES = List.of(
            new Type("say_next", Cat.DIALOGUE, "rtsapi say " + WHO + " next"),
            new Type("say_clear", Cat.DIALOGUE, "rtsapi say " + WHO + " clear"),
            new Type("say_wait", Cat.DIALOGUE, "rtsapi say " + WHO + " {head:word:head=none} {name:quoted=Guide} wait {text:rest=Do this first}"),
            new Type("say", Cat.DIALOGUE, "rtsapi say " + WHO + " {head:word:head=none} {name:quoted=Guide} {text:rest=Hello!}"),
            new Type("hl_clear", Cat.HUD, "rtsapi highlight " + WHO + " clear"),
            new Type("hl_unit", Cat.HUD, "rtsapi highlight " + WHO + " unit {unit:unit=@e[limit=1]} {text:opt_rest}"),
            new Type("hl_building", Cat.HUD, "rtsapi highlight " + WHO + " building {building:building=@b[sort=nearest,limit=1]} {text:opt_rest}"),
            new Type("hl_pos", Cat.HUD, "rtsapi highlight " + WHO + " pos {pos:pos=~ ~ ~} {text:opt_rest}"),
            new Type("hl_hud", Cat.HUD, "rtsapi highlight " + WHO + " {target:hud=minimap} {text:opt_rest}"),
            new Type("cam_move", Cat.CAMERA, "rtsapi player camera move {pos:camera=~ ~ ~} {who:word:player=@p} {lock:num=0} {pan:num=20} {zoom:num=40}"),
            new Type("cam_shake", Cat.CAMERA, "rtsapi player camera shake " + WHO + " {strength:num=5} {ticks:num=20}"),
            new Type("control", Cat.CAMERA, "rtsapi player control " + WHO + " {on:word:bool=false}"),
            new Type("zone_add", Cat.WORLD, "rtsapi zone add {name:word=zone_1} {box:box=~ ~ ~ ~ ~ ~}"),
            new Type("zone_remove", Cat.WORLD, "rtsapi zone remove {name:word:zone=zone_1}"),
            new Type("summon", Cat.WORLD, "rtsapi unit summon {unit:word:unitid} " + OWNER + " {pos:pos=~ ~ ~}"),
            new Type("res_add", Cat.PLAYER, "rtsapi player resources add {res:word:res=food} {amount:num=100} " + OWNER),
            new Type("res_remove", Cat.PLAYER, "rtsapi player resources remove {res:word:res=food} {amount:num=100} " + OWNER),
            new Type("res_set", Cat.PLAYER, "rtsapi player resources set {res:word:res=food} {amount:num=100} " + OWNER),
            new Type("victory", Cat.PLAYER, "rtsapi player victory " + OWNER + " {reason:opt_rest}"),
            new Type("defeat", Cat.PLAYER, "rtsapi player defeat " + OWNER + " {reason:opt_rest}"),
            new Type("ban", Cat.PLAYER, "rtsapi restrict {who:word:owner=*} deny {id:word:ids}"),
            new Type("allow", Cat.PLAYER, "rtsapi restrict {who:word:owner=*} allow {id:word:ids}"),
            new Type("if_res", Cat.IF, "execute {mode:word:ifunless=if} rts resources {res:word:res=food} {amount:num=100} " + OWNER + " run " + THEN),
            new Type("if_units", Cat.IF, "execute {mode:word:ifunless=if} rts units {amount:num=5} " + OWNER + " run " + THEN),
            new Type("if_buildings", Cat.IF, "execute {mode:word:ifunless=if} rts buildings {amount:num=3} " + OWNER + " run " + THEN),
            new Type("if_var", Cat.IF, "execute {mode:word:ifunless=if} score {who:word:holder=#map} {var:word:var=wave} matches {range:word=1..} run " + THEN),
            new Type("var_set", Cat.VAR, "scoreboard players set {who:word:holder=#map} {var:word:var=wave} {value:num=0}"),
            new Type("var_add", Cat.VAR, "scoreboard players add {who:word:holder=#map} {var:word:var=wave} {value:num=1}"),
            new Type("var_remove", Cat.VAR, "scoreboard players remove {who:word:holder=#map} {var:word:var=wave} {value:num=1}"),
            new Type("obj_add", Cat.GOAL, "rtsapi objective add {objective:word:objective=castle} {goal:num=1} " + WHO + " {text:rest=Destroy the castle}"),
            new Type("obj_progress", Cat.GOAL, "rtsapi objective progress {objective:word:objective=castle} {amount:num=1}"),
            new Type("obj_complete", Cat.GOAL, "rtsapi objective complete {objective:word:objective=castle}"),
            new Type("timer", Cat.GOAL, "rtsapi timer {timer:word=wave} {time:word=60s} {title:quoted=Next wave in} {file:rest:file}"),
            new Type("if_zone", Cat.IF, "execute {mode:word:ifunless=if} rts zone {zone:word:zone=zone_1} " + OWNER + " run " + THEN),
            new Type("wait_until", Cat.WAIT, "rtsapi wait until {cond:rest=score #map wave matches 3..}"),
            new Type("wait", Cat.WAIT, "rtsapi wait {time:word=3s}"),
            new Type("wait_for", Cat.WAIT, "rtsapi wait for {event:rest:event=unit_selected}"),
            new Type("run", Cat.FLOW, "rtsapi run {file:rest:file}"),
            new Type("schedule", Cat.FLOW, "rtsapi schedule {time:word=1s} {file:rest:file}"),
            new Type("note", Cat.OTHER, "# {text:opt_rest}"),
            new Type("command", Cat.OTHER, "{line:rest=say hi}"));

    private static final Type NOTE = TYPES.get(TYPES.size() - 2);
    private static final Type COMMAND = TYPES.get(TYPES.size() - 1);

    static final class Block {
        Type type;
        final Map<String, String> values = new LinkedHashMap<>();
        @Nullable Block then = null; // an if's command, kept in step with its "then" value

        Block(Type type) {
            this.type = type;
        }

        String write() {
            if (type == NOTE)
                return ("# " + values.getOrDefault("text", "")).strip();
            StringBuilder out = new StringBuilder();
            for (Object part : type.parts()) {
                String token;
                if (part instanceof Slot s) {
                    token = s.kind() == Kind.LINE && then != null ? then.write() : values.getOrDefault(s.name(), "");
                    if (s.kind() == Kind.QUOTED && !token.startsWith("\"") && (token.isEmpty() || token.matches(".*[\\s\"].*")))
                        token = '"' + token.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
                    if (token.isEmpty())
                        continue;
                } else
                    token = (String) part;
                if (!out.isEmpty())
                    out.append(' ');
                out.append(token);
            }
            return out.toString();
        }
    }

    // ------------------------------------------------------------------ reading lines

    record Token(String text, int start) { }

    // split on spaces, a "quoted string" (with \" inside) staying one token
    private static List<Token> tokens(String line) {
        List<Token> out = new ArrayList<>();
        int i = 0, n = line.length();
        while (i < n) {
            while (i < n && line.charAt(i) == ' ')
                i++;
            if (i >= n)
                break;
            int start = i;
            if (line.charAt(i) == '"') {
                i++;
                while (i < n && line.charAt(i) != '"')
                    i += line.charAt(i) == '\\' ? 2 : 1;
                i = Math.min(n, i + 1);
            }
            while (i < n && line.charAt(i) != ' ')
                i++;
            out.add(new Token(line.substring(start, i), start));
        }
        return out;
    }

    static Block parse(String line) {
        String s = line.strip();
        if (s.startsWith("#")) {
            Block b = new Block(NOTE);
            b.values.put("text", s.substring(1).strip());
            return b;
        }
        List<Token> tokens = tokens(s);
        for (Type type : TYPES) {
            if (type == NOTE)
                continue;
            Block b = read(type, s, tokens);
            if (b != null)
                return b;
        }
        Block b = new Block(COMMAND);
        b.values.put("line", s);
        return b;
    }

    @Nullable
    private static Block read(Type type, String line, List<Token> tokens) {
        Block b = new Block(type);
        int t = 0;
        for (Object part : type.parts()) {
            if (part instanceof String literal) {
                if (t >= tokens.size() || !tokens.get(t).text().equals(literal))
                    return null;
                t++;
                continue;
            }
            Slot s = (Slot) part;
            int take = switch (s.kind()) {
                case POS, CAMERA -> 3;
                case BOX -> 6;
                case HUD -> t < tokens.size() && tokens.get(t).text().equals("button") ? 2
                        : t < tokens.size() && GuideCommands.REGIONS.contains(tokens.get(t).text()) ? 1 : -1;
                case REST, LINE -> t < tokens.size() ? tokens.size() - t : -1;
                case OPT_REST -> tokens.size() - t;
                default -> 1;
            };
            if (take < 0 || t + take > tokens.size())
                return null;
            String value;
            if (take == 0)
                value = "";
            else if (s.kind() == Kind.REST || s.kind() == Kind.LINE || s.kind() == Kind.OPT_REST)
                value = line.substring(tokens.get(t).start()).strip();
            else
                value = String.join(" ", tokens.subList(t, t + take).stream().map(Token::text).toList());
            if (s.kind() == Kind.QUOTED && value.length() >= 2 && value.startsWith("\"") && value.endsWith("\""))
                value = value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
            b.values.put(s.name(), value);
            if (s.kind() == Kind.LINE)
                b.then = parse(value);
            t += take;
        }
        return t == tokens.size() ? b : null;
    }

    static Block create(Type type) {
        Block b = new Block(type);
        for (Object part : type.parts())
            if (part instanceof Slot s) {
                String value = s.fallback();
                if (value.isEmpty() && !s.options().isEmpty()) {
                    List<String> options = options(s.options());
                    value = options.isEmpty() ? "" : options.get(0);
                }
                b.values.put(s.name(), value);
                if (s.kind() == Kind.LINE)
                    b.then = parse(value);
            }
        return b;
    }

    // what a field's dropdown offers
    private static List<String> options(String key) {
        return switch (key) {
            case "player" -> withFirst(List.of("@a", "@p", "$owner"), CodeEditor.ownerIds());
            case "owner" -> withFirst(List.of("*", "$owner"), CodeEditor.ownerIds());
            case "head" -> withFirst(List.of("none"), CodeEditor.unitIds());
            case "res" -> List.of("food", "wood", "ore");
            case "bool" -> List.of("true", "false");
            case "ifunless" -> List.of("if", "unless");
            case "unitid" -> CodeEditor.unitIds();
            case "ids" -> withFirst(CodeEditor.unitIds(), withFirst(CodeEditor.researchIds(), MapSheets.buildingTypes()));
            case "file" -> RonFilesClient.files.keySet().stream().filter(p -> !p.contains(".")).sorted().toList();
            case "zone" -> zoneNames();
            case "holder" -> withFirst(List.of("#map", "$owner", "@a", "@s"), CodeEditor.ownerIds());
            case "var" -> varNames();
            case "objective" -> blocks.stream().filter(b -> b.type.id().equals("obj_add")).map(b -> b.values.getOrDefault("objective", "")).toList();
            case "region" -> GuideCommands.REGIONS;
            case "event" -> events();
            default -> List.of();
        };
    }

    private static List<String> withFirst(List<String> first, List<String> rest) {
        List<String> out = new ArrayList<>(first);
        rest.stream().filter(s -> !out.contains(s)).forEach(out::add);
        return out;
    }

    // the zones this file adds
    private static List<String> zoneNames() {
        List<String> names = new ArrayList<>();
        for (Block b : blocks)
            if (b.type.id().equals("zone_add"))
                names.add(b.values.getOrDefault("name", ""));
        return names;
    }

    private static final Pattern SCORE_USE = Pattern.compile("(?:scoreboard players \\w+|\\bscore) \\S+ ([A-Za-z0-9_.+-]{1,16})(?:\\s|$)");

    // the variables (scoreboards) this file uses
    private static List<String> varNames() {
        Set<String> names = new TreeSet<>();
        for (Block b : blocks) {
            Matcher m = SCORE_USE.matcher(b.write());
            while (m.find())
                names.add(m.group(1));
        }
        return new ArrayList<>(names);
    }

    // what wait for can wait for: each event, then its typed ones (unit_trained/pillager, zone_entered/<zone>)
    private static List<String> events() {
        List<String> out = new ArrayList<>(RonEvents.WAITABLE.stream().filter(e -> !e.startsWith("zone_")).toList());
        for (String zone : zoneNames()) {
            out.add("zone_entered/" + zone);
            out.add("zone_left/" + zone);
        }
        MapSheets.typedEvents().forEach((event, types) -> types.get().forEach(t -> out.add(event + "/" + t)));
        return out;
    }

    // the zones the open file adds, as boxes in the world (ones with ~ coordinates have no fixed place)
    static List<AABB> zoneBoxes() {
        List<AABB> out = new ArrayList<>();
        for (Block b : blocks) {
            if (!b.type.id().equals("zone_add"))
                continue;
            String[] p = b.values.getOrDefault("box", "").split(" ");
            try {
                out.add(new AABB(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                        Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5])).expandTowards(1, 1, 1));
            } catch (RuntimeException ignored) { }
        }
        return out;
    }

    // ------------------------------------------------------------------ state

    private static List<Block> blocks = new ArrayList<>();
    @Nullable private static String parsedFrom = null;
    private static int selected = -1;
    private static boolean thenSelected = false; // the selected block's then, which a palette pick replaces
    private static int scroll = 0;
    private static int maxScroll = 0;
    private static Rect listRect = Rect.EMPTY;

    // the field being typed in or picked from
    @Nullable private static Block editBlock = null;
    @Nullable private static Slot editSlot = null;
    private static Rect editRect = Rect.EMPTY;
    private static List<String> editOptions = List.of();
    private static int optionScroll = 0;
    private static Rect optionsRect = Rect.EMPTY;
    private static final MyEditBox box = new MyEditBox.Builder(0, 0, 100, 9).bordered(false).maxLength(500).value("").build();

    static {
        TextInputClientEvents.registerEditBox(box);
        box.onDefocus = text -> {
            // Enter commits; a click elsewhere commits a plain field but closes a list without picking
            boolean enter = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_ENTER);
            if (editBlock != null && editSlot != null && (enter || editOptions.isEmpty()))
                set(editBlock, editSlot, enter ? bestOption(text) : text);
            stopEditing();
        };
        WorldState.onClientReset(() -> {
            blocks = new ArrayList<>();
            parsedFrom = null;
            stopEditing();
        });
    }

    private CodeBlocks() { }

    private static CodeEditor code;

    // the file's text changed (a typed edit in the Code view, a save from elsewhere): read it again
    static void sync(CodeEditor editor) {
        code = editor;
        String text = editor.getText();
        if (text.equals(parsedFrom))
            return;
        parsedFrom = text;
        blocks = new ArrayList<>();
        rawShown.clear();
        if (!text.isEmpty())
            for (String line : text.split("\n", -1))
                blocks.add(parse(line));
        if (selected >= blocks.size())
            selected = blocks.size() - 1;
    }

    // every change goes straight to the text
    private static void changed() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < blocks.size(); i++) {
            if (i > 0)
                out.append('\n');
            out.append(blocks.get(i).write());
        }
        parsedFrom = out.toString();
        code.setText(parsedFrom);
    }

    private static void set(Block b, Slot s, String value) {
        if (s.name().startsWith("tok:")) { // a word of a raw line: empty takes it out, the one past the end adds it
            List<String> words = new ArrayList<>(tokens(b.write()).stream().map(Token::text).toList());
            int n = Integer.parseInt(s.name().substring(4));
            String v = value.strip();
            if (n < words.size()) {
                if (v.isEmpty())
                    words.remove(n);
                else
                    words.set(n, v);
            } else if (!v.isEmpty())
                words.add(v);
            Block now = parse(String.join(" ", words));
            boolean raw = rawShown.remove(b);
            int at = blocks.indexOf(b);
            if (at >= 0) {
                blocks.set(at, now);
                if (raw || now.type != COMMAND && b.type == COMMAND) // a line that now reads as a block stays shown as words
                    rawShown.add(now);
            } else
                for (Block p : blocks)
                    if (p.then == b) {
                        p.then = now;
                        p.values.put("then", now.write());
                    }
            changed();
            return;
        }
        b.values.put(s.name(), value.strip());
        if (s.kind() == Kind.LINE)
            b.then = parse(value);
        changed();
    }

    // what Enter in a list means: the option typed, else the first one with the typing in it, else the typing itself
    private static String bestOption(String typed) {
        String t = typed.strip().toLowerCase(Locale.ROOT);
        if (t.isEmpty() || editOptions.isEmpty())
            return typed;
        return editOptions.stream().filter(o -> o.equalsIgnoreCase(t)).findFirst()
                .or(() -> editOptions.stream().filter(o -> o.toLowerCase(Locale.ROOT).contains(t)).findFirst()).orElse(typed);
    }

    private static void stopEditing() {
        editBlock = null;
        editSlot = null;
        editOptions = List.of();
        optionsRect = Rect.EMPTY;
        box.setFocused(false);
    }

    static boolean escape() {
        if (addOpen) {
            addOpen = false;
            search.setFocused(false);
            return true;
        }
        if (editBlock == null)
            return false;
        editOptions = List.of(); // so the defocus doesn't commit
        stopEditing();
        return true;
    }

    // ------------------------------------------------------------------ drawing

    // known lines shown as their raw words anyway (≡ on the row), so anything a block hides can still be changed
    private static final Set<Block> rawShown = Collections.newSetFromMap(new IdentityHashMap<>());

    private static boolean isRaw(Block b) {
        return b.type == COMMAND || rawShown.contains(b);
    }

    static void render(HudFrame f, CodeEditor editor, Rect list) {
        sync(editor);
        renderList(f, list);
        renderAdd(f);
        renderEditing(f);
    }

    private static int gutterW() {
        return Math.max(16, Text.width(String.valueOf(blocks.size())) + 8);
    }

    private static void renderList(HudFrame f, Rect r) {
        listRect = r;
        Theme.outline(f.g, r.x(), r.y(), r.w(), r.h(), 1, Theme.INK);
        f.g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, Theme.SUNKEN);
        int gw = gutterW();
        f.g.fill(r.x() + 1, r.y() + 1, r.x() + 1 + gw, r.bottom() - 1, Theme.EMPTY);
        f.zone(r);
        Rect rows = new Rect(r.x() + 2 + gw, r.y(), r.w() - gw - 3, r.h());
        Map<Integer, String> problems = RonFilesClient.problems.getOrDefault(CodeEditorPopup.path(), Map.of());

        int total = PAD + ROW + PAD;
        for (int i = 0; i < blocks.size(); i++)
            total += height(blocks.get(i), rows.w(), problems.get(i + 1));
        maxScroll = Math.max(0, total - r.h());
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        Clip.push(f.g, r.x() + 1, r.y() + 1, r.w() - 2, r.h() - 2);
        int y = r.y() + PAD - scroll;
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            String problem = problems.get(i + 1);
            int h = height(b, rows.w(), problem);
            if (y + h > r.y() && y < r.bottom()) {
                String n = String.valueOf(i + 1);
                Text.draw(f.g, n, r.x() + gw - 3 - Text.width(n), y + 3, problem != null ? Theme.ERROR : i == selected ? Theme.LABEL : Theme.FAINT);
                drawRow(f, b, i, new Rect(rows.x(), y, rows.w(), h), problem);
            }
            y += h;
        }
        // + Add block: after the selected line, else at the end
        String label = "+ " + (selected >= 0 && selected < blocks.size() && !thenSelected
                ? I18n.get(KEY + "add_after", selected + 1) : I18n.get(KEY + "add"));
        Rect add = new Rect(rows.x() + 4, y + 2, Text.width(label) + 8, ROW);
        boolean over = f.hovered(add) && listRect.contains(f.mouseX, f.mouseY);
        Text.draw(f.g, label, add.x() + 2, add.y() + 2, over || (addOpen && !addReplace) ? Theme.GOLD : Theme.ORANGE);
        listClick(f, "block add", add, () -> openAdd(add, false), EditorRows.tip(I18n.get(KEY + "add"), I18n.get(KEY + "add_tip")));
        Clip.pop(f.g);
        if (maxScroll > 0) {
            int barH = Math.max(12, r.h() * r.h() / total);
            int barY = r.y() + (r.h() - barH) * scroll / maxScroll;
            f.g.fill(r.right() - 3, barY, r.right() - 1, barY + barH, Theme.FAINT);
        }
    }

    private static boolean isGap(Block b) {
        return b.type == COMMAND && b.values.getOrDefault("line", "").isEmpty();
    }

    private static final int DOT_W = 10; // the category dot and the gap after it
    private static final int MSG_H = 10; // a problem's line under its row

    private static int height(Block b, int w, @Nullable String problem) {
        if (isGap(b))
            return 8;
        int h = layout(b, w - DOT_W - TOOLS_W, 0, 0, null, problem) + 4;
        if (problem != null)
            h += MSG_H;
        if (b.then != null)
            h += layout(b.then, w - DOT_W - TOOLS_W - INDENT, 0, 0, null, null) + 2;
        return h;
    }

    private static void drawRow(HudFrame f, Block b, int index, Rect r, @Nullable String problem) {
        boolean isSelected = index == selected;
        boolean hovered = listRect.contains(f.mouseX, f.mouseY) && f.hovered(r);
        Runnable select = () -> {
            selected = index;
            thenSelected = false;
        };
        if (isGap(b)) { // a blank line: just a gap, selectable and deletable like any line
            if (isSelected || hovered)
                f.g.fill(r.x(), r.y(), r.right(), r.bottom(), isSelected ? Theme.RAISED : Theme.HOVER);
            listClick(f, "block " + index, r, select, EditorRows.tip(I18n.get(KEY + "gap"), null));
            if (isSelected || hovered)
                tools(f, index, r, false);
            return;
        }
        if (problem != null)
            f.g.fill(r.x(), r.y(), r.right(), r.bottom(), 0x1AFF6A4D);
        if (isSelected && !thenSelected) {
            f.g.fill(r.x(), r.y(), r.right(), r.bottom(), Theme.RAISED);
            f.g.fill(r.x(), r.y(), r.x() + 1, r.bottom(), Theme.ORANGE);
        } else if (hovered)
            f.g.fill(r.x(), r.y(), r.right(), r.bottom(), Theme.HOVER);
        f.g.fill(r.x() + 3, r.y() + 6, r.x() + 7, r.y() + 10, b.type.cat().colour);
        listClick(f, "block " + index, r, select, null);
        int tx = r.x() + DOT_W, w = r.w() - DOT_W - TOOLS_W;
        int bodyH = layout(b, w, tx, r.y() + 2, f, problem) + 4;
        int y = r.y() + bodyH;
        if (problem != null) {
            Text.draw(f.g, Text.fit(CodeEditor.friendly(problem), w), tx, y, Theme.DANGER_TEXT);
            y += MSG_H;
        }
        if (b.then != null) { // an if's command, under it after "then"
            Rect child = new Rect(tx, y - 1, r.right() - tx - TOOLS_W, r.bottom() - y + 1);
            boolean sel = isSelected && thenSelected;
            if (sel) {
                f.g.fill(child.x(), child.y(), child.right(), child.bottom(), Theme.ACTIVE);
                f.g.fill(child.x(), child.y(), child.x() + 1, child.bottom(), Theme.ORANGE);
            }
            Text.draw(f.g, I18n.get(KEY + "then"), tx + 2, y + 2, Theme.DIM);
            f.g.fill(tx + INDENT - 8, y + 5, tx + INDENT - 4, y + 9, b.then.type.cat().colour);
            listClick(f, "block then " + index, new Rect(child.x(), child.y(), INDENT, child.h()), () -> {
                selected = index;
                thenSelected = true;
                openAdd(new Rect(child.x(), child.y(), child.w(), child.h()), true);
            }, EditorRows.tip(I18n.get(KEY + "then_tip"), null));
            layout(b.then, w - INDENT, tx + INDENT, y + 1, f, null);
        }
        if (isSelected || hovered)
            tools(f, index, r, true);
    }

    // ▲ ▼ ⧉ ≡ ✕ at the row's right, on the selected or hovered row only
    private static void tools(HudFrame f, int index, Rect r, boolean all) {
        Block b = blocks.get(index);
        String[] icons = { "▲", "▼", "⧉", "≡", "✕" };
        String[] keys = { "up", "down", "copy", rawShown.contains(b) ? "as_block" : "as_text", "delete" };
        Runnable[] actions = {
                () -> move(index, -1), () -> move(index, 1), () -> {
                    blocks.add(index + 1, parse(b.write()));
                    selected = index + 1;
                    changed();
                }, () -> {
                    if (!rawShown.remove(b))
                        rawShown.add(b);
                }, () -> {
                    blocks.remove(index);
                    selected = Math.min(selected, blocks.size() - 1);
                    changed();
                } };
        int x = r.right() - TOOLS_W + 2;
        for (int i = 0; i < 5; i++) {
            if (!all && i != 4)
                continue;
            if (i == 3 && b.type == COMMAND)
                continue; // already its words
            Rect t = new Rect(x + i * 10, r.y() + 3, 9, 10);
            boolean over = f.hovered(t);
            int colour = i == 4 && over ? Theme.ERROR : over ? Theme.GOLD : i == 3 && rawShown.contains(b) ? Theme.ORANGE : Theme.LABEL;
            Text.drawCentered(f.g, icons[i], t.centerX() + 1, t.y() + 1, colour);
            listClick(f, "block " + keys[i] + " " + index, t, actions[i], EditorRows.tip(I18n.get(KEY + keys[i]), null));
        }
    }

    private static void move(int index, int by) {
        int to = index + by;
        if (to < 0 || to >= blocks.size())
            return;
        blocks.add(to, blocks.remove(index));
        selected = to;
        changed();
    }

    // a value of an unknown command is just its n-th word
    private static Slot token(int n) {
        return new Slot("tok:" + n, Kind.WORD, "", "");
    }

    /**
     * Lays the block out in w from (x0, y0), wrapping: its sentence's words and a field per value, or for a raw line
     * a CMD tag and each word as its own field. Draws it when f isn't null; the height it takes either way.
     */
    private static int layout(Block b, int w, int x0, int y0, @Nullable HudFrame f, @Nullable String problem) {
        int x = 0, y = 0;
        if (isRaw(b)) {
            String line = b.write();
            List<Token> words = tokens(line);
            int bad = -1, at = problem == null ? -1 : CodeEditor.errorPos(problem);
            for (int i = 0; at >= 0 && i < words.size(); i++)
                if (at >= words.get(i).start())
                    bad = i;
            String tag = I18n.get(KEY + "cmd");
            int tw = Text.smallWidth(tag) + 6;
            if (f != null) {
                Theme.outline(f.g, x0, y0 + 2, tw, 9, 1, Theme.FAINT);
                Text.small(f.g, tag, x0 + 3, y0 + 4, Theme.DIM);
            }
            x = tw + 5;
            for (int i = 0; i <= words.size(); i++) {
                String text = i < words.size() ? words.get(i).text() : "+";
                int ww = Text.width(text) + 6;
                if (x > 0 && x + ww > w) {
                    x = 0;
                    y += ROW;
                }
                if (f != null)
                    word(f, b, token(i), text, new Rect(x0 + x, y0 + y + 1, ww, CHIP_H), i == bad, i == words.size());
                x += ww + 3;
            }
            return y + ROW;
        }
        String sentence = I18n.get(KEY + b.type.id());
        Map<String, Slot> slots = new LinkedHashMap<>();
        for (Object part : b.type.parts())
            if (part instanceof Slot s && s.kind() != Kind.LINE)
                slots.put(s.name(), s);
        boolean first = true;
        for (String word : sentence.split(" ")) {
            Slot s = word.startsWith("{") && word.endsWith("}") ? slots.get(word.substring(1, word.length() - 1)) : null;
            if (word.startsWith("{") && s == null) // the if's {then} sits under it
                continue;
            int ww = s != null ? chipWidth(b, s, w) : Text.width(word);
            if (x > 0 && x + ww > w) {
                x = 0;
                y += ROW;
            }
            if (f != null) {
                if (s != null)
                    chip(f, b, s, new Rect(x0 + x, y0 + y + 1, ww, CHIP_H));
                else
                    Text.draw(f.g, word, x0 + x, y0 + y + 3, first ? Theme.TEXT : Theme.DIM);
            }
            first = false;
            x += ww + 4;
        }
        return y + ROW;
    }

    private static int chipWidth(Block b, Slot s, int max) {
        return Math.min(max, Math.max(10, Text.width(shown(b, s)) + (picker(s) != null ? 13 : 4)));
    }

    // what a field shows: its value, shortened where the raw text says little (a uuid, a selector, two corners)
    private static String shown(Block b, Slot s) {
        String v = b.values.getOrDefault(s.name(), "");
        if (v.isEmpty())
            return s.kind() == Kind.OPT_REST ? I18n.get(KEY + "optional") : "…";
        return switch (s.kind()) {
            case BOX -> {
                String[] p = v.split(" ");
                yield p.length == 6 ? p[0] + " " + p[1] + " " + p[2] + " → " + p[3] + " " + p[4] + " " + p[5] : v;
            }
            case UNIT -> v.matches("[0-9a-f-]{36}") ? I18n.get(KEY + "unit_id", v.substring(0, 8)) : v;
            case BUILDING -> {
                Matcher m = Pattern.compile("x=(-?\\d+),y=(-?\\d+),z=(-?\\d+)").matcher(v);
                yield m.find() ? I18n.get(KEY + "building_at", m.group(1), m.group(2), m.group(3)) : v;
            }
            case HUD -> v.startsWith("button ") ? v.substring(7).replace("\"", "") : I18n.get(KEY + "region." + v);
            default -> s.options().equals("ifunless") ? I18n.get(KEY + "opt." + v) : v;
        };
    }

    @Nullable
    private static CodePicker.Kind picker(Slot s) {
        return switch (s.kind()) {
            case POS -> CodePicker.Kind.POS;
            case CAMERA -> CodePicker.Kind.CAMERA;
            case BOX -> CodePicker.Kind.ZONE;
            case UNIT -> CodePicker.Kind.UNIT;
            case BUILDING -> CodePicker.Kind.BUILDING;
            case HUD -> CodePicker.Kind.HUD;
            default -> null;
        };
    }

    private static boolean editing(Block b, Slot s) {
        return editBlock == b && editSlot != null && editSlot.name().equals(s.name());
    }

    private static boolean shownInList(Rect r) {
        return r.y() >= listRect.y() && r.bottom() <= listRect.bottom();
    }

    // a value: plain text with a dashed line under it; one picked in the world has ◎ and an orange line
    private static void chip(HudFrame f, Block b, Slot s, Rect r) {
        if (editing(b, s)) {
            editRect = r;
            return; // the edit box is drawn over it
        }
        boolean visible = shownInList(r);
        boolean hovered = visible && f.hovered(r);
        CodePicker.Kind pick = picker(s);
        boolean optional = s.kind() == Kind.OPT_REST && b.values.getOrDefault(s.name(), "").isEmpty();
        if (hovered)
            f.g.fill(r.x() - 1, r.y(), r.right() + 1, r.bottom(), Theme.BUTTON_HOVER);
        int tx = r.x();
        if (pick != null) {
            Text.draw(f.g, "◎", tx, r.y() + 2, hovered ? Theme.GOLD : Theme.ORANGE);
            tx += 9;
        }
        Text.draw(f.g, Text.fit(shown(b, s), r.right() - tx), tx, r.y() + 2, optional ? Theme.FAINT : Theme.TEXT);
        dashes(f, r.x(), r.bottom() - 1, r.w(), pick != null ? (hovered ? Theme.ORANGE : Theme.ORANGE_DARK) : hovered ? Theme.LABEL : Theme.FAINT);
        if (!visible)
            return;
        String name = I18n.get(KEY + "slot." + s.name());
        if (pick != null)
            click(f, "block field " + System.identityHashCode(b) + s.name(), r, () -> pick(b, s, pick), () -> edit(b, s, r),
                    EditorRows.tip(name, I18n.get(KEY + "pick_tip")));
        else
            click(f, "block field " + System.identityHashCode(b) + s.name(), r, () -> edit(b, s, r), null,
                    EditorRows.tip(name, s.options().isEmpty() ? null : I18n.get(KEY + "list_tip")));
    }

    // a word of a raw line: its own little box, red when the problem points at it; the last one adds a word
    private static void word(HudFrame f, Block b, Slot s, String text, Rect r, boolean bad, boolean adder) {
        if (editing(b, s)) {
            editRect = r;
            return;
        }
        boolean visible = shownInList(r);
        boolean hovered = visible && f.hovered(r);
        if (!adder)
            f.g.fill(r.x(), r.y(), r.right(), r.bottom(), hovered ? Theme.BUTTON_HOVER : Theme.RAISED);
        Text.draw(f.g, text, r.x() + 3, r.y() + 2, adder ? (hovered ? Theme.GOLD : Theme.FAINT) : bad ? Theme.DANGER_TEXT : Theme.BODY);
        if (bad)
            CodeEditor.squiggle(f.g, r.x() + 1, r.bottom() - 1, r.w() - 2, Theme.ERROR);
        if (visible)
            click(f, "block word " + System.identityHashCode(b) + s.name(), r, () -> edit(b, s, r), null,
                    EditorRows.tip(I18n.get(KEY + (adder ? "word_add" : "word")), I18n.get(KEY + (adder ? "word_add_tip" : "word_tip"))));
    }

    private static void dashes(HudFrame f, int x, int y, int w, int colour) {
        for (int i = 0; i < w; i += 3)
            f.g.fill(x + i, y, x + Math.min(w, i + 2), y + 1, colour);
    }

    // types into the field where it is; a field with a list shows it under, narrowed by what's typed
    private static void edit(Block b, Slot s, Rect r) {
        stopEditing();
        addOpen = false;
        editBlock = b;
        editSlot = s;
        editRect = r;
        editOptions = s.options().isEmpty() ? List.of() : options(s.options());
        optionScroll = 0;
        String value = s.name().startsWith("tok:") ? tokenText(b, s) : b.values.getOrDefault(s.name(), "");
        box.setValue(value);
        box.hudUnits = true;
        box.setX(r.x() + 3); // where it'll be drawn, so the click that opened it lands on it and keeps it focused
        box.setY(r.centerY() - 4);
        box.setWidth(Math.max(60, r.w() + 10) - 6);
        box.setFocused(true);
        box.moveCursorToEnd();
        box.setHighlightPos(0);
    }

    private static String tokenText(Block b, Slot s) {
        List<Token> words = tokens(b.write());
        int n = Integer.parseInt(s.name().substring(4));
        return n < words.size() ? words.get(n).text() : "";
    }

    private static void pick(Block b, Slot s, CodePicker.Kind kind) {
        stopEditing();
        addOpen = false;
        CodePicker.start(kind, true, text -> {
            if (text == null)
                return;
            if (kind == CodePicker.Kind.CAMERA) { // the whole camera line, with the zoom it was looked from
                Block picked = parse(text);
                if (picked.type == b.type) { // where and how near; who and the timings stay
                    b.values.put("pos", picked.values.get("pos"));
                    b.values.put("zoom", picked.values.get("zoom"));
                }
                changed();
                return;
            }
            set(b, s, kind == CodePicker.Kind.HUD && text.startsWith("rtsapi highlight @s ") ? text.substring(20) : text);
        });
    }

    private static void renderEditing(HudFrame f) {
        if (editBlock == null || editSlot == null)
            return;
        int w = Math.max(60, editRect.w() + 10);
        Rect field = new Rect(editRect.x() - 1, editRect.y() - 1, w, editRect.h() + 2);
        EditorRows.field(f, box, field);
        f.g.fill(field.x(), field.bottom() - 1, field.right(), field.bottom(), Theme.ORANGE);
        optionsRect = Rect.EMPTY;
        if (editOptions.isEmpty())
            return;
        String typed = box.getValue().toLowerCase(Locale.ROOT);
        String current = editBlock.values.getOrDefault(editSlot.name(), "").toLowerCase(Locale.ROOT);
        // the full list while the value is still what it was, else what matches the typing
        List<String> shown = typed.equals(current) ? editOptions
                : editOptions.stream().filter(o -> o.toLowerCase(Locale.ROOT).contains(typed)).toList();
        if (shown.isEmpty())
            return;
        int rows = Math.min(MAX_OPTIONS, shown.size());
        optionScroll = Math.max(0, Math.min(optionScroll, shown.size() - rows));
        int ow = w;
        for (String o : shown.subList(optionScroll, optionScroll + rows))
            ow = Math.max(ow, Text.width(o) + 14);
        int oh = rows * 12 + 4 + (shown.size() > rows ? 10 : 0);
        int oy = field.bottom() + oh > f.layout.height() - 4 ? field.y() - oh - 1 : field.bottom() + 1;
        optionsRect = new Rect(field.x(), oy, ow, oh);
        f.zone(optionsRect);
        f.g.fill(optionsRect.x(), oy, optionsRect.right(), oy + oh, Theme.RAISED);
        Theme.outline(f.g, optionsRect.x(), oy, ow, oh, 1, Theme.CONSOLE_RIM);
        for (int i = 0; i < rows; i++) {
            String o = shown.get(optionScroll + i);
            Rect row = new Rect(optionsRect.x() + 2, oy + 2 + i * 12, ow - 4, 12);
            boolean on = o.equalsIgnoreCase(current);
            if (f.hovered(row) || on)
                f.g.fill(row.x(), row.y(), row.right(), row.bottom(), on ? Theme.ACTIVE : Theme.BUTTON_HOVER);
            Text.draw(f.g, o, row.x() + 4, row.y() + 2, on ? Theme.GOLD : Theme.TEXT);
            Block b = editBlock;
            Slot s = editSlot;
            click(f, "block option " + i, row, () -> {
                editBlock = null; // picked: the defocus that follows mustn't commit the typing
                set(b, s, o);
                stopEditing();
            }, null, null);
        }
        if (shown.size() > rows)
            Text.small(f.g, (optionScroll + 1) + "–" + (optionScroll + rows) + " / " + shown.size(), optionsRect.x() + 6,
                    optionsRect.bottom() - 9, Theme.DIM);
    }

    // ------------------------------------------------------------------ adding

    private static boolean addOpen = false;
    private static boolean addReplace = false; // replacing the selected if's then instead of adding a line
    private static Rect addAnchor = Rect.EMPTY;
    @Nullable private static Cat addCat = null; // null: every category
    private static int addScroll = 0;
    private static boolean focusSearch = false;
    private static Rect addRect = Rect.EMPTY;
    private static final MyEditBox search = new MyEditBox.Builder(0, 0, 100, 9).bordered(false).maxLength(40).value("").build();

    static {
        TextInputClientEvents.registerEditBox(search);
    }

    private static void openAdd(Rect anchor, boolean replace) {
        stopEditing();
        if (addOpen && addReplace == replace) {
            addOpen = false;
            return;
        }
        addOpen = true;
        addReplace = replace;
        addAnchor = anchor;
        addScroll = 0;
        search.setValue("");
        focusSearch = true; // next frame: the click that opened it would unfocus it now
    }

    private static List<Type> addable() {
        String q = search.getValue().strip().toLowerCase(Locale.ROOT);
        return TYPES.stream().filter(t -> t != NOTE || addCat == Cat.OTHER || addCat == null)
                .filter(t -> addCat == null || t.cat() == addCat)
                .filter(t -> q.isEmpty() || I18n.get(KEY + "name." + t.id()).toLowerCase(Locale.ROOT).contains(q)
                        || I18n.get(KEY + "tip." + t.id()).toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    private static void renderAdd(HudFrame f) {
        addRect = Rect.EMPTY;
        if (!addOpen)
            return;
        if (focusSearch) {
            search.setFocused(true);
            focusSearch = false;
        }
        int w = 250;
        List<Type> found = addable();
        int rows = Math.min(9, found.size());
        addScroll = Math.max(0, Math.min(addScroll, found.size() - rows));
        Cat[] cats = Cat.values();
        int h = 20 + catLines(cats, w) * 12 + 6 + Math.max(1, rows) * 13 + 6;
        int x = Math.min(addAnchor.x(), listRect.right() - w - 4);
        int y = addAnchor.bottom() + 2;
        if (y + h > listRect.bottom() + 20)
            y = addAnchor.y() - h - 2;
        y = Math.max(4, y);
        addRect = new Rect(x, y, w, h);
        // a click anywhere else closes it
        click(f, "block add away", new Rect(0, 0, f.layout.width(), f.layout.height()), () -> addOpen = false, () -> addOpen = false, null);
        f.zone(addRect);
        f.g.fill(x, y, x + w, y + h, Theme.RAISED);
        Theme.outline(f.g, x, y, w, h, 1, Theme.CONSOLE_RIM);

        Rect field = new Rect(x + 5, y + 5, w - 10, 13);
        EditorRows.field(f, search, field);
        f.g.fill(field.x(), field.bottom() - 1, field.right(), field.bottom(), Theme.ORANGE);
        if (search.getValue().isEmpty())
            Text.draw(f.g, I18n.get(KEY + (addReplace ? "search_then" : "search")), field.x() + 4, field.y() + 3, Theme.DIM);

        int ly = drawCats(f, cats, x, y, w) + 15;
        f.g.fill(x + 1, ly - 3, x + w - 1, ly - 2, Theme.CONSOLE_RIM);
        if (found.isEmpty())
            Text.draw(f.g, I18n.get(KEY + "none_found"), x + 8, ly + 3, Theme.DIM);
        drawFound(f, found, rows, x, w, ly);
        if (found.size() > rows)
            Text.smallRight(f.g, (addScroll + 1) + "–" + (addScroll + rows) + " / " + found.size(), x + w - 6, y + h - 8, Theme.DIM);
    }

    // how many lines the categories wrap to
    private static int catLines(Cat[] cats, int w) {
        int catX = 0, catLines = 1;
        for (int i = -1; i < cats.length; i++) {
            String name = i < 0 ? I18n.get(KEY + "cat.all") : I18n.get(KEY + "cat." + cats[i].name().toLowerCase(Locale.ROOT));
            int cw = Text.width(name) + (i < 0 ? 4 : 10) + 8;
            if (catX + cw > w - 12) {
                catX = 0;
                catLines++;
            }
            catX += cw;
        }
        return catLines;
    }

    // the category tabs, wrapped; returns the y of their last line
    private static int drawCats(HudFrame f, Cat[] cats, int x, int y, int w) {
        int cx = x + 6, cy = y + 22;
        for (int i = -1; i < cats.length; i++) {
            Cat c = i < 0 ? null : cats[i];
            String name = c == null ? I18n.get(KEY + "cat.all") : I18n.get(KEY + "cat." + c.name().toLowerCase(Locale.ROOT));
            int cw = Text.width(name) + (c == null ? 4 : 10);
            if (cx + cw > x + w - 6) {
                cx = x + 6;
                cy += 12;
            }
            Rect t = new Rect(cx - 2, cy, cw + 4, 11);
            boolean on = addCat == c, over = f.hovered(t);
            int tx = cx;
            if (c != null) {
                f.g.fill(tx, cy + 4, tx + 4, cy + 8, c.colour);
                tx += 7;
            }
            Text.draw(f.g, name, tx, cy + 2, on ? Theme.GOLD : over ? Theme.TEXT : Theme.LABEL);
            if (on)
                f.g.fill(t.x(), t.bottom() - 1, t.right(), t.bottom(), Theme.ORANGE);
            click(f, "block add cat " + i, t, () -> {
                addCat = c;
                addScroll = 0;
            }, null, null);
            cx += cw + 8;
        }
        return cy;
    }

    private static void drawFound(HudFrame f, List<Type> found, int rows, int x, int w, int ly) {
        for (int i = 0; i < rows; i++) {
            Type type = found.get(addScroll + i);
            Rect row = new Rect(x + 3, ly + i * 13, w - 6, 13);
            if (f.hovered(row))
                f.g.fill(row.x(), row.y(), row.right(), row.bottom(), Theme.BUTTON_HOVER);
            f.g.fill(row.x() + 4, row.y() + 5, row.x() + 8, row.y() + 9, type.cat().colour);
            String name = I18n.get(KEY + "name." + type.id());
            Text.draw(f.g, name, row.x() + 12, row.y() + 3, Theme.TEXT);
            int nameW = Math.max(80, Text.width(name) + 10);
            Text.draw(f.g, Text.fit(I18n.get(KEY + "tip." + type.id()), row.w() - 14 - nameW), row.x() + 12 + nameW, row.y() + 3, Theme.DIM);
            click(f, "block add " + type.id(), row, () -> add(type), null, null);
        }
    }

    // after the selected line (or at the end), or in place of the selected if's then
    private static void add(Type type) {
        addOpen = false;
        search.setFocused(false);
        Block b = create(type);
        if (type.id().equals("schedule") && CodeEditorPopup.path() != null)
            b.values.put("file", CodeEditorPopup.path()); // its own path: the file repeats
        if (addReplace && selected >= 0 && selected < blocks.size() && blocks.get(selected).then != null) {
            Block parent = blocks.get(selected);
            parent.then = b;
            parent.values.put("then", b.write());
        } else {
            int at = selected < 0 || selected >= blocks.size() ? blocks.size() : selected + 1;
            blocks.add(at, b);
            selected = at;
            thenSelected = false;
            scroll = Math.max(0, scrollTo(at));
        }
        changed();
    }

    private static int scrollTo(int index) {
        int y = PAD, w = listRect.w() - gutterW() - 3;
        for (int i = 0; i < index; i++)
            y += height(blocks.get(i), w, null);
        return y - listRect.h() / 2;
    }

    // ------------------------------------------------------------------ input

    // a click area in the list: only the part of it the list shows, so a block scrolled half out can't take the header's clicks
    private static void listClick(HudFrame f, String id, Rect r, Runnable left, @Nullable Supplier<Tooltip> tip) {
        int top = Math.max(r.y(), listRect.y()), bottom = Math.min(r.bottom(), listRect.bottom());
        if (bottom > top)
            click(f, id, new Rect(r.x(), top, r.w(), bottom - top), left, null, tip);
    }

    private static void click(HudFrame f, String id, Rect r, @Nullable Runnable left, @Nullable Runnable right, @Nullable Supplier<Tooltip> tip) {
        f.register(new ButtonBuilder(id).onLeftClick(left).onRightClick(right).build(), r, null, tip);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(ScreenEvent.MouseScrolled.Pre evt) {
        if (!CodeEditorPopup.isOpen() || !CodeEditorPopup.blocksShown())
            return;
        int ux = HudGeometry.scale().toUnits(evt.getMouseX()), uy = HudGeometry.scale().toUnits(evt.getMouseY());
        if (addRect.contains(ux, uy)) {
            addScroll -= (int) Math.signum(evt.getScrollDelta());
            evt.setCanceled(true);
        } else if (optionsRect.contains(ux, uy)) {
            optionScroll -= (int) Math.signum(evt.getScrollDelta());
            evt.setCanceled(true);
        } else if (listRect.contains(ux, uy)) {
            scroll -= (int) (evt.getScrollDelta() * 24);
            evt.setCanceled(true);
        }
    }
}
