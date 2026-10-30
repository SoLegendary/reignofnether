package com.solegendary.reignofnether.hud.panels;

import com.mojang.blaze3d.systems.RenderSystem;
import com.solegendary.reignofnether.ui.Easing;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Popups and panels that open and close: they rise into place with a little bounce and fade in, and when closed
 * drop away and fade out instead of vanishing. Whatever drew them last frame draws them on the way out, into a
 * throwaway frame so a panel that's leaving can't be clicked.
 */
public final class Appear {

    private Appear() { }

    private static final long IN_MS = 280;
    private static final long OUT_MS = 160;
    private static final float RISE = 22; // HUD units it travels

    private static final class State {
        boolean open;
        long changedMs;
        Consumer<HudFrame> last;
    }

    private static final Map<String, State> STATES = new HashMap<>();

    /** Call every frame with whether it's open; draw is how to draw it. */
    public static void draw(HudFrame f, String key, boolean open, Consumer<HudFrame> draw) {
        long now = System.currentTimeMillis();
        State s = STATES.computeIfAbsent(key, k -> new State());
        if (s.open != open) {
            s.open = open;
            s.changedMs = now;
        }
        float t = (now - s.changedMs) / (float) (open ? IN_MS : OUT_MS);
        if (open) {
            s.last = draw;
            push(f, (1 - Easing.heavy(t)) * RISE, smooth(t * 2));
            draw.accept(f);
            pop(f);
        } else if (s.last != null && t < 1) {
            push(f, Easing.dive(t) * RISE, 1 - smooth(t));
            s.last.accept(new HudFrame(f.g, f.scale, f.layout, -10000, -10000));
            pop(f);
        } else {
            s.last = null;
        }
    }

    private static void push(HudFrame f, float dy, float alpha) {
        f.g.flush();
        f.g.pose().pushPose();
        f.g.pose().translate(0, dy, 0);
        RenderSystem.setShaderColor(1, 1, 1, Mth.clamp(alpha, 0, 1));
    }

    private static void pop(HudFrame f) {
        f.g.flush();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        f.g.pose().popPose();
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
