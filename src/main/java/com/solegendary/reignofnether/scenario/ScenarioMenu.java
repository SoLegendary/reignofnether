package com.solegendary.reignofnether.scenario;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.MyEditBox;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.sandbox.SandboxPanel;
import com.solegendary.reignofnether.ui.EditorRows;
import com.solegendary.reignofnether.ui.EditorRows.Group;
import com.solegendary.reignofnether.ui.EditorRows.Row;
import com.solegendary.reignofnether.ui.EditorRows.Style;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.scenario.ScenarioClientEvents.getScenarioRoleToEdit;

/** The scenario role editor, the sandbox dock's scenario tab: names and sets up each role in turn. */
public class ScenarioMenu {

    private static final String KEY = "sandbox.reignofnether.scenario.";
    private static final int FOOTER_H = 14;

    private static MyEditBox roleNameInput = registerRoleNameInput();

    public static void reregisterRoleNameInput() {
        unregisterRoleNameInput();
        registerRoleNameInput();
    }

    public static MyEditBox registerRoleNameInput() {
        if (roleNameInput == null) {
            ScenarioRole role = ScenarioClientEvents.getScenarioRoleToEdit();
            if (role != null) {
                // placed every frame by EditorRows.field
                MyEditBox box = new MyEditBox.Builder(0, 0, 120, 9)
                        .bordered(false)
                        .maxLength(32)
                        .value(role.name)
                        .responder(value -> role.name = value)
                        .onDefocus(value -> ScenarioServerboundPacket.setRoleName(role.index, value))
                        .build();
                TextInputClientEvents.registerEditBox(box);
                roleNameInput = box;
            }
        }
        return roleNameInput;
    }

    public static void unregisterRoleNameInput() {
        TextInputClientEvents.deregisterEditBox(roleNameInput);
        roleNameInput = null;
    }

    /** Draws the editor for this role in the dock's scenario tab. Returns the scroll actually used. */
    public static int render(HudFrame f, ScenarioRole role, Rect r, int scroll) {
        int x = r.x();
        int y = r.y();
        renderHeader(f, role, x, y, r.w());
        y += EditorRows.HEADER_H;

        // above the scrolled rows: an edit box can't be cut off
        int nameH = Math.max(EditorRows.ROW_H, EditorRows.fieldHeight(f) + 4);
        Rect field = EditorRows.label(f, I18n.get(KEY + "name"), null, x, y + (nameH - EditorRows.ROW_H) / 2, r.w(), 120);
        if (roleNameInput != null)
            EditorRows.field(f, roleNameInput, new Rect(field.x(), y + 2, field.w(), nameH - 4));
        y += nameH + 4;

        String error = ScenarioClientEvents.getPublishError();
        int footerY = r.bottom() - FOOTER_H;
        int bottom = footerY - 6;
        if (error != null) {
            bottom -= Theme.bannerHeight(error, r.w());
            Theme.banner(f.g, x, bottom, r.w(), error, Theme.ORANGE_DARK);
            bottom -= 4;
        }
        int shown = EditorRows.renderScrolled(f, groups(role), new Rect(x, y, r.w(), bottom - y), scroll);
        renderFooter(f, x, footerY, r.w(), error == null);
        return shown;
    }

    /** ◀ Role N ▶ chips, help and close. */
    private static void renderHeader(HudFrame f, ScenarioRole role, int x, int y, int w) {
        EditorRows.header(f, "", x, y, w, ScenarioMenu::helpTooltip, SandboxPanel::fold);
        String title = I18n.get(KEY + "role_label") + " " + (role.index + 1);
        Rect back = new Rect(x, y, 12, 12);
        int titleW = Text.headingWidth(title, Text.px(18));
        Rect forward = new Rect(x + 18 + titleW + 6, y, 12, 12);
        chip(f, "Cycle Role Backward", "◀", back, () -> ScenarioClientEvents.cycleRoleIndexToEdit(true));
        Text.heading(f.g, title, x + 18, y + 2, Theme.GOLD, Text.px(18));
        chip(f, "Cycle Role Forward", "▶", forward, () -> ScenarioClientEvents.cycleRoleIndexToEdit(false));
    }

    private static void chip(HudFrame f, String id, String glyph, Rect r, Runnable run) {
        boolean hovered = f.hovered(r);
        Theme.chip(f.g, r.x(), r.y(), r.w(), r.h(), false, hovered);
        Text.drawCentered(f.g, glyph, r.centerX() + 1, r.y() + 2, hovered ? Theme.GOLD : Theme.BODY);
        f.register(new ButtonBuilder(id).onLeftClick(run).build(), r, null, null);
    }

    private static Tooltip helpTooltip() {
        String body = String.join(" ", I18n.get(KEY + "help_tooltip1").trim(), I18n.get(KEY + "help_tooltip2").trim(),
                I18n.get(KEY + "help_tooltip3").trim());
        return Tooltip.of(I18n.get(KEY + "help_title")).body(body);
    }

    public static void save() {
        ScenarioServerboundPacket.saveScenario();
        HudClientEvents.showTemporaryMessage(I18n.get(KEY + "saved_all_roles"));
    }

    /** Reset (shift+ctrl+right click, so it can't happen by accident) and Publish, which asks to confirm. */
    private static void renderFooter(HudFrame f, int x, int y, int w, boolean canPublish) {
        EditorRows.textButton(f, "Reset Scenario Role", I18n.get(KEY + "reset_role.label"), new Rect(x, y, 70, FOOTER_H), Style.DANGER,
                null, ScenarioMenu::resetRole,
                () -> Tooltip.of(I18n.get(KEY + "reset_role.tooltip1")).danger().body(I18n.get(KEY + "reset_role.tooltip2")));
        boolean confirm = ScenarioClientEvents.confirmPublishScenario;
        String publish = "sandbox.reignofnether.publish_scenario_tooltip";
        EditorRows.textButton(f, "Publish Scenario Map", I18n.get(confirm ? publish + "_confirm" : "sandbox.reignofnether.publish"),
                new Rect(x + w - 80, y, 80, FOOTER_H), canPublish ? Style.PRIMARY : Style.SECONDARY,
                canPublish ? ScenarioClientEvents::pressedPublishScenarioButton : null,
                () -> ScenarioClientEvents.confirmPublishScenario = false,
                () -> confirm ? Tooltip.of(I18n.get(publish + "1")).body(I18n.get(publish + "_cancel"))
                        : Tooltip.of(I18n.get(publish + "1")).body(I18n.get(publish + "2")).body(I18n.get(publish + "3")));
    }

    private static void resetRole() {
        if (!Keybindings.shiftMod.isDown() || !Keybindings.ctrlMod.isDown())
            return;
        ScenarioRole role = getScenarioRoleToEdit();
        if (role != null) {
            role.faction = Factions.NEUTRAL;
            ScenarioServerboundPacket.setRoleFaction(role.index, Factions.NEUTRAL);
            role.startingResources.food = 0;
            ScenarioServerboundPacket.setStartingResources(role.index, ResourceName.FOOD, 0);
            role.startingResources.wood = 0;
            ScenarioServerboundPacket.setStartingResources(role.index, ResourceName.WOOD, 0);
            role.startingResources.ore = 0;
            ScenarioServerboundPacket.setStartingResources(role.index, ResourceName.ORE, 0);
            role.teamNumber = role.index + 1;
            ScenarioServerboundPacket.setTeamNumber(role.index, role.teamNumber);
            role.isNpc = false;
            ScenarioServerboundPacket.setRoleIsNpc(role.index, false);
            role.name = "Player " + (role.index + 1);
            ScenarioServerboundPacket.setRoleName(role.index, role.name);
        }
    }

    private static List<Group> groups(ScenarioRole role) {
        return List.of(
            new Group(null, List.of(
                (f, x, y, w) -> EditorRows.stepper(f, null, I18n.get(KEY + "faction"), EditorRows.tip(role.faction.getName(), null), "", role.faction.icon,
                    () -> {
                        role.faction = Factions.getFaction(MiscUtil.getNextItem(Factions.PLAYABLE_FACTIONS, role.faction.key));
                        ScenarioServerboundPacket.setRoleFaction(role.index, role.faction);
                    },
                    () -> {
                        role.faction = Factions.getFaction(MiscUtil.getLastItem(Factions.PLAYABLE_FACTIONS, role.faction.key));
                        ScenarioServerboundPacket.setRoleFaction(role.index, role.faction);
                    }, x, y, w),
                (f, x, y, w) -> EditorRows.stepper(f, I18n.get(KEY + "team"),
                    EditorRows.tip(I18n.get(KEY + "team"), I18n.get(KEY + "team_number_tooltip")), String.valueOf(role.teamNumber),
                    () -> {
                        role.teamNumber += 1;
                        if (role.teamNumber > ScenarioClientEvents.scenarioRoles.size())
                            role.teamNumber = 1;
                        ScenarioServerboundPacket.setTeamNumber(role.index, role.teamNumber);
                    },
                    () -> {
                        role.teamNumber -= 1;
                        if (role.teamNumber < 1)
                            role.teamNumber = ScenarioClientEvents.scenarioRoles.size();
                        ScenarioServerboundPacket.setTeamNumber(role.index, role.teamNumber);
                    }, x, y, w),
                (f, x, y, w) -> EditorRows.toggle(f, I18n.get(KEY + "npc_only"),
                    EditorRows.tip(I18n.get(KEY + "npc_only"), I18n.get(KEY + "npc_role_tooltip")), role.isNpc,
                    () -> {
                        role.isNpc = !role.isNpc;
                        ScenarioServerboundPacket.setRoleIsNpc(role.index, role.isNpc);
                    }, x, y, w)
            )),
            new Group(I18n.get(KEY + "group.starting_resources"), List.of(
                resource(role, "starting_food", ResourceName.FOOD, () -> role.startingResources.food, v -> role.startingResources.food = v),
                resource(role, "starting_wood", ResourceName.WOOD, () -> role.startingResources.wood, v -> role.startingResources.wood = v),
                resource(role, "starting_ore", ResourceName.ORE, () -> role.startingResources.ore, v -> role.startingResources.ore = v)
            )),
            new Group(I18n.get(KEY + "group.assigned"), List.of(
                (f, x, y, w) -> EditorRows.action(f, I18n.get(KEY + "units"), selectTip("select_role_units"),
                    String.valueOf(ScenarioClientEvents.getNumScenarioUnits(role)), I18n.get(KEY + "select"),
                    () -> ScenarioClientEvents.cycleRoleUnits(false),
                    () -> {
                        if (Keybindings.shiftMod.isDown() && Keybindings.ctrlMod.isDown()) {
                            ScenarioClientEvents.clearRoleUnits();
                            UnitClientEvents.clearSelectedUnits();
                        } else {
                            ScenarioClientEvents.cycleRoleUnits(true);
                        }
                    }, x, y, w),
                (f, x, y, w) -> EditorRows.action(f, I18n.get(KEY + "buildings"), selectTip("select_role_buildings"),
                    String.valueOf(ScenarioClientEvents.getNumScenarioBuildings(role)), I18n.get(KEY + "select"),
                    () -> ScenarioClientEvents.cycleRoleBuildings(false),
                    () -> {
                        if (Keybindings.shiftMod.isDown() && Keybindings.ctrlMod.isDown()) {
                            ScenarioClientEvents.clearRoleBuildings();
                            BuildingClientEvents.clearSelectedBuildings();
                        } else {
                            ScenarioClientEvents.cycleRoleBuildings(true);
                        }
                    }, x, y, w)
            ))
        );
    }

    private static Supplier<Tooltip> selectTip(String key) {
        return () -> Tooltip.of(I18n.get(KEY + key)).body(I18n.get(KEY + "select_all_shift")).body(I18n.get(KEY + "clear_all"));
    }

    /** Left click (or ▶) adds 5, or 100 with shift; right click (or ◀) takes it away. */
    private static Row resource(ScenarioRole role, String key, ResourceName name, IntSupplier get, IntConsumer set) {
        return (f, x, y, w) -> EditorRows.stepper(f, I18n.get(KEY + key), null, String.valueOf(get.getAsInt()),
            () -> {
                int value = Math.min(10000, get.getAsInt() + (Keybindings.shiftMod.isDown() ? 100 : 5));
                ScenarioServerboundPacket.setStartingResources(role.index, name, value);
                set.accept(value);
            },
            () -> {
                int value = Math.max(0, get.getAsInt() - (Keybindings.shiftMod.isDown() ? 100 : 5));
                ScenarioServerboundPacket.setStartingResources(role.index, name, value);
                set.accept(value);
            }, x, y, w);
    }
}
