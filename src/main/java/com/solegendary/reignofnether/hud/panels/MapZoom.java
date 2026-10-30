package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import net.minecraft.client.resources.language.I18n;

/** Zoom level of a map view (1 = the whole map) with its + and − buttons. */
public final class MapZoom {

    private static final float[] STEPS = { 1f, 1.5f, 2f, 3f, 4f };
    private static final int BTN = HudLayout.ZOOM;

    private int index;
    private int minIndex;

    public MapZoom(float start) {
        for (int i = 0; i < STEPS.length; i++)
            if (STEPS[i] == start)
                index = i;
    }

    /** Starts at the furthest it can zoom out. */
    public static MapZoom minimum(float min) {
        MapZoom zoom = new MapZoom(min);
        zoom.minIndex = zoom.index;
        return zoom;
    }

    public float value() {
        return STEPS[index];
    }

    /** + and − side by side, the bottom-left corner at (x, bottom). */
    public void renderButtons(HudFrame f, int x, int bottom) {
        renderIn(f, x, bottom - BTN, BTN);
        renderOut(f, x + BTN + 2, bottom - BTN, BTN);
    }

    public void renderIn(HudFrame f, int x, int y, int size) {
        button(f, true, x, y, size, index < STEPS.length - 1, () -> index++);
    }

    public void renderOut(HudFrame f, int x, int y, int size) {
        button(f, false, x, y, size, index > minIndex, () -> index--);
    }

    private static void button(HudFrame f, boolean in, int x, int y, int size, boolean enabled, Runnable action) {
        Rect r = new Rect(x, y, size, size);
        Theme.secondaryButton(f.g, x, y, size, size, enabled && f.hovered(r));
        (in ? Icon.ZOOM_IN : Icon.ZOOM_OUT).drawIn(f.g, x + 3, y + 3, size - 6, size - 6, enabled ? Theme.BODY : Theme.FAINT);
        f.register(new ButtonBuilder(in ? "map zoom in" : "map zoom out").isEnabled(() -> enabled).onLeftClick(action).build(), r, null,
                () -> Tooltip.of(I18n.get(in ? "hud.reignofnether.map.zoom_in" : "hud.reignofnether.map.zoom_out")));
    }
}
