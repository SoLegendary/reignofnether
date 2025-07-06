package com.solegendary.reignofnether.sandbox;

import com.solegendary.reignofnether.ReignOfNether;
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
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

// action buttons but only for neutral units in sandbox

public class SandboxActionButtons {

    private static final Minecraft MC = Minecraft.getInstance();

    public static Button setAnchor;
    public static Button resetToAnchor;
    public static Button removeAnchor;

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

    private static Relationship getRelationshipToHudSelectedUnit() {
        if (MC.player != null && HudClientEvents.hudSelectedEntity != null) {
            return UnitClientEvents.getPlayerToEntityRelationship(HudClientEvents.hudSelectedEntity);
        }
        return Relationship.NEUTRAL;
    }

    public static void updateButtons() {
        setAnchor = new Button(
                "Set Anchor",
                Button.itemIconSize,
                new ResourceLocation("minecraft", "textures/block/respawn_anchor_side4.png"),
                Keybindings.keyQ,
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
                new ResourceLocation("minecraft", "textures/block/respawn_anchor_top_off.png"),
                Keybindings.keyW,
                () -> CursorClientEvents.getLeftClickSandboxAction() == SandboxAction.RESET_TO_ANCHOR,
                () -> !SandboxClientEvents.isSandboxPlayer(),
                () -> selectedUnitsHaveAnchor(),
                () -> {
                    if (!UnitClientEvents.getSelectedUnits().isEmpty()) {
                        LivingEntity entity = UnitClientEvents.getSelectedUnits().get(0);
                        if (entity instanceof Unit unit) {
                            SandboxServerboundPacket.resetToAnchor(entity.getId());
                            Unit.fullResetBehaviours(unit);
                        }
                    }
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
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
                Keybindings.keyE,
                () -> CursorClientEvents.getLeftClickSandboxAction() == SandboxAction.REMOVE_ANCHOR,
                () -> !SandboxClientEvents.isSandboxPlayer(),
                () -> selectedUnitsHaveAnchor(),
                () -> {
                    if (!UnitClientEvents.getSelectedUnits().isEmpty())
                        SandboxServerboundPacket.removeAnchor(UnitClientEvents.getSelectedUnits().get(0).getId());
                },
                null,
                List.of(
                        fcs(I18n.get("hud.actionbuttons.reignofnether.remove_anchor"), true)
                )
        );
    }

    public static Button getSetRelationshipButton() {
        return new Button(
                "Toggle Relationship",
                Button.itemIconSize,
                switch (getRelationshipToHudSelectedUnit()) {
                    case OWNED -> new ResourceLocation("minecraft", "textures/block/lime_wool.png");
                    case FRIENDLY -> new ResourceLocation("minecraft", "textures/block/blue_wool.png");
                    case NEUTRAL -> new ResourceLocation("minecraft", "textures/block/yellow_wool.png");
                    case HOSTILE -> new ResourceLocation("minecraft", "textures/block/red_wool.png");
                },
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    if (MC.player != null) {
                        for (LivingEntity entity : UnitClientEvents.getSelectedUnits()) {
                            if (entity instanceof Unit unit) {
                                switch (UnitClientEvents.getPlayerToEntityRelationship(entity)) {
                                    default -> unit.setOwnerName("");
                                    case NEUTRAL -> unit.setOwnerName("Enemy");
                                    case HOSTILE -> unit.setOwnerName(MC.player.getName().getString());
                                }
                                SandboxServerboundPacket.setOwner(entity.getId(), unit.getOwnerName());
                                updateButtons();
                            }
                        }
                    }
                },
                () -> {
                    if (MC.player != null) {
                        for (LivingEntity entity : UnitClientEvents.getSelectedUnits()) {
                            if (entity instanceof Unit unit) {
                                switch (getRelationshipToHudSelectedUnit()) {
                                    default -> unit.setOwnerName("Enemy");
                                    case NEUTRAL -> unit.setOwnerName(MC.player.getName().getString());
                                    case HOSTILE -> unit.setOwnerName("");
                                }
                                SandboxServerboundPacket.setOwner(entity.getId(), unit.getOwnerName());
                                updateButtons();
                            }
                        }
                    }
                },
                List.of(
                        fcs(I18n.get("sandbox.reignofnether.relationship_button1", SandboxClientEvents.getRelationshipName(getRelationshipToHudSelectedUnit()))),
                        fcs(I18n.get("sandbox.reignofnether.relationship_button2"))
                )
        );
    }

    static {
        updateButtons();
    }
}
