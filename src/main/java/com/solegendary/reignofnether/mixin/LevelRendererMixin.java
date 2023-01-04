package com.solegendary.reignofnether.mixin;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Final @Shadow private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;
    @Final @Shadow private AtomicReference<LevelRenderer.RenderChunkStorage> renderChunkStorage = new AtomicReference<>();
    @Final @Shadow private Minecraft minecraft;

    @Inject(
        method = "applyFrustum(Lnet/minecraft/client/renderer/culling/Frustum;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void applyFrustum(Frustum pFrustum, CallbackInfo ci) {'
        return;

        if (OrthoviewClientEvents.enabledCount <= 0)
            return;

        ci.cancel();

        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
        } else {
            this.minecraft.getProfiler().push("apply_frustum");
            this.renderChunksInFrustum.clear();

            LinkedHashSet<LevelRenderer.RenderChunkInfo> renderChunkInfos = (this.renderChunkStorage.get()).renderChunks;

            for(LevelRenderer.RenderChunkInfo chunkInfo : renderChunkInfos) {
                if (pFrustum.isVisible(chunkInfo.chunk.getBoundingBox())) {

                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {

                        Vec3 centre = chunkInfo.chunk.bb.getCenter();
                        Vec2 centre2d = new Vec2((float) centre.x(), (float) centre.z());
                        Vec2 entity2d = new Vec2((float) entity.getX(), (float) entity.getZ());

                        if (entity2d.distanceToSqr(centre2d) < 900) {
                            this.renderChunksInFrustum.add(chunkInfo);
                            break;
                        }
                    }
                }
            }
            this.minecraft.getProfiler().pop();
        }
    }

    @Shadow @Final private AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);

    @Inject(
            method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At("HEAD")
    )
    private void applyFrustum(Camera pCamera, Frustum pFrustum, boolean pHasCapturedFrustum, boolean pIsSpectator, CallbackInfo ci) {
        //if (!OrthoviewClientEvents.isEnabled())
        //    return;

        needsFrustumUpdate.set(true);
    }
}
