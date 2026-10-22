package com.solegendary.reignofnether.hud.layout;

import com.mojang.blaze3d.platform.Window;
import com.solegendary.reignofnether.ui.HudScale;
import com.solegendary.reignofnether.ui.Rect;
import net.minecraft.client.Minecraft;

/** The HUD scale and layout for the current window, recomputed only when the window or GUI Scale changes. */
public final class HudGeometry {

    private HudGeometry() { }

    private static int width, height, chosenScale;
    private static double guiScale;
    private static HudScale scale;
    private static HudLayout layout;

    // called many times a frame (every text draw), so compares fields instead of allocating a key
    public static HudScale scale() {
        Minecraft mc = Minecraft.getInstance();
        Window w = mc.getWindow();
        int chosen = w.calculateScale(mc.options.guiScale().get(), mc.isEnforceUnicode());
        if (scale == null || w.getWidth() != width || w.getHeight() != height || w.getGuiScale() != guiScale || chosen != chosenScale) {
            width = w.getWidth();
            height = w.getHeight();
            guiScale = w.getGuiScale();
            chosenScale = chosen;
            scale = HudScale.of(width, height, guiScale, chosenScale);
            layout = HudLayout.compute(scale.width(), scale.height());
        }
        return scale;
    }

    public static HudLayout layout() {
        scale();
        return layout;
    }

    /** A rectangle in HUD units converted to GUI coordinates: {x, y, w, h}. */
    public static float[] toGui(Rect r) {
        HudScale s = scale();
        return new float[] { (float) s.toGui(r.x()), (float) s.toGui(r.y()), (float) s.toGui(r.w()), (float) s.toGui(r.h()) };
    }
}
