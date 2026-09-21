package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.util.MiscUtil;
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
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.*;

// Shared base for UnitItemInventoryButton and UnitItemShopButton.
public abstract class AbstractUnitItemButton extends Button {

    protected static final float SMALL_SCALE = 0.75f; // type label, description, dot points
    protected static final int MAX_TEXT_WIDTH = 170; // on-screen wrap width, post-scale
    protected static final int LINE_HEIGHT = 10; // full-size line
    protected static final int SMALL_LINE_HEIGHT = 8; // scaled line
    protected static final int DIVIDER_HEIGHT = 5; // 2px pad + 1px rule + 2px pad
    protected static final int MIN_COLUMN_GAP = 8; // between left and right halves of a row

    public static final Style NAME_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
    public static final Style QTY_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xB4B2A9));
    public static final Style TYPE_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xFAC775));
    public static final Style DESC_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xD3D1C7));
    public static final Style POINTS_STYLE = Style.EMPTY
            .withFont(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "resource_icons"))
            .withColor(TextColor.fromRgb(0x97C459));
    public static final Style SELL_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x5DCAA5));
    public static final Style MANA_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x6EA8D9));
    public static final Style COOLDOWN_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xD9C46E));
    public static final Style RANGE_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xC97A4A));

    protected static final String EMERALD_ICON = "\uE010";

    // footer stat icons (plain textures, not font glyphs)
    protected static final ResourceLocation MANA_ICON_RL =
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/lapis.png");
    protected static final ResourceLocation COOLDOWN_ICON_RL =
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/clock.png");
    protected static final ResourceLocation RANGE_ICON_RL =
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/bow.png");
    protected static final int STAT_ICON_SIZE = 8; // on-screen size of footer stat icons
    protected static final int STAT_ICON_GAP = 2; // between an icon and its number
    protected static final int STAT_GAP = 5; // between mana stat and cooldown stat

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

        List<String> points = new ArrayList<>(unitItem.getPointDescriptions());
        points.addAll(getEnchantmentDescs(itemStack));
        points.addAll(getAttributeDescs(unitItem));
        for (String point : points)
            bodyLines.addAll(font.split(
                    MyRenderer.styledWithIcons(point.replace(" ", "   "), POINTS_STYLE), smallWrapWidth));

        boolean hasDescGap = descLineCount > 0 && bodyLines.size() > descLineCount;

        // ---- band 3: mana cost + cooldown (left) | sell value (right) ----
        FormattedCharSequence emeraldSeq = null;
        if (this.emeraldValue > 0)
            emeraldSeq = Component.literal(EMERALD_ICON).withStyle(MyRenderer.iconStyle)
                    .append(Component.literal(" " + this.emeraldValue)
                            .withStyle(SELL_STYLE.withFont(Style.DEFAULT_FONT)))
                    .getVisualOrderText();

        boolean hasMana = unitItem.manaCost > 0;
        boolean hasCooldown = unitItem.cooldownTicksMax > 0;
        boolean hasRange = unitItem.range > 0;
        String manaText = hasMana ? String.valueOf(unitItem.manaCost) : null;
        String cooldownText = hasCooldown ? (unitItem.cooldownTicksMax / 20) + "s" : null;
        String rangeText = hasRange ? String.valueOf((int) unitItem.range) : null;

        int footerRightWidth = emeraldSeq != null ? MyRenderer.scaledWidth(font, emeraldSeq, SMALL_SCALE) : 0;
        int footerLeftWidth = 0;
        if (hasMana)
            footerLeftWidth += statWidth(font, manaText, SMALL_SCALE);
        if (hasCooldown) {
            if (hasMana)
                footerLeftWidth += STAT_GAP;
            footerLeftWidth += statWidth(font, cooldownText, SMALL_SCALE);
        }
        if (hasRange) {
            if (hasMana || hasCooldown)
                footerLeftWidth += STAT_GAP;
            footerLeftWidth += statWidth(font, rangeText, SMALL_SCALE);
        }

        boolean hasBody = !bodyLines.isEmpty();
        boolean hasFooter = footerLeftWidth > 0 || footerRightWidth > 0;

        int footerWidth = footerLeftWidth + footerRightWidth;
        if (footerLeftWidth > 0 && footerRightWidth > 0)
            footerWidth += MIN_COLUMN_GAP;

        // ---- measure (in on-screen px, so scaled lines count as scaled) ----
        int width = rowWidth(font, nameSeq, 1.0f, typeSeq, SMALL_SCALE);
        for (FormattedCharSequence line : bodyLines)
            width = Math.max(width, MyRenderer.scaledWidth(font, line, SMALL_SCALE));
        if (hasFooter)
            width = Math.max(width, footerWidth);

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

            if (footerLeftWidth > 0) {
                int statX = x;
                if (hasMana)
                    statX += drawStat(guiGraphics, font, COOLDOWN_ICON_RL, cooldownText, COOLDOWN_STYLE, statX, lineY, SMALL_SCALE);
                if (hasCooldown) {
                    if (hasMana)
                        statX += STAT_GAP - 1;
                    statX += drawStat(guiGraphics, font, MANA_ICON_RL, manaText, MANA_STYLE, statX, lineY, SMALL_SCALE);
                }
                if (hasRange) {
                    if (hasMana || hasCooldown)
                        statX += STAT_GAP;
                    drawStat(guiGraphics, font, RANGE_ICON_RL, rangeText, RANGE_STYLE, statX, lineY, SMALL_SCALE);
                }
            }

            if (emeraldSeq != null) {
                int emeraldX = x + width - footerRightWidth;
                MyRenderer.drawScaledString(guiGraphics, font, emeraldSeq, emeraldX, lineY, 0xFFFFFF, SMALL_SCALE);
            }
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

    // "+5 Attack Damage", "+10% Movement Speed", ... from an item's flat attribute modifiers
    public static List<String> getAttributeDescs(UnitItem unitItem) {
        List<String> descs = new ArrayList<>();
        for (Map.Entry<Attribute, AttributeModifier> entry : unitItem.attributes.entrySet()) {
            Attribute attr = entry.getKey();
            AttributeModifier modifier = entry.getValue();
            String attrStr = MiscUtil.getAttrString(attr, modifier);
            descs.add(attrStr);
        }
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

    // on-screen width of an icon + number footer stat (icon, gap, then text at the given scale)
    protected static int statWidth(Font font, String text, float scale) {
        return STAT_ICON_SIZE + STAT_ICON_GAP + MyRenderer.scaledWidth(font, fcs(text), scale);
    }

    // draws an icon + number footer stat at (x, y), vertically centred on the icon; returns its on-screen width
    protected static int drawStat(GuiGraphics guiGraphics, Font font, ResourceLocation icon,
                                  String text, Style style, int x, int y, float scale) {
        if (text == null) return 0;
        guiGraphics.blit(icon, x, y - 1, 0, 0, STAT_ICON_SIZE, STAT_ICON_SIZE, STAT_ICON_SIZE, STAT_ICON_SIZE);
        int textX = x + STAT_ICON_SIZE + STAT_ICON_GAP;
        FormattedCharSequence seq = Component.literal(text).withStyle(style).getVisualOrderText();
        MyRenderer.drawScaledString(guiGraphics, font, seq, textX, y, 0xFFFFFF, scale);
        return STAT_ICON_SIZE + STAT_ICON_GAP + MyRenderer.scaledWidth(font, seq, scale);
    }
}