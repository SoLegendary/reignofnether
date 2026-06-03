package com.solegendary.reignofnether.commands;

import com.solegendary.reignofnether.tps.TPSClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// Top-right perf-stats overlay. Only renders while RtsDebug.enabled is true.
public class RtsDebugClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    // Server-pushed perf counters, averaged over a 5-second rolling window.
    // Updated once per second via RtsDebugStatsClientboundPacket.
    public static int pathsAvg = 0;
    public static int queueAvg = 0;
    public static int stuckAvg = 0;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre evt) {
        if (!RtsDebug.enabled || MC.font == null)
            return;

        int x = evt.getWindow().getGuiScaledWidth() - 110;
        int y = 5;
        int lineH = 10;

        double tps = TPSClientEvents.getCappedTPS();
        int tpsCol = tps < 10 ? 0xFF0000 : tps < 20 ? 0xFFFF00 : 0x00FF00;

        String fps = MC.fpsString != null && MC.fpsString.length() >= 6 ? MC.fpsString.substring(0, 6).replace("fps", "") : "?";

        int stuckCol = stuckAvg > 0 ? 0xFF6060 : 0xFFFFFF;

        evt.getGuiGraphics().drawString(MC.font, "[rts-debug] (5s avg)",          x, y,             0xFFFF00);
        evt.getGuiGraphics().drawString(MC.font, "TPS:    " + String.format("%.1f", tps),  x, y + lineH,      tpsCol);
        evt.getGuiGraphics().drawString(MC.font, "FPS:    " + fps,                         x, y + lineH * 2,  0xFFFFFF);
        evt.getGuiGraphics().drawString(MC.font, "Units:  " + UnitClientEvents.getAllUnits().size(),       x, y + lineH * 3, 0xFFFFFF);
        evt.getGuiGraphics().drawString(MC.font, "Sel:    " + UnitClientEvents.getSelectedUnits().size(),  x, y + lineH * 4, 0xFFFFFF);
        evt.getGuiGraphics().drawString(MC.font, "Paths:  " + UnitClientEvents.displayedPathCount(),       x, y + lineH * 5, 0xFFFFFF);
        evt.getGuiGraphics().drawString(MC.font, "Paths/s:" + pathsAvg,                                    x, y + lineH * 6, 0xFFFFFF);
        evt.getGuiGraphics().drawString(MC.font, "Queue:  " + queueAvg,                                    x, y + lineH * 7, 0xFFFFFF);
        evt.getGuiGraphics().drawString(MC.font, "Stuck:  " + stuckAvg,                                    x, y + lineH * 8, stuckCol);
    }
}
