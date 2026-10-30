package com.solegendary.reignofnether.hud.playerdisplay;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;

/** One row of the player panels: the player's colour, faction, head, name and a status line. */
public abstract class AbstractPlayerDisplay {

    private static final Minecraft MC = Minecraft.getInstance();

    public static final int ROW_H = 32;
    private static final int NAME_W = 120;
    /** Where a row's content starts, after the colour strip, faction, head and name. */
    public static final int CONTENT_X = 3 + 7 + 13 + 7 + 16 + 7 + NAME_W + 7;
    private static final ResourceLocation DEFAULT_HEAD = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_back.png");

    public final String playerName;
    public final Faction faction;
    public final @Nullable AbstractClientPlayer player;

    public AbstractPlayerDisplay(RTSPlayer rtsPlayer) {
        AbstractClientPlayer found = null;
        if (MC.getCurrentServer() != null) {
            for (AbstractClientPlayer p : MC.level.players()) {
                if (p.getName().getString().equals(rtsPlayer.name)) {
                    found = p;
                    break;
                }
            }
        } else if (MC.player != null && MC.player.getName().getString().equals(rtsPlayer.name)) {
            found = MC.player;
        }
        this.player = found;
        this.faction = rtsPlayer.faction;
        this.playerName = rtsPlayer.name;
    }

    // survival/adventure player
    public AbstractPlayerDisplay(AbstractClientPlayer clientPlayer) {
        this.player = clientPlayer;
        this.playerName = clientPlayer.getName().getString();
        this.faction = Factions.NONE;
    }

    /** With fog of war, only you and your allies' faction and economy are shown. */
    protected boolean hiddenByFog() {
        if (!FogOfWarClientEvents.isMatchFogOn() || MC.player == null || !com.solegendary.reignofnether.player.PlayerClientEvents.isRTSPlayer())
            return false;
        return !AlliancesClient.isAlliedOrOwned(MC.player.getName().getString(), playerName);
    }

    @Nullable
    public static String pingText(String name) {
        PlayerInfo info = MC.getConnection() == null ? null : MC.getConnection().getPlayerInfo(name);
        return info == null ? null : I18n.get("hud.reignofnether.ping", info.getLatency());
    }

    public static int pingColour(String name) {
        PlayerInfo info = MC.getConnection() == null ? null : MC.getConnection().getPlayerInfo(name);
        int ms = info == null ? 0 : info.getLatency();
        return ms < 80 ? Theme.OK : ms < 150 ? Theme.YELLOW : Theme.ERROR;
    }

    public boolean isPlayerLoggedIn() {
        return MC.getConnection() != null && MC.getConnection().getPlayerInfo(playerName) != null;
    }

    /** Row background, colour strip, faction icon, head, name and status. Returns the x where the row's content starts. */
    protected int renderIdentity(HudFrame f, int x, int y, int w, String status, int statusColour) {
        Theme.cell(f.g, x, y, w, ROW_H, Theme.CellState.READY, false);
        f.g.fill(x, y, x + 3, y + ROW_H, 0xFF000000 | PlayerColors.getPlayerColorHex(playerName));

        int cx = x + 3 + 7;
        int cy = y + ROW_H / 2;
        if (faction != null && faction.icon != null && faction != Factions.NONE && !hiddenByFog())
            MyRenderer.renderIcon(f.g, faction.icon, cx, cy - 6, 13);
        cx += 13 + 7;

        Theme.outline(f.g, cx, cy - 8, 16, 16, 1, Theme.INK);
        if (player != null && player.isSkinLoaded()) {
            ResourceLocation skin = player.getSkinTextureLocation();
            f.g.blit(skin, cx, cy - 8, 16, 16, 8f, 8f, 8, 8, 64, 64);  // face
            f.g.blit(skin, cx, cy - 8, 16, 16, 40f, 8f, 8, 8, 64, 64);  // hat layer
        } else {
            MyRenderer.renderIcon(f.g, DEFAULT_HEAD, cx, cy - 8, 16);
        }
        cx += 16 + 7;

        // ping right-aligned on the name line; bots and offline players have no connection entry
        String ping = pingText(playerName);
        int pingW = ping == null ? 0 : Text.smallWidth(ping) + 4;
        if (ping != null)
            Text.small(f.g, ping, cx + NAME_W - pingW + 4, cy - 8, pingColour(playerName));
        Text.draw(f.g, Text.fit(playerName, NAME_W - pingW), cx, cy - 9, Theme.TEXT);
        Text.small(f.g, Text.fitSmall(status, NAME_W), cx, cy + 1, statusColour);
        return cx + NAME_W + 7;
    }

    public enum Style { PRIMARY, SECONDARY, DANGER, ON }

    public static final int BTN_H = 15;

    public static int buttonWidth(String label) {
        return Text.smallWidth(label) + 14;
    }

    /** Text button in the small font, registered with the frame. Returns its width. */
    public static int textButton(HudFrame f, String label, int x, int y, Style style, boolean enabled,
                                 Runnable action, @Nullable Tooltip tooltip) {
        int w = buttonWidth(label);
        Rect r = new Rect(x, y, w, BTN_H);
        boolean hovered = enabled && f.hovered(r);
        int colour;
        if (!enabled) {
            Theme.bevel(f.g, x, y, w, BTN_H, Theme.LOCKED, 0, 0, 1, Theme.INK);
            colour = Theme.DIM;
        } else if (style == Style.PRIMARY) {
            Theme.primaryButton(f.g, x, y, w, BTN_H, hovered);
            colour = Theme.ON_ORANGE;
        } else if (style == Style.DANGER) {
            Theme.bevel(f.g, x, y, w, BTN_H, hovered ? Theme.CANCEL_HOVER : Theme.DANGER_BG, 0, 0, 1, Theme.INK);
            colour = Theme.DANGER_TEXT;
        } else if (style == Style.ON) {
            Theme.bevel(f.g, x, y, w, BTN_H, Theme.TOGGLE_ON, 0, 0, 1, Theme.INK);
            Theme.ring(f.g, x, y, w, BTN_H, 1, Theme.OK);
            colour = Theme.OK_TEXT;
        } else {
            Theme.secondaryButton(f.g, x, y, w, BTN_H, hovered);
            colour = hovered ? Theme.GOLD : Theme.BODY;
        }
        Text.smallCentered(f.g, label, x + w / 2, y + 4, colour);
        Button b = new ButtonBuilder(label).isEnabled(() -> enabled).onLeftClick(action).build();
        f.register(b, r, null, () -> tooltip);
        return w;
    }

    /** Offline players fade out, like the design's 55% opacity. */
    protected void fadeIfOffline(HudFrame f, int x, int y, int w) {
        if (!isPlayerLoggedIn())
            f.g.fill(x, y, x + w, y + ROW_H, Theme.withAlpha(Theme.PANEL, 0.45f));
    }
}
