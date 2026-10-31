package com.solegendary.reignofnether.sandbox;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.MyEditBox;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.hud.panels.SettingRows;
import com.solegendary.reignofnether.hud.panels.SettingsMenu;
import com.solegendary.reignofnether.hud.panels.TopRightPanel;
import com.solegendary.reignofnether.scenario.ScenarioClientEvents;
import com.solegendary.reignofnether.scenario.ScenarioMenu;
import com.solegendary.reignofnether.scenario.ScenarioRole;
import com.solegendary.reignofnether.ui.EditorRows;
import com.solegendary.reignofnether.ui.EditorRows.Group;
import com.solegendary.reignofnether.ui.EditorRows.Row;
import com.solegendary.reignofnether.ui.EditorRows.Style;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.SettingRow;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The sandbox tools: a rail of tabs on the right with a sheet beside it, so the command card stays what it is in a
 * match. Nothing here has a hotkey, which leaves every play key free.
 */
public final class SandboxPanel {

    private SandboxPanel() { }

    private enum Tab {
        SPAWN(Icon.USERS), SELECTION(Icon.CHECK), RULES(Icon.LIST), SCENARIO(Icon.BOOK_OPEN);

        final Icon icon;

        Tab(Icon icon) {
            this.icon = icon;
        }

        String title() {
            return I18n.get(KEY + "tab." + name().toLowerCase(Locale.ROOT));
        }
    }

    private record Chip(String label, @Nullable ResourceLocation icon, boolean on, Runnable click) { }

    private static final String KEY = "sandbox.reignofnether.";
    private static final int PAD = 7;
    private static final int RAIL_PAD = 3;
    private static final int RAIL_EXIT_GAP = 8;
    private static final int CHIP_H = 15;
    private static final int CHIP_GAP = 3;
    private static final int FIELD_H = 14;
    private static final int TOOL_H = 14;
    private static final int ICON = 9;
    private static final int LIST_ICON = 11;
    private static final int EDIT_W = 40;
    // header, owner, role and one row of tools: the only tab too short to fill the dock
    private static final int SELECTION_H = PAD * 2 + EditorRows.HEADER_H + EditorRows.GROUP_H + CHIP_H + 4 + EditorRows.ROW_H
            + 4 + EditorRows.GROUP_H + TOOL_H;

    @Nullable
    private static Tab tab = Tab.SPAWN; // null: folded
    private static int scroll = 0;
    private static boolean overSheet = false;
    private static String query = "";
    private static MyEditBox searchBox = null;
    private static List<Group> rules = null;
    private static List<Row> spawnRows = null;
    private static long spawnRowsTick = -1;

    public static void reset() {
        tab = Tab.SPAWN;
        scroll = 0;
        overSheet = false;
        rules = null;
        spawnRows = null;
        if (searchBox != null)
            searchBox.setValue("");
        ScenarioClientEvents.setMenuOpen(false);
    }

    /** Opens a tab, or folds the sheet when null. Leaving the scenario tab saves the roles, as closing their editor did. */
    private static void show(@Nullable Tab next) {
        if (tab == Tab.SCENARIO && next != Tab.SCENARIO) {
            ScenarioClientEvents.confirmPublishScenario = false;
            ScenarioMenu.save();
        }
        tab = next;
        scroll = 0;
        rules = null;
    }

    public static void fold() {
        show(null);
    }

    /** True when the wheel should scroll the sheet instead of zooming the camera. */
    public static boolean takesWheel() {
        return overSheet && HudClientEvents.isMouseOverAnyButtonOrHud();
    }

    public static void scroll(int lines) {
        scroll = Math.max(0, scroll + lines);
    }

    public static void render(HudFrame f) {
        if (!SandboxClientEvents.isSandboxPlayer())
            return;
        if ((tab == Tab.SCENARIO) != ScenarioClientEvents.isMenuOpen())
            ScenarioClientEvents.setMenuOpen(tab == Tab.SCENARIO);
        Rect dock = f.layout.sandboxDock(TopRightPanel.bottom);
        renderRail(f, dock);
        overSheet = false;
        if (tab == null)
            return;

        Rect sheet = new Rect(dock.x(), dock.y(), HudLayout.DOCK_SHEET_W, tab == Tab.SELECTION ? SELECTION_H : dock.h());
        Theme.popup(f.g, sheet.x(), sheet.y(), sheet.w(), sheet.h(), Theme.ORANGE);
        f.zone(sheet);
        overSheet = tab != Tab.SELECTION && f.hovered(sheet);
        Rect content = new Rect(sheet.x() + PAD, sheet.y() + PAD, sheet.w() - PAD * 2, sheet.h() - PAD * 2);
        switch (tab) {
            case SPAWN -> renderSpawn(f, content);
            case SELECTION -> renderSelection(f, content);
            case RULES -> renderRules(f, content);
            case SCENARIO -> {
                ScenarioRole role = ScenarioClientEvents.getScenarioRoleToEdit();
                if (role != null)
                    scroll = ScenarioMenu.render(f, role, content, scroll);
            }
        }
    }

    private static void renderRail(HudFrame f, Rect dock) {
        int cell = HudLayout.TILE_SMALL;
        int count = Tab.values().length;
        int w = HudLayout.DOCK_RAIL_W;
        int h = RAIL_PAD * 2 + count * cell + (count - 1) * HudLayout.TOOL_GAP + RAIL_EXIT_GAP + cell;
        int x = dock.right() - w;
        Theme.panel(f.g, x, dock.y(), w, h, Theme.floating());
        f.zone(x, dock.y(), w, h);

        int cx = x + RAIL_PAD;
        int y = dock.y() + RAIL_PAD;
        int selectionY = y;
        for (Tab t : Tab.values()) {
            Rect r = new Rect(cx, y, cell, cell);
            boolean open = t == tab;
            Theme.cell(f.g, cx, y, cell, cell, open ? Theme.CellState.ACTIVE : Theme.CellState.READY, f.hovered(r));
            t.icon.drawIn(f.g, cx + 3, y + 3, cell - 6, cell - 6, open ? Theme.GOLD : Theme.BODY);
            f.register(new ButtonBuilder(t.title()).onLeftClick(() -> show(open ? null : t)).build(), r, null, () -> Tooltip.of(t.title()));
            if (t == Tab.SELECTION)
                selectionY = y;
            y += cell + HudLayout.TOOL_GAP;
        }
        int selected = UnitClientEvents.getSelectedUnits().size() + BuildingClientEvents.getSelectedBuildings().size();
        if (selected > 0)
            Theme.badge(f.g, String.valueOf(selected), cx + cell + 3, selectionY - 3);

        Rect exit = new Rect(cx, y - HudLayout.TOOL_GAP + RAIL_EXIT_GAP, cell, cell);
        Theme.dangerButton(f.g, exit.x(), exit.y(), cell, cell, f.hovered(exit));
        Theme.cross(f.g, exit, Theme.DANGER_TEXT);
        f.register(SandboxClientEvents.getExitSandboxButton(), exit, null,
                () -> Tooltip.of(I18n.get(KEY + "exit1")).danger().body(I18n.get(KEY + "exit2")));
    }

    // ------------------------------------------------------------------ shared pieces

    private static int groupLabel(HudFrame f, String label, int x, int y) {
        Text.small(f.g, label.toUpperCase(Locale.ROOT), x, y + 3, Theme.LABEL);
        return y + EditorRows.GROUP_H;
    }

    private static int chips(HudFrame f, List<Chip> chips, int x, int y, int w) {
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

    private static List<Chip> ownerChips(@Nullable Relationship current, java.util.function.Consumer<Relationship> set) {
        return List.of(
                new Chip(I18n.get("hud.relationship.reignofnether.owned"), wool(Relationship.OWNED), current == Relationship.OWNED, () -> set.accept(Relationship.OWNED)),
                new Chip(I18n.get("hud.relationship.reignofnether.neutral"), wool(Relationship.NEUTRAL), current == Relationship.NEUTRAL, () -> set.accept(Relationship.NEUTRAL)),
                new Chip(I18n.get("hud.relationship.reignofnether.enemy"), wool(Relationship.HOSTILE), current == Relationship.HOSTILE, () -> set.accept(Relationship.HOSTILE)));
    }

    // ------------------------------------------------------------------ spawn

    private static void renderSpawn(HudFrame f, Rect r) {
        int x = r.x();
        int w = r.w();
        int y = EditorRows.header(f, Tab.SPAWN.title(), x, r.y(), w, null, SandboxPanel::fold);

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

        Rect field = new Rect(x, y, w, FIELD_H);
        EditorRows.field(f, searchBox(), field);
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
        List<Row> rows = new ArrayList<>();
        for (Button b : buttons) {
            String name = displayName(b);
            if (!b.isHidden.get() && name.toLowerCase(Locale.ROOT).contains(query))
                rows.add(spawnRow(b, name, placing));
        }
        return rows;
    }

    private static String displayName(Button b) {
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
        int y = EditorRows.header(f, Tab.SELECTION.title(), x, r.y(), w, null, SandboxPanel::fold);
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

        ScenarioRole role = SandboxActionButtons.getHudSelectedScenarioRole();
        y = EditorRows.stepper(f, I18n.get(KEY + "scenario_role"), null,
                role != null ? role.name : I18n.get(KEY + "scenario_role_none"),
                () -> SandboxActionButtons.cycleUnitOrBuildingRoleIndex(false),
                () -> SandboxActionButtons.cycleUnitOrBuildingRoleIndex(true), x, y, w);
        y += 4;

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
    private static void tool(HudFrame f, Button b, String label, Rect r, boolean danger) {
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

    // ------------------------------------------------------------------ rules

    private static void renderRules(HudFrame f, Rect r) {
        int y = EditorRows.header(f, Tab.RULES.title(), r.x(), r.y(), r.w(), null, SandboxPanel::fold);
        if (rules == null)
            rules = ruleGroups(); // every value is a supplier, so the rows themselves never change
        scroll = EditorRows.renderScrolled(f, rules, new Rect(r.x(), y, r.w(), r.bottom() - y), scroll);
    }

    private static List<Group> ruleGroups() {
        List<Group> groups = new ArrayList<>();
        groups.add(new Group(I18n.get(KEY + "group.cheats"), List.of(
                cheat("unit_cheats", I18n.get(KEY + "unit_cheats1").trim() + " " + I18n.get(KEY + "unit_cheats2").trim(), SandboxClientEvents.UNIT_CHEATS),
                cheat("building_cheats", I18n.get(KEY + "building_cheats1"), SandboxClientEvents.BUILDING_CHEATS),
                cheat("control_cheat", I18n.get(KEY + "nonunit_control_cheat1"), SandboxClientEvents.CONTROL_CHEATS))));
        if (SettingsMenu.canOpenGamerules())
            groups.add(new Group(I18n.get("hud.gamerule.reignofnether.title"), ruleRows(GameruleClient.ruleRows())));
        return groups;
    }

    private static Row cheat(String key, String description, List<String> cheats) {
        String name = I18n.get(KEY + key);
        return (f, x, y, w) -> EditorRows.toggle(f, name, EditorRows.tip(name, description), SandboxClientEvents.hasCheats(cheats),
                () -> SandboxClientEvents.toggleCheats(cheats), x, y, w);
    }

    private static List<Row> ruleRows(List<SettingRow> rows) {
        List<Row> list = new ArrayList<>();
        for (SettingRow rule : rows)
            list.add((f, x, y, w) -> SettingRows.renderRow(f, rule, x, y, w));
        return list;
    }
}
