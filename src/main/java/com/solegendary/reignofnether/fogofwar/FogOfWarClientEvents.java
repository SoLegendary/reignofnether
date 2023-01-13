package com.solegendary.reignofnether.fogofwar;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.keybinds.Keybindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FogOfWarClientEvents {

    // chunks that are in immediate view of a unit or building
    public static final Set<LevelRenderer.RenderChunkInfo> brightChunks = ConcurrentHashMap.newKeySet();

    // chunks that have been in range of a unit or building before
    // if out of immediate view will be rendered with semi brightness and at its past state
    // Boolean is 'shouldBeRendered' so we render it once to update the brightness
    public static final Set<Pair<LevelRenderer.RenderChunkInfo, Integer>> exploredChunks = ConcurrentHashMap.newKeySet();

    // TODO: fix smooth lighting shading issue in QuadLighter.process
    // 1. maybe have a static flag to change ClientLevel.shade brightness (since it doesn't get pos data) before call
    //    to render and revert it immediately after
    // 2. OR just disable smooth lighting entirely at the edges of those chunks?
    // 3. OR find the flag to rerender those edges

    public static float BRIGHT_CHUNK_BRIGHTNESS = 1.0f;
    public static float SEMI_DARK_CHUNK_BRIGHTNESS = 0.15f;
    public static float DARK_CHUNK_BRIGHTNESS = 0f;

    public static Minecraft MC = Minecraft.getInstance();

    public static float getPosBrightness(BlockPos pPos) {
        for (LevelRenderer.RenderChunkInfo chunkInfo : brightChunks)
            if (chunkInfo.chunk.bb.contains(pPos.getX(), pPos.getY(), pPos.getZ()))
                return BRIGHT_CHUNK_BRIGHTNESS;
        for (Pair<LevelRenderer.RenderChunkInfo, Integer> pair : exploredChunks)
            if (pair.getFirst().chunk.bb.contains(pPos.getX(), pPos.getY(), pPos.getZ()))
                return SEMI_DARK_CHUNK_BRIGHTNESS;
        return DARK_CHUNK_BRIGHTNESS;
    }

    public static int chunkManhattanDist(ChunkPos pos1, ChunkPos pos2) {
        return Math.abs(pos1.x - pos2.x) + Math.abs(pos1.z - pos2.z);
    }

    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Pre evt) {
        if (evt.getKeyCode() == Keybindings.getFnum(7).key && MC.level != null && MC.player != null) {
            System.out.println("test1");

            MC.level.getProfiler().push("queueCheckLight");
            MC.level.getChunkSource().getLightEngine().checkBlock(MC.player.getOnPos());
            MC.level.getProfiler().pop();

            MC.level.markAndNotifyBlock(MC.player.getOnPos(), (LevelChunk) MC.level.getChunk(MC.player.getOnPos()),
                    Blocks.AIR.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 3, 512);

        }
        if (evt.getKeyCode() == Keybindings.getFnum(8).key && MC.level != null && MC.player != null) {
            System.out.println("test2");

            MC.level.setBlock(MC.player.getOnPos(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            MC.level.destroyBlock(MC.player.getOnPos(), false, null);
        }
    }

    @SubscribeEvent
    // hudSelectedEntity and portraitRendererUnit should be assigned in the same event to avoid desyncs
    public static void onRenderLivingEntity(RenderLivingEvent.Pre<? extends LivingEntity, ? extends Model> evt) {
        for (LevelRenderer.RenderChunkInfo chunkInfo : brightChunks) {
            BlockPos bp = evt.getEntity().getOnPos();
            if (chunkInfo.chunk.bb.contains(bp.getX() + 0.5f, bp.getY() + 0.5f, bp.getZ() + 0.5f))
                return;
        }
        evt.setCanceled(true);
    }
}
