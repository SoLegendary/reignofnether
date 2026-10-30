package com.solegendary.reignofnether.debug;

import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.hud.panels.TopRightPanel;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
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
    public static double pathComputeMs = 0; // avg pure A* compute time per path
    public static double pathE2eMs = 0;     // avg submit -> delivered time per path (incl. queue wait)

    public static double getCappedTPS() {
        return Math.min(1000.0 / tickTime, 20);
    }

    @SubscribeEvent
    public static void onInput(InputEvent.Key evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS) {
            if (!Keybindings.shiftMod.isDown() && evt.getKey() == Keybindings.getFnum(8).getKey()) {
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
        // this event fires once per overlay
        if (displayMode == DebugDisplayMode.NONE || evt.getOverlay() != VanillaGuiOverlay.DEBUG_TEXT.type())
            return;

        GuiGraphics g = evt.getGuiGraphics();
        int x = HudGeometry.scale().width() - 95;
        int lineH = 10;
        int y = TopRightPanel.bottom - lineH; // the first line is at y + lineH

        double worldTPS = Math.min(1000.0 / tickTime, 99.99);

        int tpsCol = worldTPS < 10 ? 0xFF0000 : worldTPS < 20 ? 0xFFFF00 : 0x00FF00; // red, yellow, green
        String fps = MC.fpsString.length() >= 6 ? MC.fpsString.substring(0, 6).replace("fps", "") : "?";

        Text.atGui(g, 0, 0, () -> {
            g.drawString(MC.font, "Tick: " + String.format("%.2f", tickTime), x, y + lineH, tpsCol);
            g.drawString(MC.font, "TPS: " + String.format("%.2f", worldTPS),  x, y + lineH * 2, tpsCol);
            g.drawString(MC.font, "FPS: " + fps,                              x, y + lineH * 3,  0xFFFFFF);
            if (displayMode == DebugDisplayMode.PATHFINDING) {
                g.drawString(MC.font, "Units: " + UnitClientEvents.getAllUnits().size(),  x, y + lineH * 4, 0xFFFFFF);
                g.drawString(MC.font, "Paths: " + RtsDebugPathPreview.displayedPathCount(), x, y + lineH * 5, 0xFFFFFF);
                g.drawString(MC.font, "Paths/s: " + pathsAvg,                             x, y + lineH * 6, 0xFFFFFF);
                g.drawString(MC.font, "Queue: " + queueAvg,                               x, y + lineH * 7, 0xFFFFFF);
                g.drawString(MC.font, "Stuck: " + stuckAvg,                               x, y + lineH * 8, stuckAvg > 0 ? 0xFF6060 : 0xFFFFFF);
                g.drawString(MC.font, "Path ms: " + String.format("%.2f", pathComputeMs),  x, y + lineH * 9, 0xFFFFFF);
                g.drawString(MC.font, "Path e2e: " + String.format("%.2f", pathE2eMs),     x, y + lineH * 10, 0xFFFFFF);
            }
            if (displayMode == DebugDisplayMode.NAVMESH) {
                g.drawString(MC.font, "NAVMESH",            x, y + lineH * 4, 0xFFFFFF);
                g.drawString(MC.font, "green=open",         x, y + lineH * 5, 0x40FF40);
                g.drawString(MC.font, "red=crowded",        x, y + lineH * 6, 0xFF4040);
                g.drawString(MC.font, "orange=no fit",      x, y + lineH * 7, 0xFF9020);
                g.drawString(MC.font, "tiles: " + RtsDebugNavmesh.scoreTileCount(), x, y + lineH * 8, 0xFFFFFF);
            }
        });
    }
}
