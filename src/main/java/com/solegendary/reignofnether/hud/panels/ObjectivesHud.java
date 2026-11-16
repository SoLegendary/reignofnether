package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoClientEvents;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The vanilla bossbars, in the HUD's style (the vanilla ones are hidden in the RTS view): a map's objectives
 * (RonBars) in a panel at the right under the top-right bar, its countdowns and any other bar centred at the top.
 */
public final class ObjectivesHud {

    private ObjectivesHud() { }

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Field EVENTS = ObfuscationReflectionHelper.findField(BossHealthOverlay.class, "f_93699_");
    private static final int W = 160;
    private static final int MAX_W = 230;
    private static final int PAD = 6;
    private static final long NEW_MS = 4000;

    // when each objective first showed, for its "New objective" card
    private static final Map<UUID, Long> firstSeen = WorldState.client(new HashMap<>());

    @SuppressWarnings("unchecked")
    private static Collection<LerpingBossEvent> bars() {
        try {
            return ((Map<UUID, LerpingBossEvent>) EVENTS.get(MC.gui.getBossOverlay())).values();
        } catch (IllegalAccessException e) {
            return List.of();
        }
    }

    // RonBars' "ron:<kind> <number>" insertion, as {kind, number}
    @Nullable
    private static String[] tag(LerpingBossEvent e) {
        String s = e.getName().getStyle().getInsertion();
        return s != null && s.startsWith("ron:") ? s.substring(4).split(" ") : null;
    }

    private static String name(LerpingBossEvent e) {
        return RTSMapInfoClientEvents.mapText(e.getName().getString());
    }

    private static int number(String[] tag) {
        try {
            return tag.length > 1 ? Integer.parseInt(tag[1]) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Draws them; returns where the free space under the objectives starts. */
    public static int render(HudFrame f, int top) {
        List<LerpingBossEvent> objectives = new ArrayList<>();
        List<LerpingBossEvent> others = new ArrayList<>();
        for (LerpingBossEvent e : bars()) {
            String[] t = tag(e);
            (t != null && t[0].equals("objective") ? objectives : others).add(e);
        }
        renderTop(f, others);
        return objectives.isEmpty() ? top : renderObjectives(f, objectives, top);
    }

    private static void renderTop(HudFrame f, List<LerpingBossEvent> bars) {
        int y = HudLayout.MARGIN;
        float big = Text.px(28);
        for (LerpingBossEvent e : bars) {
            String[] t = tag(e);
            String name = name(e);
            String time = t != null && t[0].equals("timer") ? clock(number(t)) : null;
            int w = Math.max(Text.smallWidth(name), time != null ? Text.bigWidth(time, big) : 100) + PAD * 4;
            int h = time != null ? 14 + Text.bigHeight(big) + PAD : 22;
            int x = (f.layout.width() - w) / 2;
            Theme.panel(f.g, x, y, w, h, Theme.floating());
            f.g.fill(x, y, x + w, y + 1, Theme.ORANGE);
            Text.smallCentered(f.g, name, x + w / 2, y + 4, Theme.LABEL);
            if (time != null)
                Text.bigCentered(f.g, time, x + w / 2, y + 14, Theme.GOLD, big);
            else
                Theme.bar(f.g, x + PAD, y + 14, w - PAD * 2, 3, e.getProgress(), Theme.ORANGE);
            f.zone(x, y, w, h);
            y += h + HudLayout.GAP;
        }
    }

    private static String clock(int seconds) {
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private static int renderObjectives(HudFrame f, List<LerpingBossEvent> objectives, int top) {
        // as wide as the longest line needs, up to MAX_W; longer ones wrap
        int w = W;
        for (LerpingBossEvent e : objectives)
            w = Math.max(w, PAD * 2 + 10 + Text.width(name(e)) + 4 + Text.smallWidth(count(e)));
        w = Math.min(w, MAX_W);
        int x = f.layout.width() - HudLayout.MARGIN - w;
        int textX = x + PAD + 10;
        List<List<FormattedCharSequence>> lines = new ArrayList<>();
        int h = PAD + 12 + PAD;
        for (LerpingBossEvent e : objectives) {
            String count = count(e);
            lines.add(Theme.font().split(FormattedText.of(name(e)), x + w - PAD - textX - (count.isEmpty() ? 0 : Text.smallWidth(count) + 4)));
            h += lines.get(lines.size() - 1).size() * 10 + 2 + (showsBar(e) ? 4 : 0);
        }
        Theme.panel(f.g, x, top, w, h, Theme.floating());
        Text.small(f.g, I18n.get("hud.reignofnether.objectives"), x + PAD, top + PAD, Theme.LABEL);

        int y = top + PAD + 12;
        for (int i = 0; i < objectives.size(); i++) {
            LerpingBossEvent e = objectives.get(i);
            boolean done = e.getProgress() >= 1;
            Theme.outline(f.g, x + PAD + 1, y + 2, 5, 5, 1, done ? Theme.OK : Theme.LABEL);
            if (done)
                f.g.fill(x + PAD + 1, y + 2, x + PAD + 6, y + 7, Theme.OK_DARK);
            Text.smallRight(f.g, count(e), x + w - PAD, y, done ? Theme.DIM : Theme.GOLD);
            for (FormattedCharSequence line : lines.get(i)) {
                f.g.drawString(Theme.font(), line, textX, y, done ? Theme.DIM : Theme.TEXT, false);
                if (done)
                    f.g.fill(textX, y + 4, textX + Text.width(line), y + 5, Theme.DIM);
                y += 10;
            }
            y += 2;
            if (showsBar(e)) {
                Theme.bar(f.g, textX, y - 1, x + w - PAD - textX, 2, e.getProgress(), Theme.ORANGE);
                y += 4;
            }
        }
        f.zone(x, top, w, h);
        return renderNew(f, objectives, x, w, top + h + HudLayout.GAP);
    }

    private static String count(LerpingBossEvent e) {
        int goal = number(tag(e));
        return goal > 0 ? Math.round(e.getProgress() * goal) + "/" + goal : "";
    }

    private static boolean showsBar(LerpingBossEvent e) {
        return number(tag(e)) > 1 && e.getProgress() < 1;
    }

    private static int renderNew(HudFrame f, List<LerpingBossEvent> objectives, int x, int w, int y) {
        long now = System.currentTimeMillis();
        for (LerpingBossEvent e : objectives) {
            if (now - firstSeen.computeIfAbsent(e.getId(), id -> now) >= NEW_MS)
                continue;
            List<FormattedCharSequence> lines = Theme.font().split(FormattedText.of(name(e)), w - PAD * 2);
            int h = 14 + lines.size() * 10 + 2;
            Theme.panel(f.g, x, y, w, h, Theme.ACTIVE);
            Theme.outline(f.g, x, y, w, h, 1, Theme.ORANGE);
            Text.small(f.g, I18n.get("hud.reignofnether.objectives.new"), x + PAD, y + 3, Theme.ORANGE);
            for (int i = 0; i < lines.size(); i++)
                f.g.drawString(Theme.font(), lines.get(i), x + PAD, y + 14 + i * 10, Theme.GOLD, false);
            y += h + HudLayout.GAP;
        }
        return y;
    }
}
