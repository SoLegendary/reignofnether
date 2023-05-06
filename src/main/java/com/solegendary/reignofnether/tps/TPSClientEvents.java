package com.solegendary.reignofnether.tps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TPSClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static double tickTime = 10; // time to perform one tick in milliseconds

    public static void updateTickTime(double tickTime) {
        TPSClientEvents.tickTime = tickTime;
    }

    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        int x = evt.getWindow().getGuiScaledWidth() - 55;
        int y = 20;

        double worldTPS = Math.min(1000.0 / tickTime, 99.99);

        // ARGB, shaded from red to green at 5-25 TPS
        double tpsForColor = worldTPS + 5;
        int r = (int) Math.round(tpsForColor / 20);
        int g = 0xFF - (int) Math.round(tpsForColor / 20);
        int b = 0;

        int col = (0xFF << 24) | (b << 16) | (g << 8) | (r);

        String tickStr = "Tick: " + String.format("%.2f", tickTime) + "ms";
        GuiComponent.drawString(evt.getPoseStack(), MC.font, tickStr, x,y, col);

        // technically is bound to 20TPS but good to see the theoretical amount
        String tpsStr = "TPS: " + String.format("%.2f", worldTPS);
        GuiComponent.drawString(evt.getPoseStack(), MC.font, tpsStr, x,y + 10, col);

        String fpsStr = "FPS: " + Minecraft.getInstance().fpsString.replace("fps","");
        GuiComponent.drawString(evt.getPoseStack(), MC.font, fpsStr, x,y + 20, 0xFFFFFFFF);
    }
}
