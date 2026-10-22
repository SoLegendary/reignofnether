package com.solegendary.reignofnether.ui;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Text drawing for the RTS UI, in the design's three sizes:
 * <ul>
 *   <li>{@link #small}: Silkscreen pixel font (caps labels, key chips, tags, hints), 5 units tall</li>
 *   <li>{@link #draw}: the vanilla font for body text</li>
 *   <li>{@link #big}: Pixelify Sans for values, names and numbers; {@link #title} adds the gold gradient</li>
 * </ul>
 * Small and vanilla text share a baseline when drawn at the same y.
 */
public final class Text {

    private Text() { }

    // Pixelify Sans baked at 2x and shown at half size: caps are 7.5 units tall at scale 1, top at y
    public static final Style BIG_STYLE = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "big"));
    public static final Style SMALL_STYLE = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "small"));
    /** Scale for big text so it matches a font size of the 1920x1080 design (scale 1 = 20 px there). */
    public static float px(int designPx) {
        return designPx / 20f;
    }
    // gold gradient, top to bottom
    private static final int[] TITLE_BANDS = { Theme.GOLD, Theme.YELLOW, Theme.ORANGE, Theme.ORANGE_DARK };
    private static final float[] TITLE_STOPS = { 0f, 0.3f, 0.6f, 0.85f, 1f };

    public static int width(String s) {
        return Theme.font().width(s);
    }

    public static int width(FormattedCharSequence s) {
        return Theme.font().width(s);
    }

    // ---- small: Silkscreen ----

    /**
     * Small labels are three quarters of a HUD unit per font pixel: between the design's half unit (too
     * small to read) and a whole unit (too big). Not always a whole number of screen pixels, never under
     * one. Caps are 5 font pixels tall.
     */
    public static float smallScale() {
        int p = HudGeometry.scale().pixelsPerUnit();
        return Math.max(1f / p, 0.75f);
    }

    // small labels are always capitals, like the design, whatever case the caller passes
    private static FormattedCharSequence small(String s) {
        return Component.literal(s.toUpperCase(java.util.Locale.ROOT)).withStyle(SMALL_STYLE).getVisualOrderText();
    }

    public static int smallWidth(String s) {
        return (int) Math.ceil(Theme.font().width(small(s)) * smallScale());
    }

    /** Small caps text, vertically centred where an 8-unit vanilla line at y would sit. */
    public static void small(GuiGraphics g, String s, int x, int y, int colour) {
        drawSmall(g, s, x, y, colour, false);
    }

    public static void smallShadowed(GuiGraphics g, String s, int x, int y, int colour) {
        drawSmall(g, s, x, y, colour, true);
    }

    private static void drawSmall(GuiGraphics g, String s, int x, int y, int colour, boolean shadow) {
        float k = smallScale();
        g.pose().pushPose();
        // caps sit on rows 2..6 of the glyph; keep their centre at y + 4.5 whatever the scale
        g.pose().translate(x, y + 4.5f * (1 - k), 0);
        g.pose().scale(k, k, 1);
        FormattedCharSequence text = small(s);
        if (shadow)
            g.drawString(Theme.font(), text, 1, 1, 0xFF000000, false);
        g.drawString(Theme.font(), text, 0, 0, colour, false);
        g.pose().popPose();
    }

    public static void smallRight(GuiGraphics g, String s, int rightX, int y, int colour) {
        small(g, s, rightX - smallWidth(s), y, colour);
    }

    public static void smallCentered(GuiGraphics g, String s, int cx, int y, int colour) {
        small(g, s, cx - smallWidth(s) / 2, y, colour);
    }

    /** Small text split on spaces so each line fits maxW. */
    public static List<String> wrapSmall(String text, int maxW) {
        List<String> lines = new ArrayList<>();
        String current = "";
        for (String word : text.split(" ")) {
            String next = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && Text.smallWidth(next) > maxW) {
                lines.add(current);
                current = word;
            } else {
                current = next;
            }
        }
        if (!current.isEmpty())
            lines.add(current);
        return lines;
    }

    public static String fitSmall(String s, int maxWidth) {
        if (smallWidth(s) <= maxWidth)
            return s;
        String cut = s;
        while (!cut.isEmpty() && smallWidth(cut + "…") > maxWidth)
            cut = cut.substring(0, cut.length() - 1);
        return cut + "…";
    }

    // ---- big: values and names, vanilla font scaled up (readable) ----

    /** Snaps a scale so each font pixel is a whole number of screen pixels. */
    private static float crisp(float scale) {
        int p = HudGeometry.scale().pixelsPerUnit();
        return Math.max(1, Math.round(scale * p)) / (float) p;
    }

    /** Height of a line of big text, top to baseline plus descender. */
    public static int bigHeight(float scale) {
        return Math.round(8 * crisp(scale));
    }

    public static int bigWidth(String s, float scale) {
        return Math.round(width(s) * crisp(scale));
    }

    /** Big value or name with a black drop shadow; y is the top of the text. */
    public static void big(GuiGraphics g, String s, int x, int y, int colour, float scale) {
        float k = crisp(scale);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(k, k, 1);
        g.drawString(Theme.font(), s, 1, 1, 0xFF000000, false);
        g.drawString(Theme.font(), s, 0, 0, colour, false);
        g.pose().popPose();
    }

    public static String fitBig(String s, int maxWidth, float scale) {
        if (bigWidth(s, scale) <= maxWidth)
            return s;
        String cut = s;
        while (!cut.isEmpty() && bigWidth(cut + "…", scale) > maxWidth)
            cut = cut.substring(0, cut.length() - 1);
        return cut + "…";
    }

    public static void bigRight(GuiGraphics g, String s, int rightX, int y, int colour, float scale) {
        big(g, s, rightX - bigWidth(s, scale), y, colour, scale);
    }

    public static void bigCentered(GuiGraphics g, String s, int cx, int y, int colour, float scale) {
        big(g, s, cx - bigWidth(s, scale) / 2, y, colour, scale);
    }

    // ---- heading: Pixelify Sans, decorative panel titles only (never numbers) ----

    public static int headingWidth(String s, float scale) {
        return titleWidth(s, scale);
    }

    public static void heading(GuiGraphics g, String s, int x, int y, int colour, float scale) {
        FormattedCharSequence text = Component.literal(s).withStyle(BIG_STYLE).getVisualOrderText();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(Theme.font(), text, 1, 1, 0xFF000000, false);
        g.drawString(Theme.font(), text, 0, 0, colour, false);
        g.pose().popPose();
    }

    // ---- body: vanilla font ----

    public static void draw(GuiGraphics g, String s, int x, int y, int colour) {
        g.drawString(Theme.font(), s, x, y, colour, false);
    }

    public static void draw(GuiGraphics g, FormattedCharSequence s, int x, int y, int colour) {
        g.drawString(Theme.font(), s, x, y, colour, false);
    }

    public static void drawCentered(GuiGraphics g, String s, int cx, int y, int colour) {
        draw(g, s, cx - width(s) / 2, y, colour);
    }

    /** Cuts the string with an ellipsis so it fits in maxWidth. */
    public static String fit(String s, int maxWidth) {
        if (width(s) <= maxWidth)
            return s;
        String ell = "…";
        String cut = Theme.font().plainSubstrByWidth(s, Math.max(0, maxWidth - width(ell)));
        return cut + ell;
    }

    public static List<FormattedCharSequence> wrap(String s, int maxWidth) {
        return Theme.font().split(Component.literal(s), maxWidth);
    }

    public static int titleWidth(String s, float scale) {
        return Math.round(Theme.font().width(Component.literal(s).withStyle(BIG_STYLE)) * scale);
    }

    /**
     * Big gold title. Scale 1 is about 8 units tall. The gradient is drawn as horizontal bands clipped
     * with the scissor, which works in screen space, so band edges are taken from the current pose.
     */
    public static void title(GuiGraphics g, String s, int x, int y, float scale) {
        FormattedCharSequence text = Component.literal(s).withStyle(BIG_STYLE).getVisualOrderText();
        int w = Theme.font().width(text);
        int h = 8;

        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(Theme.font(), text, 1, 1, Theme.ON_ORANGE, false);

        var matrix = g.pose().last().pose();
        for (int i = 0; i < TITLE_BANDS.length; i++) {
            Vector4f top = matrix.transform(new Vector4f(0, h * TITLE_STOPS[i], 0, 1));
            Vector4f bottom = matrix.transform(new Vector4f(w + 1, h * TITLE_STOPS[i + 1], 0, 1));
            int y2 = i == TITLE_BANDS.length - 1 ? (int) Math.ceil(bottom.y) + 4 : Math.round(bottom.y);
            g.enableScissor((int) Math.floor(top.x), Math.round(top.y), (int) Math.ceil(bottom.x) + 1, y2);
            g.drawString(Theme.font(), text, 0, 0, TITLE_BANDS[i], false);
            g.disableScissor();
        }
        g.pose().popPose();
    }
}
