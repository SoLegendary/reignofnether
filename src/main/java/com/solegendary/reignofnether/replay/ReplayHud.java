package com.solegendary.reignofnether.replay;

import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.hud.panels.SpectatorHud;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;

/**
 * The replay time bar, under the spectator scoreboard: play/pause, the match clock, a timeline to click or drag
 * through with a mark for every defeat and the end, and the playback speed.
 */
public final class ReplayHud {

    private ReplayHud() { }

    private static final int H = 22;
    private static final int PAD = 4;
    private static final int BUTTON = 14;

    private static boolean dragging = false;

    public static void render(HudFrame f) {
        ReplayHeader h = ReplayClientEvents.header();
        if (h == null)
            return;
        int total = Math.max(1, h.durationTicks);
        if (ReplayClientEvents.isSeeking())
            renderLoading(f);
        int w = Math.min(380, f.layout.width() - 2 * HudLayout.MARGIN);
        int x = (f.layout.width() - w) / 2;
        int y = (SpectatorHud.isActive() ? SpectatorHud.bottom() : HudLayout.MARGIN) + HudLayout.GAP;
        Theme.panel(f.g, x, y, w, H, Theme.floating());
        f.zone(x, y, w, H);

        // play / pause
        Rect play = new Rect(x + PAD, y + (H - BUTTON) / 2, BUTTON, BUTTON);
        Theme.cell(f.g, play.x(), play.y(), play.w(), play.h(), Theme.CellState.READY, f.hovered(play));
        int px = play.x() + 5, py = play.y() + 3, colour = f.hovered(play) ? Theme.GOLD : Theme.TEXT;
        if (ReplayClientEvents.isPaused())
            for (int c = 0; c < 5; c++) // a triangle, one column at a time
                f.g.fill(px + c, py + c, px + c + 1, py + 9 - c, colour);
        else {
            f.g.fill(px - 1, py, px + 1, py + 9, colour);
            f.g.fill(px + 3, py, px + 5, py + 9, colour);
        }
        f.register(new ButtonBuilder("replay_play").onLeftClick(ReplayClientEvents::togglePause).build(), play, null,
                () -> Tooltip.of(I18n.get(ReplayClientEvents.isPaused() ? "replay.reignofnether.play" : "replay.reignofnether.pause")));

        // speed chips, right-aligned
        int cx = x + w - PAD;
        for (int i = ReplayClientEvents.SPEEDS.length - 1; i >= 0; i--) {
            float s = ReplayClientEvents.SPEEDS[i];
            String label = (s == (int) s ? String.valueOf((int) s) : String.valueOf(s)) + "x";
            int cw = Text.smallWidth(label) + 6;
            cx -= cw;
            Rect chip = new Rect(cx, y + (H - 12) / 2, cw, 12);
            boolean on = ReplayClientEvents.speedIndex() == i;
            Theme.chip(f.g, chip.x(), chip.y(), chip.w(), chip.h(), on, f.hovered(chip));
            Text.small(f.g, label, chip.x() + 3, chip.y() + 3, on ? Theme.GOLD : Theme.BODY);
            int index = i;
            f.register(new ButtonBuilder("replay_speed_" + i).onLeftClick(() -> ReplayClientEvents.setSpeedIndex(index)).build(),
                    chip, null, () -> Tooltip.of(I18n.get("replay.reignofnether.speed")));
            cx -= 2;
        }

        // clock
        int shown = ReplayClientEvents.tick();
        int timeX = play.right() + 6;
        String clock = TimeUtils.getTimeStrFromTicks(shown) + " / " + TimeUtils.getTimeStrFromTicks(total);
        int barX = timeX + Text.smallWidth(TimeUtils.getTimeStrFromTicks(total) + " / " + TimeUtils.getTimeStrFromTicks(total)) + 8;
        Rect bar = new Rect(barX, y + 4, cx - 6 - barX, H - 8);

        // the timeline, dragged with the left mouse button held
        boolean held = GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (dragging) {
            shown = tickAt(bar, f.mouseX, total);
            if (!held) {
                dragging = false;
                ReplayClientEvents.seek(shown);
            }
        }
        Text.small(f.g, clock, timeX, y + (H - 7) / 2, ReplayClientEvents.isSeeking() ? Theme.MUTED : Theme.TEXT);
        int trackY = bar.centerY() - 2;
        Theme.bar(f.g, bar.x(), trackY, bar.w(), 4, (float) shown / total, Theme.ORANGE);
        String hoveredMarker = null;
        for (ReplayHeader.Marker m : h.markers) {
            int mx = bar.x() + Math.round(bar.w() * Math.min(1f, (float) m.tick / total));
            f.g.fill(mx, bar.y() + 1, mx + 1, bar.bottom() - 1, m.type.equals("defeat") ? Theme.ERROR : Theme.GOLD);
            if (f.hovered(bar) && Math.abs(f.mouseX - mx) <= 2)
                hoveredMarker = TimeUtils.getTimeStrFromTicks(m.tick) + "  " + (m.type.equals("defeat")
                        ? I18n.get("replay.reignofnether.marker_defeat", m.name) : I18n.get("replay.reignofnether.marker_end"));
        }
        int head = bar.x() + Math.round(bar.w() * (float) shown / total);
        f.g.fill(head - 1, bar.y(), head + 2, bar.bottom(), Theme.GOLD);
        String hoverTip = hoveredMarker != null ? hoveredMarker : TimeUtils.getTimeStrFromTicks(tickAt(bar, f.mouseX, total));
        f.register(new ButtonBuilder("replay_timeline").onLeftClick(() -> dragging = true).build(), bar, null,
                () -> Tooltip.of(hoverTip));
    }

    // jumping redraws the map in big steps, so cover it until it lands
    private static void renderLoading(HudFrame f) {
        f.g.fill(0, 0, f.layout.width(), f.layout.height(), 0xC0000000);
        int w = 160, ph = 34, x = (f.layout.width() - w) / 2, y = (f.layout.height() - ph) / 2;
        Theme.panel(f.g, x, y, w, ph, Theme.floating());
        float p = ReplayClientEvents.seekProgress();
        Text.smallCentered(f.g, I18n.get("replay.reignofnether.seeking", Math.round(p * 100)), x + w / 2, y + 8, Theme.TEXT);
        Theme.bar(f.g, x + 10, y + 21, w - 20, 4, p, Theme.ORANGE);
    }

    private static int tickAt(Rect bar, int mouseX, int total) {
        float t = (mouseX - bar.x()) / (float) Math.max(1, bar.w());
        return Math.round(Math.max(0, Math.min(1, t)) * total);
    }
}
