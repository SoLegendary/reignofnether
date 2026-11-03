package com.solegendary.reignofnether.util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Smoothly eases a per-entity float toward a target along a cubic Bezier curve.
 * Create one instance per animated property (scale, opacity, etc.).
 * Render-thread only (not synchronized).
 */
public final class EntityEaser {

    // Presets (CSS-style cubic-bezier x1, y1, x2, y2)
    public static final float[] EASE_IN_OUT = {0.35f, 0.0f, 0.65f, 1.0f};

    private final float durationTicks;
    private final float x1, y1, x2, y2;
    private final Map<Entity, State> states = new WeakHashMap<>();

    private static final class State {
        float from, to, current, startTime;
        State(float value) { from = to = current = value; }
    }

    public EntityEaser(float durationTicks, float[] curve) {
        this(durationTicks, curve[0], curve[1], curve[2], curve[3]);
    }

    public EntityEaser(float durationTicks, float x1, float y1, float x2, float y2) {
        this.durationTicks = durationTicks;
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
    }

    /**
     * Returns the eased value for this entity this frame.
     * The first call for an entity snaps to the target (no animation on spawn).
     * If the target changes mid-transition, the new curve starts from the
     * currently displayed value, so there's no pop.
     */
    public float get(Entity entity, float target, float partialTicks) {
        float now = entity.tickCount + partialTicks;
        State s = states.computeIfAbsent(entity, e -> new State(target));

        if (s.to != target) {
            s.from = s.current;
            s.to = target;
            s.startTime = now;
        }

        float progress = Mth.clamp((now - s.startTime) / durationTicks, 0f, 1f);
        s.current = s.from + (s.to - s.from) * ease(progress);
        return s.current;
    }

    /** Drops an entity's state, e.g. to force a snap on next get(). */
    public void reset(Entity entity) {
        states.remove(entity);
    }

    private float ease(float progress) {
        if (progress <= 0f) return 0f;
        if (progress >= 1f) return 1f;

        float lo = 0f, hi = 1f, t = progress;
        for (int i = 0; i < 12; i++) {
            t = (lo + hi) * 0.5f;
            if (bezier(t, x1, x2) < progress) lo = t; else hi = t;
        }
        return bezier(t, y1, y2);
    }

    /** 1D cubic bezier with endpoints fixed at 0 and 1. */
    private static float bezier(float t, float p1, float p2) {
        float u = 1f - t;
        return 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t;
    }
}