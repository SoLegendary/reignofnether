package com.solegendary.reignofnether.matchstart;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.gamerules.GameruleServerboundPacket;
import com.solegendary.reignofnether.chat.ChatLines;
import com.solegendary.reignofnether.chat.ChatLog;
import com.solegendary.reignofnether.chat.ChatPanel;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.hud.panels.SettingRows;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.hud.playerdisplay.AbstractPlayerDisplay;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.p2p.HostPopup;
import com.solegendary.reignofnether.p2p.P2PHost;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoClientEvents;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerboundPacket;
import com.solegendary.reignofnether.startpos.SeatJoin;
import com.solegendary.reignofnether.startpos.StartPos;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.startpos.StartPosServerboundPacket;
import com.solegendary.reignofnether.ui.Clip;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.HudScale;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.SettingRow;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Match setup: pick a seat on the map or in the roster, pick a faction, ready up. Drawn in HUD units
 * (see {@link HudScale}) so it keeps its proportions on any screen.
 *
 * <pre>
 *  MATCH SETUP · MODE                                   PLAYERS 2/4 READY  [GAME RULES] [CLOSE]
 *  MAP NAME    description
 *  +-- map with seat markers --+   +-- roster (teams, seats, spectators) -----------------+
 *  |                           |   |                                                       |
 *  +---------------------------+   +-- lobby chat ----------------------+-- your pick ----+
 *  [1 take a seat][2 faction][3 ready] |                                 |  READY UP       |
 * </pre>
 */
public class MatchStartScreen extends Screen {

    private static final Faction[] FACTIONS = { Factions.VILLAGERS, Factions.MONSTERS, Factions.PIGLINS, Factions.RANDOM };

    private static final int M = 14;   // outer margin
    private static final int G = 10;   // gap between panels
    private static final int ROW_H = 36;
    private static final int ROW_PAD = 7;  // roster row padding before the team strip
    private static final int HEAD = 24;    // roster row head box
    // mostly solid so the rows read clearly, a hint of the world behind
    private static final int PANEL_FILL = 0xE016151B;

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, path);
    }

    private HudFrame frame;
    private EditBox chatInput;
    private Rect chatRect = Rect.EMPTY;
    private Rect rosterRect = Rect.EMPTY;
    // the map is drawn before the roster, so it shows the seat row hovered last frame
    private StartPos hoveredRow, nextHoveredRow;
    private int rosterScroll = 0;
    private int rosterMaxScroll = 0;
    private int chatScroll = 0;
    private int chatMaxScroll = 0;

    // the minimap as it was before this screen, restored on close (init runs again on every resize)
    private final boolean priorLargeMap = MinimapClientEvents.isLargeMap();
    private final boolean priorMapLocked = MinimapClientEvents.isMapLocked();

    public MatchStartScreen() {
        super(Component.translatable("matchstart.reignofnether.title"));
    }

    @Override
    protected void init() {
        super.init();
        MinimapClientEvents.setLargeMap(true);
        MinimapClientEvents.suppressViewQuad = true;
        recentreMapOnStartPoses();

        // drawn and fed input by this screen in HUD units, so it is not added as a vanilla widget; kept across resizes
        if (chatInput != null)
            return;
        chatInput = new EditBox(this.font, 0, 0, 100, 16, Component.translatable("matchstart.reignofnether.chat.placeholder"));
        chatInput.setBordered(false);
        chatInput.setMaxLength(ChatPanel.MAX_LENGTH);
        chatInput.setTextColor(Theme.TEXT);
    }

    private void recentreMapOnStartPoses() {
        Minecraft mc = Minecraft.getInstance();
        List<StartPos> poses = StartPosClientEvents.startPoses;
        if (poses.isEmpty()) {
            if (mc.player != null)
                MinimapClientEvents.forceMapCentre((int) mc.player.getX(), (int) mc.player.getZ());
            MinimapClientEvents.setMapLocked(false);
            return;
        }
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (StartPos sp : poses) {
            minX = Math.min(minX, sp.pos.getX());
            maxX = Math.max(maxX, sp.pos.getX());
            minZ = Math.min(minZ, sp.pos.getZ());
            maxZ = Math.max(maxZ, sp.pos.getZ());
        }
        MinimapClientEvents.forceMapCentre((minX + maxX) / 2, (minZ + maxZ) / 2);
        MinimapClientEvents.setMapLocked(true);
    }

    // ------------------------------------------------------------------ helpers

    private static String localName() {
        var player = Minecraft.getInstance().player;
        return player != null ? player.getName().getString() : "";
    }

    /**
     * The enemy on a hidden seat when it can only be them: one hidden enemy and one hidden seat. Their faction
     * stays hidden; only the seat is no secret.
     */
    @Nullable
    private static String deduced(StartPos sp) {
        if (!sp.hidden || StartPosClientEvents.hiddenPlayers.size() != 1
                || StartPosClientEvents.startPoses.stream().filter(p -> p.hidden).count() != 1)
            return null;
        return StartPosClientEvents.hiddenPlayers.keySet().iterator().next();
    }

    /** Taken by someone, including an enemy whose pick is hidden from us. */
    private static boolean taken(StartPos sp) {
        return !sp.playerName.isBlank() || sp.hidden;
    }

    private static void seatHead(GuiGraphics g, StartPos sp, int x, int y, int size) {
        if (sp.faction == Factions.RANDOM)
            MyRenderer.renderPlayerHead(g, sp.playerName, x, y, size);
        else
            MyRenderer.renderPlayerHead(g, sp.playerName, x, y, size, sp.faction.icon);
    }

    private static String factionKey(Faction f) {
        return f == Factions.RANDOM ? "random" : f.getName();
    }

    /** Seat colour, with the design's brighter red and blue in place of Minecraft's dark ones. */
    private static int seatColour(StartPos sp) {
        int hex = sp.getHexColor();
        if (hex == 0xA12722)
            hex = 0xD8443A;
        else if (hex == 0x35399D && StartPosClientEvents.startPoses.stream().noneMatch(o -> o.getHexColor() == 0x3AAFD9))
            hex = 0x3AAFD9;
        return 0xFF000000 | hex;
    }

    /** Enabled and disabled seats grouped by team colour, in map order. */
    private static Map<Integer, List<StartPos>> teams() {
        Map<Integer, List<StartPos>> byColour = new LinkedHashMap<>();
        for (StartPos sp : StartPosClientEvents.startPoses)
            byColour.computeIfAbsent(sp.colorId, k -> new ArrayList<>()).add(sp);
        return byColour;
    }

    private static List<String> spectators() {
        List<String> seated = new ArrayList<>(StartPosClientEvents.hiddenPlayers.keySet());
        for (SeatJoin join : StartPosClientEvents.joins)
            seated.add(join.playerName);
        for (StartPos sp : StartPosClientEvents.startPoses)
            if (!sp.playerName.isBlank())
                seated.add(sp.playerName);
        List<String> specs = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        if (level != null)
            for (AbstractClientPlayer p : level.players())
                if (!seated.contains(p.getName().getString()))
                    specs.add(p.getName().getString());
        return specs;
    }

    private static int[] readyCount() {
        return new int[] { StartPosClientEvents.getNumPlayersReady(), StartPosClientEvents.getNumEnabledPoses() };
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        HudScale scale = HudGeometry.scale();
        frame = new HudFrame(g, scale, HudGeometry.layout(), scale.toUnits(mouseX), scale.toUnits(mouseY));
        hoveredRow = nextHoveredRow;
        nextHoveredRow = null;
        int w = scale.width();
        int h = scale.height();

        g.pose().pushPose();
        g.pose().scale(scale.poseScale(), scale.poseScale(), 1);

        // the world stays visible under a nether-red wash
        g.fillGradient(0, 0, w, h, 0xA62A0806, 0xCC140303);

        int headerH = 38;
        renderHeader(w);

        int bodyTop = M + headerH + G;
        int bodyH = h - M - bodyTop;
        int leftW = Math.max(200, Math.min(420, (w - 2 * M - G) * 42 / 100));
        boolean showSteps = bodyH > 240;
        int stepsH = showSteps ? 32 : 0;

        renderMap(new Rect(M, bodyTop, leftW, bodyH - (showSteps ? stepsH + G : 0)));
        if (showSteps)
            renderSteps(new Rect(M, bodyTop + bodyH - stepsH, leftW, stepsH));

        int rx = M + leftW + G;
        int rw = w - M - rx;
        int bottomH = Math.max(96, Math.min(150, bodyH * 38 / 100));
        rosterRect = new Rect(rx, bodyTop, rw, bodyH - bottomH - G);
        renderRoster(rosterRect);
        int pickW = Math.max(150, Math.min(215, rw * 42 / 100));
        chatRect = new Rect(rx, bodyTop + bodyH - bottomH, rw - pickW - G, bottomH);
        renderChat(chatRect);
        renderPick(new Rect(chatRect.right() + G, chatRect.y(), pickW, bottomH));

        if (StartPosClientEvents.isStarting)
            renderCountdown(w);
        if (GameruleClient.gamerulesMenuOpen)
            renderRulesModal(w, h);
        else if (HostPopup.open)
            HostPopup.render(frame, w, h);

        frame.showHoveredTooltip();
        Tooltip.renderPending(g, w, h);
        g.pose().popPose();
    }

    private void renderHeader(int w) {
        var g = frame.g;
        String mode = RTSMapInfoClientEvents.selectedMode;
        String kicker = I18n.get("matchstart.reignofnether.header", mode.isBlank() ? "" : " · " + mode.toUpperCase(Locale.ROOT));
        Text.medium(g, kicker, M, M - 2, Theme.ORANGE);

        String map = RTSMapInfoClientEvents.mapName.isBlank() ? I18n.get("matchstart.reignofnether.unnamed_map") : RTSMapInfoClientEvents.mapName;
        float titleScale = Text.px(48);
        Text.title(g, map.toUpperCase(Locale.ROOT), M, M + 9, titleScale);
        int tx = M + Text.titleWidth(map.toUpperCase(Locale.ROOT), titleScale) + 12;

        // right side: ready count, game rules, close
        int right = w - M;
        int bh = 22;
        int by = M + 6;
        right = headerButton(I18n.get("matchstart.reignofnether.close"), null, right, by, bh, MatchStartClientEvents::dismiss, null, true);
        right = headerButton(I18n.get("matchstart.reignofnether.game_rules"), Icon.LIST, right - 6, by, bh,
                () -> GameruleClient.gamerulesMenuOpen = !GameruleClient.gamerulesMenuOpen,
                Tooltip.of(I18n.get("matchstart.reignofnether.game_rules")).body(I18n.get(StartPosClientEvents.isHost()
                        ? "matchstart.reignofnether.rules_host" : "matchstart.reignofnether.rules_view")), true);
        // our own world: let players elsewhere join by code
        if (Minecraft.getInstance().hasSingleplayerServer())
            right = headerButton(I18n.get(P2PHost.state == P2PHost.State.OFF ? "matchstart.reignofnether.host_online" : "matchstart.reignofnether.invite"),
                    Icon.USERS, right - 6, by, bh, () -> HostPopup.open = !HostPopup.open,
                    Tooltip.of(I18n.get("matchstart.reignofnether.host_online")).body(I18n.get("matchstart.reignofnether.host_tip")), true);

        int[] rc = readyCount();
        String ready = I18n.get("matchstart.reignofnether.ready_count", rc[0], rc[1]);
        int rx = right - 12;
        float readyScale = Text.px(26);
        Text.smallRight(g, I18n.get("matchstart.reignofnether.players"), rx, by - 2, Theme.LABEL);
        Text.bigRight(g, ready.toUpperCase(Locale.ROOT), rx, by + 8, rc[0] == rc[1] && rc[1] > 1 ? Theme.OK : Theme.TEXT, readyScale);
        int infoRight = rx - Math.max(Text.bigWidth(ready.toUpperCase(Locale.ROOT), readyScale), Text.smallWidth(I18n.get("matchstart.reignofnether.players"))) - 16;

        int infoW = infoRight - tx;
        if (infoW > 60) {
            String desc = RTSMapInfoClientEvents.description;
            if (!desc.isBlank())
                Text.draw(g, Text.fit(desc, infoW), tx, M + 13, Theme.MUTED);
            String meta = I18n.get("matchstart.reignofnether.meta", RTSMapInfoClientEvents.version, StartPosClientEvents.startPoses.size());
            Text.small(g, Text.fitSmall(meta.toUpperCase(Locale.ROOT), infoW), tx, M + 23, Theme.DIM);
        }
    }

    /** A secondary button right-aligned at `right`. Returns its left edge. */
    private int headerButton(String label, @Nullable Icon icon, int right, int y, int h, Runnable action, @Nullable Tooltip tooltip, boolean enabled) {
        int iconW = icon != null ? 12 + 5 : 0;
        int w = Text.width(label) + iconW + 18;
        Rect r = new Rect(right - w, y, w, h);
        boolean hovered = enabled && frame.hovered(r);
        Theme.secondaryButton(frame.g, r.x(), y, w, h, hovered);
        if (icon != null)
            icon.draw(frame.g, r.x() + 9, y + (h - 12) / 2, 12, hovered ? Theme.GOLD : Theme.BODY);
        Text.draw(frame.g, label, r.x() + 9 + iconW, y + (h - 8) / 2 + 1, hovered ? Theme.GOLD : Theme.BODY);
        frame.register(new ButtonBuilder(label).isEnabled(() -> enabled).onLeftClick(action).build(), r, null,
                tooltip == null ? null : () -> tooltip);
        return r.x();
    }

    // ------------------------------------------------------------------ map

    private void renderMap(Rect p) {
        var g = frame.g;
        Theme.panel(g, p.x(), p.y(), p.w(), p.h(), PANEL_FILL);
        renderModeChips(p.right() - 11, p.y() + 6);

        int top = p.y() + (RTSMapInfoClientEvents.modeNames.size() > 1 ? 24 : 8);
        int side = Math.max(40, Math.min(p.w() - 16, p.bottom() - top - 8));
        int mx = p.x() + (p.w() - side) / 2;
        int my = top + (p.bottom() - top - 8 - side) / 2;
        Theme.outline(g, mx - 2, my - 2, side + 4, side + 4, 1, Theme.INK);
        Theme.outline(g, mx, my, side, side, 1, Theme.CONSOLE_RIM);
        if (MinimapClientEvents.isMapReady()) {
            MinimapClientEvents.renderMapInto(g, mx, my, side, side);
            g.fill(mx, my, mx + side, my + side, 0x33000000);
        } else {
            g.fill(mx, my, mx + side, my + side, Theme.EMPTY);
            Text.drawCentered(g, I18n.get("matchstart.reignofnether.map_loading"), mx + side / 2, my + side / 2 - 4, Theme.DIM);
        }

        int marker = Math.max(18, Math.min(40, side * 12 / 100));
        Clip.push(g, mx - marker, my - marker, side + marker * 2, side + marker * 2);
        for (StartPos sp : StartPosClientEvents.startPoses) {
            Vector2f pos = MinimapClientEvents.worldToRect(sp.pos.getX(), sp.pos.getZ(), mx, my, side, side);
            if (pos.x < mx || pos.x > mx + side || pos.y < my || pos.y > my + side)
                continue;
            renderMarker(sp, (int) pos.x, (int) pos.y, marker);
        }
        Clip.pop(g);
    }

    private void renderModeChips(int right, int y) {
        List<String> modes = RTSMapInfoClientEvents.modeNames;
        if (modes.size() <= 1)
            return;
        boolean canPick = StartPosClientEvents.isHost() && !StartPosClientEvents.isStarting;
        int x = right;
        for (int i = modes.size() - 1; i >= 0; i--) {
            String mode = modes.get(i);
            String label = mode.toUpperCase(Locale.ROOT);
            int w = Text.smallWidth(label) + 12;
            x -= w;
            Rect r = new Rect(x, y, w, 15);
            boolean on = mode.equals(RTSMapInfoClientEvents.selectedMode);
            Theme.chip(frame.g, x, y, w, 15, on, canPick && frame.hovered(r));
            Text.smallCentered(frame.g, label, r.centerX(), y + 3, on ? Theme.GOLD : canPick ? Theme.DESC : Theme.DIM);
            frame.register(new ButtonBuilder(mode)
                    .isEnabled(() -> canPick && !on)
                    .onLeftClick(() -> RTSMapInfoServerboundPacket.setStartingMode(mode))
                    .build(), r, null, canPick ? null : () -> Tooltip.of(label).status(I18n.get("matchstart.reignofnether.host_only"), Theme.LABEL));
            x -= 4;
        }
    }

    private void renderMarker(StartPos sp, int cx, int cy, int size) {
        var g = frame.g;
        String me = localName();
        boolean mine = !sp.playerName.isBlank() && sp.playerName.equals(me);
        boolean open = sp.enabled && !taken(sp);
        int x = cx - size / 2;
        int y = cy - size / 2;
        Rect r = new Rect(x, y, size, size);
        boolean hovered = frame.hovered(r) && (open || mine);

        if (hoveredRow != null && hoveredRow.pos.equals(sp.pos))
            Theme.outline(g, x - 4, y - 4, size + 8, size + 8, 2, Theme.GOLD);
        if (mine)
            Theme.outline(g, x - 2, y - 2, size + 4, size + 4, 2, Theme.ORANGE);
        Theme.outline(g, x, y, size, size, 1, Theme.INK);
        g.fill(x, y + (hovered ? -1 : 0), x + size, y + size + (hovered ? -1 : 0), sp.enabled ? seatColour(sp) : 0xFF2A2930);
        g.fill(x, y, x + size, y + 2, 0x4DFFFFFF);
        g.fill(x, y + size - 2, x + size, y + size, 0x4D000000);

        int inner = size * 2 / 3;
        if (!sp.enabled)
            Icon.LOCK.drawIn(g, x, y, size, size, Theme.TEXT);
        else if (sp.hidden && deduced(sp) != null)
            MyRenderer.renderPlayerHead(g, deduced(sp), x + (size - inner) / 2, y + (size - inner) / 2, inner);
        else if (sp.hidden)
            Text.unknown(g, x + (size - inner) / 2, y + (size - inner) / 2, inner);
        else if (sp.playerName.isBlank())
            Icon.PLUS.drawIn(g, x, y, size, size, Theme.TEXT);
        else
            seatHead(g, sp, x + (size - inner) / 2, y + (size - inner) / 2, inner);
        if (sp.ready && !sp.playerName.isBlank()) {
            g.fill(x + size - 7, y + size - 7, x + size + 2, y + size + 2, Theme.OK_DARK);
            Theme.ring(g, x + size - 7, y + size - 7, 9, 9, 1, Theme.OK);
        }

        String label = !sp.enabled ? I18n.get("matchstart.reignofnether.seat.closed")
                : sp.hidden ? (deduced(sp) != null ? deduced(sp).toUpperCase(Locale.ROOT) : I18n.get("matchstart.reignofnether.pick_hidden"))
                : sp.playerName.isBlank() ? I18n.get("matchstart.reignofnether.seat.open")
                : mine ? I18n.get("matchstart.reignofnether.seat.you") : sp.playerName.toUpperCase(Locale.ROOT);
        int lw = Text.smallWidth(label) + 8;
        int ly = y + size + 4;
        g.fill(cx - lw / 2, ly, cx - lw / 2 + lw, ly + 11, 0xE60B0A0F);
        Text.smallCentered(g, label, cx, ly + 2, mine ? Theme.GOLD : open ? Theme.TEXT : Theme.MUTED);

        Button b = new ButtonBuilder(StartPos.BUTTON_NAME)
                .onLeftClick(() -> claim(sp))
                .onRightClick(StartPosClientEvents.isHost() && !StartPosClientEvents.isStarting ? () -> setSeatOpen(sp, !sp.enabled) : null)
                .build();
        frame.register(b, r, null, () -> seatTooltip(sp));
    }

    private Tooltip seatTooltip(StartPos sp) {
        Tooltip t = Tooltip.of(sp.hidden ? (deduced(sp) != null ? deduced(sp) : I18n.get("matchstart.reignofnether.pick_hidden"))
                : sp.playerName.isBlank() ? I18n.get(sp.enabled ? "matchstart.reignofnether.seat.open_title"
                : "matchstart.reignofnether.seat.closed_title") : sp.playerName);
        if (sp.enabled && !taken(sp))
            t.status(I18n.get(StartPosClientEvents.getPos() != null ? "matchstart.reignofnether.seat.move_hint" : "matchstart.reignofnether.seat.take_hint"), Theme.GOLD);
        if (StartPosClientEvents.isHost())
            t.status(I18n.get(sp.enabled ? "matchstart.reignofnether.seat.close_hint" : "matchstart.reignofnether.seat.open_hint"), Theme.LABEL);
        return t;
    }

    // ------------------------------------------------------------------ steps

    private void renderSteps(Rect r) {
        StartPos mine = StartPosClientEvents.getPos();
        boolean seated = mine != null;
        boolean picked = seated && mine.faction != Factions.NONE;
        boolean ready = seated && mine.ready;
        int w = (r.w() - 2 * 6) / 3;
        step(r.x(), r.y(), w, r.h(), 1, I18n.get("matchstart.reignofnether.step.seat"), seated, !seated);
        step(r.x() + w + 6, r.y(), w, r.h(), 2, I18n.get("matchstart.reignofnether.step.faction"), picked, seated && !picked);
        step(r.x() + 2 * (w + 6), r.y(), w, r.h(), 3, I18n.get("matchstart.reignofnether.step.ready"), ready, picked && !ready);
    }

    private void step(int x, int y, int w, int h, int n, String title, boolean done, boolean current) {
        var g = frame.g;
        Theme.bevel(g, x, y, w, h, current ? Theme.ACTIVE : Theme.PANEL, current ? Theme.ORANGE : 0x0AFFFFFF, 0, 1, Theme.INK);
        int mark = 16;
        int mx = x + 8;
        int my = y + (h - mark) / 2;
        g.fill(mx, my, mx + mark, my + mark, done ? Theme.OK_DARK : current ? Theme.ORANGE : Theme.RAISED);
        if (done)
            Icon.CHECK.drawIn(g, mx, my, mark, mark, Theme.OK_TEXT);
        else
            Text.drawCentered(g, String.valueOf(n), mx + mark / 2 + 1, my + (mark - 8) / 2 + 1, current ? Theme.ON_ORANGE : Theme.DIM);
        int tx = mx + mark + 7;
        Text.draw(g, Text.fit(title, x + w - tx - 6), tx, y + (h - 8) / 2 + 1, done ? Theme.MUTED : current ? Theme.GOLD : Theme.TEXT);
    }

    // ------------------------------------------------------------------ roster

    private void renderRoster(Rect p) {
        var g = frame.g;
        Theme.panel(g, p.x(), p.y(), p.w(), p.h(), PANEL_FILL);
        Text.small(g, I18n.get("matchstart.reignofnether.roster"), p.x() + 11, p.y() + 9, Theme.LABEL);
        renderRuleToggles(p.right() - 11, p.y() + 6);

        List<StartPos> empty = new ArrayList<>();
        boolean anySeated = false;
        for (StartPos sp : StartPosClientEvents.startPoses) {
            if (sp.enabled && !taken(sp))
                empty.add(sp);
            if (taken(sp))
                anySeated = true;
        }
        anySeated |= !StartPosClientEvents.hiddenPlayers.isEmpty();
        int blockerH = !empty.isEmpty() && anySeated ? 22 : 0;

        int top = p.y() + 24;
        int bottom = p.bottom() - 4 - blockerH;
        int x = p.x() + 8;
        int w = p.w() - 16;
        Clip.push(g, p.x(), top, p.w(), bottom - top);
        int y = top - rosterScroll;

        String me = localName();
        StartPos mine = StartPosClientEvents.getPos();
        int teamIndex = 1;
        for (List<StartPos> team : teams().values()) {
            boolean myTeam = mine != null && team.contains(mine);
            long filled = team.stream().filter(MatchStartScreen::taken).count();
            long open = team.stream().filter(sp -> sp.enabled).count();
            y = teamHeader(x, y, seatColour(team.get(0)), I18n.get("matchstart.reignofnether.team", teamIndex++),
                    I18n.get("matchstart.reignofnether.team_sub", filled, open) + (myTeam ? " · " + I18n.get("matchstart.reignofnether.your_team") : ""));
            for (StartPos sp : team) {
                if (y + ROW_H > top - ROW_H && y < bottom + ROW_H)
                    renderSeatRow(sp, me, x, y, w, top, bottom);
                y += ROW_H + 3;
                for (SeatJoin join : StartPosClientEvents.joins) {
                    if (!join.isSoldier() && join.seat.equals(sp.pos)) {
                        renderJoinRow(join, me, x, y, w, top, bottom);
                        y += ROW_H + 3;
                    }
                }
            }
            int colorId = team.get(0).colorId;
            List<SeatJoin> soldiers = StartPosClientEvents.joins.stream().filter(j -> j.isSoldier() && j.colorId == colorId).toList();
            for (SeatJoin join : soldiers) {
                renderJoinRow(join, me, x, y, w, top, bottom);
                y += ROW_H + 3;
            }
            if (soldiers.size() < GameruleClient.soldiersPerTeam && team.stream().anyMatch(sp -> sp.enabled)) {
                renderSoldierSlot(team, x, y, w, top, bottom);
                y += ROW_H + 3;
            }
            y += 4;
        }
        boolean onSeat = StartPosClientEvents.startPoses.stream().anyMatch(seat -> deduced(seat) != null);
        if (!StartPosClientEvents.hiddenPlayers.isEmpty() && !onSeat) {
            y = teamHeader(x, y, Theme.DIM, I18n.get("matchstart.reignofnether.other_teams"), I18n.get("matchstart.reignofnether.picks_hidden"));
            for (Map.Entry<String, Boolean> e : StartPosClientEvents.hiddenPlayers.entrySet()) {
                renderHiddenRow(e.getKey(), e.getValue(), x, y, w);
                y += ROW_H + 3;
            }
            y += 4;
        }
        if (StartPosClientEvents.startPoses.isEmpty())
            Text.draw(g, I18n.get("matchstart.reignofnether.no_positions"), x, y, Theme.DIM);

        y = renderSpectators(x, y, w, mine != null || StartPosClientEvents.getJoin() != null, top, bottom);
        Clip.pop(g);

        int contentH = y + rosterScroll - top;
        rosterMaxScroll = Math.max(0, contentH - (bottom - top));
        rosterScroll = Math.min(rosterScroll, rosterMaxScroll);
        if (rosterMaxScroll > 0) {
            int trackH = bottom - top;
            int thumbH = Math.max(16, trackH * trackH / Math.max(1, contentH));
            int thumbY = top + (trackH - thumbH) * rosterScroll / rosterMaxScroll;
            g.fill(p.right() - 4, top, p.right() - 2, bottom, Theme.INK);
            g.fill(p.right() - 4, thumbY, p.right() - 2, thumbY + thumbH, Theme.FAINT);
        }

        if (blockerH > 0)
            renderBlocker(new Rect(p.x() + 1, p.bottom() - blockerH - 1, p.w() - 2, blockerH), empty.size());
    }

    /** Fog of war and alliance lock as quick toggles, right-aligned at `right`. Host only; players see them in the rules panel. */
    private void renderRuleToggles(int right, int y) {
        if (!StartPosClientEvents.isHost())
            return;
        int x = ruleToggle(right, y, "fog_of_war", FogOfWarClientEvents.isMatchFogOn(), GameruleServerboundPacket::setFogOfWar);
        x = ruleToggle(x - 4, y, "lock_alliances", GameruleClient.lockAlliances, GameruleServerboundPacket::setLockAlliances);
        x = ruleToggle(x - 4, y, "co_commanders", GameruleClient.coCommanders, GameruleServerboundPacket::setCoCommanders);
        // soldiers cycle 0..MAX seats per team
        ruleChip(x - 4, y, "soldiers_per_team", GameruleClient.soldiersPerTeam > 0, String.valueOf(GameruleClient.soldiersPerTeam),
                () -> GameruleServerboundPacket.setSoldiersPerTeam((GameruleClient.soldiersPerTeam + 1) % (GameruleClient.MAX_SOLDIERS_PER_TEAM + 1)));
    }

    /** An ON/OFF chip for a lobby game rule. Returns its left edge. */
    private int ruleToggle(int right, int y, String rule, boolean on, Consumer<Boolean> set) {
        return ruleChip(right, y, rule, on, null, () -> set.accept(!on));
    }

    /** A lobby game rule chip, with its value after the name when given. Returns its left edge. */
    private int ruleChip(int right, int y, String rule, boolean on, @Nullable String value, Runnable click) {
        boolean teamLocked = rule.equals("lock_alliances") && RTSMapInfoClientEvents.teamMode;
        boolean canToggle = StartPosClientEvents.isHost() && !StartPosClientEvents.isStarting && !teamLocked;
        String label = I18n.get("gamerule.reignofnether.name." + rule).toUpperCase(Locale.ROOT) + (value != null ? " " + value : "");
        int w = Text.smallWidth(label) + 19;
        Rect r = new Rect(right - w, y, w, 15);
        Theme.chip(frame.g, r.x(), y, w, 15, false, canToggle && frame.hovered(r));
        frame.g.fill(r.x() + 5, y + 5, r.x() + 10, y + 10, on ? Theme.OK : Theme.FAINT);
        Text.small(frame.g, label, r.x() + 14, y + 3, canToggle ? Theme.BODY : Theme.LABEL);
        frame.register(new ButtonBuilder(rule)
                .isEnabled(() -> canToggle)
                .onLeftClick(click)
                .build(), r, null, () -> Tooltip.of(I18n.get("gamerule.reignofnether.name." + rule))
                .tag(value != null ? value : I18n.get(on ? "options.on" : "options.off"), on ? Theme.OK : Theme.LABEL)
                .body(I18n.get("commands.reignofnether.gamerule." + rule))
                .status(canToggle ? "" : I18n.get(teamLocked ? "hud.gamerule.reignofnether.team_locked" : "matchstart.reignofnether.host_only"), Theme.LABEL));
        return r.x();
    }

    private int teamHeader(int x, int y, int colour, String name, String sub) {
        Theme.outline(frame.g, x, y + 1, 7, 7, 1, Theme.INK);
        frame.g.fill(x, y + 1, x + 7, y + 8, colour);
        Text.big(frame.g, name, x + 12, y, Theme.TEXT, Text.px(18));
        Text.small(frame.g, sub.toUpperCase(Locale.ROOT), x + 12 + Text.bigWidth(name, Text.px(18)) + 8, y, Theme.DIM);
        return y + 12;
    }

    private void renderSeatRow(StartPos sp, String me, int x, int y, int w, int clipTop, int clipBottom) {
        var g = frame.g;
        boolean mine = !sp.playerName.isBlank() && sp.playerName.equals(me);
        boolean empty = !taken(sp);
        boolean host = StartPosClientEvents.isHost() && !StartPosClientEvents.isStarting;
        boolean visible = y >= clipTop && y + ROW_H <= clipBottom;
        boolean claimable = sp.enabled && empty && !StartPosClientEvents.isStarting;
        Rect row = new Rect(x, y, w, ROW_H);
        if (visible && frame.hovered(row))
            nextHoveredRow = sp;

        // the whole row of an open seat takes it; its buttons are registered after, so they win
        int hx = rowFrame(x, y, w, mine, !sp.enabled ? Theme.SUNKEN : Theme.RAISED, claimable && frame.hovered(row),
                sp.enabled ? seatColour(sp) : Theme.FAINT);
        int hy = y + (ROW_H - HEAD) / 2;
        if (visible && claimable)
            frame.register(new ButtonBuilder("take seat row").onLeftClick(() -> claim(sp)).build(), row, null,
                    () -> Tooltip.of(I18n.get("matchstart.reignofnether.seat.open_title"))
                            .status(I18n.get(StartPosClientEvents.getPos() != null ? "matchstart.reignofnether.seat.move_hint" : "matchstart.reignofnether.seat.take_hint"), Theme.GOLD));

        if (!sp.enabled)
            Icon.LOCK.drawIn(g, hx, hy, HEAD, HEAD, Theme.DIM);
        else if (sp.hidden && deduced(sp) != null)
            MyRenderer.renderPlayerHead(g, deduced(sp), hx + 2, hy + 2, HEAD - 4);
        else if (sp.hidden)
            Text.unknown(g, hx + 2, hy + 2, HEAD - 4);
        else if (empty)
            Icon.PLUS.drawIn(g, hx, hy, HEAD, HEAD, Theme.DIM);
        else
            seatHead(g, sp, hx + 2, hy + 2, HEAD - 4);

        // right side first, so the name gets what's left: status column, then actions
        int right = x + w - ROW_PAD;
        // a hidden enemy's ready state is in the other teams list, not on their seat
        String known = deduced(sp);
        boolean ready = known != null ? StartPosClientEvents.hiddenPlayers.getOrDefault(known, false) : sp.ready;
        String status = !sp.enabled || (sp.hidden && known == null) ? "" : empty ? I18n.get("matchstart.reignofnether.status.waiting")
                : ready ? I18n.get("matchstart.reignofnether.status.ready") : I18n.get("matchstart.reignofnether.status.not_ready");
        if (!mine) {
            Text.mediumRight(g, status, right, y + (ROW_H - 8) / 2, ready ? Theme.OK : empty ? Theme.ORANGE : Theme.LABEL);
            right -= 52 + 8;
        }
        if (visible)
            right = renderSeatActions(sp, mine, empty, host, right, y + (ROW_H - 20) / 2, 20);

        int nx = hx + HEAD + 8;
        if (mine) {
            int size = 20;
            boolean labels = right - pickerWidth(size, true) > nx + 70;
            int fx = right - pickerWidth(size, labels);
            renderFactionPicker(sp, fx, y + (ROW_H - size) / 2, size, visible, labels);
            right = fx - 8;
        } else if (known != null) {
            String fLabel = I18n.get("matchstart.reignofnether.faction_hidden");
            int fx = right - Text.smallWidth(fLabel);
            if (fx > nx + 50) {
                Text.small(g, fLabel, fx, y + (ROW_H - 8) / 2, Theme.DIM);
                right = fx - 8;
            }
        } else if (!empty && !sp.hidden) {
            boolean picked = sp.faction != Factions.NONE;
            String fLabel = picked ? MiscUtil.getFactionName(sp.faction) : I18n.get("matchstart.reignofnether.choosing");
            int fw = Text.smallWidth(fLabel) + (picked ? 19 : 0);
            int fx = right - fw;
            if (fx > nx + 50) {
                if (picked)
                    drawFactionIcon(g, sp.faction, fx, y + (ROW_H - 14) / 2, 14);
                Text.small(g, fLabel, fx + (picked ? 19 : 0), y + (ROW_H - 8) / 2, picked ? Theme.BODY : Theme.LABEL);
                right = fx - 8;
            }
        }

        String name = !sp.enabled ? I18n.get("matchstart.reignofnether.seat.closed_title")
                : sp.hidden ? (known != null ? known : I18n.get("matchstart.reignofnether.pick_hidden"))
                : empty ? I18n.get("matchstart.reignofnether.seat.open_title")
                : mine ? I18n.get("matchstart.reignofnether.you_name", sp.playerName) : sp.playerName;
        Text.draw(g, Text.fit(name, Math.max(10, Math.min(90, right - nx))), nx, y + 8, mine ? Theme.GOLD : empty || (sp.hidden && known == null) || !sp.enabled ? Theme.LABEL : Theme.TEXT);
        String seat = I18n.get("matchstart.reignofnether.seat_number", StartPosClientEvents.startPoses.indexOf(sp) + 1);
        Text.small(g, seat, nx, y + 19, Theme.DIM);
        if (!empty && !sp.hidden)
            drawPing(sp.playerName, nx + Text.smallWidth(seat) + 6, y + 19);
        if (!sp.enabled)
            g.fill(x, y, x + w, y + ROW_H, 0x66121118);
    }

    /** Background, team strip and empty head box shared by every roster row. Returns the head box's x. */
    private int rowFrame(int x, int y, int w, boolean mine, int fill, boolean hovered, int strip) {
        var g = frame.g;
        Theme.bevel(g, x, y, w, ROW_H, mine ? Theme.ACTIVE : fill, mine ? 0 : 0x0AFFFFFF, 0, 1, Theme.INK);
        if (mine)
            Theme.ring(g, x, y, w, ROW_H, 1, Theme.ORANGE);
        else if (hovered)
            g.fill(x, y, x + w, y + ROW_H, Theme.HOVER);
        g.fill(x + ROW_PAD, y + 5, x + ROW_PAD + 3, y + ROW_H - 5, strip);
        int hx = x + ROW_PAD + 3 + 8;
        int hy = y + (ROW_H - HEAD) / 2;
        Theme.outline(g, hx, hy, HEAD, HEAD, 1, Theme.INK);
        g.fill(hx, hy, hx + HEAD, hy + HEAD, Theme.EMPTY);
        return hx;
    }

    /** Faction icon; random uses the shuffle icon. */
    private static void drawFactionIcon(GuiGraphics g, Faction f, int x, int y, int size) {
        if (f == Factions.RANDOM)
            Icon.SHUFFLE.drawIn(g, x, y, size, size, Theme.BUFF);
        else
            MyRenderer.renderIcon(g, f.icon, x, y, size);
    }

    /** Take / move / leave / kick / open / close buttons for a seat row. Returns the new right edge. */
    private int renderSeatActions(StartPos sp, boolean mine, boolean empty, boolean host, int right, int y, int h) {
        if (StartPosClientEvents.isStarting)
            return right;
        if (!sp.enabled) {
            if (host)
                right = rowButton(I18n.get("matchstart.reignofnether.action.open"), right, y, h, false, () -> setSeatOpen(sp, true)) - 4;
            return right;
        }
        if (empty) {
            if (host)
                right = rowButton(I18n.get("matchstart.reignofnether.action.close"), right, y, h, false, () -> setSeatOpen(sp, false)) - 4;
            String take = I18n.get(StartPosClientEvents.getPos() != null ? "matchstart.reignofnether.action.move" : "matchstart.reignofnether.action.take");
            return primaryRowButton(take, right, y, h, () -> claim(sp)) - 4;
        }
        if (mine)
            return rowButton(I18n.get("matchstart.reignofnether.action.leave"), right, y, h, false, this::leaveOwnSeat) - 4;
        if (host)
            right = rowButton(I18n.get("matchstart.reignofnether.action.kick"), right, y, h, true,
                    () -> StartPosServerboundPacket.unreservePos(sp.pos)) - 4;
        boolean coTaken = StartPosClientEvents.joins.stream().anyMatch(j -> !j.isSoldier() && j.seat.equals(sp.pos));
        if (GameruleClient.coCommanders && !coTaken && !sp.hidden)
            right = rowButton(I18n.get("matchstart.reignofnether.action.co_command"), right, y, h, false, () -> {
                leaveOwnSeat();
                StartPosServerboundPacket.joinAsCoCommander(sp.pos);
            }) - 4;
        return right;
    }

    private int rowButton(String label, int right, int y, int h, boolean danger, Runnable action) {
        int w = Text.width(label) + 14;
        Rect r = new Rect(right - w, y, w, h);
        boolean hovered = frame.hovered(r);
        if (danger)
            Theme.dangerButton(frame.g, r.x(), y, w, h, hovered);
        else
            Theme.button(frame.g, r.x(), y, w, h, hovered);
        Text.drawCentered(frame.g, label, r.centerX(), y + (h - 8) / 2 + 1, danger ? Theme.DANGER_TEXT : hovered ? Theme.GOLD : Theme.BODY);
        frame.register(new ButtonBuilder(label).onLeftClick(action).build(), r, null, null);
        return r.x();
    }

    private int iconRowButton(Icon icon, int colour, boolean danger, String tooltip, int right, int y, int h, Runnable action) {
        Rect r = new Rect(right - h, y, h, h);
        boolean hovered = frame.hovered(r);
        if (danger)
            Theme.dangerButton(frame.g, r.x(), y, h, h, hovered);
        else
            Theme.button(frame.g, r.x(), y, h, h, hovered);
        icon.drawIn(frame.g, r.x(), y, h, h, colour);
        frame.register(new ButtonBuilder(tooltip).onLeftClick(action).build(), r, null, () -> Tooltip.of(I18n.get(tooltip)));
        return r.x();
    }

    private int primaryRowButton(String label, int right, int y, int h, Runnable action) {
        int w = Text.width(label) + 14;
        Rect r = new Rect(right - w, y, w, h);
        Theme.primaryButton(frame.g, r.x(), y, w, h, frame.hovered(r));
        Text.drawCentered(frame.g, label, r.centerX(), y + (h - 8) / 2 + 1, Theme.ON_ORANGE);
        frame.register(new ButtonBuilder(label).onLeftClick(action).build(), r, null, null);
        return r.x();
    }

    private static int pickerButtonWidth(Faction f, int size, boolean label) {
        return label ? size + Text.width(MiscUtil.getFactionName(f)) + 8 : size;
    }

    private static int pickerWidth(int size, boolean labels) {
        int w = 0;
        for (Faction f : FACTIONS)
            w += pickerButtonWidth(f, size, labels) + 4;
        return w - 4;
    }

    /** Your faction choice: icon and name buttons like the design, or icons only when the row is narrow. */
    private void renderFactionPicker(StartPos sp, int x, int y, int size, boolean clickable, boolean labels) {
        for (Faction f : FACTIONS) {
            int w = pickerButtonWidth(f, size, labels);
            Rect r = new Rect(x, y, w, size);
            boolean on = sp.faction == f;
            boolean hovered = frame.hovered(r);
            if (on)
                Theme.chip(frame.g, x, y, w, size, true, hovered);
            else
                Theme.button(frame.g, x, y, w, size, hovered);
            drawFactionIcon(frame.g, f, x + 3, y + 3, size - 6);
            if (labels)
                Text.draw(frame.g, MiscUtil.getFactionName(f), x + size, y + (size - 8) / 2 + 1, on ? Theme.GOLD : Theme.DESC);
            if (clickable)
                frame.register(new ButtonBuilder(f.getName()).onLeftClick(() -> pickFaction(sp, f)).build(), r, null,
                        () -> Tooltip.of(MiscUtil.getFactionName(f)).body(I18n.get("matchstart.reignofnether.faction_desc." + factionKey(f))));
            x += w + 4;
        }
    }

    /** A soldier or co-commander: no seat or faction of their own. */
    private void renderJoinRow(SeatJoin join, String me, int x, int y, int w, int clipTop, int clipBottom) {
        var g = frame.g;
        boolean mine = join.playerName.equals(me);
        boolean visible = y >= clipTop && y + ROW_H <= clipBottom;
        int indent = 14;
        int rx = x + indent;
        int rw = w - indent;
        int hx = rowFrame(rx, y, rw, mine, Theme.RAISED, false, teamColour(join.colorId));
        MyRenderer.renderPlayerHead(g, join.playerName, hx + 2, y + (ROW_H - HEAD) / 2 + 2, HEAD - 4);

        int right = rx + rw - ROW_PAD;
        String status = I18n.get(join.pending ? "matchstart.reignofnether.status.pending"
                : join.ready ? "matchstart.reignofnether.status.ready" : "matchstart.reignofnether.status.not_ready");
        Text.mediumRight(g, status, right, y + (ROW_H - 8) / 2, join.pending ? Theme.ORANGE : join.ready ? Theme.OK : Theme.LABEL);
        right -= 52 + 8;
        if (visible && mine && !StartPosClientEvents.isStarting)
            right = rowButton(I18n.get("matchstart.reignofnether.action.leave"), right, y + (ROW_H - 20) / 2, 20, false,
                    StartPosServerboundPacket::leaveJoin) - 4;
        // the check only while they're asking; the X also removes someone already let in
        if (visible && !StartPosClientEvents.isStarting && StartPosClientEvents.startPoses.stream().anyMatch(sp -> sp.playerName.equals(me) && join.answeredBy(sp))) {
            right = iconRowButton(Icon.CLOSE, Theme.DANGER_TEXT, true, "matchstart.reignofnether.action.refuse", right, y + (ROW_H - 20) / 2, 20,
                    () -> StartPosServerboundPacket.answerJoin(join.playerName, false)) - 4;
            if (join.pending)
                right = iconRowButton(Icon.CHECK, Theme.OK, false, "matchstart.reignofnether.action.accept", right, y + (ROW_H - 20) / 2, 20,
                        () -> StartPosServerboundPacket.answerJoin(join.playerName, true)) - 4;
        }

        int nx = hx + HEAD + 8;
        String name = mine ? I18n.get("matchstart.reignofnether.you_name", join.playerName) : join.playerName;
        Text.draw(g, Text.fit(name, Math.max(10, Math.min(90, right - nx))), nx, y + 8, mine ? Theme.GOLD : Theme.TEXT);
        Text.small(g, I18n.get(join.isSoldier() ? "matchstart.reignofnether.role.soldier" : "matchstart.reignofnether.role.co_commander"),
                nx, y + 19, Theme.DIM);
    }

    /** An open soldier slot on a team. */
    private void renderSoldierSlot(List<StartPos> team, int x, int y, int w, int clipTop, int clipBottom) {
        var g = frame.g;
        int indent = 14;
        int rx = x + indent;
        int rw = w - indent;
        int hx = rowFrame(rx, y, rw, false, Theme.RAISED, false, seatColour(team.get(0)));
        Icon.PLUS.drawIn(g, hx, y + (ROW_H - HEAD) / 2, HEAD, HEAD, Theme.DIM);
        int right = rx + rw - ROW_PAD;
        if (y >= clipTop && y + ROW_H <= clipBottom && !StartPosClientEvents.isStarting)
            right = primaryRowButton(I18n.get("matchstart.reignofnether.action.join_soldier"), right, y + (ROW_H - 20) / 2, 20, () -> {
                leaveOwnSeat();
                StartPosServerboundPacket.joinAsSoldier(team.get(0).pos);
            }) - 4;
        int nx = hx + HEAD + 8;
        Text.draw(g, Text.fit(I18n.get("matchstart.reignofnether.seat.soldier_open"), Math.max(10, right - nx)), nx, y + 8, Theme.LABEL);
        Text.small(g, I18n.get("matchstart.reignofnether.role.soldier"), nx, y + 19, Theme.DIM);
    }

    private static int teamColour(int colorId) {
        return StartPosClientEvents.startPoses.stream().filter(sp -> sp.colorId == colorId).findFirst()
                .map(MatchStartScreen::seatColour).orElse(Theme.DIM);
    }

    private void renderHiddenRow(String name, boolean ready, int x, int y, int w) {
        var g = frame.g;
        Theme.outline(g, x, y, w, ROW_H, 1, Theme.INK);
        g.fill(x, y, x + w, y + ROW_H, Theme.RAISED);
        g.fill(x, y, x + 3, y + ROW_H, Theme.DIM);
        int hs = ROW_H - 8;
        MyRenderer.renderPlayerHead(g, name, x + 9, y + 5, hs - 2);
        Text.draw(g, Text.fit(name, w / 2), x + 8 + hs + 7, y + (ROW_H - 8) / 2, Theme.TEXT);
        String status = I18n.get(ready ? "matchstart.reignofnether.status.ready" : "matchstart.reignofnether.status.not_ready");
        Text.mediumRight(g, status, x + w - 6, y + (ROW_H - 8) / 2, ready ? Theme.OK : Theme.LABEL);
        String hidden = I18n.get("matchstart.reignofnether.faction_hidden");
        Text.smallRight(g, hidden, x + w - 14 - Text.mediumWidth(status), y + (ROW_H - 8) / 2, Theme.DIM);
    }

    private int renderSpectators(int x, int y, int w, boolean seated, int clipTop, int clipBottom) {
        var g = frame.g;
        List<String> specs = spectators();
        y = teamHeader(x, y, Theme.DIM, I18n.get("matchstart.reignofnether.spectators"), String.valueOf(specs.size()));
        if (seated && !StartPosClientEvents.isStarting && y - 16 >= clipTop && y < clipBottom) {
            String label = I18n.get("matchstart.reignofnether.spectate");
            rowButton(label, x + w, y - 16, 14, false, this::leaveOwnSeat);
        }
        if (specs.isEmpty()) {
            Text.draw(g, I18n.get("matchstart.reignofnether.no_spectators"), x, y + 2, Theme.DIM);
            return y + 14;
        }
        int cx = x;
        String me = localName();
        for (String name : specs) {
            String label = name.equals(me) ? I18n.get("matchstart.reignofnether.you_name", name) : name;
            String ping = AbstractPlayerDisplay.pingText(name);
            int cw = Text.width(label) + 13 + 4 + 12 + (ping != null ? Text.smallWidth(ping) + 6 : 0);
            if (cx + cw > x + w) {
                cx = x;
                y += 25;
            }
            Theme.bevel(g, cx, y, cw, 21, Theme.RAISED, 0, 0, 1, Theme.INK);
            if (name.equals(me))
                Theme.ring(g, cx, y, cw, 21, 1, Theme.ORANGE);
            MyRenderer.renderPlayerHead(g, name, cx + 4, y + 4, 13);
            Text.draw(g, label, cx + 21, y + 7, name.equals(me) ? Theme.GOLD : Theme.BODY);
            if (ping != null)
                drawPing(name, cx + 21 + Text.width(label) + 6, y + 8);
            cx += cw + 4;
        }
        return y + 27;
    }

    private void renderBlocker(Rect r, int openSeats) {
        var g = frame.g;
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xF22F2418);
        g.fill(r.x(), r.y(), r.right(), r.y() + Theme.BEVEL, Theme.ORANGE);
        if ((System.currentTimeMillis() / 500) % 2 == 0)
            g.fill(r.x() + 7, r.y() + 8, r.x() + 12, r.y() + 13, Theme.ORANGE);
        int right = r.right() - 6;
        if (StartPosClientEvents.isHost() && !StartPosClientEvents.isStarting)
            right = rowButton(I18n.get("matchstart.reignofnether.close_empty"), right, r.y() + 4, r.h() - 8, false,
                    StartPosServerboundPacket::closeEmptyPositions) - 6;
        Text.draw(g, Text.fit(I18n.get("matchstart.reignofnether.blocker", openSeats), right - r.x() - 18), r.x() + 17, r.y() + 7, Theme.TEXT);
    }

    // ------------------------------------------------------------------ chat

    private void renderChat(Rect p) {
        var g = frame.g;
        Theme.panel(g, p.x(), p.y(), p.w(), p.h(), PANEL_FILL);
        Text.small(g, I18n.get("matchstart.reignofnether.chat"), p.x() + 11, p.y() + 9, Theme.LABEL);

        int inputH = 26;
        int top = p.y() + 22;
        int bottom = p.bottom() - inputH - 4;
        int x = p.x() + 11;
        int w = p.w() - 22;

        int total = ChatLines.render(g, ChatLog.entries(), new Rect(x, top, w, bottom - top), chatScroll);
        chatMaxScroll = Math.max(0, total - (bottom - top) / ChatLines.LINE_H);
        chatScroll = Math.min(chatScroll, chatMaxScroll);

        // input row
        int iy = p.bottom() - inputH;
        g.fill(p.x(), iy, p.right(), p.bottom(), Theme.SUNKEN);
        g.fill(p.x(), iy, p.right(), iy + 1, Theme.INK);
        String send = I18n.get("matchstart.reignofnether.send");
        int sendW = Text.smallWidth(send) + 16;
        Rect field = new Rect(p.x() + 6, iy + 6, p.w() - 12 - sendW - 4, inputH - 12);
        g.fill(field.x(), field.y(), field.right(), field.bottom(), Theme.INK);
        g.fill(field.x(), field.y(), field.right(), field.y() + Theme.BEVEL, 0x99000000);
        chatInput.setX(field.x() + 6);
        chatInput.setY(field.y() + (field.h() - 8) / 2 + 1);
        chatInput.setWidth(field.w() - 8);
        chatInput.render(g, frame.mouseX, frame.mouseY, 0);
        if (chatInput.getValue().isEmpty() && !chatInput.isFocused())
            Text.draw(g, Text.fit(I18n.get("matchstart.reignofnether.chat.click_to_type"), field.w() - 12), field.x() + 6, field.y() + (field.h() - 8) / 2 + 1, Theme.DIM);
        frame.register(new ButtonBuilder("chat input").onLeftClick(() -> chatInput.setFocused(true)).build(), field, null, null);
        rowButton(send, p.right() - 6, field.y(), field.h(), false, this::sendChat);
    }

    private void sendChat() {
        ChatPanel.sendMessage(chatInput.getValue());
        chatInput.setValue("");
        chatScroll = 0;
    }

    // ------------------------------------------------------------------ your pick + ready

    private void renderPick(Rect p) {
        var g = frame.g;
        StartPos mine = StartPosClientEvents.getPos();
        SeatJoin join = StartPosClientEvents.getJoin();
        Faction faction = mine != null ? mine.faction : Factions.NONE;
        boolean pending = join != null && join.pending;
        boolean picked = (mine != null && faction != Factions.NONE) || (join != null && !pending);
        boolean ready = (mine != null && mine.ready) || (join != null && join.ready);

        frame.zone(p);
        Theme.outline(g, p.x(), p.y(), p.w(), p.h(), 1, Theme.INK);
        g.fill(p.x(), p.y(), p.right(), p.bottom(), Theme.PANEL);
        ResourceLocation art = rl("textures/gui/factions/" + faction.getName() + "_blur.png");
        // blurred art, zoomed to cover the whole card like the design's object-fit: cover; addon factions may ship none
        if (mine != null && picked && faction != Factions.RANDOM && minecraft.getResourceManager().getResource(art).isPresent()) {
            float zoom = Math.max(p.w() / 480f, p.h() / 253f) * 1.15f;
            int aw = Math.round(480 * zoom);
            int ah = Math.round(253 * zoom);
            Clip.push(g, p.x(), p.y(), p.w(), p.h());
            g.blit(art, p.x() + (p.w() - aw) / 2, p.y() + (p.h() - ah) / 2, aw, ah, 0, 0, 480, 253, 480, 253);
            g.fill(p.x(), p.y(), p.right(), p.bottom(), 0x9E000000);
            Clip.pop(g);
        }
        g.fillGradient(p.x(), p.y(), p.right(), p.bottom(), 0x3316151B, 0xF516151B);
        g.fill(p.x(), p.y(), p.right(), p.y() + Theme.BEVEL, ready ? Theme.OK : picked ? Theme.ORANGE : Theme.FAINT);

        String kicker, title, desc;
        if (join != null) {
            String role = join.isSoldier() ? "soldier" : "co_commander";
            kicker = I18n.get(pending ? "matchstart.reignofnether.pick.pending" : "matchstart.reignofnether.pick.joined");
            title = I18n.get("matchstart.reignofnether.role." + role);
            desc = I18n.get("matchstart.reignofnether.pick." + role + "_desc");
        } else if (mine == null) {
            kicker = I18n.get("matchstart.reignofnether.pick.spectating");
            title = I18n.get("matchstart.reignofnether.pick.no_seat");
            desc = I18n.get("matchstart.reignofnether.pick.no_seat_desc");
        } else if (!picked) {
            kicker = I18n.get("matchstart.reignofnether.pick.seated");
            title = I18n.get("matchstart.reignofnether.pick.pick_faction");
            desc = I18n.get("matchstart.reignofnether.pick.pick_faction_desc");
        } else {
            kicker = I18n.get("matchstart.reignofnether.pick.your_faction");
            title = MiscUtil.getFactionName(faction);
            desc = I18n.get("matchstart.reignofnether.faction_desc." + factionKey(faction));
        }

        int pad = 11;
        int btnH = 26;
        int btnY = p.bottom() - pad - btnH - 3;
        List<FormattedCharSequence> descLines = Text.wrap(desc, p.w() - pad * 2);
        int lh = Text.lineHeight();
        int maxLines = Math.max(0, (btnY - 6 - (p.y() + pad + 30)) / lh);
        int y = btnY - 6 - Math.min(maxLines, descLines.size()) * lh;
        for (int i = 0; i < Math.min(maxLines, descLines.size()); i++)
            Text.draw(g, descLines.get(i), p.x() + pad, y + i * lh, Theme.DESC);
        Text.heading(g, title.toUpperCase(Locale.ROOT), p.x() + pad, y - 19, Theme.TEXT, Text.px(32));
        Text.medium(g, kicker, p.x() + pad, y - 30, Theme.ORANGE);

        boolean canReady = picked && !(StartPosClientEvents.isStarting && !ready);
        String label = mine == null && join == null ? I18n.get("matchstart.reignofnether.ready.take_seat")
                : pending ? I18n.get("matchstart.reignofnether.ready.pending")
                : !picked ? I18n.get("matchstart.reignofnether.ready.pick_faction")
                : ready ? I18n.get("matchstart.reignofnether.ready.unready") : I18n.get("matchstart.reignofnether.ready.ready_up");
        Rect btn = new Rect(p.x() + pad, btnY, p.w() - pad * 2, btnH);
        boolean hover = canReady && frame.hovered(btn);
        Theme.readyButton(g, btn.x(), btn.y(), btn.w(), btn.h(),
                ready ? Theme.ReadyState.READY : canReady ? Theme.ReadyState.CAN_READY : Theme.ReadyState.DISABLED, hover);
        // ready: a check and READY, turning into UNREADY on hover
        if (ready && hover)
            label = I18n.get("matchstart.reignofnether.ready.unready_short");
        else if (ready)
            label = I18n.get("matchstart.reignofnether.ready.ready");
        int lw = Text.mediumWidth(label) + (ready && !hover ? 12 + 6 : 0);
        int lx = btn.centerX() - lw / 2;
        if (ready && !hover) {
            Icon.CHECK.draw(g, lx, btn.y() + (btnH - 12) / 2, 12, Theme.OK_TEXT);
            lx += 12 + 6;
        }
        Text.medium(g, label, lx, btn.y() + (btnH - 9) / 2,
                ready ? Theme.OK_TEXT : canReady ? Theme.ON_ORANGE : Theme.DIM);
        frame.register(new ButtonBuilder("ready").isEnabled(() -> canReady).onLeftClick(() -> toggleReady(mine)).build(), btn, null, null);
    }

    // ------------------------------------------------------------------ overlays

    private void renderCountdown(int w) {
        var g = frame.g;
        int seconds = Math.max(0, (MatchStartClientEvents.getCountdownTicks() + 19) / 20);
        String label = I18n.get("matchstart.reignofnether.all_ready");
        String title = I18n.get("matchstart.reignofnether.starting_in", seconds);
        StartPos mine = StartPosClientEvents.getPos();
        boolean canCancel = mine != null && mine.ready;
        String unready = I18n.get("matchstart.reignofnether.ready.unready_short");
        float titleScale = Text.px(34);
        int titleW = Text.headingWidth(title, titleScale);
        int bw = canCancel ? Text.smallWidth(unready) + 14 : 0;
        int pw = 12 + Text.mediumWidth(label) + 12 + titleW + 12 + bw + (canCancel ? 12 : 0);
        int ph = 34;
        int x = (w - pw) / 2;
        int y = M + 4;
        // shadowed text sits slightly above z 0, so it would show through a banner drawn at the same level
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        frame.zone(x, y, pw, ph);
        Theme.bevel(g, x, y, pw, ph, 0xFF24232C, Theme.OK, 0, 2, Theme.INK);
        Text.medium(g, label, x + 12, y + 12, Theme.OK);
        Text.heading(g, title, x + 24 + Text.mediumWidth(label), y + 11, Theme.TEXT, titleScale);
        if (canCancel)
            rowButton(unready, x + pw - 12, y + 8, 18, false, () -> toggleReady(mine));
        g.pose().popPose();
    }

    private void renderRulesModal(int w, int h) {
        var g = frame.g;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(0, 0, w, h, 0xCC050203);
        frame.register(new ButtonBuilder("close rules").onLeftClick(() -> GameruleClient.gamerulesMenuOpen = false).build(),
                new Rect(0, 0, w, h), null, null);

        List<SettingRow> rules = GameruleClient.ruleRows();
        int colW = 253;
        int pad = 12;
        int maxH = h - 2 * M - 60;
        Rect size = SettingRows.measure(rules, colW, maxH);
        int pw = Math.min(w - 2 * M, size.w() + pad * 2);
        int ph = size.h() + pad * 2 + 30;
        int x = (w - pw) / 2;
        int y = (h - ph) / 2;
        Theme.popup(g, x, y, pw, ph, Theme.ORANGE);
        frame.register(new ButtonBuilder("rules panel").build(), new Rect(x, y, pw, ph), null, null);
        Text.heading(g, I18n.get("matchstart.reignofnether.game_rules").toUpperCase(Locale.ROOT), x + pad, y + pad - 1, Theme.GOLD, Text.px(26));
        String note = I18n.get(StartPosClientEvents.isHost() ? "matchstart.reignofnether.rules_host" : "matchstart.reignofnether.rules_view");
        Rect close = new Rect(x + pw - pad - 18, y + pad - 4, 18, 18);
        Text.smallRight(g, note, close.x() - 8, y + pad + 1, StartPosClientEvents.isHost() ? Theme.OK : Theme.LABEL);
        Theme.button(g, close.x(), close.y(), 18, 18, frame.hovered(close));
        Icon.CLOSE.drawIn(g, close.x(), close.y(), 18, 18, frame.hovered(close) ? Theme.GOLD : Theme.BODY);
        frame.register(new ButtonBuilder("close").onLeftClick(() -> GameruleClient.gamerulesMenuOpen = false).build(), close, null, null);
        SettingRows.render(frame, rules, x + pad, y + pad + 26, colW, maxH);
        g.pose().popPose();
    }

    private void drawPing(String name, int x, int y) {
        String ping = AbstractPlayerDisplay.pingText(name);
        if (ping != null)
            Text.small(frame.g, ping, x, y, AbstractPlayerDisplay.pingColour(name));
    }

    // ------------------------------------------------------------------ actions

    private void claim(StartPos sp) {
        if (Minecraft.getInstance().player == null || !sp.enabled || StartPosClientEvents.isStarting)
            return;
        if (!taken(sp))
            StartPosServerboundPacket.reservePos(sp.pos, StartPosClientEvents.selectedFaction);
    }

    private void setSeatOpen(StartPos sp, boolean open) {
        if (open)
            StartPosServerboundPacket.enablePos(sp.pos);
        else
            StartPosServerboundPacket.disablePos(sp.pos);
    }

    private void leaveOwnSeat() {
        StartPos current = StartPosClientEvents.getPos();
        if (current != null) {
            StartPosServerboundPacket.unreservePos(current.pos);
            StartPosClientEvents.selectedFaction = Factions.NONE;
        }
        if (StartPosClientEvents.getJoin() != null)
            StartPosServerboundPacket.leaveJoin();
    }

    private void pickFaction(StartPos pos, Faction faction) {
        if (!pos.playerName.equals(localName()))
            return;
        Faction next = pos.faction == faction ? Factions.NONE : faction;
        StartPosClientEvents.selectedFaction = next;
        StartPosServerboundPacket.reservePos(pos.pos, next);
    }

    private void toggleReady(@Nullable StartPos pos) {
        if (pos == null) {
            SeatJoin join = StartPosClientEvents.getJoin();
            if (join != null)
                toggleReady(join.ready);
            return;
        }
        toggleReady(pos.ready);
    }

    private void toggleReady(boolean ready) {
        if (ready)
            StartPosServerboundPacket.unreadyPlayer();
        else
            StartPosServerboundPacket.readyPlayer();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (frame == null)
            return super.mouseClicked(mouseX, mouseY, button);
        int ux = frame.scale.toUnits(mouseX);
        int uy = frame.scale.toUnits(mouseY);
        boolean inInput = !GameruleClient.gamerulesMenuOpen && chatInput.isMouseOver(ux, uy);
        chatInput.setFocused(inInput);
        if (inInput)
            return chatInput.mouseClicked(ux, uy, button);
        frame.click(ux, uy, button == GLFW.GLFW_MOUSE_BUTTON_1);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (frame == null)
            return false;
        int ux = frame.scale.toUnits(mouseX);
        int uy = frame.scale.toUnits(mouseY);
        if (rosterRect.contains(ux, uy))
            rosterScroll = Math.max(0, Math.min(rosterMaxScroll, rosterScroll - (int) (delta * 18)));
        else if (chatRect.contains(ux, uy))
            chatScroll = Math.max(0, Math.min(chatMaxScroll, chatScroll + (int) (delta * 3)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (chatInput.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                sendChat();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                chatInput.setFocused(false);
                return true;
            }
            return chatInput.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && GameruleClient.gamerulesMenuOpen) {
            GameruleClient.gamerulesMenuOpen = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && HostPopup.open) {
            HostPopup.open = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_F9) {
            MatchStartClientEvents.dismiss();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_T || keyCode == GLFW.GLFW_KEY_ENTER) {
            chatInput.setFocused(true);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_SLASH) {
            chatInput.setFocused(true);
            chatInput.setValue("/");
            chatInput.moveCursorToEnd();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return chatInput.isFocused() ? chatInput.charTyped(codePoint, modifiers) : super.charTyped(codePoint, modifiers);
    }

    @Override
    public void tick() {
        chatInput.tick();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        MinimapClientEvents.suppressViewQuad = false;
        MinimapClientEvents.setMapLocked(priorMapLocked);
        MinimapClientEvents.setLargeMap(priorLargeMap);
        GameruleClient.gamerulesMenuOpen = false;
        super.removed();
    }
}
