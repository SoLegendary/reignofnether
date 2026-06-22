package com.solegendary.reignofnether.debug;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.pathfinding.ChunkSnapshot;
import com.solegendary.reignofnether.unit.pathfinding.GridNeighbors;
import com.solegendary.reignofnether.unit.pathfinding.MobilityClass;
import com.solegendary.reignofnether.unit.pathfinding.RtsPathfinder;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
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
        PATHFINDING,
        NAVMESH // per-block walkability + cost overlay for the selected unit
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
                    displayMode = DebugDisplayMode.NAVMESH;
                else if (displayMode == DebugDisplayMode.NAVMESH)
                    displayMode = DebugDisplayMode.NONE;
            }
        }
    }

    // Path-preview state. Keyed by entityId. Decremented in onClientTick. Entries auto-purge at zero.
    public static class PathDisplay {
        public final java.util.List<BlockPos> nodes;
        public final byte pathType;
        public int ticksRemaining;
        public PathDisplay(java.util.List<BlockPos> nodes, byte pathType, int ticksRemaining) {
            this.nodes = nodes;
            this.pathType = pathType;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private static final HashMap<Integer, PathDisplay> displayedPaths = new HashMap<>();

    public static void receiveUnitPath(int entityId, byte pathType, List<BlockPos> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            displayedPaths.remove(entityId);
            return;
        }
        displayedPaths.put(entityId, new PathDisplay(nodes, pathType, PATH_DISPLAY_TICKS));
    }

    public static int displayedPathCount() { return displayedPaths.size(); }

    // --- NAVMESH / block-scoring overlay -------------------------------------------------------------------
    // A colored ground tile: where the navmesh says a cell is standable, tinted by its A* score for the
    // selected unit. Recomputed periodically (not every frame) from a client-side ChunkSnapshot.
    private record ScoreTile(BlockPos pos, float r, float g, float b, float score, boolean noFit, boolean label) {}

    private static final ArrayList<ScoreTile> scoreTiles = new ArrayList<>();
    private static int scoreRecalcCooldown = 0;

    // Built navmesh chunk keys pushed from the server once a second; drawn as blue chunk outlines.
    private static volatile long[] builtChunks = new long[0];
    public static void setBuiltChunks(long[] keys) { builtChunks = keys; }

    private static final int NAVMESH_RADIUS = 14;
    private static final int NAVMESH_UP = 10;   // cells above the unit to scan (overhangs / upper floors)
    private static final int NAVMESH_DOWN = 18; // cells below the unit to scan (stairs / drops / lower floors)

    private static void recomputeScoreTiles() {
        scoreTiles.clear();
        if (MC.level == null) return;
        LivingEntity sel = null;
        for (LivingEntity e : UnitClientEvents.getSelectedUnits())
            if (e instanceof Unit) { sel = e; break; }
        if (!(sel instanceof Mob mob) || !(sel instanceof Unit unit)) return;

        MobilityClass mobility = MobilityClass.of(unit);
        int fr = RtsPathfinder.footprintRadiusFor(mob);
        int clearance = Math.max(2, Mth.ceil(mob.getBbHeight()));
        BlockPos center = mob.blockPosition();
        int R = NAVMESH_RADIUS;

        ChunkSnapshot view;
        try {
            view = ChunkSnapshot.capture(MC.level, center, center, R + 4, mobility, clearance, fr, 1.0f, false);
        } catch (Throwable t) {
            return;
        }

        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                int x = center.getX() + dx, z = center.getZ() + dz;
                // Show EVERY standable navmesh cell in a vertical band - so stairs, bridges, overhangs and
                // separate floors above/below the unit all show, not just the surface nearest its feet.
                for (int dy = NAVMESH_UP; dy >= -NAVMESH_DOWN; dy--) {
                    int y = center.getY() + dy;
                    float base = mobility.costFor(view.kindAt(x, y, z), 1.0f);
                    if (Float.isInfinite(base)) continue;
                    float r, g, b, score;
                    boolean noFit = false;
                    if (fr > 0 && !GridNeighbors.wideFits(view, x, y, z)) {
                        // walkable for a small unit, but this unit's footprint can't fit here
                        r = 1.0f; g = 0.35f; b = 0.0f; score = 0f; noFit = true;
                    } else {
                        // green (open) -> yellow (one wall 2 cells away) -> red (wall right beside).
                        float malus = GridNeighbors.crowdingMalus(view, x, y, z);
                        score = base + malus; // total per-cell A* score: terrain cost + avoid-box malus
                        float t = Math.min(1f, malus / 0.25f); // 0.125 (far wall) -> yellow, 0.25+ (near wall) -> red
                        r = Math.min(1f, t * 2f);
                        g = Math.min(1f, 2f - t * 2f);
                        b = 0.1f;
                    }
                    boolean label = Math.abs(dy) <= 1; // only number the cells near the unit's own level (readable)
                    scoreTiles.add(new ScoreTile(new BlockPos(x, y - 1, z), r, g, b, score, noFit, label)); // tile = top of floor block
                }
            }
        }
    }

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
        if (displayMode == DebugDisplayMode.NAVMESH) {
            evt.getGuiGraphics().drawString(MC.font, "NAVMESH",            x, y + lineH * 4, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, "green=open",         x, y + lineH * 5, 0x40FF40);
            evt.getGuiGraphics().drawString(MC.font, "red=crowded",        x, y + lineH * 6, 0xFF4040);
            evt.getGuiGraphics().drawString(MC.font, "orange=no fit",      x, y + lineH * 7, 0xFF9020);
            evt.getGuiGraphics().drawString(MC.font, "tiles: " + scoreTiles.size(), x, y + lineH * 8, 0xFFFFFF);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        // NAVMESH overlay: recompute the scored tiles a few times a second (cheap thanks to the chunk cache).
        if (displayMode == DebugDisplayMode.NAVMESH) {
            if (scoreRecalcCooldown <= 0) {
                recomputeScoreTiles();
                scoreRecalcCooldown = 10;
            } else {
                scoreRecalcCooldown--;
            }
        } else if (!scoreTiles.isEmpty()) {
            scoreTiles.clear();
        }

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

    // Draw a tile's numeric score as billboarded text floating just above the cell (orthoview only).
    private static void drawScoreLabel(com.mojang.blaze3d.vertex.PoseStack poseStack, ScoreTile t) {
        net.minecraft.client.Camera camera = MC.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        Vec3 cam = camera.getPosition();
        String text = t.noFit() ? "X" : String.format("%.2f", t.score());

        poseStack.pushPose();
        poseStack.translate(t.pos().getX() + 0.5 - cam.x(),
                t.pos().getY() + 1.15 - cam.y(),
                t.pos().getZ() + 0.5 - cam.z());
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        float w = -MC.font.width(text) / 2f;
        MC.font.drawInBatch(text, w, 0, t.noFit() ? 0xFFFF9020 : 0xFFFFFFFF, false,
                poseStack.last().pose(), MC.renderBuffers().bufferSource(),
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 255);
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (MC.level == null)
            return;

        // NAVMESH overlay: blue outline around every built (cached) navmesh chunk - so you can see what's built.
        if (evt.getStage() == UnitClientEvents.stage && displayMode == DebugDisplayMode.NAVMESH
                && OrthoviewClientEvents.isEnabled() && builtChunks.length > 0 && MC.player != null) {
            double yy = MC.player.getY() + 0.05;
            int pcx = MC.player.chunkPosition().x, pcz = MC.player.chunkPosition().z;
            float br = 0.2f, bg = 0.45f, bb = 1.0f, ba = 0.9f;
            for (long key : builtChunks) {
                int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
                if (Math.abs(cx - pcx) > 16 || Math.abs(cz - pcz) > 16) continue; // cull far chunks
                double x0 = cx << 4, z0 = cz << 4, x1 = x0 + 16, z1 = z0 + 16;
                Vec3 a = new Vec3(x0, yy, z0), b = new Vec3(x1, yy, z0), c = new Vec3(x1, yy, z1), d = new Vec3(x0, yy, z1);
                MyRenderer.drawLine(evt.getPoseStack(), null, a, b, br, bg, bb, ba);
                MyRenderer.drawLine(evt.getPoseStack(), null, b, c, br, bg, bb, ba);
                MyRenderer.drawLine(evt.getPoseStack(), null, c, d, br, bg, bb, ba);
                MyRenderer.drawLine(evt.getPoseStack(), null, d, a, br, bg, bb, ba);
            }
        }

        // NAVMESH overlay: draw a colored top-face on each standable cell around the selected unit.
        if (evt.getStage() == UnitClientEvents.stage && displayMode == DebugDisplayMode.NAVMESH
                && OrthoviewClientEvents.isEnabled() && !scoreTiles.isEmpty()) {
            for (ScoreTile t : scoreTiles)
                MyRenderer.drawBlockFace(evt.getPoseStack(), null, Direction.UP, t.pos(), t.r(), t.g(), t.b(), 0.5f);
            for (ScoreTile t : scoreTiles)
                if (t.label()) drawScoreLabel(evt.getPoseStack(), t);
        }

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
                            // green for vanilla/A*/flow, red for failed-to-reach
                            float lineR, lineG, lineB;
                            if (pd.pathType == RtsPathfinder.TYPE_FAILED) {
                                lineR = 1.0f; lineG = 0.2f; lineB = 0.2f;
                            } else {
                                lineR = PATH_LINE_R; lineG = PATH_LINE_G; lineB = PATH_LINE_B;
                            }
                            BlockPos prev = null;
                            for (BlockPos node : pd.nodes) {
                                if (prev != null) {
                                    Vec3 a0 = new Vec3(prev.getX() + 0.5, prev.getY() + PATH_LINE_Y_OFFSET, prev.getZ() + 0.5);
                                    Vec3 b0 = new Vec3(node.getX() + 0.5, node.getY() + PATH_LINE_Y_OFFSET, node.getZ() + 0.5);
                                    MyRenderer.drawLine(evt.getPoseStack(), vertexConsumerLine, a0, b0,
                                            lineR, lineG, lineB, PATH_LINE_BASE_ALPHA);
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
