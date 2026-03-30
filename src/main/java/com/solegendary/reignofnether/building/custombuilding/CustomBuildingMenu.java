package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.ButtonBuilder;
import com.solegendary.reignofnether.hud.PortraitRendererBuilding;
import com.solegendary.reignofnether.hud.buttons.BooleanButton;
import com.solegendary.reignofnether.hud.buttons.IntegerButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
                .onLeftClick(() -> CustomBuildingClientEvents.showCommandsMenu = !CustomBuildingClientEvents.showCommandsMenu)
                .tooltipLines(tooltips)
                .build();
        renderButton(commandsMenuButton, x, y, evt);

        CustomBuilding building = getCustomBuildingToEdit();
        int commandCount = 0;
        if (building != null)
            for (CustomBuildingCommand command : building.commands)
                if (command.condition != CustomBuildingCommand.TriggerCondition.NONE && !command.command.isBlank())
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

    public static List<Button> renderCommandsButtonsAndInputs(ScreenEvent.Render.Post evt, CustomBuilding customBuilding, int x, int y) {
        //TODO
        evt.getGuiGraphics().drawString(MC.font, fcs("Coming soon!"), x + 10, y + 10, 0xFFFFFF);
        return List.of();
    }
}
