package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class FogOfWarClientEvents {

    // all chunks that have ever been explored, including currently bright chunks
    public static final Set<FogChunk> fogChunks = ConcurrentHashMap.newKeySet();

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
    public static final int CHUNK_VIEW_DIST = 2;

    public static boolean forceUpdate = true;
    public static int forceUpdateDelayTicks = 0;
    public static int enableDelayTicks = 0;

    public static Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    // can't use ScreenEvent.KeyboardKeyPressedEvent as that only happens when a screen is up
    public static void onInput(InputEvent.Key evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions
            // toggle fog of war without changing explored chunks
            if (evt.getKey() == Keybindings.getFnum(8).key) {
                setEnabled(!enabled);
                forceUpdateDelayTicks = 20;
            }
            // reset fog of war
            if (enabled && evt.getKey() == Keybindings.getFnum(7).key) {
                fogChunks.clear();
                setEnabled(false);
                enableDelayTicks = 20;
                forceUpdateDelayTicks = 40;
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }
    public static void setEnabled(boolean value) {
        enabled = value;
        // reload chunks like player pressed F3 + A
        MC.levelRenderer.allChanged();
    }

    // returns the shade modifier that should be applied at a given position based on the fog of war state there
    public static float getPosBrightness(BlockPos pPos) {
        if (!isEnabled())
            return 1.0f;

        for (FogChunk chunkInfo : fogChunks)
            if (chunkInfo.chunkInfo.chunk.bb.contains(pPos.getX() + 0.5f, pPos.getY() + 0.5f, pPos.getZ() + 0.5f))
                return chunkInfo.brightness;

        return 0.0f;
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

        for (FogChunk fogChunk : fogChunks)
            if (fogChunk.isBrightChunk() && fogChunk.chunkInfo.chunk.bb.contains(bp.getX() + 0.5f, bp.getY() + 0.5f, bp.getZ() + 0.5f))
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

    private static int updateLightingTicks = 0;
    private static final int UPDATE_LIGHTING_TICKS_MAX = 10;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (MC.level == null || evt.phase != TickEvent.Phase.END)
            return;

        if (enableDelayTicks > 0) {
            enableDelayTicks -= 1;
            if (enableDelayTicks == 0)
                setEnabled(true);
        }

        if (!enabled)
            return;

        if (forceUpdateDelayTicks > 0) {
            forceUpdateDelayTicks -= 1;
            if (forceUpdateDelayTicks == 0)
                forceUpdate = true;
        }

        for (FogChunk fogChunk : FogOfWarClientEvents.fogChunks)
            fogChunk.tickBrightness();

        updateLightingTicks -= 1;
        if (updateLightingTicks <= 0) {
            updateLightingTicks = UPDATE_LIGHTING_TICKS_MAX;
            updateChunkLighting();
        }
    }

    // TODO: issue with lighting bugs is that the semi-bright chunks with issues are the ones 2 chunks away, not the
    // closest ones
    // maybe mark chunks adjacent to those leaving with needsLightUpdate = true?
    public static void onChunksChange(Set<FogChunk> newBrightChunks, Set<FogChunk> newExploredChunks) {
        newBrightChunks.addAll(newExploredChunks);

        for (FogChunk fogChunk : newBrightChunks) {
            fogChunk.needsLightUpdate = true;

            for (FogChunk chunk : FogOfWarClientEvents.fogChunks)
                if (chunk.chunkInfo.chunk.bb.getCenter().distanceToSqr(fogChunk.chunkInfo.chunk.bb.getCenter()) < 550)
                    chunk.needsLightUpdate = true;
        }
    }

    public static void updateChunkLighting() {
        if (MC.level == null)
            return;

        Set<ChunkAccess> chunks = ConcurrentHashMap.newKeySet();

        // update bright chunks regardless of if they have needsLightUpdate or not as we want to be 100%
        // sure they're free of glitches as the player is most often looking at them
        for (FogChunk fogChunk : FogOfWarClientEvents.fogChunks)
            if (fogChunk.needsLightUpdate || fogChunk.isBrightChunk())
                chunks.add(MC.level.getChunk(fogChunk.chunkInfo.chunk.getOrigin()));

        for (ChunkAccess chunk : chunks) {
            for (int y = MC.level.getMaxBuildHeight(); y > MC.level.getMinBuildHeight(); y -= 1) {
                BlockPos pos = new BlockPos(chunk.getPos().getMiddleBlockX(), y, chunk.getPos().getMiddleBlockZ());
                BlockState bs = MC.level.getBlockState(pos);
                if (!bs.isAir()) {
                    MC.level.setBlockAndUpdate(pos, Blocks.GLOWSTONE.defaultBlockState());
                    MC.level.setBlockAndUpdate(pos, bs);
                    break;
                }
            }
        }

        for (FogChunk fogChunk : fogChunks)
            if (fogChunk.needsLightUpdate)
                CompletableFuture.delayedExecutor(2000, TimeUnit.MILLISECONDS).execute(() -> {
                    fogChunk.needsLightUpdate = false;
                });
    }
}
