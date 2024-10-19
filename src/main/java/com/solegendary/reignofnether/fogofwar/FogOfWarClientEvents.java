package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.sounds.SoundClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.solegendary.reignofnether.fogofwar.FogOfWarServerboundPacket.setServerFog;

public class FogOfWarClientEvents {
    public static final float BRIGHT = 1.0f;
    public static final float DARK = 0.35f;
    public static final float EXTRA_DARK = 0.10f;

    public static final int CHUNK_VIEW_DIST = 1;
    public static final int CHUNK_FAR_VIEW_DIST = 2;
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int UPDATE_TICKS_MAX = 10;
    private static int updateTicksLeft = UPDATE_TICKS_MAX;

    public static final Set<ChunkPos> brightChunks = new HashSet<>();
    public static final Set<ChunkPos> lastBrightChunks = ConcurrentHashMap.newKeySet();
    public static final Set<ChunkPos> rerenderChunks = ConcurrentHashMap.newKeySet();
    public static final Set<BlockPos> semiFrozenChunks = ConcurrentHashMap.newKeySet();
    public static final Set<FrozenChunk> frozenChunks = ConcurrentHashMap.newKeySet();

    public static boolean fogEnableWarningSent = false;
    private static boolean enabled = false;
    private static final Set<String> revealedPlayerNames = ConcurrentHashMap.newKeySet();
    public static boolean movedToCapitol = false;
    public static Set<ChunkPos> chunksToRefresh = new HashSet<>();
    public static ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum = new ObjectArrayList<>();

    public static void revealOrHidePlayer(boolean reveal, String playerName) {
        if (reveal) {
            revealedPlayerNames.add(playerName);
        } else {
            revealedPlayerNames.remove(playerName); // Direct remove instead of removeIf to avoid unnecessary iteration
        }
    }

    public static boolean isPlayerRevealed(String name) {
        return !PlayerClientEvents.isRTSPlayer || revealedPlayerNames.contains(name);
    }

    @SubscribeEvent
    public static void onInput(InputEvent.Key evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS && MC.player != null && MC.player.hasPermissions(4)) {
            if (evt.getKey() == Keybindings.getFnum(8).key && isEnabled()) {
                resetFogChunks();
            }
        }
    }

    public static void resetFogChunks() {
        MC.levelRenderer.allChanged();
        semiFrozenChunks.clear();
    }
    public static void unmuteChunks() {
        SoundClientEvents.mutedBps.clear();
    }
    public static void setEnabled(boolean value) {
        if (MC.player == null) return;
        if (enabled == value) return; // Avoid resetting if the state doesn't change

        enabled = value;
        resetFogChunks();

        if (enabled) {
            updateFogChunks();
            for (Building building : BuildingClientEvents.getBuildings()) {
                building.freezeChunks(MC.player.getName().getString(), false);
            }
        } else {
            for (FrozenChunk frozenChunk : frozenChunks) {
                frozenChunk.unloadBlocks();
            }
            frozenChunks.clear();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterClientCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("rts-fog").then(Commands.literal("enable")
                .executes(command -> handleFogCommand(true))));
        evt.getDispatcher().register(Commands.literal("rts-fog").then(Commands.literal("disable")
                .executes(command -> handleFogCommand(false))));
    }

    private static int handleFogCommand(boolean enable) {
        if (MC.player == null || !MC.player.hasPermissions(4)) return -1;
        if (!enable && !fogEnableWarningSent) {
            // Warning logic
            return 1;
        } else {
            setServerFog(enable);
            return 1;
        }
    }

    public static float getPosBrightness(BlockPos pPos) {
        if (MC.level == null) return BRIGHT;
        if (!MC.level.getWorldBorder().isWithinBounds(pPos)) return EXTRA_DARK;

        ChunkPos chunkPos = new ChunkPos(pPos);
        if (brightChunks.contains(chunkPos)) return BRIGHT;
        return isEnabled() ? DARK : BRIGHT;
    }

    public static boolean isBuildingInBrightChunk(Building building) {
        return !isEnabled() || BuildingUtils.getUniqueChunkBps(building).stream().anyMatch(FogOfWarClientEvents::isInBrightChunk);
    }

    public static boolean isInBrightChunk(BlockPos bp) {
        if (!isEnabled() || MC.level == null) return true;
        return brightChunks.contains(new ChunkPos(bp));
    }

    public static boolean isInBrightChunk(Entity entity) {
        if (!isEnabled() || MC.level == null) return true;
        ChunkPos chunkPos = new ChunkPos(entity.getOnPos());
        return brightChunks.contains(chunkPos) || (entity instanceof RangedAttackerUnit && ((RangedAttackerUnit) entity).getFogRevealDuration() > 0);
    }

    @SubscribeEvent
    public static void onRenderLivingEntity(RenderLivingEvent.Pre<? extends LivingEntity, ? extends Model> evt) {
        if (MC.level == null || !MC.level.getWorldBorder().isWithinBounds(evt.getEntity().getOnPos())) {
            evt.setCanceled(true);
        } else if (!isInBrightChunk(evt.getEntity())) {
            evt.setCanceled(true);
        }
    }

    public static Set<ChunkPos> getEnemyOccupiedChunks() {
        Set<ChunkPos> enemyOccupiedChunks = ConcurrentHashMap.newKeySet();

        UnitClientEvents.getAllUnits().stream()
                .filter(entity -> UnitClientEvents.getPlayerToEntityRelationship(entity) != Relationship.OWNED)
                .forEach(entity -> enemyOccupiedChunks.add(new ChunkPos(entity.getOnPos())));

        BuildingClientEvents.getBuildings().stream()
                .filter(building -> BuildingClientEvents.getPlayerToBuildingRelationship(building) != Relationship.OWNED && !isPlayerRevealed(building.ownerName))
                .forEach(building -> building.getRenderChunkOrigins(true)
                        .stream().map(bp -> MC.level.getChunk(bp).getPos()).forEach(enemyOccupiedChunks::add));

        return enemyOccupiedChunks;
    }

    public static void updateFogChunks() {
        brightChunks.clear();

        Set<ChunkPos> viewerChunks = ConcurrentHashMap.newKeySet();
        Set<ChunkPos> farViewerChunks = ConcurrentHashMap.newKeySet();

        // Process units and buildings in parallel to gather the chunks
        CompletableFuture<Void> unitProcessing = CompletableFuture.runAsync(() -> {
            UnitClientEvents.getAllUnits().stream()
                    .parallel()
                    .filter(entity -> UnitClientEvents.getPlayerToEntityRelationship(entity) == Relationship.OWNED ||
                            (entity instanceof Unit unit && isPlayerRevealed(unit.getOwnerName())))
                    .forEach(entity -> {
                        ChunkPos chunkPos = new ChunkPos(entity.getOnPos());
                        if (entity instanceof GhastUnit) {
                            farViewerChunks.add(chunkPos);
                        } else {
                            viewerChunks.add(chunkPos);
                        }
                    });
        });

        CompletableFuture<Void> buildingProcessing = CompletableFuture.runAsync(() -> {
            BuildingClientEvents.getBuildings().stream()
                    .parallel()
                    .filter(building -> BuildingClientEvents.getPlayerToBuildingRelationship(building) == Relationship.OWNED || isPlayerRevealed(building.ownerName))
                    .forEach(building -> {
                        ChunkPos chunkPos = new ChunkPos(building.centrePos);
                        if ((building instanceof GarrisonableBuilding && GarrisonableBuilding.getNumOccupants(building) > 0 && building.isBuilt) || building.isCapitol) {
                            farViewerChunks.add(chunkPos);
                        } else {
                            viewerChunks.add(chunkPos);
                        }
                    });
        });

        // Wait for both parallel tasks to complete before proceeding
        CompletableFuture<Void> allProcessing = CompletableFuture.allOf(unitProcessing, buildingProcessing);

        allProcessing.thenRun(() -> {
            // Now expand chunks for both viewer and farViewerChunks after gathering them
            expandChunks(viewerChunks, CHUNK_VIEW_DIST);
            expandChunks(farViewerChunks, CHUNK_FAR_VIEW_DIST);

            handleNewlyDarkChunks();
            handleNewlyBrightChunks();
            updateSemiFrozenChunks();
        }).join();  // Ensure that we wait for all processing to finish before proceeding
    }
    public static void revealRangedUnit(String playerBeingAttacked, int unitId) {
        if (MC.player != null && MC.player.getName().getString().equals(playerBeingAttacked)) {
            for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                if (entity.getId() == unitId && entity instanceof RangedAttackerUnit) {
                    ((RangedAttackerUnit) entity).setFogRevealDuration(RangedAttackerUnit.FOG_REVEAL_TICKS_MAX);
                }
            }
        }
    }

    private static void expandChunks(Set<ChunkPos> chunks, int distance) {
        Set<ChunkPos> expandedChunks = new HashSet<>();
        chunks.forEach(chunkPos -> {
            for (int x = -distance; x <= distance; x++) {
                for (int z = -distance; z <= distance; z++) {
                    expandedChunks.add(new ChunkPos(chunkPos.x + x, chunkPos.z + z));
                }
            }
        });
        synchronized (brightChunks) {
            brightChunks.addAll(expandedChunks);
        }
    }



    private static void handleNewlyDarkChunks() {
        Set<ChunkPos> newlyDarkChunks = ConcurrentHashMap.newKeySet();
        newlyDarkChunks.addAll(lastBrightChunks);
        newlyDarkChunks.removeAll(brightChunks);

        newlyDarkChunks.forEach(cpos -> onChunkUnexplore(cpos));

        // Add chunks for rerender in a batch.
        Set<ChunkPos> rerenderBatch = new HashSet<>();
        newlyDarkChunks.forEach(cpos -> {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    rerenderBatch.add(new ChunkPos(cpos.x + x, cpos.z + z));
                }
            }
        });
        rerenderChunks.addAll(rerenderBatch); // Batch add rerender chunks
    }


    private static void handleNewlyBrightChunks() {
        Set<ChunkPos> newlyBrightChunks = ConcurrentHashMap.newKeySet();
        newlyBrightChunks.addAll(brightChunks);
        newlyBrightChunks.removeAll(lastBrightChunks);

        newlyBrightChunks.forEach(FogOfWarClientEvents::onChunkExplore);
    }

    private static void updateSemiFrozenChunks() {
        if (OrthoviewClientEvents.isEnabled()) {
            semiFrozenChunks.removeIf(bp -> bp.offset(8, 8, 8).distSqr(MC.player.getOnPos()) > Math.pow(OrthoviewClientEvents.getZoom() * 3, 2));
        } else {
            semiFrozenChunks.removeIf(bp -> bp.offset(8, 8, 8).distSqr(MC.player.getOnPos()) > Math.pow(MC.levelRenderer.getLastViewDistance() * 8, 2));
        }

        semiFrozenChunks.removeIf(bp -> {
            if (isInBrightChunk(bp)) {
                updateChunkLighting(bp);
                return true;
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (isEnabled() && MC.level != null && MC.player != null && evt.phase == TickEvent.Phase.END) {
            if (updateTicksLeft > 0) {
                updateTicksLeft--;
            } else {
                updateTicksLeft = UPDATE_TICKS_MAX;
                CompletableFuture.runAsync(FogOfWarClientEvents::updateFogChunks);
            }
        }
    }


    public static void updateChunkLighting(BlockPos originBp) {
        if (MC.level == null) return;

        for (int i = 0; i < 4; i++) {
            BlockPos updatePos = originBp.offset(4 * i, 0, 4 * i);
            boolean foundBlock = false;  // Stop iterating once we find a non-air block
            for (int y = MC.level.getMaxBuildHeight(); y > MC.level.getMinBuildHeight() && !foundBlock; y--) {
                BlockPos bp = new BlockPos(updatePos.getX(), y, updatePos.getZ());
                BlockState bs = MC.level.getBlockState(bp);
                if (!bs.isAir()) {
                    MC.level.setBlockAndUpdate(bp, Blocks.GLOWSTONE.defaultBlockState());
                    MC.level.setBlockAndUpdate(bp, bs);
                    foundBlock = true;  // Exit the loop early
                }
            }
        }
    }


    public static void onChunkExplore(ChunkPos cpos) {
        if (MC.level == null) return;

        Set<ChunkPos> chunksInFrustum = renderChunksInFrustum.stream()
                .map(rci -> MC.level.getChunk(rci.chunk.getOrigin()).getPos())
                .collect(Collectors.toSet());

        if (!chunksInFrustum.contains(cpos)) {
            chunksToRefresh.add(cpos);
        }

        frozenChunks.removeIf(fc -> fc.removeOnExplore && MC.level.getChunk(fc.origin).getPos().equals(cpos));

        for (FrozenChunk frozenChunk : frozenChunks) {
            if (MC.level.getChunk(frozenChunk.origin).getPos().equals(cpos)) {
                frozenChunk.unloadBlocks();
            }
        }

        BuildingClientEvents.getBuildings().stream()
                .filter(building -> !building.isExploredClientside)
                .forEach(building -> building.getRenderChunkOrigins(false).stream()
                        .filter(bp -> bp.getX() == cpos.getWorldPosition().getX() && bp.getZ() == cpos.getWorldPosition().getZ())
                        .forEach(bp -> building.isExploredClientside = true));
    }

    public static void onChunkUnexplore(ChunkPos cpos) {
        frozenChunks.removeIf(fc -> fc.removeOnExplore && MC.level.getChunk(fc.origin).getPos().equals(cpos));
        frozenChunks.stream()
                .filter(frozenChunk -> MC.level.getChunk(frozenChunk.origin).getPos().equals(cpos) && MC.level.isLoaded(frozenChunk.origin))
                .forEach(FrozenChunk::saveBlocks);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load evt) {
        BlockPos bp = evt.getChunk().getPos().getWorldPosition();
        frozenChunks.stream()
                .filter(fc -> evt.getLevel().isClientSide() && bp.getX() == fc.origin.getX() && bp.getZ() == fc.origin.getZ())
                .forEach(fc -> {
                    if (fc.unsaved) fc.saveFakeBlocks();
                    if (!isInBrightChunk(bp)) fc.loadBlocks();
                });
    }

    public static void setBuildingDestroyedServerside(BlockPos buildingOrigin) {
        BuildingClientEvents.getBuildings().stream()
                .filter(building -> building.originPos.equals(buildingOrigin))
                .forEach(building -> building.isDestroyedServerside = true);
    }

    public static void setBuildingBuiltServerside(BlockPos buildingOrigin) {
        BuildingClientEvents.getBuildings().stream()
                .filter(building -> building.originPos.equals(buildingOrigin))
                .forEach(building -> building.isBuiltServerside = true);
    }
}
