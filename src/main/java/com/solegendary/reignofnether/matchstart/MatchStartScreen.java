package com.solegendary.reignofnether.matchstart;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.gamemode.ClientGameModeHelper;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.startpos.StartPos;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.startpos.StartPosServerboundPacket;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MatchStartScreen extends Screen {

    private static final ResourceLocation ICON_FRAME      = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png");
    private static final ResourceLocation ICON_FRAME_SEL  = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_selected.png");
    private static final ResourceLocation VILLAGER_ICON   = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png");
    private static final ResourceLocation MONSTER_ICON    = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png");
    private static final ResourceLocation PIGLIN_ICON     = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/grunt.png");
    private static final ResourceLocation TICK_ICON       = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/tick.png");
    private static final ResourceLocation CROSS_ICON      = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross.png");

    private static final int BG_PANEL     = 0xA0000000;
    private static final int BG_ICON      = 0x64000000;
    private static final int ACCENT       = 0xFFE6C76A;
    private static final int TEXT_DIM     = 0xFFB0B8C0;
    private static final int TEXT_NORMAL  = 0xFFFFFFFF;
    private static final int READY_COL    = 0xFF6CE26C;

    private static final int MARGIN = 12;
    private static final int HEADER_H = 32;
    private static final int BOTTOM_H = 28;
    private static final int CHAT_H = 140;
    private static final int SETTINGS_H = 36;
    private static final int FRAME_SIZE = 22;
    private static final int ICON_SIZE = 14;
    private static final int ROW_H = 26;

    private final List<FactionHit> factionHits = new ArrayList<>();
    private final List<ReadyHit> readyHits = new ArrayList<>();
    private final List<RowHit> rowHits = new ArrayList<>();
    private final List<Button> hudButtons = new ArrayList<>();
    private int mapL, mapT, mapR, mapB;
    private int rosL, rosT, rosR, rosB;
    private int setL, setT, setR, setB;
    private int closeBtnX, closeBtnY;
    private int spectateBtnX1, spectateBtnY1, spectateBtnX2, spectateBtnY2;
    private boolean spectateBtnVisible;

    private EditBox chatInput;
    private int chatL, chatT, chatR, chatB;
    private int chatScroll = 0;
    private int chatTotalLines = 0;
    private int chatViewLines = 0;
    private net.minecraft.client.gui.components.Button closeButton;

    private boolean priorLargeMap;
    private boolean priorMapLocked;

    private int rosterScroll = 0;
    private int rosterContentH = 0;
    private int rosterViewH = 0;

    private record FactionHit(StartPos pos, Faction faction, int x, int y) {}
    private record ReadyHit(StartPos pos, int x, int y) {}
    private record RowHit(StartPos pos, int x1, int y1, int x2, int y2) {}

    public MatchStartScreen() {
        super(Component.translatable("matchstart.reignofnether.title"));
    }

    @Override
    protected void init() {
        super.init();

        priorLargeMap = MinimapClientEvents.isLargeMap();
        priorMapLocked = MinimapClientEvents.isMapLocked();
        MinimapClientEvents.setLargeMap(true);
        MinimapClientEvents.suppressViewQuad = true;
        recentreMapOnStartPoses();

        int chatBLocal = this.height - BOTTOM_H - MARGIN - 6;
        int chatLLocal = this.width / 2 + 3;
        int chatRLocal = this.width - MARGIN;

        chatInput = new EditBox(this.font, chatLLocal + 8, chatBLocal - 22,
                chatRLocal - chatLLocal - 16, 18,
                Component.translatable("matchstart.reignofnether.chat.placeholder"));
        chatInput.setBordered(true);
        chatInput.setMaxLength(256);
        chatInput.setCanLoseFocus(true);
        chatInput.setFocused(false);
        addRenderableWidget(chatInput);

        int closeW = 20;
        int closeH = 20;
        int closeX = this.width - MARGIN - closeW - 6;
        int closeY = MARGIN + (HEADER_H - closeH) / 2;
        closeButton = net.minecraft.client.gui.components.Button.builder(
                        Component.literal("✕"),
                        b -> MatchStartClientEvents.dismiss())
                .pos(closeX, closeY).size(closeW, closeH).build();
        addRenderableWidget(closeButton);
    }

    private void recentreMapOnStartPoses() {
        Minecraft mc = Minecraft.getInstance();
        List<StartPos> poses = StartPosClientEvents.startPoses;
        if (poses.isEmpty()) {
            if (mc.player != null) {
                MinimapClientEvents.forceMapCentre((int) mc.player.getX(), (int) mc.player.getZ());
            }
            MinimapClientEvents.setMapLocked(false);
            return;
        }
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (StartPos sp : poses) {
            minX = Math.min(minX, sp.pos.getX());
            maxX = Math.max(maxX, sp.pos.getX());
            minZ = Math.min(minZ, sp.pos.getZ());
            maxZ = Math.max(maxZ, sp.pos.getZ());
        }
        MinimapClientEvents.forceMapCentre((minX + maxX) / 2, (minZ + maxZ) / 2);
        MinimapClientEvents.setMapLocked(true);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderDirtBackground(g);

        factionHits.clear();
        readyHits.clear();
        rowHits.clear();
        hudButtons.clear();

        Minecraft mcLocal = Minecraft.getInstance();
        boolean showSettings = mcLocal.player != null && mcLocal.player.hasPermissions(2);

        int leftBottom = this.height - BOTTOM_H - MARGIN - 6;
        mapL = MARGIN;
        mapT = MARGIN + HEADER_H + 6;
        mapR = this.width / 2 - 3;
        mapB = showSettings ? leftBottom - SETTINGS_H - 6 : leftBottom;

        setL = mapL;
        setT = mapB + 6;
        setR = mapR;
        setB = leftBottom;

        int rightTotalH = leftBottom - mapT - 6;
        int rosterH = rightTotalH * 60 / 100;
        chatL = this.width / 2 + 3;
        chatR = this.width - MARGIN;
        rosL = chatL;
        rosT = mapT;
        rosR = chatR;
        rosB = rosT + rosterH;
        chatT = rosB + 6;
        chatB = leftBottom;

        renderHeader(g, mouseX, mouseY);
        renderMap(g, mapL, mapT, mapR, mapB, mouseX, mouseY);
        renderRoster(g, rosL, rosT, rosR, rosB, mouseX, mouseY);
        renderChatCard(g, chatL, chatT, chatR, chatB, mouseX, mouseY);
        if (showSettings) {
            renderSettings(g, setL, setT, setR, setB, mouseX, mouseY);
        }
        renderBottomStrip(g);

        if (GameruleClient.gamerulesMenuOpen) {
            renderGamerulesPopover(g, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partialTick);

        for (Button b : hudButtons) {
            if (b.isMouseOver(mouseX, mouseY)) {
                b.renderTooltip(g, mouseX, mouseY);
            }
        }
    }

    private void renderHeader(GuiGraphics g, int lastMouseX, int lastMouseY) {
        MyRenderer.renderFrameWithBg(g, MARGIN, MARGIN, this.width - MARGIN * 2, HEADER_H, BG_PANEL);

        g.drawString(this.font,
                Component.translatable("matchstart.reignofnether.title").getString(),
                MARGIN + 10, MARGIN + 12, ACCENT, false);

        String mode = Component.translatable("matchstart.reignofnether.mode",
                ClientGameModeHelper.gameMode == null ? "—" : ClientGameModeHelper.gameMode.name()).getString();
        int modeW = this.font.width(mode);
        g.drawString(this.font, mode, this.width / 2 - modeW / 2, MARGIN + 12, TEXT_NORMAL, false);

        String cd = countdownLabel();
        int cdW = this.font.width(cd);
        int cdCol = StartPosClientEvents.isStarting ? READY_COL : TEXT_DIM;
        int cdRight = closeButton != null ? closeButton.getX() - 10 : this.width - MARGIN - 10;
        g.drawString(this.font, cd, cdRight - cdW, MARGIN + 12, cdCol, false);
    }

    private void renderMap(GuiGraphics g, int x1, int y1, int x2, int y2, int mx, int my) {
        MyRenderer.renderFrameWithBg(g, x1, y1, x2 - x1, y2 - y1, BG_PANEL);

        g.drawString(this.font,
                Component.translatable("matchstart.reignofnether.map_preview").getString(),
                x1 + 10, y1 + 8, ACCENT, false);

        int drawL = x1 + 8;
        int drawT = y1 + 22;
        int drawR = x2 - 8;
        int drawB = y2 - 8;
        int drawW = drawR - drawL;
        int drawH = drawB - drawT;

        if (MinimapClientEvents.isMapReady()) {
            MinimapClientEvents.renderMapInto(g, drawL, drawT, drawW, drawH);
        } else {
            g.fill(drawL, drawT, drawR, drawB, 0xFF0A0E12);
            String msg = Component.translatable("matchstart.reignofnether.map_loading").getString();
            g.drawString(this.font, msg,
                    (drawL + drawR) / 2 - this.font.width(msg) / 2,
                    (drawT + drawB) / 2 - 4, TEXT_DIM, false);
        }

        Minecraft mcRef = Minecraft.getInstance();
        String localName = mcRef.player != null ? mcRef.player.getName().getString() : "";
        boolean isOp = mcRef.player != null && mcRef.player.hasPermissions(4);
        int btnFrame = Button.DEFAULT_ICON_FRAME_SIZE;

        for (StartPos sp : StartPosClientEvents.startPoses) {
            Vector2f screen = MinimapClientEvents.worldToRect(
                    sp.pos.getX(), sp.pos.getZ(), drawL, drawT, drawW, drawH);
            int px = (int) screen.x;
            int pz = (int) screen.y;

            if (px < drawL - btnFrame || px > drawR + btnFrame ||
                pz < drawT - btnFrame || pz > drawB + btnFrame) {
                continue;
            }

            Button spButton = sp.getButton(localName, isOp);
            int bx = px - btnFrame / 2;
            int by = pz - btnFrame / 2;
            spButton.render(g, bx, by, mx, my);
            hudButtons.add(spButton);

            if (!sp.playerName.isBlank()) {
                int w = this.font.width(sp.playerName);
                g.drawString(this.font, sp.playerName, px - w / 2, pz + btnFrame / 2 + 2, 0xFFE0E0E0, false);
            }
        }
    }

    private void renderRoster(GuiGraphics g, int x1, int y1, int x2, int y2, int mx, int my) {
        MyRenderer.renderFrameWithBg(g, x1, y1, x2 - x1, y2 - y1, BG_PANEL);

        g.drawString(this.font,
                Component.translatable("matchstart.reignofnether.roster").getString(),
                x1 + 10, y1 + 8, ACCENT, false);

        Minecraft mc = Minecraft.getInstance();
        String localName = mc.player != null ? mc.player.getName().getString() : "";

        Map<Integer, List<StartPos>> byColor = new LinkedHashMap<>();
        Set<String> seatedPlayers = new HashSet<>();
        for (StartPos sp : StartPosClientEvents.startPoses) {
            if (!sp.enabled) continue;
            byColor.computeIfAbsent(sp.colorId, k -> new ArrayList<>()).add(sp);
            if (!sp.playerName.isBlank()) seatedPlayers.add(sp.playerName);
        }

        List<String> spectators = new ArrayList<>();
        if (mc.level != null) {
            for (AbstractClientPlayer p : mc.level.players()) {
                String n = p.getName().getString();
                if (!seatedPlayers.contains(n)) spectators.add(n);
            }
        }

        int contentTop = y1 + 24;
        int viewTop = contentTop;
        int viewBottom = y2 - 4;
        rosterViewH = viewBottom - viewTop;

        boolean localSeated = mc.player != null && seatedPlayers.contains(localName);
        int contentH = 0;
        for (List<StartPos> team : byColor.values()) {
            contentH += 12 + team.size() * ROW_H + 4;
        }
        contentH += 12 + Math.max(spectators.size(), 1) * ROW_H;
        if (localSeated) contentH += ROW_H + 2;
        rosterContentH = contentH;

        int maxScroll = Math.max(0, contentH - rosterViewH);
        if (rosterScroll > maxScroll) rosterScroll = maxScroll;
        if (rosterScroll < 0) rosterScroll = 0;

        g.enableScissor(x1 + 6, viewTop, x2 - 6, viewBottom);

        int y = contentTop - rosterScroll;
        int teamIdx = 1;
        for (Map.Entry<Integer, List<StartPos>> entry : byColor.entrySet()) {
            int teamCol = 0xFF000000 | entry.getValue().get(0).getHexColor();
            g.fill(x1 + 8, y, x1 + 12, y + 10, teamCol);
            g.drawString(this.font,
                    Component.translatable("matchstart.reignofnether.team", teamIdx).getString(),
                    x1 + 16, y + 1, 0xFFE0E0E0, false);
            y += 12;
            for (StartPos sp : entry.getValue()) {
                renderSlotRow(g, sp, localName, x1 + 8, y, x2 - x1 - 16, mx, my);
                y += ROW_H;
            }
            y += 4;
            teamIdx++;
        }

        if (byColor.isEmpty() && spectators.isEmpty()) {
            g.drawString(this.font,
                    Component.translatable("matchstart.reignofnether.no_positions").getString(),
                    x1 + 10, contentTop, TEXT_DIM, false);
        }

        g.fill(x1 + 8, y, x1 + 12, y + 10, 0xFF8E8E86);
        g.drawString(this.font,
                Component.translatable("matchstart.reignofnether.spectators").getString(),
                x1 + 16, y + 1, 0xFFE0E0E0, false);
        y += 12;
        spectateBtnVisible = false;
        if (localSeated) {
            int rowX1 = x1 + 8;
            int rowX2 = x2 - 8;
            int rowY2 = y + ROW_H - 4;
            g.fill(rowX1, y, rowX2, rowY2, 0x40404040);
            boolean hovered = mx >= rowX1 && mx <= rowX2 && my >= y && my <= rowY2;
            if (hovered) g.fill(rowX1, y, rowX2, rowY2, 0x32FFFFFF);
            String join = Component.translatable("matchstart.reignofnether.join_spectators").getString();
            g.drawString(this.font, join, rowX1 + 8, y + 7, ACCENT, false);
            spectateBtnX1 = rowX1;
            spectateBtnY1 = y;
            spectateBtnX2 = rowX2;
            spectateBtnY2 = rowY2;
            spectateBtnVisible = true;
            y += ROW_H + 2;
        }
        for (String name : spectators) {
            renderSpectatorRow(g, name, name.equals(localName), x1 + 8, y, x2 - x1 - 16);
            y += ROW_H;
        }

        g.disableScissor();

        if (maxScroll > 0) {
            int trackX = x2 - 6;
            g.fill(trackX, viewTop, trackX + 2, viewBottom, 0x40000000);
            int thumbH = Math.max(20, rosterViewH * rosterViewH / contentH);
            int thumbY = viewTop + (rosterViewH - thumbH) * rosterScroll / maxScroll;
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, ACCENT);
        }
    }

    private void renderSlotRow(GuiGraphics g, StartPos sp, String localName,
                               int x, int y, int width, int mx, int my) {
        boolean mine = !sp.playerName.isBlank() && sp.playerName.equals(localName);
        boolean empty = sp.playerName.isBlank();
        int tint = (0x60000000) | (sp.getHexColor() & 0xFFFFFF);
        int rowBottom = y + ROW_H - 2;
        g.fill(x, y, x + width, rowBottom, tint);
        if (mine) {
            g.fill(x, y, x + 2, rowBottom, ACCENT);
        }

        int tileY = y + (ROW_H - FRAME_SIZE) / 2;
        int readyX = x + width - FRAME_SIZE - 6;
        int factionTotalW = FRAME_SIZE * 3 + 2 * 2;
        int factionStartX = readyX - 8 - factionTotalW;

        int headX = x + 6;
        int innerOffset = (FRAME_SIZE - ICON_SIZE) / 2;
        if (empty) {
            MyRenderer.renderIconFrameWithBg(g, ICON_FRAME, headX, tileY, FRAME_SIZE, BG_ICON);
            String plus = "+";
            float scale = 1.15f;
            int plusW = this.font.width(plus);
            int plusH = this.font.lineHeight;
            g.pose().pushPose();
            g.pose().translate(headX + FRAME_SIZE / 2.0f, tileY + FRAME_SIZE / 2.0f + 1, 0);
            g.pose().scale(scale, scale, 1f);
            g.drawString(this.font, plus, -plusW / 2, -plusH / 2, ACCENT, false);
            g.pose().popPose();
        } else {
            MyRenderer.renderIconFrameWithBg(g, ICON_FRAME, headX, tileY, FRAME_SIZE, BG_ICON);
            renderPlayerHead(g, sp.playerName, headX + innerOffset, tileY + innerOffset);
        }

        String name = empty
                ? Component.translatable("matchstart.reignofnether.empty_slot").getString()
                : sp.playerName;
        int nameCol = empty ? TEXT_DIM : TEXT_NORMAL;
        int nameX = headX + FRAME_SIZE + 6;
        int nameMaxW = factionStartX - nameX - 6;
        String drawnName = this.font.plainSubstrByWidth(name, nameMaxW);
        g.drawString(this.font, drawnName, nameX, tileY + (FRAME_SIZE - this.font.lineHeight) / 2 + 1, nameCol, false);

        Faction[] order = { Faction.VILLAGERS, Faction.MONSTERS, Faction.PIGLINS };
        int currentX = factionStartX;
        for (Faction f : order) {
            renderFactionTile(g, sp, f, currentX, tileY, localName, mx, my);
            currentX += FRAME_SIZE + 2;
        }

        renderReadyTile(g, sp, readyX, tileY, localName, mx, my);

        if (sp.enabled && (empty || mine)) {
            int rowHitRight = factionStartX - 4;
            rowHits.add(new RowHit(sp, x, y, rowHitRight, rowBottom));
        }
    }

    private void renderSpectatorRow(GuiGraphics g, String name, boolean isLocal,
                                    int x, int y, int width) {
        int rowBottom = y + ROW_H - 2;
        g.fill(x, y, x + width, rowBottom, 0x40808080);
        if (isLocal) g.fill(x, y, x + 2, rowBottom, ACCENT);
        renderPlayerHead(g, name, x + 6, y + (ROW_H - ICON_SIZE) / 2);
        g.drawString(this.font, name, x + 6 + ICON_SIZE + 4, y + 9, TEXT_DIM, false);
        String tag = Component.translatable("matchstart.reignofnether.spectator_tag").getString();
        int w = this.font.width(tag);
        g.drawString(this.font, tag, x + width - w - 8, y + 9, TEXT_DIM, false);
    }

    private void renderPlayerHead(GuiGraphics g, String playerName, int x, int y) {
        if (playerName == null || playerName.isBlank()) {
            MyRenderer.renderIcon(g, ICON_FRAME, x, y, ICON_SIZE);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer p = null;
        if (mc.level != null) {
            for (AbstractClientPlayer candidate : mc.level.players()) {
                if (candidate.getName().getString().equals(playerName)) {
                    p = candidate;
                    break;
                }
            }
        }
        if (p != null && p.isSkinLoaded()) {
            ResourceLocation skin = p.getSkinTextureLocation();
            g.blit(skin, x, y, ICON_SIZE, ICON_SIZE, 8.0f, 8.0f, 8, 8, 64, 64);
            g.blit(skin, x, y, ICON_SIZE, ICON_SIZE, 40.0f, 8.0f, 8, 8, 64, 64);
        } else {
            MyRenderer.renderIcon(g, ICON_FRAME, x, y, ICON_SIZE);
        }
    }

    private void renderFactionTile(GuiGraphics g, StartPos sp, Faction f,
                                   int x, int y, String localName, int mx, int my) {
        boolean mine = !sp.playerName.isBlank() && sp.playerName.equals(localName);
        boolean selected = sp.faction == f && !sp.playerName.isBlank();
        boolean canPick = sp.enabled && mine;

        MyRenderer.renderIconFrameWithBg(g, ICON_FRAME, x, y, FRAME_SIZE, BG_ICON);
        ResourceLocation icon = factionIcon(f);
        int innerOffset = (FRAME_SIZE - ICON_SIZE) / 2;
        MyRenderer.renderIcon(g, icon, x + innerOffset, y + innerOffset, ICON_SIZE);
        if (!canPick) {
            g.fill(x + 1, y + 1, x + FRAME_SIZE - 1, y + FRAME_SIZE - 1, 0xA0000000);
        }
        if (selected) {
            MyRenderer.renderIcon(g, ICON_FRAME_SEL, x - 1, y - 1, FRAME_SIZE + 2);
        }
        boolean hovered = mx >= x && mx <= x + FRAME_SIZE && my >= y && my <= y + FRAME_SIZE;
        if (canPick && hovered) {
            g.fill(x + 1, y + 1, x + FRAME_SIZE - 1, y + FRAME_SIZE - 1, 0x32FFFFFF);
        }
        if (canPick) factionHits.add(new FactionHit(sp, f, x, y));
    }

    private void renderReadyTile(GuiGraphics g, StartPos sp, int x, int y,
                                  String localName, int mx, int my) {
        boolean mine = !sp.playerName.isBlank() && sp.playerName.equals(localName);
        boolean otherClaimed = !sp.playerName.isBlank() && !mine;
        boolean canToggle = mine;

        MyRenderer.renderIconFrameWithBg(g, ICON_FRAME, x, y, FRAME_SIZE, BG_ICON);
        ResourceLocation icon = sp.ready ? TICK_ICON : CROSS_ICON;
        int innerOffset = (FRAME_SIZE - ICON_SIZE) / 2;
        MyRenderer.renderIcon(g, icon, x + innerOffset, y + innerOffset, ICON_SIZE);

        if (sp.ready) {
            MyRenderer.renderIcon(g, ICON_FRAME_SEL, x - 1, y - 1, FRAME_SIZE + 2);
        }
        if (otherClaimed || sp.playerName.isBlank()) {
            g.fill(x + 1, y + 1, x + FRAME_SIZE - 1, y + FRAME_SIZE - 1, 0xA0000000);
        }
        boolean hovered = mx >= x && mx <= x + FRAME_SIZE && my >= y && my <= y + FRAME_SIZE;
        if (canToggle && hovered) {
            g.fill(x + 1, y + 1, x + FRAME_SIZE - 1, y + FRAME_SIZE - 1, 0x32FFFFFF);
        }
        if (canToggle) readyHits.add(new ReadyHit(sp, x, y));
    }

    private void renderSettings(GuiGraphics g, int x1, int y1, int x2, int y2, int mx, int my) {
        MyRenderer.renderFrameWithBg(g, x1, y1, x2 - x1, y2 - y1, BG_PANEL);

        int btnY = y1 + (y2 - y1 - FRAME_SIZE) / 2;
        int bx = x1 + 10;

        Button modeBtn = ClientGameModeHelper.getButton();
        if (modeBtn != null) {
            modeBtn.render(g, bx, btnY, mx, my);
            hudButtons.add(modeBtn);
        }
        bx += FRAME_SIZE + 8;

        Button grBtn = GameruleClient.getGamerulesButton();
        if (grBtn != null && !grBtn.isHidden.get()) {
            grBtn.render(g, bx, btnY, mx, my);
            hudButtons.add(grBtn);
        }
    }

    private void renderGamerulesPopover(GuiGraphics g, int mx, int my) {
        int panelWidth = 145;
        int panelHeight = 262;
        int xTR = setL + panelWidth + 8;
        int yTR = setT - 6 - panelHeight + 36;
        List<Button> buttons = GameruleClient.renderGamerulesGUI(g, xTR, yTR, mx, my);
        hudButtons.addAll(buttons);
    }

    private void renderChatCard(GuiGraphics g, int x1, int y1, int x2, int y2, int mouseX, int mouseY) {
        MyRenderer.renderFrameWithBg(g, x1, y1, x2 - x1, y2 - y1, BG_PANEL);
        g.drawString(this.font,
                Component.translatable("matchstart.reignofnether.chat").getString(),
                x1 + 10, y1 + 6, ACCENT, false);

        int textTop = y1 + 18;
        int textBottom = y2 - 26;
        int maxWidth = (x2 - x1) - 16;

        java.util.List<net.minecraft.util.FormattedCharSequence> allLines = new ArrayList<>();
        for (Component c : MatchStartClientEvents.getChatBuffer()) {
            allLines.addAll(this.font.split(c, maxWidth));
        }

        int lineH = this.font.lineHeight + 1;
        chatTotalLines = allLines.size();
        chatViewLines = Math.max(1, (textBottom - textTop) / lineH);
        int maxScroll = Math.max(0, chatTotalLines - chatViewLines);
        if (chatScroll > maxScroll) chatScroll = maxScroll;
        if (chatScroll < 0) chatScroll = 0;

        g.enableScissor(x1 + 4, textTop, x2 - 4, textBottom);
        int bottomIdx = allLines.size() - 1 - chatScroll;
        int y = textBottom - lineH;
        for (int i = bottomIdx; i >= 0 && y >= textTop - lineH; i--) {
            g.drawString(this.font, allLines.get(i), x1 + 8, y, TEXT_NORMAL, false);
            y -= lineH;
        }
        g.disableScissor();

        if (chatTotalLines > chatViewLines) {
            int trackX = x2 - 6;
            g.fill(trackX, textTop, trackX + 2, textBottom, 0x40000000);
            int thumbH = Math.max(20, (textBottom - textTop) * chatViewLines / chatTotalLines);
            int travel = (textBottom - textTop) - thumbH;
            int thumbY = textBottom - thumbH - travel * chatScroll / maxScroll;
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, ACCENT);
        }

        if (chatInput != null && !chatInput.isFocused() && chatInput.getValue().isEmpty()) {
            g.drawString(this.font,
                    Component.translatable("matchstart.reignofnether.chat.click_to_type").getString(),
                    chatInput.getX() + 4, chatInput.getY() + 5, TEXT_DIM, false);
        }
    }

    private void sendChatFromInput() {
        if (chatInput == null) return;
        String text = chatInput.getValue().trim();
        chatInput.setValue("");
        chatInput.setFocused(false);
        if (text.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        mc.gui.getChat().addRecentChat(text);
        if (text.startsWith("/")) {
            mc.player.connection.sendCommand(text.substring(1));
        } else {
            mc.player.connection.sendChat(text);
        }
    }

    private void renderBottomStrip(GuiGraphics g) {
        int y = this.height - BOTTOM_H - MARGIN;
        MyRenderer.renderFrameWithBg(g, MARGIN, y, this.width - MARGIN * 2, BOTTOM_H, BG_PANEL);

        StartPos current = StartPosClientEvents.getPos();
        String hint;
        if (current == null) {
            hint = Component.translatable("matchstart.reignofnether.hint.pick_pos").getString();
        } else if (current.faction == Faction.NONE) {
            hint = Component.translatable("matchstart.reignofnether.hint.pick_faction").getString();
        } else if (!current.ready) {
            hint = Component.translatable("matchstart.reignofnether.hint.ready_up").getString();
        } else {
            hint = Component.translatable("matchstart.reignofnether.hint.waiting").getString();
        }
        g.drawString(this.font, hint, MARGIN + 10, y + 10, ACCENT, false);

        String close = Component.translatable("matchstart.reignofnether.hint.close").getString();
        int w = this.font.width(close);
        g.drawString(this.font, close, this.width - MARGIN - 10 - w, y + 10, TEXT_DIM, false);
    }

    private String countdownLabel() {
        if (StartPosClientEvents.isStarting) {
            int seconds = Math.max(0, (MatchStartClientEvents.getCountdownTicks() + 19) / 20);
            return Component.translatable("matchstart.reignofnether.starting", seconds).getString();
        }
        int ready = 0, total = 0;
        for (StartPos sp : StartPosClientEvents.startPoses) {
            if (!sp.enabled) continue;
            total++;
            if (sp.ready && !sp.playerName.isBlank()) ready++;
        }
        return Component.translatable("matchstart.reignofnether.ready_count", ready, total).getString();
    }

    private static ResourceLocation factionIcon(Faction faction) {
        return switch (faction) {
            case VILLAGERS -> VILLAGER_ICON;
            case MONSTERS -> MONSTER_ICON;
            case PIGLINS -> PIGLIN_ICON;
            default -> ICON_FRAME;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int mx = (int) mouseX, my = (int) mouseY;
        if (mx >= rosL && mx <= rosR && my >= rosT && my <= rosB) {
            rosterScroll -= (int) (delta * 18);
            int maxScroll = Math.max(0, rosterContentH - rosterViewH);
            if (rosterScroll > maxScroll) rosterScroll = maxScroll;
            if (rosterScroll < 0) rosterScroll = 0;
            return true;
        }
        if (mx >= chatL && mx <= chatR && my >= chatT && my <= chatB) {
            chatScroll += (int) (delta * 3);
            int maxScroll = Math.max(0, chatTotalLines - chatViewLines);
            if (chatScroll > maxScroll) chatScroll = maxScroll;
            if (chatScroll < 0) chatScroll = 0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        boolean left = button == 0;

        if (left && spectateBtnVisible
            && mx >= spectateBtnX1 && mx <= spectateBtnX2
            && my >= spectateBtnY1 && my <= spectateBtnY2) {
            leaveOwnSlot();
            return true;
        }

        for (Button b : hudButtons) {
            b.checkClicked(mx, my, left);
        }

        if (left) {
            for (FactionHit fh : factionHits) {
                if (mx >= fh.x && mx <= fh.x + FRAME_SIZE && my >= fh.y && my <= fh.y + FRAME_SIZE) {
                    pickFaction(fh.pos, fh.faction);
                    return true;
                }
            }
            for (ReadyHit rh : readyHits) {
                if (mx >= rh.x && mx <= rh.x + FRAME_SIZE && my >= rh.y && my <= rh.y + FRAME_SIZE) {
                    toggleReady(rh.pos);
                    return true;
                }
            }
            for (RowHit rh : rowHits) {
                if (mx >= rh.x1 && mx <= rh.x2 && my >= rh.y1 && my <= rh.y2) {
                    claimDot(rh.pos);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void leaveOwnSlot() {
        StartPos current = StartPosClientEvents.getPos();
        if (current != null) {
            StartPosServerboundPacket.unreservePos(current.pos);
            StartPosClientEvents.selectedFaction = Faction.NONE;
        }
    }

    private void pickFaction(StartPos pos, Faction faction) {
        if (Minecraft.getInstance().player == null) return;
        String name = Minecraft.getInstance().player.getName().getString();
        if (pos.playerName.isBlank()) {
            StartPosClientEvents.selectedFaction = faction;
            StartPosServerboundPacket.reservePos(pos.pos, faction, name);
        } else if (pos.playerName.equals(name)) {
            if (pos.faction == faction) {
                StartPosClientEvents.selectedFaction = Faction.NONE;
                StartPosServerboundPacket.reservePos(pos.pos, Faction.NONE, name);
            } else {
                StartPosClientEvents.selectedFaction = faction;
                StartPosServerboundPacket.reservePos(pos.pos, faction, name);
            }
        }
    }

    private void claimDot(StartPos pos) {
        if (Minecraft.getInstance().player == null || !pos.enabled) return;
        String name = Minecraft.getInstance().player.getName().getString();
        if (pos.playerName.equals(name)) {
            StartPosServerboundPacket.unreservePos(pos.pos);
        } else if (pos.playerName.isBlank()) {
            StartPosServerboundPacket.reservePos(pos.pos, StartPosClientEvents.selectedFaction, name);
        }
    }

    private void toggleReady(StartPos pos) {
        if (Minecraft.getInstance().player == null) return;
        String name = Minecraft.getInstance().player.getName().getString();
        if (pos.ready) {
            StartPosServerboundPacket.unreadyPlayer(name);
        } else {
            StartPosServerboundPacket.readyPlayer(name);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (chatInput != null && chatInput.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                sendChatFromInput();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                chatInput.setFocused(false);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (chatInput != null && keyCode == GLFW.GLFW_KEY_T) {
            chatInput.setFocused(true);
            return true;
        }
        if (chatInput != null && keyCode == GLFW.GLFW_KEY_SLASH) {
            chatInput.setFocused(true);
            chatInput.setValue("/");
            chatInput.moveCursorToEnd();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (chatInput != null && chatInput.isFocused()) return false;
        return !OrthoviewClientEvents.isEnabled();
    }

    @Override
    public void removed() {
        MinimapClientEvents.suppressViewQuad = false;
        MinimapClientEvents.setMapLocked(priorMapLocked);
        MinimapClientEvents.setLargeMap(priorLargeMap);
        GameruleClient.gamerulesMenuOpen = false;
        super.removed();
    }

    public static boolean shouldAutoOpen() {
        if (!OrthoviewClientEvents.isEnabled()) return false;
        if (ClientGameModeHelper.gameMode != GameMode.CLASSIC) return false;
        if (PlayerClientEvents.rtsLocked) return false;
        if (StartPosClientEvents.startPoses.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        for (RTSPlayer p : PlayerClientEvents.rtsPlayers) {
            if (p.name.equals(mc.player.getName().getString())) return false;
        }
        return true;
    }
}
