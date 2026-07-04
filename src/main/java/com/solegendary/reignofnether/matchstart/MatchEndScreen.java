package com.solegendary.reignofnether.matchstart;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.player.MatchStatsClientboundPacket.MatchStatRow;
import com.solegendary.reignofnether.player.RTSPlayerScoresEnum;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// End-of-match stats popup. A compact, centered panel (battlefield stays visible behind
// it) that groups players by team into WINNER / LOSER sections, showing each player's
// cumulative match totals plus a per-team party total. Opened by MatchEndClientEvents.
public class MatchEndScreen extends Screen {

    // score array indices (order of RTSPlayerScoresEnum.values())
    private static final int SCORE_BUILDINGS = RTSPlayerScoresEnum.TOTAL_BUILDINGS_CONSTRUCTED.ordinal();
    private static final int SCORE_UNITS     = RTSPlayerScoresEnum.TOTAL_UNITS_PRODUCED.ordinal();
    private static final int SCORE_MILITARY  = RTSPlayerScoresEnum.MILITARY_UNITS_PRODUCED.ordinal();
    private static final int SCORE_RESOURCES = RTSPlayerScoresEnum.TOTAL_RESOURCES_HARVESTED.ordinal();

    private static final int BG_PANEL    = 0x40000000; // light overlay so the dirt shows through
    private static final int BG_ROW_SELF = 0x40FFFFFF;
    private static final int ACCENT      = 0xFFE6C76A;
    private static final int WIN_COL     = 0xFF6CE26C;
    private static final int LOSE_COL    = 0xFFE05A5A;
    private static final int TEXT_NORMAL = 0xFFFFFFFF;
    private static final int TEXT_DIM    = 0xFFB0B8C0;
    private static final int DIVIDER     = 0x40FFFFFF;

    private static final int PANEL_W = 420;
    private static final int PAD = 12;
    private static final int HEADER_H = 30;
    private static final int TEAM_HEADER_H = 18;
    private static final int ROW_H = 20;
    private static final int LINE_H = 12;
    private static final int TEAM_GAP = 10;
    private static final int HEAD = 16;

    // column x-offsets (right edge) from the panel's left content edge
    private static final int COL_UNITS = 230;
    private static final int COL_MIL    = 300;
    private static final int COL_BLDG   = 380;

    private static class Team {
        boolean winner;
        final List<MatchStatRow> members = new ArrayList<>();
        long units, military, buildings, resources;
    }

    private final List<Team> teams = new ArrayList<>();
    private int panelL, panelT, panelW, panelH;

    public MatchEndScreen() {
        super(Component.translatable("matchend.reignofnether.title"));
        buildTeams();
    }

    private void buildTeams() {
        teams.clear();
        Map<Integer, Team> byTeamId = new LinkedHashMap<>();
        for (MatchStatRow row : MatchEndClientEvents.getRows()) {
            Team t = byTeamId.computeIfAbsent(row.teamId, k -> new Team());
            t.winner = row.winner; // all members of a team share a result
            t.members.add(row);
            t.units += row.scores[SCORE_UNITS];
            t.military += row.scores[SCORE_MILITARY];
            t.buildings += row.scores[SCORE_BUILDINGS];
            t.resources += row.scores[SCORE_RESOURCES];
        }
        // winners first
        for (Team t : byTeamId.values()) if (t.winner) teams.add(t);
        for (Team t : byTeamId.values()) if (!t.winner) teams.add(t);
    }

    @Override
    protected void init() {
        panelW = PANEL_W;
        panelH = PAD + HEADER_H + LINE_H; // header + column-header row
        for (Team t : teams)
            panelH += TEAM_HEADER_H + t.members.size() * ROW_H + LINE_H + LINE_H + TEAM_GAP;
        panelH += PAD;
        panelL = (this.width - panelW) / 2;
        panelT = (this.height - panelH) / 2;

        // [X] close button, top-right corner of the panel
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(panelL + panelW - 26, panelT + 5, 20, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // tiled dirt, but only inside the popup - the battlefield stays visible around it
        g.setColor(0.25F, 0.25F, 0.25F, 1.0F);
        g.blit(BACKGROUND_LOCATION, panelL, panelT, 0, panelL, panelT, panelW, panelH, 32, 32);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        MyRenderer.renderFrameWithBg(g, panelL, panelT, panelW, panelH, BG_PANEL);

        int cl = panelL + PAD;               // content left
        int cr = panelL + panelW - PAD;      // content right
        int y = panelT + PAD;

        // header: title + duration
        g.drawString(font, Component.translatable("matchend.reignofnether.title"), cl, y + 4, ACCENT, true);
        String dur = TimeUtils.getTimeStrFromTicks(MatchEndClientEvents.getGameDurationTicks());
        g.drawString(font, dur, cr - 34 - font.width(dur), y + 4, TEXT_DIM, true);
        y += HEADER_H - 6;
        g.fill(cl, y, cr, y + 1, DIVIDER);
        y += 4;

        // column headers
        drawColHeader(g, "matchend.reignofnether.col_units", cl + COL_UNITS, y);
        drawColHeader(g, "matchend.reignofnether.col_military", cl + COL_MIL, y);
        drawColHeader(g, "matchend.reignofnether.col_buildings", cl + COL_BLDG, y);
        y += LINE_H;

        Minecraft mc = Minecraft.getInstance();
        String localName = mc.player != null ? mc.player.getName().getString() : "";

        for (Team t : teams) {
            // team header: WINNER / LOSER
            Component label = Component.translatable(t.winner ? "matchend.reignofnether.winner" : "matchend.reignofnether.loser");
            g.drawString(font, label, cl, y + 4, t.winner ? WIN_COL : LOSE_COL, true);
            y += TEAM_HEADER_H;

            for (MatchStatRow row : t.members) {
                if (row.name.equals(localName))
                    g.fill(cl - 2, y - 1, cr + 2, y + HEAD + 1, BG_ROW_SELF);

                // player head
                ResourceLocation skin = MyRenderer.getPlayerSkinRl(row.name);
                g.blit(skin, cl, y, HEAD, HEAD, 8.0f, 8.0f, 8, 8, 64, 64);
                g.blit(skin, cl, y, HEAD, HEAD, 40.0f, 8.0f, 8, 8, 64, 64);

                // faction icon
                ResourceLocation fIcon = MiscUtil.getFactionIcon(row.faction);
                if (fIcon != null)
                    MyRenderer.renderIcon(g, fIcon, cl + HEAD + 4, y, HEAD);

                int textY = y + (HEAD - font.lineHeight) / 2;
                g.drawString(font, row.name, cl + HEAD + 4 + HEAD + 4, textY, TEXT_NORMAL, true);
                drawNum(g, row.scores[SCORE_UNITS], cl + COL_UNITS, textY, TEXT_NORMAL);
                drawNum(g, row.scores[SCORE_MILITARY], cl + COL_MIL, textY, TEXT_NORMAL);
                drawNum(g, row.scores[SCORE_BUILDINGS], cl + COL_BLDG, textY, TEXT_NORMAL);
                y += ROW_H;
            }

            // party total line
            String totalLabel = Component.translatable("matchend.reignofnether.party_total").getString();
            g.drawString(font, totalLabel, cl + HEAD + 4 + HEAD + 4, y, TEXT_DIM, true);
            drawNum(g, t.units, cl + COL_UNITS, y, ACCENT);
            drawNum(g, t.military, cl + COL_MIL, y, ACCENT);
            drawNum(g, t.buildings, cl + COL_BLDG, y, ACCENT);
            y += LINE_H;
            // party resource total
            String res = Component.translatable("matchend.reignofnether.resources", String.format("%,d", t.resources)).getString();
            g.drawString(font, res, cl + HEAD + 4 + HEAD + 4, y, TEXT_DIM, true);
            y += LINE_H + TEAM_GAP;
        }

        super.render(g, mouseX, mouseY, partialTick); // renders the [X] button
    }

    private void drawNum(GuiGraphics g, long value, int rightX, int y, int color) {
        String s = String.format("%,d", value);
        g.drawString(font, s, rightX - font.width(s), y, color, true);
    }

    private void drawColHeader(GuiGraphics g, String key, int rightX, int y) {
        String s = Component.translatable(key).getString();
        g.drawString(font, s, rightX - font.width(s), y, TEXT_DIM, true);
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
