package com.solegendary.reignofnether.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Single short message shown above the HUD console (replaces the old floating temp message).
 * One at a time: a new message replaces the current one.
 */
public final class Toast {

    private Toast() { }

    public static final int DEFAULT_TICKS = 150;
    private static final int FADE_TICKS = 20;

    private static String message = "";
    private static int colour = Theme.ORANGE;
    private static int ticksLeft = 0;

    public static void show(String msg, int ticks) {
        show(msg, ticks, Theme.ORANGE);
    }

    public static void show(String msg, int ticks, int accent) {
        message = msg == null ? "" : msg;
        ticksLeft = ticks;
        colour = accent;
    }

    public static void clear() {
        ticksLeft = 0;
    }

    public static void tick() {
        if (ticksLeft > 0)
            ticksLeft--;
    }

    public static boolean isVisible() {
        return ticksLeft > 0 && !message.isEmpty();
    }

    /** Draws the toast centred on cx with its bottom edge at bottomY. Returns its height (0 if hidden). */
    public static int render(GuiGraphics g, int cx, int bottomY) {
        if (!isVisible())
            return 0;
        float alpha = Math.min(1f, ticksLeft / (float) FADE_TICKS);
        int w = Text.width(message) + 22;
        int h = 16;
        int x = cx - w / 2;
        int y = bottomY - h;

        Theme.outline(g, x, y, w, h, 1, Theme.withAlpha(Theme.INK, alpha));
        g.fill(x, y, x + w, y + h, Theme.withAlpha(0xF516151B, alpha));
        g.fill(x, y, x + w, y + 1, Theme.withAlpha(colour, alpha));
        g.fill(x + 6, y + 6, x + 11, y + 11, Theme.withAlpha(colour, alpha));
        if (alpha > 0.1f)
            Text.draw(g, message, x + 15, y + 4, Theme.withAlpha(Theme.TEXT, alpha));
        return h;
    }
}
