package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

// Shared base for UnitItemInventoryButton and UnitItemShopButton.
public abstract class AbstractUnitItemButton extends Button {

    protected static final float SMALL_SCALE = 0.75f; // type label, description, dot points
    protected static final int MAX_TEXT_WIDTH = 170; // on-screen wrap width, post-scale
    protected static final int LINE_HEIGHT = 10; // full-size line
    protected static final int SMALL_LINE_HEIGHT = 8; // scaled line
    protected static final int DIVIDER_HEIGHT = 5; // 2px pad + 1px rule + 2px pad
    protected static final int MIN_COLUMN_GAP = 8; // between left and right halves of a row

    protected static final Style NAME_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
    protected static final Style QTY_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xB4B2A9));
    protected static final Style TYPE_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xFAC775));
    protected static final Style DESC_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xD3D1C7));
    protected static final Style POINTS_STYLE = Style.EMPTY
            .withFont(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "resource_icons"))
            .withColor(TextColor.fromRgb(0x97C459));
    protected static final Style SELL_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x5DCAA5));

    protected static final String EMERALD_ICON = "\uE010";

    protected UnitItem unitItem;
    protected ItemStack itemStack;
    public int invIndex = 0;
    public UUID invUUID;

    protected int emeraldValue; // buyCost for shop buttons, sellValue for inventory buttons

    protected AbstractUnitItemButton(
            String name,
            int iconSize,
            ResourceLocation iconRl,
            Keybinding hotkey,
            Supplier<Boolean> isSelected,
            Supplier<Boolean> isHidden,
            Supplier<Boolean> isEnabled,
            Runnable onLeftClick,
            Runnable onRightClick,
            List<FormattedCharSequence> tooltipLines,
            UnitItem unitItem,
            ItemStack itemStack
    ) {
        super(name, iconSize, iconRl, hotkey, isSelected, isHidden, isEnabled,
                onLeftClick, onRightClick, tooltipLines);
        this.unitItem = unitItem;
        this.itemStack = itemStack;
        this.invUUID = ItemUtil.getUUID(itemStack);
        this.emeraldValue = unitItem.sellValue;
    }

    @Override
    public void checkPressed(int key) {
        if (!OrthoviewClientEvents.isEnabled() || !isEnabled.get())
            return;

        if (hotkey != null && hotkey.getKey() == key) {
            if (MC.player != null)
                MC.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.2f, 1.0f);
            this.onLeftClickRelease.run();
        }
    }

    public boolean hasUseAction() {
        return this.unitItem.onUse != null ||
                this.unitItem.onUseEntity != null ||
                this.unitItem.onUseBuilding != null ||
                this.unitItem.onUseGround != null;
    }

    // scale down and recentre
    @Override
    protected void renderHotkey(GuiGraphics guiGraphics, int x, int y) {
        if (this.hotkey != null) {
            String hotkeyStr = hotkey.getCurrentLabel();
            hotkeyStr = hotkeyStr.substring(0, Math.min(3, hotkeyStr.length()));

            int drawX = x + iconSize + 8 - (hotkeyStr.length() * 4);
            int drawY = y + iconSize;

            guiGraphics.pose().pushPose();

            guiGraphics.pose().translate(drawX, drawY, 0);
            guiGraphics.pose().scale(SMALL_SCALE, SMALL_SCALE, 1.0f);
            guiGraphics.pose().translate(-drawX, -drawY, 0);

            guiGraphics.drawCenteredString(MC.font,
                    hotkeyStr,
                    drawX,
                    drawY,
                    0xFFFFFF);

            guiGraphics.pose().popPose();
        }
    }

    @Override
    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (MC.screen == null || !unitItem.enableTooltip)
            return;

        Font font = MC.font;
        // font.split works in unscaled units, so widen the wrap point to compensate
        int smallWrapWidth = Math.round(MAX_TEXT_WIDTH / SMALL_SCALE);

        // ---- band 1: name (+qty) | type ----
        MutableComponent nameComp = unitItem.getName().copy().withStyle(NAME_STYLE);
        if (itemStack.getCount() > 1)
            nameComp.append(Component.literal(" (" + itemStack.getCount() + ")").withStyle(QTY_STYLE));
        FormattedCharSequence nameSeq = nameComp.getVisualOrderText();
        FormattedCharSequence typeSeq = Component.literal(unitItem.type.getLabel())
                .withStyle(TYPE_STYLE).getVisualOrderText();

        // ---- band 2: description + dot points (all small) ----
        List<FormattedCharSequence> bodyLines = new ArrayList<>();
        String desc = unitItem.getDescription();
        if (!desc.isBlank())
            bodyLines.addAll(font.split(Component.literal(desc).withStyle(DESC_STYLE), smallWrapWidth));

        int descLineCount = bodyLines.size(); // marks where desc ends and points begin

        List<String> points = new ArrayList<>(unitItem.getPointDescs());
        points.addAll(getEnchantmentDescs(itemStack));
        for (String point : points)
            bodyLines.addAll(font.split(
                    MyRenderer.styledWithIcons(point.replace(" ", "   "), POINTS_STYLE), smallWrapWidth));

        boolean hasDescGap = descLineCount > 0 && bodyLines.size() > descLineCount;

        // ---- band 3: sell value ----
        FormattedCharSequence emeraldSeq = null;
        if (this.emeraldValue > 0)
            emeraldSeq = Component.literal(EMERALD_ICON).withStyle(MyRenderer.iconStyle)
                    .append(Component.literal(" " + this.emeraldValue)
                            .withStyle(SELL_STYLE.withFont(Style.DEFAULT_FONT)))
                    .getVisualOrderText();

        boolean hasBody = !bodyLines.isEmpty();
        boolean hasFooter = emeraldSeq != null;

        // ---- measure (in on-screen px, so scaled lines count as scaled) ----
        int width = rowWidth(font, nameSeq, 1.0f, typeSeq, SMALL_SCALE);
        for (FormattedCharSequence line : bodyLines)
            width = Math.max(width, MyRenderer.scaledWidth(font, line, SMALL_SCALE));
        if (hasFooter)
            width = Math.max(width, rowWidth(font, emeraldSeq, SMALL_SCALE, fcs(""), SMALL_SCALE));

        int height = LINE_HEIGHT;
        if (hasBody)
            height += DIVIDER_HEIGHT + (bodyLines.size() * SMALL_LINE_HEIGHT);
        if (hasDescGap)
            height += 2;
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
            for (int i = 0; i < bodyLines.size(); i++) {
                if (hasDescGap && i == descLineCount)
                    lineY += 2; // extra gap between desc and point lines
                MyRenderer.drawScaledString(guiGraphics, font, bodyLines.get(i), x, lineY, 0xFFFFFF, SMALL_SCALE);
                lineY += SMALL_LINE_HEIGHT;
            }
        }

        if (hasFooter) {
            MyRenderer.renderTooltipDivider(guiGraphics, x, lineY, width);
            lineY += DIVIDER_HEIGHT;
            MyRenderer.renderJustifiedRow(guiGraphics, emeraldSeq, SMALL_SCALE, fcs(""), SMALL_SCALE, x, lineY, width);
        }
        guiGraphics.pose().popPose();
    }

    // "Sharpness V", "Unbreaking III", ... in NBT order
    protected static List<String> getEnchantmentDescs(ItemStack itemStack) {
        List<String> descs = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(itemStack).entrySet())
            descs.add(entry.getKey().getFullname(entry.getValue()).getString());
        return descs;
    }

    // on-screen width needed to fit both halves of a justified row without them touching
    protected static int rowWidth(Font font, FormattedCharSequence left, float leftScale,
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