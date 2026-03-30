package com.solegendary.reignofnether.scenario;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.*;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ScenarioClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    private static boolean isMenuOpen = false;

    public static boolean isMenuOpen() {
        return isMenuOpen;
    }
    public static void setMenuOpen(boolean value) {
        isMenuOpen = value;
        if (!isMenuOpen)
            ScenarioMenu.unregisterRoleNameInput();
        else
            ScenarioMenu.registerRoleNameInput();
    }

    public static boolean confirmPublishScenario = false;

    // index 0 is treated as neutral
    public static final ArrayList<ScenarioRole> scenarioRoles = new ArrayList<>(List.of(
            new ScenarioRole(0),
            new ScenarioRole(1),
            new ScenarioRole(2),
            new ScenarioRole(3),
            new ScenarioRole(4),
            new ScenarioRole(5),
            new ScenarioRole(6),
            new ScenarioRole(7)
    ));
    private static int roleIndexToEdit = 0; // list index, not role.index
    private static int roleIndexToPlay = 0; // list index, not role.index
    private static final ArrayList<Button> renderedButtons = new ArrayList<>();
    private static final ArrayList<RectZone> hudZones = new ArrayList<>();

    public static void cycleRoleIndexToEdit(boolean reverse) {
        roleIndexToEdit += reverse ? -1 : 1;
        if (roleIndexToEdit < 0)
            roleIndexToEdit = scenarioRoles.size() - 1;
        else if (roleIndexToEdit >= scenarioRoles.size())
            roleIndexToEdit = 0;
        if (isMenuOpen())
            ScenarioMenu.reregisterRoleNameInput();
    }

    @Nullable
    public static ScenarioRole getScenarioRoleToEdit() {
        try {
            return scenarioRoles.get(roleIndexToEdit);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @Nullable
    public static Button getCycleRoleToPlayButton() {
        List<FormattedCharSequence> tooltipLines = new ArrayList<>();
        ScenarioRole role = scenarioRoles.get(roleIndexToPlay);
        tooltipLines.add(fcs(I18n.get("hud.gamemode.reignofnether.choose_scenario_role", role.name, role.faction.name())));

        if (role.isNpc) {
            tooltipLines.add(fcs(I18n.get("hud.gamemode.reignofnether.npc_scenario_role")));
        } else {
            for (RTSPlayer rtsPlayer : PlayerClientEvents.rtsPlayers) {
                if (rtsPlayer.scenarioRoleIndex == roleIndexToPlay) {
                    tooltipLines.add(fcs(I18n.get("hud.gamemode.reignofnether.taken_scenario_role", rtsPlayer.name)));
                    break;
                }
            }
        }
        tooltipLines.add(fcs(I18n.get("hud.gamemode.reignofnether.cycle_scenario_role")));

        if ((getNumScenarioUnits() == 0 && getNumScenarioBuildings() == 0) || getNumPlayerRoles() <= 0) {
            return null;
        }

        return new ButtonBuilder("Change Scenario Role")
                .iconResource(MiscUtil.getFactionIcon(role.faction))
                .onLeftClick(() -> {
                    roleIndexToPlay += 1;
                    if (roleIndexToPlay >= scenarioRoles.size())
                        roleIndexToPlay = 0;
                    goToNextVisibleRole(false);
                })
                .onRightClick(() -> {
                    roleIndexToPlay -= 1;
                    if (roleIndexToPlay < 0)
                        roleIndexToPlay = scenarioRoles.size() - 1;
                    goToNextVisibleRole(true);
                })
                .tooltipLines(tooltipLines)
                .build();
    }

    private static void goToNextVisibleRole(boolean reverse) {
        int i = 0;
        ScenarioRole role = ScenarioUtils.getScenarioRole(true, roleIndexToPlay);
        if (role != null) {
            while (getNumScenarioUnits(role) == 0 && getNumScenarioBuildings(role) == 0) {
                roleIndexToPlay += reverse ? -1 : 1;
                if (roleIndexToPlay >= scenarioRoles.size())
                    roleIndexToPlay = 0;
                if (roleIndexToPlay < 0)
                    roleIndexToPlay = scenarioRoles.size() - 1;
                role = ScenarioUtils.getScenarioRole(true, roleIndexToPlay);
                if (i++ > scenarioRoles.size() * 2 || role == null)
                    return;
            }
        }
    }

    public static Button getScenarioStartButton() {
        return new ButtonBuilder("Start Scenario")
                .iconResource(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/tick.png"))
                .isEnabled(() -> {
                    ScenarioRole role = ScenarioUtils.getScenarioRole(true, roleIndexToPlay);
                    if (role == null || role.isNpc || (getNumScenarioUnits(role) == 0 && getNumScenarioBuildings(role) == 0))
                        return false;
                    for (RTSPlayer rtsPlayer : PlayerClientEvents.rtsPlayers)
                        if (rtsPlayer.scenarioRoleIndex == roleIndexToPlay)
                            return false;
                    return true;
                })
                .onLeftClick(() -> PlayerServerboundPacket.startRTSScenario(roleIndexToPlay))
                .onRightClick(() -> {
                    roleIndexToPlay += 1;
                    if (roleIndexToPlay >= scenarioRoles.size())
                        roleIndexToPlay = 0;
                })
                .tooltipLines(List.of(
                        fcs(I18n.get("hud.gamemode.reignofnether.start_scenario"), true)
                )).build();
    }

    public static void cycleRoleUnits(boolean reverse) {
        List<LivingEntity> selUnits = UnitClientEvents.getSelectedUnits();
        LivingEntity selUnit = selUnits.isEmpty() ? null : selUnits.get(0);
        int id1 = selUnit != null ? selUnit.getId() : -1;

        ScenarioRole role = ScenarioClientEvents.getScenarioRoleToEdit();

        List<LivingEntity> eligible = new ArrayList<>();
        for (LivingEntity le : UnitClientEvents.getAllUnits())
            if (role != null && le instanceof Unit unit && unit.getScenarioRoleIndex() == role.index)
                eligible.add(le);

        eligible.sort(Comparator.comparingInt(LivingEntity::getId));

        if (eligible.isEmpty()) return;

        UnitClientEvents.clearSelectedUnits();

        if (Keybindings.shiftMod.isDown()) {
            for (LivingEntity le : eligible)
                UnitClientEvents.addSelectedUnit(le);
            return;
        }
        if (id1 < 0) { // Nothing selected — pick first (or last if reversing)
            UnitClientEvents.addSelectedUnit(reverse ? eligible.get(eligible.size() - 1) : eligible.get(0));
            return;
        }
        int currentIndex = -1;
        for (int i = 0; i < eligible.size(); i++) {
            if (eligible.get(i).getId() == id1) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex;
        if (currentIndex < 0)
            nextIndex = reverse ? eligible.size() - 1 : 0;
        else
            nextIndex = (currentIndex + (reverse ? -1 : 1) + eligible.size()) % eligible.size();
        UnitClientEvents.addSelectedUnit(eligible.get(nextIndex));

        if (!UnitClientEvents.getSelectedUnits().isEmpty()) {
            OrthoviewClientEvents.centreCameraOnPos(UnitClientEvents.getSelectedUnits().get(0).position());
        }
    }

    public static void cycleRoleBuildings(boolean reverse) {
        List<BuildingPlacement> selBpls = BuildingClientEvents.getSelectedBuildings();
        BuildingPlacement selBpl = selBpls.isEmpty() ? null : selBpls.get(0);
        long originHash = selBpl != null ? MyMath.getBlockPosHash(selBpl.originPos) : 0;

        ScenarioRole role = ScenarioClientEvents.getScenarioRoleToEdit();

        List<BuildingPlacement> eligible = new ArrayList<>();
        for (BuildingPlacement bpl : BuildingClientEvents.getBuildings())
            if (role != null && bpl.scenarioRoleIndex == role.index)
                eligible.add(bpl);

        eligible.sort(Comparator.comparingInt(bpl -> bpl.originPos.getX() * 100 + bpl.originPos.getY() + bpl.originPos.getZ()));

        if (eligible.isEmpty()) return;

        BuildingClientEvents.clearSelectedBuildings();

        if (Keybindings.shiftMod.isDown()) {
            for (BuildingPlacement bpl : eligible)
                BuildingClientEvents.addSelectedBuilding(bpl);
            return;
        }
        if (originHash == 0) { // Nothing selected — pick first (or last if reversing)
            BuildingClientEvents.addSelectedBuilding(reverse ? eligible.get(eligible.size() - 1) : eligible.get(0));
            return;
        }
        int currentIndex = -1;
        for (int i = 0; i < eligible.size(); i++) {
            if (originHash == MyMath.getBlockPosHash(eligible.get(i).originPos)) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex;
        if (currentIndex < 0)
            nextIndex = reverse ? eligible.size() - 1 : 0;
        else
            nextIndex = (currentIndex + (reverse ? -1 : 1) + eligible.size()) % eligible.size();
        BuildingClientEvents.addSelectedBuilding(eligible.get(nextIndex));

        if (!BuildingClientEvents.getSelectedBuildings().isEmpty()) {
            OrthoviewClientEvents.centreCameraOnPos(BuildingClientEvents.getSelectedBuildings().get(0).centrePos);
        }
    }

    public static void clearRoleUnits() {
        for (LivingEntity le : UnitClientEvents.getAllUnits()) {
            if (le instanceof Unit unit) {
                ScenarioServerboundPacket.setUnitRole(-1, le.getId());
                unit.setScenarioRoleIndex(-1);
            }
        }
    }

    public static void clearRoleBuildings() {
        for (BuildingPlacement bpl : BuildingClientEvents.getBuildings()) {
            ScenarioServerboundPacket.setBuildingRole(-1, bpl.originPos);
            bpl.scenarioRoleIndex = -1;
        }
    }

    public static int getNumScenarioUnits() {
        int count = 0;
        for (LivingEntity le : UnitClientEvents.getAllUnits()) {
            if (le instanceof Unit unit && unit.getScenarioRoleIndex() >= 0) {
                count++;
            }
        }
        return count;
    }

    public static int getNumScenarioBuildings() {
        int count = 0;
        for (BuildingPlacement bpl : BuildingClientEvents.getBuildings()) {
            if (bpl.scenarioRoleIndex >= 0) {
                count++;
            }
        }
        return count;
    }

    public static int getNumScenarioUnits(ScenarioRole role) {
        int count = 0;
        for (LivingEntity le : UnitClientEvents.getAllUnits()) {
            if (role != null && le instanceof Unit unit && unit.getScenarioRoleIndex() == role.index) {
                count++;
            }
        }
        return count;
    }

    public static int getNumScenarioBuildings(ScenarioRole role) {
        int count = 0;
        for (BuildingPlacement bpl : BuildingClientEvents.getBuildings()) {
            if (role != null && bpl.scenarioRoleIndex == role.index) {
                count++;
            }
        }
        return count;
    }

    public static int getNumPlayerRoles() {
        int count = 0;
        for (ScenarioRole role : scenarioRoles) {
            if (!role.isNpc)
                count++;
        }
        return count;
    }

    public static void pressedPublishScenarioButton() {
        if (!confirmPublishScenario) {
            confirmPublishScenario = true;
        } else {
            if (getNumScenarioBuildings() == 0 && getNumScenarioUnits() == 0) {
                HudClientEvents.showTemporaryMessage(I18n.get("sandbox.reignofnether.publish_scenario_tooltip_error1"));
            } else if (getNumPlayerRoles() <= 0) {
                HudClientEvents.showTemporaryMessage(I18n.get("sandbox.reignofnether.publish_scenario_tooltip_error2"));
            } else {
                PlayerServerboundPacket.publishScenario();
                setMenuOpen(false);
            }
            confirmPublishScenario = false;
        }
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
        hudZones.clear();
        renderedButtons.clear();
        if (MC.screen instanceof TopdownGui && isMenuOpen()) {
            int width = 254;
            int height = 160;
            int blitX = MC.screen.width - width - 120;
            int blitY = 5;
            MyRenderer.renderFrameWithBg(evt.getGuiGraphics(), blitX, blitY, width, height, 0xA0000000);

            ScenarioRole role = getScenarioRoleToEdit();
            if (role != null) {
                renderedButtons.addAll(ScenarioMenu.renderRoleNameAndCycleButtons(evt, getScenarioRoleToEdit(), blitX + 14, blitY + 14));
                renderedButtons.add(ScenarioMenu.renderHelpButton(evt, blitX + width - Button.itemIconSize - 32, blitY + 4));
                renderedButtons.add(ScenarioMenu.renderCloseButton(evt, blitX + width - Button.itemIconSize - 12, blitY + 4));
                renderedButtons.addAll(ScenarioMenu.renderCustomisationButtonsColumn1(evt, blitX + 16, blitY + 56));
                renderedButtons.addAll(ScenarioMenu.renderCustomisationButtonsColumn2(evt, blitX + 140, blitY + 56));
                renderedButtons.add(ScenarioMenu.renderResetRoleButton(evt, blitX + width - Button.itemIconSize - 12, blitY + height - 26));
            }
            hudZones.add(new RectZone(blitX, blitY, blitX + width, blitY + height));
        }
    }

    public static boolean isMouseOverHud(int mouseX, int mouseY) {
        for (RectZone hudZone : hudZones)
            if (hudZone.isMouseOver(mouseX, mouseY))
                return true;
        return false;
    }

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Post evt) {
        for (Button button : renderedButtons) {
            if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), true);
            } else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), false);
            }
        }
    }
}
