package com.solegendary.reignofnether.fogofwar;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FogOfWarClientEvents {

    // chunks that have been in range of a unit or building before
    // if out of immediate view will be rendered with semi brightness and at its past state

    public static final Set<LevelRenderer.RenderChunkInfo> brightChunks = ConcurrentHashMap.newKeySet();



    static final Minecraft MC = Minecraft.getInstance();
    static ArrayList<Pair<BlockPos, Direction>> foggedBlocks = new ArrayList<>(); // x/z coords that are in fog of war (to darken on the minimap)

    public static float BRIGHT_CHUNK_BRIGHTNESS = 1.0f;
    public static float SEMI_DARK_CHUNK_BRIGHTNESS = 0.15f;
    public static float DARK_CHUNK_BRIGHTNESS = 0f;

    public static float getPosBrightness(BlockPos pPos) {
        // causes comodofication errors??

        for (LevelRenderer.RenderChunkInfo chunkInfo : brightChunks)
            if (chunkInfo.chunk.bb.contains(pPos.getX(), pPos.getY(), pPos.getZ()))
                return BRIGHT_CHUNK_BRIGHTNESS;
        return DARK_CHUNK_BRIGHTNESS;
    }
}
