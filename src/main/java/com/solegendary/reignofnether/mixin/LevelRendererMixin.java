package com.solegendary.reignofnether.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    private ClientLevel level;

    @Final
    @Shadow
    private RenderBuffers renderBuffers;

    @Shadow
    private void renderEntity(Entity p_109518_, double p_109519_, double p_109520_, double p_109521_, float p_109522_, PoseStack p_109523_, MultiBufferSource p_109524_) {
        throw new IllegalStateException("Mixin failed to shadow renderEntity()");
    }


    @Inject(
        method = "renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lcom/mojang/math/Matrix4f;)V",
        at = @At("HEAD")
    )
    private <E extends Entity> void shouldRender(
            PoseStack p_109600_, float p_109601_, long p_109602_, boolean p_109603_, Camera p_109604_, GameRenderer p_109605_, LightTexture p_109606_, Matrix4f p_109607_, CallbackInfo ci
    ) {
        if (!OrthoviewClientEvents.isEnabled())
            return;

        Vec3 vec3 = p_109604_.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();

        MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();

        for(Entity entity : UnitClientEvents.allEntities) {
            this.renderEntity(entity, d0, d1, d2, p_109601_, p_109600_, multibuffersource$buffersource);
        }

        // TODO: this technically works and triggers renderEvents but the units still leave the level
        // TODO: maybe for clientside try saving the Entity reference directly instead of id only?
        // TODO: maybe we can prevent LevelCallback.onTrackingEnd from affecting Units?
    }

}
