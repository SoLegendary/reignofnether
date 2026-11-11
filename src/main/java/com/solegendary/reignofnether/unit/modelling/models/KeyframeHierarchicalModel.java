
package com.solegendary.reignofnether.unit.modelling.models;

import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.units.piglins.MarauderUnit;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Function;


@OnlyIn(Dist.CLIENT)
public abstract class KeyframeHierarchicalModel<E extends Entity> extends HierarchicalModel<E> {
    private static final Vector3f ANIMATION_VECTOR_CACHE = new Vector3f();

    // x, y, z, xRot, yRot, zRot, xScale, yScale, zScale
    private static final int FLOATS_PER_PART = 9;

    // The model instance is shared by every unit of its type, so the per-unit blend state is held here, keyed weakly by
    // the entity (Entity.hashCode/equals use the entity id). Values must never reference the entity.
    private final WeakHashMap<Entity, PoseBlend> poseBlends = new WeakHashMap<>();

    // the animation applied during the current setupAnim; set by animateWalk/restartThenAnimate and consumed by finishPose
    private AnimationDefinition currentPoseSource = null;

    private static class PoseBlend {
        AnimationDefinition lastSource = null;
        float[] lastPose = null; // final pose rendered last frame
        float[] blendFromPose = null;
        float blendStartTicks = 0;
        boolean blending = false;
    }

    public KeyframeHierarchicalModel() { this(RenderType::entityCutoutNoCull); }

    public KeyframeHierarchicalModel(Function<ResourceLocation, RenderType> p_170623_) {
        super(p_170623_);
    }

    // from 1.20 HierarchicalModel
    protected void animateWalk(AnimationDefinition animDef, float limbSwing, float limbSwingAmount, float limbSwingSpeed, float limbSwingAmountSpeed) {
        currentPoseSource = animDef;
        long i = (long)(limbSwing * 50.0F * limbSwingSpeed);
        float f = Math.min(limbSwingAmount * limbSwingAmountSpeed, 1.0f);
        KeyframeAnimations.animate(this, animDef, i, f, ANIMATION_VECTOR_CACHE);
    }

    // original method
    protected void restart(KeyframeAnimated kfa, AnimationState animState, float ageInTicks) {
        if (!animState.isStarted()) {
            kfa.setAgeInTicksOffset(ageInTicks);
            kfa.stopAllAnimations();
            animState.start(0);
        }
    }

    protected void restartThenAnimate(KeyframeAnimated kfa, AnimationState animState, AnimationDefinition animDef, float ageInTicks) {
        restartThenAnimate(kfa, animState, animDef, ageInTicks, 1.0f, 1.0f);
    }

    protected void restartThenAnimate(KeyframeAnimated kfa, AnimationState animState, AnimationDefinition animDef, float ageInTicks, float scale) {
        restartThenAnimate(kfa, animState, animDef, ageInTicks, scale, 1.0f);
    }

    protected void restartThenAnimate(KeyframeAnimated kfa, AnimationState animState, AnimationDefinition animDef, float ageInTicks, float scale, float speed) {
        currentPoseSource = animDef;
        restart(kfa, animState, ageInTicks);
        animState.updateTime(ageInTicks - kfa.getAgeInTicksOffset(), speed);
        animState.ifStarted((time) -> {
            KeyframeAnimations.animate(this, animDef, time.getAccumulatedTime(), scale, ANIMATION_VECTOR_CACHE);
        });
    }

    /**
     * Call at the end of setupAnim, after the animation has been applied. Animations are offsets on top of the rest pose,
     * so switching between them (idle/walk/attack etc.) would otherwise snap from one pose to the other unless both
     * start and end at the rest pose. Instead, when the animation changes, crossfade from the last rendered pose.
     */
    protected void finishPose(Entity entity, float ageInTicks) {
        AnimationDefinition source = currentPoseSource;
        currentPoseSource = null;

        List<ModelPart> parts = this.root().getAllParts().toList();
        int size = parts.size() * FLOATS_PER_PART;
        PoseBlend blend = poseBlends.computeIfAbsent(entity, e -> new PoseBlend());

        if (blend.lastPose == null || blend.lastPose.length != size) {
            blend.lastPose = new float[size];
            blend.blendFromPose = new float[size];
            blend.lastSource = source;
            blend.blending = false;
            capturePose(parts, blend.lastPose);
            return;
        }
        if (source != blend.lastSource) {
            System.arraycopy(blend.lastPose, 0, blend.blendFromPose, 0, size);
            blend.blendStartTicks = ageInTicks;
            blend.blending = true;
            blend.lastSource = source;
        }
        if (blend.blending) {
            int blendTicks = entity instanceof KeyframeAnimated kfa ? kfa.getAnimationBlendTicks() : 5;
            float t = blendTicks > 0 ? (ageInTicks - blend.blendStartTicks) / blendTicks : 1;
            if (t >= 1 || t < 0) {
                blend.blending = false;
            } else {
                applyBlend(parts, blend.blendFromPose, t * t * (3 - 2 * t));
            }
        }
        capturePose(parts, blend.lastPose);
    }

    private static void capturePose(List<ModelPart> parts, float[] out) {
        for (int i = 0; i < parts.size(); i++) {
            ModelPart p = parts.get(i);
            int o = i * FLOATS_PER_PART;
            out[o] = p.x;
            out[o + 1] = p.y;
            out[o + 2] = p.z;
            out[o + 3] = p.xRot;
            out[o + 4] = p.yRot;
            out[o + 5] = p.zRot;
            out[o + 6] = p.xScale;
            out[o + 7] = p.yScale;
            out[o + 8] = p.zScale;
        }
    }

    // moves each part from its snapshot (amount = 0) to its current pose (amount = 1)
    private static void applyBlend(List<ModelPart> parts, float[] from, float amount) {
        for (int i = 0; i < parts.size(); i++) {
            ModelPart p = parts.get(i);
            int o = i * FLOATS_PER_PART;
            p.x = Mth.lerp(amount, from[o], p.x);
            p.y = Mth.lerp(amount, from[o + 1], p.y);
            p.z = Mth.lerp(amount, from[o + 2], p.z);
            p.xRot = Mth.lerp(amount, from[o + 3], p.xRot);
            p.yRot = Mth.lerp(amount, from[o + 4], p.yRot);
            p.zRot = Mth.lerp(amount, from[o + 5], p.zRot);
            p.xScale = Mth.lerp(amount, from[o + 6], p.xScale);
            p.yScale = Mth.lerp(amount, from[o + 7], p.yScale);
            p.zScale = Mth.lerp(amount, from[o + 8], p.zScale);
        }
    }
}
