package com.solegendary.reignofnether.mixin.fogofwar;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.fogofwar.FogChunk;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.fogofwar.FogTransitionBrightness;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents.CHUNK_VIEW_DIST_BUILDING;
import static com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents.CHUNK_VIEW_DIST_UNIT;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Final @Shadow private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;
    @Final @Shadow private AtomicReference<LevelRenderer.RenderChunkStorage> renderChunkStorage = new AtomicReference<>();
    @Final @Shadow private Minecraft minecraft;

    // any chunkInfo objects added to renderChunksInFrustum will be rendered
    // we can collect old chunk data here to render them in their past state
    @Inject(
        method = "applyFrustum(Lnet/minecraft/client/renderer/culling/Frustum;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void applyFrustum(Frustum pFrustum, CallbackInfo ci) {
        if (this.minecraft.level == null)
            return;

        ci.cancel();

        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
        } else {
            this.minecraft.getProfiler().push("apply_frustum");

            LinkedHashSet<LevelRenderer.RenderChunkInfo> renderChunkInfos = (this.renderChunkStorage.get()).renderChunks;

            // bright chunks in last tick
            Set<FogChunk> oldBrightChunks = FogOfWarClientEvents.fogChunks.stream()
                    .filter(FogChunk::isBrightChunk)
                    .collect(Collectors.toSet());
            // bright chunks in current tick
            Set<FogChunk> newBrightChunks = ConcurrentHashMap.newKeySet();

            // refresh renderChunksInFrustum
            this.renderChunksInFrustum.clear();
            for (LevelRenderer.RenderChunkInfo chunkInfo : renderChunkInfos)
                if (pFrustum.isVisible(chunkInfo.chunk.getBoundingBox()))
                    this.renderChunksInFrustum.add(chunkInfo);

            for (LevelRenderer.RenderChunkInfo chunkInfo : renderChunkInfos) {
                    ChunkPos chunkPos1 = this.minecraft.level.getChunk(chunkInfo.chunk.getOrigin()).getPos();

                    // chunks in view of owned units
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (UnitClientEvents.getPlayerToEntityRelationship(entity) != Relationship.OWNED)
                            continue;

                        ChunkPos chunkPos2 = this.minecraft.level.getChunk(entity.getOnPos()).getPos();

                        if (chunkPos1.getChessboardDistance(chunkPos2) < CHUNK_VIEW_DIST_UNIT) {
                            newBrightChunks.add(new FogChunk(chunkInfo, FogTransitionBrightness.DARK_TO_BRIGHT));
                            break;
                        }
                    }
                    // chunks in view of owned buildings
                    for (Building building : BuildingClientEvents.getBuildings()) {
                        if (BuildingClientEvents.getPlayerToBuildingRelationship(building) != Relationship.OWNED)
                            continue;

                        BlockPos bp = building.centrePos;
                        ChunkPos chunkPos2 = this.minecraft.level.getChunk(bp).getPos();

                        if (chunkPos1.getChessboardDistance(chunkPos2) < CHUNK_VIEW_DIST_BUILDING) {
                            newBrightChunks.add(new FogChunk(chunkInfo, FogTransitionBrightness.DARK_TO_BRIGHT));
                            break;
                        }
                    }
            }

            if (!newBrightChunks.equals(oldBrightChunks)) {

                // chunks that just entered the bright zone
                Set<FogChunk> diff1 = ConcurrentHashMap.newKeySet();
                diff1.addAll(newBrightChunks);
                diff1.removeIf(c -> {
                    for (FogChunk oldBrightChunk : oldBrightChunks)
                        if (c.chunkInfo.chunk.bb.equals(oldBrightChunk.chunkInfo.chunk.bb))
                            return true;
                    return false;
                });

                // chunks that just left the bright zone
                Set<FogChunk> diff2 = ConcurrentHashMap.newKeySet();
                diff2.addAll(oldBrightChunks);
                diff2.removeIf(c -> {
                    for (FogChunk newBrightChunk : newBrightChunks)
                        if (c.chunkInfo.chunk.bb.equals(newBrightChunk.chunkInfo.chunk.bb))
                            return true;
                    return false;
                });

                // add new bright chunks
                boolean chunkExists;
                for (FogChunk fogChunkNew : diff1) {
                    chunkExists = false;
                    for (FogChunk fogChunkCurrent : FogOfWarClientEvents.fogChunks) {
                        // this chunk already exists, so just update its brightness
                        if (fogChunkNew.chunkInfo.chunk.bb.equals(fogChunkCurrent.chunkInfo.chunk.bb)) {
                            fogChunkCurrent.setBrightness(FogTransitionBrightness.SEMI_TO_BRIGHT);
                            chunkExists = true;
                        }
                    }
                    if (!chunkExists) {
                        FogOfWarClientEvents.fogChunks.add(fogChunkNew);
                    }
                }

                // update chunks that fell out of bright zone
                for (FogChunk fogChunkNew : diff2) {
                    for (FogChunk fogChunkCurrent : FogOfWarClientEvents.fogChunks) {
                        // this chunk already exists, so just update its brightness
                        if (fogChunkNew.chunkInfo.chunk.bb.equals(fogChunkCurrent.chunkInfo.chunk.bb)) {
                            fogChunkCurrent.setBrightness(FogTransitionBrightness.BRIGHT_TO_SEMI);
                            break;
                        }
                    }
                }
            }

            this.minecraft.getProfiler().pop();
        }
    }


    @Shadow private ChunkRenderDispatcher chunkRenderDispatcher;
    @Shadow private ClientLevel level;

    @Inject(
            method = "compileChunks(Lnet/minecraft/client/Camera;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void compileChunks(Camera pCamera, CallbackInfo ci) {
        if (!FogOfWarClientEvents.isEnabled())
            return;

        ci.cancel();

        this.minecraft.getProfiler().push("populate_chunks_to_compile");
        RenderRegionCache renderregioncache = new RenderRegionCache();
        BlockPos blockpos = pCamera.getBlockPosition();
        List<ChunkRenderDispatcher.RenderChunk> list = Lists.newArrayList();

        List<AABB> brightAABBs = FogOfWarClientEvents.fogChunks.stream()
                .filter(FogChunk::isBrightChunk)
                .map(c -> c.chunkInfo.chunk.bb).toList();
        List<FogChunk> exploredChunksToNoLongerRender = new ArrayList<>();

        outerLoop:
        for(LevelRenderer.RenderChunkInfo chunkInfo : this.renderChunksInFrustum) {

            if (!brightAABBs.contains(chunkInfo.chunk.bb)) {
                // exploredChunks contains the bb and shouldBeRendered is false
                for (FogChunk chunk : FogOfWarClientEvents.fogChunks) {
                    if (chunk.isExploredChunk() && chunk.isAtFinalBrightness() && chunk.chunkInfo.chunk.bb.equals(chunkInfo.chunk.bb)) {
                        if (!chunk.shouldBeRendered)
                            continue outerLoop; // skip rendering this entirely, causes the chunk to retain its old view
                        else {
                            exploredChunksToNoLongerRender.add(chunk); // render this now and never again
                        }
                    }
                }
            }

            ChunkRenderDispatcher.RenderChunk renderChunk = chunkInfo.chunk;
            ChunkPos chunkpos = new ChunkPos(renderChunk.getOrigin());
            if (renderChunk.isDirty() && this.level.getChunk(chunkpos.x, chunkpos.z).isClientLightReady()) {
                boolean flag = false;
                if (this.minecraft.options.prioritizeChunkUpdates().get() != PrioritizeChunkUpdates.NEARBY) {
                    if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
                        flag = renderChunk.isDirtyFromPlayer();
                    }
                } else {
                    BlockPos blockpos1 = renderChunk.getOrigin().offset(8, 8, 8);
                    flag = !net.minecraftforge.common.ForgeConfig.CLIENT.alwaysSetupTerrainOffThread.get() && (blockpos1.distSqr(blockpos) < 768.0D || renderChunk.isDirtyFromPlayer()); // the target is the else block below, so invert the forge addition to get there early
                }

                if (flag) {
                    this.minecraft.getProfiler().push("build_near_sync");
                    this.chunkRenderDispatcher.rebuildChunkSync(renderChunk, renderregioncache);
                    renderChunk.setNotDirty();
                    this.minecraft.getProfiler().pop();
                } else {
                    list.add(renderChunk);
                }
            }
        }

        this.minecraft.getProfiler().popPush("upload");
        this.chunkRenderDispatcher.uploadAllPendingUploads();
        this.minecraft.getProfiler().popPush("schedule_async_compile");

        for(ChunkRenderDispatcher.RenderChunk renderChunk1 : list) {
            renderChunk1.rebuildChunkAsync(this.chunkRenderDispatcher, renderregioncache);
            renderChunk1.setNotDirty();
        }

        this.minecraft.getProfiler().pop();

        // mark all chunks that just left the bright region as rendered
        for (FogChunk chunk : exploredChunksToNoLongerRender)
            chunk.shouldBeRendered = false;
    }

    @Shadow @Final private AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);

    // always recheck chunks being in frustum - without this normally only checks when the camera moves
    @Inject(
            method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At("HEAD")
    )
    private void setupRender(Camera pCamera, Frustum pFrustum, boolean pHasCapturedFrustum, boolean pIsSpectator, CallbackInfo ci) {
        if (!FogOfWarClientEvents.isEnabled())
            return;

        if (!OrthoviewClientEvents.isEnabled())
            return;

        needsFrustumUpdate.set(true);
    }
}
