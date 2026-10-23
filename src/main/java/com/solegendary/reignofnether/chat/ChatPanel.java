package com.solegendary.reignofnether.chat;

import com.solegendary.reignofnether.chat.ChatLog.Channel;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.ui.HudScale;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Chat in the RTS view, replacing vanilla's ChatScreen: the log, a channel chip, the input and SEND.
 * Drawn in HUD units like MatchStartScreen, so the EditBox is drawn and fed input by hand.
 */
public class ChatPanel extends Screen {

    private static final int W = 300;
    private static final int HEADER = 20;
    private static final int LOG_H = 140;
    private static final int ROW_H = 30;
    private static final int H = HEADER + LOG_H + 6 + ROW_H;

    private EditBox input;
    private HudFrame frame;
    private Rect logRect = Rect.EMPTY;
    private Rect fieldRect = Rect.EMPTY;
    private int scroll = 0;
    private int maxScroll = 0;
    private int historyPos = -1;

    // the RTS screen under the chat: closing goes straight back to it instead of waiting for the server to reopen it
    private final Screen parent;

    public ChatPanel() {
        super(Component.translatable("chat.reignofnether.panel.title"));
        Screen current = Minecraft.getInstance().screen;
        this.parent = current instanceof TopdownGui ? current : null;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    /** The RTS view draws its own chat (overlay and this panel), so vanilla's is hidden there. */
    public static boolean hidesVanillaChat() {
        Screen s = Minecraft.getInstance().screen;
        return OrthoviewClientEvents.isEnabled() && (s instanceof TopdownGui || s instanceof ChatPanel);
    }

    public static final int MAX_LENGTH = 256;

    @Override
    protected void init() {
        // init runs again on resize: keep what was typed
        if (input != null)
            return;
        input = new EditBox(this.font, 0, 0, 100, 16, Component.translatable("chat.reignofnether.panel.placeholder"));
        input.setBordered(false);
        input.setMaxLength(MAX_LENGTH);
        input.setTextColor(Theme.TEXT);
        input.setFocused(true);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        HudClientEvents.renderUnderChat(g);
        HudScale scale = HudGeometry.scale();
        HudLayout layout = HudGeometry.layout();
        frame = new HudFrame(g, scale, layout, scale.toUnits(mouseX), scale.toUnits(mouseY));
        int x = HudLayout.MARGIN;
        int y = layout.console().y() - HudLayout.GAP * 2 - H;

        g.pose().pushPose();
        g.pose().scale(scale.poseScale(), scale.poseScale(), 1);
        Theme.popup(g, x, y, W, H, Theme.ORANGE);
        renderHeader(x, y);

        logRect = new Rect(x + 8, y + HEADER, W - 16, LOG_H);
        int total = ChatLines.render(g, ChatLog.entries(), logRect, scroll);
        maxScroll = Math.max(0, total - LOG_H / ChatLines.LINE_H);
        scroll = Math.min(scroll, maxScroll);

        renderInputRow(x, y + H - ROW_H);
        g.pose().popPose();
    }

    private void renderHeader(int x, int y) {
        GuiGraphics g = frame.g;
        String title = I18n.get("chat.reignofnether.panel.title");
        Text.small(g, title, x + 8, y + 7, Theme.LABEL);
        Rect close = new Rect(x + W - 8 - 13, y + 4, 13, 13);
        int hintX = x + 8 + Text.smallWidth(title) + 6;
        int right = close.x() - 6;
        String hints = I18n.get("chat.reignofnether.panel.hints");
        if (Text.smallWidth(hints) <= right - hintX)
            Text.tiny(g, hints, hintX, y + 7, Theme.DIM);
        boolean hovered = frame.hovered(close);
        Theme.secondaryButton(g, close.x(), close.y(), close.w(), close.h(), hovered);
        Theme.cross(g, close, hovered ? Theme.GOLD : Theme.BODY);
        frame.register(new ButtonBuilder("chat close").onLeftClick(this::onClose).build(), close, null, null);
    }

    private void renderInputRow(int x, int ry) {
        GuiGraphics g = frame.g;
        g.fill(x, ry, x + W, ry + ROW_H, Theme.SUNKEN);
        g.fill(x, ry, x + W, ry + 1, Theme.INK);

        Channel ch = Channel.ALL;
        Rect chip = new Rect(x + 5, ry + 5, 32, 20);
        g.fill(chip.x(), chip.y(), chip.right(), chip.bottom(), Theme.RAISED);
        Theme.ring(g, chip.x(), chip.y(), chip.w(), chip.h(), 1, ch.colour);
        Text.smallCentered(g, ch.label(), chip.centerX(), chip.y() + 6, ch.colour);

        String send = I18n.get("chat.reignofnether.panel.send");
        int sendW = Text.smallWidth(send) + 14;
        Rect sendRect = new Rect(x + W - 5 - sendW, chip.y(), sendW, chip.h());
        boolean hovered = frame.hovered(sendRect);
        Theme.secondaryButton(g, sendRect.x(), sendRect.y(), sendRect.w(), sendRect.h(), hovered);
        Text.smallCentered(g, send, sendRect.centerX(), sendRect.y() + 6, hovered ? Theme.GOLD : Theme.BODY);
        frame.register(new ButtonBuilder("chat send").onLeftClick(this::send).build(), sendRect, null, null);

        fieldRect = new Rect(chip.right() + 4, chip.y(), sendRect.x() - 4 - chip.right() - 4, chip.h());
        g.fill(fieldRect.x(), fieldRect.y(), fieldRect.right(), fieldRect.bottom(), Theme.INK);
        g.fill(fieldRect.x(), fieldRect.y(), fieldRect.right(), fieldRect.y() + 1, 0x99000000);
        input.setX(fieldRect.x() + 6);
        input.setY(fieldRect.y() + 6);
        input.setWidth(fieldRect.w() - 12);
        input.render(g, frame.mouseX, frame.mouseY, 0);
        if (input.getValue().isEmpty())
            Text.draw(g, I18n.get("chat.reignofnether.panel.placeholder"), fieldRect.x() + 6, fieldRect.y() + 6, Theme.DIM);
    }

    private void send() {
        sendMessage(input.getValue());
        input.setValue("");
        historyPos = -1;
        scroll = 0;
    }

    /** Sends typed chat or a /command. */
    public static void sendMessage(String typed) {
        String text = typed.trim();
        Minecraft mc = Minecraft.getInstance();
        if (text.isEmpty() || mc.player == null)
            return;
        mc.gui.getChat().addRecentChat(text);
        if (text.startsWith("/"))
            mc.player.connection.sendCommand(text.substring(1));
        else
            mc.player.connection.sendChat(text);
    }

    // up / down walk through what was sent before, like vanilla chat
    private void recall(int step) {
        List<String> history = Minecraft.getInstance().gui.getChat().getRecentChat();
        if (history.isEmpty())
            return;
        historyPos = historyPos < 0 ? (step < 0 ? history.size() - 1 : -1) : historyPos + step;
        if (historyPos < 0 || historyPos >= history.size()) {
            historyPos = -1;
            input.setValue("");
        } else {
            input.setValue(history.get(historyPos));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                // closes like vanilla chat so RTS hotkeys work again right away
                send();
                onClose();
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                recall(-1);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                recall(1);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN -> {
                int page = LOG_H / ChatLines.LINE_H - 1;
                scrollBy(keyCode == GLFW.GLFW_KEY_PAGE_UP ? page : -page);
                return true;
            }
            default -> {
                return input.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return input.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (frame == null)
            return false;
        int ux = frame.scale.toUnits(mouseX);
        int uy = frame.scale.toUnits(mouseY);
        if (fieldRect.contains(ux, uy))
            return input.mouseClicked(ux, uy, button);
        return frame.click(ux, uy, button == GLFW.GLFW_MOUSE_BUTTON_1);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (frame != null && logRect.contains(frame.scale.toUnits(mouseX), frame.scale.toUnits(mouseY)))
            scrollBy((int) Math.signum(delta) * 3);
        return true;
    }

    private void scrollBy(int lines) {
        scroll = Math.max(0, Math.min(maxScroll, scroll + lines));
    }

    @Override
    public void tick() {
        input.tick();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
