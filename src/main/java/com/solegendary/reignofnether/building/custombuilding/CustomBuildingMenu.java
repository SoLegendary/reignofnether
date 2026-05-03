package com.solegendary.reignofnether.building.custombuilding;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.hud.*;
import com.solegendary.reignofnether.hud.buttons.BooleanButton;
import com.solegendary.reignofnether.hud.buttons.IntegerButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents.getCustomBuildingToEdit;
import static com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents.setCustomBuildingToEdit;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class CustomBuildingMenu {

    private static Minecraft MC = Minecraft.getInstance();

    public static PortraitRendererBuilding portraitRendererBuilding = new PortraitRendererBuilding();

    public static Button renderIconButtonNameAndPortrait(ScreenEvent.Render.Post evt, CustomBuilding building, int x, int y) {
        Button iconButton = new Button("Change Building Icon",
                Button.itemIconSize,
                MiscUtil.getTextureForBlock(building.portraitBlock),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> building.cycleIconAndPortrait(false),
                () -> building.cycleIconAndPortrait(true),
                List.of(fcs(I18n.get("sandbox.reignofnether.custom_buildings.change_icon.tooltip1")))
        );
        portraitRendererBuilding.drawBlockOnScreen(x + 2, y - 22, building.portraitBlock, 4.0f);
        evt.getGuiGraphics().drawString(MC.font, fcs(building.name, true), x + 52, y, 0xFFFFFF);
        renderButton(iconButton, x - 8, y - 8, evt);
        return iconButton;
    }

    public static Button renderCloseButton(ScreenEvent.Render.Post evt, int x, int y) {
        Button closeButton = new Button("Close Custom Building Menu",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross_square.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> setCustomBuildingToEdit(null),
                null,
                List.of()
        );
        closeButton.frameResource = null;
        renderButton(closeButton, x, y, evt);
        return closeButton;
    }

    public static Button renderDeregisterButton(ScreenEvent.Render.Post evt, int x, int y) {
        CustomBuilding building = getCustomBuildingToEdit();

        boolean confirm = CustomBuildingClientEvents.deregisterConfirm;
        List<FormattedCharSequence> tooltips = confirm ? List.of(
            fcs(I18n.get("sandbox.reignofnether.custom_buildings.deregister.tooltip1"), true),
            fcs(I18n.get("sandbox.reignofnether.custom_buildings.deregister.confirm"))
        ) : List.of(
            fcs(I18n.get("sandbox.reignofnether.custom_buildings.deregister.tooltip1"), true),
            fcs(I18n.get("sandbox.reignofnether.custom_buildings.deregister.tooltip2"))
        );

        Button deregisterButton = new Button("Deregister Custom Building",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
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
                tooltips

        );
        deregisterButton.frameResource = null;
        renderButton(deregisterButton, x, y, evt);
        return deregisterButton;
    }

    public static Button renderCommandsMenuButton(ScreenEvent.Render.Post evt, int x, int y) {
        List<FormattedCharSequence> tooltips = CustomBuildingClientEvents.showCommandsMenu ?
            List.of(fcs(I18n.get("sandbox.reignofnether.custom_buildings.switch_to_general_menu"))) :
            List.of(fcs(I18n.get("sandbox.reignofnether.custom_buildings.switch_to_commands_menu")));

        Button commandsMenuButton = new ButtonBuilder("Toggle Commands Menu")
                .iconResource(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_back.png"))
                .onLeftClick(() -> {
                    CustomBuildingClientEvents.showCommandsMenu = !CustomBuildingClientEvents.showCommandsMenu;
                    CustomBuildingMenu.forceRefreshCommandEditBoxes();
                })
                .tooltipLines(tooltips)
                .build();
        renderButton(commandsMenuButton, x, y, evt);

        CustomBuilding building = getCustomBuildingToEdit();
        int commandCount = 0;
        if (building != null)
            for (CustomBuildingCommand command : building.commands)
                if (command.condition != CustomBuildingCommand.TriggerCondition.NONE && !command.commandStr.isBlank())
                    commandCount += 1;

        evt.getGuiGraphics().drawString(MC.font, I18n.get("sandbox.reignofnether.custom_buildings.commands_count", commandCount) ,x + 25, y + 7, 0xFFFFFF);
        return commandsMenuButton;
    }


    private static void renderButton(Button button, int x, int y, ScreenEvent.Render.Post evt) {
        if (!button.isHidden.get()) {
            button.render(evt.getGuiGraphics(), x, y, evt.getMouseX(), evt.getMouseY());
            if (button.isMouseOver(evt.getMouseX(), evt.getMouseY()) && button.tooltipLines != null)
                button.renderTooltip(evt.getGuiGraphics(), evt.getMouseX(), evt.getMouseY());
        }
    }

    public static List<Button> renderCustomisationButtons(ScreenEvent.Render.Post evt, CustomBuilding customBuilding, int x, int y) {
        int origX = x;
        int origY = y;
        ArrayList<Button> buttonsCol1 = new ArrayList<>();
        ArrayList<Button> buttonsCol2 = new ArrayList<>();

        buttonsCol1.add(new BooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_buildable_by_villagers.label"), customBuilding.buildableByVillagers,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_BUILDABLE_BY_VILLAGERS, customBuilding.name, !customBuilding.buildableByVillagers);
                    customBuilding.buildableByVillagers = !customBuilding.buildableByVillagers;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_buildable_by_villagers.tooltip1")
        ));
        buttonsCol1.add(new BooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_buildable_by_monsters.label"), customBuilding.buildableByMonsters,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_BUILDABLE_BY_MONSTERS, customBuilding.name, !customBuilding.buildableByMonsters);
                    customBuilding.buildableByMonsters = !customBuilding.buildableByMonsters;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_buildable_by_monsters.tooltip1")
        ));
        buttonsCol1.add(new BooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_buildable_by_piglins.label"), customBuilding.buildableByPiglins,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_BUILDABLE_BY_PIGLINS, customBuilding.name, !customBuilding.buildableByPiglins);
                    customBuilding.buildableByPiglins = !customBuilding.buildableByPiglins;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_buildable_by_piglins.tooltip1")
        ));
        buttonsCol1.add(new BooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_nether_terrain_only.label"), customBuilding.netherTerrainOnly,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_NETHER_TERRAIN_ONLY, customBuilding.name, !customBuilding.netherTerrainOnly);
                    customBuilding.netherTerrainOnly = !customBuilding.netherTerrainOnly;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_nether_terrain_only.tooltip1")
        ));

        Button setFoodCostButton = new IntegerButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_food_cost.label") + ": " + customBuilding.cost.food,
                () -> {
                    int value = Math.min(10000, customBuilding.cost.food + (Keybindings.shiftMod.isDown() ? 100 : 5));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_FOOD_COST, customBuilding.name, value);
                    customBuilding.cost.food = value;
                },
                () -> {
                    int value = Math.max(0, customBuilding.cost.food - (Keybindings.shiftMod.isDown() ? 100 : 5));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_FOOD_COST, customBuilding.name, value);
                    customBuilding.cost.food = value;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_food_cost.tooltip1")
        );
        setFoodCostButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wheat.png");
        buttonsCol1.add(setFoodCostButton);

        Button setWoodCostButton = new IntegerButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_wood_cost.label") + ": " + customBuilding.cost.wood,
                () -> {
                    int value = Math.min(10000, customBuilding.cost.wood + (Keybindings.shiftMod.isDown() ? 100 : 5));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_WOOD_COST, customBuilding.name, value);
                    customBuilding.cost.wood = value;
                },
                () -> {
                    int value = Math.max(0, customBuilding.cost.wood - (Keybindings.shiftMod.isDown() ? 100 : 5));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_WOOD_COST, customBuilding.name, value);
                    customBuilding.cost.wood = value;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_wood_cost.tooltip1")
        );
        setWoodCostButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wood.png");
        buttonsCol1.add(setWoodCostButton);

        Button setOreCostButton = new IntegerButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_ore_cost.label") + ": " + customBuilding.cost.ore,
                () -> {
                    int value = Math.min(10000, customBuilding.cost.ore + (Keybindings.shiftMod.isDown() ? 100 : 5));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_ORE_COST, customBuilding.name, value);
                    customBuilding.cost.ore = value;
                },
                () -> {
                    int value = Math.max(0, customBuilding.cost.ore - (Keybindings.shiftMod.isDown() ? 100 : 5));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_ORE_COST, customBuilding.name, value);
                    customBuilding.cost.ore = value;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_ore_cost.tooltip1")
        );
        setOreCostButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/iron_ore.png");
        buttonsCol1.add(setOreCostButton);

        buttonsCol2.add(new BooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_capturable.label"), customBuilding.capturable,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_CAPTURABLE, customBuilding.name, !customBuilding.capturable);
                    customBuilding.capturable = !customBuilding.capturable;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_capturable.tooltip1")
        ));
        buttonsCol2.add(new BooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_invulnerable.label"), customBuilding.invulnerable,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_INVULNERABLE, customBuilding.name, !customBuilding.invulnerable);
                    customBuilding.invulnerable = !customBuilding.invulnerable;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_invulnerable.tooltip1")
        ));
        buttonsCol2.add(new BooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_repairable.label"), customBuilding.repairable,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_REPAIRABLE, customBuilding.name, !customBuilding.repairable);
                    customBuilding.repairable = !customBuilding.repairable;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_repairable.tooltip1")
        ));
        buttonsCol2.add(new BooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_destroy_on_reset.label"), customBuilding.shouldDestroyOnReset,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_DESTROY_ON_RESET, customBuilding.name, !customBuilding.shouldDestroyOnReset);
                    customBuilding.shouldDestroyOnReset = !customBuilding.shouldDestroyOnReset;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_destroy_on_reset.tooltip1")
        ));

        Button setNightRadiusButton = new IntegerButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_night_radius.label") + ": " + customBuilding.nightRadius,
                () -> {
                    int value = Math.min(200, customBuilding.nightRadius + (Keybindings.shiftMod.isDown() ? 10 : 1));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_NIGHT_RADIUS, customBuilding.name, value);
                    customBuilding.nightRadius = value;
                },
                () -> {
                    int value = Math.max(0, customBuilding.nightRadius - (Keybindings.shiftMod.isDown() ? 10 : 1));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_NIGHT_RADIUS, customBuilding.name, value);
                    customBuilding.nightRadius = value;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_night_radius.tooltip1")
        );
        setNightRadiusButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/moon.png");
        buttonsCol2.add(setNightRadiusButton);

        Button setNetherRadiusButton = new IntegerButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_nether_radius.label") + ": " + customBuilding.netherRadius,
                () -> {
                    int value = Math.min(200, customBuilding.netherRadius + (Keybindings.shiftMod.isDown() ? 10 : 1));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_NETHER_RADIUS, customBuilding.name, value);
                    customBuilding.netherRadius = value;
                },
                () -> {
                    int value = Math.max(0, customBuilding.netherRadius - (Keybindings.shiftMod.isDown() ? 10 : 1));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_NETHER_RADIUS, customBuilding.name, value);
                    customBuilding.netherRadius = value;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_nether_radius.tooltip1")
        );
        setNetherRadiusButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/portal.png");
        buttonsCol2.add(setNetherRadiusButton);

        Button setGarrisonCapacityButton = new IntegerButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_garrison_capacity.label") + ": " + customBuilding.garrisonCapacity,
                () -> {
                    int value = Math.min(100, customBuilding.garrisonCapacity + (Keybindings.shiftMod.isDown() ? 10 : 1));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_GARRISON_CAPACITY, customBuilding.name, value);
                    customBuilding.garrisonCapacity = value;
                },
                () -> {
                    int value = Math.max(0, customBuilding.garrisonCapacity - (Keybindings.shiftMod.isDown() ? 10 : 1));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_GARRISON_CAPACITY, customBuilding.name, value);
                    customBuilding.garrisonCapacity = value;
                },
                getGarrisonTooltips(customBuilding, I18n.get("sandbox.reignofnether.custom_buildings.set_garrison_capacity.tooltip1"))
        );
        setGarrisonCapacityButton.iconResource = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/ladder.png");
        buttonsCol2.add(setGarrisonCapacityButton);

        Button setGarrisonBonusRangeButton = new IntegerButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_garrison_range.label") + ": " + customBuilding.garrisonRange,
                () -> {
                    int value = Math.min(100, customBuilding.garrisonRange + (Keybindings.shiftMod.isDown() ? 10 : 1));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_GARRISON_RANGE, customBuilding.name, value);
                    customBuilding.garrisonRange = value;
                },
                () -> {
                    int value = Math.max(0, customBuilding.garrisonRange - (Keybindings.shiftMod.isDown() ? 10 : 1));
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_GARRISON_RANGE, customBuilding.name, value);
                    customBuilding.garrisonRange = value;
                },
                getGarrisonTooltips(customBuilding, I18n.get("sandbox.reignofnether.custom_buildings.set_garrison_range.tooltip1"))
        );
        setGarrisonBonusRangeButton.iconResource = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/bow.png");
        buttonsCol2.add(setGarrisonBonusRangeButton);

        for (Button button : buttonsCol1) {
            renderButton(button, x, y, evt);
            y += 18;
        }
        x += 148;
        y = origY;
        for (Button button : buttonsCol2) {
            renderButton(button, x, y, evt);
            y += 18;
        }
        buttonsCol1.addAll(buttonsCol2);
        return buttonsCol1;
    }

    private static List<FormattedCharSequence> getGarrisonTooltips(CustomBuilding customBuilding, String originalTooltip) {
        ArrayList<FormattedCharSequence> tooltips = new ArrayList<>();
        tooltips.add(fcs(originalTooltip));
        if (customBuilding.numGarrisonZones <= 0) {
            tooltips.add(fcsIcons(I18n.get("sandbox.reignofnether.custom_buildings.garrison_warning.no_zones")));
        } else if (customBuilding.numGarrisonEntries <= 0) {
            tooltips.add(fcsIcons(I18n.get("sandbox.reignofnether.custom_buildings.garrison_warning.no_entry")));
        } else if (customBuilding.numGarrisonEntries > 1) {
            tooltips.add(fcsIcons(I18n.get("sandbox.reignofnether.custom_buildings.garrison_warning.multiple_entries")));
        }
        if (customBuilding.numGarrisonExits <= 0) {
            tooltips.add(fcsIcons(I18n.get("sandbox.reignofnether.custom_buildings.garrison_warning.no_exit")));
        } else if (customBuilding.numGarrisonExits > 1) {
            tooltips.add(fcsIcons(I18n.get("sandbox.reignofnether.custom_buildings.garrison_warning.multiple_exits")));
        }
        return tooltips;
    }

    private static final int MAX_COMMANDS = 8;
    private static final int COMMAND_ROW_HEIGHT = 16;
    private static final int EDIT_BOX_HEIGHT = 12;
    private static final int CONDITION_BUTTON_WIDTH = 32;
    private static final int TEXT_EDIT_BOX_WIDTH = 180;
    private static final int COOLDOWN_EDIT_BOX_WIDTH = 40;

    private static final ArrayList<MyEditBox> commandEditBoxes = new ArrayList<>();
    private static final ArrayList<MyEditBox> commandCooldownEditBoxes = new ArrayList<>();

    /** Tear down all currently-registered EditBoxes and rebuild them for the given building. */
    private static void registerCommandEditBoxes(CustomBuilding customBuilding, int x, int y) {
        forceRefreshCommandEditBoxes();

        int editBoxX = x + CONDITION_BUTTON_WIDTH;

        for (int i = 0; i < customBuilding.commands.size(); i++) {
            final int idx = i;
            int rowY = y + i * (COMMAND_ROW_HEIGHT + 2) + (COMMAND_ROW_HEIGHT - EDIT_BOX_HEIGHT) / 2;
            CustomBuildingCommand cmd = customBuilding.commands.get(idx);

            MyEditBox cdBox = new MyEditBox.Builder(editBoxX, rowY, COOLDOWN_EDIT_BOX_WIDTH, EDIT_BOX_HEIGHT)
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
                    .tooltipLines(List.of(fcs(I18n.get("sandbox.reignofnether.custom_buildings.commands.cooldown"))))
                    .build();

            TextInputClientEvents.registerEditBox(cdBox);
            commandCooldownEditBoxes.add(cdBox);

            MyEditBox textBox = new MyEditBox.Builder(editBoxX + COOLDOWN_EDIT_BOX_WIDTH + 5, rowY, TEXT_EDIT_BOX_WIDTH, EDIT_BOX_HEIGHT)
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

            TextInputClientEvents.registerEditBox(textBox);
            commandEditBoxes.add(textBox);
        }
    }

    // run this to force a rebuild on the next render frame
    public static void forceRefreshCommandEditBoxes() {
        for (MyEditBox box : commandEditBoxes)
            TextInputClientEvents.deregisterEditBox(box);
        for (MyEditBox box : commandCooldownEditBoxes)
            TextInputClientEvents.deregisterEditBox(box);
        commandEditBoxes.clear();
        commandCooldownEditBoxes.clear();
    }


    public static List<Button> renderCommandsButtonsAndInputs(ScreenEvent.Render.Post evt, CustomBuilding customBuilding, int x, int y) {
        ArrayList<Button> buttons = new ArrayList<>();

        CustomBuildingCommand.TriggerCondition[] conditions = CustomBuildingCommand.TriggerCondition.values();

        if (!customBuilding.commands.isEmpty())
            if (commandEditBoxes.isEmpty() || commandCooldownEditBoxes.isEmpty())
                registerCommandEditBoxes(customBuilding, x, y);

        for (int i = 0; i < customBuilding.commands.size(); i++) {
            final int idx = i;
            CustomBuildingCommand cmd = customBuilding.commands.get(idx);
            int rowY = y + i * (COMMAND_ROW_HEIGHT + 2);

            ResourceLocation condRl = switch (cmd.condition) {
                case ON_BUILD_COMPLETE          -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_pickaxe.png");
                case ON_DESTROY                 -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/tnt.png");
                case ON_DAMAGE_TAKEN            -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_sword.png");
                case ON_CAPTURE                 -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/nether_star.png");
                case OFF_COOLDOWN_IF_COMPLETE   -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/clock.png");
                case OFF_COOLDOWN_IF_GARRISONED -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/ladder.png");
                case NONE                       -> null;
            };

            String tooltip = switch (cmd.condition) {
                case ON_BUILD_COMPLETE          -> I18n.get("sandbox.reignofnether.custom_buildings.commands.trigger.on_build_complete");
                case ON_DESTROY                 -> I18n.get("sandbox.reignofnether.custom_buildings.commands.trigger.on_destroy");
                case ON_DAMAGE_TAKEN            -> I18n.get("sandbox.reignofnether.custom_buildings.commands.trigger.on_damage_taken");
                case ON_CAPTURE                 -> I18n.get("sandbox.reignofnether.custom_buildings.commands.trigger.on_capture");
                case OFF_COOLDOWN_IF_COMPLETE   -> I18n.get("sandbox.reignofnether.custom_buildings.commands.trigger.off_cooldown_if_complete");
                case OFF_COOLDOWN_IF_GARRISONED -> I18n.get("sandbox.reignofnether.custom_buildings.commands.trigger.off_cooldown_if_garrisoned");
                case NONE                       -> I18n.get("sandbox.reignofnether.custom_buildings.commands.trigger.none");
            };

            Button condButton = new ButtonBuilder("Cycle Trigger Condition " + idx)
                    .iconResource(condRl)
                    .onLeftClick(() -> {
                        int next = (cmd.condition.ordinal() + 1) % conditions.length;
                        cmd.condition = conditions[next];
                        CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_COMMAND_TRIGGER, customBuilding.name, idx, cmd.condition.name());
                    })
                    .onRightClick(() -> {
                        int prev = (cmd.condition.ordinal() - 1 + conditions.length) % conditions.length;
                        cmd.condition = conditions[prev];
                        CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_COMMAND_TRIGGER, customBuilding.name, idx, cmd.condition.name());
                    })
                    .tooltipLines(List.of(
                            fcs(tooltip)
                    ))
                    .iconSize(10)
                    .build();
            renderButton(condButton, x + 6, rowY - 3, evt);
            buttons.add(condButton);

            ArrayList<FormattedCharSequence> exceptions = new ArrayList<>();
            exceptions.add(fcs(I18n.get("sandbox.reignofnether.custom_buildings.commands.invalid"), true));
            for (CommandSyntaxException exception : cmd.getExceptions().values())
                exceptions.add(fcs(exception.getMessage()));
            Button validIndicator = new ButtonBuilder("Validity " + idx)
                    .iconResource(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/warning.png"))
                    .tooltipLines(exceptions)
                    .isHidden(cmd::isValid)
                    .iconSize(10)
                    .build();
            validIndicator.frameResource = null;
            renderButton(validIndicator, x + COOLDOWN_EDIT_BOX_WIDTH + TEXT_EDIT_BOX_WIDTH + 40, rowY - 3, evt);
            buttons.add(validIndicator);

            Button deleteButton = new ButtonBuilder("Delete Command " + idx)
                    .iconResource(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross.png"))
                    .onLeftClick(() -> {
                        customBuilding.commands.remove(idx);
                        CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.DELETE_COMMAND, customBuilding.name, idx);
                        forceRefreshCommandEditBoxes();
                    })
                    .tooltipLines(List.of(
                        fcs(I18n.get("sandbox.reignofnether.custom_buildings.commands.delete"))
                    ))
                    .iconSize(10)
                    .build();
            deleteButton.frameResource = null;
            renderButton(deleteButton, x + COOLDOWN_EDIT_BOX_WIDTH + TEXT_EDIT_BOX_WIDTH + 60, rowY - 3, evt);
            buttons.add(deleteButton);
        }

        if (customBuilding.commands.size() < MAX_COMMANDS) {
            int addBtnY = y + customBuilding.commands.size() * (COMMAND_ROW_HEIGHT + 2) + 4;
            Button addButton = new ButtonBuilder("Add Command")
                    .iconResource(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/corner_plus.png"))
                    .bgIconResource(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/chain_command_block_back.png"))
                    .onLeftClick(() -> {
                        if (customBuilding.commands.size() < MAX_COMMANDS)
                            customBuilding.commands.add(new CustomBuildingCommand());
                        CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.ADD_COMMAND, customBuilding.name);
                        forceRefreshCommandEditBoxes();
                    })
                    .tooltipLines(List.of(
                            fcs(I18n.get("sandbox.reignofnether.custom_buildings.commands.add"))))
                    .build();
            renderButton(addButton, x + 6, addBtnY, evt);
            buttons.add(addButton);
        }

        return buttons;
    }
}
