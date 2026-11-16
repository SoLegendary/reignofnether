package com.solegendary.reignofnether.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Pixelarticons (MIT, the launcher's icon set), stored white and tinted when drawn. The art is 12x12 pixels
 * on a 24x24 texture, so sizes that are a multiple of 6 units stay pixel-exact. PAW to SATELLITE were drawn for
 * the map tools in the same grid.
 */
public enum Icon {
    CHECK, CLOSE, PLUS, MINUS, LOCK, UNLOCK, SETTINGS("settings-cog"), MESSAGE, USERS, EYE, SHUFFLE, DICE,
    ZOOM_IN("zoom-in"), ZOOM_OUT("zoom-out"), UNDO, REDO, LIST("bulletlist"), MAP, BOOK_OPEN("book-open"), GLOBE,
    PAW, LEAF, SLIDERS, EXPAND, SHRINK, SATELLITE;

    public final ResourceLocation texture;

    Icon() {
        this(null);
    }

    Icon(String file) {
        String name = file != null ? file : name().toLowerCase();
        this.texture = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/gui/icons/" + name + ".png");
    }

    public void draw(GuiGraphics g, int x, int y, int size, int colour) {
        drawTexture(g, texture, x, y, size, colour);
    }

    /** Any 24x24 icon texture drawn tinted, e.g. a button's pixelarticon. */
    public static void drawTexture(GuiGraphics g, ResourceLocation texture, int x, int y, int size, int colour) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(((colour >> 16) & 0xFF) / 255f, ((colour >> 8) & 0xFF) / 255f, (colour & 0xFF) / 255f,
                ((colour >>> 24) & 0xFF) / 255f);
        g.blit(texture, x, y, size, size, 0, 0, 24, 24, 24, 24);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    /** Centred in a box, at the largest pixel-exact size that fits (multiples of 6, at least 6). */
    public void drawIn(GuiGraphics g, int x, int y, int w, int h, int colour) {
        int size = Math.max(6, Math.min(w, h) / 6 * 6);
        draw(g, x + (w - size) / 2, y + (h - size) / 2, size, colour);
    }
}
