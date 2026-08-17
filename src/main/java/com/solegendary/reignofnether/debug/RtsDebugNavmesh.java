package com.solegendary.reignofnether.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.debug.RtsDebugClientEvents.DebugDisplayMode;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.pathfinding.ChunkSnapshot;
import com.solegendary.reignofnether.unit.pathfinding.GridNeighbors;
import com.solegendary.reignofnether.unit.pathfinding.MobilityClass;
import com.solegendary.reignofnether.unit.pathfinding.RtsPathfinder;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

// NAVMESH overlay: a colored ground tile on every standable cell around the selected unit, tinted by its A*
// score, plus blue outlines around the built (cached) navmesh chunks. Drawn only in NAVMESH mode + orthoview.
public class RtsDebugNavmesh {

    private static final Minecraft MC = Minecraft.getInstance();

    // A colored ground tile: where the navmesh says a cell is standable, tinted by its A* score for the
    // selected unit. Recomputed periodically (not every frame) from a client-side ChunkSnapshot.
    private record ScoreTile(BlockPos pos, float r, float g, float b, float score, boolean noFit, boolean label) {}

    private static final ArrayList<ScoreTile> scoreTiles = new ArrayList<>();
    private static int scoreRecalcCooldown = 0;

    // Built navmesh chunk keys pushed from the server once a second; drawn as blue chunk outlines.
    private static volatile long[] builtChunks = new long[0];
    public static void setBuiltChunks(long[] keys) { builtChunks = keys; }

    public static int scoreTileCount() { return scoreTiles.size(); }

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
                    float base = mobility.costFor(mobility, view.kindAt(x, y, z), 1.0f);
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
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        // Recompute the scored tiles a few times a second (cheap thanks to the chunk cache).
        if (RtsDebugClientEvents.displayMode == DebugDisplayMode.NAVMESH) {
            if (scoreRecalcCooldown <= 0) {
                recomputeScoreTiles();
                scoreRecalcCooldown = 10;
            } else {
                scoreRecalcCooldown--;
            }
        } else if (!scoreTiles.isEmpty()) {
            scoreTiles.clear();
        }
    }

    // Draw a tile's numeric score as billboarded text floating just above the cell (orthoview only).
    private static void drawScoreLabel(PoseStack poseStack, ScoreTile t) {
        Camera camera = MC.getEntityRenderDispatcher().camera;
        if (camera == null) return;
        Vec3 cam = camera.getPosition();
        String text = t.noFit() ? "X" : String.format("%.2f", t.score());

        poseStack.pushPose();
        poseStack.translate(t.pos().getX() + 0.5 - cam.x(),
                t.pos().getY() + 1.15 - cam.y(),
                t.pos().getZ() + 0.5 - cam.z());
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        float w = -MC.font.width(text) / 2f;
        MC.font.drawInBatch(text, w, 0, t.noFit() ? 0xFFFF9020 : 0xFFFFFFFF, false,
                poseStack.last().pose(), MC.renderBuffers().bufferSource(),
                Font.DisplayMode.NORMAL, 0, 255);
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (MC.level == null || evt.getStage() != UnitClientEvents.stage
                || RtsDebugClientEvents.displayMode != DebugDisplayMode.NAVMESH
                || !OrthoviewClientEvents.isEnabled())
            return;

        // Blue outline around every built (cached) navmesh chunk - so you can see what's built.
        if (builtChunks.length > 0 && MC.player != null) {
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

        // Colored top-face on each standable cell around the selected unit.
        if (!scoreTiles.isEmpty()) {
            for (ScoreTile t : scoreTiles)
                MyRenderer.drawBlockFace(evt.getPoseStack(), null, Direction.UP, t.pos(), t.r(), t.g(), t.b(), 0.5f);
            for (ScoreTile t : scoreTiles)
                if (t.label()) drawScoreLabel(evt.getPoseStack(), t);
        }
    }
}
