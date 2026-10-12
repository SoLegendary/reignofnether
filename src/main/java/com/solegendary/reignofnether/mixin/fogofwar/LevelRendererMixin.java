package com.solegendary.reignofnether.mixin.fogofwar;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import static com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents.isEnabled;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Final @Shadow private Minecraft minecraft;
    @Final @Shadow private RenderBuffers renderBuffers;
    @Final @Shadow private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

    @Shadow private ClientLevel level;

    // always recheck chunks being in frustum - without this normally only checks when the camera moves
    @Inject(
            method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At("HEAD")
    )
    private void setupRender(Camera pCamera, Frustum pFrustum, boolean pHasCapturedFrustum, boolean pIsSpectator, CallbackInfo ci) {
        // 1.21.1 deleted LevelRenderer's AtomicBoolean needsFrustumUpdate along with the cached
        // frustum it guarded: setupRender now always rebuilds the frustum from the camera, which is
        // exactly what this hook was asking for - so there is nothing left to force here.
    }

    // rerun blockDestroyProgress overlays but with range extended to between 32-256 blocks
    @Inject(
            method = "renderLevel",
            at = @At("TAIL")
    )
    // 1.21.1 changed the signature to (DeltaTracker, boolean, Camera, GameRenderer, LightTexture,
    // Matrix4f, Matrix4f) and dropped the PoseStack the overlay used to be drawn with; projection
    // and model-view are now handed over separately instead. A PoseStack is rebuilt from the live
    // model-view matrix, which is the view-space transform vanilla used to pass in.
    private void renderLevel(DeltaTracker pDeltaTracker, boolean pRenderBlockOutline, Camera pCamera,
                             GameRenderer pGameRenderer, LightTexture pLightTexture,
                             Matrix4f pProjectionMatrix, Matrix4f pModelViewMatrix, CallbackInfo ci) {

        PoseStack pPoseStack = new PoseStack();
        pPoseStack.mulPose(RenderSystem.getModelViewMatrix());
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
                    // SheetedDecalTextureGenerator lost its normal-matrix argument in 1.21.1 and now
                    // takes the PoseStack.Pose itself.
                    VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(k1)), posestack$pose, 1);
                    ModelData modelData = this.level.getModelDataManager().getAt(blockpos2);
                    this.minecraft.getBlockRenderer().renderBreakingTexture(this.level.getBlockState(blockpos2), blockpos2, this.level, pPoseStack, vertexconsumer1, modelData == null ? ModelData.EMPTY : modelData);
                    pPoseStack.popPose();
                }
            }
        }
    }

    // increase render distance for particles
    @Shadow private ParticleStatus calculateParticleLevel(boolean pDecreased) { return null; }

    @Shadow @Nullable private PostChain entityEffect;

    @Inject(
            method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true
    )
    public void addParticleInternal(ParticleOptions pOptions, boolean pForce, boolean pDecreased, double pX, double pY, double pZ,
                                    double pXSpeed, double pYSpeed, double pZSpeed, CallbackInfoReturnable<Particle> cir) {
        if (!OrthoviewClientEvents.isEnabled())
            return;

        Camera camera = this.minecraft.gameRenderer.getMainCamera();
        if (this.minecraft != null && camera.isInitialized() && this.minecraft.particleEngine != null) {
            ParticleStatus particlestatus = this.calculateParticleLevel(pDecreased);
            if (pForce) {
                cir.setReturnValue(this.minecraft.particleEngine.createParticle(pOptions, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed));
            } else if (camera.getPosition().distanceToSqr(pX, pY, pZ) > 4096) {
                cir.setReturnValue(null);
            } else {
                cir.setReturnValue(particlestatus == ParticleStatus.MINIMAL ? null : this.minecraft.particleEngine.createParticle(pOptions, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed));
            }
        } else {
            cir.setReturnValue(null);
        }
    }

    /**
     * 1.21.1 replaced 16-block chunk sections and {@code LevelRenderer#compileChunks} with the
     * finer 16³ render sections and {@code ViewArea}, so the old "walk the frustum and
     * {@code chunk.setDirty(true)}" loop has no counterpart. {@code setBlocksDirty} is now the
     * public entry point and takes block coordinates, which is exactly what
     * {@link UnitClientEvents#windowPositions} holds, so the leaf-reveal re-render is expressed
     * directly as "dirty the sections around each window position" — same behaviour, no frustum
     * walk and no second bookkeeping pass needed.
     */
    @Shadow public abstract void setBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    private static final int LEAF_WINDOW_RADIUS = 25;
    private static final int LEAF_WINDOW_RADIUS_MINUS_1 = LEAF_WINDOW_RADIUS - 1;

    @Inject(
            method = "tick()V",
            at = @At("HEAD")
    )
    private void ron$reDirtyLeafWindowSections(CallbackInfo ci) {
        // hiding leaves around cursor
        if (OrthoviewClientEvents.hideLeavesMethod != OrthoviewClientEvents.LeafHideMethod.AROUND_UNITS_AND_CURSOR ||
                !OrthoviewClientEvents.isEnabled()) {
            return;
        }

        UnitClientEvents.windowUpdateTicks -= 1;
        if (UnitClientEvents.windowUpdateTicks > 0) {
            return;
        }
        UnitClientEvents.windowUpdateTicks = UnitClientEvents.WINDOW_UPDATE_TICKS_MAX;

        synchronized (UnitClientEvents.windowPositions) {
            for (BlockPos bp : UnitClientEvents.windowPositions) {
                setBlocksDirty(
                        bp.getX() - LEAF_WINDOW_RADIUS, bp.getY() - LEAF_WINDOW_RADIUS, bp.getZ() - LEAF_WINDOW_RADIUS,
                        bp.getX() + LEAF_WINDOW_RADIUS, bp.getY() + LEAF_WINDOW_RADIUS, bp.getZ() + LEAF_WINDOW_RADIUS
                );
            }
        }
    }
}
