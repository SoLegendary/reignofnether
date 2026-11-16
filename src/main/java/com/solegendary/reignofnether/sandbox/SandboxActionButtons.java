package com.solegendary.reignofnether.sandbox;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.scenario.ScenarioClientEvents;
import com.solegendary.reignofnether.scenario.ScenarioRole;
import com.solegendary.reignofnether.scenario.ScenarioServerboundPacket;
import com.solegendary.reignofnether.scenario.ScenarioUtils;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.ArrayUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

// action buttons but only for neutral units in sandbox

public class SandboxActionButtons {

    public static Button setAnchor;
    public static Button resetToAnchor;
    public static Button removeAnchor;
    public static Button removeBuildingPlacement;

    private static boolean selectedUnitsHaveAnchor() {
        for (LivingEntity entity : UnitClientEvents.getSelectedUnits())
            if (entity instanceof Unit unit && unit.getAnchor() != null && !unit.getAnchor().equals(new BlockPos(0, 0, 0)))
                return true;
        return false;
    }

    public static void updateButtons() {
        setAnchor = new Button(
                "Set Anchor",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/respawn_anchor_side4.png"),
                (Keybinding) null,
                () -> CursorClientEvents.getLeftClickSandboxAction() == SandboxAction.SET_ANCHOR,
                () -> !SandboxClientEvents.isSandboxPlayer(),
                () -> true,
                () -> CursorClientEvents.setLeftClickSandboxAction(SandboxAction.SET_ANCHOR),
                null,
                List.of(
                        fcs(I18n.get("hud.actionbuttons.reignofnether.set_anchor"), true),
                        fcs(I18n.get("hud.actionbuttons.reignofnether.set_anchor.tooltip1"))
                )
        );
        resetToAnchor = new Button(
                "Reset to Anchor",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/respawn_anchor_top_off.png"),
                (Keybinding) null,
                () -> CursorClientEvents.getLeftClickSandboxAction() == SandboxAction.RESET_TO_ANCHOR,
                () -> !SandboxClientEvents.isSandboxPlayer(),
                SandboxActionButtons::selectedUnitsHaveAnchor,
                () -> {
                    SandboxServerboundPacket.resetToAnchor(ArrayUtil.livingEntityListToIdArray(UnitClientEvents.getSelectedUnits()));
                    for (LivingEntity entity : UnitClientEvents.getSelectedUnits())
                        if (entity instanceof Unit unit)
                            Unit.fullResetBehaviours(unit);
                },
                null,
                List.of(
                        fcs(I18n.get("hud.actionbuttons.reignofnether.reset_to_anchor"), true),
                        fcs(I18n.get("hud.actionbuttons.reignofnether.reset_to_anchor.tooltip1"))
                )
        );
        removeAnchor = new Button(
                "Remove Anchor",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
                (Keybinding) null,
                () -> CursorClientEvents.getLeftClickSandboxAction() == SandboxAction.REMOVE_ANCHOR,
                () -> !SandboxClientEvents.isSandboxPlayer(),
                SandboxActionButtons::selectedUnitsHaveAnchor,
                () -> SandboxServerboundPacket.removeAnchor(ArrayUtil.livingEntityListToIdArray(UnitClientEvents.getSelectedUnits())),
                null,
                List.of(
                        fcs(I18n.get("hud.actionbuttons.reignofnether.remove_anchor"), true)
                )
        );
        removeBuildingPlacement = new Button(
                "Remove Building Placement",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
                (Keybinding) null,
                () -> false,
                () -> !SandboxClientEvents.isSandboxPlayer(),
                () -> true,
                () -> {
                    if (HudClientEvents.hudSelectedPlacement != null) {
                        SandboxServerboundPacket.removeBuilding(HudClientEvents.hudSelectedPlacement.originPos);
                    }
                },
                null,
                List.of(
                        fcs(I18n.get("hud.actionbuttons.reignofnether.remove_building"), true),
                        fcs(I18n.get("hud.actionbuttons.reignofnether.remove_building.tooltip1"))
                )
        );
    }

    public static Relationship getSelectionRelationship() {
        if (HudClientEvents.hudSelectedEntity != null)
            return UnitClientEvents.getPlayerToEntityRelationship(HudClientEvents.hudSelectedEntity);
        if (HudClientEvents.hudSelectedPlacement != null)
            return BuildingClientEvents.getPlayerToBuildingRelationship(HudClientEvents.hudSelectedPlacement);
        return Relationship.OWNED;
    }

    public static void setSelectionOwner(Relationship relationship) {
        String ownerName = SandboxClientEvents.ownerName(relationship);
        for (LivingEntity entity : UnitClientEvents.getSelectedUnits())
            if (entity instanceof Unit unit)
                unit.setOwnerName(ownerName);
        if (HudClientEvents.hudSelectedEntity instanceof Unit)
            SandboxServerboundPacket.setUnitOwner(ArrayUtil.livingEntityListToIdArray(UnitClientEvents.getSelectedUnits()), ownerName);
        for (BuildingPlacement bpl : BuildingClientEvents.getSelectedBuildings()) {
            bpl.ownerName = ownerName;
            SandboxServerboundPacket.setBuildingOwner(bpl.originPos, ownerName);
        }
        updateButtons();
    }

    public static void cycleUnitOrBuildingRoleIndex(boolean reverse) {
        int currentRoleIndex = 0;
        if (HudClientEvents.hudSelectedPlacement != null) {
            currentRoleIndex = HudClientEvents.hudSelectedPlacement.scenarioRoleIndex;
        } else if (HudClientEvents.hudSelectedEntity instanceof Unit unit) {
            currentRoleIndex = unit.getScenarioRoleIndex();
        }
        currentRoleIndex += reverse ? -1 : 1;
        if (currentRoleIndex > ScenarioClientEvents.scenarioRoles.size() - 1)
            currentRoleIndex = -1;
        if (currentRoleIndex < -1)
            currentRoleIndex = ScenarioClientEvents.scenarioRoles.size() - 1;

        for (LivingEntity le : UnitClientEvents.getSelectedUnits()) {
            if (le instanceof Unit unit) {
                ScenarioServerboundPacket.setUnitRole(currentRoleIndex, le.getId());
                unit.setScenarioRoleIndex(currentRoleIndex);
            }
        }
        for (BuildingPlacement bpl : BuildingClientEvents.getSelectedBuildings()) {
            ScenarioServerboundPacket.setBuildingRole(currentRoleIndex, bpl.originPos);
            bpl.scenarioRoleIndex = currentRoleIndex;
        }
    }

    @Nullable
    public static ScenarioRole getHudSelectedScenarioRole() {
        ScenarioRole role = null;
        if (HudClientEvents.hudSelectedEntity instanceof Unit unit) {
            role = ScenarioUtils.getScenarioRole(true, unit.getScenarioRoleIndex());
        } else if (HudClientEvents.hudSelectedPlacement != null) {
            int index = HudClientEvents.hudSelectedPlacement.scenarioRoleIndex;
            role = ScenarioUtils.getScenarioRole(true, index);
        }
        return role;
    }

    static {
        updateButtons();
    }
}
