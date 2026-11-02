package com.solegendary.reignofnether.p2p;

import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * "Join a game" from the title screen: paste a code from the host (website code or direct D- code).
 * Shows the steps, the host's mod differences and clear errors. Laid out in HUD units like the map pack popup.
 */
public class JoinScreen extends Screen {

    private static final int PAD = 16, BUTTON_H = 32, W = 420;

    private record Hit(int x, int y, int w, int h, Runnable action) { }

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private final String initialCode;
    private EditBox input;

    public JoinScreen(Screen parent, String initialCode) {
        super(Component.translatable("p2p.reignofnether.join_title"));
        this.parent = parent;
        this.initialCode = initialCode;
    }

    /** "key|arg|arg" from P2PJoin/P2PApi into text. */
    static String tr(String keyAndArgs) {
        String[] p = keyAndArgs.split("\\|");
        return I18n.get(p[0], (Object[]) Arrays.copyOfRange(p, 1, p.length));
    }

    @Override
    protected void init() {
        parent.resize(minecraft, width, height); // it stays visible behind the popup
        if (input != null)
            return; // kept across resizes
        input = new EditBox(font, 0, 0, 100, 16, Component.translatable("p2p.reignofnether.code_hint"));
        input.setBordered(false);
        input.setMaxLength(64);
        input.setTextColor(Theme.TEXT);
        input.setValue(initialCode);
        input.setFocused(true);
        if (!initialCode.isBlank())
            P2PJoin.join(initialCode, parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // a popup over the screen it came from, not a page of its own
        parent.render(g, -1, -1, partialTick);
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(0, 0, width, height, 0xB30D0305);
        float k = Text.guiPerUnit();
        int screenW = Math.round(width / k), screenH = Math.round(height / k);
        int mx = Math.round(mouseX / k), my = Math.round(mouseY / k);
        hits.clear();

        P2PJoin.Step step = P2PJoin.step;
        boolean busy = step == P2PJoin.Step.LOOKING_UP || step == P2PJoin.Step.CONNECTING;
        int w = Math.min(W, screenW - 32), bw = w - 2 * PAD;
        List<FormattedCharSequence> intro = Text.wrap(Component.translatable("p2p.reignofnether.join_body"), bw);
        List<String> details = details();
        // errors and mod warnings are a banner (red, orange); progress is a plain line
        boolean banner = step == P2PJoin.Step.FAILED || step == P2PJoin.Step.CONFIRM;
        String status = P2PJoin.message.isEmpty() ? "" : tr(P2PJoin.message);
        int statusH = status.isEmpty() ? 0 : banner ? Theme.bannerHeight(status, bw) + 6 : Text.wrap(status, bw).size() * Text.lineHeight();
        int h = PAD + 12 + 24 + intro.size() * Text.lineHeight() + 10 + 26 + 10
                + statusH + details.size() * Text.lineHeight() + 10 + BUTTON_H + PAD;
        int x = (screenW - w) / 2, y = Math.max(8, (screenH - h) / 2);

        g.pose().scale(k, k, 1);
        Theme.popup(g, x, y, w, h, Theme.ORANGE);
        int tx = x + PAD, ty = y + PAD;
        Text.small(g, I18n.get("p2p.reignofnether.kicker"), tx, ty, Theme.ORANGE);
        Text.title(g, title.getString().toUpperCase(), tx, ty + 12, 2f);
        ty += 12 + 24;
        for (FormattedCharSequence line : intro) {
            Text.draw(g, line, tx, ty, Theme.DESC);
            ty += Text.lineHeight();
        }
        ty += 10;

        // the code field
        g.fill(tx, ty, tx + bw, ty + 26, Theme.SUNKEN);
        Theme.outline(g, tx, ty, bw, 26, 1, input.isFocused() ? Theme.ORANGE : Theme.INK);
        input.setX(tx + 8);
        input.setY(ty + 9);
        input.setWidth(bw - 16);
        input.setEditable(!busy);
        input.render(g, mx, my, partialTick);
        if (input.getValue().isEmpty() && !input.isFocused())
            Text.draw(g, I18n.get("p2p.reignofnether.code_hint"), tx + 8, ty + 9, Theme.DIM);
        ty += 26 + 10;

        if (banner && !status.isEmpty()) {
            ty += Theme.banner(g, tx, ty, bw, status, step == P2PJoin.Step.FAILED ? Theme.ERROR : Theme.ORANGE_DARK) + 6;
        } else if (!status.isEmpty()) {
            for (FormattedCharSequence line : Text.wrap(status + (busy ? " " + ".".repeat((int) (System.currentTimeMillis() / 400 % 4)) : ""), bw)) {
                Text.draw(g, line, tx, ty, Theme.LABEL);
                ty += Text.lineHeight();
            }
        }
        for (String d : details) {
            Text.draw(g, Text.fit(d, bw), tx, ty, d.startsWith("x ") ? Theme.DANGER_TEXT : Theme.MUTED);
            ty += Text.lineHeight();
        }

        int by = y + h - PAD - BUTTON_H;
        int backW = Math.max(90, Text.mediumWidth(I18n.get("gui.back")) + 24);
        if (step == P2PJoin.Step.CONFIRM)
            button(g, I18n.get("p2p.reignofnether.join_anyway"), tx, by, bw - backW - 8, true, mx, my, P2PJoin::confirm);
        else if (busy)
            button(g, I18n.get("gui.cancel"), tx, by, bw - backW - 8, false, mx, my, P2PJoin::cancel);
        else
            button(g, I18n.get("p2p.reignofnether.join"), tx, by, bw - backW - 8, !input.getValue().isBlank(), mx, my, this::submit);
        button(g, I18n.get("gui.back"), tx + bw - backW, by, backW, false, mx, my, this::onClose);
        g.pose().popPose();
    }

    /** One line per mod difference, blocking ones first. */
    private static List<String> details() {
        List<String> out = new ArrayList<>();
        P2PApi.Diff d = P2PJoin.diff;
        if (d == null || (P2PJoin.step != P2PJoin.Step.CONFIRM && P2PJoin.step != P2PJoin.Step.FAILED))
            return out;
        d.blocking().forEach(b -> out.add("x " + tr(b)));
        d.warnings().stream().limit(8).forEach(wn -> out.add("- " + tr(wn)));
        if (d.warnings().size() > 8)
            out.add(I18n.get("p2p.reignofnether.diff.more", d.warnings().size() - 8));
        return out;
    }

    private void submit() {
        if (!input.getValue().isBlank())
            P2PJoin.join(input.getValue(), parent);
    }

    private void button(GuiGraphics g, String label, int x, int y, int w, boolean primary, int mx, int my, Runnable action) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + BUTTON_H;
        if (primary)
            Theme.primaryButton(g, x, y, w, BUTTON_H, hov);
        else
            Theme.secondaryButton(g, x, y, w, BUTTON_H, hov);
        Text.mediumCentered(g, label, x + w / 2, y + (BUTTON_H - 8) / 2, primary ? Theme.ON_ORANGE : hov ? Theme.GOLD : Theme.BODY);
        hits.add(new Hit(x, y, w, BUTTON_H, action));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float k = Text.guiPerUnit();
        double mx = mouseX / k, my = mouseY / k;
        input.setFocused(input.isMouseOver(mx, my));
        if (input.isFocused())
            return input.mouseClicked(mx, my, button);
        if (button == 0)
            for (Hit h : List.copyOf(hits))
                if (mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h) {
                    h.action.run();
                    return true;
                }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (P2PJoin.step == P2PJoin.Step.CONFIRM)
                P2PJoin.confirm();
            else if (P2PJoin.step != P2PJoin.Step.LOOKING_UP && P2PJoin.step != P2PJoin.Step.CONNECTING)
                submit();
            return true;
        }
        if (keyCode != GLFW.GLFW_KEY_ESCAPE && input.isFocused() && input.keyPressed(keyCode, scanCode, modifiers))
            return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return input.isFocused() ? input.charTyped(codePoint, modifiers) : super.charTyped(codePoint, modifiers);
    }

    @Override
    public void tick() {
        input.tick();
    }

    @Override
    public void onClose() {
        P2PJoin.cancel();
        minecraft.setScreen(parent);
    }
}
