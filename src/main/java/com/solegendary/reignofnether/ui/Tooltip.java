package com.solegendary.reignofnether.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The one tooltip of the RTS UI: title with an optional tag, body lines, coloured status lines and a
 * footer with costs and the hotkey. Built fluently and shown with {@link #show}; the HUD draws the
 * pending tooltip last so it is always on top.
 */
public final class Tooltip {

    public static final int WIDTH = 170;
    private static final int PAD = 7;

    private String title;
    private static final float TITLE_SCALE = Text.px(20);
    private int titleColour = Theme.TEXT;
    private String tag;
    private int tagColour = Theme.ORANGE;
    private int accent = Theme.ORANGE;
    private final List<FormattedCharSequence> body = new ArrayList<>();
    private final List<Line> status = new ArrayList<>();
    private final List<FooterEntry> footer = new ArrayList<>();
    private String key;

    private record Line(String text, int colour) { }

    public record FooterEntry(@Nullable ResourceLocation icon, String text, int colour) { }

    public static Tooltip of(String title) {
        Tooltip t = new Tooltip();
        t.title = title;
        return t;
    }

    public static Tooltip of(FormattedCharSequence title) {
        StringBuilder sb = new StringBuilder();
        title.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return of(sb.toString());
    }

    /**
     * Wraps the legacy tooltip lines of a Button: first line becomes the title, the rest the body.
     * Legacy lines keep their styles, including the resource icon glyphs.
     */
    public static Tooltip fromLines(List<FormattedCharSequence> lines) {
        if (lines == null || lines.isEmpty())
            return null;
        Tooltip t = of(lines.get(0));
        for (int i = 1; i < lines.size(); i++)
            t.body.add(lines.get(i));
        // legacy tooltips often end on a blank spacer line
        while (!t.body.isEmpty() && Text.width(t.body.get(t.body.size() - 1)) == 0)
            t.body.remove(t.body.size() - 1);
        return t;
    }

    public Tooltip titleColour(int colour) { this.titleColour = colour; return this; }

    public Tooltip tag(String tag, int colour) { this.tag = tag; this.tagColour = colour; return this; }

    public Tooltip danger() { this.accent = Theme.ERROR; return this; }

    public Tooltip body(String text) {
        body.addAll(Text.wrap(text, WIDTH - PAD * 2));
        return this;
    }

    public Tooltip body(FormattedCharSequence line) { body.add(line); return this; }

    public Tooltip status(String text, int colour) {
        if (text != null && !text.isEmpty())
            status.add(new Line(text, colour));
        return this;
    }

    public Tooltip cost(@Nullable ResourceLocation icon, String text, int colour) {
        footer.add(new FooterEntry(icon, text, colour));
        return this;
    }

    public Tooltip key(String key) { this.key = key; return this; }

    private static final int MAX_WIDTH = 230;

    /** A legacy line wider than maxW, split keeping its styles (colours, icon glyphs). */
    private static List<FormattedCharSequence> wrapLine(FormattedCharSequence line, int maxW) {
        if (Text.width(line) <= maxW)
            return List.of(line);
        MutableComponent c = Component.empty();
        line.accept((index, style, codePoint) -> {
            c.append(Component.literal(Character.toString(codePoint)).withStyle(style));
            return true;
        });
        return Text.wrap(c, maxW);
    }

    /**
     * Draws the tooltip next to an anchor rectangle: above it when the anchor is in the lower half of
     * the screen, below it otherwise, clamped to the screen. Lines wider than the tooltip are wrapped.
     */
    public void render(GuiGraphics g, int anchorX, int anchorY, int anchorW, int anchorH, int screenW, int screenH) {
        int maxW = Math.min(MAX_WIDTH, screenW - 8) - PAD * 2;
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (FormattedCharSequence line : body)
            lines.addAll(wrapLine(line, maxW));
        List<Line> statusLines = new ArrayList<>();
        for (Line line : status)
            for (String part : Text.wrapSmall(line.text, maxW))
                statusLines.add(new Line(part, line.colour));

        int tagW = tag != null ? Text.smallWidth(tag) + 10 : 0;
        String shownTitle = Text.fitBig(title, maxW - tagW, TITLE_SCALE);
        int cw = Text.bigWidth(shownTitle, TITLE_SCALE) + tagW;
        for (FormattedCharSequence line : lines)
            cw = Math.max(cw, Text.width(line));
        for (Line line : statusLines)
            cw = Math.max(cw, Text.smallWidth(line.text));
        int fw = 0;
        for (FooterEntry e : footer)
            fw += Text.width(e.text) + (e.icon != null ? 10 : 0) + 6;
        if (key != null)
            fw += Text.tinyWidth(key) + 8;
        cw = Math.min(maxW, Math.max(cw, fw));

        int titleH = Text.bigHeight(TITLE_SCALE) + 2;
        int h = PAD + titleH;
        if (!lines.isEmpty())
            h += 2 + lines.size() * Text.lineHeight();
        if (!statusLines.isEmpty())
            h += 3 + statusLines.size() * 8;
        h += PAD - 2;
        if (!footer.isEmpty() || key != null)
            h += 15;

        int w = cw + PAD * 2;
        int x = Math.max(4, Math.min(anchorX + anchorW / 2 - w / 2, screenW - w - 4));
        int y = anchorY > screenH / 2 ? anchorY - h - 6 : anchorY + anchorH + 6;
        y = Math.max(4, Math.min(y, screenH - h - 4));

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        Theme.popup(g, x, y, w, h, accent);

        int cy = y + PAD;
        Text.big(g, shownTitle, x + PAD, cy, titleColour, TITLE_SCALE);
        if (tag != null)
            Text.smallRight(g, tag, x + w - PAD, cy, tagColour);
        cy += titleH;

        if (!lines.isEmpty()) {
            cy += 2;
            for (FormattedCharSequence line : lines) {
                Text.draw(g, line, x + PAD, cy, Theme.DESC);
                cy += Text.lineHeight();
            }
        }
        if (!statusLines.isEmpty()) {
            cy += 3;
            for (Line line : statusLines) {
                Text.small(g, line.text, x + PAD, cy - 1, line.colour);
                cy += 8;
            }
        }
        if (!footer.isEmpty() || key != null) {
            int fy = y + h - 15;
            g.fill(x, fy, x + w, y + h, Theme.SUNKEN);
            g.fill(x, fy, x + w, fy + 1, Theme.INK);
            int fx = x + PAD;
            for (FooterEntry e : footer) {
                if (e.icon != null) {
                    g.blit(e.icon, fx, fy + 4, 0, 0, 8, 8, 8, 8);
                    fx += 10;
                }
                Text.draw(g, e.text, fx, fy + 4, e.colour);
                fx += Text.width(e.text) + 6;
            }
            if (key != null) {
                int kw = Text.tinyWidth(key) + 5;
                int kx = x + w - PAD - kw;
                g.fill(kx, fy + 3, kx + kw, fy + 12, Theme.INK);
                Theme.ring(g, kx, fy + 3, kw, 9, 1, Theme.FAINT);
                Text.tiny(g, key, kx + 3, fy + 3, Theme.GOLD);
            }
        }
        g.pose().popPose();
    }

    // ---- pending tooltip for the current frame ----

    private static Tooltip pending;
    private static int pendingX, pendingY, pendingW, pendingH;

    /** Queues this tooltip to be drawn at the end of the frame. Later calls win. */
    public void show(int anchorX, int anchorY, int anchorW, int anchorH) {
        pending = this;
        pendingX = anchorX;
        pendingY = anchorY;
        pendingW = anchorW;
        pendingH = anchorH;
    }

    public static void renderPending(GuiGraphics g, int screenW, int screenH) {
        if (pending != null)
            pending.render(g, pendingX, pendingY, pendingW, pendingH, screenW, screenH);
        pending = null;
    }
}
