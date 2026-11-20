package com.solegendary.reignofnether.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** The website's background used behind menus (pause, options and match setup; the title keeps its panorama). */
public final class Backdrop {

    private Backdrop() { }

    // the website's colours: the page, the warmer band behind its sections, and the ember glow
    private static final int VOID = 0x0D0305;
    private static final int VOID_DEEP = 0x14060A;
    private static final int EMBER = 0xDD6003;
    private static final int FLAME = 0xF19A0F;
    private static final int ASH = 60;

    /**
     * The website's background: the dark red void, warming toward the bottom, a faint ember glow from below and ash
     * drifting up through it.
     *
     * @param opacity 1 hides what is behind; less lets the world show through (the pause menu)
     */
    public static void website(GuiGraphics g, int width, int height, float opacity) {
        int a = Math.round(opacity * 255);
        g.fill(0, 0, width, height, a << 24 | VOID);
        g.fillGradient(0, height / 3, width, height, VOID_DEEP, Math.round(opacity * 0.7f * 255) << 24 | VOID_DEEP);
        emberGlow(g, width, height, opacity);
        ash(g, width, height, opacity, 0);
    }

    /**
     * Ash drifting up, dying out on the way. Each speck has its own speed, size and sway from its index, so nothing
     * needs storing; shiftX slides them all sideways, wrapping round (the title screen moves them with its panorama).
     */
    public static void ash(GuiGraphics g, int width, int height, float opacity, float shiftX) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < ASH; i++) {
            float seed = (i * 0.6180339f) % 1f;
            float speed = 0.012f + 0.02f * ((i * 0.37f) % 1f); // gui px per ms
            int span = height + 20;
            float y = height + 10 - ((now * speed + seed * span * 7) % span);
            float x = Mth.positiveModulo(seed * width + (float) Math.sin(now / 1400.0 + i) * 6 + shiftX, width);
            int size = i % 3 == 0 ? 2 : 1;
            float fade = Math.min(1, y / (height * 0.35f));
            int colour = i % 4 == 0 ? FLAME : EMBER;
            g.fill((int) x, (int) y, (int) x + size, (int) y + size, Math.round(opacity * fade * 0.55f * 255) << 24 | colour);
        }
    }

    /** The website's ember glow rising from the bottom edge. */
    public static void emberGlow(GuiGraphics g, int width, int height, float opacity) {
        g.fillGradient(0, height * 3 / 5, width, height, EMBER, Math.round(opacity * 0.10f * 255) << 24 | EMBER);
    }

    /**
     * The website's frame for a showcased panel: a flame border and an ember glow spreading out
     * around it (its 80px blur, drawn as fading rings).
     */
    public static void glow(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        int rings = 24;
        for (int i = rings; i >= 1; i--) {
            float f = 1 - i / (float) rings;
            int a = Math.round(opacity * 0.3f * f * f * 0.25f * 255);
            if (a <= 0)
                continue;
            int c = a << 24 | EMBER;
            int d = i * 2;
            g.fill(x - d, y - d, x + w + d, y - d + 2, c);
            g.fill(x - d, y + h + d - 2, x + w + d, y + h + d, c);
            g.fill(x - d, y - d + 2, x - d + 2, y + h + d - 2, c);
            g.fill(x + w + d - 2, y - d + 2, x + w + d, y + h + d - 2, c);
        }
        Theme.outline(g, x - 1, y - 1, w + 2, h + 2, 1, Math.round(opacity * 255) << 24 | FLAME);
    }
}
