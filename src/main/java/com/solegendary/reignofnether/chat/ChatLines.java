package com.solegendary.reignofnether.chat;

import com.solegendary.reignofnether.chat.ChatLog.Entry;
import com.solegendary.reignofnether.ui.Clip;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.ChatVisiblity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Chat lines in HUD units, drawn exactly as vanilla chat formats them. */
public final class ChatLines {

    private ChatLines() { }

    public static final int LINE_H = 11;
    private static final int STRIP = 0x990B0A0F;
    private static final long RECENT_MS = 10_000;
    private static final long FADE_MS = 1_500;

    // wrapping is per entry and width; entries never change once logged
    private static final Map<Integer, Map<Entry, List<FormattedCharSequence>>> WRAPPED = new HashMap<>();

    /**
     * Scrollable log: the newest line sits on the bottom of area, older ones above, clipped to area.
     *
     * @param scroll lines scrolled up from the newest
     * @return the total number of wrapped lines, to bound scroll
     */
    public static int render(GuiGraphics g, List<Entry> entries, Rect area, int scroll) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Entry e : entries)
            lines.addAll(wrap(e, area.w()));
        Clip.push(g, area.x(), area.y(), area.w(), area.h());
        int y = area.bottom() - LINE_H;
        // only whole lines: a half-cut line at the top reads as broken
        for (int i = lines.size() - 1 - scroll; i >= 0 && y >= area.y(); i--, y -= LINE_H) {
            drawLine(g, lines.get(i), area.x(), y, 1f);
        }
        Clip.pop(g);
        return lines.size();
    }

    /** The last 4 lines of the past 10 s, each on a dark strip, stacked up from bottom and fading out. */
    public static void renderRecent(GuiGraphics g, int x, int bottom, int maxW) {
        if (Minecraft.getInstance().options.chatVisibility().get() == ChatVisiblity.HIDDEN)
            return;
        List<Entry> recent = ChatLog.recent(4, RECENT_MS);
        int y = bottom;
        for (int i = recent.size() - 1; i >= 0; i--) {
            Entry e = recent.get(i);
            float alpha = Math.min(1f, (RECENT_MS - e.age()) / (float) FADE_MS);
            // vanilla text treats an almost-transparent colour as opaque
            if (alpha < 0.05f)
                continue;
            List<FormattedCharSequence> wrapped = wrap(e, maxW - 10);
            int textW = 0;
            for (FormattedCharSequence s : wrapped)
                textW = Math.max(textW, Text.width(s));
            int h = wrapped.size() * LINE_H + 3;
            y -= h;
            g.fill(x, y, x + textW + 10, y + h, Theme.withAlpha(STRIP, alpha));
            for (int j = 0; j < wrapped.size(); j++)
                drawLine(g, wrapped.get(j), x + 5, y + 3 + j * LINE_H, alpha);
            y -= 2;
        }
    }

    /** Wrapped lines without the blank lines some server messages pad themselves with ("\nText\n"). */
    private static List<FormattedCharSequence> wrap(Entry e, int width) {
        Map<Entry, List<FormattedCharSequence>> cache = WRAPPED.computeIfAbsent(width, w -> new IdentityHashMap<>());
        if (cache.size() > 2 * ChatLog.MAX)
            cache.clear(); // drop entries that fell out of the log
        return cache.computeIfAbsent(e, k -> wrapUncached(e, width));
    }

    private static List<FormattedCharSequence> wrapUncached(Entry e, int width) {
        List<FormattedCharSequence> lines = new ArrayList<>(Theme.font().split(e.line(), Math.max(20, width)));
        while (!lines.isEmpty() && isBlank(lines.get(0)))
            lines.remove(0);
        while (!lines.isEmpty() && isBlank(lines.get(lines.size() - 1)))
            lines.remove(lines.size() - 1);
        return lines;
    }

    private static boolean isBlank(FormattedCharSequence s) {
        return s.accept((i, style, cp) -> Character.isWhitespace(cp));
    }

    // the line exactly as vanilla chat shows it: only the frame around chat is restyled, not the messages
    private static void drawLine(GuiGraphics g, FormattedCharSequence text, int x, int y, float alpha) {
        g.drawString(Theme.font(), text, x, y, Theme.withAlpha(Theme.TEXT, alpha), true);
    }
}
