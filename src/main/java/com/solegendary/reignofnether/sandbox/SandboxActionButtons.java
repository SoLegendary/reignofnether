package com.solegendary.reignofnether.sandbox;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.Button;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

// action buttons but only for neutral units in sandbox

public class SandboxActionButtons {

    private static final Minecraft MC = Minecraft.getInstance();

    public static Button setAnchor;
    public static Button resetToAnchor;
    public static Button removeAnchor;
    public static Button removeBuildingPlacement;

    private static boolean neutralUnitsSelected() {
        for (LivingEntity entity : UnitClientEvents.getSelectedUnits())
            if (entity instanceof Unit unit && !unit.getOwnerName().isBlank())
                return false;
        return true;
    }

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

    public static Button getSetRelationshipButton() {
        Relationship relationship = Relationship.OWNED;
        if (HudClientEvents.hudSelectedEntity != null)
            relationship = UnitClientEvents.getPlayerToEntityRelationship(HudClientEvents.hudSelectedEntity);
        else if (HudClientEvents.hudSelectedPlacement != null)
            relationship = BuildingClientEvents.getPlayerToBuildingRelationship(HudClientEvents.hudSelectedPlacement);
        final Relationship finalRelationship = relationship;

        return new Button(
                "Toggle Relationship",
                Button.itemIconSize,
                switch (finalRelationship) {
                    case OWNED -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/lime_wool.png");
                    case FRIENDLY -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/blue_wool.png");
                    case NEUTRAL ->
                            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/yellow_wool.png");
                    case HOSTILE -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/red_wool.png");
                },
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    if (MC.player != null) {
                        for (LivingEntity entity : UnitClientEvents.getSelectedUnits()) {
                            if (entity instanceof Unit unit) {
                                switch (finalRelationship) {
                                    case NEUTRAL -> unit.setOwnerName("Enemy");
                                    case HOSTILE -> unit.setOwnerName(MC.player.getName().getString());
                                    default -> unit.setOwnerName("");
                                }
                            }
                        }
                        if (HudClientEvents.hudSelectedEntity instanceof Unit unit) {
                            SandboxServerboundPacket.setUnitOwner(ArrayUtil.livingEntityListToIdArray(UnitClientEvents.getSelectedUnits()), unit.getOwnerName());
                            updateButtons();
                        }
                        for (BuildingPlacement bpl : BuildingClientEvents.getSelectedBuildings()) {
                            switch (finalRelationship) {
                                case NEUTRAL -> bpl.ownerName = "Enemy";
                                case HOSTILE -> bpl.ownerName = MC.player.getName().getString();
                                default -> bpl.ownerName = "";
                            }
                            SandboxServerboundPacket.setBuildingOwner(bpl.originPos, bpl.ownerName);
                            updateButtons();
                        }
                    }
                },
                () -> {
                    if (MC.player != null) {
                        for (LivingEntity entity : UnitClientEvents.getSelectedUnits()) {
                            if (entity instanceof Unit unit) {
                                switch (finalRelationship) {
                                    case NEUTRAL -> unit.setOwnerName(MC.player.getName().getString());
                                    case HOSTILE -> unit.setOwnerName("");
                                    default -> unit.setOwnerName("Enemy");
                                }
                            }
                        }
                        if (HudClientEvents.hudSelectedEntity instanceof Unit unit) {
                            SandboxServerboundPacket.setUnitOwner(ArrayUtil.livingEntityListToIdArray(UnitClientEvents.getSelectedUnits()), unit.getOwnerName());
                            updateButtons();
                        }
                        for (BuildingPlacement bpl : BuildingClientEvents.getSelectedBuildings()) {
                            switch (finalRelationship) {
                                case NEUTRAL -> bpl.ownerName = MC.player.getName().getString();
                                case HOSTILE -> bpl.ownerName = "";
                                default -> bpl.ownerName = "Enemy";
                            }
                            SandboxServerboundPacket.setBuildingOwner(bpl.originPos, bpl.ownerName);
                            updateButtons();
                        }
                    }
                },
                List.of(
                        fcs(I18n.get("hud.relationship.reignofnether.owned"), relationship == Relationship.OWNED),
                        fcs(I18n.get("hud.relationship.reignofnether.neutral"), relationship == Relationship.NEUTRAL),
                        fcs(I18n.get("hud.relationship.reignofnether.enemy"), relationship == Relationship.HOSTILE)
                )
        );
    }

    public static Button getCycleScenarioRoleButton() {
        return new Button(
                "Switch Building Scenario Role",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_conditional.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> cycleUnitOrBuildingRoleIndex(false),
                () -> cycleUnitOrBuildingRoleIndex(true),
                List.of(
                        fcs(I18n.get("sandbox.reignofnether.cycle_scenario_role", getHudSelectedScenarioRoleName()))
                )
        );
    }

    private static void cycleUnitOrBuildingRoleIndex(boolean reverse) {
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

    private static String getHudSelectedScenarioRoleName() {
        ScenarioRole role = null;
        if (HudClientEvents.hudSelectedEntity instanceof Unit unit) {
            role = ScenarioUtils.getScenarioRole(true, unit.getScenarioRoleIndex());
        } else if (HudClientEvents.hudSelectedPlacement != null) {
            int index = HudClientEvents.hudSelectedPlacement.scenarioRoleIndex;
            role = ScenarioUtils.getScenarioRole(true, index);
        }
        if (role != null) {
            return role.name;
        }
        return I18n.get("sandbox.reignofnether.scenario_role_none");
    }

    static {
        updateButtons();
    }
}
