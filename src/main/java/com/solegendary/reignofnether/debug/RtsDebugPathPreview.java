package com.solegendary.reignofnether.debug;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.debug.RtsDebugClientEvents.DebugDisplayMode;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.pathfinding.RtsPathfinder;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;

// Server-pushed move-command path preview: the line a selected unit will follow, drawn in orthoview while in
// PATHFINDING mode. Entries persist until a new path arrives or the unit reaches the last node.
public class RtsDebugPathPreview {

    private static final Minecraft MC = Minecraft.getInstance();

    // Path-line render constants.
    public static final float PATH_LINE_R = 0.2f;
    public static final float PATH_LINE_G = 1.0f;
    public static final float PATH_LINE_B = 0.4f;
    public static final float PATH_LINE_BASE_ALPHA = 0.9f;
    public static final float PATH_LINE_Y_OFFSET = 0.1f;
    public static final int PATH_DISPLAY_TICKS = 40; // ~2s at 20 tps

    // Path-preview state, keyed by entityId. Decremented in onClientTick; entries auto-purge at zero.
    public static class PathDisplay {
        public final List<BlockPos> nodes;
        public final byte pathType;
        public int ticksRemaining;
        public PathDisplay(List<BlockPos> nodes, byte pathType, int ticksRemaining) {
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

    public static void removeUnitPath(int entityId) {
        displayedPaths.remove(entityId);
    }

    public static int displayedPathCount() { return displayedPaths.size(); }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        // While in PATHFINDING mode entries don't expire on a timer - they persist until a new path arrives or
        // the unit reaches the last node. Outside it they tick down and expire after PATH_DISPLAY_TICKS.
        if (!displayedPaths.isEmpty()) {
            displayedPaths.entrySet().removeIf(e -> {
                PathDisplay pd = e.getValue();
                if (RtsDebugClientEvents.displayMode != DebugDisplayMode.PATHFINDING) {
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
        if (MC.level == null || MC.player == null || evt.getStage() != UnitClientEvents.stage
                || RtsDebugClientEvents.displayMode != DebugDisplayMode.PATHFINDING
                || !OrthoviewClientEvents.isEnabled())
            return;

        VertexConsumer vertexConsumerLine = MC.renderBuffers().bufferSource().getBuffer(RenderType.LINE_STRIP);
        for (LivingEntity entity : UnitClientEvents.getSelectedUnits()) {
            if (!(entity instanceof Unit unit))
                continue;
            PathDisplay pd = displayedPaths.get(entity.getId());
            if (pd == null || pd.nodes.size() < 2
                    || !(unit.getOwnerName().equals(MC.player.getName().getString())
                    || AlliancesClient.canControlAlly(unit.getOwnerName())))
                continue;

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
