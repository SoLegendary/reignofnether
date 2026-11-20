package com.solegendary.reignofnether.editor;

import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.hud.MyEditBox;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.rtsmap.RonFiles;
import com.solegendary.reignofnether.rtsmap.RonFilesClient;
import com.solegendary.reignofnether.ui.CodeEditor;
import com.solegendary.reignofnether.ui.EditorRows;
import com.solegendary.reignofnether.ui.EditorRows.Style;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Toast;
import com.solegendary.reignofnether.util.WorldState;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.sandbox.MapSheets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * A centred modal popup that edits one ron/ .mcfunction file: the code on the left, its problems on the right and
 * Open externally / Run now / Cancel / Save below. Closing it (✕ or Esc) keeps unsaved text as a draft until the
 * world closes; Cancel throws the draft away. A file that doesn't exist yet opens empty and is created on Save.
 */
public final class CodeEditorPopup {

    private static final String KEY = "editor.reignofnether.popup.";
    private static final int W = 630;
    private static final int H = 380;
    private static final int PAD = 8;
    private static final int BUTTON_H = 16;
    private static final int VALIDATE_MS = 700; // a pause in typing this long checks the draft

    private static final CodeEditor code = new CodeEditor();
    private static final Map<String, String> drafts = WorldState.client(new HashMap<>()); // path -> unsaved text
    @Nullable private static String path = null;
    private static String title = "";
    private static String runsWhen = "";
    @Nullable private static String base = null; // the file's saved text the editor started from (null: not here yet)
    private static int seenVersion = -1;
    private static String lastText = "";
    private static long editedMs = 0;
    private static String validated = "";
    @Nullable private static GuiGraphics drawnOn = null; // a new GuiGraphics every frame: drawn once per frame
    private static long drawnMs = 0;
    private static boolean blocks = true; // the Blocks view instead of the Code view, kept between files

    // the file's path, editable: saving under another path moves the file there
    private static final MyEditBox pathBox = new MyEditBox.Builder(0, 0, 120, 9).bordered(false).maxLength(200).value("").build();

    static {
        TextInputClientEvents.registerEditBox(pathBox);
        code.onSave = CodeEditorPopup::save;
        // Ctrl+click on a script's path opens it
        code.onLink = word -> {
            String target = RonFilesClient.normalize(word);
            if (!RonFilesClient.exists(target))
                return false;
            open(target, target, MapSheets.runsWhen(target));
            return true;
        };
        WorldState.onClientReset(() -> {
            path = null;
            base = null;
            drawnOn = null;
        });
    }

    private CodeEditorPopup() { }

    /** Opens ron/&lt;path&gt;.mcfunction; title is the popup's heading and runsWhen says when the file runs. */
    public static void open(String path, String title, String runsWhen) {
        stash();
        path = RonFilesClient.normalize(path);
        CodeEditorPopup.path = path;
        CodeEditorPopup.title = title;
        CodeEditorPopup.runsWhen = runsWhen;
        pathBox.setValue(path);
        base = RonFilesClient.contents.get(path);
        String draft = drafts.remove(path);
        code.setText(draft != null ? draft : base != null ? base : "");
        lastText = validated = code.getText();
        seenVersion = RonFilesClient.version();
        code.setProblems(RonFilesClient.problems.getOrDefault(path, Map.of()));
        code.setFocused(!blocks);
        RonFilesClient.open(path); // the file may have changed by hand since it was last read
    }

    public static boolean isOpen() {
        return path != null;
    }

    @Nullable
    static String path() {
        return path;
    }

    static boolean blocksShown() {
        return blocks;
    }

    /** Closes it, keeping unsaved text for the next time this file opens. */
    public static void close() {
        stash();
        path = null;
        code.setFocused(false);
    }

    // closes it and forgets the unsaved text
    private static void cancel() {
        if (path != null)
            drafts.remove(path);
        path = null;
        code.setFocused(false);
    }

    /** Esc while the popup shows and its editor isn't focused closes it (the draft is kept); true if it did. */
    public static boolean escape() {
        if (path == null || System.currentTimeMillis() - drawnMs > 250 || CodeEditor.focusedEditor() != null)
            return false;
        if (menu != Menu.NONE) {
            menu = Menu.NONE;
            return true;
        }
        if (blocks && CodeBlocks.escape()) // Esc while typing in a block's field stops typing
            return true;
        close();
        return true;
    }

    private static void stash() {
        if (path == null)
            return;
        if (isDirty())
            drafts.put(path, code.getText());
        else
            drafts.remove(path);
    }

    private static boolean isDirty() {
        return !code.getText().equals(base != null ? base : "") || !newPath().equals(path);
    }

    private static String newPath() {
        String typed = RonFilesClient.normalize(pathBox.getValue());
        return typed.isEmpty() ? path : typed;
    }

    private static boolean save() {
        if (path == null)
            return false;
        String to = newPath();
        if (!RonFilesClient.save(to, code.getText()))
            return false;
        if (!to.equals(path)) { // moved: the old file goes
            if (RonFilesClient.exists(path))
                RonFilesClient.delete(path);
            drafts.remove(path);
            path = to;
            if (!title.isEmpty() && !title.equals(to))
                title = to;
        }
        Toast.show(I18n.get(KEY + "saved", fileName()), Toast.DEFAULT_TICKS);
        return true;
    }

    private static void delete() {
        if (path == null)
            return;
        if (RonFilesClient.exists(path))
            RonFilesClient.delete(path);
        cancel();
    }

    // the popup hides while picking; what was picked is typed at the cursor
    private static void pick(CodePicker.Kind kind) {
        String line = code.lineBeforeCursor();
        CodePicker.start(kind, text -> {
            code.setFocused(true);
            if (text == null)
                return;
            boolean blankLine = line.isBlank();
            if (kind == CodePicker.Kind.HUD && !line.matches(".*\\bhighlight\\s+\\S+\\s+$"))
                text = "rtsapi highlight @s " + text;
            else if ((kind == CodePicker.Kind.ZONE || kind == CodePicker.Kind.CAMERA) && !blankLine)
                text = "\n" + text;
            else if (!line.isEmpty() && !line.endsWith(" ") && kind != CodePicker.Kind.ZONE && kind != CodePicker.Kind.CAMERA)
                text = " " + text;
            code.insert(text);
        });
    }

    private static void saveAndClose() {
        if (save()) {
            drafts.remove(path);
            path = null;
            code.setFocused(false);
        }
    }

    private static String fileName() {
        return "ron/" + path + ".mcfunction";
    }

    // the world's own folder: only on the world you're hosting
    private static void openExternal() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            Toast.show(I18n.get(KEY + "external_own_world"), Toast.DEFAULT_TICKS, Theme.ERROR);
            return;
        }
        Path file = RonFiles.dir(server).resolve(path + ".mcfunction");
        if (!Files.isRegularFile(file)) {
            Toast.show(I18n.get(KEY + "external_save_first"), Toast.DEFAULT_TICKS, Theme.ERROR);
            return;
        }
        Util.getPlatform().openFile(file.toFile());
    }

    // mirrors the server: new saved text replaces the editor's unless it has been edited since; pauses get checked
    private static void sync() {
        if (RonFilesClient.version() != seenVersion) {
            seenVersion = RonFilesClient.version();
            String saved = RonFilesClient.contents.get(path);
            if (saved != null && !saved.equals(base)) {
                if (!code.getText().equals(saved) && code.getText().equals(base != null ? base : ""))
                    code.setText(saved);
                base = saved;
                lastText = validated = code.getText();
            }
            code.setProblems(RonFilesClient.problems.getOrDefault(path, Map.of()));
        }
        long now = System.currentTimeMillis();
        String text = code.getText();
        if (!text.equals(lastText)) {
            lastText = text;
            editedMs = now;
        } else if (!text.equals(validated) && now - editedMs > VALIDATE_MS) {
            validated = text;
            RonFilesClient.validate(path, text);
        }
    }

    // ------------------------------------------------------------------ drawing

    /** Draws it over everything else in the frame; call it last. The custom building popup draws it itself when open. */
    public static void render(HudFrame f) {
        if (!CustomBuildingClientEvents.isMenuShown())
            draw(f);
        CodePicker.render(f);
    }

    /** From the custom building popup's own frame, after the popup, so it's on top of it. */
    public static void renderOverMenu(HudFrame f) {
        draw(f);
        CodePicker.render(f);
    }

    private static void draw(HudFrame f) {
        if (path == null || f.g == drawnOn || CodePicker.isPicking())
            return;
        drawnOn = f.g;
        drawnMs = System.currentTimeMillis();
        sync();

        int sw = f.layout.width(), sh = f.layout.height();
        f.g.fill(0, 0, sw, sh, 0x80000000);
        f.zone(0, 0, sw, sh); // nothing under it takes clicks
        int w = Math.min(W, sw - 2 * PAD), h = Math.min(H, sh - 2 * PAD);
        int x = (sw - w) / 2, y = (sh - h) / 2;
        Theme.popup(f.g, x, y, w, h, Theme.ORANGE);
        int cx = x + PAD + 2, cw = w - 2 * PAD - 4;

        drawHeader(f, cx, cw, y + PAD + 1);
        int top = y + PAD + 18;
        f.g.fill(x + 1, top - 3, x + w - 1, top - 2, Theme.INK);

        int footerY = y + h - PAD - BUTTON_H;
        Rect body = new Rect(cx, top + 2, cw, footerY - 7 - top);
        CodeBlocks.sync(code); // the Code view too: picking shows the file's zones
        if (blocks) {
            code.setFocused(false);
            CodeBlocks.render(f, code, body);
        } else
            code.render(f, body);
        f.g.fill(x + 1, footerY - 4, x + w - 1, footerY - 3, Theme.INK);

        drawFooter(f, cx, cw, footerY);
    }

    // one header row: the name, where it lives (the path typed in place), then saved, Code | Blocks and ✕
    private static void drawHeader(HudFrame f, int cx, int cw, int hy) {
        float ts = Text.px(18);
        Text.heading(f.g, Text.fit(title, cw / 3), cx, hy, Theme.GOLD, ts);
        int rx = cx + cw;
        Rect close = new Rect(rx - 11, hy, 11, 11);
        Theme.cross(f.g, close, f.hovered(close) ? Theme.GOLD : Theme.LABEL);
        f.register(new ButtonBuilder("code popup close").onLeftClick(CodeEditorPopup::close).build(), close, null,
                EditorRows.tip(I18n.get(KEY + "close"), I18n.get(KEY + "close_tooltip")));
        rx -= 11 + 8;
        rx -= viewToggle(f, rx, hy - 1) + 8;
        boolean dirty = isDirty();
        String status = I18n.get(KEY + (dirty ? "unsaved" : "saved_status"));
        int stw = Text.width(status);
        if (dirty)
            f.g.fill(rx - stw - 6, hy + 4, rx - stw - 3, hy + 7, Theme.YELLOW);
        Text.draw(f.g, status, rx - stw, hy + 2, dirty ? Theme.YELLOW : Theme.DIM);
        rx -= stw + 14;
        int px = cx + Math.min(Text.headingWidth(title, ts), cw / 3) + 10;
        Text.draw(f.g, "ron/", px, hy + 2, Theme.DIM);
        px += Text.width("ron/");
        boolean editing = pathBox.isFocused();
        int pw = Math.min(Math.max(30, Text.width(pathBox.getValue()) + 8), Math.max(30, (rx - px) / 2));
        Rect field = new Rect(px - 3, hy - 1, pw, 12);
        pathBox.setTextColor(editing ? Theme.TEXT : Theme.LABEL);
        placeBox(f, pathBox, field);
        if (editing || f.hovered(field))
            f.g.fill(field.x() + 3, field.bottom(), field.right() - 3, field.bottom() + 1, editing ? Theme.ORANGE : Theme.FAINT);
        Text.draw(f.g, Text.fit(".mcfunction · " + runsWhen, Math.max(0, rx - field.right())), field.right() - 3, hy + 2, Theme.DIM);
    }

    // the footer: Save, Run and ⋯ at the right; the world picker and the problems at the left
    private static void drawFooter(HudFrame f, int cx, int cw, int footerY) {
        int bx = cx + cw;
        bx = button(f, "save", I18n.get(KEY + "save"), bx, footerY, Style.PRIMARY, CodeEditorPopup::saveAndClose);
        bx = button(f, "run", "▶ " + I18n.get(KEY + "run"), bx, footerY, Style.SECONDARY, () -> RonFilesClient.run(path));
        Rect more = new Rect(bx - 20, footerY, 20, BUTTON_H);
        EditorRows.textButton(f, "code popup more", "⋯", more, Style.SECONDARY, () -> menu = menu == Menu.MORE ? Menu.NONE : Menu.MORE, null,
                EditorRows.tip(I18n.get(KEY + "more"), null));
        int lx = cx;
        Rect insert = Rect.EMPTY;
        if (blocks)
            Text.draw(f.g, Text.fit(I18n.get(KEY + "blocks_hint"), more.x() - lx - 8), lx, footerY + 4, Theme.DIM);
        else {
            String label = "◎ " + I18n.get(KEY + "insert") + " ▴";
            insert = new Rect(lx, footerY, Text.mediumWidth(label) + 14, BUTTON_H);
            EditorRows.textButton(f, "code popup insert", label, insert, Style.SECONDARY, () -> menu = menu == Menu.INSERT ? Menu.NONE : Menu.INSERT, null,
                    EditorRows.tip(I18n.get(KEY + "insert"), I18n.get(KEY + "insert_tooltip")));
            if (menu == Menu.INSERT)
                Theme.outline(f.g, insert.x(), insert.y(), insert.w(), insert.h(), 1, Theme.ORANGE);
            lx = insert.right() + 8;
        }
        Map<Integer, String> problems = RonFilesClient.problems.getOrDefault(path, Map.of());
        if (!problems.isEmpty() && !blocks)
            drawProblemsLink(f, problems, lx, footerY);

        if (menu == Menu.INSERT && !blocks)
            insertMenu(f, insert.x(), insert.y());
        else if (menu == Menu.MORE)
            moreMenu(f, more.right(), more.y());
        else
            menu = Menu.NONE;
    }

    private static void drawProblemsLink(HudFrame f, Map<Integer, String> problems, int lx, int footerY) {
        int first = problems.keySet().stream().min(Integer::compare).orElse(1);
        String text = problems.size() == 1 ? I18n.get(KEY + "problem_one", first) : I18n.get(KEY + "problem_many", problems.size(), first);
        Rect link = new Rect(lx, footerY, Text.width(text) + 4, BUTTON_H);
        Text.draw(f.g, text, lx, footerY + 4, f.hovered(link) ? Theme.ERROR : Theme.DANGER_TEXT);
        StringBuilder all = new StringBuilder();
        problems.forEach((line, msg) -> all.append(all.isEmpty() ? "" : "\n").append(I18n.get("editor.reignofnether.code.line", line))
                .append(": ").append(CodeEditor.friendly(msg)));
        f.register(new ButtonBuilder("code popup problems").build(), link, null, EditorRows.tip(I18n.get(KEY + "problems"), all.toString()));
    }

    private enum Menu { NONE, INSERT, MORE }

    private static Menu menu = Menu.NONE;

    // a borderless edit box placed in r, in HUD units like EditorRows.field but without its sunken box
    private static void placeBox(HudFrame f, MyEditBox box, Rect r) {
        f.zone(r);
        box.hudUnits = true;
        box.placed = true;
        box.setX(r.x() + 3);
        box.setY(r.centerY() - 4);
        box.setWidth(r.w() - 6);
    }

    // a list of choices above (x, bottomY); a click outside it closes it
    private record Item(String label, String hint, int colour, Runnable click) { }

    private static void menu(HudFrame f, List<Item> items, @Nullable String heading, int x, int bottomY, boolean alignRight) {
        int w = 0;
        for (Item i : items)
            w = Math.max(w, Text.width(i.label()) + (i.hint().isEmpty() ? 0 : Text.smallWidth(i.hint()) + 16) + 24);
        if (heading != null)
            w = Math.max(w, Text.smallWidth(heading) + 16);
        int h = items.size() * 15 + (heading != null ? 14 : 0) + 6;
        int mx = alignRight ? x - w : x, my = bottomY - h - 3;
        // the rest of the popup closes it
        f.register(new ButtonBuilder("code popup menu away").onLeftClick(() -> menu = Menu.NONE).onRightClick(() -> menu = Menu.NONE).build(),
                new Rect(0, 0, f.layout.width(), f.layout.height()), null, null);
        Rect r = new Rect(mx, my, w, h);
        f.zone(r);
        f.g.fill(mx, my, mx + w, my + h, Theme.RAISED);
        Theme.outline(f.g, mx, my, w, h, 1, Theme.CONSOLE_RIM);
        int iy = my + 3;
        if (heading != null) {
            Text.small(f.g, heading, mx + 8, iy + 3, Theme.DIM);
            iy += 14;
        }
        for (int k = 0; k < items.size(); k++) {
            Item item = items.get(k);
            Rect row = new Rect(mx + 3, iy, w - 6, 15);
            if (f.hovered(row))
                f.g.fill(row.x(), row.y(), row.right(), row.bottom(), Theme.BUTTON_HOVER);
            int tx = row.x() + 5;
            if (item.colour() != 0) {
                f.g.fill(tx, row.y() + 5, tx + 4, row.y() + 9, item.colour());
                tx += 10;
            }
            Text.draw(f.g, item.label(), tx, row.y() + 4, item.colour() == Theme.ERROR ? Theme.DANGER_TEXT : Theme.TEXT);
            if (!item.hint().isEmpty())
                Text.smallRight(f.g, item.hint(), row.right() - 5, row.y() + 5, Theme.DIM);
            f.register(new ButtonBuilder("code popup menu " + k).onLeftClick(() -> {
                menu = Menu.NONE;
                item.click().run();
            }).build(), row, null, null);
            iy += 15;
        }
    }

    private static final int[] PICK_COLOURS = { 0xFF8FE03C, 0xFF8FE03C, 0xFFF2C94C, 0xFFF2C94C, 0xFFFFB347, 0xFF5CC8FF };

    private static void insertMenu(HudFrame f, int x, int y) {
        List<Item> items = new ArrayList<>();
        CodePicker.Kind[] kinds = CodePicker.Kind.values();
        for (int k = 0; k < kinds.length; k++) {
            CodePicker.Kind kind = kinds[k];
            String id = kind.name().toLowerCase(Locale.ROOT);
            items.add(new Item(I18n.get("editor.reignofnether.pick.button." + id), I18n.get(KEY + "gives." + id), PICK_COLOURS[k], () -> pick(kind)));
        }
        menu(f, items, I18n.get(KEY + "insert_hint").toUpperCase(), x, y, false);
    }

    private static void moreMenu(HudFrame f, int rightX, int y) {
        menu(f, List.of(
                new Item(I18n.get(KEY + "external"), "", 0, CodeEditorPopup::openExternal),
                new Item(I18n.get(KEY + "discard"), "", 0, CodeEditorPopup::cancel),
                new Item(I18n.get(KEY + "delete"), "", Theme.ERROR, CodeEditorPopup::delete)), null, rightX, y, true);
    }

    // Code | Blocks, ending at rx: the same file either way; returns its width
    private static int viewToggle(HudFrame f, int rx, int y) {
        String[] labels = { I18n.get(KEY + "view_code"), I18n.get(KEY + "view_blocks") };
        int w0 = Text.width(labels[0]) + 12, w1 = Text.width(labels[1]) + 12;
        int x = rx - w0 - w1 - 2;
        f.g.fill(x, y, rx, y + 13, Theme.SUNKEN);
        Theme.outline(f.g, x, y, rx - x, 13, 1, Theme.INK);
        for (int i = 0; i < 2; i++) {
            boolean on = blocks == (i == 1);
            Rect r = new Rect(i == 0 ? x + 1 : x + 1 + w0, y + 1, i == 0 ? w0 : w1, 11);
            if (on)
                f.g.fill(r.x(), r.y(), r.right(), r.bottom(), Theme.ACTIVE);
            else if (f.hovered(r))
                f.g.fill(r.x(), r.y(), r.right(), r.bottom(), Theme.BUTTON_HOVER);
            Text.drawCentered(f.g, labels[i], r.centerX(), r.y() + 2, on ? Theme.GOLD : Theme.LABEL);
            boolean toBlocks = i == 1;
            f.register(new ButtonBuilder("code popup view " + i).onLeftClick(() -> {
                blocks = toBlocks;
                menu = Menu.NONE;
                code.setFocused(!toBlocks);
            }).build(), r, null, EditorRows.tip(labels[i], I18n.get(KEY + (toBlocks ? "view_blocks_tooltip" : "view_code_tooltip"))));
        }
        return rx - x;
    }

    // a footer button ending at rx; returns where the next one ends
    private static int button(HudFrame f, String key, String label, int rx, int y, Style style, Runnable click) {
        int w = Text.mediumWidth(label) + 18;
        EditorRows.textButton(f, "code popup " + key, label, new Rect(rx - w, y, w, BUTTON_H), style, click, null,
                EditorRows.tip(I18n.get(KEY + key), I18n.get(KEY + key + "_tooltip")));
        return rx - w - 4;
    }
}
