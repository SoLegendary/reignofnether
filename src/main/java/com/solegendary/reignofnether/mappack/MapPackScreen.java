package com.solegendary.reignofnether.mappack;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ui.Backdrop;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Title screen popup that downloads the official map pack, from the map pack button.
 * Laid out in HUD units like the rest of the RTS UI, so it keeps its proportions at any GUI scale.
 */
public class MapPackScreen extends Screen {

    private static final ResourceLocation BANNER =
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/gui/mappack.png");
    private static final int BANNER_W = 640, BANNER_H = 286;
    private static final int PAD = 16, BUTTON_H = 36;

    private record Hit(int x, int y, int w, int h, Runnable action) { }

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();

    public MapPackScreen(Screen parent) {
        super(Component.translatable("mappack.reignofnether.title"));
        this.parent = parent;
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Backdrop.render(g, width, height, 0.6f);
        float k = Text.guiPerUnit();
        int screenW = Math.round(width / k), screenH = Math.round(height / k);
        int mx = Math.round(mouseX / k), my = Math.round(mouseY / k);
        hits.clear();

        MapPack.State state = MapPack.state();
        int w = Math.min(600, screenW - 32);
        int imgH = w * 3 / 10;
        List<FormattedCharSequence> body = Text.wrap(Component.translatable(state == MapPack.State.INSTALLED
                ? "mappack.reignofnether.body_installed" : "mappack.reignofnether.body"), w - 2 * PAD);
        int lines = body.size() + (state == MapPack.State.FAILED ? 1 : 0);
        int controlsH = state == MapPack.State.DOWNLOADING ? BUTTON_H + 30 : BUTTON_H;
        int h = imgH + PAD + 12 + 24 + 8 + lines * Text.lineHeight() + PAD + controlsH + PAD;
        int x = (screenW - w) / 2, y = Math.max(8, (screenH - h) / 2);

        g.pose().pushPose();
        g.pose().scale(k, k, 1);
        Theme.popup(g, x, y, w, h, Theme.ORANGE);
        // the banner is wider than tall: show its middle band
        int srcH = imgH * BANNER_W / w;
        g.blit(BANNER, x, y + 2, w, imgH, 0, (BANNER_H - srcH) / 2f, BANNER_W, srcH, BANNER_W, BANNER_H);

        int tx = x + PAD, ty = y + imgH + PAD;
        int kickerColour = switch (state) {
            case INSTALLED -> Theme.OK;
            case FAILED -> Theme.ERROR;
            default -> Theme.ORANGE;
        };
        Text.small(g, tr(kickerKey(state)), tx, ty, kickerColour);
        Text.title(g, title.getString().toUpperCase(), tx, ty + 12, 2f);
        int ly = ty + 12 + 24;
        for (FormattedCharSequence line : body) {
            Text.draw(g, line, tx, ly, Theme.DESC);
            ly += Text.lineHeight();
        }
        if (state == MapPack.State.FAILED)
            Text.draw(g, Text.fit(MapPack.error(), w - 2 * PAD), tx, ly, Theme.ERROR);

        int by = y + h - PAD - BUTTON_H, bw = w - 2 * PAD;
        switch (state) {
            case MISSING, FAILED -> {
                int notNowW = Math.max(100, Text.mediumWidth(tr("mappack.reignofnether.not_now")) + 24);
                button(g, tr("mappack.reignofnether.download"), tx, by, bw - notNowW - 8, true, mx, my, MapPack::download);
                button(g, tr("mappack.reignofnether.not_now"), tx + bw - notNowW, by, notNowW, false, mx, my, this::onClose);
            }
            case DOWNLOADING -> {
                float p = MapPack.progress();
                int barY = by - 30;
                Theme.bar(g, tx, barY, bw, 8, Math.max(p, 0), Theme.ORANGE);
                Text.small(g, tr(kickerKey(state)), tx, barY + 12, Theme.LABEL);
                if (p >= 0)
                    Text.smallRight(g, Math.round(p * 100) + "%", tx + bw, barY + 12, Theme.GOLD);
                button(g, tr("mappack.reignofnether.background"), tx, by, bw, false, mx, my, this::onClose);
            }
            case INSTALLED -> button(g, tr("mappack.reignofnether.done"), tx, by, bw, true, mx, my, this::onClose);
        }
        g.pose().popPose();
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
        if (button == 0)
            for (Hit h : List.copyOf(hits))
                if (mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h) {
                    h.action.run();
                    return true;
                }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static String kickerKey(MapPack.State state) {
        return switch (state) {
            case MISSING -> "mappack.reignofnether.missing";
            case DOWNLOADING -> MapPack.progress() < 0 ? "mappack.reignofnether.installing" : "mappack.reignofnether.downloading";
            case INSTALLED -> "mappack.reignofnether.installed";
            case FAILED -> "mappack.reignofnether.failed";
        };
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
