package com.solegendary.reignofnether.mixin.fogofwar;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.fogofwar.FogChunk;
import com.solegendary.reignofnether.fogofwar.FogTransitionBrightness;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents.*;


@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    private static final int UPDATE_TICKS_MAX = 10;
    private static int updateTicks = 0;
    private static int timesUpdated = 0;

    @Final @Shadow private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;
    @Final @Shadow private AtomicReference<LevelRenderer.RenderChunkStorage> renderChunkStorage = new AtomicReference<>();
    @Final @Shadow private Minecraft minecraft;
    @Final @Shadow private RenderBuffers renderBuffers;
    @Final @Shadow private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

    // any chunkInfo objects added to renderChunksInFrustum will be rendered
    // we can collect old chunk data here to render them in their past state
    @Inject(
        method = "applyFrustum(Lnet/minecraft/client/renderer/culling/Frustum;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void applyFrustum(Frustum pFrustum, CallbackInfo ci) {
        if (this.minecraft.level == null || !isEnabled())
            return;

        ci.cancel();

        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
        } else {
            this.minecraft.getProfiler().push("apply_frustum");

            LinkedHashSet<LevelRenderer.RenderChunkInfo> renderChunkInfos = (this.renderChunkStorage.get()).renderChunks;

            // refresh renderChunksInFrustum
            this.renderChunksInFrustum.clear();
            for (LevelRenderer.RenderChunkInfo chunkInfo : renderChunkInfos)
                if (pFrustum.isVisible(chunkInfo.chunk.getBoundingBox()))
                    this.renderChunksInFrustum.add(chunkInfo);

            if (updateTicks < UPDATE_TICKS_MAX) {
                updateTicks += 1;
                this.minecraft.getProfiler().pop();
                return;
            }
            else {
                updateTicks = 0;
                occupiedChunks.clear();
            }

            // bright chunks in last tick
            Set<FogChunk> oldBrightChunks = fogChunks.stream()
                    .filter(fc -> fc.getFinalBrightness() == FogChunk.BRIGHT)
                    .collect(Collectors.toSet());
            // bright chunks in current tick
            Set<FogChunk> newBrightChunks = ConcurrentHashMap.newKeySet();
            // chunks rebuilt from exploredChunkPoses
            Set<FogChunk> rebuiltExploredChunks = ConcurrentHashMap.newKeySet();


            // get chunks that have units/buildings that can see
            for (LivingEntity entity : UnitClientEvents.getAllUnits())
                if (UnitClientEvents.getPlayerToEntityRelationship(entity) == Relationship.OWNED)
                    occupiedChunks.add(this.minecraft.level.getChunk(entity.getOnPos()).getPos());

            for (Building building : BuildingClientEvents.getBuildings())
                if (BuildingClientEvents.getPlayerToBuildingRelationship(building) == Relationship.OWNED)
                    occupiedChunks.add(this.minecraft.level.getChunk(building.centrePos).getPos());


            // can't use renderChunksInFrustum because then we wouldn't update explored status of chunks we aren't looking at
            if (!lastOccupiedChunks.equals(occupiedChunks) || forceUpdate) {
                forceUpdate = false;

                outerLoop:
                for (LevelRenderer.RenderChunkInfo chunkInfo : renderChunkInfos) {
                    ChunkPos renderChunkPos = this.minecraft.level.getChunk(chunkInfo.chunk.getOrigin()).getPos();

                    for (ChunkPos chunkPos : occupiedChunks) {
                        if (chunkPos.getChessboardDistance(renderChunkPos) < CHUNK_VIEW_DIST) {
                            newBrightChunks.add(new FogChunk(chunkInfo, FogTransitionBrightness.DARK_TO_BRIGHT));
                            continue outerLoop;
                        }
                    }
                    // after resetting fog of war for any reason, we can rebuild using a list of explored ChunkPoses
                    // this has to be a continuous effort since we don't have all renderChunkInfos loaded at one time
                    // and we don't know when we are done with them all
                    if (exploredChunks.contains(renderChunkPos))
                        rebuiltExploredChunks.add(new FogChunk(chunkInfo, FogTransitionBrightness.DARK_TO_SEMI));
                }

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

                if (diff1.size() > 0 || diff2.size() > 0)
                    onChunksChange(diff1, diff2);

                // add new bright chunks
                boolean chunkExists;
                for (FogChunk fogChunkNew : diff1) {
                    exploredChunks.add(minecraft.level.getChunk(fogChunkNew.chunkInfo.chunk.getOrigin()).getPos());

                    chunkExists = false;
                    for (FogChunk fogChunkCurrent : fogChunks) {
                        // this chunk already exists, so just update its brightness
                        if (fogChunkNew.chunkInfo.chunk.bb.equals(fogChunkCurrent.chunkInfo.chunk.bb)) {
                            fogChunkCurrent.setBrightness(FogTransitionBrightness.SEMI_TO_BRIGHT);
                            chunkExists = true;
                        }
                    }
                    if (!chunkExists)
                        fogChunks.add(fogChunkNew);
                }

                // update chunks that fell out of bright zone
                for (FogChunk fogChunkNew : diff2) {
                    for (FogChunk fogChunkCurrent : fogChunks) {
                        // this chunk already exists, so just update its brightness
                        if (fogChunkNew.chunkInfo.chunk.bb.equals(fogChunkCurrent.chunkInfo.chunk.bb)) {
                            fogChunkCurrent.setBrightness(FogTransitionBrightness.BRIGHT_TO_SEMI);
                            break;
                        }
                    }
                }

                // update chunks that are readded from saved exploredChunkPoses
                for (FogChunk fogChunkNew : rebuiltExploredChunks) {
                    chunkExists = false;
                    for (FogChunk fogChunkCurrent : fogChunks) {
                        if (fogChunkNew.chunkInfo.chunk.bb.equals(fogChunkCurrent.chunkInfo.chunk.bb)) {
                            chunkExists = true;
                            break;
                        }
                    }
                    if (!chunkExists)
                        fogChunks.add(fogChunkNew);
                }
            }
            lastOccupiedChunks.clear();
            lastOccupiedChunks.addAll(occupiedChunks);

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
        if (!isEnabled())
            return;

        ci.cancel();

        this.minecraft.getProfiler().push("populate_chunks_to_compile");
        RenderRegionCache renderregioncache = new RenderRegionCache();
        BlockPos blockpos = pCamera.getBlockPosition();
        List<ChunkRenderDispatcher.RenderChunk> list = Lists.newArrayList();

        List<AABB> brightAABBs = fogChunks.stream()
                .filter(fc -> fc.getFinalBrightness() == FogChunk.BRIGHT)
                .map(c -> c.chunkInfo.chunk.bb).toList();
        List<FogChunk> exploredChunksToNoLongerRender = new ArrayList<>();

        outerLoop:
        for(LevelRenderer.RenderChunkInfo chunkInfo : this.renderChunksInFrustum) {

            if (!brightAABBs.contains(chunkInfo.chunk.bb)) {
                // exploredChunks contains the bb and shouldBeRendered is false
                for (FogChunk chunk : fogChunks) {
                    if (chunk.getFinalBrightness() == FogChunk.SEMI && chunk.isAtFinalBrightness() && !chunk.needsLightUpdate && chunk.chunkInfo.chunk.bb.equals(chunkInfo.chunk.bb)) {
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
        if (!isEnabled())
            return;

        if (!OrthoviewClientEvents.isEnabled())
            return;

        needsFrustumUpdate.set(true);
    }

    // rerun blockDestroyProgress overlays but with range extended to between 32-256 blocks
    @Inject(
            method = "renderLevel",
            at = @At("TAIL")
    )
    private void renderLevel(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime,
                             boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer,
                             LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci) {

        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();

        ObjectIterator var42 = this.destructionProgress.long2ObjectEntrySet().iterator();

        while (var42.hasNext()) {
            Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>> entry = (Long2ObjectMap.Entry) var42.next();
            BlockPos blockpos2 = BlockPos.of(entry.getLongKey());
            double d3 = (double) blockpos2.getX() - d0;
            double d4 = (double) blockpos2.getY() - d1;
            double d5 = (double) blockpos2.getZ() - d2;
            double distSqr = d3 * d3 + d4 * d4 + d5 * d5;
            if ((distSqr > 1024.0 && distSqr < 65536)) {
                SortedSet<BlockDestructionProgress> sortedset1 = (SortedSet) entry.getValue();
                if (sortedset1 != null && !sortedset1.isEmpty()) {
                    int k1 = (sortedset1.last()).getProgress();
                    pPoseStack.pushPose();
                    pPoseStack.translate((double) blockpos2.getX() - d0, (double) blockpos2.getY() - d1, (double) blockpos2.getZ() - d2);
                    PoseStack.Pose posestack$pose = pPoseStack.last();
                    VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer((RenderType) ModelBakery.DESTROY_TYPES.get(k1)), posestack$pose.pose(), posestack$pose.normal());
                    ModelData modelData = this.level.getModelDataManager().getAt(blockpos2);
                    this.minecraft.getBlockRenderer().renderBreakingTexture(this.level.getBlockState(blockpos2), blockpos2, this.level, pPoseStack, vertexconsumer1, modelData == null ? ModelData.EMPTY : modelData);
                    pPoseStack.popPose();
                }
            }
        }
    }
}
