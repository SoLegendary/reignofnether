package com.solegendary.reignofnether.guide;

import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.soldier.SoldierHudClientEvents;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * Cutscenes sent by map scripts: the player's control taken away (no selecting or ordering, the HUD replaced by
 * letterbox bars, only the dialogue left), and the camera following a unit, turning or shaking.
 * In first person (a soldier) follow looks through the target's eyes and rotate turns the view only, never the player:
 * once it's over and they have control, the view is back on them as they were.
 */
public class CinematicClient {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Random RANDOM = new Random();
    private static final int BAR_H = 34;

    private static boolean locked = false;
    private static int followId = -1;
    private static int followTicks = 0; // 0: until stopped
    private static float rotLeft = 0; // degrees still to turn
    private static int rotTicks = 0;
    private static float fpvYaw = 0; // first person: degrees the view is turned from the player's own
    private static float shakeStrength = 0;
    private static int shakeTicks = 0, shakeTicksMax = 1;

    static {
        WorldState.onClientReset(CinematicClient::reset);
    }

    private static void reset() {
        locked = false;
        endFollow();
        fpvYaw = 0;
        rotLeft = 0;
        rotTicks = 0;
        shakeTicks = 0;
    }

    public static boolean isLocked() {
        return locked;
    }

    static void setControl(boolean control) {
        locked = !control;
        if (control && firstPerson()) { // the cutscene's over: their own eyes, facing as before
            endFollow();
            rotLeft = 0;
            rotTicks = 0;
            fpvYaw = 0;
        }
    }

    static void follow(int entityId, int ticks) {
        if (entityId < 0)
            endFollow();
        followId = entityId;
        followTicks = ticks;
    }

    private static void endFollow() {
        followId = -1;
        if (MC.player != null && MC.getCameraEntity() != MC.player)
            MC.setCameraEntity(MC.player);
    }

    // a soldier, not a commander who left the RTS view (F12): their camera stays theirs
    private static boolean firstPerson() {
        return SoldierHudClientEvents.active() && !OrthoviewClientEvents.isEnabled();
    }

    static void rotate(float degrees, int ticks) {
        rotLeft = degrees;
        rotTicks = Math.max(1, ticks);
    }

    static void shake(float strength, int ticks) {
        shakeStrength = strength;
        shakeTicks = shakeTicksMax = Math.max(1, ticks);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || MC.level == null)
            return;
        if (followId >= 0) {
            Entity e = MC.level.getEntity(followId); // ponytail: null while out of tracking range, it just waits
            if (e != null && !e.isAlive()) {
                endFollow();
            } else if (e != null && firstPerson()) {
                if (MC.getCameraEntity() != e)
                    MC.setCameraEntity(e);
            } else if (e != null) {
                if (MC.getCameraEntity() != MC.player) // stopped being a soldier mid-follow
                    MC.setCameraEntity(MC.player);
                OrthoviewClientEvents.followPos(e.position());
            }
            if (followTicks > 0 && --followTicks == 0)
                endFollow();
        }
        if (rotTicks > 0) {
            float step = rotLeft / rotTicks;
            if (firstPerson())
                fpvYaw += step;
            else
                OrthoviewClientEvents.setCam(OrthoviewClientEvents.getZoom(), OrthoviewClientEvents.getCamRotX() + step);
            rotLeft -= step;
            rotTicks--;
        }
        // first person: back to the player's own view once the cutscene is over
        if (fpvYaw != 0 && !locked && rotTicks == 0 && followId < 0)
            fpvYaw = 0;
        if (shakeTicks > 0)
            shakeTicks--;
    }

    // first person turning, and shaking: a jolt that fades over its ticks
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles evt) {
        if (fpvYaw != 0 && firstPerson())
            evt.setYaw(evt.getYaw() + fpvYaw);
        if (shakeTicks <= 0) // the RTS camera and first person alike
            return;
        float s = shakeStrength * shakeTicks / shakeTicksMax;
        evt.setYaw(evt.getYaw() + (RANDOM.nextFloat() - 0.5f) * s);
        evt.setPitch(evt.getPitch() + (RANDOM.nextFloat() - 0.5f) * s);
        evt.setRoll(evt.getRoll() + (RANDOM.nextFloat() - 0.5f) * s * 0.5f);
    }

    // without control the RTS view only takes SPACE (the dialogue) and Esc
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKey(ScreenEvent.KeyPressed.Pre evt) {
        if (locked && evt.getScreen() instanceof TopdownGui
                && evt.getKeyCode() != GLFW.GLFW_KEY_SPACE && evt.getKeyCode() != GLFW.GLFW_KEY_ESCAPE)
            evt.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Pre evt) {
        if (locked && evt.getScreen() instanceof TopdownGui)
            evt.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseRelease(ScreenEvent.MouseButtonReleased.Pre evt) {
        if (locked && evt.getScreen() instanceof TopdownGui)
            evt.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScroll(ScreenEvent.MouseScrolled.Pre evt) {
        if (locked && evt.getScreen() instanceof TopdownGui)
            evt.setCanceled(true);
    }

    // first person (a soldier): standing still, no attacking or using
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent evt) {
        if (!locked)
            return;
        var input = evt.getInput();
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered evt) {
        if (locked) {
            evt.setSwingHand(false);
            evt.setCanceled(true);
        }
    }

    /** The letterbox bars in place of the HUD. */
    public static void renderBars(HudFrame f) {
        int w = f.layout.width(), h = f.layout.height();
        f.g.fill(0, 0, w, BAR_H, 0xFF000000);
        f.g.fill(0, h - BAR_H, w, h, 0xFF000000);
    }
}
