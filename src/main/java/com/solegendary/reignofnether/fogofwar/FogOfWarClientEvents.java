package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class FogOfWarClientEvents {

    public static final float BRIGHT = 1.0f;
    public static final float SEMI = 0.15f;

    // ChunkPoses that have at least one owned unit/building in them - can be used to determine current bright chunks
    public static final Set<ChunkPos> occupiedChunks = ConcurrentHashMap.newKeySet();
    // TODO: remake lastOccupiedChunks and make sure LevelRenderer rerenders chunks that fell out of the bright zone

    private static ChunkPos lastPlayerChunkPos = new ChunkPos(0,0);

    // all chunk origins that have ever been explored, including currently bright chunks
    public static final Set<BlockPos> frozenChunks = ConcurrentHashMap.newKeySet();

    // if false, disables ALL mixins related to fog of war
    private static boolean enabled = true;

    public static final int CHUNK_VIEW_DIST = 2;

    public static boolean forceUpdate = true;
    private static int forceUpdateDelayTicks = 0;
    public static int enableDelayTicks = 0;

    private static final Minecraft MC = Minecraft.getInstance();

    public static void loadExploredChunks(String playerName, int[] xPos, int[] zPos) {

    }

    @SubscribeEvent
    // can't use ScreenEvent.KeyboardKeyPressedEvent as that only happens when a screen is up
    public static void onInput(InputEvent.Key evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions
            // toggle fog of war without changing explored chunks
            if (evt.getKey() == Keybindings.getFnum(8).key) {
                setEnabled(!enabled);
            }
            // reset fog of war
            if (enabled && evt.getKey() == Keybindings.getFnum(7).key) {
                frozenChunks.clear();
                setEnabled(false);
                enableDelayTicks = 20;
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
        if (!isEnabled() || MC.level == null)
            return BRIGHT;

        // first check if the ChunkPos is already occupied as this is faster
        for (ChunkPos chunkPos : occupiedChunks)
            if (MC.level.getChunk(pPos).getPos().getChessboardDistance(chunkPos) < CHUNK_VIEW_DIST)
                return BRIGHT;

        return SEMI;
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
        if (!enabled || MC.level == null)
            return true;

        // first check if the ChunkPos is already occupied as this is faster
        for (ChunkPos chunkPos : occupiedChunks)
            if (MC.level.getChunk(bp).getPos().getChessboardDistance(chunkPos) < CHUNK_VIEW_DIST)
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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (MC.level == null || MC.player == null || evt.phase != TickEvent.Phase.END)
            return;

        ChunkPos pos = MC.level.getChunk(MC.player.getOnPos()).getPos();
        if (!pos.equals(lastPlayerChunkPos))
            forceUpdate = true;
        lastPlayerChunkPos = pos;

        if (enableDelayTicks > 0) {
            enableDelayTicks -= 1;
            if (enableDelayTicks == 0)
                setEnabled(true);
        }

        if (!enabled)
            return;

        if (forceUpdateDelayTicks > 0) {
            forceUpdateDelayTicks -= 1;
            if (forceUpdateDelayTicks == 0) {
                forceUpdate = true;
            }
        }

        if (forceUpdate) {
            forceUpdate = false;
            occupiedChunks.clear();

            // get chunks that have units/buildings that can see
            for (LivingEntity entity : UnitClientEvents.getAllUnits())
                if (UnitClientEvents.getPlayerToEntityRelationship(entity) == Relationship.OWNED)
                    occupiedChunks.add(MC.level.getChunk(entity.getOnPos()).getPos());

            for (Building building : BuildingClientEvents.getBuildings())
                if (BuildingClientEvents.getPlayerToBuildingRelationship(building) == Relationship.OWNED)
                    occupiedChunks.add(MC.level.getChunk(building.centrePos).getPos());

            frozenChunks.removeIf(bp -> {
                if (isInBrightChunk(bp)) {
                    updateChunkLighting(bp);
                    return true;
                }
                return false;
            });
            forceUpdateDelayTicks = 10;
        }
    }

    public static void updateChunkLighting(BlockPos bp) {
        if (MC.level == null)
            return;

        for (int y = MC.level.getMaxBuildHeight(); y > MC.level.getMinBuildHeight(); y -= 1) {
            BlockState bs = MC.level.getBlockState(bp);
            if (!bs.isAir()) {
                MC.level.setBlockAndUpdate(bp, Blocks.GLOWSTONE.defaultBlockState());
                MC.level.setBlockAndUpdate(bp, bs);
                break;
            }
        }
    }
}
