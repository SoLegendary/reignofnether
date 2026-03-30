package com.solegendary.reignofnether.scenario;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.hud.buttons.BooleanButton;
import com.solegendary.reignofnether.hud.buttons.IntegerButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.faction.Faction.*;
import static com.solegendary.reignofnether.scenario.ScenarioClientEvents.getScenarioRoleToEdit;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ScenarioMenu {

    private static Minecraft MC = Minecraft.getInstance();

    private static EditBox roleNameInput = registerRoleNameInput();

    public static void reregisterRoleNameInput() {
        unregisterRoleNameInput();
        registerRoleNameInput();
    }

    public static EditBox registerRoleNameInput() {
        if (roleNameInput == null) {
            ScenarioRole role = ScenarioClientEvents.getScenarioRoleToEdit();
            if (role != null) {
                roleNameInput = TextInputClientEvents.register(0, 0, 120, 12,
                        value -> ScenarioServerboundPacket.setRoleName(role.index, value));
                roleNameInput.setMaxLength(32);
                roleNameInput.setFocused(true);
                roleNameInput.setBordered(true);
                roleNameInput.setValue(role.name);
                roleNameInput.setResponder(value -> role.name = value);
            }
        }
        return roleNameInput;
    }

    public static void unregisterRoleNameInput() {
        TextInputClientEvents.unregister(roleNameInput);
        roleNameInput = null;
    }

    public static List<Button> renderRoleNameAndCycleButtons(ScreenEvent.Render.Post evt, ScenarioRole role, int x, int y) {
        int xr = x + 5;
        int yr = y;
        evt.getGuiGraphics().drawString(MC.font, fcs(I18n.get("sandbox.reignofnether.scenario.role_number", role.index + 1), true), xr, yr, 0xFFFFFF);
        evt.getGuiGraphics().drawString(MC.font, fcs(I18n.get("sandbox.reignofnether.scenario.role_name"), true), xr, yr + 24, 0xFFFFFF);

        roleNameInput.setX(xr + 35);
        roleNameInput.setY(yr + 22);
        roleNameInput.render(evt.getGuiGraphics(), evt.getMouseX(), evt.getMouseY(), evt.getPartialTick());

        Button cycleButtonBackward = new Button("Cycle Role Backward",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/tutorial_arrow_left.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> ScenarioClientEvents.cycleRoleIndexToEdit(true),
                null,
                List.of()
        );
        Button cycleButtonForward = new Button("Cycle Role Forward",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/tutorial_arrow_right.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> ScenarioClientEvents.cycleRoleIndexToEdit(false),
                null,
                List.of()
        );
        xr += 110;
        yr -= 7;
        renderButton(cycleButtonBackward, xr, yr, evt);
        xr += Button.DEFAULT_ICON_FRAME_SIZE;
        renderButton(cycleButtonForward, xr, yr, evt);
        return List.of(cycleButtonBackward, cycleButtonForward);
    }

    public static List<Button> renderCustomisationButtonsColumn2(ScreenEvent.Render.Post evt, int x, int y) {
        Button unitsButton = new Button("Role Units",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/spawn_egg.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> ScenarioClientEvents.cycleRoleUnits(false),
                () -> {
                    if (Keybindings.shiftMod.isDown() && Keybindings.ctrlMod.isDown()) {
                        ScenarioClientEvents.clearRoleUnits();
                        UnitClientEvents.clearSelectedUnits();
                    } else {
                        ScenarioClientEvents.cycleRoleUnits(true);
                    }
                },
                List.of(
                        fcs(I18n.get("sandbox.reignofnether.scenario.select_role_units")),
                        fcs(I18n.get("sandbox.reignofnether.scenario.select_all_shift")),
                        fcs(I18n.get("sandbox.reignofnether.scenario.clear_all"))
                )
        );
        Button buildingsButton = new Button("Role Buildings",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/crafting_table_front.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> ScenarioClientEvents.cycleRoleBuildings(false),
                () -> {
                    if (Keybindings.shiftMod.isDown() && Keybindings.ctrlMod.isDown()) {
                        ScenarioClientEvents.clearRoleBuildings();
                        BuildingClientEvents.clearSelectedBuildings();
                    } else {
                        ScenarioClientEvents.cycleRoleBuildings(true);
                    }
                },
                List.of(
                        fcs(I18n.get("sandbox.reignofnether.scenario.select_role_buildings")),
                        fcs(I18n.get("sandbox.reignofnether.scenario.select_all_shift")),
                        fcs(I18n.get("sandbox.reignofnether.scenario.clear_all"))
                )
        );
        int xr = x;
        int yr = y;

        ScenarioRole roleToEdit = getScenarioRoleToEdit();

        renderButton(unitsButton, xr, yr, evt);
        String unitLabel = I18n.get("sandbox.reignofnether.scenario.role_units", ScenarioClientEvents.getNumScenarioUnits(roleToEdit));
        evt.getGuiGraphics().drawString(MC.font, unitLabel, xr + 27, yr + 7, 0xFFFFFF);

        yr += Button.DEFAULT_ICON_FRAME_SIZE;

        renderButton(buildingsButton, xr, yr, evt);
        String buildingLabel = I18n.get("sandbox.reignofnether.scenario.role_buildings", ScenarioClientEvents.getNumScenarioBuildings(roleToEdit));
        evt.getGuiGraphics().drawString(MC.font, buildingLabel, xr + 27, yr + 7, 0xFFFFFF);

        yr += Button.DEFAULT_ICON_FRAME_SIZE;

        ScenarioRole role = getScenarioRoleToEdit();
        if (role != null) {
            Button npcRoleButton = new BooleanButton(
                    role.isNpc ? I18n.get("sandbox.reignofnether.scenario.npc_role_true") :
                                I18n.get("sandbox.reignofnether.scenario.npc_role_false"),
                    role.isNpc,
                    () -> {
                        role.isNpc = !role.isNpc;
                        ScenarioServerboundPacket.setRoleIsNpc(role.index, role.isNpc);
                    },
                    I18n.get("sandbox.reignofnether.scenario.npc_role_tooltip")
            );
            renderButton(npcRoleButton, xr, yr, evt);
            return List.of(buildingsButton, unitsButton, npcRoleButton);
        } else {
            return List.of();
        }
    }

    public static Button renderCloseButton(ScreenEvent.Render.Post evt, int x, int y) {
        Button closeButton = new Button("Close & Save Scenario",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross_square.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    ScenarioClientEvents.setMenuOpen(false);
                    ScenarioServerboundPacket.saveScenario();
                    HudClientEvents.showTemporaryMessage(I18n.get("sandbox.reignofnether.scenario.saved_all_roles"));
                },
                null,
                List.of(fcs(I18n.get("sandbox.reignofnether.scenario.close_and_save")))
        );
        closeButton.frameResource = null;
        renderButton(closeButton, x, y, evt);
        return closeButton;
    }

    public static Button renderHelpButton(ScreenEvent.Render.Post evt, int x, int y) {
        Button closeButton = new Button("Scenario Help",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/help.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                List.of(
                    fcs(I18n.get("sandbox.reignofnether.scenario.help_tooltip1")),
                    fcs(I18n.get("sandbox.reignofnether.scenario.help_tooltip2")),
                    fcs(I18n.get("sandbox.reignofnether.scenario.help_tooltip3"))
                )
        );
        closeButton.frameResource = null;
        renderButton(closeButton, x, y, evt);
        return closeButton;
    }

    public static Button renderResetRoleButton(ScreenEvent.Render.Post evt, int x, int y) {
        Button deregisterButton = new Button("Reset Scenario Role",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                () -> {
                    if (!Keybindings.shiftMod.isDown() || !Keybindings.ctrlMod.isDown())
                        return;
                    ScenarioRole role = getScenarioRoleToEdit();
                    if (role != null) {
                        role.faction = NEUTRAL;
                        ScenarioServerboundPacket.setRoleFaction(role.index, NEUTRAL);
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
                },
                List.of(
                        fcs(I18n.get("sandbox.reignofnether.scenario.reset_role.tooltip1"), true),
                        fcs(I18n.get("sandbox.reignofnether.scenario.reset_role.tooltip2"))
                )
        );
        deregisterButton.frameResource = null;
        renderButton(deregisterButton, x, y, evt);
        return deregisterButton;
    }

    public static List<Button> renderCustomisationButtonsColumn1(ScreenEvent.Render.Post evt, int x, int y) {
        int origX = x;
        int origY = y;
        ArrayList<Button> buttons = new ArrayList<>();
        ScenarioRole role = getScenarioRoleToEdit();

        if (role == null)
            return List.of();

        // TODO:
        // Scenario Name
        // Scenario opening message
        //
        // Per role:
        // - NPC role (prevents players choosing it to start)

        Button factionButton = new Button(
            "Toggle Faction",
            Button.itemIconSize,
            MiscUtil.getFactionIcon(role.faction),
            (Keybinding) null,
            () -> false,
            () -> false,
            () -> true,
            () -> {
                switch (role.faction) {
                    case VILLAGERS -> {
                        role.faction = MONSTERS;
                        ScenarioServerboundPacket.setRoleFaction(role.index, MONSTERS);
                    }
                    case MONSTERS -> {
                        role.faction = PIGLINS;
                        ScenarioServerboundPacket.setRoleFaction(role.index, PIGLINS);
                    }
                    case PIGLINS -> {
                        role.faction = NEUTRAL;
                        ScenarioServerboundPacket.setRoleFaction(role.index, NEUTRAL);
                    }
                    case NONE, NEUTRAL -> {
                        role.faction = VILLAGERS;
                        ScenarioServerboundPacket.setRoleFaction(role.index, VILLAGERS);
                    }
                }

            },
            () -> {
                switch (role.faction) {
                    case VILLAGERS -> {
                        role.faction = NEUTRAL;
                        ScenarioServerboundPacket.setRoleFaction(role.index, NEUTRAL);
                    }
                    case MONSTERS -> {
                        role.faction = VILLAGERS;
                        ScenarioServerboundPacket.setRoleFaction(role.index, VILLAGERS);
                    }
                    case PIGLINS -> {
                        role.faction = MONSTERS;
                        ScenarioServerboundPacket.setRoleFaction(role.index, MONSTERS);
                    }
                    case NONE, NEUTRAL -> {
                        role.faction = PIGLINS;
                        ScenarioServerboundPacket.setRoleFaction(role.index, PIGLINS);
                    }
                }
            },
            List.of()
        );
        String factionStr = switch (role.faction) {
            case VILLAGERS -> I18n.get("hud.faction.reignofnether.villager");
            case MONSTERS -> I18n.get("hud.faction.reignofnether.monster");
            case PIGLINS -> I18n.get("hud.faction.reignofnether.piglin");
            case NONE, NEUTRAL -> I18n.get("hud.faction.reignofnether.neutral");
        };
        String label = I18n.get("sandbox.reignofnether.faction_button1", factionStr);
        evt.getGuiGraphics().drawString(MC.font, label, x + 27, y + 7, 0xFFFFFF);
        renderButton(factionButton, x, y, evt);

        Button setStartingFoodButton = new IntegerButton(
                I18n.get("sandbox.reignofnether.scenario.starting_food") + ": " + role.startingResources.food,
                () -> {
                    int value = Math.min(10000, role.startingResources.food + (Keybindings.shiftMod.isDown() ? 100 : 5));
                    ScenarioServerboundPacket.setStartingResources(role.index, ResourceName.FOOD, value);
                    role.startingResources.food = value;
                },
                () -> {
                    int value = Math.max(0, role.startingResources.food - (Keybindings.shiftMod.isDown() ? 100 : 5));
                    ScenarioServerboundPacket.setStartingResources(role.index, ResourceName.FOOD, value);
                    role.startingResources.food = value;
                }
        );
        setStartingFoodButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wheat.png");
        buttons.add(setStartingFoodButton);

        Button setStartingWoodButton = new IntegerButton(
                I18n.get("sandbox.reignofnether.scenario.starting_wood") + ": " + role.startingResources.wood,
                () -> {
                    int value = Math.min(10000, role.startingResources.wood + (Keybindings.shiftMod.isDown() ? 100 : 5));
                    ScenarioServerboundPacket.setStartingResources(role.index, ResourceName.WOOD, value);
                    role.startingResources.wood = value;
                },
                () -> {
                    int value = Math.max(0, role.startingResources.wood - (Keybindings.shiftMod.isDown() ? 100 : 5));
                    ScenarioServerboundPacket.setStartingResources(role.index, ResourceName.WOOD, value);
                    role.startingResources.wood = value;
                }
        );
        setStartingWoodButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wood.png");
        buttons.add(setStartingWoodButton);

        Button setStartingOreButton = new IntegerButton(
                I18n.get("sandbox.reignofnether.scenario.starting_ore") + ": " + role.startingResources.ore,
                () -> {
                    int value = Math.min(10000, role.startingResources.ore + (Keybindings.shiftMod.isDown() ? 100 : 5));
                    ScenarioServerboundPacket.setStartingResources(role.index, ResourceName.ORE, value);
                    role.startingResources.ore = value;
                },
                () -> {
                    int value = Math.max(0, role.startingResources.ore - (Keybindings.shiftMod.isDown() ? 100 : 5));
                    ScenarioServerboundPacket.setStartingResources(role.index, ResourceName.ORE, value);
                    role.startingResources.ore = value;
                }
        );
        setStartingOreButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/iron_ore.png");
        buttons.add(setStartingOreButton);

        Button setTeamButton = new IntegerButton(
                I18n.get("sandbox.reignofnether.scenario.team_number", role.teamNumber),
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
                },
                I18n.get("sandbox.reignofnether.scenario.team_number_tooltip")
        );
        setTeamButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/sweet_berries.png");
        buttons.add(setTeamButton);

        y += 20;
        for (Button button : buttons) {
            renderButton(button, x, y, evt);
            y += 18;
        }
        buttons.add(factionButton);
        return buttons;
    }

    private static void renderButton(Button button, int x, int y, ScreenEvent.Render.Post evt) {
        if (!button.isHidden.get()) {
            button.render(evt.getGuiGraphics(), x, y, evt.getMouseX(), evt.getMouseY());
            if (button.isMouseOver(evt.getMouseX(), evt.getMouseY()) && button.tooltipLines != null)
                button.renderTooltip(evt.getGuiGraphics(), evt.getMouseX(), evt.getMouseY());
        }
    }
}
