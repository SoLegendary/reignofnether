package com.solegendary.reignofnether.sandbox;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
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
            if (entity instanceof Unit unit && unit.getAnchor() != null && !unit.getAnchor().equals(new BlockPos(0,0,0)))
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
                        SandboxServerboundPacket.resetToAnchor(UnitClientEvents.getSelectedUnits().stream().mapToInt(Entity::getId).toArray());
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
                () -> SandboxServerboundPacket.removeAnchor(UnitClientEvents.getSelectedUnits().stream().mapToInt(Entity::getId).toArray()),
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
                    case NEUTRAL -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/yellow_wool.png");
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
                                    default -> unit.setOwnerName("");
                                    case NEUTRAL -> unit.setOwnerName("Enemy");
                                    case HOSTILE -> unit.setOwnerName(MC.player.getName().getString());
                                }
                            }
                        }
                        if (HudClientEvents.hudSelectedEntity instanceof Unit unit) {
                            SandboxServerboundPacket.setUnitOwner(UnitClientEvents.getSelectedUnits().stream().mapToInt(Entity::getId).toArray(), unit.getOwnerName());
                            updateButtons();
                        }
                        for (BuildingPlacement bpl : BuildingClientEvents.getSelectedBuildings()) {
                            switch (finalRelationship) {
                                default -> bpl.ownerName = "";
                                case NEUTRAL -> bpl.ownerName = "Enemy";
                                case HOSTILE -> bpl.ownerName = MC.player.getName().getString();
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
                                    default -> unit.setOwnerName("Enemy");
                                    case NEUTRAL -> unit.setOwnerName(MC.player.getName().getString());
                                    case HOSTILE -> unit.setOwnerName("");
                                }
                            }
                        }
                        if (HudClientEvents.hudSelectedEntity instanceof Unit unit) {
                            SandboxServerboundPacket.setUnitOwner(UnitClientEvents.getSelectedUnits().stream().mapToInt(Entity::getId).toArray(), unit.getOwnerName());
                            updateButtons();
                        }
                        for (BuildingPlacement bpl : BuildingClientEvents.getSelectedBuildings()) {
                            switch (finalRelationship) {
                                default -> bpl.ownerName = "Enemy";
                                case NEUTRAL -> bpl.ownerName = MC.player.getName().getString();
                                case HOSTILE -> bpl.ownerName = "";
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

    static {
        updateButtons();
    }
}
