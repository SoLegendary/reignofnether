package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.gamerules.GameruleServerboundPacket;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.PortraitRendererBuilding;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents.getCustomBuildingToEdit;
import static com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents.setCustomBuildingToEdit;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

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
        Button deregisterButton = new Button("Deregister Custom Building",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    CustomBuildingServerboundPacket.deregisterBuilding(building.name);
                    CustomBuildingClientEvents.customBuildings.removeIf(b -> b.name.equals(building.name));
                    BuildingClientEvents.clearSelectedBuildings();
                    BuildingClientEvents.getBuildings().removeIf(b -> b.getBuilding().name.equals(building.name));
                    setCustomBuildingToEdit(null);
                },
                null,
                List.of(
                        fcs(I18n.get("sandbox.reignofnether.custom_buildings.deregister.tooltip1"), true),
                        fcs(I18n.get("sandbox.reignofnether.custom_buildings.deregister.tooltip2"))
                )
        );
        deregisterButton.frameResource = null;
        renderButton(deregisterButton, x, y, evt);
        return deregisterButton;
    }

    private static class CustomBuildingBooleanButton extends Button {
        private final String label;
        public CustomBuildingBooleanButton(String label, boolean enabled, Runnable onLeftClick, String tooltip) {
            super(
                    "Boolean Customise Building",
                    10,
                    enabled ? ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/tick.png") :
                            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross.png"),
                    (Keybinding) null,
                    () -> false,
                    () -> false,
                    () -> true,
                    onLeftClick,
                    null,
                    tooltip != null ? List.of(fcs(tooltip)) : null
            );
            this.label = label;
            this.frameResource = null;
        }
        @Override
        public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
            super.render(guiGraphics, x, y, mouseX, mouseY);
            if (!label.isBlank())
                guiGraphics.drawString(MC.font, label,x + 23, y + 7, 0xFFFFFF);
        }
        @Override
        public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            if (tooltipLines != null)
                MyRenderer.renderTooltip(guiGraphics, tooltipLines, mouseX, mouseY + tooltipOffsetY - 10);
        }
    }

    private static class CustomBuildingIntegerButton extends Button {
        private final String label;
        public CustomBuildingIntegerButton(String label, Runnable onLeftClick, Runnable onRightClick, String tooltip) {
            super(
                    "Integer Customise Building",
                    10,
                    ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_back.png"),
                    (Keybinding) null,
                    () -> false,
                    () -> false,
                    () -> true,
                    onLeftClick,
                    onRightClick,
                    tooltip != null ? List.of(fcs(tooltip)) : null
            );
            this.label = label;
        }
        @Override
        public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
            super.render(guiGraphics, x, y, mouseX, mouseY);
            if (!label.isBlank())
                guiGraphics.drawString(MC.font, label, x + 23, y + 7, 0xFFFFFF);
        }
        @Override
        public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            if (tooltipLines != null)
                MyRenderer.renderTooltip(guiGraphics, tooltipLines, mouseX, mouseY + tooltipOffsetY - 10);
        }
    }

    private static void renderButton(Button button, int x, int y, ScreenEvent.Render.Post evt) {
        if (!button.isHidden.get()) {
            button.render(evt.getGuiGraphics(), x, y, evt.getMouseX(), evt.getMouseY());
            if (button.isMouseOver(evt.getMouseX(), evt.getMouseY()) && button.tooltipLines != null)
                button.renderTooltip(evt.getGuiGraphics(), evt.getMouseX(), evt.getMouseY());
        }
    }

    public static List<Button> renderCustomisationButtons(ScreenEvent.Render.Post evt, int x, int y) {
        int origX = x;
        int origY = y;
        ArrayList<Button> buttonsCol1 = new ArrayList<>();
        ArrayList<Button> buttonsCol2 = new ArrayList<>();
        CustomBuilding customBuilding = getCustomBuildingToEdit();

        buttonsCol2.add(new CustomBuildingBooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_capturable.label"), customBuilding.capturable,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_CAPTURABLE, customBuilding.name, !customBuilding.capturable);
                    customBuilding.capturable = !customBuilding.capturable;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_capturable.tooltip1")
        ));
        buttonsCol2.add(new CustomBuildingBooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_invulnerable.label"), customBuilding.invulnerable,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_INVULNERABLE, customBuilding.name, !customBuilding.invulnerable);
                    customBuilding.invulnerable = !customBuilding.invulnerable;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_invulnerable.tooltip1")
        ));

        Button setNightRadiusButton = new CustomBuildingIntegerButton(
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

        Button setNetherRadiusButton = new CustomBuildingIntegerButton(
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

        buttonsCol1.add(new CustomBuildingBooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_buildable_by_villagers.label"), customBuilding.buildableByVillagers,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_BUILDABLE_BY_VILLAGERS, customBuilding.name, !customBuilding.buildableByVillagers);
                    customBuilding.buildableByVillagers = !customBuilding.buildableByVillagers;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_buildable_by_villagers.tooltip1")
        ));
        buttonsCol1.add(new CustomBuildingBooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_buildable_by_monsters.label"), customBuilding.buildableByMonsters,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_BUILDABLE_BY_VILLAGERS, customBuilding.name, !customBuilding.buildableByMonsters);
                    customBuilding.buildableByMonsters = !customBuilding.buildableByMonsters;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_buildable_by_monsters.tooltip1")
        ));
        buttonsCol1.add(new CustomBuildingBooleanButton(
                I18n.get("sandbox.reignofnether.custom_buildings.set_buildable_by_piglins.label"), customBuilding.buildableByPiglins,
                () -> {
                    CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_BUILDABLE_BY_PIGLINS, customBuilding.name, !customBuilding.buildableByPiglins);
                    customBuilding.buildableByPiglins = !customBuilding.buildableByPiglins;
                },
                I18n.get("sandbox.reignofnether.custom_buildings.set_buildable_by_piglins.tooltip1")
        ));

        Button setFoodCostButton = new CustomBuildingIntegerButton(
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

        Button setWoodCostButton = new CustomBuildingIntegerButton(
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

        Button setOreCostButton = new CustomBuildingIntegerButton(
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

        for (Button button : buttonsCol1) {
            renderButton(button, x, y, evt);
            y += 18;
        }
        x += 150;
        y = origY;
        for (Button button : buttonsCol2) {
            renderButton(button, x, y, evt);
            y += 18;
        }
        return Stream.of(buttonsCol1, buttonsCol2).flatMap(List::stream).collect(Collectors.toList());
    }
}
