package com.solegendary.reignofnether.fogofwar;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FogOfWarClientEvents {

    // chunks that are in immediate view of a unit or building
    public static final Set<ChunkRenderDispatcher.RenderChunk> brightChunks = ConcurrentHashMap.newKeySet();

    // chunks that have been in range of a unit or building before
    // if out of immediate view will be rendered with semi brightness and at its past state
    public static final Set<ChunkRenderDispatcher.RenderChunk> semiDarkChunks = ConcurrentHashMap.newKeySet();

    public static float BRIGHT_CHUNK_BRIGHTNESS = 1.0f;
    public static float SEMI_DARK_CHUNK_BRIGHTNESS = 0.15f;
    public static float DARK_CHUNK_BRIGHTNESS = 0f;

    public static float getPosBrightness(BlockPos pPos) {
        for (ChunkRenderDispatcher.RenderChunk chunk : brightChunks)
            if (chunk.bb.contains(pPos.getX(), pPos.getY(), pPos.getZ()))
                return BRIGHT_CHUNK_BRIGHTNESS;
        for (ChunkRenderDispatcher.RenderChunk chunk : semiDarkChunks)
            if (chunk.bb.contains(pPos.getX(), pPos.getY(), pPos.getZ()))
                return SEMI_DARK_CHUNK_BRIGHTNESS;
        return DARK_CHUNK_BRIGHTNESS;
    }

    @SubscribeEvent
    // hudSelectedEntity and portraitRendererUnit should be assigned in the same event to avoid desyncs
    public static void onRenderLivingEntity(RenderLivingEvent.Pre<? extends LivingEntity, ? extends Model> evt) {
        for (ChunkRenderDispatcher.RenderChunk chunk : brightChunks) {
            BlockPos bp = evt.getEntity().getOnPos();
            if (chunk.bb.contains(bp.getX() + 0.5f, bp.getY() + 0.5f, bp.getZ() + 0.5f))
                return;
        }
        evt.setCanceled(true);
    }
}
