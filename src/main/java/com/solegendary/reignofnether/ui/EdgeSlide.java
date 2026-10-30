package com.solegendary.reignofnether.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Parts of the interface coming and going through the nearest edge of the screen: a top bar from the top, a side
 * panel from its side. Those nearest an edge come in first and leave last, so the screen fills from the outside in
 * and empties from the inside out. Coming in they land with a little bounce; going out they speed up as they leave.
 */
public final class EdgeSlide {

    private EdgeSlide() { }

    public static final long IN_MS = 450;
    public static final long OUT_MS = 320;
    public static final long SPREAD_MS = 180; // between the part nearest an edge and the one furthest from any
    private static final int MARGIN = 12;     // how far past the edge it goes, so its glow and shadow leave too

    /** How long until every part has arrived or gone. */
    public static long total(boolean entering) {
        return (entering ? IN_MS : OUT_MS) + SPREAD_MS;
    }

    /** 0 touching an edge, 1 as far from every edge as anything can be. */
    public static float depth(Rect r, int w, int h) {
        float d = Math.min(Math.min(r.x(), r.y()), Math.min(w - r.right(), h - r.bottom()));
        return Mth.clamp(d / (Math.min(w, h) / 2f), 0, 1);
    }

    /** The move that takes it fully off through its nearest edge; a tie goes to the top or bottom. */
    public static float[] away(Rect r, int w, int h) {
        int top = r.y(), bottom = h - r.bottom(), left = r.x(), right = w - r.right();
        int vertical = Math.min(top, bottom), sideways = Math.min(left, right);
        if (vertical <= sideways)
            return top <= bottom ? new float[] { 0, -(r.bottom() + MARGIN) } : new float[] { 0, h - r.y() + MARGIN };
        return left <= right ? new float[] { -(r.right() + MARGIN), 0 } : new float[] { w - r.x() + MARGIN, 0 };
    }

    /** 1 in place, a little past 1 for the bounce, 0 off the screen. */
    public static float in(long sinceMs, float depth, boolean entering) {
        if (entering)
            return Easing.heavy((sinceMs - depth * SPREAD_MS) / (float) IN_MS);
        return 1 - Easing.dive((sinceMs - (1 - depth) * SPREAD_MS) / (float) OUT_MS);
    }

    public static float alpha(long sinceMs, float depth, boolean entering) {
        if (entering)
            return smooth((sinceMs - depth * SPREAD_MS) / (IN_MS * 0.4f));
        return 1 - smooth((sinceMs - (1 - depth) * SPREAD_MS - OUT_MS * 0.4f) / (OUT_MS * 0.6f));
    }

    /**
     * Starts drawing a part at rect (in the units w and h are in); everything until pop moves and fades with it.
     * unitScale converts those units to the pose's, for parts drawn without the HUD's scaling.
     */
    public static void push(GuiGraphics g, Rect r, int w, int h, long sinceMs, boolean entering, float unitScale) {
        float depth = depth(r, w, h);
        float p = in(sinceMs, depth, entering);
        float[] away = away(r, w, h);
        g.flush(); // what came before keeps its own fade
        g.pose().pushPose();
        g.pose().translate(away[0] * (1 - p) * unitScale, away[1] * (1 - p) * unitScale, 0);
        RenderSystem.setShaderColor(1, 1, 1, Mth.clamp(alpha(sinceMs, depth, entering), 0, 1));
    }

    /** Starts a part that only fades, in step with the rest. */
    public static void pushFade(GuiGraphics g, long sinceMs, boolean entering) {
        g.flush();
        g.pose().pushPose();
        float a = entering ? smooth(sinceMs / (IN_MS * 0.5f)) : 1 - smooth(sinceMs / (float) total(false));
        RenderSystem.setShaderColor(1, 1, 1, Mth.clamp(a, 0, 1));
    }

    public static void pop(GuiGraphics g) {
        g.flush();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.pose().popPose();
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
