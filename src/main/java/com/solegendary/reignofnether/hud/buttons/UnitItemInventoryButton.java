package com.solegendary.reignofnether.hud.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.items.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.mixin.UnitInventoryMobMixin;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import com.solegendary.reignofnether.hud.buttons.UnitItemInventoryButton;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.ItemServerboundPacket;
import com.solegendary.reignofnether.items.ItemClientEvents;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.AbstractUnitItemButton;

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
                    boolean ownsUnit = player != null && player.getName().getString().equals(unit.getOwnerName());
                    return ownsUnit || AlliancesClient.canControlAlly(unit) || SandboxClientEvents.isSandboxPlayer();
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
            if (!ItemClientEvents.hasDragActionItem() && this.unit instanceof UnitInventory inv &&
                    inv.checkManaCostAndCooldown(unitItem, itemStack)) {

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
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
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
        super.render(guiGraphics, x, y, mouseX, mouseY);
    }

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
                    imageSize + (innerIconSizeModifier * 2));
        }
        if (iconResource != null) {
            guiGraphics.pose().translate(0, 0, 1);
            MyRenderer.renderIcon(guiGraphics, iconResource,
                    ghostIconX(x, xyDiff), ghostIconY(y, xyDiff),
                    imageSize + (innerIconSizeModifier * 2));
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
        iconX -= innerIconSizeModifier;
        return iconX + (DEFAULT_ICON_SIZE - imageSize) / 2;
    }

    private int ghostIconY(int y, int xyDiff) {
        int iconY = y + 4 + (7 - xyDiff - iconSize / 2);
        iconY -= innerIconSizeModifier;
        return iconY + (DEFAULT_ICON_SIZE - imageSize) / 2;
    }
}