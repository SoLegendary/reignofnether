package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.loading.LoadingScreenClientEvents;
import com.solegendary.reignofnether.matchstart.MatchEndScreen;
import com.solegendary.reignofnether.matchstart.MatchStartScreen;
import com.solegendary.reignofnether.ui.EdgeSlide;
import com.solegendary.reignofnether.ui.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * The HUD leaving and coming back through the screen's edges (EdgeSlide): it leaves while a match screen (start,
 * resume, end) is up, and comes in once the loading screen is done or that screen closes. The pause menu and chat
 * leave it in place.
 */
public final class HudMotion {

    private HudMotion() { }

    private static final long NOT_DRAWN_MS = 500; // not drawn this long (RTS view off): it comes in fresh next time

    private static boolean shown = false;
    private static long changedMs = 0;
    private static long lastDrawMs = 0;

    private static boolean wanted() {
        Screen s = Minecraft.getInstance().screen;
        return !LoadingScreenClientEvents.isShowing()
                && !(s instanceof MatchStartScreen || s instanceof MatchEndScreen);
    }

    /** Once a frame, before the HUD draws. */
    public static void update() {
        long now = System.currentTimeMillis();
        if (now - lastDrawMs > NOT_DRAWN_MS) {
            shown = false;
            changedMs = now - EdgeSlide.total(false); // already gone, so it comes in fresh
        }
        lastDrawMs = now;
        boolean want = wanted();
        if (want != shown) {
            shown = want;
            changedMs = now;
        }
    }

    /** How long until the HUD has left, if it were told to now: 0 when it's already gone or not drawn. */
    public static long leaveMs() {
        if (System.currentTimeMillis() - lastDrawMs > NOT_DRAWN_MS || gone())
            return 0;
        long left = shown ? EdgeSlide.total(false) : Math.max(0, EdgeSlide.total(false) - since());
        return (long) (left / LEAVE_SPEED);
    }

    /** Fully off the screen: nothing to draw or click. */
    public static boolean gone() {
        return !shown && since() >= EdgeSlide.total(false);
    }

    /** Moving in or out, when its buttons aren't where they're drawn. */
    public static boolean isMoving() {
        return since() < EdgeSlide.total(shown);
    }

    // it gets out of the way quickly, about 0.3 s, so the screen that opened has the stage; it comes back at full pace
    private static final float LEAVE_SPEED = 1.6f;

    private static long since() {
        long t = System.currentTimeMillis() - changedMs;
        return shown ? t : (long) (t * LEAVE_SPEED);
    }

    /** A part of the HUD at rect (HUD units); unitScale for parts drawn without the HUD's scaling. */
    public static void push(GuiGraphics g, Rect rect, HudLayout layout, float unitScale) {
        EdgeSlide.push(g, rect, layout.width(), layout.height(), since(), shown, unitScale);
    }

    public static void pushFade(GuiGraphics g) {
        EdgeSlide.pushFade(g, since(), shown);
    }

    public static void pop(GuiGraphics g) {
        EdgeSlide.pop(g);
    }
}
