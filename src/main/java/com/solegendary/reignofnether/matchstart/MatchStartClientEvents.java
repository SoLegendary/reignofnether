package com.solegendary.reignofnether.matchstart;

import com.solegendary.reignofnether.chat.ChatLog;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

public class MatchStartClientEvents {

    private static final int COUNTDOWN_TICKS_MAX = 100;
    private static int countdownTicks = -1;
    private static boolean wasStartingLastTick = false;
    private static boolean wasOrthoLastTick = false;

    public static int getCountdownTicks() {
        return countdownTicks;
    }

    public static void dismiss() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MatchStartScreen) {
            mc.setScreen(null);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        tickCountdown();

        boolean orthoOn = OrthoviewClientEvents.isEnabled();
        wasOrthoLastTick = orthoOn;

        // Close the screen if ortho is disabled or player is rts-locked
        if (mc.screen instanceof MatchStartScreen) {
            if (!orthoOn || PlayerClientEvents.rtsLocked) {
                mc.setScreen(null);
            }
        }
    }

    private static void tickCountdown() {
        boolean starting = StartPosClientEvents.isStarting;
        if (starting && !wasStartingLastTick) {
            countdownTicks = COUNTDOWN_TICKS_MAX;
        } else if (!starting) {
            countdownTicks = -1;
        } else if (countdownTicks > 0) {
            countdownTicks--;
        }
        wasStartingLastTick = starting;
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut evt) {
        countdownTicks = -1;
        wasStartingLastTick = false;
        wasOrthoLastTick = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MatchStartScreen) {
            mc.setScreen(null);
        }
    }
}