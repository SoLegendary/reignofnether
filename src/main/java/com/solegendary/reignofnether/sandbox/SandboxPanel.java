package com.solegendary.reignofnether.sandbox;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.editor.CodeEditorPopup;
import com.solegendary.reignofnether.editor.EditorHistory;
import com.solegendary.reignofnether.editor.EditorTest;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.MyEditBox;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.hud.panels.SettingRows;
import com.solegendary.reignofnether.hud.panels.TopRightPanel;
import com.solegendary.reignofnether.rtsmap.MapInfoMenu;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoClientEvents;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerEvents;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerboundPacket;
import com.solegendary.reignofnether.rtsmap.RonFilesClient;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.ui.EditorRows;
import com.solegendary.reignofnether.ui.EditorRows.Group;
import com.solegendary.reignofnether.ui.EditorRows.Row;
import com.solegendary.reignofnether.ui.EditorRows.Style;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MyRenderer;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * The sandbox tools and map editor: a rail of tabs on the right with a sheet beside it, so the command card stays
 * what it is in a match. Below the tabs, Test plays the map and the cross leaves the editor. The sheets that edit
 * the map's files are built in {@link MapSheets}. Nothing here has a hotkey (but Ctrl+Z / Ctrl+Y for undo), which
 * leaves every play key free.
 */
public final class SandboxPanel {

    private SandboxPanel() { }

    private enum Tab {
        SPAWN(Icon.USERS, false), SELECTION(Icon.CHECK, false), RULES(Icon.LIST, false), SEATS(Icon.BOOK_OPEN, true),
        MAP(Icon.MAP, true), SCRIPTS(Icon.SETTINGS, true);

        final Icon icon;
        final boolean opOnly;

        Tab(Icon icon, boolean opOnly) {
            this.icon = icon;
            this.opOnly = opOnly;
        }

        String title() {
            return I18n.get(KEY + "tab." + name().toLowerCase(Locale.ROOT));
        }

        // seats, map and scripts change the map's files in ron/ and the world border, so they're for ops only,
        // and an old or newer format map has none loaded (the banner says why)
        boolean shown() {
            return !opOnly || op() && RTSMapInfoClientEvents.isEditable();
        }
    }

    public record Chip(String label, @Nullable ResourceLocation icon, boolean on, Runnable click) { }

    private static final Minecraft MC = Minecraft.getInstance();
    private static final String KEY = "sandbox.reignofnether.";
    private static final int PAD = 7;
    private static final int RAIL_PAD = 3;
    private static final int RAIL_EXIT_GAP = 8;
    private static final int CHIP_H = 15;
    private static final int CHIP_GAP = 3;
    private static final int FIELD_H = 14;
    private static final int TOOL_H = 14;
    private static final int HEAD_BTN = 12;
    private static final int FOOTER_H = 14;
    private static final int BANNER_W = 500;
    private static final int ICON = 9;
    private static final int LIST_ICON = 11;
    private static final int EDIT_W = 40;
    // header, then owner and anchor, each under its label: the only tab too short to fill the dock
    private static final int SELECTION_H = PAD * 2 + EditorRows.HEADER_H + EditorRows.GROUP_H + CHIP_H + 4
            + EditorRows.GROUP_H + TOOL_H;

    @Nullable
    private static Tab tab = Tab.SPAWN; // null: folded
    private static int scroll = 0;
    private static boolean overSheet = false;
    private static String query = "";
    private static MyEditBox searchBox = null;
    private static List<Row> spawnRows = null;
    // heads only, several per row, for mapmakers who know the icons; a preference, so it outlives the world
    private static boolean spawnGrid = false;
    private static long spawnRowsTick = -1;
    private static boolean convertConfirm = false; // Convert was clicked once: the next click converts
    private static boolean hidden = false; // the top bar's editor button hides the whole dock, to see the map

    /** Under the top bar while editing: shows or hides the editor's dock (the mode button is the crafting table). */
    public static final Button toggleButton = new ButtonBuilder("Editor panel")
            .iconResource(Icon.EYE.texture)
            .isSelected(() -> !hidden)
            .isHidden(() -> !isEditing() || !RTSMapInfoClientEvents.isEditable())
            .onLeftClick(() -> hidden = !hidden)
            .tooltipLines(List.of(fcs(I18n.get(KEY + "toggle_panel"), true)))
            .build();

    public static void reset() {
        tab = Tab.SPAWN;
        scroll = 0;
        overSheet = false;
        spawnRows = null;
        convertConfirm = false;
        hidden = false;
        MapSheets.sub = null;
        MapSheets.rulesTab = MapSheets.RulesTab.MAP;
        if (searchBox != null)
            searchBox.setValue("");
    }

    private static boolean op() {
        return StartPosClientEvents.isHost();
    }

    /** Editing the map: a sandbox player who isn't playing a test match. */
    public static boolean isEditing() {
        return SandboxClientEvents.isSandboxPlayer() && !EditorTest.isTesting();
    }

    /** Opens a tab, or folds the sheet when null; always on its list, not a detail view. */
    private static void show(@Nullable Tab next) {
        tab = next;
        scroll = 0;
        MapSheets.sub = null;
        if (next == Tab.SCRIPTS) // files written by hand since
            RonFilesClient.refresh();
    }

    /** The Rules tab, from the pause menu's rules button. */
    public static void openRules() {
        hidden = false;
        show(Tab.RULES);
    }

    /** The Map tab, from map.json in the Scripts tab's file tree. */
    public static void openMap() {
        show(Tab.MAP);
    }

    public static void fold() {
        show(null);
    }

    static void scrollTop() {
        scroll = 0;
    }

    /** True when the wheel should scroll the sheet instead of zooming the camera (or nothing, under the code popup). */
    public static boolean takesWheel() {
        return CodeEditorPopup.isOpen() || overSheet && HudClientEvents.isMouseOverAnyButtonOrHud();
    }

    public static void scroll(int lines) {
        if (!CodeEditorPopup.isOpen())
            scroll = Math.max(0, scroll + lines);
    }

    // only what's saved is tested; the save packets reach the server before Test's
    private static void test() {
        if (MapInfoMenu.isDirty())
            MapInfoMenu.save();
        if (MapInfoMenu.isDirty()) // refused (too big), and it said why: nothing to test what's on screen with
            return;
        EditorTest.start();
    }

    public static void render(HudFrame f) {
        // an old or newer format map has nothing to edit: only its banner shows, with Convert
        if (!SandboxClientEvents.isSandboxPlayer() || hidden || !RTSMapInfoClientEvents.isEditable())
            return;
        EditorHistory.poll();
        if (tab != null && !tab.shown())
            show(null);
        Rect dock = f.layout.sandboxDock(TopRightPanel.bottom);
        renderRail(f, dock);
        overSheet = false;
        if (tab != null) {
            Rect sheet = new Rect(dock.x(), dock.y(), HudLayout.DOCK_SHEET_W, tab == Tab.SELECTION ? SELECTION_H : dock.h());
            Theme.popup(f.g, sheet.x(), sheet.y(), sheet.w(), sheet.h(), Theme.ORANGE);
            f.zone(sheet);
            overSheet = tab != Tab.SELECTION && f.hovered(sheet);
            Rect content = new Rect(sheet.x() + PAD, sheet.y() + PAD, sheet.w() - PAD * 2, sheet.h() - PAD * 2);
            switch (tab) {
                case SPAWN -> renderSpawn(f, content);
                case SELECTION -> renderSelection(f, content);
                case RULES -> renderSheet(f, content, MapSheets.rules(op()));
                case SEATS -> renderSheet(f, content, MapSheets.seats());
                case MAP -> renderSheet(f, content, MapSheets.map());
                case SCRIPTS -> renderSheet(f, content, MapSheets.scripts());
            }
        }
        CodeEditorPopup.render(f); // on top of everything else here
    }

    /**
     * At the top centre while editing: for a map in an old (v1) or newer format, what that means (with Convert for
     * a v1 map), and below it for a map that isn't RTS optimised, what it misses with a button that sets the border
     * to the limit.
     */
    public static void renderBanner(HudFrame f) {
        if (!SandboxClientEvents.isSandboxPlayer() || MC.level == null)
            return;
        int y = HudLayout.MARGIN + HudLayout.TOP_CARD_H + HudLayout.GAP;
        int format = RTSMapInfoClientEvents.mapFormat();
        if (format < RTSMapInfoServerEvents.SUPPORTED_FORMAT) {
            String convert = I18n.get(KEY + (convertConfirm ? "format.convert_confirm" : "format.convert"));
            y = banner(f, y, Theme.ORANGE, Theme.GOLD, I18n.get(KEY + "format.old_title"), I18n.get(KEY + "format.old_body"),
                    convert, () -> {
                        if (convertConfirm)
                            RTSMapInfoServerboundPacket.convertMap();
                        convertConfirm = !convertConfirm;
                    }, () -> convertConfirm = false, EditorRows.tip(convert, I18n.get(KEY + "format.convert_tooltip"))) + HudLayout.GAP;
        } else if (format > RTSMapInfoServerEvents.SUPPORTED_FORMAT) {
            y = banner(f, y, Theme.ERROR, Theme.DANGER_TEXT, I18n.get(KEY + "format.newer_title", format),
                    I18n.get(KEY + "format.newer_body"), null, null, null, null) + HudLayout.GAP;
        }
        if (!MapSheets.optimised()) {
            int max = WorldBorderServerEvents.RTS_OPTIMIZED_BORDER;
            banner(f, y, Theme.ERROR, Theme.DANGER_TEXT, I18n.get(KEY + "rts.title", MapInfoMenu.borderSize(), max),
                    I18n.get(KEY + "rts.body"), I18n.get(KEY + "rts.fix", max), () -> MapInfoMenu.fitBorder(max), null, null);
        }
    }

    // a banner with a "!" mark, a title, the wrapped body and an optional button (for ops only); returns its bottom
    private static int banner(HudFrame f, int y, int accent, int titleColour, String title, String body, @Nullable String button,
                              @Nullable Runnable click, @Nullable Runnable rightClick, @Nullable Supplier<Tooltip> tip) {
        int buttonW = button != null && op() ? Text.mediumWidth(button) + 14 : 0;
        int dockX = f.layout.sandboxDock(TopRightPanel.bottom).x();
        int w = Math.min(BANNER_W, dockX - HudLayout.GAP - HudLayout.MARGIN);
        int x = Math.max(HudLayout.MARGIN, Math.min((f.layout.width() - w) / 2, dockX - HudLayout.GAP - w));
        int mark = 16;
        int textX = x + PAD + mark + 6;
        int textW = x + w - PAD - (buttonW > 0 ? buttonW + 6 : 0) - textX;
        List<FormattedCharSequence> lines = Text.wrap(body, textW);
        int h = Math.max(mark, Text.lineHeight() * (lines.size() + 1) + 2) + PAD * 2;

        Theme.popup(f.g, x, y, w, h, accent);
        f.zone(x, y, w, h);
        int my = y + (h - mark) / 2;
        Theme.outline(f.g, x + PAD - 1, my - 1, mark + 2, mark + 2, 1, Theme.ON_ORANGE);
        f.g.fill(x + PAD, my, x + PAD + mark, my + mark, accent);
        Text.drawCentered(f.g, "!", x + PAD + mark / 2 + 1, my + 4, Theme.ON_ORANGE);
        int ty = y + PAD;
        Text.draw(f.g, Text.fit(title, textW), textX, ty, titleColour);
        ty += Text.lineHeight() + 2;
        for (FormattedCharSequence line : lines) {
            Text.draw(f.g, line, textX, ty, Theme.BODY);
            ty += Text.lineHeight();
        }
        if (buttonW > 0)
            EditorRows.textButton(f, "sandbox banner " + title, button, new Rect(x + w - PAD - buttonW, y + (h - TOOL_H) / 2, buttonW, TOOL_H),
                    Style.PRIMARY, click, rightClick, tip);
        return y + h;
    }

    private static void renderRail(HudFrame f, Rect dock) {
        int cell = HudLayout.TILE_SMALL;
        List<Tab> tabs = Arrays.stream(Tab.values()).filter(Tab::shown).toList();
        int count = tabs.size();
        int w = HudLayout.DOCK_RAIL_W;
        int h = RAIL_PAD * 2 + count * cell + (count - 1) * HudLayout.TOOL_GAP + RAIL_EXIT_GAP + cell * 2 + HudLayout.TOOL_GAP;
        int x = dock.right() - w;
        Theme.panel(f.g, x, dock.y(), w, h, Theme.floating());
        f.zone(x, dock.y(), w, h);

        int cx = x + RAIL_PAD;
        int y = dock.y() + RAIL_PAD;
        int selectionY = y;
        int mapY = -1;
        for (Tab t : tabs) {
            Rect r = new Rect(cx, y, cell, cell);
            boolean open = t == tab;
            Theme.cell(f.g, cx, y, cell, cell, open ? Theme.CellState.ACTIVE : Theme.CellState.READY, f.hovered(r));
            t.icon.drawIn(f.g, cx + 3, y + 3, cell - 6, cell - 6, open ? Theme.GOLD : Theme.BODY);
            f.register(new ButtonBuilder(t.title()).onLeftClick(() -> show(open ? null : t)).build(), r, null, () -> Tooltip.of(t.title()));
            if (t == Tab.SELECTION)
                selectionY = y;
            if (t == Tab.MAP)
                mapY = y;
            y += cell + HudLayout.TOOL_GAP;
        }
        int selected = UnitClientEvents.getSelectedUnits().size() + BuildingClientEvents.getSelectedBuildings().size();
        if (selected > 0)
            Theme.badge(f.g, String.valueOf(selected), cx + cell + 3, selectionY - 3);
        if (mapY >= 0 && !MapSheets.optimised())
            errorBadge(f, "!", cx + cell + 3, mapY - 3);

        Rect test = new Rect(cx, y - HudLayout.TOOL_GAP + RAIL_EXIT_GAP, cell, cell);
        boolean testHovered = f.hovered(test);
        Theme.primaryButton(f.g, test.x(), test.y(), cell, cell, testHovered);
        Text.drawCentered(f.g, "▶", test.centerX() + 1, test.centerY() - 4, Theme.ON_ORANGE);
        f.register(new ButtonBuilder("sandbox test").onLeftClick(SandboxPanel::test).build(), test, null,
                EditorRows.tip(I18n.get("editor.reignofnether.test"), I18n.get("editor.reignofnether.test_tooltip")));

        // hides the editor, the top bar's editor button brings it back; leaving the Editor is the mode button's
        Rect exit = new Rect(cx, test.bottom() + HudLayout.TOOL_GAP, cell, cell);
        Theme.dangerButton(f.g, exit.x(), exit.y(), cell, cell, f.hovered(exit));
        Theme.cross(f.g, exit, Theme.DANGER_TEXT);
        f.register(new ButtonBuilder("sandbox hide").onLeftClick(() -> hidden = true).build(), exit, null,
                () -> Tooltip.of(I18n.get(KEY + "hide1")).body(I18n.get(KEY + "hide2")));
    }

    // a red badge like Theme.badge, for something to fix
    private static void errorBadge(HudFrame f, String text, int rightX, int topY) {
        int w = Math.max(9, Text.smallWidth(text) + 5);
        int x = rightX - w;
        Theme.outline(f.g, x - 1, topY - 1, w + 2, 11, 1, Theme.ON_ORANGE);
        f.g.fill(x, topY, x + w, topY + 9, Theme.ERROR);
        Text.small(f.g, text, x + (w - Text.smallWidth(text) + 1) / 2, topY, Theme.ON_ORANGE);
    }

    // ------------------------------------------------------------------ sheet frame

    /** Header, the file it's saved in, the rows (scrolled by the wheel) and, for map data, the Save footer. */
    private static void renderSheet(HudFrame f, Rect r, MapSheets.Sheet sheet) {
        int y = header(f, sheet.title(), sheet.nav(), sheet.back(), r.x(), r.y(), r.w());
        if (!sheet.file().isEmpty()) {
            Text.draw(f.g, Text.fit(sheet.file(), r.w()), r.x(), y - 6, Theme.DIM);
            y += 6;
        }
        int bottom = sheet.save() ? r.bottom() - FOOTER_H - 4 : r.bottom();
        scroll = EditorRows.renderScrolled(f, sheet.groups(), new Rect(r.x(), y, r.w(), bottom - y), scroll);
        if (sheet.save())
            footer(f, new Rect(r.x(), r.bottom() - FOOTER_H, r.w(), FOOTER_H));
    }

    /** EditorRows.header's title and cross, with undo, and in a detail view ◀ ▶ to step through its list and back. */
    private static int header(HudFrame f, String title, @Nullable IntConsumer nav, boolean back, int x, int y, int w) {
        int b = HEAD_BTN;
        int rx = x + w - b;
        headerButton(f, "close", null, new Rect(rx, y, b, b), true, SandboxPanel::fold, null);
        rx -= b + 3;
        headerButton(f, "undo", Theme.ART_UNDO, new Rect(rx, y, b, b), EditorHistory.canUndo(), EditorHistory::undo,
                () -> Tooltip.of(I18n.get("editor.reignofnether.undo")).body(I18n.get("editor.reignofnether.undo_tooltip"))
                        .key(I18n.get("editor.reignofnether.undo_key")));
        if (back) {
            rx -= b + 3;
            headerButton(f, "back", Theme.ART_BACK, new Rect(rx, y, b, b), true, () -> MapSheets.open(null, ""),
                    () -> Tooltip.of(I18n.get(KEY + "back")));
        }
        int tx = x;
        if (nav != null) {
            rx -= b + 3;
            headerButton(f, "next", Theme.ART_NEXT, new Rect(rx, y, b, b), true, () -> nav.accept(1), null);
            headerButton(f, "previous", Theme.ART_PREVIOUS, new Rect(x, y, b, b), true, () -> nav.accept(-1), null);
            tx += b + 4;
        }
        float scale = Text.px(18);
        int room = rx - 4 - tx;
        String shown = title;
        for (int cut = title.length(); cut > 0 && Text.headingWidth(shown, scale) > room; )
            shown = title.substring(0, --cut) + "…";
        Text.heading(f.g, shown, tx, y + 2, Theme.GOLD, scale);
        return y + EditorRows.HEADER_H;
    }

    // art null: the close cross
    private static void headerButton(HudFrame f, String id, @Nullable String[] art, Rect r, boolean enabled,
                                     Runnable click, @Nullable Supplier<Tooltip> tip) {
        boolean hovered = enabled && f.hovered(r);
        Theme.secondaryButton(f.g, r.x(), r.y(), r.w(), r.h(), hovered);
        int colour = !enabled ? Theme.FAINT : hovered ? Theme.GOLD : Theme.BODY;
        if (art != null)
            Theme.pixelArt(f.g, r, art, colour);
        else
            Theme.cross(f.g, r, colour);
        f.register(new ButtonBuilder("sandbox sheet " + id).onLeftClick(click).build(), r, null, tip);
    }

    // "● unsaved changes" or "saved", and Save: map.json and the restrictions
    private static void footer(HudFrame f, Rect r) {
        boolean dirty = MapInfoMenu.isDirty();
        f.g.fill(r.x(), r.y() - 3, r.right(), r.y() - 2, Theme.INK);
        Text.draw(f.g, I18n.get(KEY + (dirty ? "unsaved" : "saved")), r.x(), r.y() + 3, dirty ? Theme.YELLOW : Theme.LABEL);
        String save = I18n.get("sandbox.reignofnether.map.save");
        int bw = Text.mediumWidth(save) + 24;
        EditorRows.textButton(f, "sandbox save", save, new Rect(r.right() - bw, r.y(), bw, r.h()),
                dirty ? Style.PRIMARY : Style.SECONDARY, MapInfoMenu::save, null,
                EditorRows.tip(save, I18n.get("sandbox.reignofnether.map.save_tooltip")));
    }

    // ------------------------------------------------------------------ shared pieces

    private static int groupLabel(HudFrame f, String label, int x, int y) {
        Text.small(f.g, label.toUpperCase(Locale.ROOT), x, y + 3, Theme.LABEL);
        return y + EditorRows.GROUP_H;
    }

    public static int chips(HudFrame f, List<Chip> chips, int x, int y, int w) {
        int chipW = (w - (chips.size() - 1) * CHIP_GAP) / chips.size();
        for (int i = 0; i < chips.size(); i++) {
            Chip c = chips.get(i);
            Rect r = new Rect(x + i * (chipW + CHIP_GAP), y, chipW, CHIP_H);
            Theme.chip(f.g, r.x(), r.y(), r.w(), r.h(), c.on(), f.hovered(r));
            int iconW = c.icon() != null ? ICON + 3 : 0;
            String label = Text.fit(c.label(), r.w() - iconW - 4);
            int tx = r.centerX() - (iconW + Text.width(label)) / 2;
            if (c.icon() != null)
                MyRenderer.renderIcon(f.g, c.icon(), tx, r.y() + (CHIP_H - ICON) / 2, ICON);
            Text.draw(f.g, label, tx + iconW, r.y() + 4, c.on() ? Theme.TEXT : Theme.BODY);
            f.register(new ButtonBuilder(c.label()).onLeftClick(c.click()).build(), r, null, null);
        }
        return y + CHIP_H + 4;
    }

    private static ResourceLocation wool(Relationship owner) {
        String colour = switch (owner) {
            case NEUTRAL -> "yellow";
            case HOSTILE -> "red";
            default -> "lime";
        };
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/" + colour + "_wool.png");
    }

    public static List<Chip> ownerChips(@Nullable Relationship current, Consumer<Relationship> set) {
        return List.of(
                new Chip(I18n.get("hud.relationship.reignofnether.owned"), wool(Relationship.OWNED), current == Relationship.OWNED, () -> set.accept(Relationship.OWNED)),
                new Chip(I18n.get("hud.relationship.reignofnether.neutral"), wool(Relationship.NEUTRAL), current == Relationship.NEUTRAL, () -> set.accept(Relationship.NEUTRAL)),
                new Chip(I18n.get("hud.relationship.reignofnether.enemy"), wool(Relationship.HOSTILE), current == Relationship.HOSTILE, () -> set.accept(Relationship.HOSTILE)));
    }

    // ------------------------------------------------------------------ spawn

    private static void renderSpawn(HudFrame f, Rect r) {
        int x = r.x();
        int w = r.w();
        int y = header(f, Tab.SPAWN.title(), null, false, x, r.y(), w);

        y = groupLabel(f, I18n.get(KEY + "group.owner"), x, y);
        y = chips(f, ownerChips(SandboxClientEvents.relationship, owner -> SandboxClientEvents.relationship = owner), x, y, w);

        y = groupLabel(f, I18n.get(KEY + "group.faction"), x, y);
        List<Chip> factions = new ArrayList<>();
        for (ResourceLocation rl : Factions.PLAYABLE_FACTIONS)
            factions.add(new Chip(I18n.get(String.format("hud.faction.%s.%s", rl.getNamespace(), rl.getPath())), null,
                    SandboxClientEvents.getFaction() == Factions.getFaction(rl), () -> {
                        SandboxClientEvents.setFaction(Factions.getFaction(rl));
                        listChanged();
                    }));
        y = chips(f, factions, x, y, w);

        y = groupLabel(f, I18n.get(KEY + "group.type"), x, y);
        y = chips(f, List.of(typeChip(SandboxMenuType.UNITS, "tab.units"), typeChip(SandboxMenuType.BUILDINGS, "tab.buildings"),
                typeChip(SandboxMenuType.CUSTOM_BUILDINGS, "tab.custom")), x, y, w);

        y = groupLabel(f, I18n.get(KEY + "group.cheats"), x, y);
        y = chips(f, List.of(cheatChip("cheat.units", SandboxClientEvents.UNIT_CHEATS),
                cheatChip("cheat.buildings", SandboxClientEvents.BUILDING_CHEATS),
                cheatChip("cheat.control", SandboxClientEvents.CONTROL_CHEATS)), x, y, w);

        Rect field = new Rect(x, y, w - FIELD_H - 3, FIELD_H);
        EditorRows.field(f, searchBox(), field);
        Rect toggle = new Rect(field.right() + 3, y, FIELD_H, FIELD_H);
        Theme.button(f.g, toggle.x(), toggle.y(), toggle.w(), toggle.h(), f.hovered(toggle));
        int iconColour = f.hovered(toggle) ? Theme.GOLD : Theme.BODY;
        if (spawnGrid) // shows what a click switches to: names, or heads only
            Icon.LIST.drawIn(f.g, toggle.x(), toggle.y(), toggle.w(), toggle.h(), iconColour);
        else
            for (int i = 0; i < 4; i++)
                f.g.fill(toggle.x() + 3 + i % 2 * 5, toggle.y() + 3 + i / 2 * 5, toggle.x() + 7 + i % 2 * 5, toggle.y() + 7 + i / 2 * 5, iconColour);
        f.register(new ButtonBuilder("spawn view").onLeftClick(() -> {
            spawnGrid = !spawnGrid;
            listChanged();
        }).build(), toggle, null, () -> Tooltip.of(I18n.get(KEY + (spawnGrid ? "view_list" : "view_grid"))));
        if (query.isEmpty() && !searchBox.isFocused())
            Text.draw(f.g, I18n.get(KEY + "search"), x + 3, field.centerY() - 4, Theme.DIM);
        y += FIELD_H + 3;

        if (SandboxClientEvents.sandboxMenuType == SandboxMenuType.CUSTOM_BUILDINGS) {
            y = EditorRows.stepper(f, I18n.get(KEY + "sort_by"), null,
                    I18n.get(KEY + "sort." + SandboxClientEvents.customBuildingSortOption.name().toLowerCase(Locale.ROOT)),
                    () -> cycleSort(false), () -> cycleSort(true), x, y, w);
        }

        long tick = System.currentTimeMillis() / 50; // the custom building buttons are built anew each time
        if (spawnRows == null || tick != spawnRowsTick) {
            spawnRows = buildSpawnRows();
            spawnRowsTick = tick;
        }
        scroll = EditorRows.renderScrolled(f, List.of(new Group(null, spawnRows)), new Rect(x, y, w, r.bottom() - y), scroll);
    }

    private static Chip typeChip(SandboxMenuType type, String key) {
        return new Chip(I18n.get(KEY + key), null, SandboxClientEvents.sandboxMenuType == type, () -> {
            SandboxClientEvents.sandboxMenuType = type;
            listChanged();
        });
    }

    private static void cycleSort(boolean reverse) {
        SandboxClientEvents.cycleCustomBuildingSort(reverse);
        spawnRows = null;
    }

    private static void listChanged() {
        scroll = 0;
        spawnRows = null;
    }

    private static MyEditBox searchBox() {
        if (searchBox == null) {
            // placed every frame by EditorRows.field
            searchBox = new MyEditBox.Builder(0, 0, 120, 9)
                    .bordered(false)
                    .maxLength(32)
                    .responder(value -> {
                        query = value.toLowerCase(Locale.ROOT);
                        listChanged();
                    })
                    .build();
            TextInputClientEvents.registerEditBox(searchBox);
        }
        return searchBox;
    }

    private static List<Row> buildSpawnRows() {
        boolean placing = SandboxClientEvents.sandboxMenuType != SandboxMenuType.UNITS;
        List<? extends Button> buttons = switch (SandboxClientEvents.sandboxMenuType) {
            case UNITS -> SandboxClientEvents.getUnitButtons();
            case BUILDINGS -> SandboxClientEvents.getBuildingButtons();
            case CUSTOM_BUILDINGS -> SandboxClientEvents.getCustomBuildingButtons();
        };
        List<Button> shown = new ArrayList<>();
        for (Button b : buttons)
            if (!b.isHidden.get() && displayName(b).toLowerCase(Locale.ROOT).contains(query))
                shown.add(b);
        List<Row> rows = new ArrayList<>();
        if (!spawnGrid) {
            for (Button b : shown)
                rows.add(spawnRow(b, displayName(b), placing));
            return rows;
        }
        int perRow = (HudLayout.DOCK_SHEET_W - PAD * 2 - 4 + GRID_GAP) / (GRID_CELL + GRID_GAP);
        for (int i = 0; i < shown.size(); i += perRow) {
            List<Button> line = shown.subList(i, Math.min(shown.size(), i + perRow));
            rows.add((f, x, y, w) -> {
                for (int j = 0; j < line.size(); j++)
                    spawnCell(f, line.get(j), placing, x + j * (GRID_CELL + GRID_GAP), y);
            });
        }
        return rows;
    }

    private static final int GRID_CELL = EditorRows.ROW_H - 1;
    private static final int GRID_GAP = 2;

    // a head in the grid: the name is its tooltip, right-click edits a custom building like the list's Edit
    private static void spawnCell(HudFrame f, Button b, boolean placing, int x, int y) {
        Runnable click = !placing ? b.onLeftClick : () -> {
            UnitClientEvents.clearSelectedUnits();
            b.onLeftClick.run();
        };
        Rect r = new Rect(x, y, GRID_CELL, GRID_CELL);
        boolean on = b.isSelected.get();
        Theme.cell(f.g, x, y, GRID_CELL, GRID_CELL, on ? Theme.CellState.ACTIVE : b.isEnabled.get() ? Theme.CellState.READY : Theme.CellState.LOCKED, f.hovered(r));
        if (b.iconResource != null)
            MyRenderer.renderIcon(f.g, b.iconResource, x + 2, y + 2, GRID_CELL - 4);
        f.register(new ButtonBuilder(b.name).onLeftClick(click).onRightClick(b.onRightClick).isEnabled(b.isEnabled).build(), r, null,
                () -> Tooltip.fromLines(b.tooltipLines));
    }

    public static String displayName(Button b) {
        if (b.tooltipLines == null || b.tooltipLines.isEmpty())
            return b.name;
        StringBuilder sb = new StringBuilder();
        FormattedCharSequence title = b.tooltipLines.get(0);
        title.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    private static Row spawnRow(Button b, String name, boolean placing) {
        // a placement goes to the selected unit's owner and workers, not the owner chip: start from an empty selection
        Runnable click = !placing ? b.onLeftClick : () -> {
            UnitClientEvents.clearSelectedUnits();
            b.onLeftClick.run();
        };
        Button row = new ButtonBuilder(b.name).onLeftClick(click).onRightClick(b.onRightClick).isEnabled(b.isEnabled).build();
        return (f, x, y, w) -> {
            int h = EditorRows.ROW_H;
            Rect r = new Rect(x, y, w, h);
            boolean on = b.isSelected.get();
            f.g.fill(x, y + h - 1, x + w, y + h, Theme.RAISED);
            if (on) {
                f.g.fill(x, y, x + w, y + h, Theme.ACTIVE);
                Theme.ring(f.g, x, y, w, h, 1, Theme.ORANGE);
            } else if (f.hovered(r)) {
                f.g.fill(x, y, x + w, y + h, Theme.HOVER);
            }
            if (b.iconResource != null)
                MyRenderer.renderIcon(f.g, b.iconResource, x + 1, y + (h - LIST_ICON) / 2, LIST_ICON);
            Text.draw(f.g, Text.fit(name, w - LIST_ICON - EDIT_W - 8), x + LIST_ICON + 4, y + 4,
                    !b.isEnabled.get() ? Theme.MUTED : on ? Theme.GOLD : Theme.TEXT);
            f.register(row, r, null, () -> Tooltip.fromLines(b.tooltipLines));
            if (b.onRightClick != null) // custom buildings open their editor with it
                EditorRows.textButton(f, b.name + " edit", I18n.get(KEY + "custom_buildings.edit"),
                        new Rect(x + w - EDIT_W, y + 2, EDIT_W, h - 4), Style.SECONDARY, b.onRightClick, null, null);
        };
    }

    // ------------------------------------------------------------------ selection

    private static void renderSelection(HudFrame f, Rect r) {
        int x = r.x();
        int w = r.w();
        int y = header(f, Tab.SELECTION.title(), null, false, x, r.y(), w);
        BuildingPlacement building = HudClientEvents.hudSelectedPlacement;
        boolean unit = HudClientEvents.hudSelectedEntity instanceof Unit;
        if (building == null && !unit) {
            for (FormattedCharSequence line : Text.wrap(I18n.get(KEY + "selection.empty"), w)) {
                Text.draw(f.g, line, x, y, Theme.DESC);
                y += Text.lineHeight();
            }
            return;
        }
        y = groupLabel(f, I18n.get(KEY + "group.owner"), x, y);
        y = chips(f, ownerChips(SandboxActionButtons.getSelectionRelationship(), SandboxActionButtons::setSelectionOwner), x, y, w);

        if (building != null) {
            tool(f, SandboxActionButtons.removeBuildingPlacement, I18n.get(KEY + "anchor.remove"), new Rect(x, y, w, TOOL_H), true);
            return;
        }
        y = groupLabel(f, I18n.get(KEY + "group.anchor"), x, y);
        int toolW = (w - 2 * CHIP_GAP) / 3;
        tool(f, SandboxActionButtons.setAnchor, I18n.get(KEY + "anchor.set"), new Rect(x, y, toolW, TOOL_H), false);
        tool(f, SandboxActionButtons.resetToAnchor, I18n.get(KEY + "anchor.reset"), new Rect(x + toolW + CHIP_GAP, y, toolW, TOOL_H), false);
        tool(f, SandboxActionButtons.removeAnchor, I18n.get(KEY + "anchor.remove"), new Rect(x + 2 * (toolW + CHIP_GAP), y, toolW, TOOL_H), false);
    }

    /** One of the sandbox action buttons as a labelled button; it keeps its own click, state and tooltip. */
    public static void tool(HudFrame f, Button b, String label, Rect r, boolean danger) {
        boolean enabled = b.isEnabled.get();
        boolean hovered = enabled && f.hovered(r);
        boolean on = b.isSelected.get();
        if (danger)
            Theme.dangerButton(f.g, r.x(), r.y(), r.w(), r.h(), hovered);
        else if (on)
            Theme.chip(f.g, r.x(), r.y(), r.w(), r.h(), true, hovered);
        else
            Theme.button(f.g, r.x(), r.y(), r.w(), r.h(), hovered);
        int colour = !enabled ? Theme.FAINT : danger ? Theme.DANGER_TEXT : on || hovered ? Theme.GOLD : Theme.BODY;
        Text.mediumCentered(f.g, Text.fit(label, r.w() - 4), r.centerX(), r.y() + (r.h() - 8) / 2, colour);
        f.register(b, r, null, () -> Tooltip.fromLines(b.tooltipLines));
    }

    // a sandbox cheat set, on the Spawn tab beside what it applies to
    private static Chip cheatChip(String key, List<String> cheats) {
        return new Chip(I18n.get(KEY + key), null, SandboxClientEvents.hasCheats(cheats), () -> SandboxClientEvents.toggleCheats(cheats));
    }

}
