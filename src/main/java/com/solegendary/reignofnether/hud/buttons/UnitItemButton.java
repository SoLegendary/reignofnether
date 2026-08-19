package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class UnitItemButton extends Button {

    private static final float SMALL_SCALE = 0.75f; // type label, description, dot points
    private static final int MAX_TEXT_WIDTH = 170; // on-screen wrap width, post-scale
    private static final int LINE_HEIGHT = 10; // full-size line
    private static final int SMALL_LINE_HEIGHT = 8; // scaled line
    private static final int DIVIDER_HEIGHT = 5; // 2px pad + 1px rule + 2px pad
    private static final int MIN_COLUMN_GAP = 8; // between left and right halves of a row

    private static final Style NAME_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
    private static final Style QTY_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xB4B2A9));
    private static final Style TYPE_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xFAC775));
    private static final Style DESC_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xD3D1C7));
    private static final Style POINTS_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x97C459));
    private static final Style SELL_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x5DCAA5));
    private static final Style HOTKEY_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xB4B2A9));

    private static final String EMERALD_ICON = "\uE010";

    private UnitItem unitItem;

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
        this.unitItem = unitItem;
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

    @Override
    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (MC.screen == null)
            return;

        Font font = MC.font;
        // font.split works in unscaled units, so widen the wrap point to compensate
        int smallWrapWidth = Math.round(MAX_TEXT_WIDTH / SMALL_SCALE);

        // ---- band 1: name (+qty) | type ----
        MutableComponent nameComp = unitItem.getName().copy().withStyle(NAME_STYLE);
        if (unitItem.stackQty > 1)
            nameComp.append(Component.literal(" (" + unitItem.stackQty + ")").withStyle(QTY_STYLE));
        FormattedCharSequence nameSeq = nameComp.getVisualOrderText();
        FormattedCharSequence typeSeq = Component.literal(unitItem.type.getLabel())
                .withStyle(TYPE_STYLE).getVisualOrderText();

        // ---- band 2: description + dot points (all small) ----
        List<FormattedCharSequence> bodyLines = new ArrayList<>();
        String desc = unitItem.getDescription();
        if (desc != null)
            bodyLines.addAll(font.split(Component.literal(desc).withStyle(DESC_STYLE), smallWrapWidth));
        for (String point : unitItem.getPointLines())
            bodyLines.addAll(font.split(
                    Component.literal("\u2022 " + point).withStyle(POINTS_STYLE), smallWrapWidth));

        // ---- band 3: sell value | hotkey ----
        FormattedCharSequence sellSeq = null;
        if (unitItem.sellValue > 0)
            sellSeq = Component.literal(EMERALD_ICON).withStyle(MyRenderer.iconStyle)
                    .append(Component.literal(" " + unitItem.sellValue)
                            .withStyle(SELL_STYLE.withFont(Style.DEFAULT_FONT)))
                    .getVisualOrderText();

        FormattedCharSequence hotkeySeq = null;
        if (hotkey != null)
            hotkeySeq = Component.literal(hotkey.getCurrentLabel())
                    .withStyle(HOTKEY_STYLE).getVisualOrderText();

        boolean hasBody = !bodyLines.isEmpty();
        boolean hasFooter = sellSeq != null || hotkeySeq != null;

        // ---- measure (in on-screen px, so scaled lines count as scaled) ----
        int width = rowWidth(font, nameSeq, 1.0f, typeSeq, SMALL_SCALE);
        for (FormattedCharSequence line : bodyLines)
            width = Math.max(width, MyRenderer.scaledWidth(font, line, SMALL_SCALE));
        if (hasFooter)
            width = Math.max(width, rowWidth(font, sellSeq, SMALL_SCALE, hotkeySeq, 1.0f));

        int height = LINE_HEIGHT;
        if (hasBody)
            height += DIVIDER_HEIGHT + (bodyLines.size() * SMALL_LINE_HEIGHT);
        if (hasFooter)
            height += DIVIDER_HEIGHT + LINE_HEIGHT;
        height -= 2; // trailing line spacing isn't visible ink

        // ---- position: prefer above-right of the cursor, flip near screen edges ----
        int x = mouseX + 12;
        int y = mouseY + tooltipOffsetY - height - 12;
        if (x + width + 4 > MC.screen.width)
            x = Math.max(4, mouseX - width - 12);
        if (y < 4)
            y = mouseY + tooltipOffsetY + 16;
        if (y + height + 4 > MC.screen.height)
            y = Math.max(4, MC.screen.height - height - 4);

        // ---- draw ----
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 3000);

        MyRenderer.renderTooltipBackground(guiGraphics, x, y, width, height);

        int lineY = y;
        MyRenderer.renderJustifiedRow(guiGraphics, nameSeq, 1.0f, typeSeq, SMALL_SCALE, x, lineY, width);
        lineY += LINE_HEIGHT;

        if (hasBody) {
            MyRenderer.renderTooltipDivider(guiGraphics, x, lineY, width);
            lineY += DIVIDER_HEIGHT;
            for (FormattedCharSequence line : bodyLines) {
                MyRenderer.drawScaledString(guiGraphics, font, line, x, lineY, 0xFFFFFF, SMALL_SCALE);
                lineY += SMALL_LINE_HEIGHT;
            }
        }

        if (hasFooter) {
            MyRenderer.renderTooltipDivider(guiGraphics, x, lineY, width);
            lineY += DIVIDER_HEIGHT;
            MyRenderer.renderJustifiedRow(guiGraphics, sellSeq, SMALL_SCALE, hotkeySeq, 1.0f, x, lineY, width);
        }

        guiGraphics.pose().popPose();
    }

    // on-screen width needed to fit both halves of a justified row without them touching
    private static int rowWidth(Font font, FormattedCharSequence left, float leftScale,
                                FormattedCharSequence right, float rightScale) {
        int w = 0;
        if (left != null)
            w += MyRenderer.scaledWidth(font, left, leftScale);
        if (right != null)
            w += MyRenderer.scaledWidth(font, right, rightScale);
        if (left != null && right != null)
            w += MIN_COLUMN_GAP;
        return w;
    }
}
