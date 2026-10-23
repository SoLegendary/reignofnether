package com.solegendary.reignofnether.ui;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** The blurred nether image used behind menus (title, pause, options and match setup). */
public final class Backdrop {

    private Backdrop() { }

    private static final ResourceLocation IMAGE = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/gui/nether_backdrop.png");
    private static final int IMG_W = 1280;
    private static final int IMG_H = 800;

    /**
     * Fills the screen with the image (cropped to cover it) and darkens it.
     *
     * @param dim 0 = full brightness, 1 = black
     */
    public static void render(GuiGraphics g, int width, int height, float dim) {
        float scale = Math.max(width / (float) IMG_W, height / (float) IMG_H);
        int w = Math.round(IMG_W * scale);
        int h = Math.round(IMG_H * scale);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        g.blit(IMAGE, x, y, w, h, 0, 0, IMG_W, IMG_H, IMG_W, IMG_H);
        g.fill(0, 0, width, height, Theme.withAlpha(0xFF0D0305, dim));
        // darker top and bottom like the design
        g.fillGradient(0, 0, width, height / 3, 0x660D0305, 0x000D0305);
        g.fillGradient(0, height * 2 / 3, width, height, 0x000D0305, 0x990D0305);
    }
}
