package com.solegendary.reignofnether.ui;

import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nullable;

/**
 * How a full screen comes and goes: each part slides in through its nearest edge, the outermost first, and leaves
 * the same way, the innermost first (see EdgeSlide). Each part is drawn between push and pop with its rect.
 */
public final class ScreenMotion {

    private final long openedMs;
    private long closingMs = 0;
    @Nullable
    private Runnable whenClosed = null;

    public ScreenMotion() {
        this(0);
    }

    /** @param delayMs how long its parts wait off screen before coming in (while the HUD leaves) */
    public ScreenMotion(long delayMs) {
        openedMs = System.currentTimeMillis() + delayMs;
    }

    /** Plays the way out, then runs close; a second call while leaving does nothing. */
    public void close(Runnable close) {
        if (closingMs != 0)
            return;
        closingMs = System.currentTimeMillis();
        whenClosed = close;
    }

    public boolean isClosing() {
        return closingMs != 0;
    }

    /** True while parts are still moving, when clicks would land where they are going rather than where they are. */
    public boolean isMoving() {
        return closingMs != 0 || System.currentTimeMillis() - openedMs < EdgeSlide.total(true);
    }

    /** Call once a frame, after drawing: closes for real once the last part has gone. */
    public void tick() {
        if (closingMs != 0 && whenClosed != null && System.currentTimeMillis() - closingMs >= EdgeSlide.total(false)) {
            Runnable close = whenClosed;
            whenClosed = null;
            close.run();
        }
    }

    private long since() {
        return System.currentTimeMillis() - (closingMs != 0 ? closingMs : openedMs);
    }

    /** Starts a part at rect on a w×h screen: everything drawn until pop moves and fades with it. */
    public void push(GuiGraphics g, Rect rect, int w, int h) {
        EdgeSlide.push(g, rect, w, h, since(), closingMs == 0, 1);
    }

    /** Starts a part that only fades, like the wash behind everything. */
    public void pushFade(GuiGraphics g) {
        EdgeSlide.pushFade(g, since(), closingMs == 0);
    }

    public void pop(GuiGraphics g) {
        EdgeSlide.pop(g);
    }
}
