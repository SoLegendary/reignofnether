package com.solegendary.reignofnether.ui;

import net.minecraft.util.Mth;

/** Motion curves for things that should feel heavy rather than slide at one speed. All take and give 0 to 1. */
public final class Easing {

    private Easing() { }

    private static final float BACK = 0.9f * 1.525f; // how far it leans back and overshoots: 0 for none

    /**
     * A heavy block given a shove: it leans back a touch, gathers speed, then slows and settles a little past where
     * it stops before coming back. Goes slightly outside 0 to 1.
     */
    public static float heavy(float t) {
        t = Mth.clamp(t, 0, 1);
        return t < 0.5f
                ? 4 * t * t * ((BACK + 1) * 2 * t - BACK) / 2
                : ((2 * t - 2) * (2 * t - 2) * ((BACK + 1) * (2 * t - 2) + BACK) + 2) / 2;
    }

    /** The same lean back and build-up, then it slows to a stop without passing it, for what mustn't overshoot. */
    public static float push(float t) {
        t = Mth.clamp(t, 0, 1);
        return t < 0.5f ? heavy(t) : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    /** Slow to start, then rushing in. */
    public static float dive(float t) {
        t = Mth.clamp(t, 0, 1);
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }
}
