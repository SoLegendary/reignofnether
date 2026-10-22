package com.solegendary.reignofnether.hud.playerdisplay;

import com.solegendary.reignofnether.player.PlayerRolesClient;
import com.solegendary.reignofnether.alliance.SharingClient;
import com.solegendary.reignofnether.alliance.AllianceAction;
import com.solegendary.reignofnether.alliance.AllianceServerboundPacket;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.panels.ButtonCell;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.resources.ResourcesServerboundPacket;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** A diplomacy row: alliance status and actions, and resource sending for RTS players. */
public class DiplomacyPlayerDisplay extends AbstractPlayerDisplay {

    private static final Minecraft MC = Minecraft.getInstance();

    private static final ResourceLocation[] TRADE_ICONS = { ButtonCell.ICON_FOOD, ButtonCell.ICON_WOOD, ButtonCell.ICON_ORE };
    private static final String[] TRADE_TOOLTIPS = { "alliances.reignofnether.tooltip.send_food",
            "alliances.reignofnether.tooltip.send_wood", "alliances.reignofnether.tooltip.send_ore" };
    private static final int FIELD_W = 51;
    private static final int FIELD_H = 17;
    private static final int MAX_AMOUNT = 99999;

    // the amount field being typed in, if any
    @Nullable private static DiplomacyPlayerDisplay focused = null;
    private static int focusedSlot = 0;

    private final int[] amounts = new int[3]; // food, wood, ore

    public DiplomacyPlayerDisplay(RTSPlayer rtsPlayer) {
        super(rtsPlayer);
    }

    public DiplomacyPlayerDisplay(AbstractClientPlayer player) {
        super(player);
    }

    private enum Relation {
        ALLY("ally", Theme.OK), ENEMY("enemy", Theme.ERROR), SENT("request_sent", Theme.ORANGE), INCOMING("wants_alliance", Theme.GOLD);

        final String key;
        final int colour;

        Relation(String key, int colour) {
            this.key = key;
            this.colour = colour;
        }
    }

    private Relation relation() {
        if (MC.player != null && AlliancesClient.isAllied(PlayerRolesClient.myOwner(), playerName))
            return Relation.ALLY;
        if (AlliancesClient.outboundPendingAlliances.contains(playerName))
            return Relation.SENT;
        if (AlliancesClient.inboundPendingAlliances.contains(playerName))
            return Relation.INCOMING;
        return Relation.ENEMY;
    }

    protected boolean isRTSPlayer() {
        return PlayerClientEvents.isRTSPlayer(playerName);
    }

    public void render(HudFrame f, int x, int y, int w) {
        Relation rel = relation();
        boolean online = isPlayerLoggedIn();
        boolean rts = isRTSPlayer();

        StringBuilder status = new StringBuilder();
        if (!rts)
            status.append(I18n.get("hud.reignofnether.diplomacy.first_person")).append(" · ");
        if (!online)
            status.append(I18n.get("hud.reignofnether.diplomacy.offline")).append(" · ");
        status.append(I18n.get("hud.reignofnether.diplomacy." + rel.key));
        if (rel == Relation.ALLY && SharingClient.canControl(playerName))
            status.append(" · ").append(I18n.get("hud.reignofnether.diplomacy.shares_control"));

        int cx = renderIdentity(f, x, y, w, status.toString(), online ? rel.colour : Theme.LABEL);
        int by = y + (ROW_H - BTN_H) / 2;
        int actionsX = renderActions(f, rel, x + w - 7, by);

        if (rts) {
            for (int i = 0; i < 3; i++) {
                renderField(f, i, cx, y + (ROW_H - FIELD_H) / 2, online);
                cx += FIELD_W + 5;
            }
            textButton(f, I18n.get("hud.reignofnether.diplomacy.send"), cx, by, Style.SECONDARY,
                    online && total() > 0, this::sendResources,
                    Tooltip.of(I18n.get("alliances.reignofnether.tooltip.confirm_send_resources")));
        } else {
            String go = I18n.get("hud.reignofnether.diplomacy.go_to_player");
            int goW = buttonWidth(go);
            int hintW = actionsX - 7 - goW - 6 - cx;
            Text.draw(f.g, Text.fit(I18n.get("hud.reignofnether.diplomacy.first_person_hint"), hintW), cx, y + ROW_H / 2 - 4, Theme.LABEL);
            textButton(f, go, actionsX - 7 - goW, by, Style.SECONDARY, rel == Relation.ALLY, this::goToPlayer,
                    Tooltip.of(I18n.get("alliances.reignofnether.tooltip.goto_player")));
        }
        fadeIfOffline(f, x, y, w);
    }

    /** Alliance buttons, right-aligned at right. Returns their left edge. */
    private int renderActions(HudFrame f, Relation rel, int right, int y) {
        record Action(String key, Style style, Runnable run, String tooltipKey) { }
        List<Action> actions = new ArrayList<>();
        switch (rel) {
            case ALLY -> actions.add(new Action("disband", Style.DANGER, this::disbandAlliance, "disband_alliance"));
            case ENEMY -> actions.add(new Action("request", Style.SECONDARY, this::requestAlliance, "request_alliance"));
            case SENT -> actions.add(new Action("cancel_request", Style.SECONDARY, this::cancelAllianceRequest, "cancel_request_alliance"));
            case INCOMING -> actions.add(new Action("accept", Style.PRIMARY, this::acceptAllianceRequest, "accept_alliance"));
        }
        // only asking for an alliance needs the player online
        boolean enabled = rel != Relation.ENEMY || isPlayerLoggedIn();
        int x = right;
        for (int i = actions.size() - 1; i >= 0; i--) {
            Action a = actions.get(i);
            String label = I18n.get("hud.reignofnether.diplomacy." + a.key);
            x -= buttonWidth(label);
            textButton(f, label, x, y, a.style, enabled, a.run,
                    Tooltip.of(I18n.get("alliances.reignofnether.tooltip." + a.tooltipKey)));
            x -= 5;
        }
        return x;
    }

    private void renderField(HudFrame f, int slot, int x, int y, boolean online) {
        Rect r = new Rect(x, y, FIELD_W, FIELD_H);
        Theme.outline(f.g, x, y, FIELD_W, FIELD_H, 1, Theme.INK);
        f.g.fill(x, y, x + FIELD_W, y + FIELD_H, Theme.EMPTY);
        MyRenderer.renderIcon(f.g, TRADE_ICONS[slot], x + 4, y + 4, 9);

        int ix = x + 16;
        int iw = FIELD_W - 18;
        boolean isFocused = focused == this && focusedSlot == slot;
        f.g.fill(ix, y + 2, ix + iw, y + FIELD_H - 2, Theme.INK);
        if (isFocused)
            Theme.ring(f.g, ix, y + 2, iw, FIELD_H - 4, 1, Theme.ORANGE);
        else if (f.hovered(r) && online)
            Theme.ring(f.g, ix, y + 2, iw, FIELD_H - 4, 1, Theme.FAINT);

        String value = amounts[slot] > 0 ? String.valueOf(amounts[slot]) : "0";
        int textRight = ix + iw - 3;
        Text.bigRight(f.g, value, textRight, y + 5, amounts[slot] > 0 ? Theme.GOLD : Theme.FAINT, 1f);
        if (isFocused && (Util.getMillis() / 500) % 2 == 0)
            f.g.fill(textRight + 1, y + 4, textRight + 2, y + FIELD_H - 4, Theme.GOLD);

        f.register(new ButtonBuilder("Trade amount").isEnabled(() -> online).onLeftClick(() -> {
            focused = this;
            focusedSlot = slot;
        }).build(), r, null, () -> Tooltip.of(I18n.get(TRADE_TOOLTIPS[slot])));
    }

    private int total() {
        return amounts[0] + amounts[1] + amounts[2];
    }

    // ------------------------------------------------------------------ typing

    public static boolean isTyping() {
        return focused != null;
    }

    public static void stopTyping() {
        focused = null;
    }

    /** @return true if the key was used by the focused amount field */
    public static boolean keyPressed(int keyCode) {
        if (focused == null)
            return false;
        int[] a = focused.amounts;
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> a[focusedSlot] /= 10;
            case GLFW.GLFW_KEY_DELETE -> a[focusedSlot] = 0;
            case GLFW.GLFW_KEY_TAB -> focusedSlot = (focusedSlot + 1) % 3;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> focused = null;
            default -> { }
        }
        return true;
    }

    public static void charTyped(char c) {
        if (focused == null || c < '0' || c > '9')
            return;
        int[] a = focused.amounts;
        long next = a[focusedSlot] * 10L + (c - '0');
        a[focusedSlot] = (int) Math.min(next, Math.min(MAX_AMOUNT, owned(focusedSlot)));
    }

    private static int owned(int slot) {
        Resources res = ResourcesClientEvents.getOwnResources();
        if (res == null)
            return MAX_AMOUNT;
        return slot == 0 ? res.food : slot == 1 ? res.wood : res.ore;
    }

    // ------------------------------------------------------------------ actions

    private void sendResources() {
        if (MC.player == null)
            return;
        ResourcesServerboundPacket.sendResources(new Resources(playerName, amounts[0], amounts[1], amounts[2]),
                PlayerRolesClient.myOwner());
        amounts[0] = amounts[1] = amounts[2] = 0;
        if (focused == this)
            focused = null;
    }

    private void goToPlayer() {
        if (player != null)
            OrthoviewClientEvents.centreCameraOnPos(player.position());
    }

    private static boolean alliancesLocked() {
        if (MC.player == null)
            return true;
        if (GameruleClient.lockAlliances) {
            MC.player.sendSystemMessage(SystemMessage.error(Component.translatable("alliance.reignofnether.alliances_lock")));
            return true;
        }
        if (GameruleClient.coopMode) {
            MC.player.sendSystemMessage(SystemMessage.error(Component.translatable("alliance.reignofnether.alliances_lock_coop")));
            return true;
        }
        return false;
    }

    private void requestAlliance() {
        if (alliancesLocked())
            return;
        AllianceServerboundPacket.doAllianceAction(AllianceAction.REQUEST, playerName);
        AlliancesClient.outboundPendingAlliances.add(playerName);
    }

    private void cancelAllianceRequest() {
        AllianceServerboundPacket.doAllianceAction(AllianceAction.CANCEL_REQUEST, playerName);
        AlliancesClient.outboundPendingAlliances.remove(playerName);
    }

    private void acceptAllianceRequest() {
        AllianceServerboundPacket.doAllianceAction(AllianceAction.ACCEPT_REQUEST, playerName);
    }

    private void disbandAlliance() {
        if (alliancesLocked())
            return;
        AllianceServerboundPacket.doAllianceAction(AllianceAction.DISBAND, playerName);
    }
}
