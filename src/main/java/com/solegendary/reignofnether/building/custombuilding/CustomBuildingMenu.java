package com.solegendary.reignofnether.building.custombuilding;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingCommand;
import com.solegendary.reignofnether.hud.*;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.hud.panels.ButtonCell;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.ui.EditorRows;
import com.solegendary.reignofnether.ui.EditorRows.Group;
import com.solegendary.reignofnether.ui.EditorRows.Row;
import com.solegendary.reignofnether.ui.EditorRows.Style;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents.getCustomBuildingToEdit;
import static com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents.setCustomBuildingToEdit;
import static com.solegendary.reignofnether.resources.ResourcesServerEvents.NEUTRAL_BUILDING_BOUNTY_PERCENT;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

/**
 * The custom building editor: a popup in HUD units with the building's settings in grouped rows,
 * or its list of commands. Every change is sent to the server as it is made.
 */
public class CustomBuildingMenu {

    private static final String KEY = "sandbox.reignofnether.custom_buildings.";
    private static final int PAD = 7;
    private static final int COL_W = 200;
    private static final int COMMANDS_W = 450;
    private static final int FOOTER_H = 14;
    private static final int COMMANDS_FOOTER_H = 21;
    private static final int PORTRAIT = 44; // the switch button, and the box of the preview next to it
    private static final int HEADER_H = PORTRAIT + 2;

    /** Draws the whole editor for this building. */
    public static void render(HudFrame f, CustomBuilding building) {
        boolean commands = CustomBuildingClientEvents.showCommandsMenu;
        int top = HudLayout.MARGIN + HudLayout.TOP_CARD_H + HudLayout.GAP;
        int footerH = commands ? COMMANDS_FOOTER_H : FOOTER_H;
        int maxBody = f.layout.height() - HudLayout.MARGIN - top - PAD * 2 - HEADER_H - footerH - 6;
        List<Group> groups = commands ? List.of() : groups(building);
        int bodyH = commands ? VISIBLE_COMMAND_ROWS * COMMAND_ROW_H : EditorRows.measure(groups, COL_W, maxBody).h();
        int bodyW = commands ? COMMANDS_W : EditorRows.measure(groups, COL_W, maxBody).w();
        int w = bodyW + PAD * 2;
        int h = HEADER_H + bodyH + 6 + footerH + PAD * 2;
        int x = HudLayout.MARGIN;
        Theme.popup(f.g, x, top, w, h, Theme.ORANGE);
        f.zone(x, top, w, h);

        int cx = x + PAD;
        int y = top + PAD;
        renderHeader(f, building, cx, y, bodyW);
        y += HEADER_H;
        if (commands)
            renderCommands(f, building, cx, y);
        else
            EditorRows.render(f, groups, cx, y, COL_W, maxBody);
        renderFooter(f, building, cx, top + h - PAD - footerH, bodyW, footerH);
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

    /** Left to right: the general/commands switch, Add Command on the commands tab and the two-click deregister button. */
    private static void renderFooter(HudFrame f, CustomBuilding building, int x, int y, int w, int h) {
        boolean commands = CustomBuildingClientEvents.showCommandsMenu;
        int buttonW = commands ? 120 : 80;
        int commandCount = 0;
        for (BuildingCommand command : building.commands)
            if (command.condition != BuildingCommand.TriggerCondition.NONE && !command.commandStr.isBlank())
                commandCount += 1;
        String countText = I18n.get(KEY + "commands_count", commandCount);
        String label = commands ? I18n.get(KEY + "general_tab") : I18n.get(KEY + "commands_tab") + " (" + commandCount + ")";
        EditorRows.textButton(f, "Toggle Commands Menu", label, new Rect(x, y, buttonW, h), Style.SECONDARY,
            () -> {
                CustomBuildingClientEvents.showCommandsMenu = !commands;
                forceRefreshCommandEditBoxes();
            }, null,
            EditorRows.tip(I18n.get(KEY + (commands ? "switch_to_general_menu" : "switch_to_commands_menu")), countText));

        if (commands && building.commands.size() < MAX_COMMANDS)
            EditorRows.textButton(f, "Add Command", "+ " + I18n.get(KEY + "commands.add_label"), new Rect(x + buttonW + 4, y, 75, h), Style.SECONDARY,
                () -> {
                    if (building.commands.size() < MAX_COMMANDS)
                        building.commands.add(new BuildingCommand());
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.ADD_COMMAND, building.name);
                    // jump the view down to reveal the newly-added row
                    commandScrollOffset = maxCommandScrollOffset(building.commands.size());
                    forceRefreshCommandEditBoxes();
                }, null,
                EditorRows.tip(I18n.get(KEY + "commands.add"), null));

        boolean confirm = CustomBuildingClientEvents.deregisterConfirm;
        String deregister = confirm ? I18n.get(KEY + "deregister.confirm") : I18n.get(KEY + "deregister.label");
        EditorRows.textButton(f, "Deregister Custom Building", deregister, new Rect(x + buttonW + 4 + (commands ? 79 : 0), y, buttonW, h), Style.DANGER,
            () -> {
                if (!CustomBuildingClientEvents.deregisterConfirm) {
                    CustomBuildingClientEvents.deregisterConfirm = true;
                } else {
                    CustomBuildingServerboundPacket.deregisterBuilding(building.name);
                    CustomBuildingClientEvents.customBuildings.removeIf(b -> b.name.equals(building.name));
                    BuildingClientEvents.clearSelectedBuildings();
                    BuildingClientEvents.getBuildings().removeIf(b -> b.getBuilding().name.equals(building.name));
                    setCustomBuildingToEdit(null);
                }
            },
            () -> CustomBuildingClientEvents.deregisterConfirm = false,
            () -> Tooltip.of(I18n.get(KEY + "deregister.tooltip1")).danger()
                .body(I18n.get(KEY + (confirm ? "deregister.confirm" : "deregister.tooltip2"))));
    }

    // ---------------------------------------------------------------- general tab

    private static List<Group> groups(CustomBuilding b) {
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

    // ---------------------------------------------------------------- commands tab

    private static final int MAX_COMMANDS = 50;
    private static final int EDIT_BOX_HEIGHT = 9;
    private static final int COOLDOWN_FIELD_W = 60;
    private static final int COMMAND_ROW_H = 26;
    private static final int COMMAND_CONTROL = 18; // delete and warning, right of the fields
    private static final int GUIDE_BUTTON_W = 13;

    // how many command rows are visible in the scrollable viewport at once
    private static final int VISIBLE_COMMAND_ROWS = 10;
    private static final int SCROLLBAR_WIDTH = 6;

    // index of the first command row currently visible (i.e. scroll position)
    private static int commandScrollOffset = 0;

    // one per visible row; the cooldown list holds null for rows without a cooldown trigger
    private static final ArrayList<MyEditBox> commandEditBoxes = new ArrayList<>();
    private static final ArrayList<MyEditBox> commandCooldownEditBoxes = new ArrayList<>();
    private static boolean commandEditBoxesDirty = true;

    private static int maxCommandScrollOffset(int totalCommands) {
        return Math.max(0, totalCommands - VISIBLE_COMMAND_ROWS);
    }

    private static void clampCommandScrollOffset(int totalCommands) {
        commandScrollOffset = Math.min(Math.max(commandScrollOffset, 0), maxCommandScrollOffset(totalCommands));
    }

    /** Scroll the commands list by the given number of rows (positive = down, negative = up). Clamped to bounds. */
    public static void scrollCommands(int rows) {
        CustomBuilding building = getCustomBuildingToEdit();
        int total = building == null ? 0 : building.commands.size();
        commandScrollOffset += rows;
        clampCommandScrollOffset(total);
        forceRefreshCommandEditBoxes();
    }

    /** Tear down all currently-registered EditBoxes and rebuild them for the currently visible rows only. */
    private static void registerCommandEditBoxes(CustomBuilding customBuilding) {
        forceRefreshCommandEditBoxes();

        clampCommandScrollOffset(customBuilding.commands.size());

        int firstVisible = commandScrollOffset;
        int lastVisibleExclusive = Math.min(customBuilding.commands.size(), commandScrollOffset + VISIBLE_COMMAND_ROWS);

        // positions are set every frame by EditorRows.field
        for (int i = firstVisible; i < lastVisibleExclusive; i++) {
            final int idx = i;
            BuildingCommand cmd = customBuilding.commands.get(idx);

            MyEditBox textBox = new MyEditBox.Builder(0, 0, 100, EDIT_BOX_HEIGHT)
                .bordered(false)
                .maxLength(256)
                .value(cmd.commandStr)
                .onDefocus(value -> {
                    cmd.commandStr = value;
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_COMMAND_TEXT, customBuilding.name, idx, value);
                })
                .responder(value -> {
                    cmd.commandStr = value;
                    cmd.updateValidityAndExceptions();
                })
                .commandSuggestions(true)
                .build();

            MyEditBox cdBox = null;
            if (cmd.hasCooldownCondition()) {
                cdBox = new MyEditBox.Builder(0, 0, 30, EDIT_BOX_HEIGHT)
                    .bordered(false)
                    .maxLength(5)
                    .value(String.valueOf(cmd.tickCooldownMax))
                    .isNumber(true)
                    .onDefocus(value -> {
                        try {
                            cmd.tickCooldownMax = Integer.parseInt(value);
                            CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_COMMAND_COOLDOWN, customBuilding.name, idx, value);
                        } catch (NumberFormatException e) {
                            cmd.tickCooldownMax = 100;
                            CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_COMMAND_COOLDOWN, customBuilding.name, idx, "100");
                        }
                    })
                    .tooltipLines(List.of(fcs(I18n.get(KEY + "commands.cooldown"))))
                    .build();
                TextInputClientEvents.registerEditBox(cdBox);
            }
            commandCooldownEditBoxes.add(cdBox);

            TextInputClientEvents.registerEditBox(textBox);
            commandEditBoxes.add(textBox);
        }

        commandEditBoxesDirty = false;
    }

    // run this to force a rebuild on the next render frame
    public static void forceRefreshCommandEditBoxes() {
        for (MyEditBox box : commandEditBoxes)
            TextInputClientEvents.deregisterEditBox(box);
        for (MyEditBox box : commandCooldownEditBoxes)
            TextInputClientEvents.deregisterEditBox(box);
        commandEditBoxes.clear();
        commandCooldownEditBoxes.clear();
        commandEditBoxesDirty = true;
    }

    /** Each row: trigger cell (click cycles, right click goes back), command text, cooldown, warning and delete. */
    private static void renderCommands(HudFrame f, CustomBuilding customBuilding, int x, int y) {
        BuildingCommand.TriggerCondition[] conditions = BuildingCommand.TriggerCondition.values();

        clampCommandScrollOffset(customBuilding.commands.size());

        if (!customBuilding.commands.isEmpty() && commandEditBoxesDirty)
            registerCommandEditBoxes(customBuilding);

        int rowH = COMMAND_ROW_H;
        int cell = rowH - 2;
        int firstVisible = commandScrollOffset;
        int lastVisibleExclusive = Math.min(customBuilding.commands.size(), commandScrollOffset + VISIBLE_COMMAND_ROWS);
        int right = x + COMMANDS_W - SCROLLBAR_WIDTH - 4;

        for (int i = firstVisible; i < lastVisibleExclusive; i++) {
            final int idx = i;
            BuildingCommand cmd = customBuilding.commands.get(idx);
            int rowY = y + (i - firstVisible) * rowH;
            f.g.fill(x, rowY + rowH - 1, right, rowY + rowH, Theme.RAISED);

            ResourceLocation condRl = switch (cmd.condition) {
                case ON_BUILD_COMPLETE          -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_pickaxe.png");
                case ON_DESTROY                 -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/tnt.png");
                case ON_DAMAGE_TAKEN            -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_sword.png");
                case ON_CAPTURE                 -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/nether_star.png");
                case OFF_COOLDOWN_IF_COMPLETE   -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/clock.png");
                case OFF_COOLDOWN_IF_GARRISONED -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/ladder.png");
                case OFF_COOLDOWN_IF_CAPTURED   -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/clock_half.png");
                case ON_SCENARIO_START          -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/writable_book.png");
                case NONE                       -> null;
            };
            ResourceLocation bgRl = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/nether_star.png");

            String tooltip = I18n.get(KEY + "commands.trigger." + cmd.condition.name().toLowerCase(java.util.Locale.ROOT));

            Button condButton = new ButtonBuilder("Cycle Trigger Condition " + idx)
                .iconResource(condRl)
                .bgIconResource(cmd.condition == BuildingCommand.TriggerCondition.OFF_COOLDOWN_IF_CAPTURED ? bgRl : null)
                .onLeftClick(() -> {
                    int next = (cmd.condition.ordinal() + 1) % conditions.length;
                    cmd.condition = conditions[next];
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_COMMAND_TRIGGER, customBuilding.name, idx, cmd.condition.name());
                    forceRefreshCommandEditBoxes();
                })
                .onRightClick(() -> {
                    int prev = (cmd.condition.ordinal() - 1 + conditions.length) % conditions.length;
                    cmd.condition = conditions[prev];
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_COMMAND_TRIGGER, customBuilding.name, idx, cmd.condition.name());
                    forceRefreshCommandEditBoxes();
                })
                .tooltipLines(List.of(fcs(tooltip)))
                .build();
            f.cell(condButton, x, rowY + 1, cell, null);

            // delete, then the warning, then the cooldown field from the right
            Rect delete = new Rect(right - COMMAND_CONTROL, rowY + (rowH - COMMAND_CONTROL) / 2, COMMAND_CONTROL, COMMAND_CONTROL);
            Theme.secondaryButton(f.g, delete.x(), delete.y(), delete.w(), delete.h(), f.hovered(delete));
            Theme.cross(f.g, delete, f.hovered(delete) ? Theme.ERROR : Theme.BODY);
            f.register(new ButtonBuilder("Delete Command " + idx)
                .onLeftClick(() -> {
                    customBuilding.commands.remove(idx);
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.DELETE_COMMAND, customBuilding.name, idx);
                    forceRefreshCommandEditBoxes();
                }).build(), delete, null, EditorRows.tip(I18n.get(KEY + "commands.delete"), null));

            Rect warning = new Rect(delete.x() - COMMAND_CONTROL - 2, delete.y(), COMMAND_CONTROL, COMMAND_CONTROL);
            if (!cmd.isValid()) {
                f.g.blit(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/warning.png"),
                    warning.x() + 1, warning.y() + 1, 0, 0, COMMAND_CONTROL - 2, COMMAND_CONTROL - 2, COMMAND_CONTROL - 2, COMMAND_CONTROL - 2);
                f.register(new ButtonBuilder("Validity " + idx).build(), warning, null, () -> {
                    Tooltip t = Tooltip.of(I18n.get(KEY + "commands.invalid")).danger();
                    for (CommandSyntaxException exception : cmd.getExceptions().values())
                        t.body(String.valueOf(exception.getMessage()));
                    return t;
                });
            }

            int fieldH = rowH - 4;
            int textX = x + cell + 4;
            int cooldownX = warning.x() - 4 - COOLDOWN_FIELD_W;
            int visible = i - firstVisible;
            MyEditBox cdBox = visible < commandCooldownEditBoxes.size() ? commandCooldownEditBoxes.get(visible) : null;
            int textRight = cdBox != null ? cooldownX - 4 : warning.x() - 4;
            if (visible < commandEditBoxes.size())
                EditorRows.field(f, commandEditBoxes.get(visible), new Rect(textX, rowY + 2, textRight - textX, fieldH));
            if (cdBox != null)
                EditorRows.field(f, cdBox, new Rect(cooldownX, rowY + 2, COOLDOWN_FIELD_W, fieldH));
        }

        renderCommandsScrollbar(f, customBuilding, x + COMMANDS_W - SCROLLBAR_WIDTH, y, rowH);
    }

    private static void renderCommandsScrollbar(HudFrame f, CustomBuilding customBuilding, int x, int y, int rowH) {
        int total = customBuilding.commands.size();
        // only show scroll controls once there are more commands than fit in the viewport
        if (total <= VISIBLE_COMMAND_ROWS)
            return;

        int trackHeight = VISIBLE_COMMAND_ROWS * rowH;
        f.g.fill(x, y, x + SCROLLBAR_WIDTH, y + trackHeight, Theme.INK);

        int maxOffset = maxCommandScrollOffset(total);
        int thumbHeight = Math.max(8, trackHeight * VISIBLE_COMMAND_ROWS / total);
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = y + (maxOffset == 0 ? 0 : thumbTravel * commandScrollOffset / maxOffset);
        f.g.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, Theme.LABEL);
    }
}
