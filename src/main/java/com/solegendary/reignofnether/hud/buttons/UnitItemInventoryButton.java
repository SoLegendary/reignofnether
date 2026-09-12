package com.solegendary.reignofnether.hud.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.solegendary.reignofnether.items.ItemClientEvents;
import com.solegendary.reignofnether.items.ItemServerboundPacket;
import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

// A unit's inventory slot: draggable, and either uses the item directly on left-click
// or arms a targeted use action (entity/building/ground).
public class UnitItemInventoryButton extends AbstractUnitItemButton {

    private static final float GHOST_ALPHA = 0.45f;

    public UnitItemInventoryButton(int invIndex, UnitItem unitItem, ItemStack itemStack, Unit unit, Keybinding hotkey) {
        super(
                "button_" + itemStack.getItem().getDescriptionId(),
                Button.DEFAULT_ICON_SIZE,
                null,
                null,
                () -> ItemClientEvents.actionableUnitItemDrag == unitItem &&
                        ItemClientEvents.actionableInvIndex == invIndex &&
                        ItemClientEvents.actionableInvUUID.equals(ItemUtil.getUUID(itemStack)),
                () -> false,
                () -> true,
                () -> {
                    ItemClientEvents.actionableUnitItem = unitItem;
                    ItemClientEvents.actionableUnitItemDrag = unitItem;
                    ItemClientEvents.actionableInvIndex = invIndex;
                    ItemClientEvents.actionableInvUUID = ItemUtil.getUUID(itemStack);
                },
                null,
                List.of(),
                unitItem,
                itemStack
        );
        this.iconItem = itemStack;
        this.invIndex = invIndex;

        this.onLeftClickRelease = () -> { // actual item use actions
            if (!ItemClientEvents.hasDragActionItem()) {
                if (unitItem.onUse != null) {
                    ItemServerboundPacket.use(((Entity) unit).getId(), invUUID);
                } else if (unitItem.onUseEntity != null ||
                        unitItem.onUseBuilding != null ||
                        unitItem.onUseGround != null) {
                    ItemClientEvents.actionableUnitItem = unitItem;
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

    // TODO: remove alpha, hide original button and render to move the actual button away when dragging
    // render a translucent version of this button
    public void renderGhost(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = mouseX - (DEFAULT_ICON_SIZE / 2);
        int y = mouseY - (DEFAULT_ICON_SIZE / 2);
        int xyDiff = (DEFAULT_ICON_SIZE - iconSize) / 2;
        float alpha = Mth.clamp(GHOST_ALPHA, 0.0f, 1.0f);

        guiGraphics.pose().pushPose(); // contain the z-translates below

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        // frame + bg: shader colour fades the texture, but the bg is a solid
        // fill inside renderIconFrameWithBg, so fade its alpha channel manually
        if (this.frameResource != null) {
            int a = Math.round(((bgColour >>> 24) & 0xFF) * alpha);
            int bgCol = (a << 24) | (bgColour & 0x00FFFFFF);
            guiGraphics.pose().translate(0, 0, 1);
            MyRenderer.renderIconFrameWithBg(guiGraphics, this.frameResource,
                    x + xyDiff, y + xyDiff, iconFrameSize, bgCol);
        }
        if (bgIconResource != null) {
            guiGraphics.pose().translate(0, 0, 1);
            MyRenderer.renderIcon(guiGraphics, bgIconResource,
                    ghostIconX(x, xyDiff), ghostIconY(y, xyDiff),
                    stretchIconToBorders ? imageSize + 2 : imageSize);
        }
        if (iconResource != null) {
            guiGraphics.pose().translate(0, 0, 1);
            MyRenderer.renderIcon(guiGraphics, iconResource,
                    ghostIconX(x, xyDiff), ghostIconY(y, xyDiff),
                    stretchIconToBorders ? imageSize + 2 : imageSize);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // item models ignore shader colour, so draw normally then wash out on top
        if (iconItem != null) {
            int offset = 2 + Math.round((1.0f - iconItemScale) * 8f);
            int itemX = x + offset + (7 - xyDiff - iconSize / 2);
            int itemY = y + offset + (7 - xyDiff - iconSize / 2);
            int itemPx = Math.round(16 * iconItemScale);

            guiGraphics.pose().translate(0, 0, 1);
            MyRenderer.renderItem(guiGraphics, iconItem, itemX, itemY, iconItemScale);

            guiGraphics.pose().translate(0, 0, 1);
            int washAlpha = Math.round((1.0f - alpha) * 0xC0);
            guiGraphics.fill(RenderType.guiGhostRecipeOverlay(),
                    itemX, itemY, itemX + itemPx, itemY + itemPx,
                    (washAlpha << 24) | 0xFFFFFF);
        }
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }

    private int ghostIconX(int x, int xyDiff) {
        int iconX = x + 4 + (7 - xyDiff - iconSize / 2);
        if (stretchIconToBorders) iconX -= 1;
        return iconX + (DEFAULT_ICON_SIZE - imageSize) / 2;
    }

    private int ghostIconY(int y, int xyDiff) {
        int iconY = y + 4 + (7 - xyDiff - iconSize / 2);
        if (stretchIconToBorders) iconY -= 1;
        return iconY + (DEFAULT_ICON_SIZE - imageSize) / 2;
    }
}