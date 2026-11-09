package com.solegendary.reignofnether.ui;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector4f;

/** Scissor clipping in the current pose's coordinates (the GUI scissor itself ignores the pose). */
public final class Clip {

    private Clip() { }

    public static void push(GuiGraphics g, int x, int y, int w, int h) {
        var m = g.pose().last().pose();
        Vector4f a = m.transform(new Vector4f(x, y, 0, 1));
        Vector4f b = m.transform(new Vector4f(x + w, y + h, 0, 1));
        g.enableScissor(Math.round(a.x), Math.round(a.y), Math.round(b.x), Math.round(b.y));
    }

    public static void pop(GuiGraphics g) {
        g.disableScissor();
    }
}
