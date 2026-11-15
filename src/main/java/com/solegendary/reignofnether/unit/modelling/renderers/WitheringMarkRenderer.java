package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.heroAbilities.witherborn.WitheringMark;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.modelling.animations.WitheringMarkAnimations;
import net.minecraft.client.Camera;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

// draws the withering mark eye as a camera-facing quad above any unit with the WITHERING_MARK effect
// the eye is a flat plane so it doesn't need an entity or model, just the texture and the scale animation
@OnlyIn(Dist.CLIENT)
public class WitheringMarkRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ReignOfNether.MOD_ID, "textures/misc/withering_mark.png");

    private static final float SIZE = 1.25f; // in blocks
    private static final float HEIGHT_ABOVE_HEAD = 2.0f; // clears the in-world health bar
    private static final float OPEN_TICKS = 10; // how long the eye takes to zip open when applied
    private static final float CLOSE_TICKS = 10; // how long the eye takes to zip shut at the end
    // the eye is 32x32 in the top left of a 64x64 texture
    private static final float UV_MAX = 0.5f;

    public static void render(float partialTick, PoseStack matrix, Camera camera) {
        if (camera == null)
            return;

        List<LivingEntity> markedEntities = UnitClientEvents.getAllUnits().stream()
                .filter(e -> !e.isRemoved() &&
                        e.hasEffect(MobEffectRegistrar.WITHERING_MARK.get()) &&
                        FogOfWarClientEvents.isInBrightChunk(e))
                .toList();
        if (markedEntities.isEmpty())
            return;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Vec3 camPos = camera.getPosition();

        for (LivingEntity entity : markedEntities) {
            MobEffectInstance effect = entity.getEffect(MobEffectRegistrar.WITHERING_MARK.get());
            if (effect == null)
                continue;

            double x = Mth.lerp(partialTick, entity.xo, entity.getX());
            double y = Mth.lerp(partialTick, entity.yo, entity.getY());
            double z = Mth.lerp(partialTick, entity.zo, entity.getZ());

            // each mark animates from when it was applied rather than in sync with every other mark
            // (effect durations count down so the effect's age is the max duration minus what's left)
            float ageTicks = Math.max(0, WitheringMark.DURATION_TICKS - effect.getDuration()) + partialTick;
            Vector3f scale = getScale(WitheringMarkAnimations.MARKED, WitheringMarkAnimations.EYE_MARK, ageTicks / 20f);

            // zip open vertically from the centre when applied (overshooting like the blink) and zip shut as the debuff runs out
            float open = ageTicks >= OPEN_TICKS ? 1f : easeOutBack(ageTicks / OPEN_TICKS);
            float close = Mth.clamp((WitheringMark.DURATION_TICKS - ageTicks) / CLOSE_TICKS, 0f, 1f);
            float openness = open * close * close;
            if (openness <= 0.001f)
                continue;

            matrix.pushPose();
            matrix.translate(x - camPos.x, y + entity.getBbHeight() + HEIGHT_ABOVE_HEAD - camPos.y, z - camPos.z);
            matrix.mulPose(camera.rotation());
            matrix.scale(SIZE * scale.x, SIZE * scale.y * openness, 1);

            drawQuad(matrix.last().pose());
            matrix.popPose();
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // 0 -> 1 with a small overshoot past 1 on the way
    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        float u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }

    // camera.rotation() makes local +x point to the viewer's left, so u is flipped (same as vanilla particles)
    private static void drawQuad(Matrix4f m4f) {
        float h = 0.5f;
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(m4f, -h, -h, 0).uv(UV_MAX, UV_MAX).endVertex();
        buffer.vertex(m4f, -h,  h, 0).uv(UV_MAX, 0).endVertex();
        buffer.vertex(m4f,  h,  h, 0).uv(0, 0).endVertex();
        buffer.vertex(m4f,  h, -h, 0).uv(0, UV_MAX).endVertex();
        tessellator.end();
    }

    // evaluates the SCALE channel of a bone the same way KeyframeAnimations.animate() does, but without needing a ModelPart
    private static Vector3f getScale(AnimationDefinition animDef, String boneName, float seconds) {
        Vector3f result = new Vector3f(1, 1, 1);
        List<AnimationChannel> channels = animDef.boneAnimations().get(boneName);
        if (channels == null)
            return result;

        float time = animDef.looping() ? seconds % animDef.lengthInSeconds() : Math.min(seconds, animDef.lengthInSeconds());

        for (AnimationChannel channel : channels) {
            if (channel.target() != AnimationChannel.Targets.SCALE)
                continue;
            Keyframe[] keyframes = channel.keyframes();
            int i = Math.max(0, Mth.binarySearch(0, keyframes.length, idx -> time <= keyframes[idx].timestamp()) - 1);
            int j = Math.min(keyframes.length - 1, i + 1);
            Keyframe k0 = keyframes[i];
            Keyframe k1 = keyframes[j];
            float span = k1.timestamp() - k0.timestamp();
            float delta = span > 0 ? Mth.clamp((time - k0.timestamp()) / span, 0.0f, 1.0f) : 0;

            Vector3f offset = new Vector3f();
            k1.interpolation().apply(offset, delta, keyframes, i, j, 1.0f);
            // scale keyframes are stored relative to 1 (see KeyframeAnimations.scaleVec)
            result.add(offset);
        }
        return result;
    }
}
