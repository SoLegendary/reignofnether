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
 *   <li>{@link #small}: Silkscreen caps labels, tags and hints (the design's 8-10px); {@link #medium}: its 11-12px ones</li>
 *   <li>{@link #draw}: the vanilla font for body text, like chat; {@link #medium} is it in capitals</li>
 *   <li>{@link #big}: the vanilla font for values, names and numbers; {@link #heading} and {@link #title} (gold) are Pixelify Sans, for titles only</li>
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

    /**
     * Medium and body text are the vanilla font at 2 screen pixels per font pixel (like chat), small and tiny
     * are Silkscreen at 1, big is the vanilla font at 3.
     * Every size sits on whole screen pixels, so nothing is soft.
     */
    public static float bodyScale() {
        return 1f; // the vanilla font at 1 font pixel per unit: 2 screen pixels at GUI scale 3-4, like chat
    }

    /** Distance between two lines of body text. */
    public static int lineHeight() {
        return Math.max(5, Math.round(10 * bodyScale()));
    }

    /** Width of body text. */
    public static int width(String s) {
        return Theme.font().width(s);
    }

    public static int width(FormattedCharSequence s) {
        return Theme.font().width(s);
    }

    // ---- small and tiny: Silkscreen capitals at 1x (labels, tags, hints; key chips and counts in cells) ----

    /** Tiny is the design's 9px Silkscreen on whole screen pixels: 1 screen pixel per font pixel at GUI scale 3-4. */
    public static float tinyScale() {
        int p = HudGeometry.scale().pixelsPerUnit();
        return Math.max(1, Math.round(9 * p / 16f)) / (float) p;
    }

    // labels are always capitals, like the design, whatever case the caller passes
    private static FormattedCharSequence caps(String s) {
        return Component.literal(s.toUpperCase(java.util.Locale.ROOT)).withStyle(SMALL_STYLE).getVisualOrderText();
    }

    // medium: the vanilla font in capitals
    private static FormattedCharSequence label(String s) {
        return Component.literal(s.toUpperCase(java.util.Locale.ROOT)).getVisualOrderText();
    }

    public static int smallWidth(String s) {
        return tinyWidth(s);
    }

    public static int tinyWidth(String s) {
        return (int) Math.ceil(Theme.font().width(caps(s)) * tinyScale());
    }

    /** Small caps text, vertically centred where an 8-unit vanilla line at y would sit. */
    public static void small(GuiGraphics g, String s, int x, int y, int colour) {
        // Silkscreen's caps sit on rows 2..6 of the glyph: their centre is 4.5 font pixels down
        draw(g, caps(s), tinyScale(), 4.5f, x, y, colour, false);
    }

    public static void smallShadowed(GuiGraphics g, String s, int x, int y, int colour) {
        draw(g, caps(s), tinyScale(), 4.5f, x, y, colour, true);
    }

    /** Key chips, counts and costs inside cells, where there is no room for small labels. */
    public static void tiny(GuiGraphics g, String s, int x, int y, int colour) {
        draw(g, caps(s), tinyScale(), 4.5f, x, y, colour, false);
    }

    public static void tinyShadowed(GuiGraphics g, String s, int x, int y, int colour) {
        draw(g, caps(s), tinyScale(), 4.5f, x, y, colour, true);
    }

    public static void tinyRight(GuiGraphics g, String s, int rightX, int y, int colour) {
        tiny(g, s, rightX - tinyWidth(s), y, colour);
    }

    private static final int[][] OUTLINE = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    /** Keeps the caps' centre (capsCentre font units down the line) at y + 4.5, on a whole screen pixel. */
    private static void draw(GuiGraphics g, FormattedCharSequence text, float k, float capsCentre, int x, int y, int colour, boolean outline) {
        int p = HudGeometry.scale().pixelsPerUnit();
        g.pose().pushPose();
        g.pose().translate(x, y + Math.round((4.5f - capsCentre * k) * p) / (float) p, 0);
        g.pose().scale(k, k, 1);
        // a black outline exactly one screen pixel wide, whatever the scale, so it never eats the letters
        if (outline) {
            float px = 1f / (k * p);
            for (int[] d : OUTLINE) {
                g.pose().pushPose();
                g.pose().translate(d[0] * px, d[1] * px, 0);
                g.drawString(Theme.font(), text, 0, 0, 0xFF000000, false);
                g.pose().popPose();
            }
        }
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

    // ---- medium: statuses and headers, bigger than small ----

    /** Medium is Silkscreen at 2 screen pixels per font pixel at GUI scale 3-4, one step over tiny. */
    public static float mediumScale() {
        return bodyScale();
    }

    public static int mediumWidth(String s) {
        return Theme.font().width(label(s));
    }

    /** Medium text (body font capitals), centred where an 8-unit vanilla line at y would sit. */
    public static void medium(GuiGraphics g, String s, int x, int y, int colour) {
        // the vanilla font's caps sit on rows 0..6: their centre is 3.5 font pixels down
        draw(g, label(s), 1f, 3.5f, x, y, colour, false);
    }

    public static void mediumRight(GuiGraphics g, String s, int rightX, int y, int colour) {
        medium(g, s, rightX - mediumWidth(s), y, colour);
    }

    public static void mediumCentered(GuiGraphics g, String s, int cx, int y, int colour) {
        medium(g, s, cx - mediumWidth(s) / 2, y, colour);
    }

    // ---- big: values, names and numbers, the vanilla font scaled up (Pixelify is for titles only) ----

    /** Snaps a scale so each font pixel is a whole number of screen pixels. */
    private static float crisp(float scale) {
        int p = HudGeometry.scale().pixelsPerUnit();
        return Math.max(1, Math.round(scale * p)) / (float) p;
    }

    /** Pixelify is baked at 2x, so one texel is half a font unit: snap to whole screen pixels per texel. */
    private static float crispBig(float scale) {
        int p = HudGeometry.scale().pixelsPerUnit();
        int n = Math.round(scale * p / 2f);
        return n >= 1 ? 2f * n / p : 1f / p;
    }

    private static FormattedCharSequence bigText(String s) {
        return Component.literal(s).withStyle(BIG_STYLE).getVisualOrderText();
    }

    private static float bigK(float scale) {
        return crisp(scale);
    }

    /** Height of a line of big text, top to baseline plus descender. */
    public static int bigHeight(float scale) {
        return Math.round(8 * bigK(scale));
    }

    public static int bigWidth(String s, float scale) {
        return Math.round(Theme.font().width(s) * bigK(scale));
    }

    /** Big value or name with a black drop shadow; y is the top of the text. */
    public static void big(GuiGraphics g, String s, int x, int y, int colour, float scale) {
        float k = bigK(scale);
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
        FormattedCharSequence text = bigText(s);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        float k = crispBig(scale);
        g.pose().scale(k, k, 1);
        g.drawString(Theme.font(), text, 1, 1, 0xFF000000, false);
        g.drawString(Theme.font(), text, 0, 0, colour, false);
        g.pose().popPose();
    }

    // ---- body: the vanilla font, as in chat ----

    public static void draw(GuiGraphics g, String s, int x, int y, int colour) {
        draw(g, Component.literal(s).getVisualOrderText(), x, y, colour);
    }

    /** Body text with Minecraft's drop shadow, like chat, but none under dark text (on orange buttons). */
    public static void draw(GuiGraphics g, FormattedCharSequence s, int x, int y, int colour) {
        int lum = ((colour >> 16) & 0xFF) * 3 + ((colour >> 8) & 0xFF) * 6 + (colour & 0xFF);
        g.drawString(Theme.font(), s, x, y, colour, lum > 128 * 10);
    }

    public static void drawCentered(GuiGraphics g, String s, int cx, int y, int colour) {
        draw(g, s, cx - width(s) / 2, y, colour);
    }

    /** Cuts the string with an ellipsis so it fits in maxWidth. */
    public static String fit(String s, int maxWidth) {
        if (width(s) <= maxWidth)
            return s;
        String ell = "…";
        String cut = s;
        while (!cut.isEmpty() && width(cut + ell) > maxWidth)
            cut = cut.substring(0, cut.length() - 1);
        return cut + ell;
    }

    public static List<FormattedCharSequence> wrap(String s, int maxWidth) {
        return wrap(Component.literal(s), maxWidth);
    }

    /** Splits styled text so each line fits maxWidth units at the body size. */
    public static List<FormattedCharSequence> wrap(Component c, int maxWidth) {
        return Theme.font().split(c, maxWidth);
    }

    public static int titleWidth(String s, float scale) {
        return Math.round(Theme.font().width(bigText(s)) * crispBig(scale));
    }

    /**
     * Big gold title. Scale 1 is about 8 units tall. The gradient is drawn as horizontal bands clipped
     * with the scissor, which works in screen space, so band edges are taken from the current pose.
     */
    public static void title(GuiGraphics g, String s, int x, int y, float scale) {
        title(g, s, x, y, scale, TITLE_BANDS, Theme.ON_ORANGE);
    }

    /** {@link #title} with its own gradient (4 bands, top to bottom) and drop shadow colour. */
    public static void title(GuiGraphics g, String s, int x, int y, float scale, int[] bands, int shadow) {
        FormattedCharSequence text = Component.literal(s).withStyle(BIG_STYLE).getVisualOrderText();
        int w = Theme.font().width(text);
        int h = 8;

        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        float k = crispBig(scale);
        g.pose().scale(k, k, 1);
        g.drawString(Theme.font(), text, 1, 1, shadow, false);

        var matrix = g.pose().last().pose();
        for (int i = 0; i < bands.length; i++) {
            Vector4f top = matrix.transform(new Vector4f(0, h * TITLE_STOPS[i], 0, 1));
            Vector4f bottom = matrix.transform(new Vector4f(w + 1, h * TITLE_STOPS[i + 1], 0, 1));
            int y2 = i == bands.length - 1 ? (int) Math.ceil(bottom.y) + 4 : Math.round(bottom.y);
            g.enableScissor((int) Math.floor(top.x), Math.round(top.y), (int) Math.ceil(bottom.x) + 1, y2);
            g.drawString(Theme.font(), text, 0, 0, bands[i], false);
            g.disableScissor();
        }
        g.pose().popPose();
    }
}
