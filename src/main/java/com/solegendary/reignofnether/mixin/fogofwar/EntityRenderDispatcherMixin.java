package com.solegendary.reignofnether.mixin.fogofwar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    private static final ResourceLocation SOUL_FIRE_0 =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/soul_fire_0");
    private static final ResourceLocation SOUL_FIRE_1 =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/soul_fire_1");

    private static TextureAtlasSprite getSprite(ResourceLocation loc) {
        return Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(loc);
    }

    @Shadow public Camera camera;
    @Shadow private static void fireVertex(PoseStack.Pose pMatrixEntry, VertexConsumer pBuffer, float pX, float pY, float pZ, float pTexU, float pTexV) { }

    // TODO: cache this?
    @Unique private static boolean reignofnether$shouldRenderSoulfire(Entity entity) {
        if (entity instanceof LivingEntity le) {
            return le.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get());
        } else if (entity instanceof Projectile proj && proj.getOwner() instanceof LivingEntity le) {
            return le.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get());
        }
        return false;
    }

    @Inject(
            method = "renderFlame",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderFlame(PoseStack poseStack, MultiBufferSource pBuffer, Entity pEntity, CallbackInfo ci) {
        if (!FogOfWarClientEvents.isInBrightChunk(pEntity))
            ci.cancel();
        else if (reignofnether$shouldRenderSoulfire(pEntity)) {
            ci.cancel();
            TextureAtlasSprite textureatlassprite = getSprite(SOUL_FIRE_0);
            TextureAtlasSprite textureatlassprite1 = getSprite(SOUL_FIRE_1);
            poseStack.pushPose();
            float f = pEntity.getBbWidth() * 1.4F;
            poseStack.scale(f, f, f);
            float f1 = 0.5F;
            float f2 = 0.0F;
            float f3 = pEntity.getBbHeight() / f;
            float f4 = 0.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(-this.camera.getYRot()));
            poseStack.translate(0.0F, 0.0F, -0.3F + (float)((int)f3) * 0.02F);
            float f5 = 0.0F;
            int i = 0;
            VertexConsumer vertexconsumer = pBuffer.getBuffer(Sheets.cutoutBlockSheet());

            for(PoseStack.Pose posestack$pose = poseStack.last(); f3 > 0.0F; ++i) {
                TextureAtlasSprite textureatlassprite2 = i % 2 == 0 ? textureatlassprite : textureatlassprite1;
                float f6 = textureatlassprite2.getU0();
                float f7 = textureatlassprite2.getV0();
                float f8 = textureatlassprite2.getU1();
                float f9 = textureatlassprite2.getV1();
                if (i / 2 % 2 == 0) {
                    float f10 = f8;
                    f8 = f6;
                    f6 = f10;
                }
                fireVertex(posestack$pose, vertexconsumer, f1 - 0.0F, 0.0F - f4, f5, f8, f9);
                fireVertex(posestack$pose, vertexconsumer, -f1 - 0.0F, 0.0F - f4, f5, f6, f9);
                fireVertex(posestack$pose, vertexconsumer, -f1 - 0.0F, 1.4F - f4, f5, f6, f7);
                fireVertex(posestack$pose, vertexconsumer, f1 - 0.0F, 1.4F - f4, f5, f8, f7);
                f3 -= 0.45F;
                f4 -= 0.45F;
                f1 *= 0.9F;
                f5 += 0.03F;
            }
            poseStack.popPose();
        }
    }
}
