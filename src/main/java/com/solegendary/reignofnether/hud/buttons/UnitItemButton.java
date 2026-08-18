package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class UnitItemButton extends Button {

    public UnitItemButton(UnitItem unitItem) {
        super(
                "button_" + unitItem.getItemStack().getItem().getDescriptionId(),
                Button.DEFAULT_ICON_SIZE,
                null,
                null,
                () -> false, // todo: if cursor action is on sell/drop/give
                () -> false,
                () -> true,
                () -> { }, // todo: left click use and drag to enable targeting for sell/drop/give
                () -> { }, // todo: right click to toggle targeting for sell/drop/give
                List.of()
        );
        this.iconItem = new ItemStack(unitItem.getItemStack().getItem());
    }

    // render a translucent version of this button
    public void renderGhost(GuiGraphics guiGraphics, int x, int y) {
        int xyDiff = (DEFAULT_ICON_SIZE - iconSize) / 2;

        if (this.frameResource != null) {
            guiGraphics.pose().translate(0,0,1);
            MyRenderer.renderIconFrameWithBg(guiGraphics, this.frameResource, x + xyDiff, y + xyDiff, iconFrameSize, bgColour);
        }

        if (bgIconResource != null) {
            guiGraphics.pose().translate(0,0,1);
            int iconX = x+4 + (7 - xyDiff - iconSize/2);
            int iconY = y+4 + (7 - xyDiff - iconSize/2);
            if (stretchIconToBorders) {
                iconX -= 1;
                iconY -= 1;
            }
            iconX += (DEFAULT_ICON_SIZE - imageSize) / 2;
            iconY += (DEFAULT_ICON_SIZE - imageSize) / 2;
            MyRenderer.renderIcon(
                    guiGraphics,
                    bgIconResource,
                    iconX,
                    iconY,
                    stretchIconToBorders ? imageSize + 2 : imageSize
            );
        }

        // item/unit icon
        if (iconResource != null) {
            int iconX = x+4 + (7 - xyDiff - iconSize/2);
            int iconY = y+4 + (7 - xyDiff - iconSize/2);
            if (stretchIconToBorders) {
                iconX -= 1;
                iconY -= 1;
            }
            iconX += (DEFAULT_ICON_SIZE - imageSize) / 2;
            iconY += (DEFAULT_ICON_SIZE - imageSize) / 2;
            guiGraphics.pose().translate(0,0,1);
            MyRenderer.renderIcon(
                    guiGraphics,
                    iconResource,
                    iconX, iconY,
                    stretchIconToBorders ? imageSize + 2 : imageSize
            );
        }
        if (iconItem != null) {
            guiGraphics.pose().translate(0,0,1);
            int offset = 2 + Math.round((1.0f - iconItemScale) * 8f);
            MyRenderer.renderItem(guiGraphics, iconItem, x+offset + (7 - xyDiff - iconSize/2), y+offset + (7 - xyDiff - iconSize/2), iconItemScale);
        }
    }
}
