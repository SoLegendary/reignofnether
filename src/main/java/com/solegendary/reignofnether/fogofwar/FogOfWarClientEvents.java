package com.solegendary.reignofnether.fogofwar;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.keybinds.Keybindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FogOfWarClientEvents {

    // chunks that are in immediate view of a unit or building
    public static final Set<LevelRenderer.RenderChunkInfo> brightChunks = ConcurrentHashMap.newKeySet();

    // chunks that have been in range of a unit or building before
    // if out of immediate view will be rendered with semi brightness and at its past state
    // Boolean is 'shouldBeRendered' so we render it once to update the brightness
    // is a superset of brightChunks
    public static final Set<Pair<LevelRenderer.RenderChunkInfo, Boolean>> exploredChunks = ConcurrentHashMap.newKeySet();

    // if false, disables ALL mixins related to fog of war
    private static boolean enabled = true;

    // TODO: fix smooth lighting shading issue in QuadLighter.process
    // 1. maybe have a static flag to change ClientLevel.shade brightness (since it doesn't get pos data) before call
    //    to render and revert it immediately after
    // 2. OR just disable smooth lighting entirely at the edges of those chunks?
    // 3. OR find the flag to rerender those edges

    public static float BRIGHT_CHUNK_BRIGHTNESS = 1.0f;
    public static float SEMI_DARK_CHUNK_BRIGHTNESS = 0.15f;
    public static float DARK_CHUNK_BRIGHTNESS = 0f;

    public static final int CHUNK_VIEW_DIST_UNIT = 2;
    public static final int CHUNK_VIEW_DIST_BUILDING = 2;

    public static Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    // can't use ScreenEvent.KeyboardKeyPressedEvent as that only happens when a screen is up
    public static void onInput(InputEvent.Key evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions
            if (evt.getKey() == Keybindings.getFnum(8).key)
                setEnabled(!enabled);
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }
    public static void setEnabled(boolean value) {
        enabled = value;
        if (enabled) {
            brightChunks.clear();
            exploredChunks.clear();
        }
        // reload chunks like player pressed F3 + A
        MC.levelRenderer.allChanged();
    }

    public static float getPosBrightness(BlockPos pPos) {
        if (!isEnabled())
            return BRIGHT_CHUNK_BRIGHTNESS;

        if (isInBrightChunk(pPos))
            return BRIGHT_CHUNK_BRIGHTNESS;

        if (isInExploredChunk(pPos))
            return SEMI_DARK_CHUNK_BRIGHTNESS;

        return DARK_CHUNK_BRIGHTNESS;
    }

    public static int chunkManhattanDist(ChunkPos pos1, ChunkPos pos2) {
        return Math.abs(pos1.x - pos2.x) + Math.abs(pos1.z - pos2.z);
    }

    public static boolean isBuildingInBrightChunk(Building building) {
        if (!enabled)
            return true;

        for (BlockPos bp : BuildingUtils.getUniqueChunkBps(building))
            if (isInBrightChunk(bp))
                return true;

        return false;
    }

    public static boolean isInBrightChunk(BlockPos bp) {
        if (!enabled)
            return true;

        for (LevelRenderer.RenderChunkInfo chunkInfo : brightChunks)
            if (chunkInfo.chunk.bb.contains(bp.getX() + 0.5f, bp.getY() + 0.5f, bp.getZ() + 0.5f))
                return true;
        return false;
    }

    public static boolean isInExploredChunk(BlockPos bp) {
        for (Pair<LevelRenderer.RenderChunkInfo, Boolean> pair : exploredChunks)
            if (pair.getFirst().chunk.bb.contains(bp.getX() + 0.5f, bp.getY() + 0.5f, bp.getZ() + 0.5f))
                return true;
        return false;
    }

    @SubscribeEvent
    // hudSelectedEntity and portraitRendererUnit should be assigned in the same event to avoid desyncs
    public static void onRenderLivingEntity(RenderLivingEvent.Pre<? extends LivingEntity, ? extends Model> evt) {
        if (!isEnabled())
            return;

        // don't render entities in non-bright chunks
        if (isInBrightChunk(evt.getEntity().getOnPos()))
            return;

        evt.setCanceled(true);
    }
}
