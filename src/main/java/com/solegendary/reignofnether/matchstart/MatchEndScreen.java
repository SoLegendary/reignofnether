package com.solegendary.reignofnether.matchstart;

import com.solegendary.reignofnether.player.MatchStatsClientboundPacket.MatchStatRow;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.MatchStatsClientboundPacket;
import com.solegendary.reignofnether.player.RTSPlayerScoresEnum;
import com.solegendary.reignofnether.replay.ReplayClientEvents;
import com.solegendary.reignofnether.replay.ReplayHeader;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoClientEvents;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static com.solegendary.reignofnether.player.RTSPlayerScoresEnum.*;

// End-of-match results: VICTORY / DEFEAT over the launcher's result colours, every player's stats by team in
// three tabs, each player's army over the match, a few highlights and the replay download. Laid out in HUD
// units like ReplayBrowserScreen. Opened by MatchEndClientEvents.
public class MatchEndScreen extends Screen {

    private static final int PAD = 14, HEADER_H = 64, TABS_H = 18, COL_H = 14, TEAM_H = 14, ROW_H = 30;
    private static final int CHART_H = 76, FOOTER_H = 34, SIDE_W = 176, BUTTON_H = 22;

    // the launcher's result screen: a warm glow for a win, cold for a loss
    private static final int WIN_TOP = 0xE02A160C, WIN_BOTTOM = 0xF00D0305;
    private static final int LOSS_TOP = 0xE01A1A26, LOSS_BOTTOM = 0xF008070B;
    private static final int[] DEFEAT_BANDS = { Theme.TEXT, Theme.DESC, 0xFF8A5A6A, 0xFF6A2A2A };

    private enum Tab { OVERVIEW, ECONOMY, MILITARY }

    private record Hit(int x, int y, int w, int h, Runnable action) { }

    // one stats column: its header, the big number and an optional line under it
    private record Column(String key, ToIntFunction<MatchStatRow> value, @Nullable Function<MatchStatRow, String> sub) { }

    private static class Team {
        boolean winner;
        final List<MatchStatRow> members = new ArrayList<>();
    }

    private final List<Team> teams = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final String localName;
    @Nullable private final Boolean localWon; // null: watched, didn't play
    private Tab tab = Tab.OVERVIEW;

    public MatchEndScreen() {
        super(Component.translatable("matchend.reignofnether.title"));
        Minecraft mc = Minecraft.getInstance();
        localName = mc.player != null ? mc.player.getName().getString() : "";
        Boolean won = null;
        Map<Integer, Team> byTeamId = new LinkedHashMap<>();
        for (MatchStatRow row : MatchEndClientEvents.getRows()) {
            Team t = byTeamId.computeIfAbsent(row.teamId, k -> new Team());
            t.winner = row.winner; // all members of a team share a result
            t.members.add(row);
            if (row.name.equals(localName))
                won = row.winner;
        }
        localWon = won;
        // winners first
        for (Team t : byTeamId.values()) if (t.winner) teams.add(t);
        for (Team t : byTeamId.values()) if (!t.winner) teams.add(t);
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static String num(long n) {
        return String.format("%,d", n);
    }

    private static int minutes() {
        return Math.max(1, (int) (MatchEndClientEvents.getGameDurationTicks() / 1200));
    }

    private static List<Column> columns(Tab tab) {
        return switch (tab) {
            case OVERVIEW -> List.of(
                    new Column("matchend.reignofnether.col_units", r -> r.score(TOTAL_UNITS_PRODUCED),
                            r -> tr("matchend.reignofnether.workers_army", r.score(WORKER_UNITS_PRODUCED), r.score(MILITARY_UNITS_PRODUCED))),
                    new Column("matchend.reignofnether.col_kills", r -> r.score(UNITS_KILLED),
                            r -> tr("matchend.reignofnether.lost", r.score(UNITS_LOST))),
                    new Column("matchend.reignofnether.col_buildings", r -> r.score(TOTAL_BUILDINGS_CONSTRUCTED),
                            r -> tr("matchend.reignofnether.destroyed", r.score(BUILDINGS_DESTROYED))),
                    new Column("matchend.reignofnether.col_resources", r -> r.score(TOTAL_RESOURCES_HARVESTED), null));
            case ECONOMY -> List.of(
                    new Column("matchend.reignofnether.col_food", r -> r.score(FOOD_HARVESTED), null),
                    new Column("matchend.reignofnether.col_wood", r -> r.score(WOOD_HARVESTED), null),
                    new Column("matchend.reignofnether.col_ore", r -> r.score(ORES_HARVESTED), null),
                    new Column("matchend.reignofnether.col_workers", r -> r.score(WORKER_UNITS_PRODUCED), null),
                    new Column("matchend.reignofnether.col_per_min", r -> r.score(TOTAL_RESOURCES_HARVESTED) / minutes(), null));
            case MILITARY -> List.of(
                    new Column("matchend.reignofnether.col_army", r -> r.score(MILITARY_UNITS_PRODUCED), null),
                    new Column("matchend.reignofnether.col_kills", r -> r.score(UNITS_KILLED), null),
                    new Column("matchend.reignofnether.col_lost", r -> r.score(UNITS_LOST), null),
                    new Column("matchend.reignofnether.col_peak", r -> r.score(PEAK_ARMY), null),
                    new Column("matchend.reignofnether.col_razed", r -> r.score(BUILDINGS_DESTROYED), null));
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean win = Boolean.TRUE.equals(localWon);
        g.fillGradient(0, 0, width, height, win ? WIN_TOP : LOSS_TOP, win ? WIN_BOTTOM : LOSS_BOTTOM);

        float k = Text.guiPerUnit();
        int screenW = Math.round(width / k), screenH = Math.round(height / k);
        int mx = Math.round(mouseX / k), my = Math.round(mouseY / k);
        hits.clear();

        int rowsH = 0;
        for (Team t : teams)
            rowsH += TEAM_H + t.members.size() * (ROW_H + 3) + 6;
        int w = Math.min(760, screenW - 24);
        int bodyH = TABS_H + 8 + COL_H + rowsH;
        boolean chart = HEADER_H + bodyH + CHART_H + 8 + FOOTER_H + 2 * PAD <= screenH - 16; // dropped when it can't fit
        if (chart)
            bodyH += CHART_H + 8;
        bodyH = Math.max(bodyH, 4 * 34 + 70);
        int h = HEADER_H + PAD + bodyH + PAD + FOOTER_H;
        int x = (screenW - w) / 2, y = Math.max(8, (screenH - h) / 2);

        g.pose().pushPose();
        g.pose().scale(k, k, 1);
        Theme.popup(g, x, y, w, h, win ? Theme.ORANGE : Theme.FAINT);
        header(g, x, y, w);

        int bx = x + PAD, by = y + HEADER_H + PAD, mainW = w - 3 * PAD - SIDE_W;
        tabs(g, bx, by, mx, my);
        int ty = table(g, bx, by + TABS_H + 8, mainW);
        if (chart)
            chart(g, bx, ty + 8, mainW, CHART_H);
        sidebar(g, x + w - PAD - SIDE_W, by, SIDE_W, bodyH, mx, my);

        int fy = y + h - FOOTER_H;
        g.fill(x + 1, fy, x + w - 1, fy + 1, Theme.INK);
        Text.small(g, tr("matchend.reignofnether.esc_hint"), bx, fy + (FOOTER_H - 7) / 2, Theme.MUTED);
        String close = tr("matchend.reignofnether.view_battlefield");
        int cw = Text.mediumWidth(close) + 24;
        button(g, close, x + w - PAD - cw, fy + (FOOTER_H - BUTTON_H) / 2, cw, BUTTON_H, false, true, mx, my, this::onClose);
        g.pose().popPose();
    }

    private void header(GuiGraphics g, int x, int y, int w) {
        int tx = x + PAD, ty = y + PAD - 2;
        String mode = teams.size() == 2 ? teams.get(0).members.size() + "V" + teams.get(1).members.size() : "FFA";
        String kicker = tr("matchend.reignofnether.kicker", mode)
                + (RTSMapInfoClientEvents.mapName.isBlank() ? "" : " · " + RTSMapInfoClientEvents.mapName.toUpperCase());
        Text.small(g, kicker, tx, ty, Theme.LABEL);
        if (localWon == null)
            Text.title(g, title.getString().toUpperCase(), tx, ty + 11, 2.5f);
        else if (localWon)
            Text.title(g, tr("matchend.reignofnether.victory"), tx, ty + 11, 3f);
        else
            Text.title(g, tr("matchend.reignofnether.defeat"), tx, ty + 11, 3f, DEFEAT_BANDS, 0xFF000000);
        List<String> winners = new ArrayList<>();
        for (Team t : teams)
            if (t.winner)
                t.members.forEach(m -> winners.add(m.name));
        String duration = TimeUtils.getTimeStrFromTicks(MatchEndClientEvents.getGameDurationTicks());
        if (!winners.isEmpty())
            Text.small(g, tr("matchend.reignofnether.won_after", String.join(", ", winners), duration), tx, ty + 11 + Text.titleHeight(3f) + 4, Theme.BODY);

        // match-wide numbers, right-aligned
        long kills = 0, razed = 0;
        for (Team t : teams)
            for (MatchStatRow r : t.members) {
                kills += r.score(UNITS_KILLED);
                razed += r.score(BUILDINGS_DESTROYED);
            }
        String[][] stats = {
                { tr("matchend.reignofnether.duration"), duration },
                { tr("matchend.reignofnether.col_kills"), num(kills) },
                { tr("matchend.reignofnether.col_razed"), num(razed) } };
        int cx = x + w - PAD;
        for (int i = stats.length - 1; i >= 0; i--) {
            int cw = Math.max(64, Math.max(Text.smallWidth(stats[i][0]), Text.mediumWidth(stats[i][1])) + 16);
            cx -= cw;
            Theme.card(g, cx, y + PAD, cw, 32);
            Text.small(g, stats[i][0].toUpperCase(), cx + 8, y + PAD + 6, Theme.LABEL);
            Text.medium(g, stats[i][1], cx + 8, y + PAD + 18, Theme.TEXT);
            cx -= 6;
        }
        g.fill(x + 1, y + HEADER_H, x + w - 1, y + HEADER_H + 1, Theme.INK);
    }

    private void tabs(GuiGraphics g, int x, int y, int mx, int my) {
        for (Tab t : Tab.values()) {
            String label = tr("matchend.reignofnether.tab_" + t.name().toLowerCase());
            int tw = Text.mediumWidth(label) + 16;
            boolean hov = mx >= x && mx < x + tw && my >= y && my < y + TABS_H;
            Theme.chip(g, x, y, tw, TABS_H, t == tab, hov);
            Text.mediumCentered(g, label, x + tw / 2, y + (TABS_H - 8) / 2, t == tab ? Theme.GOLD : hov ? Theme.TEXT : Theme.BODY);
            hits.add(new Hit(x, y, tw, TABS_H, () -> tab = t));
            x += tw + 4;
        }
    }

    // @return the bottom of the table
    private int table(GuiGraphics g, int x, int y, int w) {
        List<Column> cols = columns(tab);
        int nameW = w * 2 / 5, colW = (w - nameW - 16) / cols.size();
        for (int i = 0; i < cols.size(); i++)
            Text.smallRight(g, tr(cols.get(i).key()).toUpperCase(), x + nameW + 8 + (i + 1) * colW - 8, y, Theme.LABEL);
        y += COL_H;
        for (Team t : teams) {
            int colour = 0xFF000000 | PlayerColors.getPlayerColorHex(t.members.get(0).name);
            g.fill(x, y + 2, x + 6, y + 8, colour);
            Text.small(g, tr(t.winner ? "matchend.reignofnether.winner" : "matchend.reignofnether.loser"), x + 10, y + 2,
                    t.winner ? Theme.GOLD : Theme.ERROR);
            y += TEAM_H;
            for (MatchStatRow r : t.members) {
                row(g, r, colour, cols, x, y, w, nameW, colW);
                y += ROW_H + 3;
            }
            y += 6;
        }
        return y;
    }

    private void row(GuiGraphics g, MatchStatRow r, int colour, List<Column> cols, int x, int y, int w, int nameW, int colW) {
        boolean self = r.name.equals(localName);
        Theme.card(g, x, y, w, ROW_H);
        if (self)
            g.fill(x + 1, y + 1, x + w - 1, y + ROW_H - 1, Theme.SECONDARY);
        g.fill(x, y, x + 3, y + ROW_H, colour);

        ResourceLocation skin = MyRenderer.getPlayerSkinRl(r.name);
        int hy = y + (ROW_H - 16) / 2;
        g.blit(skin, x + 8, hy, 16, 16, 8.0f, 8.0f, 8, 8, 64, 64);
        g.blit(skin, x + 8, hy, 16, 16, 40.0f, 8.0f, 8, 8, 64, 64);
        if (r.faction.icon != null)
            MyRenderer.renderIcon(g, r.faction.icon, x + 28, hy + 2, 12);
        Text.draw(g, Text.fit(r.name, nameW - 64), x + 44, y + 7, Theme.TEXT);
        if (self)
            Text.small(g, tr("matchend.reignofnether.you"), x + 44, y + 18, Theme.GOLD);

        for (int i = 0; i < cols.size(); i++) {
            Column c = cols.get(i);
            int right = x + nameW + 8 + (i + 1) * colW - 8;
            int v = c.value().applyAsInt(r);
            if (c.sub() != null) {
                Text.mediumRight(g, num(v), right, y + 6, Theme.TEXT);
                Text.smallRight(g, c.sub().apply(r), right, y + 18, Theme.MUTED);
            } else {
                Text.mediumRight(g, num(v), right, y + (ROW_H - 8) / 2 - (tab == Tab.OVERVIEW ? 4 : 0), Theme.TEXT);
            }
            if (tab == Tab.OVERVIEW && c.sub() == null) // resources: food / wood / ore split
                splitBar(g, right - colW + 12, y + 20, colW - 12, r);
        }
    }

    private static void splitBar(GuiGraphics g, int x, int y, int w, MatchStatRow r) {
        int food = r.score(FOOD_HARVESTED), wood = r.score(WOOD_HARVESTED), ore = r.score(ORES_HARVESTED);
        int total = food + wood + ore;
        g.fill(x, y, x + w, y + 3, Theme.INK);
        if (total <= 0)
            return;
        int fw = w * food / total, ww = w * wood / total;
        g.fill(x, y, x + fw, y + 3, Theme.FOOD);
        g.fill(x + fw, y, x + fw + ww, y + 3, Theme.WOOD);
        g.fill(x + fw + ww, y, x + w, y + 3, Theme.ORE);
    }

    // every player's army size over the match, as a stepped line in their colour
    private void chart(GuiGraphics g, int x, int y, int w, int h) {
        Theme.card(g, x, y, w, h);
        Text.small(g, tr("matchend.reignofnether.army_chart"), x + 8, y + 6, Theme.LABEL);
        int px = x + 8, py = y + 18, pw = w - 16, ph = h - 34;
        g.fill(px, py + ph, px + pw, py + ph + 1, Theme.FAINT);
        int peak = 1, samples = 1;
        for (Team t : teams)
            for (MatchStatRow r : t.members) {
                samples = Math.max(samples, r.army.length);
                for (int a : r.army)
                    peak = Math.max(peak, a);
            }
        int lx = x + w - 8;
        for (Team t : teams)
            for (MatchStatRow r : t.members) {
                int colour = 0xFF000000 | PlayerColors.getPlayerColorHex(r.name);
                int prevY = py + ph;
                for (int i = 0; i < r.army.length; i++) {
                    int x0 = px + pw * i / samples, x1 = px + pw * (i + 1) / samples;
                    int sy = py + ph - ph * r.army[i] / peak;
                    g.fill(x0, Math.min(prevY, sy), x0 + 1, Math.max(prevY, sy) + 1, colour);
                    g.fill(x0, sy, x1, sy + 1, colour);
                    prevY = sy;
                }
                lx -= Text.smallWidth(r.name);
                Text.small(g, r.name, lx, y + 6, colour);
                lx -= 10;
            }
        int axisY = py + ph + 4;
        Text.small(g, "0:00", px, axisY, Theme.MUTED);
        long ticks = (long) samples * MatchStatsClientboundPacket.ARMY_SAMPLE_TICKS;
        Text.smallCentered(g, TimeUtils.getTimeStrFromTicks(ticks / 2), px + pw / 2, axisY, Theme.MUTED);
        Text.smallRight(g, TimeUtils.getTimeStrFromTicks(MatchEndClientEvents.getGameDurationTicks()), px + pw, axisY, Theme.MUTED);
    }

    private void sidebar(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        Text.small(g, tr("matchend.reignofnether.highlights"), x, y + 4, Theme.LABEL);
        int cy = y + 16;
        cy = highlight(g, x, cy, w, "matchend.reignofnether.best_economy", Theme.FOOD,
                r -> r.score(TOTAL_RESOURCES_HARVESTED) / minutes(), "matchend.reignofnether.per_min");
        cy = highlight(g, x, cy, w, "matchend.reignofnether.most_kills", Theme.ERROR,
                r -> r.score(UNITS_KILLED), "matchend.reignofnether.units");
        highlight(g, x, cy, w, "matchend.reignofnether.master_builder", Theme.ORE,
                r -> r.score(TOTAL_BUILDINGS_CONSTRUCTED), "matchend.reignofnether.buildings");

        if (ReplayClientEvents.isActive()) // watching a replay: nothing to download
            return;
        int rh = 66, ry = y + h - rh;
        Theme.card(g, x, ry, w, rh);
        Text.small(g, tr("matchend.reignofnether.replay"), x + 8, ry + 8, Theme.LABEL);
        Text.smallRight(g, "v" + ReplayHeader.modVersion(), x + w - 8, ry + 8, Theme.MUTED);
        ReplayClientEvents.Download state = ReplayClientEvents.download();
        boolean busy = state == ReplayClientEvents.Download.WAITING || state == ReplayClientEvents.Download.DOWNLOADING;
        button(g, replayLabel(state), x + 8, ry + 22, w - 16, BUTTON_H + 4, state != ReplayClientEvents.Download.DONE, !busy, mx, my, () -> {
            if (ReplayClientEvents.download() == ReplayClientEvents.Download.DONE)
                ReplayClientEvents.openReplaysFolder();
            else
                ReplayClientEvents.requestDownload();
        });
        if (state == ReplayClientEvents.Download.DOWNLOADING)
            Theme.bar(g, x + 8, ry + 54, w - 16, 3, ReplayClientEvents.downloadProgress(), Theme.ORANGE);
    }

    // the player with the most of something; @return the y under the card
    private int highlight(GuiGraphics g, int x, int y, int w, String key, int accent, ToIntFunction<MatchStatRow> value, String unitKey) {
        MatchStatRow best = null;
        for (Team t : teams)
            for (MatchStatRow r : t.members)
                if (best == null || value.applyAsInt(r) > value.applyAsInt(best))
                    best = r;
        if (best == null || value.applyAsInt(best) <= 0)
            return y;
        Theme.card(g, x, y, w, 30);
        g.fill(x + 1, y + 1, x + 3, y + 29, accent);
        Text.small(g, tr(key), x + 10, y + 6, Theme.MUTED);
        Text.draw(g, Text.fit(best.name + " · " + tr(unitKey, num(value.applyAsInt(best))), w - 18), x + 10, y + 17, Theme.TEXT);
        return y + 34;
    }

    private static String replayLabel(ReplayClientEvents.Download state) {
        return switch (state) {
            case NONE -> tr("replay.reignofnether.download");
            case WAITING -> tr("replay.reignofnether.download_waiting");
            case DOWNLOADING -> tr("replay.reignofnether.downloading", Math.round(ReplayClientEvents.downloadProgress() * 100));
            case DONE -> tr("replay.reignofnether.download_done");
            case FAILED -> tr("replay.reignofnether.download_failed");
        };
    }

    private void button(GuiGraphics g, String label, int x, int y, int w, int h, boolean primary, boolean clickable,
                        int mx, int my, Runnable action) {
        boolean hov = clickable && mx >= x && mx < x + w && my >= y && my < y + h;
        if (primary)
            Theme.primaryButton(g, x, y, w, h, hov);
        else
            Theme.secondaryButton(g, x, y, w, h, hov);
        Text.mediumCentered(g, Text.fit(label, w - 8), x + w / 2, y + (h - 8) / 2,
                primary ? Theme.ON_ORANGE : hov ? Theme.GOLD : clickable ? Theme.BODY : Theme.MUTED);
        if (clickable)
            hits.add(new Hit(x, y, w, h, action));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float k = Text.guiPerUnit();
        double mx = mouseX / k, my = mouseY / k;
        if (button == 0)
            for (Hit h : List.copyOf(hits))
                if (mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h) {
                    h.action.run();
                    return true;
                }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        MatchEndClientEvents.dismiss();
    }
}
