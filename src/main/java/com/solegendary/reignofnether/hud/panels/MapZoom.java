package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import net.minecraft.client.resources.language.I18n;

import java.util.function.Consumer;

/** Zoom level of a map view (1 = the whole map) with its + and − buttons. */
public final class MapZoom {

    private static final float[] STEPS = { 1f, 1.5f, 2f, 3f, 4f };

    private int index;
    private Consumer<Boolean> wideStep = null; // one step past the whole map: the map itself covers more world
    private boolean wide = false;

    public MapZoom(float start) {
        for (int i = 0; i < STEPS.length; i++)
            if (STEPS[i] == start)
                index = i;
    }

    /** Adds a zoom out past the last step, handled by the owner of the map. */
    public MapZoom withWideStep(Consumer<Boolean> wideStep) {
        this.wideStep = wideStep;
        return this;
    }

    private void setWide(boolean value) {
        wide = value;
        wideStep.accept(value);
    }

    public float value() {
        return STEPS[index];
    }

    public void renderIn(HudFrame f, int x, int y, int size) {
        if (wide)
            button(f, true, x, y, size, true, () -> setWide(false));
        else
            button(f, true, x, y, size, index < STEPS.length - 1, () -> index++);
    }

    public void renderOut(HudFrame f, int x, int y, int size) {
        if (wideStep != null && index == 0)
            button(f, false, x, y, size, !wide, () -> setWide(true));
        else
            button(f, false, x, y, size, index > 0, () -> index--);
    }

    private static void button(HudFrame f, boolean in, int x, int y, int size, boolean enabled, Runnable action) {
        Rect r = new Rect(x, y, size, size);
        Theme.secondaryButton(f.g, x, y, size, size, enabled && f.hovered(r));
        (in ? Icon.ZOOM_IN : Icon.ZOOM_OUT).drawIn(f.g, x + 3, y + 3, size - 6, size - 6, enabled ? Theme.BODY : Theme.FAINT);
        f.register(new ButtonBuilder(in ? "map zoom in" : "map zoom out").isEnabled(() -> enabled).onLeftClick(action).build(), r, null,
                () -> Tooltip.of(I18n.get(in ? "hud.reignofnether.map.zoom_in" : "hud.reignofnether.map.zoom_out")));
    }
}
