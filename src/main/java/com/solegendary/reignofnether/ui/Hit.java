package com.solegendary.reignofnether.ui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** A click area a hand-drawn screen adds while it renders (HUD units), run on a left click over it. */
public record Hit(int x, int y, int w, int h, Runnable action) {

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Runs the first hit under the mouse (screen pixels) on a left click; false if none was. */
    public static boolean click(List<Hit> hits, double mouseX, double mouseY, int button) {
        if (button != 0)
            return false;
        float k = Text.guiPerUnit();
        for (Hit hit : List.copyOf(hits)) // an action may rebuild the list
            if (hit.contains(mouseX / k, mouseY / k)) {
                hit.action.run();
                return true;
            }
        return false;
    }

    /** Orange (primary) or grey text button that adds its click to hits; greyed out and unclickable unless clickable. */
    public static void button(List<Hit> hits, GuiGraphics g, String label, int x, int y, int w, int h, boolean primary,
                              boolean clickable, int mx, int my, Runnable action) {
        boolean hov = clickable && mx >= x && mx < x + w && my >= y && my < y + h;
        if (primary)
            Theme.primaryButton(g, x, y, w, h, hov);
        else
            Theme.secondaryButton(g, x, y, w, h, hov);
        Text.mediumCentered(g, Text.fit(label, w - 8), x + w / 2, y + (h - 8) / 2,
                primary ? Theme.ON_ORANGE : hov ? Theme.GOLD : clickable ? Theme.BODY : Theme.MUTED);
        if (clickable)
            hits.add(new Hit(x, y, w, h, action));
    }
}
