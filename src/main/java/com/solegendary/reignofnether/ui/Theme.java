package com.solegendary.reignofnether.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Colours and bevelled shapes shared by the RTS HUD and the match setup screen.
 * All sizes are in HUD units (see {@link HudScale}).
 */
public final class Theme {

    private Theme() { }

    // surfaces
    public static final int INK = 0xFF0B0A0F;          // outlines, bar tracks, dividers
    public static final int PANEL = 0xFF16151B;
    public static final int RAISED = 0xFF1E1D26;       // cells and buttons
    public static final int RAISED_HOVER = 0xFF2A2933;
    public static final int SECONDARY = 0xFF26252E;
    public static final int SUNKEN = 0xFF121118;       // footers, input rows
    public static final int EMPTY = 0xFF0F0E13;
    public static final int LOCKED = 0xFF17161C;
    public static final int POOR = 0xFF251619;
    public static final int ACTIVE = 0xFF2F2418;
    public static final int CANCEL_HOVER = 0xFF3A1A14;
    public static final int TOGGLE_ON = 0xFF1D3A1D;
    public static final int DANGER_BG = 0xFF2A1A1A;
    public static final int CONSOLE_RIM = 0xFF2C2A33;

    // text
    public static final int TEXT = 0xFFE7E9F5;
    public static final int BODY = 0xFFCFD3E8;
    public static final int DESC = 0xFFB7BCD6;
    public static final int MUTED = 0xFFA9AECF;
    public static final int LABEL = 0xFF9498B5;
    public static final int DIM = 0xFF6C709A;
    public static final int FAINT = 0xFF3C3E5A;

    // accents
    public static final int ORANGE = 0xFFF19A0F;
    public static final int GOLD = 0xFFFEF481;
    public static final int ORANGE_DARK = 0xFFDD6003;
    public static final int ON_ORANGE = 0xFF380000;
    public static final int YELLOW = 0xFFFBEB3C;
    public static final int HINT = 0xFFE0C48A;
    public static final int HINT_BG = 0xF52F2418;

    // status
    public static final int OK = 0xFF55FF55;
    public static final int OK_DARK = 0xFF1D5A1D;
    public static final int OK_TEXT = 0xFFB9FFB9;
    public static final int ERROR = 0xFFFF6A4D;
    public static final int DANGER_TEXT = 0xFFFF8A6D;
    public static final int BUFF = 0xFF55FFFF;
    public static final int MANA = 0xFF4D6BFF;
    public static final int MANA_TEXT = 0xFF8FB8FF;
    public static final int XP = 0xFF8FE03C;
    public static final int EMERALD = 0xFF5DCAA5;

    // translucent overlays
    private static final int HIGHLIGHT = 0x12FFFFFF;
    private static final int HIGHLIGHT_SOFT = 0x0DFFFFFF;
    private static final int SHADE = 0x8C000000;
    private static final int SHADE_SOFT = 0x66000000;
    private static final int SHADE_EMPTY = 0x73000000;
    private static final int POOR_GLOW = 0x38FF6A4D;
    public static final int ALARM_TINT = 0x1AFF6A4D;   // a resource or housing cell that needs attention
    public static final int ALARM_GLOW = 0x73FF6A4D;   // flashing outline of the under-attack alert
    public static final int HOVER = 0x1FFFFFFF;
    private static final int BTN_HIGHLIGHT = 0x17FFFFFF;
    private static final int RING_DARK = 0xFF14131A;
    public static final int CHIP_ON = 0xFF3A2A14;
    public static final int BUTTON_HOVER = 0xFF33323D;

    /** Bevel thickness: the design's 3-4 px edges at half scale, rounded up. */
    public static final int BEVEL = 2;

    public enum CellState { READY, ACTIVE, POOR, COOLDOWN, LOCKED, EMPTY }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static int hpColor(float fraction) {
        if (fraction > 0.5f)
            return OK;
        return fraction > 0.25f ? YELLOW : ERROR;
    }

    public static int withAlpha(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0, Math.min(1, alpha)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int t, int colour) {
        g.fill(x - t, y - t, x + w + t, y, colour);
        g.fill(x - t, y + h, x + w + t, y + h + t, colour);
        g.fill(x - t, y, x, y + h, colour);
        g.fill(x + w, y, x + w + t, y + h, colour);
    }

    /** Inner ring drawn just inside the rectangle. */
    public static void ring(GuiGraphics g, int x, int y, int w, int h, int t, int colour) {
        outline(g, x + t, y + t, w - 2 * t, h - 2 * t, t, colour);
    }

    /** Main surface: dark fill, soft top highlight, bottom shade and an ink outline. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        panel(g, x, y, w, h, PANEL);
    }

    /** See-through panel for the HUD's floating bottom cards, so the world shows behind them. */
    public static final int FLOATING = 0xB316151B; // 70% solid

    public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill) {
        bevel(g, x, y, w, h, fill, HIGHLIGHT_SOFT, SHADE, 1, INK);
    }

    /**
     * The one bevel recipe: fill, a top highlight and bottom shade BEVEL units thick (0 colour = none),
     * and an outer ring.
     */
    public static void bevel(GuiGraphics g, int x, int y, int w, int h, int fill, int top, int bottom, int ringW, int ring) {
        if (ringW > 0)
            outline(g, x, y, w, h, ringW, ring);
        g.fill(x, y, x + w, y + h, fill);
        int t = Math.min(BEVEL, h / 4);
        if (top != 0)
            g.fill(x, y, x + w, y + t, top);
        if (bottom != 0)
            g.fill(x, y + h - t, x + w, y + h, bottom);
    }

    /** Popup surface (tooltip, toast, chat, menu) with a coloured top edge. */
    public static void popup(GuiGraphics g, int x, int y, int w, int h, int accent) {
        bevel(g, x, y, w, h, 0xF516151B, accent, 0, 1, INK);
    }

    /** Small card inside a panel (stat cards, sub-panels). */
    public static void card(GuiGraphics g, int x, int y, int w, int h) {
        bevel(g, x, y, w, h, PANEL, 0x0AFFFFFF, 0, 1, INK);
    }

    public static void cell(GuiGraphics g, int x, int y, int w, int h, CellState state, boolean hovered) {
        switch (state) {
            case EMPTY -> {
                bevel(g, x, y, w, h, EMPTY, SHADE_EMPTY, 0, 1, INK);
                return;
            }
            case LOCKED -> bevel(g, x, y, w, h, LOCKED, 0, 0, 1, INK);
            case POOR -> bevel(g, x, y, w, h, POOR, POOR_GLOW, SHADE, 1, INK);
            case ACTIVE -> {
                bevel(g, x, y, w, h, ACTIVE, 0, SHADE_SOFT, 1, INK);
                ring(g, x, y, w, h, 1, ORANGE);
            }
            default -> bevel(g, x, y, w, h, RAISED, HIGHLIGHT, SHADE, 1, INK);
        }
        if (hovered && state != CellState.LOCKED)
            g.fill(x, y, x + w, y + h, HOVER);
    }

    public static void divider(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, INK);
    }

    /** Progress or health bar with a sunken track. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int colour) {
        g.fill(x, y, x + w, y + h, INK);
        int filled = Math.round(w * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) {
            g.fill(x, y, x + filled, y + h, colour);
            if (h >= 3) {
                g.fill(x, y, x + filled, y + 1, 0x59FFFFFF);
                g.fill(x, y + h - 1, x + filled, y + h, 0x40000000);
            }
        }
    }

    public static void primaryButton(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        bevel(g, x, y, w, h, ORANGE, GOLD, ORANGE_DARK, 1, ON_ORANGE);
        if (hovered)
            g.fill(x, y, x + w, y + h, HOVER);
    }

    /** Big header-style button (GAME RULES, LEAVE): highlight, shade and a dark ring. */
    public static void secondaryButton(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        bevel(g, x, y, w, h, hovered ? BUTTON_HOVER : SECONDARY, BTN_HIGHLIGHT, SHADE, 1, RING_DARK);
    }

    /** {@link #secondaryButton} in another fill colour (the title screen's Discord button). */
    public static void colourButton(GuiGraphics g, int x, int y, int w, int h, int fill) {
        bevel(g, x, y, w, h, fill, BTN_HIGHLIGHT, SHADE, 1, RING_DARK);
    }

    /** Small row button (ADD AI, CLOSE, SEND): highlight only. */
    public static void button(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        bevel(g, x, y, w, h, hovered ? BUTTON_HOVER : SECONDARY, BTN_HIGHLIGHT, 0, 1, INK);
    }

    /** Red-text variant of {@link #button} (KICK). */
    public static void dangerButton(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        bevel(g, x, y, w, h, DANGER_BG, BTN_HIGHLIGHT, 0, 1, INK);
        if (hovered)
            g.fill(x, y, x + w, y + h, HOVER);
    }

    /** Flat chip (mode, rule toggle, faction pick): ring only when off, orange inner ring when on. */
    public static void chip(GuiGraphics g, int x, int y, int w, int h, boolean on, boolean hovered) {
        bevel(g, x, y, w, h, on ? CHIP_ON : hovered ? SECONDARY : RAISED, 0, 0, 1, INK);
        if (on)
            ring(g, x, y, w, h, 1, ORANGE);
    }

    public enum ReadyState { CAN_READY, READY, DISABLED }

    /** The big READY button: orange with a drop shadow, green once ready, grey when it can't be used. */
    public static void readyButton(GuiGraphics g, int x, int y, int w, int h, ReadyState state, boolean hovered) {
        switch (state) {
            case CAN_READY -> {
                g.fill(x, y + h, x + w, y + h + 3, 0xFF7A2C06);
                bevel(g, x, y, w, h, ORANGE, GOLD, ORANGE_DARK, 1, ON_ORANGE);
            }
            case READY -> bevel(g, x, y, w, h, OK_DARK, OK, 0xFF0B2A0B, 1, 0xFF0B2A0B);
            case DISABLED -> bevel(g, x, y, w, h, 0xFF2A2930, 0x0DFFFFFF, 0x80000000, 1, RING_DARK);
        }
        if (hovered && state != ReadyState.DISABLED)
            g.fill(x, y, x + w, y + h, HOVER);
    }

    /** Hotkey chip in the top-left corner of a cell, in the small font. Returns its width. */
    public static int keyCap(GuiGraphics g, String key, int x, int y, int textColour) {
        if (key == null || key.isEmpty())
            return 0;
        int w = Text.tinyWidth(key) + 3;
        g.fill(x, y, x + w, y + 7, 0xD90B0A0F);
        Text.tiny(g, key, x + 2, y - 1, textColour);
        return w;
    }

    /** Orange count badge, e.g. idle workers. */
    public static void badge(GuiGraphics g, String text, int rightX, int topY) {
        int w = Math.max(9, Text.tinyWidth(text) + 5);
        int x = rightX - w;
        outline(g, x, topY, w, 9, 1, ON_ORANGE);
        orangeChip(g, x, topY, w, 9);
        Text.tiny(g, text, x + (w - Text.tinyWidth(text) + 1) / 2, topY, ON_ORANGE);
    }

    /** Orange chip with a gold top edge (badges, hero level, skill points); text goes on top in ON_ORANGE. */
    public static void orangeChip(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, ORANGE);
        g.fill(x, y, x + w, y + 1, GOLD);
    }

    /** Padlock icon centred in the box (x, y, size), used on locked cells and seats. */
    public static void lock(GuiGraphics g, int x, int y, int size, int colour) {
        Icon.LOCK.drawIn(g, x - 2, y - 2, size + 4, size + 4, colour);
    }

    /** Close icon centred in the box (x, y, size). */
    public static void cross(GuiGraphics g, int x, int y, int size, int colour) {
        Icon.CLOSE.drawIn(g, x - 2, y - 2, size + 4, size + 4, colour);
    }

    /** Close icon (6 units) exactly centred on a button, half units included. */
    public static void cross(GuiGraphics g, com.solegendary.reignofnether.ui.Rect r, int colour) {
        g.pose().pushPose();
        g.pose().translate(r.x() + (r.w() - 6) / 2f, r.y() + (r.h() - 6) / 2f, 0);
        Icon.CLOSE.draw(g, 0, 0, 6, colour);
        g.pose().popPose();
    }
}
