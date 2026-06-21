package com.solegendary.reignofnether.debug;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;

// Top-right perf-stats overlay. Only renders while RtsDebug.enabled is true.
public class RtsDebugClientEvents {

    enum DebugDisplayMode {
        NONE,
        NO_PATHFINDING,
        PATHFINDING
    }

    private static final Minecraft MC = Minecraft.getInstance();
    private static DebugDisplayMode displayMode = DebugDisplayMode.NONE;

    // Path-line render constants used by the move-command path preview.
    public static final float PATH_LINE_R = 0.2f;
    public static final float PATH_LINE_G = 1.0f;
    public static final float PATH_LINE_B = 0.4f;
    public static final float PATH_LINE_BASE_ALPHA = 0.9f;
    public static final float PATH_LINE_Y_OFFSET = 0.1f;
    public static final int PATH_DISPLAY_TICKS = 40; // ~2s at 20 tps

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
                    displayMode = DebugDisplayMode.NONE;
            }
        }
    }

    // Path-preview state. Keyed by entityId. Decremented in onClientTick. Entries auto-purge at zero.
    public static class PathDisplay {
        public final java.util.List<BlockPos> nodes;
        public int ticksRemaining;
        public PathDisplay(java.util.List<BlockPos> nodes, int ticksRemaining) {
            this.nodes = nodes;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private static final HashMap<Integer, PathDisplay> displayedPaths = new HashMap<>();

    public static void receiveUnitPath(int entityId, List<BlockPos> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            displayedPaths.remove(entityId);
            return;
        }
        displayedPaths.put(entityId, new PathDisplay(nodes, PATH_DISPLAY_TICKS));
    }

    public static int displayedPathCount() { return displayedPaths.size(); }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre evt) {
        if (displayMode == DebugDisplayMode.NONE)
            return;

        int x = evt.getWindow().getGuiScaledWidth() - (displayMode == DebugDisplayMode.PATHFINDING ? 95 : 85);
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
            evt.getGuiGraphics().drawString(MC.font, "Units: " + UnitClientEvents.getAllUnits().size(), x, y + lineH * 4, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, "Paths: " + displayedPathCount(),                  x, y + lineH * 5, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, "Paths/s: " + pathsAvg,                            x, y + lineH * 6, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, "Queue: " + queueAvg,                              x, y + lineH * 7, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, "Stuck: " + stuckAvg,                              x, y + lineH * 8, stuckAvg > 0 ? 0xFF6060 : 0xFFFFFF);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        // Tick path-preview entries down. Remove expired in a single pass.
        // When rts-debug is enabled, entries don't expire on a timer — they persist until either
        // a new path arrives or the unit reaches the last node of its current path.
        if (!displayedPaths.isEmpty()) {
            displayedPaths.entrySet().removeIf(e -> {
                PathDisplay pd = e.getValue();
                if (displayMode != DebugDisplayMode.PATHFINDING) {
                    pd.ticksRemaining -= 1;
                    if (pd.ticksRemaining <= 0) return true;
                }
                // Drop the entry once the unit is within 2 blocks of the path's last node.
                if (MC.level != null) {
                    var entity = MC.level.getEntity(e.getKey());
                    if (entity == null) return true;
                    BlockPos last = pd.nodes.get(pd.nodes.size() - 1);
                    return entity.distanceToSqr(last.getX() + 0.5, last.getY() + 0.5, last.getZ() + 0.5) < 4;
                }
                return false;
            });
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (MC.level == null)
            return;
        if (evt.getStage() == UnitClientEvents.stage && displayMode == DebugDisplayMode.PATHFINDING) {
            if (OrthoviewClientEvents.isEnabled() && evt.getStage() == UnitClientEvents.stage) {
                VertexConsumer vertexConsumerLine = MC.renderBuffers().bufferSource().getBuffer(RenderType.LINE_STRIP);
                for (LivingEntity entity : UnitClientEvents.getSelectedUnits()) {
                    if (entity instanceof Unit unit) {
                        // draw path preview — gated by /rts-debug. When debug is off, never render.
                        // When debug is on, always render at full alpha (no fade).
                        PathDisplay pd = displayedPaths.get(entity.getId());
                        if (pd != null && pd.nodes.size() >= 2 && MC.player != null
                                && (unit.getOwnerName().equals(MC.player.getName().getString())
                                || AlliancesClient.canControlAlly(unit.getOwnerName()))) {
                            BlockPos prev = null;
                            for (BlockPos node : pd.nodes) {
                                if (prev != null) {
                                    Vec3 a0 = new Vec3(prev.getX() + 0.5, prev.getY() + PATH_LINE_Y_OFFSET, prev.getZ() + 0.5);
                                    Vec3 b0 = new Vec3(node.getX() + 0.5, node.getY() + PATH_LINE_Y_OFFSET, node.getZ() + 0.5);
                                    MyRenderer.drawLine(evt.getPoseStack(), vertexConsumerLine, a0, b0,
                                            PATH_LINE_R, PATH_LINE_G, PATH_LINE_B, PATH_LINE_BASE_ALPHA);
                                }
                                prev = node;
                            }
                        }
                    }
                }
            }
        }
    }
}
