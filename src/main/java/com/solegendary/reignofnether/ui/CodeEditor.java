package com.solegendary.reignofnether.ui;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.commands.rtsapi.RonIds;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.rtsmap.MapInfoMenu;
import com.solegendary.reignofnether.rtsmap.RonFileServerboundPacket;
import com.solegendary.reignofnether.rtsmap.RonFilesClient;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A multi-line text editor for .mcfunction files, drawn in HUD units: vanilla's MultilineTextField holds the text,
 * cursor and selection (and does arrows, Home/End, Ctrl+A/C/X/V), long lines wrap. Draw it every frame with
 * {@link #render}; TextInputClientEvents routes keys, typing, clicks and the wheel to it.
 */
public class CodeEditor {

    private static final int LINE_H = 10;
    private static final int PAD = 3;
    private static final int VANILLA_LINE_H = 9; // what MultilineTextField.seekCursorToPoint counts in
    private static final int MAX_SUGGESTIONS = 6;
    // what a word typed after each of these can be; the last group is the word so far
    private static final List<Context> CONTEXTS = List.of(
            new Context(Pattern.compile("(?:^|\\s)(deny|allow|summon|place)\\s+(\\S*)$"), m -> ids(m.group(1))),
            new Context(Pattern.compile("(?:^|\\s)(?:run|then|schedule\\s+\\d+)\\s+(\\S*)$"), m -> files()),
            new Context(Pattern.compile("(?:^|\\s)playsound\\s+(\\S*)$"), m -> sounds()),
            new Context(Pattern.compile("(?:^|\\s)say\\s+\\S+\\s+(\\S*)$"), m -> heads()),
            new Context(Pattern.compile("(?:^|\\s)(?:restrict|victory|defeat|soldier\\s+\\S+)\\s+(\\S*)$"), m -> owners()));
    private static final int SELECTION = (Theme.ORANGE & 0xFFFFFF) | 0x55000000;
    private static final int PROBLEM_TINT = (Theme.ERROR & 0xFFFFFF) | 0x14000000;

    // editors drawn lately (the wheel and clicks go to the one under the mouse), and the one typing goes to
    private static final List<CodeEditor> shown = WorldState.client(new ArrayList<>());
    @Nullable private static CodeEditor focused = null;
    static {
        WorldState.onClientReset(() -> focused = null);
    }

    public Runnable onSave = null; // Ctrl+S

    private MultilineTextField field = new MultilineTextField(Theme.font(), 100);
    private Map<Integer, String> problems = Map.of();
    private Rect rect = Rect.EMPTY;
    private Rect textRect = Rect.EMPTY;
    private Rect popupRect = Rect.EMPTY;
    private int scroll = 0; // first display line shown
    private boolean followCursor = false;
    private boolean dragging = false;
    private long renderedMs = 0;
    private List<Suggestion> suggestions = List.of();

    private record Suggestion(String id, String kind) { }

    private record Context(Pattern pattern, Function<Matcher, List<Suggestion>> list) { }

    // Ctrl+click on a word: true if it was a link (a file path) that got opened
    public Predicate<String> onLink = null;

    public CodeEditor() {
        field.setCharacterLimit(RonFileServerboundPacket.MAX_BYTES);
    }

    public void setText(String text) {
        field.setValue(text.replace("\r", ""));
        field.seekCursor(Whence.ABSOLUTE, 0);
        scroll = 0;
    }

    public String getText() {
        return field.value();
    }

    /** Types text at the cursor, replacing the selection. */
    public void insert(String text) {
        field.insertText(text);
        followCursor = true;
    }

    /** The current line up to the cursor. */
    public String lineBeforeCursor() {
        String value = field.value();
        return value.substring(value.lastIndexOf('\n', field.cursor() - 1) + 1, field.cursor());
    }

    /** Problems by line number (from 1): their numbers turn red, with the message as their tooltip. */
    public void setProblems(Map<Integer, String> problems) {
        this.problems = problems != null ? problems : Map.of();
    }

    public boolean isFocused() {
        return focused == this && isShown();
    }

    public void setFocused(boolean focus) {
        if (focus)
            focused = this;
        else if (focused == this)
            focused = null;
    }

    private boolean isShown() {
        return System.currentTimeMillis() - renderedMs < 250;
    }

    // ---- drawing ----

    public void render(HudFrame f, Rect r) {
        var g = f.g;
        var font = Theme.font();
        renderedMs = System.currentTimeMillis();
        if (!shown.contains(this))
            shown.add(this);
        rect = r;
        f.zone(r);
        Theme.outline(g, r.x(), r.y(), r.w(), r.h(), 1, Theme.INK);
        g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, Theme.SUNKEN);

        int lineCount = field.getLineCount();
        int gutterW = Math.max(16, font.width(String.valueOf(lineCount)) + 7);
        g.fill(r.x() + 1, r.y() + 1, r.x() + 1 + gutterW, r.bottom() - 1, Theme.EMPTY);
        g.fill(r.x() + 1 + gutterW, r.y() + 1, r.x() + 2 + gutterW, r.bottom() - 1, Theme.INK);
        textRect = new Rect(r.x() + 2 + gutterW + PAD, r.y() + PAD, r.w() - gutterW - 3 - 2 * PAD, r.h() - 2 * PAD);
        if (textRect.w() < 10 || textRect.h() < LINE_H)
            return;
        rewrapTo(textRect.w());

        int visible = textRect.h() / LINE_H;
        lineCount = field.getLineCount();
        scrollToCursor(visible, lineCount);
        dragSelect(f);
        drawLines(f, r, gutterW, visible);

        // a scrollbar thumb when it doesn't all fit
        if (lineCount > visible) {
            int trackH = r.h() - 4;
            int thumbH = Math.max(6, trackH * visible / lineCount);
            int thumbY = r.y() + 2 + (trackH - thumbH) * scroll / Math.max(1, lineCount - visible);
            g.fill(r.right() - 3, thumbY, r.right() - 1, thumbY + thumbH, Theme.FAINT);
        }

        suggestions = isFocused() ? suggestions() : List.of();
        renderSuggestions(f);
    }

    private void scrollToCursor(int visible, int lineCount) {
        if (followCursor) {
            int at = field.getLineAtCursor();
            if (at < scroll)
                scroll = at;
            else if (at >= scroll + visible)
                scroll = at - visible + 1;
            followCursor = false;
        }
        scroll = Math.max(0, Math.min(scroll, lineCount - visible));
    }

    private void dragSelect(HudFrame f) {
        if (dragging) {
            if (MiscUtil.isLeftClickDown(Minecraft.getInstance())) {
                field.setSelecting(true);
                seekTo(f.mouseX, f.mouseY);
                if (f.mouseY < textRect.y() || f.mouseY >= textRect.bottom())
                    followCursor = true;
            } else
                dragging = false;
        }
    }

    private void drawLines(HudFrame f, Rect r, int gutterW, int visible) {
        var g = f.g;
        var font = Theme.font();
        String value = field.value();
        var selected = field.getSelected();
        boolean blink = isFocused() && (System.currentTimeMillis() / 500) % 2 == 0;
        int cursorLine = field.getLineAtCursor();

        Clip.push(g, r.x() + 1, textRect.y(), r.w() - 2, textRect.h());
        int lineNo = 1;
        int index = 0;
        for (var line : field.iterateLines()) {
            boolean firstOfLine = index == 0 || value.charAt(line.beginIndex() - 1) == '\n'; // not a wrapped part
            if (index > 0 && firstOfLine)
                lineNo++;
            int row = index - scroll;
            index++;
            if (row < 0)
                continue;
            if (row >= visible)
                break;
            int y = textRect.y() + row * LINE_H;
            String problem = problems.get(lineNo);
            if (problem != null)
                g.fill(textRect.x() - PAD, y, textRect.right() + PAD, y + LINE_H, PROBLEM_TINT);
            if (firstOfLine)
                drawLineNumber(f, r, gutterW, y, lineNo, problem);
            // selection, the text, then the cursor
            int s = Math.max(line.beginIndex(), selected.beginIndex());
            int e = Math.min(line.endIndex(), selected.endIndex());
            boolean emptyLineSelected = line.beginIndex() == line.endIndex() && selected.beginIndex() <= line.beginIndex() && line.beginIndex() < selected.endIndex();
            if (s < e || emptyLineSelected)
                g.fill(textRect.x() + font.width(value.substring(line.beginIndex(), s)), y,
                        textRect.x() + font.width(value.substring(line.beginIndex(), e)) + (s == e ? 2 : 0), y + LINE_H, SELECTION);
            String text = value.substring(line.beginIndex(), line.endIndex());
            Text.draw(g, text, textRect.x(), y + 1, Theme.TEXT);
            if (problem != null)
                drawProblem(g, value, line, text, y, problem);
            if (blink && index - 1 == cursorLine) {
                int cx = textRect.x() + font.width(value.substring(line.beginIndex(), Math.min(field.cursor(), line.endIndex())));
                g.fill(cx, y, cx + 1, y + LINE_H - 1, Theme.TEXT);
            }
        }
        Clip.pop(g);
    }

    private void drawLineNumber(HudFrame f, Rect r, int gutterW, int y, int lineNo, @Nullable String problem) {
        String n = String.valueOf(lineNo);
        Text.draw(f.g, n, r.x() + gutterW - 3 - Theme.font().width(n), y + 1, problem != null ? Theme.ERROR : Theme.FAINT);
        if (problem != null) {
            Rect cell = new Rect(r.x() + 1, y, gutterW, LINE_H);
            f.register(new ButtonBuilder("code line " + lineNo).build(), cell, null,
                    EditorRows.tip(I18n.get("editor.reignofnether.code.line", lineNo), problem));
        }
    }

    // the part the problem points at underlined, the problem itself after the line where there's room
    private void drawProblem(GuiGraphics g, String value, MultilineTextField.StringView line, String text, int y, String problem) {
        var font = Theme.font();
        int lineStart = value.lastIndexOf('\n', line.beginIndex() - 1) + 1;
        int at = errorPos(problem);
        if (at >= 0) {
            int s0 = lineStart + at - line.beginIndex();
            if (s0 >= 0 && s0 <= text.length()) {
                int from = Math.max(0, text.lastIndexOf(' ', Math.max(0, s0 - 1)) + 1), to = text.indexOf(' ', s0);
                if (to < 0)
                    to = text.length();
                int ux = textRect.x() + font.width(text.substring(0, from)), uw = Math.max(4, font.width(text.substring(from, to)));
                squiggle(g, ux, y + LINE_H - 1, uw, Theme.ERROR);
            }
        }
        boolean lastPart = line.endIndex() >= value.length() || value.charAt(line.endIndex()) == '\n';
        int mx = textRect.x() + font.width(text) + 12;
        if (lastPart && textRect.right() - mx > 40)
            Text.draw(g, Text.fit(friendly(problem), textRect.right() - mx), mx, y + 1, Theme.DANGER_TEXT);
    }

    // MultilineTextField wraps to the width it was made with: a new width needs a new one
    private void rewrapTo(int width) {
        if (fieldWidth == width)
            return;
        fieldWidth = width;
        String value = field.value();
        int cursor = field.cursor();
        field = new MultilineTextField(Theme.font(), width);
        field.setCharacterLimit(RonFileServerboundPacket.MAX_BYTES);
        field.setValue(value);
        field.seekCursor(Whence.ABSOLUTE, cursor);
    }
    private int fieldWidth = 100;

    private void seekTo(int ux, int uy) {
        int row = Math.floorDiv(uy - textRect.y(), LINE_H) + scroll;
        field.seekCursorToPoint(ux - textRect.x(), row * VANILLA_LINE_H + VANILLA_LINE_H / 2.0);
    }

    private static final Pattern POSITION = Pattern.compile(" at position (\\d+)");

    /** Where in its line a command problem points (brigadier's "at position N"), or -1. */
    public static int errorPos(String problem) {
        Matcher m = POSITION.matcher(problem);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** A problem without brigadier's position and its "...text<--[HERE]" tail. */
    public static String friendly(String problem) {
        int at = problem.indexOf(" at position ");
        return at > 0 ? problem.substring(0, at) : problem;
    }

    /** A wavy line under w pixels from x, like a spelling mistake. */
    public static void squiggle(GuiGraphics g, int x, int y, int w, int colour) {
        for (int i = 0; i < w; i++)
            g.fill(x + i, y + (i / 2 % 2), x + i + 1, y + (i / 2 % 2) + 1, colour);
    }

    // ---- id suggestions ----

    private List<Suggestion> suggestions() {
        if (field.hasSelection())
            return List.of();
        Matcher m = context();
        if (m == null)
            return List.of();
        String partial = m.group(m.groupCount()).toLowerCase(Locale.ROOT).replaceFirst("^" + ReignOfNether.MOD_ID + ":", "");
        List<Suggestion> out = new ArrayList<>();
        for (Suggestion s : contextList(m)) {
            String id = s.id().toLowerCase(Locale.ROOT);
            if (id.startsWith(partial) && !id.equals(partial))
                out.add(s);
            if (out.size() == MAX_SUGGESTIONS)
                break;
        }
        return out;
    }

    // the context the word at the cursor is in, matched on the line so far
    @Nullable
    private Matcher context() {
        String line = lineBeforeCursor();
        for (Context c : CONTEXTS) {
            Matcher m = c.pattern().matcher(line);
            if (m.find())
                return m;
        }
        return null;
    }

    private static List<Suggestion> contextList(Matcher m) {
        for (Context c : CONTEXTS)
            if (c.pattern() == m.pattern())
                return c.list().apply(m);
        return List.of();
    }

    // ron/ scripts, for rtsapi run, say ... then and rtsapi schedule
    private static List<Suggestion> files() {
        String kind = I18n.get("editor.reignofnether.code.file");
        return RonFilesClient.files.keySet().stream().map(p -> new Suggestion(p, kind)).toList();
    }

    private static List<Suggestion> sounds() {
        String kind = I18n.get("editor.reignofnether.code.sound");
        return ForgeRegistries.SOUND_EVENTS.getKeys().stream().map(id -> new Suggestion(id.toString(), kind))
                .sorted(Comparator.comparing(Suggestion::id)).toList();
    }

    // rtsapi say's head: a unit's portrait, or none
    private static List<Suggestion> heads() {
        String kind = I18n.get("editor.reignofnether.code.head");
        List<Suggestion> list = new ArrayList<>(List.of(new Suggestion("none", kind)));
        unitIds().forEach(id -> list.add(new Suggestion(id, kind)));
        return list;
    }

    // who a restriction, victory or soldier is for: everyone, the map's seats, then online players
    private static List<Suggestion> owners() {
        List<Suggestion> list = new ArrayList<>(List.of(new Suggestion("*", I18n.get("editor.reignofnether.code.everyone"))));
        String seat = I18n.get("editor.reignofnether.code.seat");
        for (var s : MapInfoMenu.info().getSeats())
            list.add(new Suggestion(MapInfoMenu.seatName(s).replace(' ', '_'), seat));
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null)
            for (var p : connection.getOnlinePlayers())
                list.add(new Suggestion(p.getProfile().getName(), I18n.get("editor.reignofnether.code.player")));
        return list;
    }

    /** The map's seats, then online players, as rtsapi player names take them. */
    public static List<String> ownerIds() {
        return owners().stream().map(Suggestion::id).filter(id -> !id.equals("*")).toList();
    }

    /** Every unit's id, as unit_trained/<unit> and rtsapi unit summon use them. */
    public static List<String> unitIds() {
        if (summonIds == null) {
            summonIds = new ArrayList<>();
            for (ResourceLocation type : ForgeRegistries.ENTITY_TYPES.getKeys())
                if (type.getNamespace().equals(ReignOfNether.MOD_ID))
                    summonIds.add(shortId(RonIds.prodId(type)));
            summonIds.sort(null);
        }
        return summonIds;
    }

    /** Every research id (production items that aren't units). */
    public static List<String> researchIds() {
        List<String> list = new ArrayList<>();
        for (ResourceLocation id : ReignOfNetherRegistries.PRODUCTION_ITEM.keySet())
            if (!unitIds().contains(shortId(id)))
                list.add(shortId(id));
        list.sort(null);
        return list;
    }

    private static List<String> summonIds = null;

    // RonIds' canonical ids without the reignofnether: namespace: units, research and buildings for restrict deny|allow,
    // every mod entity for summon (as /rtsapi unit summon suggests them), buildings for place
    private static List<Suggestion> ids(String verb) {
        unitIds();
        String unit = I18n.get("editor.reignofnether.code.unit");
        String building = I18n.get("editor.reignofnether.code.building");
        List<Suggestion> list = new ArrayList<>();
        if (verb.equals("summon"))
            summonIds.forEach(id -> list.add(new Suggestion(id, unit)));
        if (verb.equals("deny") || verb.equals("allow"))
            for (ResourceLocation id : ReignOfNetherRegistries.PRODUCTION_ITEM.keySet())
                list.add(new Suggestion(shortId(id), summonIds.contains(shortId(id)) ? unit : I18n.get("editor.reignofnether.code.research")));
        if (!verb.equals("summon")) {
            for (ResourceLocation id : ReignOfNetherRegistries.BUILDING.keySet())
                list.add(new Suggestion(shortId(id), building));
            for (CustomBuilding custom : CustomBuildingClientEvents.customBuildings)
                list.add(new Suggestion(RonIds.of(custom).toString(), building));
        }
        list.sort(Comparator.comparing(Suggestion::id));
        return list;
    }

    private static String shortId(ResourceLocation id) {
        return id.getNamespace().equals(ReignOfNether.MOD_ID) ? id.getPath() : id.toString();
    }

    private void renderSuggestions(HudFrame f) {
        popupRect = Rect.EMPTY;
        if (suggestions.isEmpty())
            return;
        var font = Theme.font();
        int w = Text.smallWidth(I18n.get("editor.reignofnether.code.suggestions")) + 8;
        for (Suggestion s : suggestions)
            w = Math.max(w, font.width(s.id()) + Text.smallWidth(s.kind()) + 14);
        int h = 12 + suggestions.size() * LINE_H + 3;
        int x = Math.min(cursorX(), rect.right() - w - 2);
        int y = cursorY() + LINE_H + 1;
        if (y + h > rect.bottom())
            y = cursorY() - h - 1;
        popupRect = new Rect(x, y, w, h);
        f.zone(popupRect);
        Theme.popup(f.g, x, y, w, h, Theme.ORANGE);
        Text.small(f.g, I18n.get("editor.reignofnether.code.suggestions"), x + 4, y + 2, Theme.LABEL);
        for (int i = 0; i < suggestions.size(); i++) {
            Rect row = suggestionRect(i);
            if (f.hovered(row) || i == 0)
                f.g.fill(row.x(), row.y(), row.right(), row.bottom(), i == 0 ? Theme.ACTIVE : Theme.RAISED_HOVER);
            Text.draw(f.g, suggestions.get(i).id(), row.x() + 3, row.y() + 1, Theme.TEXT);
            boolean sound = suggestions.get(i).kind().equals(I18n.get("editor.reignofnether.code.sound"));
            Text.smallRight(f.g, (sound ? "▶ " : "") + suggestions.get(i).kind(), row.right() - 3, row.y() + 1, sound ? Theme.GOLD : Theme.DIM);
        }
    }

    private Rect suggestionRect(int i) {
        return new Rect(popupRect.x() + 1, popupRect.y() + 12 + i * LINE_H, popupRect.w() - 2, LINE_H);
    }

    private int cursorX() {
        var line = field.getLineView(field.getLineAtCursor());
        return textRect.x() + Theme.font().width(field.value().substring(line.beginIndex(), field.cursor()));
    }

    private int cursorY() {
        return textRect.y() + (field.getLineAtCursor() - scroll) * LINE_H;
    }

    private void insertSuggestion(Suggestion s) {
        Matcher m = context();
        if (m == null)
            return;
        field.setSelecting(false);
        field.deleteText(-m.group(m.groupCount()).length());
        field.insertText(s.id());
        followCursor = true;
    }

    // ---- input, from TextInputClientEvents (coordinates in GUI pixels) ----

    @Nullable
    private static CodeEditor at(int ux, int uy) {
        for (int i = shown.size() - 1; i >= 0; i--) {
            CodeEditor e = shown.get(i);
            if (e.isShown() && (e.rect.contains(ux, uy) || e.popupRect.contains(ux, uy)))
                return e;
        }
        return null;
    }

    @Nullable
    public static CodeEditor focusedEditor() {
        return focused != null && focused.isShown() ? focused : null;
    }

    /** @return true if a code editor took the key */
    public static boolean keyPressed(int keyCode) {
        CodeEditor e = focusedEditor();
        if (e == null)
            return false;
        e.followCursor = true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE)
            e.setFocused(false);
        else if (keyCode == GLFW.GLFW_KEY_S && Screen.hasControlDown()) {
            if (e.onSave != null)
                e.onSave.run();
        } else if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (!e.suggestions.isEmpty())
                e.insertSuggestion(e.suggestions.get(0));
            else
                e.field.insertText("  ");
        } else
            e.field.keyPressed(keyCode);
        return true; // every key belongs to the editor while it's focused, so no hotkey fires while typing
    }

    public static boolean charTyped(char c) {
        CodeEditor e = focusedEditor();
        if (e == null || !SharedConstants.isAllowedChatCharacter(c))
            return false;
        e.field.insertText(Character.toString(c));
        e.followCursor = true;
        return true;
    }

    /** A click on an editor focuses it and moves the cursor there; a click anywhere else unfocuses it. */
    public static boolean mouseClicked(double guiX, double guiY, int button) {
        HudScale scale = HudGeometry.scale();
        int ux = scale.toUnits(guiX), uy = scale.toUnits(guiY);
        CodeEditor e = at(ux, uy);
        if (focused != null && focused != e)
            focused = null;
        if (e == null)
            return false;
        if (button != GLFW.GLFW_MOUSE_BUTTON_1)
            return true;
        for (int i = 0; i < e.suggestions.size(); i++)
            if (e.suggestionRect(i).contains(ux, uy)) {
                Suggestion s = e.suggestions.get(i);
                // the right end of a sound plays it instead
                if (s.kind().equals(I18n.get("editor.reignofnether.code.sound")) && ux > e.suggestionRect(i).right() - 40)
                    playSound(s.id());
                else
                    e.insertSuggestion(s);
                return true;
            }
        if (e.popupRect.contains(ux, uy))
            return true;
        focused = e;
        e.field.setSelecting(Screen.hasShiftDown());
        e.seekTo(ux, uy);
        if (Screen.hasControlDown() && e.onLink != null && e.onLink.test(e.wordAtCursor()))
            return true;
        e.dragging = true;
        return true;
    }

    public static boolean mouseScrolled(double guiX, double guiY, double delta) {
        HudScale scale = HudGeometry.scale();
        CodeEditor e = at(scale.toUnits(guiX), scale.toUnits(guiY));
        if (e == null)
            return false;
        e.scroll = Math.max(0, e.scroll + (delta > 0 ? -3 : 3)); // clamped to the end when drawn
        return true;
    }

    // the word around the cursor, quotes and a ron/ prefix or .mcfunction suffix taken off
    private String wordAtCursor() {
        String value = field.value();
        int a = field.cursor(), b = field.cursor();
        while (a > 0 && !Character.isWhitespace(value.charAt(a - 1)))
            a--;
        while (b < value.length() && !Character.isWhitespace(value.charAt(b)))
            b++;
        return value.substring(a, b).replace("\"", "").replaceFirst("^ron/", "");
    }

    private static void playSound(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null)
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvent.createVariableRangeEvent(rl), 1f));
    }
}
