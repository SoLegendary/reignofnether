package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingCommand;
import com.solegendary.reignofnether.editor.CodeEditorPopup;
import com.solegendary.reignofnether.hud.PortraitRendererBuilding;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.hud.panels.ButtonCell;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.rtsmap.RonFilesClient;
import com.solegendary.reignofnether.ui.EditorRows;
import com.solegendary.reignofnether.ui.EditorRows.Group;
import com.solegendary.reignofnether.ui.EditorRows.Row;
import com.solegendary.reignofnether.ui.EditorRows.Style;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents.setCustomBuildingToEdit;
import static com.solegendary.reignofnether.resources.ResourcesServerEvents.NEUTRAL_BUILDING_BOUNTY_PERCENT;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

/**
 * The custom building editor: a popup in HUD units with the building's settings in grouped rows,
 * or its triggers, each a file in ron/buildings/<id>/ opened in the code popup. Every change is sent to the server as it is made.
 */
public class CustomBuildingMenu {

    private static final String KEY = "sandbox.reignofnether.custom_buildings.";
    private static final int PAD = 7;
    private static final int COL_W = 200;
    private static final int FOOTER_H = 14;
    private static final int PORTRAIT = 44; // the switch button, and the box of the preview next to it
    private static final int HEADER_H = PORTRAIT + 2;

    /** Draws the whole editor for this building. */
    public static void render(HudFrame f, CustomBuilding building) {
        boolean triggers = CustomBuildingClientEvents.showTriggersMenu;
        int top = HudLayout.MARGIN + HudLayout.TOP_CARD_H + HudLayout.GAP;
        int maxBody = f.layout.height() - HudLayout.MARGIN - top - PAD * 2 - HEADER_H - FOOTER_H - 6;
        List<Group> groups = triggers ? List.of() : groups(building);
        int bodyH = triggers ? TRIGGERS.size() * TRIGGER_ROW_H : EditorRows.measure(groups, COL_W, maxBody).h();
        int bodyW = triggers ? TRIGGERS_W : EditorRows.measure(groups, COL_W, maxBody).w();
        int w = bodyW + PAD * 2;
        int h = HEADER_H + bodyH + 6 + FOOTER_H + PAD * 2;
        int x = HudLayout.MARGIN;
        Theme.popup(f.g, x, top, w, h, Theme.ORANGE);
        f.zone(x, top, w, h);

        int cx = x + PAD;
        int y = top + PAD;
        renderHeader(f, building, cx, y, bodyW);
        y += HEADER_H;
        if (triggers)
            renderTriggers(f, building, cx, y);
        else
            EditorRows.render(f, groups, cx, y, COL_W, maxBody);
        renderFooter(f, building, cx, top + h - PAD - FOOTER_H, bodyW, FOOTER_H);
    }

    /** The change-icon cell, the spinning portrait block, the building's name and the close cross. */
    private static void renderHeader(HudFrame f, CustomBuilding building, int x, int y, int w) {
        Button iconButton = new Button("Change Building Icon",
            Button.itemIconSize,
            MiscUtil.getTextureForBlock(building.portraitBlock),
            (Keybinding) null,
            () -> false,
            () -> false,
            () -> true,
            () -> building.cycleIconAndPortrait(false),
            () -> building.cycleIconAndPortrait(true),
            List.of(fcs(I18n.get(KEY + "change_icon.tooltip1")))
        );
        f.cell(iconButton, x, y, PORTRAIT, null);
        PortraitRendererBuilding.drawBlock(f.g, building.portraitBlock, x + PORTRAIT * 1.5f + 4, y + PORTRAIT / 2f, 4f);
        int nameX = PORTRAIT * 2 + 8;
        EditorRows.header(f, building.name, x + nameX, y, w - nameX, null, () -> setCustomBuildingToEdit(null));
    }

    /** Left to right: the general/triggers switch and the two-click deregister button. */
    private static void renderFooter(HudFrame f, CustomBuilding building, int x, int y, int w, int h) {
        boolean triggers = CustomBuildingClientEvents.showTriggersMenu;
        int buttonW = 80;
        String label = triggers ? I18n.get(KEY + "general_tab") : I18n.get(KEY + "triggers_tab", triggerCount(building));
        EditorRows.textButton(f, "Toggle Triggers Menu", label, new Rect(x, y, buttonW, h), Style.SECONDARY,
            () -> {
                CustomBuildingClientEvents.showTriggersMenu = !triggers;
                if (!triggers)
                    RonFilesClient.refresh(); // files may have been written by hand
            }, null,
            EditorRows.tip(I18n.get(KEY + (triggers ? "switch_to_general_menu" : "switch_to_triggers_menu")),
                I18n.get(KEY + "triggers.folder", "ron/buildings/" + building.id + "/")));

        boolean confirm = CustomBuildingClientEvents.deregisterConfirm;
        String deregister = confirm ? I18n.get(KEY + "deregister.confirm") : I18n.get(KEY + "deregister.label");
        EditorRows.textButton(f, "Deregister Custom Building", deregister, new Rect(x + buttonW + 4, y, buttonW, h), Style.DANGER,
            () -> {
                if (deregister(building))
                    setCustomBuildingToEdit(null);
            },
            () -> CustomBuildingClientEvents.deregisterConfirm = false,
            () -> Tooltip.of(I18n.get(KEY + "deregister.tooltip1")).danger()
                .body(I18n.get(KEY + (confirm ? "deregister.confirm" : "deregister.tooltip2"))));
    }

    /** The first click arms it, the second removes the type and every building of it; true when removed. */
    public static boolean deregister(CustomBuilding building) {
        if (!CustomBuildingClientEvents.deregisterConfirm) {
            CustomBuildingClientEvents.deregisterConfirm = true;
            return false;
        }
        CustomBuildingClientEvents.deregisterConfirm = false;
        CustomBuildingServerboundPacket.deregisterBuilding(building.name);
        CustomBuildingClientEvents.customBuildings.removeIf(b -> b.name.equals(building.name));
        BuildingClientEvents.clearSelectedBuildings();
        BuildingClientEvents.getBuildings().removeIf(b -> b.getBuilding().name.equals(building.name));
        return true;
    }

    // ---------------------------------------------------------------- general tab

    /** The building type's attributes (the General tab), also the Editor's Build mode inspector. */
    public static List<Group> groups(CustomBuilding b) {
        return List.of(
            new Group(I18n.get(KEY + "group.buildable_by"), List.of(
                toggle(b, "set_buildable_by_villagers", CustomBuildingAction.SET_BUILDABLE_BY_VILLAGERS, () -> b.buildableByVillagers, v -> b.buildableByVillagers = v),
                toggle(b, "set_buildable_by_monsters", CustomBuildingAction.SET_BUILDABLE_BY_MONSTERS, () -> b.buildableByMonsters, v -> b.buildableByMonsters = v),
                toggle(b, "set_buildable_by_piglins", CustomBuildingAction.SET_BUILDABLE_BY_PIGLINS, () -> b.buildableByPiglins, v -> b.buildableByPiglins = v),
                toggle(b, "set_nether_terrain_only", CustomBuildingAction.SET_NETHER_TERRAIN_ONLY, () -> b.netherTerrainOnly, v -> b.netherTerrainOnly = v)
            )),
            new Group(I18n.get(KEY + "group.cost"), List.of(
                number(b, "set_food_cost", ButtonCell.ICON_FOOD, CustomBuildingAction.SET_FOOD_COST, () -> b.cost.food, v -> b.cost.food = v, 0, 10000, 5, 100,
                    EditorRows.tip(I18n.get(KEY + "set_food_cost.label"), I18n.get(KEY + "set_food_cost.tooltip1", b.cost.food * NEUTRAL_BUILDING_BOUNTY_PERCENT))),
                number(b, "set_wood_cost", ButtonCell.ICON_WOOD, CustomBuildingAction.SET_WOOD_COST, () -> b.cost.wood, v -> b.cost.wood = v, 0, 10000, 5, 100,
                    EditorRows.tip(I18n.get(KEY + "set_wood_cost.label"), I18n.get(KEY + "set_wood_cost.tooltip1", b.cost.wood * NEUTRAL_BUILDING_BOUNTY_PERCENT))),
                number(b, "set_ore_cost", ButtonCell.ICON_ORE, CustomBuildingAction.SET_ORE_COST, () -> b.cost.ore, v -> b.cost.ore = v, 0, 10000, 5, 100,
                    EditorRows.tip(I18n.get(KEY + "set_ore_cost.label"), I18n.get(KEY + "set_ore_cost.tooltip1", b.cost.ore * NEUTRAL_BUILDING_BOUNTY_PERCENT))),
                number(b, "set_pop_supply", modIcon("textures/icons/items/bed.png"), CustomBuildingAction.SET_POPULATION_SUPPLY, () -> b.cost.population, v -> b.cost.population = v, 0, 300, 1, 10, null)
            )),
            new Group(I18n.get(KEY + "group.contents"), List.of(
                (f, x, y, w) -> EditorRows.action(f, vanillaIcon("textures/block/fletching_table_front.png"), I18n.get(KEY + "trainable_units.label"), trainableUnitsTooltip(b),
                    String.valueOf(b.productions.get().size()), "?", GUIDE_BUTTON_W, null, null, x, y, w),
                (f, x, y, w) -> EditorRows.action(f, modIcon("textures/item/heart_medallion.png"), I18n.get(KEY + "buyable_items.label"),
                    () -> guideTooltip("buyable_items"),
                    String.valueOf(b.stockedItems.size()), "?", GUIDE_BUTTON_W, null, null, x, y, w)
            )),
            new Group(I18n.get(KEY + "group.building"), List.of(
                maxHealth(b),
                number(b, "set_night_radius", modIcon("textures/hud/moon.png"), CustomBuildingAction.SET_NIGHT_RADIUS, () -> b.nightRadius, v -> b.nightRadius = v, 0, 200, 1, 10, null),
                number(b, "set_nether_radius", modIcon("textures/icons/blocks/portal.png"), CustomBuildingAction.SET_NETHER_RADIUS, () -> b.netherRadius, v -> b.netherRadius = v, 0, 200, 1, 10, null),
                number(b, "set_garrison_capacity", vanillaIcon("textures/block/ladder.png"), CustomBuildingAction.SET_GARRISON_CAPACITY, () -> b.garrisonCapacity, v -> b.garrisonCapacity = v, 0, 100, 1, 10,
                    garrisonTooltip(b, "set_garrison_capacity")),
                number(b, "set_garrison_range", vanillaIcon("textures/item/bow.png"), CustomBuildingAction.SET_GARRISON_RANGE, () -> b.garrisonRange, v -> b.garrisonRange = v, 0, 100, 1, 10,
                    garrisonTooltip(b, "set_garrison_range")),
                toggle(b, "set_capturable", CustomBuildingAction.SET_CAPTURABLE, () -> b.capturable, v -> b.capturable = v),
                toggle(b, "set_invulnerable", CustomBuildingAction.SET_INVULNERABLE, () -> b.invulnerable, v -> b.invulnerable = v),
                toggle(b, "set_repairable", CustomBuildingAction.SET_REPAIRABLE, () -> b.repairable, v -> b.repairable = v),
                toggle(b, "set_draw_aggro", CustomBuildingAction.SET_DRAW_AGGRO, () -> b.drawAggro, v -> b.drawAggro = v),
                toggle(b, "set_destroy_on_reset", CustomBuildingAction.SET_DESTROY_ON_RESET, () -> b.shouldDestroyOnReset, v -> b.shouldDestroyOnReset = v)
            ))
        );
    }

    private static ResourceLocation modIcon(String path) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, path);
    }

    private static ResourceLocation vanillaIcon(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    /** Lists are edited in the world: the [?] button's tooltip explains how. */
    private static Tooltip guideTooltip(String key) {
        return Tooltip.of(I18n.get(KEY + key + ".label")).body(I18n.get(KEY + key + ".hint1")).body(I18n.get(KEY + key + ".hint2"));
    }

    private static Row toggle(CustomBuilding b, String key, CustomBuildingAction action, BooleanSupplier get, Consumer<Boolean> set) {
        String label = I18n.get(KEY + key + ".label");
        return (f, x, y, w) -> EditorRows.toggle(f, label, EditorRows.tip(label, I18n.get(KEY + key + ".tooltip1")), get.getAsBoolean(), () -> {
            CustomBuildingServerboundPacket.customiseBuilding(action, b.name, !get.getAsBoolean());
            set.accept(!get.getAsBoolean());
        }, x, y, w);
    }

    /** Left click (or ▶) adds small, or big with shift; right click (or ◀) takes it away, within [min, max]. */
    private static Row number(CustomBuilding b, String key, ResourceLocation icon, CustomBuildingAction action, IntSupplier get, IntConsumer set,
                              int min, int max, int small, int big, Supplier<Tooltip> tooltip) {
        String label = I18n.get(KEY + key + ".label");
        Supplier<Tooltip> tip = tooltip != null ? tooltip : EditorRows.tip(label, I18n.get(KEY + key + ".tooltip1"));
        return (f, x, y, w) -> EditorRows.stepper(f, icon, label, tip, String.valueOf(get.getAsInt()), null,
            () -> step(b, action, get, set, max, small, big, 1),
            () -> step(b, action, get, set, min, small, big, -1), x, y, w);
    }

    private static void step(CustomBuilding b, CustomBuildingAction action, IntSupplier get, IntConsumer set,
                             int limit, int small, int big, int sign) {
        int delta = Keybindings.shiftMod.isDown() ? big : small;
        int value = sign > 0 ? Math.min(limit, get.getAsInt() + delta) : Math.max(limit, get.getAsInt() - delta);
        CustomBuildingServerboundPacket.customiseBuilding(action, b.name, value);
        set.accept(value);
    }

    /** Max health steps in fives; ctrl+click resets it to the default for the building's size. */
    private static Row maxHealth(CustomBuilding b) {
        String label = I18n.get(KEY + "set_max_health.label");
        return (f, x, y, w) -> EditorRows.stepper(f, modIcon("textures/icons/items/heart.png"), label, EditorRows.tip(label, I18n.get(KEY + "set_max_health.tooltip1")),
            String.valueOf((int) b.maxHealth), null, () -> stepMaxHealth(b, 1), () -> stepMaxHealth(b, -1), x, y, w);
    }

    private static void stepMaxHealth(CustomBuilding b, int sign) {
        if (Keybindings.ctrlMod.isDown()) {
            b.setToDefaultMaxHealth();
            CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_MAX_HEALTH, b.name, (int) b.maxHealth);
        } else {
            int delta = Keybindings.shiftMod.isDown() ? 100 : 5;
            int value = sign > 0 ? Math.min(10000, (int) b.maxHealth + delta) : Math.max(5, (int) b.maxHealth - delta);
            value = (int) (Math.round((double) value / 5.0d) * 5);
            CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_MAX_HEALTH, b.name, value);
            b.maxHealth = value;
        }
    }

    private static Supplier<Tooltip> garrisonTooltip(CustomBuilding customBuilding, String key) {
        return () -> {
            Tooltip t = Tooltip.of(I18n.get(KEY + key + ".label")).body(I18n.get(KEY + key + ".tooltip1"));
            if (customBuilding.numGarrisonZones <= 0) {
                t.body(fcsIcons(I18n.get(KEY + "garrison_warning.no_zones")));
            } else if (customBuilding.numGarrisonEntries <= 0) {
                t.body(fcsIcons(I18n.get(KEY + "garrison_warning.no_entry")));
            } else if (customBuilding.numGarrisonEntries > 1) {
                t.body(fcsIcons(I18n.get(KEY + "garrison_warning.multiple_entries")));
            }
            if (customBuilding.numGarrisonExits <= 0) {
                t.body(fcsIcons(I18n.get(KEY + "garrison_warning.no_exit")));
            } else if (customBuilding.numGarrisonExits > 1) {
                t.body(fcsIcons(I18n.get(KEY + "garrison_warning.multiple_exits")));
            }
            return t;
        };
    }

    private static Supplier<Tooltip> trainableUnitsTooltip(CustomBuilding customBuilding) {
        return () -> {
            Tooltip t = guideTooltip("trainable_units");
            if (customBuilding.numSpawnBlocks <= 0) {
                t.body(fcsIcons(I18n.get(KEY + "trainable_units.no_spawn_block")));
            } else if (customBuilding.numSpawnBlocks > 1) {
                t.body(fcsIcons(I18n.get(KEY + "trainable_units.multiple_spawn_blocks")));
            }
            return t;
        };
    }

    // ---------------------------------------------------------------- triggers tab

    private static final int GUIDE_BUTTON_W = 13;
    private static final int TRIGGERS_W = 450;
    private static final int TRIGGER_ROW_H = 22;
    private static final int TRIGGER_BUTTON_W = 50;
    private static final int COOLDOWN_W = 76;
    private static final List<BuildingCommand.TriggerCondition> TRIGGERS = Arrays.stream(BuildingCommand.TriggerCondition.values())
        .filter(t -> t != BuildingCommand.TriggerCondition.NONE).toList();

    private static String triggerKey(BuildingCommand.TriggerCondition t) {
        return t.name().toLowerCase(Locale.ROOT);
    }

    // the building's commands for this trigger: placements run its file through them, and they hold its cooldown
    private static List<BuildingCommand> commandsOf(CustomBuilding b, BuildingCommand.TriggerCondition t) {
        return b.commands.stream().filter(c -> c.condition == t).toList();
    }

    private static boolean hasCooldown(BuildingCommand.TriggerCondition t) {
        BuildingCommand c = new BuildingCommand();
        c.condition = t;
        return c.hasCooldownCondition();
    }

    // a trigger without a command never runs its file: the server adds one, with the default cooldown
    private static void ensureCommand(CustomBuilding b, BuildingCommand.TriggerCondition t) {
        if (!commandsOf(b, t).isEmpty())
            return;
        int value = new BuildingCommand().tickCooldownMax;
        b.setTriggerCooldown(t.name(), value);
        CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_TRIGGER_COOLDOWN, b.name, value, t.name());
    }

    // the server adds the trigger's command when there's none (or its file never runs) and sets every one of them
    private static void stepCooldown(CustomBuilding b, BuildingCommand.TriggerCondition t, int sign) {
        int delta = Keybindings.shiftMod.isDown() ? 100 : 10;
        List<BuildingCommand> commands = commandsOf(b, t);
        int value = Math.max(0, Math.min(72000, (commands.isEmpty() ? 0 : commands.get(0).tickCooldownMax) + sign * delta));
        b.setTriggerCooldown(t.name(), value);
        CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_TRIGGER_COOLDOWN, b.name, value, t.name());
    }

    /** How many commands a trigger's file has (from the file list, else the building's commands); null: no file. */
    private static Integer commandCount(CustomBuilding b, BuildingCommand.TriggerCondition t) {
        Integer n = RonFilesClient.files.get("buildings/" + b.id + "/" + triggerKey(t));
        if (n != null)
            return n;
        long listed = commandsOf(b, t).stream().filter(c -> !c.commandStr.isBlank()).count();
        return listed > 0 ? (int) listed : null;
    }

    private static int triggerCount(CustomBuilding b) {
        int n = 0;
        for (BuildingCommand.TriggerCondition t : TRIGGERS)
            if (commandCount(b, t) != null)
                n++;
        return n;
    }

    /** Each row: the trigger's icon, its name, its file's command count, its cooldown and Edit / + Create. */
    private static void renderTriggers(HudFrame f, CustomBuilding b, int x, int y) {
        int right = x + TRIGGERS_W;
        for (BuildingCommand.TriggerCondition t : TRIGGERS) {
            String key = triggerKey(t);
            String path = "buildings/" + b.id + "/" + key;
            String name = I18n.get(KEY + "triggers." + key);
            f.g.fill(x, y + TRIGGER_ROW_H - 1, right, y + TRIGGER_ROW_H, Theme.RAISED);

            Button icon = new ButtonBuilder("Trigger " + key)
                .iconResource(triggerIcon(t))
                .bgIconResource(t == BuildingCommand.TriggerCondition.OFF_COOLDOWN_IF_CAPTURED ? vanillaIcon("textures/item/nether_star.png") : null)
                .tooltipLines(List.of(fcs(I18n.get(KEY + "commands.trigger." + key))))
                .build();
            f.cell(icon, x, y + 1, TRIGGER_ROW_H - 2, null);
            Text.draw(f.g, name, x + TRIGGER_ROW_H + 4, y + (TRIGGER_ROW_H - 8) / 2, Theme.TEXT);

            Integer n = commandCount(b, t);
            String count = n == null ? I18n.get(KEY + "triggers.no_file") : I18n.get(KEY + "triggers.commands", n);
            Text.draw(f.g, count, x + 190, y + (TRIGGER_ROW_H - 8) / 2, n == null ? Theme.DIM : Theme.BODY);

            Rect button = new Rect(right - TRIGGER_BUTTON_W, y + 3, TRIGGER_BUTTON_W, TRIGGER_ROW_H - 6);
            if (hasCooldown(t)) {
                List<BuildingCommand> commands = commandsOf(b, t);
                String cooldownLabel = I18n.get(KEY + "commands.cooldown");
                EditorRows.wideStepper(f, cooldownLabel + " " + key, EditorRows.tip(cooldownLabel, I18n.get(KEY + "triggers.cooldown_tooltip")),
                    commands.isEmpty() ? "100" : String.valueOf(commands.get(0).tickCooldownMax),
                    () -> stepCooldown(b, t, 1), () -> stepCooldown(b, t, -1),
                    button.x() - 6 - COOLDOWN_W, y + (TRIGGER_ROW_H - EditorRows.ROW_H) / 2, COOLDOWN_W);
            }
            String when = I18n.get(KEY + "triggers.when." + key);
            EditorRows.textButton(f, "Trigger file " + key, I18n.get(KEY + (n == null ? "triggers.create" : "triggers.edit")), button, Style.SECONDARY,
                () -> {
                    ensureCommand(b, t);
                    CodeEditorPopup.open(path, b.name + " · " + name, when);
                }, null,
                EditorRows.tip(name, "ron/" + path + ".mcfunction · " + when));
            y += TRIGGER_ROW_H;
        }
    }

    private static ResourceLocation triggerIcon(BuildingCommand.TriggerCondition t) {
        return switch (t) {
            case ON_BUILD_COMPLETE          -> vanillaIcon("textures/item/iron_pickaxe.png");
            case ON_DESTROY                 -> modIcon("textures/icons/blocks/tnt.png");
            case ON_DAMAGE_TAKEN            -> vanillaIcon("textures/item/iron_sword.png");
            case ON_CAPTURE                 -> vanillaIcon("textures/item/nether_star.png");
            case OFF_COOLDOWN_IF_COMPLETE   -> modIcon("textures/icons/items/clock.png");
            case OFF_COOLDOWN_IF_GARRISONED -> vanillaIcon("textures/block/ladder.png");
            case OFF_COOLDOWN_IF_CAPTURED   -> modIcon("textures/icons/items/clock_half.png");
            case NONE                       -> null;
        };
    }
}
