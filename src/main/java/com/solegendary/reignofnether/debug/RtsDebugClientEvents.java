package com.solegendary.reignofnether.debug;

import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

// RTS debug mode toggle + top-right perf-stats HUD. The F-key cycles the shared display mode; the navmesh and
// path-preview overlays (RtsDebugNavmesh, RtsDebugPathPreview) read it to decide whether to draw.
public class RtsDebugClientEvents {

    enum DebugDisplayMode {
        NONE,
        NO_PATHFINDING,
        PATHFINDING,
        NAVMESH // per-block walkability + cost overlay for the selected unit
    }

    private static final Minecraft MC = Minecraft.getInstance();
    static DebugDisplayMode displayMode = DebugDisplayMode.NONE;

    // Server-pushed perf counters, averaged over a 5-second rolling window.
    // Updated once per second via RtsDebugStatsClientboundPacket.
    public static int pathsAvg = 0;
    public static int queueAvg = 0;
    public static int stuckAvg = 0;
    public static double tickTime = 10; // time to perform one tick in milliseconds

    public static double getCappedTPS() {
        return Math.min(1000.0 / tickTime, 20);
    }

    @SubscribeEvent
    public static void onInput(InputEvent.Key evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS) {
            if (evt.getKey() == Keybindings.getFnum(7).getKey()) {
                if (displayMode == DebugDisplayMode.NONE)
                    displayMode = DebugDisplayMode.NO_PATHFINDING;
                else if (displayMode == DebugDisplayMode.NO_PATHFINDING)
                    displayMode = DebugDisplayMode.PATHFINDING;
                else if (displayMode == DebugDisplayMode.PATHFINDING)
                    displayMode = DebugDisplayMode.NAVMESH;
                else if (displayMode == DebugDisplayMode.NAVMESH)
                    displayMode = DebugDisplayMode.NONE;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre evt) {
        if (displayMode == DebugDisplayMode.NONE)
            return;

        int x = evt.getWindow().getGuiScaledWidth() - (displayMode == DebugDisplayMode.NONE ? 85 : 95);
        int y = 25;
        int lineH = 10;

        double worldTPS = Math.min(1000.0 / tickTime, 99.99);

        int tpsCol = 0x00FF00; // green
        if (worldTPS < 10) {
            tpsCol = 0xFF0000; // red
        } else if (worldTPS < 20) {
            tpsCol = 0xFFFF00; // yellow
        }
        String fps = MC.fpsString.length() >= 6 ? MC.fpsString.substring(0, 6).replace("fps", "") : "?";

        evt.getGuiGraphics().drawString(MC.font, "Tick: " + String.format("%.2f", tickTime), x, y + lineH, tpsCol);
        evt.getGuiGraphics().drawString(MC.font, "TPS: " + String.format("%.2f", worldTPS),  x, y + lineH * 2, tpsCol);
        evt.getGuiGraphics().drawString(MC.font, "FPS: " + fps,                              x, y + lineH * 3,  0xFFFFFF);
        if (displayMode == DebugDisplayMode.PATHFINDING) {
            evt.getGuiGraphics().drawString(MC.font, "Units: " + UnitClientEvents.getAllUnits().size(),  x, y + lineH * 4, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, "Paths: " + RtsDebugPathPreview.displayedPathCount(), x, y + lineH * 5, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, "Paths/s: " + pathsAvg,                             x, y + lineH * 6, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, "Queue: " + queueAvg,                               x, y + lineH * 7, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, "Stuck: " + stuckAvg,                               x, y + lineH * 8, stuckAvg > 0 ? 0xFF6060 : 0xFFFFFF);
        }
        if (displayMode == DebugDisplayMode.NAVMESH) {
            evt.getGuiGraphics().drawString(MC.font, "NAVMESH",            x, y + lineH * 4, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, "green=open",         x, y + lineH * 5, 0x40FF40);
            evt.getGuiGraphics().drawString(MC.font, "red=crowded",        x, y + lineH * 6, 0xFF4040);
            evt.getGuiGraphics().drawString(MC.font, "orange=no fit",      x, y + lineH * 7, 0xFF9020);
            evt.getGuiGraphics().drawString(MC.font, "tiles: " + RtsDebugNavmesh.scoreTileCount(), x, y + lineH * 8, 0xFFFFFF);
        }
    }
}
