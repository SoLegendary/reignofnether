package com.solegendary.reignofnether.fogofwar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerChunksClientEvents {

    private static Map<UUID, Set<ChunkPos>> liveChunks = new HashMap<>();
    private static Map<UUID, Set<ChunkPos>> sentChunks = new HashMap<>();

    private static final Minecraft MC = Minecraft.getInstance();

    // toggle this from a keybind/command if you don't want it rendering all the time
    public static boolean enabled = true;

    private static final float BOX_Y = 0f;      // world y to draw the flat outlines at
    private static final float BOX_HEIGHT = 0.05f;

    public static void applyServerState(Map<UUID, Set<ChunkPos>> live, Map<UUID, Set<ChunkPos>> sent) {
        liveChunks = live;
        sentChunks = sent;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!enabled || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        if (MC.level == null)
            return;

        PoseStack poseStack = event.getPoseStack();


        // sent chunks first (drawn underneath/behind in intent), live chunks on top
        for (UUID uuid : sentChunks.keySet())
            if (uuid.equals(MC.player.getUUID()))
                for (ChunkPos cp :  sentChunks.get(uuid))
                    drawChunkOutline(poseStack, cp, 1.0f, 1.0f, 0.0f, 0.5f); // yellow

        for (UUID uuid : liveChunks.keySet())
            if (uuid.equals(MC.player.getUUID()))
                for (ChunkPos cp :  liveChunks.get(uuid))
                    drawChunkOutline(poseStack, cp, 0.0f, 1.0f, 0.0f, 0.75f); // green
    }

    private static void drawChunkOutline(PoseStack poseStack, ChunkPos cp, float r, float g, float b, float a) {
        AABB aabb = new AABB(
                cp.getMinBlockX(), GameruleClient.groundYLevel + 1.2f, cp.getMinBlockZ(),
                cp.getMaxBlockX() + 1, GameruleClient.groundYLevel + 1.2f, cp.getMaxBlockZ() + 1
        ).inflate(-0.2);
        MyRenderer.drawBoxBottom(poseStack, aabb, r, g, b, a);
    }
}