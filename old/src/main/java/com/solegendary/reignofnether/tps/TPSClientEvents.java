package com.solegendary.reignofnether.tps;

import com.solegendary.reignofnether.keybinds.Keybindings;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class TPSClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static double tickTime = 10; // time to perform one tick in milliseconds
    private static boolean enabled = false;

    public static void updateTickTime(double tickTime) {
        TPSClientEvents.tickTime = tickTime;
    }

    public static double getCappedTPS() {
        return Math.min(1000.0 / tickTime, 20);
    }

    @SubscribeEvent
    // can't use ScreenEvent.KeyboardKeyPressedEvent as that only happens when a screen is up
    public static void onInput(InputEvent.Key evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS) {
            if (evt.getKey() == Keybindings.getFnum(7).key)
                enabled = !enabled;
        }
    }

    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        if (!enabled)
            return;

        int x = evt.getWindow().getGuiScaledWidth() - 55;
        int y = 35;

        double worldTPS = Math.min(1000.0 / tickTime, 99.99);

        // ARGB, shaded red, yellow or white
        int col = 0x00FF00;
        if (worldTPS < 10) {
            col = 0xFF0000;
        } else if (worldTPS < 20) {
            col = 0xFFFF00;
        }
        String tickStr = "Tick: " + String.format("%.2f", tickTime);
        evt.getGuiGraphics().drawString(MC.font, tickStr, x,y, col);

        // technically is bound to 20TPS but good to see the theoretical amount
        String tpsStr = "TPS: " + String.format("%.2f", worldTPS);
        evt.getGuiGraphics().drawString(MC.font, tpsStr, x,y + 10, col);

        String fpsStr = "FPS: " + Minecraft.getInstance().fpsString.substring(0,6).replace("fps","");
        evt.getGuiGraphics().drawString(MC.font, fpsStr, x,y + 20, 0xFFFFFFFF);
    }
}
