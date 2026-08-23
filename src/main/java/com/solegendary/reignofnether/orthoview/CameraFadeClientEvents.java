package com.solegendary.reignofnether.orthoview;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Fades the screen to black, snaps the RTS camera to a position while it is fully black,
 * then fades back in. Used by CameraFadeClientboundPacket / the rtsapi player camera fade command.
 */
public class CameraFadeClientEvents {

    public static final int DEFAULT_FADE_TICKS = 10;
    public static final int DEFAULT_BLACKOUT_TICKS = 5;

    private static final Minecraft MC = Minecraft.getInstance();

    private static boolean fading = false;
    private static int fadeOutTicks = 0;
    private static int blackoutTIcks = 0;
    private static int fadeInTicks = 0;
    private static int ticksElapsed = 0;
    private static boolean moved = false;
    private static BlockPos targetPos = null;

    public static boolean isFading() {
        return fading;
    }

    public static void fadeMoveCam(BlockPos pos, int fadeOut, int hold, int fadeIn) {
        if (MC.player == null || MC.level == null) {
            return;
        }
        // a fade already in progress is replaced by the new one rather than queued
        targetPos = pos;
        fadeOutTicks = Math.max(0, fadeOut);
        blackoutTIcks = Math.max(0, hold);
        fadeInTicks = Math.max(0, fadeIn);
        ticksElapsed = 0;
        moved = false;
        fading = true;
        OrthoviewClientEvents.lockCam();

        // with no fade out there is nothing to hide the move behind, so do it immediately
        if (fadeOutTicks <= 0) {
            doMove();
        }
    }

    public static void fadeMoveCam(String playerName, BlockPos pos, int fadeOut, int hold, int fadeIn) {
        if (MC.player != null && MC.player.getName().getString().equals(playerName)) {
            fadeMoveCam(pos, fadeOut, hold, fadeIn);
        }
    }

    private static void doMove() {
        moved = true;
        if (targetPos != null && OrthoviewClientEvents.isEnabled()) {
            OrthoviewClientEvents.centreCameraOnPos(targetPos);
        }
    }

    private static void stop() {
        fading = false;
        moved = false;
        targetPos = null;
        ticksElapsed = 0;
        OrthoviewClientEvents.unlockCam();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || !fading) {
            return;
        }
        if (MC.player == null || MC.level == null) {
            stop();
            return;
        }

        ticksElapsed += 1;

        if (!moved && ticksElapsed >= fadeOutTicks) {
            doMove();
        }
        if (ticksElapsed >= fadeOutTicks + blackoutTIcks + fadeInTicks) {
            stop();
        }
    }

    // 0 = fully transparent, 1 = fully black
    private static float getAlpha(float partialTick) {
        if (!fading) {
            return 0;
        }
        float t = ticksElapsed + partialTick;

        if (t < fadeOutTicks) {
            return Mth.clamp(t / fadeOutTicks, 0, 1);
        }
        float held = t - fadeOutTicks;
        if (held < blackoutTIcks || fadeInTicks <= 0) {
            return 1;
        }
        return Mth.clamp(1 - ((held - blackoutTIcks) / fadeInTicks), 0, 1);
    }

    private static void drawFade(GuiGraphics guiGraphics, float partialTick) {
        float alpha = getAlpha(partialTick);
        if (alpha <= 0) {
            return;
        }
        int alphaInt = (int) (alpha * 255.0f) << 24;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), alphaInt);
        RenderSystem.disableBlend();
    }

    // covers the HUD when no screen is open
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post evt) {
        if (fading && MC.screen == null) {
            drawFade(evt.getGuiGraphics(), evt.getPartialTick());
        }
    }

    // covers TopdownGui and any other open screen, drawn after its widgets
    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post evt) {
        if (fading) {
            drawFade(evt.getGuiGraphics(), evt.getPartialTick());
        }
    }
}