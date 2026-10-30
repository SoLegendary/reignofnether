package com.solegendary.reignofnether.matchstart;

import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.p2p.HostPopup;
import com.solegendary.reignofnether.p2p.P2PHost;
import com.solegendary.reignofnether.ui.HudScale;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.startpos.StartPos;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shown when a saved game is reopened mid-match, styled like the match setup screen: the same header, the roster
 * as teams of seat rows (read-only, from the saved players) and the pick card turned into a RESUME card.
 * It is a pause screen, so in singleplayer the world stays frozen until the host clicks Resume.
 *
 * <pre>
 *  SAVED GAME · PAUSED                                     MATCH TIME 12:34   [SAVE & QUIT]
 *  MAP NAME
 *  +-- the map now, bases --+   +-- roster (teams, online / offline) ------------------+
 *  |                        |   |                                                       |
 *  |                        |   +-- paused card: fog note, offline players, RESUME ----+
 * </pre>
 */
public class MatchResumeScreen extends Screen {

    // the match setup screen's measurements
    private static final int M = 14, G = 10, ROW_H = 36, ROW_PAD = 7, HEAD = 24;
    private static final int PANEL_FILL = 0xE016151B;

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final String mapName;
    private final long gameTicks;
    private final boolean host;
    private final List<MatchResumeClientboundPacket.Row> rows;
    private final boolean fog;
    private final List<Hit> hits = new ArrayList<>();
    private HudFrame frame; // only for the host popup, which is shared with the setup screen
    // the minimap as it was before this screen (or the one it refreshes), restored when it closes
    private final boolean priorLargeMap, priorMapLocked;

    public MatchResumeScreen(String mapName, long gameTicks, boolean host, List<MatchResumeClientboundPacket.Row> rows, boolean fog) {
        super(Component.literal(mapName));
        this.fog = fog;
        boolean refresh = Minecraft.getInstance().screen instanceof MatchResumeScreen;
        MatchResumeScreen old = refresh ? (MatchResumeScreen) Minecraft.getInstance().screen : null;
        this.priorLargeMap = old != null ? old.priorLargeMap : MinimapClientEvents.isLargeMap();
        this.priorMapLocked = old != null ? old.priorMapLocked : MinimapClientEvents.isMapLocked();
        this.mapName = mapName;
        this.gameTicks = gameTicks;
        this.host = host;
        this.rows = rows;
    }

    static void show(boolean open, String mapName, long gameTicks, boolean host, List<MatchResumeClientboundPacket.Row> rows, boolean fog) {
        Minecraft mc = Minecraft.getInstance();
        if (open)
            mc.setScreen(new MatchResumeScreen(mapName, gameTicks, host, rows, fog));
        else if (mc.screen instanceof MatchResumeScreen)
            mc.setScreen(null);
    }

    @Override
    protected void init() {
        super.init();
        // the large minimap, centred on the play area, like the match setup screen
        MinimapClientEvents.setLargeMap(true);
        MinimapClientEvents.suppressViewQuad = true;
        var level = Minecraft.getInstance().level;
        if (level != null) {
            var border = level.getWorldBorder();
            MinimapClientEvents.forceMapCentre((int) border.getCenterX(), (int) border.getCenterZ());
            MinimapClientEvents.setMapLocked(true);
        }
    }

    @Override
    public void tick() {
        MinimapClientEvents.tickForScreen(); // screens still tick while the game is paused
    }

    @Override
    public void removed() {
        MinimapClientEvents.suppressViewQuad = false;
        MinimapClientEvents.setMapLocked(priorMapLocked);
        MinimapClientEvents.setLargeMap(priorLargeMap);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // the match only goes on through Resume
    }

    // ------------------------------------------------------------------ helpers

    private static Faction factionOf(String key) {
        try {
            return Factions.getFaction(ResourceLocation.parse(key));
        } catch (Exception e) {
            return null;
        }
    }

    /** Seat colour, with the setup screen's brighter red and blue. */
    private static int seatColour(int colorId) {
        int hex = new StartPos(BlockPos.ZERO, colorId).getHexColor();
        if (hex == 0xA12722)
            hex = 0xD8443A;
        else if (hex == 0x35399D)
            hex = 0x3AAFD9;
        return 0xFF000000 | hex;
    }

    /** Players grouped into teams by alliance, in roster order. */
    private Map<Integer, List<MatchResumeClientboundPacket.Row>> teams() {
        Map<String, Integer> teamOf = new LinkedHashMap<>();
        Map<Integer, List<MatchResumeClientboundPacket.Row>> teams = new LinkedHashMap<>();
        int next = 0;
        for (MatchResumeClientboundPacket.Row r : rows) {
            Integer team = teamOf.get(r.name());
            if (team == null)
                for (String ally : Arrays.asList(r.allies().split(",\\s*")))
                    if (teamOf.containsKey(ally))
                        team = teamOf.get(ally);
            if (team == null)
                team = next++;
            teamOf.put(r.name(), team);
            teams.computeIfAbsent(team, k -> new ArrayList<>()).add(r);
        }
        return teams;
    }

    /** "Oceanborn · 26 Oct 21:40" -> "Oceanborn": a saved game is named after its map. */
    private String map() {
        int dot = mapName.lastIndexOf(" · ");
        return dot > 0 ? mapName.substring(0, dot) : mapName;
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        float k = Text.guiPerUnit();
        int w = Math.round(width / k), h = Math.round(height / k);
        int mx = Math.round(mouseX / k), my = Math.round(mouseY / k);
        g.pose().pushPose();
        g.pose().scale(k, k, 1);

        // the world stays visible under the setup screen's nether-red wash
        g.fillGradient(0, 0, w, h, 0xA62A0806, 0xCC140303);

        int headerH = 38;
        renderHeader(g, w, mx, my);

        // one block: the map panel (a square map), and beside it the roster above the paused card, as tall as it
        int bodyTop = M + headerH + G;
        int bodyH = h - M - bodyTop;
        int blockW = Math.min(w - 2 * M, 760);
        int mapSide = Math.max(120, Math.min(blockW * 48 / 100 - 16, bodyH - 32));
        int leftW = mapSide + 16, panelH = mapSide + 32;
        int bx = (w - blockW) / 2, by = bodyTop + Math.max(0, (bodyH - panelH) / 2);
        renderMap(g, bx, by, leftW, panelH);
        int rx = bx + leftW + G, rw = blockW - leftW - G;
        int cardH = 104;
        renderRoster(g, rx, by, rw, panelH - cardH - G);
        renderCard(g, rx, by + panelH - cardH, rw, cardH, mx, my);

        HudScale scale = HudGeometry.scale();
        frame = new HudFrame(g, scale, HudGeometry.layout(), scale.toUnits(mouseX), scale.toUnits(mouseY));
        if (HostPopup.open)
            HostPopup.render(frame, w, h);

        g.pose().popPose();
    }

    private void renderHeader(GuiGraphics g, int w, int mx, int my) {
        Text.medium(g, "SAVED GAME · PAUSED", M, M - 2, Theme.ORANGE);
        float titleScale = Text.px(48);
        String title = map().toUpperCase(Locale.ROOT);
        Text.title(g, Text.fitBig(title, w / 2, titleScale), M, M + 9, titleScale);

        // right side: match time, save & quit
        int bh = 22, by = M + 6;
        String quit = "SAVE & QUIT";
        int qw = Text.width(quit) + 18;
        int qx = w - M - qw;
        boolean hq = over(mx, my, qx, by, qw, bh);
        Theme.secondaryButton(g, qx, by, qw, bh, hq);
        Text.draw(g, quit, qx + 9, by + (bh - 8) / 2 + 1, hq ? Theme.GOLD : Theme.BODY);
        hits.add(new Hit(qx, by, qw, bh, this::saveAndQuit));

        // our own world: let players elsewhere join by code, same popup as the setup screen
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            String host = I18n.get(P2PHost.state == P2PHost.State.OFF ? "matchstart.reignofnether.host_online" : "matchstart.reignofnether.invite");
            int hw = Text.width(host) + 12 + 5 + 18;
            int hx = qx - 6 - hw;
            boolean hh = !HostPopup.open && over(mx, my, hx, by, hw, bh);
            Theme.secondaryButton(g, hx, by, hw, bh, hh);
            Icon.USERS.draw(g, hx + 9, by + (bh - 12) / 2, 12, hh ? Theme.GOLD : Theme.BODY);
            Text.draw(g, host, hx + 9 + 12 + 5, by + (bh - 8) / 2 + 1, hh ? Theme.GOLD : Theme.BODY);
            hits.add(new Hit(hx, by, hw, bh, () -> HostPopup.open = true));
            qx = hx;
        }

        long seconds = gameTicks / 20;
        String time = String.format("%d:%02d", seconds / 60, seconds % 60);
        float timeScale = Text.px(26);
        int rx = qx - 12;
        Text.smallRight(g, "MATCH TIME", rx, by - 2, Theme.LABEL);
        Text.bigRight(g, time, rx, by + 8, Theme.TEXT, timeScale);
    }

    /** The map as it is now, with each player's base (their capitol) as a seat-style marker. */
    private void renderMap(GuiGraphics g, int px, int py, int pw, int ph) {
        Theme.panel(g, px, py, pw, ph, PANEL_FILL);
        Text.small(g, "THE MAP NOW", px + 11, py + 9, Theme.LABEL);
        if (fog)
            Text.smallRight(g, "ALLIED BASES ONLY \u00b7 FOG OF WAR", px + pw - 11, py + 9, Theme.DIM);
        int top = py + 24;
        int side = Math.max(40, Math.min(pw - 16, py + ph - top - 8));
        int mx = px + (pw - side) / 2, my = top;
        Theme.outline(g, mx - 2, my - 2, side + 4, side + 4, 1, Theme.INK);
        Theme.outline(g, mx, my, side, side, 1, Theme.CONSOLE_RIM);
        if (!MinimapClientEvents.isMapReady()) {
            g.fill(mx, my, mx + side, my + side, Theme.EMPTY);
            Text.drawCentered(g, "Loading map\u2026", mx + side / 2, my + side / 2 - 4, Theme.DIM);
            return;
        }
        MinimapClientEvents.renderMapInto(g, mx, my, side, side);
        g.fill(mx, my, mx + side, my + side, 0x33000000);
        String me = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getName().getString() : "";
        int marker = Math.max(18, Math.min(40, side * 12 / 100));
        for (MatchResumeClientboundPacket.Row r : rows) {
            if (r.baseX() == MatchResumeClientboundPacket.NO_BASE)
                continue;
            Vector2f c = MinimapClientEvents.worldToRect(r.baseX(), r.baseZ(), mx, my, side, side);
            int x = (int) c.x - marker / 2, y = (int) c.y - marker / 2;
            if (r.name().equals(me))
                Theme.outline(g, x - 2, y - 2, marker + 4, marker + 4, 2, Theme.ORANGE);
            Theme.outline(g, x, y, marker, marker, 1, Theme.INK);
            g.fill(x, y, x + marker, y + marker, seatColour(r.colorId()));
            g.fill(x, y, x + marker, y + 2, 0x4DFFFFFF);
            g.fill(x, y + marker - 2, x + marker, y + marker, 0x4D000000);
            int inner = marker * 2 / 3;
            Faction f = factionOf(r.faction());
            seatIcon(g, r.name(), r.bot(), f, x + (marker - inner) / 2, y + (marker - inner) / 2, inner);
            if (!r.online())
                g.fill(x, y, x + marker, y + marker, 0x99121118);
        }
    }

    private void renderRoster(GuiGraphics g, int px, int py, int pw, int ph) {
        Theme.panel(g, px, py, pw, ph, PANEL_FILL);
        Text.small(g, "PLAYERS", px + 11, py + 9, Theme.LABEL);
        Text.smallRight(g, rows.size() + (rows.size() == 1 ? " PLAYER" : " PLAYERS"), px + pw - 11, py + 9, Theme.DIM);

        int x = px + 8, y = py + 24, w = pw - 16;
        String me = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getName().getString() : "";
        int teamIndex = 1;
        for (List<MatchResumeClientboundPacket.Row> team : teams().values()) {
            // team header: colour square, TEAM n, count
            int colour = seatColour(team.get(0).colorId());
            Theme.outline(g, x, y + 1, 7, 7, 1, Theme.INK);
            g.fill(x, y + 1, x + 7, y + 8, colour);
            String name = "Team " + teamIndex++;
            Text.big(g, name, x + 12, y, Theme.TEXT, Text.px(18));
            Text.small(g, (team.size() + (team.size() == 1 ? " PLAYER" : " PLAYERS")), x + 12 + Text.bigWidth(name, Text.px(18)) + 8, y, Theme.DIM);
            y += 12;
            for (MatchResumeClientboundPacket.Row r : team) {
                if (y + ROW_H > py + ph - 4)
                    break; // ponytail: no scrolling, a roster longer than the panel is cut
                renderRow(g, r, r.name().equals(me), x, y, w);
                y += ROW_H + 3;
            }
            y += 4;
        }
    }

    /** A seat row like the setup screen's: team strip, head, name, faction; read-only. */
    private void renderRow(GuiGraphics g, MatchResumeClientboundPacket.Row r, boolean mine, int x, int y, int w) {
        Theme.bevel(g, x, y, w, ROW_H, mine ? Theme.ACTIVE : Theme.RAISED, mine ? 0 : 0x0AFFFFFF, 0, 1, Theme.INK);
        if (mine)
            Theme.ring(g, x, y, w, ROW_H, 1, Theme.ORANGE);
        g.fill(x + ROW_PAD, y + 5, x + ROW_PAD + 3, y + ROW_H - 5, seatColour(r.colorId()));
        int hx = x + ROW_PAD + 3 + 8, hy = y + (ROW_H - HEAD) / 2;
        Theme.outline(g, hx, hy, HEAD, HEAD, 1, Theme.INK);
        g.fill(hx, hy, hx + HEAD, hy + HEAD, Theme.EMPTY);
        Faction faction = factionOf(r.faction());
        seatIcon(g, r.name(), r.bot(), faction, hx + 2, hy + 2, HEAD - 4);

        int right = x + w - ROW_PAD;
        if (!r.bot()) {
            Text.mediumRight(g, r.online() ? "ONLINE" : "OFFLINE", right, y + (ROW_H - 8) / 2, r.online() ? Theme.OK : Theme.ORANGE);
            right -= 52 + 8;
        }
        if (faction != null) {
            String fLabel = MiscUtil.getFactionName(faction);
            int fw = Text.smallWidth(fLabel) + 19;
            int fx = right - fw;
            MyRenderer.renderIcon(g, faction.icon, fx, y + (ROW_H - 14) / 2, 14);
            Text.small(g, fLabel, fx + 19, y + (ROW_H - 8) / 2, Theme.BODY);
            right = fx - 8;
        }
        int nx = hx + HEAD + 8;
        String name = mine ? r.name() + " (you)" : r.name();
        Text.draw(g, Text.fit(name, Math.max(10, right - nx)), nx, y + 8, mine ? Theme.GOLD : Theme.TEXT);
        String sub = r.bot() ? "BOT" : "PLAYER";
        Text.small(g, sub, nx, y + 19, Theme.DIM);
        if (!r.online())
            g.fill(x, y, x + w, y + ROW_H, 0x66121118);
    }

    /** The setup screen's pick card, as the paused card: what happens, then RESUME (host) or waiting. */
    private void renderCard(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my) {
        Theme.outline(g, px, py, pw, ph, 1, Theme.INK);
        g.fill(px, py, px + pw, py + ph, Theme.PANEL);
        g.fillGradient(px, py, px + pw, py + ph, 0x3316151B, 0xF516151B);
        g.fill(px, py, px + pw, py + Theme.BEVEL, Theme.ORANGE);

        int pad = 11, btnH = 26;
        int btnY = py + ph - pad - btnH - 3;
        List<String> offline = new ArrayList<>();
        for (MatchResumeClientboundPacket.Row r : rows)
            if (!r.online())
                offline.add(r.name());
        String desc = host
                ? "The match is frozen where you left it: units, buildings, resources and alliances are as they were."
                : "The match is frozen where it was left. The host resumes it.";
        if (!offline.isEmpty())
            desc += " Offline: " + String.join(", ", offline) + ". They can rejoin at any time; until then their units stand idle.";
        if (fog)
            desc += " Fog of war is back on; enemy buildings you had scouted stay hidden until you see them again.";
        List<FormattedCharSequence> lines = Text.wrap(desc, pw - pad * 2);
        int lh = Text.lineHeight();
        Text.medium(g, "SAVED GAME", px + pad, py + pad, Theme.ORANGE);
        Text.heading(g, "PAUSED", px + pad, py + pad + 11, Theme.TEXT, Text.px(32));
        int y = py + pad + 30;
        int maxLines = Math.max(0, (btnY - 4 - y) / lh);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++)
            Text.draw(g, lines.get(i), px + pad, y + i * lh, Theme.DESC);

        int bx = px + pad, bw = pw - pad * 2;
        boolean hover = host && over(mx, my, bx, btnY, bw, btnH);
        Theme.readyButton(g, bx, btnY, bw, btnH, host ? Theme.ReadyState.CAN_READY : Theme.ReadyState.DISABLED, hover);
        String label = !host ? "WAITING FOR THE HOST" : offline.isEmpty() ? "RESUME"
                : "RESUME WITHOUT " + offline.size() + (offline.size() == 1 ? " PLAYER" : " PLAYERS");
        int lw = Text.mediumWidth(label) + (host ? 12 + 6 : 0);
        int lx = bx + (bw - lw) / 2;
        if (host) {
            Icon.CHECK.draw(g, lx, btnY + (btnH - 12) / 2, 12, Theme.ON_ORANGE);
            lx += 12 + 6;
        }
        Text.medium(g, label, lx, btnY + (btnH - 9) / 2, host ? Theme.ON_ORANGE : Theme.DIM);
        if (host)
            hits.add(new Hit(bx, btnY, bw, btnH, MatchResumeServerboundPacket::resume));
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float k = Text.guiPerUnit();
        double mx = mouseX / k, my = mouseY / k;
        if (HostPopup.open && frame != null) {
            frame.click(frame.scale.toUnits(mouseX), frame.scale.toUnits(mouseY), button == 0);
            return true;
        }
        if (button == 0)
            for (Hit hit : List.copyOf(hits))
                if (hit.contains(mx, my)) {
                    hit.action.run();
                    return true;
                }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && HostPopup.open) {
            HostPopup.open = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // same as the pause menu's Save and Quit
    private void saveAndQuit() {
        boolean local = minecraft.isLocalServer();
        if (minecraft.level != null)
            minecraft.level.disconnect();
        if (local)
            minecraft.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
        else
            minecraft.clearLevel();
        minecraft.setScreen(new TitleScreen());
    }

    private static void seatIcon(GuiGraphics g, String name, boolean bot, @Nullable Faction faction, int x, int y, int size) {
        if (bot && faction != null)
            MyRenderer.renderIcon(g, faction.icon, x, y, size);
        else if (bot)
            Text.unknown(g, x, y, size);
        else if (faction != null)
            MyRenderer.renderPlayerHead(g, name, x, y, size, faction.icon);
        else
            MyRenderer.renderPlayerHead(g, name, x, y, size);
    }
}
