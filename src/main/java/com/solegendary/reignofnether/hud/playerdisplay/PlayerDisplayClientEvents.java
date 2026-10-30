package com.solegendary.reignofnether.hud.playerdisplay;

import net.minecraft.resources.ResourceLocation;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AllianceAction;
import com.solegendary.reignofnether.alliance.AllianceServerboundPacket;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.hud.panels.MapZoom;
import com.solegendary.reignofnether.hud.panels.SpectatorHud;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.ui.Clip;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

/**
 * The player panels opened from the top-right bar: diplomacy for RTS players (alliances and sending
 * resources) and the observer panel for spectators (every player's economy). Drawn by the HUD in units.
 */
public class PlayerDisplayClientEvents {

    private enum DisplayType {
        OBSERVER,
        DIPLOMACY,
        NONE
    }
    private static final Minecraft MC = Minecraft.getInstance();

    private static final List<ObserverPlayerDisplay> observerPlayerDisplays = new ArrayList<>();
    private static final List<DiplomacyPlayerDisplay> rtsDiplomacyPlayerDisplays = new ArrayList<>();
    private static final List<DiplomacyPlayerDisplay> fpvDiplomacyPlayerDisplays = new ArrayList<>();
    private static DisplayType displayType = DisplayType.NONE;

    private static final int TOP = 50;
    private static final int WIDTH = 575;
    private static final int PAD = 10;
    private static final int HEADER_H = 40; // title row and column labels
    private static final int ROW_GAP = 4;
    private static final int MAP = 200;
    // the map is a diamond, so zoom 2 is as far out as it goes and still fills the card: the large map keeps it all in
    private static final MapZoom ZOOM = MapZoom.minimum(2f);
    private static boolean largeMapShown = false;

    public static void resetDisplay() {
        displayType = DisplayType.NONE;
        DiplomacyPlayerDisplay.stopTyping();
    }

    public static void clearAll() {
        observerPlayerDisplays.clear();
        rtsDiplomacyPlayerDisplays.clear();
        fpvDiplomacyPlayerDisplays.clear();
        resetDisplay();
    }

    /** Diplomacy key: diplomacy for RTS players, the economy overview for everyone else. */
    private static void toggleFromKey() {
        toggle(PlayerClientEvents.isRTSPlayer() ? DisplayType.DIPLOMACY : DisplayType.OBSERVER);
    }

    private static void toggle(DisplayType type) {
        if (displayType != DisplayType.NONE)
            resetDisplay();
        else
            displayType = type;
    }

    public static final Button observerButton = new Button(
            "Toggle Observer Displays",
            Button.DEFAULT_ICON_SIZE,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/map.png"),
            (Keybinding) null, // its key is handled in onKeyPress so it works even when this button isn't shown
            () -> displayType == DisplayType.OBSERVER,
            PlayerClientEvents::isRTSPlayer, // with no players its panel is just the big map, so it's shown before a match too
            () -> true,
            () -> toggle(DisplayType.OBSERVER),
            null,
            List.of(fcs(I18n.get("alliances.reignofnether.tooltip.toggle_observer_player_displays"))
            ));

    public static final Button diplomacyButton = new Button(
            "Toggle Diplomacy Displays",
            Button.DEFAULT_ICON_SIZE,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/map.png"),
            (Keybinding) null,
            () -> displayType == DisplayType.DIPLOMACY,
            () -> !PlayerClientEvents.isRTSPlayer(),
            () -> true,
            () -> toggle(DisplayType.DIPLOMACY),
            null,
            List.of(fcs(I18n.get("alliances.reignofnether.tooltip.toggle_diplomacy_player_displays"))
            ));

    // ------------------------------------------------------------------ rendering

    public static void render(HudFrame f) {
        // opened before the match started: it would show every enemy's economy
        if (displayType == DisplayType.OBSERVER && PlayerClientEvents.isRTSPlayer())
            resetDisplay();
        if (largeMapShown != (displayType != DisplayType.NONE)) { // here because the map texture needs the render thread
            largeMapShown = !largeMapShown;
            MinimapClientEvents.setLargeMap(largeMapShown);
        }
        if (displayType == DisplayType.DIPLOMACY && MC.level != null && MC.player != null) {
            renderDiplomacy(f);
        } else {
            DiplomacyPlayerDisplay.stopTyping();
            if (displayType == DisplayType.OBSERVER)
                renderObserver(f);
        }
    }

    private static void renderObserver(HudFrame f) {
        List<String> names = PlayerClientEvents.rtsPlayers.stream().map(p -> p.name).toList();
        observerPlayerDisplays.removeIf(d -> !names.contains(d.playerName));
        for (RTSPlayer rtsPlayer : PlayerClientEvents.rtsPlayers)
            if (observerPlayerDisplays.stream().noneMatch(d -> d.playerName.equals(rtsPlayer.name)))
                observerPlayerDisplays.add(new ObserverPlayerDisplay(rtsPlayer));

        Rect panel = renderPanel(f, I18n.get("hud.reignofnether.observer.title"), observerPlayerDisplays.size());
        int cx = panel.x() + PAD + AbstractPlayerDisplay.CONTENT_X;
        int ly = panel.y() + HEADER_H - 9;
        Text.small(f.g, I18n.get("hud.reignofnether.diplomacy.col_player"), panel.x() + PAD + 10, ly, Theme.DIM);
        Text.small(f.g, I18n.get("hud.reignofnether.observer.col_resources"), cx, ly, Theme.DIM);
        Text.small(f.g, I18n.get("hud.reignofnether.observer.col_population"), cx + 3 * ObserverPlayerDisplay.CELL_STEP, ly, Theme.DIM);

        int y = panel.y() + HEADER_H;
        for (ObserverPlayerDisplay d : observerPlayerDisplays) {
            d.render(f, panel.x() + PAD, y, panel.w() - PAD * 2);
            y += AbstractPlayerDisplay.ROW_H + ROW_GAP;
        }
    }

    private static void renderDiplomacy(HudFrame f) {
        String self = MC.player.getName().getString();
        List<String> rtsNames = PlayerClientEvents.rtsPlayers.stream().map(p -> p.name).toList();
        List<String> levelNames = MC.level.players().stream().map(p -> p.getName().getString()).toList();
        rtsDiplomacyPlayerDisplays.removeIf(d -> !rtsNames.contains(d.playerName));
        fpvDiplomacyPlayerDisplays.removeIf(d -> !levelNames.contains(d.playerName));

        for (AbstractClientPlayer player : MC.level.players()) {
            String name = player.getName().getString();
            if (player != MC.player && PlayerClientEvents.getRTSPlayer(name) == null
                    && !player.isSpectator() && !player.isCreative()
                    && fpvDiplomacyPlayerDisplays.stream().noneMatch(d -> d.playerName.equals(name))) {
                fpvDiplomacyPlayerDisplays.add(new DiplomacyPlayerDisplay(player));
                rtsDiplomacyPlayerDisplays.removeIf(d -> d.playerName.equals(name));
            }
        }
        for (RTSPlayer rtsPlayer : PlayerClientEvents.rtsPlayers) {
            if (!rtsPlayer.name.equals(self)
                    && rtsDiplomacyPlayerDisplays.stream().noneMatch(d -> d.playerName.equals(rtsPlayer.name))) {
                rtsDiplomacyPlayerDisplays.add(new DiplomacyPlayerDisplay(rtsPlayer));
                fpvDiplomacyPlayerDisplays.removeIf(d -> d.playerName.equals(rtsPlayer.name));
            }
        }

        List<DiplomacyPlayerDisplay> rows = new ArrayList<>(rtsDiplomacyPlayerDisplays);
        rows.addAll(fpvDiplomacyPlayerDisplays);
        Rect panel = renderPanel(f, I18n.get("hud.reignofnether.diplomacy.title"), rows.size());

        boolean canShareUnitControl = !rtsDiplomacyPlayerDisplays.isEmpty()
                && PlayerClientEvents.isRTSPlayer() && PlayerClientEvents.rtsPlayers.size() > 1;
        if (canShareUnitControl)
            renderShareControlToggle(f, panel.right() - PAD - 16 - 8, panel.y() + 8);

        int ly = panel.y() + HEADER_H - 9;
        Text.small(f.g, I18n.get("hud.reignofnether.diplomacy.col_player"), panel.x() + PAD + 10, ly, Theme.DIM);
        Text.small(f.g, I18n.get("hud.reignofnether.diplomacy.col_send"), panel.x() + PAD + AbstractPlayerDisplay.CONTENT_X, ly, Theme.DIM);
        Text.smallRight(f.g, I18n.get("hud.reignofnether.diplomacy.col_alliance"), panel.right() - PAD - 7, ly, Theme.DIM);

        int y = panel.y() + HEADER_H;
        for (DiplomacyPlayerDisplay d : rows) {
            d.render(f, panel.x() + PAD, y, panel.w() - PAD * 2);
            y += AbstractPlayerDisplay.ROW_H + ROW_GAP;
        }
    }

    /** Panel background, title, close hint and close button. Returns the panel rectangle. */
    private static Rect renderPanel(HudFrame f, String title, int rows) {
        // two solid cards side by side: the player rows, only as tall as they need, and the map
        int rowsH = Math.max(AbstractPlayerDisplay.ROW_H, rows * (AbstractPlayerDisplay.ROW_H + ROW_GAP) - ROW_GAP);
        // below the spectator scoreboard, which grows a row per two players
        int minTop = SpectatorHud.isActive() ? Math.max(TOP, SpectatorHud.bottom() + HudLayout.GAP) : TOP;
        int map = Math.min(MAP, f.layout.console().y() - minTop - PAD * 2 - HudLayout.GAP);
        // and only if it fits beside the rows on a small screen
        map = Math.min(map, f.layout.width() - WIDTH - PAD * 2 - HudLayout.GAP * 3);
        int mapW = map > 0 ? map + PAD * 2 + HudLayout.GAP : 0;
        int h = HEADER_H + rowsH + PAD;
        int x = (f.layout.width() - WIDTH - mapW) / 2;
        // centred between the top of the screen and the console, never over the top bar
        int top = Math.max(minTop, (f.layout.console().y() - Math.max(h, map > 0 ? map + PAD * 2 : 0)) / 2);
        Rect panel = new Rect(x, top, WIDTH, h);
        Theme.panel(f.g, x, top, WIDTH, h, Theme.floating());
        f.g.fill(x, top, x + WIDTH, top + 2, Theme.ORANGE);
        f.zone(panel);
        if (map > 0) {
            Rect card = new Rect(panel.right() + HudLayout.GAP, top, map + PAD * 2, map + PAD * 2);
            Theme.panel(f.g, card.x(), card.y(), card.w(), card.h(), Theme.floating());
            f.zone(card);
            renderMap(f, card.x() + PAD, card.y() + PAD, map);
        }

        Text.heading(f.g, title, x + PAD, top + 10, Theme.GOLD, Text.px(24));

        Rect close = new Rect(panel.right() - PAD - 16, top + 8, 16, 16);
        boolean hovered = f.hovered(close);
        Theme.secondaryButton(f.g, close.x(), close.y(), close.w(), close.h(), hovered);
        Theme.cross(f.g, close, hovered ? Theme.GOLD : Theme.BODY);
        f.register(new ButtonBuilder("Close player panel").onLeftClick(PlayerDisplayClientEvents::resetDisplay).build(),
                close, null, () -> null);
        return panel;
    }

    /** The minimap, bigger, in the same frame as the HUD's. */
    private static void renderMap(HudFrame f, int x, int y, int size) {
        if (size <= 0)
            return;
        Theme.outline(f.g, x - 2, y - 2, size + 4, size + 4, 1, Theme.INK);
        Theme.outline(f.g, x, y, size, size, 2, Theme.CONSOLE_RIM);
        f.g.fill(x, y, x + size, y + size, Theme.INK);
        Clip.push(f.g, x, y, size, size);
        MinimapClientEvents.renderSquare(f.g, x, y, size, ZOOM.value());
        Clip.pop(f.g);
        // clicking the map moves the view there, like the minimap; registered first so the buttons on it win
        float zoom = ZOOM.value();
        int mx = f.mouseX, my = f.mouseY;
        f.register(new ButtonBuilder("Move view on map").onLeftClick(() -> {
            BlockPos pos = MinimapClientEvents.getWorldPosOnSquare(mx, my, x, y, size, zoom);
            if (pos != null && MC.player != null)
                PlayerServerboundPacket.teleportPlayer((double) pos.getX(), MC.player.getY(), (double) pos.getZ());
        }).build(), new Rect(x, y, size, size), null, () -> null);
        ZOOM.renderButtons(f, x + 4, y + size - 4);

        // the map toggles that matter mid-match; the rest are in the settings menu
        int tile = HudLayout.TOOL;
        int tx = x + size - tile - 4, ty = y + 4;
        if (size < (tile + 2) * 3 + HudLayout.ZOOM + 8)
            return;
        f.cell(MinimapClientEvents.getHighlightAnimalsButton(), tx, ty, tile, null);
        f.cell(OrthoviewClientEvents.getLeavesHidingButton(), tx, ty + tile + 2, tile, null);
        f.cell(MinimapClientEvents.getCamSensitivityButton(), tx, ty + (tile + 2) * 2, tile, null);
    }

    /** "Allies can control my units" chip with an on/off dot, like the match setup's rule toggles. */
    private static void renderShareControlToggle(HudFrame f, int right, int y) {
        boolean sharing = AlliancesClient.sharingAllyControl();
        String label = I18n.get("hud.reignofnether.diplomacy.share_control").toUpperCase(java.util.Locale.ROOT);
        int w = Text.smallWidth(label) + 19;
        Rect r = new Rect(right - w, y, w, 16);
        Theme.chip(f.g, r.x(), y, w, r.h(), false, f.hovered(r));
        f.g.fill(r.x() + 5, y + 5, r.x() + 10, y + 10, sharing ? Theme.OK : Theme.FAINT);
        Text.small(f.g, label, r.x() + 14, y + 4, Theme.BODY);
        f.register(new ButtonBuilder("Share unit control")
                .onLeftClick(() -> AllianceServerboundPacket.doAllianceAction(AllianceAction.SET_ALLY_CONTROL, !sharing))
                .build(), r, null, () -> null);
    }

    // ------------------------------------------------------------------ typing in the amount fields

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Pre evt) {
        // a click on an amount field focuses it again after this, through the HUD
        DiplomacyPlayerDisplay.stopTyping();
    }

    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Pre evt) {
        if (DiplomacyPlayerDisplay.keyPressed(evt.getKeyCode())) {
            evt.setCanceled(true);
        } else if (evt.getKeyCode() == Keybindings.diplomacy.getKey() && evt.getScreen() instanceof TopdownGui
                && OrthoviewClientEvents.isEnabled() && !TextInputClientEvents.isAnyInputFocused()) {
            toggleFromKey();
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCharTyped(ScreenEvent.CharacterTyped.Pre evt) {
        if (DiplomacyPlayerDisplay.isTyping()) {
            DiplomacyPlayerDisplay.charTyped(evt.getCodePoint());
            evt.setCanceled(true);
        }
    }
}
