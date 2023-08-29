package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.solegendary.reignofnether.fogofwar.FogOfWarServerboundPacket.setServerFog;

public class FogOfWarClientEvents {

    public static final float BRIGHT = 1.0f;
    public static final float DARK = 0.35f;
    public static final int CHUNK_VIEW_DIST = 1;
    public static final int CHUNK_FAR_VIEW_DIST = 2;
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int UPDATE_TICKS_MAX = 10;
    private static int updateTicksLeft = UPDATE_TICKS_MAX;

    public static final Set<ChunkPos> brightChunks = ConcurrentHashMap.newKeySet();
    public static final Set<ChunkPos> lastBrightChunks = ConcurrentHashMap.newKeySet();
    public static final Set<ChunkPos> rerenderChunks = ConcurrentHashMap.newKeySet();

    // all chunk origins that have ever been explored, including currently bright chunks
    public static final Set<BlockPos> frozenChunks = ConcurrentHashMap.newKeySet();

    // if false, disables ALL mixins related to fog of war
    private static boolean enabled = false;

    @SubscribeEvent
    // can't use ScreenEvent.KeyboardKeyPressedEvent as that only happens when a screen is up
    public static void onInput(InputEvent.Key evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions
            if (MC.player == null)
                return;
            if (!MC.player.hasPermissions(4))
                return;

            // resetFogChunks
            if (evt.getKey() == Keybindings.getFnum(8).key)
                resetFogChunks();
        }
    }

    // reload chunks like player pressed F3 + A
    public static void resetFogChunks() {
        MC.levelRenderer.allChanged();
        frozenChunks.clear();
    }

    public static void setEnabled(boolean value) {
        if (MC.player == null)
            return;

        if (enabled != value) {
            enabled = value;
            resetFogChunks();
        }
    }

    public static boolean isEnabled() {
        if (ResearchClient.hasCheat("iseedeadpeople"))
            return false;
        return enabled;
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterClientCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("fog").then(Commands.literal("enable")
                .executes((command) -> {
                    if (MC.player == null)
                        return -1;
                    if (!MC.player.hasPermissions(4))
                        return -1;
                    setServerFog(true);
                    return 1;
                })));
        evt.getDispatcher().register(Commands.literal("fog").then(Commands.literal("disable")
                .executes((command) -> {
                    if (MC.player == null)
                        return -1;
                    if (!MC.player.hasPermissions(4))
                        return -1;
                    setServerFog(false);
                    return 1;
                })));
    }

    // returns the shade modifier that should be applied at a given position based on the fog of war state there
    public static float getPosBrightness(BlockPos pPos) {
        if (!isEnabled() || MC.level == null)
            return BRIGHT;

        // first check if the ChunkPos is already occupied as this is faster
        for (ChunkPos chunkPos : brightChunks)
            if (new ChunkPos(pPos).equals(chunkPos))
                return BRIGHT;

        return DARK;
    }

    public static boolean isBuildingInBrightChunk(Building building) {
        if (!isEnabled())
            return true;

        for (BlockPos bp : BuildingUtils.getUniqueChunkBps(building))
            if (isInBrightChunk(bp))
                return true;

        return false;
    }

    public static boolean isInBrightChunk(BlockPos bp) {
        if (!isEnabled() || MC.level == null)
            return true;

        // first check if the ChunkPos is already occupied as this is faster
        for (ChunkPos chunkPos : brightChunks)
            if (new ChunkPos(bp).equals(chunkPos))
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
        if (!isEnabled() || MC.level == null || MC.player == null || evt.phase != TickEvent.Phase.END)
            return;

        if (updateTicksLeft > 0) {
            updateTicksLeft -= 1;
        } else {
            updateTicksLeft = UPDATE_TICKS_MAX;
            brightChunks.clear();
            Set<ChunkPos> occupiedChunks = ConcurrentHashMap.newKeySet();
            Set<ChunkPos> occupiedFarviewChunks = ConcurrentHashMap.newKeySet();

            // get chunks that have units/buildings that can see
            for (LivingEntity entity : UnitClientEvents.getAllUnits())
                if (UnitClientEvents.getPlayerToEntityRelationship(entity) == Relationship.OWNED)
                    occupiedChunks.add(new ChunkPos(entity.getOnPos()));

            for (Building building : BuildingClientEvents.getBuildings()) {
                if (BuildingClientEvents.getPlayerToBuildingRelationship(building) == Relationship.OWNED) {
                    if (building instanceof GarrisonableBuilding && building.isBuilt)
                        occupiedFarviewChunks.add(new ChunkPos(building.centrePos));
                    else
                        occupiedChunks.add(new ChunkPos(building.centrePos));
                }
            }

            for (ChunkPos chunkPos : occupiedChunks)
                for (int x = -CHUNK_VIEW_DIST; x <= CHUNK_VIEW_DIST; x++)
                    for (int z = -CHUNK_VIEW_DIST; z <= CHUNK_VIEW_DIST; z++)
                        brightChunks.add(new ChunkPos(chunkPos.x + x, chunkPos.z + z));

            for (ChunkPos chunkPos : occupiedFarviewChunks)
                for (int x = -CHUNK_FAR_VIEW_DIST; x <= CHUNK_FAR_VIEW_DIST; x++)
                    for (int z = -CHUNK_FAR_VIEW_DIST; z <= CHUNK_FAR_VIEW_DIST; z++)
                        brightChunks.add(new ChunkPos(chunkPos.x + x, chunkPos.z + z));

            Set<ChunkPos> newlyDarkChunks = ConcurrentHashMap.newKeySet();
            newlyDarkChunks.addAll(lastBrightChunks);
            newlyDarkChunks.removeAll(brightChunks);

            for (ChunkPos cpos : newlyDarkChunks) {
                for (int x = -1; x <= 1; x++)
                    for (int z = -1; z <= 1; z++)
                        rerenderChunks.add(new ChunkPos(cpos.x + x, cpos.z + z));
            }
            frozenChunks.removeIf(bp -> {
                if (isInBrightChunk(bp)) {
                    updateChunkLighting(bp);
                    return true;
                }
                return false;
            });

            lastBrightChunks.clear();
            lastBrightChunks.addAll(brightChunks);
        }
    }

    public static void updateChunkLighting(BlockPos originBp) {
        if (MC.level == null)
            return;

        for (int i = 0; i < 4; i++) {
            BlockPos updatePos = originBp.offset(4*i, 0, 4*i);
            for (int y = MC.level.getMaxBuildHeight(); y > MC.level.getMinBuildHeight(); y -= 1) {
                BlockPos bp = new BlockPos(updatePos.getX(), y, updatePos.getZ());
                BlockState bs = MC.level.getBlockState(bp);
                if (!bs.isAir()) {
                    MC.level.setBlockAndUpdate(bp, Blocks.GLOWSTONE.defaultBlockState());
                    MC.level.setBlockAndUpdate(bp, bs);
                    break;
                }
            }
        }
    }
}
