package com.solegendary.reignofnether.p2p;

import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/** The "play online" popup (P2P hosting), shared by the match setup and the saved-game resume screens. */
public final class HostPopup {
    private HostPopup() { }

    public static boolean open = false;
    private static boolean hostPublicly = true;
    // which code was just copied, and when
    private static String copied = "";
    private static long copiedAt = 0;

    /** "Play online": start hosting, then the codes to send to friends. */
    public static void render(HudFrame frame, int w, int h) {
        var g = frame.g;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(0, 0, w, h, 0xCC050203);
        frame.register(new ButtonBuilder("close host").onLeftClick(() -> open = false).build(), new Rect(0, 0, w, h), null, null);

        int pad = 14;
        int pw = Math.min(w - 2 * 14, 380);
        int bw = pw - 2 * pad;
        P2PHost.State state = P2PHost.state;
        List<FormattedCharSequence> body = Text.wrap(I18n.get(state == P2PHost.State.OFF ? "matchstart.reignofnether.host_body" : "matchstart.reignofnether.host_share"), bw);
        String code = P2PHost.websiteCode, direct = P2PHost.directCode;
        String codeNote = code != null ? I18n.get(P2PHost.isPublic ? "matchstart.reignofnether.game_code_public" : "matchstart.reignofnether.game_code_private")
                : I18n.get("auth_failed".equals(P2PHost.websiteError) ? "matchstart.reignofnether.game_code_auth" : "matchstart.reignofnether.game_code_down");
        String directNote = I18n.get(direct != null ? "matchstart.reignofnether.direct_note" : "matchstart.reignofnether.direct_none");
        int ph = pad + 26 + body.size() * Text.lineHeight() + 10 + switch (state) {
            case OFF -> 22 + 10 + 28;
            case STARTING -> 14;
            case HOSTING -> codeBlockHeight(code, codeNote, true, bw) + 8 + codeBlockHeight(direct, directNote, false, bw) + 10 + 28;
        } + pad;
        int x = (w - pw) / 2;
        int y = (h - ph) / 2;
        Theme.popup(g, x, y, pw, ph, Theme.ORANGE);
        frame.register(new ButtonBuilder("host panel").build(), new Rect(x, y, pw, ph), null, null);
        Text.heading(g, I18n.get("matchstart.reignofnether.host_title").toUpperCase(Locale.ROOT), x + pad, y + pad - 1, Theme.GOLD, Text.px(26));
        Rect close = new Rect(x + pw - pad - 18, y + pad - 4, 18, 18);
        Theme.button(g, close.x(), close.y(), 18, 18, frame.hovered(close));
        Icon.CLOSE.drawIn(g, close.x(), close.y(), 18, 18, frame.hovered(close) ? Theme.GOLD : Theme.BODY);
        frame.register(new ButtonBuilder("close").onLeftClick(() -> open = false).build(), close, null, null);

        int tx = x + pad, ty = y + pad + 26;
        for (FormattedCharSequence line : body) {
            Text.draw(g, line, tx, ty, Theme.DESC);
            ty += Text.lineHeight();
        }
        ty += 10;
        switch (state) {
            case OFF -> {
                Rect toggle = new Rect(tx, ty, bw, 22);
                Theme.bevel(g, tx, ty, 14, 14, hostPublicly ? Theme.TOGGLE_ON : Theme.SUNKEN, 0, 0, 1, Theme.INK);
                if (hostPublicly)
                    Icon.CHECK.drawIn(g, tx, ty, 14, 14, Theme.OK);
                Text.draw(g, I18n.get("matchstart.reignofnether.host_public"), tx + 20, ty + 3, frame.hovered(toggle) ? Theme.GOLD : Theme.BODY);
                frame.register(new ButtonBuilder("public").onLeftClick(() -> hostPublicly = !hostPublicly).build(), toggle, null, null);
                ty += 22 + 10;
                Rect start = new Rect(tx, ty, bw, 28);
                Theme.primaryButton(g, start.x(), start.y(), start.w(), start.h(), frame.hovered(start));
                Text.mediumCentered(g, I18n.get("matchstart.reignofnether.host_start"), tx + bw / 2, ty + 10, Theme.ON_ORANGE);
                frame.register(new ButtonBuilder("start hosting").onLeftClick(() -> P2PHost.start(hostPublicly)).build(), start, null, null);
            }
            case STARTING -> Text.draw(g, I18n.get("matchstart.reignofnether.host_starting") + ".".repeat((int) (System.currentTimeMillis() / 400 % 4)), tx, ty, Theme.LABEL);
            case HOSTING -> {
                ty += codeBlock(frame, tx, ty, bw, I18n.get("matchstart.reignofnether.game_code"), code, codeNote, true) + 8;
                ty += codeBlock(frame, tx, ty, bw, I18n.get("matchstart.reignofnether.direct_code"), direct, directNote, false) + 10;
                Rect stop = new Rect(tx, ty, bw, 28);
                Theme.secondaryButton(g, stop.x(), stop.y(), stop.w(), stop.h(), frame.hovered(stop));
                Text.mediumCentered(g, I18n.get("matchstart.reignofnether.host_stop"), tx + bw / 2, ty + 10, frame.hovered(stop) ? Theme.GOLD : Theme.BODY);
                frame.register(new ButtonBuilder("stop hosting").onLeftClick(() -> new Thread(P2PHost::stop, "RoN P2P stop").start()).build(), stop, null, null);
            }
        }
        g.pose().popPose();
    }

    /** A label, then the code with a copy button and a note under it, or a red banner saying why there's none. Returns its height. */
    private static int codeBlock(HudFrame frame, int x, int y, int w, String label, @Nullable String code, String note, boolean big) {
        var g = frame.g;
        Text.small(g, label, x, y, Theme.LABEL);
        if (code == null)
            return 11 + Theme.banner(g, x, y + 11, w, note, Theme.ERROR);
        int boxH = big ? 26 : 20;
        int copyW = 64;
        g.fill(x, y + 11, x + w - copyW - 6, y + 11 + boxH, Theme.SUNKEN);
        Theme.outline(g, x, y + 11, w - copyW - 6, boxH, 1, Theme.INK);
                if (big)
            Text.big(g, code, x + 8, y + 11 + (boxH - Text.bigHeight(Text.px(30))) / 2, Theme.GOLD, Text.px(30));
        else
            Text.draw(g, Text.fit(code, w - copyW - 22), x + 8, y + 11 + (boxH - 8) / 2, Theme.BODY);
        if (code != null) {
            Rect copy = new Rect(x + w - copyW, y + 11, copyW, boxH);
            boolean justCopied = code.equals(copied) && System.currentTimeMillis() - copiedAt < 1500;
            Theme.secondaryButton(g, copy.x(), copy.y(), copy.w(), copy.h(), frame.hovered(copy));
            Text.mediumCentered(g, I18n.get(justCopied ? "matchstart.reignofnether.copied" : "matchstart.reignofnether.copy"),
                    copy.x() + copyW / 2, copy.y() + (boxH - 8) / 2, justCopied ? Theme.OK : frame.hovered(copy) ? Theme.GOLD : Theme.BODY);
            frame.register(new ButtonBuilder("copy " + label).onLeftClick(() -> {
                Minecraft.getInstance().keyboardHandler.setClipboard(code);
                copied = code;
                copiedAt = System.currentTimeMillis();
            }).build(), copy, null, null);
        }
        Text.small(g, Text.fitSmall(note, w), x, y + 11 + boxH + 4, Theme.DIM);
        return codeBlockHeight(code, note, big, w);
    }

    private static int codeBlockHeight(@Nullable String code, String note, boolean big, int w) {
        return code == null ? 11 + Theme.bannerHeight(note, w) : 11 + (big ? 26 : 20) + 4 + 8;
    }
}
