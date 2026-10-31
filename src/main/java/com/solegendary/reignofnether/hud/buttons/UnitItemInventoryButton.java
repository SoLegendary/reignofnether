package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.player.PlayerRolesClient;
import com.solegendary.reignofnether.alliance.SharingClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.solegendary.reignofnether.items.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.hud.panels.ButtonCell;
import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

// A unit's inventory slot: draggable, and either uses the item directly on left-click
// or arms a targeted use action (entity/building/ground).
public class UnitItemInventoryButton extends AbstractUnitItemButton {

    private static final float GHOST_ALPHA = 0.5f;

    private Unit unit;

    public UnitItemInventoryButton(int invIndex, UnitItem unitItem, ItemStack itemStack, Unit unit, Keybinding hotkey) {
        super(
                "button_" + itemStack.getItem().getDescriptionId(),
                Button.DEFAULT_ICON_SIZE,
                null,
                null,
                () -> (ItemClientEvents.actionableUnitItemDrag == unitItem &&
                        ItemClientEvents.actionableInvIndex == invIndex &&
                        ItemClientEvents.actionableInvUUID != null &&
                        ItemClientEvents.actionableInvUUID.equals(ItemUtil.getUUID(itemStack))) ||
                        ItemUtil.isActive(itemStack),
                () -> false,
                () -> {
                    Player player = Minecraft.getInstance().player;
                    boolean ownsUnit = player != null && PlayerRolesClient.myOwner().equals(unit.getOwnerName());
                    return ownsUnit || SharingClient.canControl(unit) || SandboxClientEvents.isSandboxPlayer();
                },
                () -> { // onLeftClick
                    ItemClientEvents.actionableUnitItem = unitItem;
                    ItemClientEvents.actionableUnitItem.updateHighlightBps(((LivingEntity) unit).level());
                    ItemClientEvents.actionableInvUUID = ItemUtil.getUUID(itemStack);
                    ItemClientEvents.actionableInvIndex = invIndex;
                    ItemClientEvents.actionableUnitItemDrag = unitItem;
                },
                null,
                List.of(),
                unitItem,
                itemStack
        );
        this.iconItem = itemStack;
        this.invIndex = invIndex;
        this.unit = unit;

        this.onLeftClickRelease = () -> { // actual item use actions
            if (!ItemClientEvents.hasDragActionItem() && this.unit instanceof UnitInventory) {

                if (unitItem.onUse != null) {
                    ItemServerboundPacket.use(((Entity) unit).getId(), invUUID);
                }
                else if (unitItem.onUseEntity != null ||
                        unitItem.onUseBuilding != null ||
                        unitItem.onUseGround != null) {
                    ItemClientEvents.actionableUnitItem = unitItem;
                    ItemClientEvents.actionableUnitItem.updateHighlightBps(((LivingEntity) unit).level());
                    ItemClientEvents.actionableInvIndex = invIndex;
                    ItemClientEvents.actionableInvUUID = ItemUtil.getUUID(itemStack);
                    ItemClientEvents.leftClickUseItem = true;
                    ItemClientEvents.actionableUnitItemDrag = null;
                }
            }
        };
        if (hasUseAction())
            this.hotkey = hotkey;
    }

    @Override
    public void refresh() {
        if (MC.level != null && unitItem.cooldownTicksMax > 0) {
            Long cooldownTicksLeft = ItemUtil.getCooldownTicksLeft(itemStack, ((Entity) unit).level());
            this.greyPercent = 1 - Math.min(1f, (float) cooldownTicksLeft / unitItem.cooldownTicksMax);
        } else {
            this.greyPercent = 0;
        }
        this.bottomLeftText = () -> {
            if (this.itemStack.getCount() > 1)
                return String.valueOf(this.itemStack.getCount());
            return "";
        };
    }

    // render a translucent version of this button, the size of an inventory slot, under the cursor
    public void renderGhost(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        var scale = com.solegendary.reignofnether.hud.layout.HudGeometry.scale();
        float k = scale.poseScale();
        int size = 26;
        int x = scale.toUnits(mouseX) - size / 2;
        int y = scale.toUnits(mouseY) - size / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);
        guiGraphics.pose().scale(k, k, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // fills are batched, so flush while the faded shader colour is still set
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, GHOST_ALPHA);
        Theme.cell(guiGraphics, x, y, size, size, Theme.CellState.READY, false);
        int icon = Math.round(size * 0.56f);
        ButtonCell.drawIcon(guiGraphics, this, x + (size - icon) / 2, y + (size - icon) / 2, icon);
        guiGraphics.flush();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        Theme.ring(guiGraphics, x, y, size, size, 1, Theme.ORANGE);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }
}