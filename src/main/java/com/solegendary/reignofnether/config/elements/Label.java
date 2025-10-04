package com.solegendary.reignofnether.config.elements;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.Rect2i;

public class Label implements Renderable {
    protected static final Minecraft MC = Minecraft.getInstance();
    private final String label;
    private int x = 0;
    private int y = 0;
    private int w = 10;
    private int h = 10;

    public Label(String label) {
        this.label = label;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        //guiGraphics.drawCenteredString(MC.font, this.label, this.x + this.w / 2, this.y + this.h / 2 - font.lineHeight / 2, 0xFFFFFFFF);
        guiGraphics.drawString(MC.font, this.label, this.x, this.y + this.h / 2 - MC.font.lineHeight / 2, 0xFFFFFFFF);
    }

    public Label pos(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Label size(int w, int h) {
        this.w = w;
        this.h = h;
        return this;
    }
}
