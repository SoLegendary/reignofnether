
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
    // set by restart() when any animation restarts during the current setupAnim; consumed by finishPose
    private boolean poseRestarted = false;

    private static class PoseBlend {
        AnimationDefinition lastSource = null;
        float[] lastPose = null; // final pose rendered last frame
        float[] blendFromPose = null;
        float blendStartTicks = 0;
        boolean blending = false;
        float walkWeight = 0; // how much of the walk layer is applied over idle, 0-1
        float lastTicks = 0; // ageInTicks of the previous setupAnim
    }

    public KeyframeHierarchicalModel() { this(RenderType::entityCutoutNoCull); }

    public KeyframeHierarchicalModel(Function<ResourceLocation, RenderType> p_170623_) {
        super(p_170623_);
    }

    private static final float WALK_LIMB_SWING_THRESHOLD = 0.001f;

    // scratch poses for the idle base layer, only valid within a single setupAnim
    private float[] preIdlePose = null;
    private float[] idlePose = null;
    private Entity walkLayerEntity = null;
    private float walkLayerAgeInTicks = 0;
    private boolean walkLayerPending = false; // beginWalkOverIdle was called, so the next animateWalk blends over idle
    private boolean walkLayerUsed = false;

    // true while the unit is moving, or while the walk layer is still fading out after it stopped
    protected boolean isWalking(Entity entity, float limbSwingAmount) {
        if (entity.isInWaterOrBubble()) {
            return false;
        }
        if (limbSwingAmount > WALK_LIMB_SWING_THRESHOLD) {
            return true;
        }
        PoseBlend blend = poseBlends.get(entity);
        return blend != null && blend.walkWeight > 0;
    }

    /**
     * Only needed for models whose animations are not synced in the bbmodel with the default pose, i.e. the first frame of
     * every animation does not match the default pose (such as the skeleton vanguard). Models that follow that rule should
     * keep using restart(walkAnimState) and the default animateWalk.
     * <p>
     * Why: the default animateWalk fades the walk in and out by shrinking it towards the default pose. That is fine when
     * idle also starts from the default pose, because stopping just settles into idle. But if idle does NOT start from the
     * default pose, a stopping unit would slide back to the default pose first and then jump to idle's first frame, and a
     * starting unit would do the reverse.
     * <p>
     * What this does instead: idle keeps playing underneath the whole time (so it is never restarted from its first frame),
     * and the walk is faded in and out on top of it by a weight that follows limbSwingAmount. The unit always moves between
     * the idle and walk poses directly and never passes through the default pose.
     * <p>
     * Call in place of restart(walkAnimState) in the walk branch, immediately before animateWalk.
     */
    protected void beginWalkOverIdle(KeyframeAnimated kfa, AnimationState idleState, AnimationDefinition idleDef, float ageInTicks) {
        List<ModelPart> parts = this.root().getAllParts().toList();
        int size = parts.size() * FLOATS_PER_PART;
        if (preIdlePose == null || preIdlePose.length != size) {
            preIdlePose = new float[size];
            idlePose = new float[size];
        }
        capturePose(parts, preIdlePose);
        restartThenAnimate(kfa, idleState, idleDef, ageInTicks);
        capturePose(parts, idlePose);
        walkLayerEntity = (Entity) kfa;
        walkLayerAgeInTicks = ageInTicks;
        walkLayerPending = true;
    }

    // from 1.20 HierarchicalModel
    protected void animateWalk(AnimationDefinition animDef, float limbSwing, float limbSwingAmount, float limbSwingSpeed, float limbSwingAmountSpeed) {
        long i = (long)(limbSwing * 50.0F * limbSwingSpeed);
        float f = Math.min(limbSwingAmount * limbSwingAmountSpeed, 1.0f);

        if (!walkLayerPending) {
            currentPoseSource = animDef;
            KeyframeAnimations.animate(this, animDef, i, f, ANIMATION_VECTOR_CACHE);
            return;
        }
        walkLayerPending = false;
        walkLayerUsed = true;

        // move the walk weight towards the target at a limited rate so starting/stopping always takes about blendTicks
        PoseBlend blend = poseBlends.computeIfAbsent(walkLayerEntity, e -> new PoseBlend());
        int blendTicks = walkLayerEntity instanceof KeyframeAnimated kfa ? kfa.getAnimationBlendTicks() : 8;
        float maxStep = blendTicks > 0 ? Mth.clamp(walkLayerAgeInTicksDelta(blend), 0, 2) / blendTicks : 1;
        blend.walkWeight += Mth.clamp(f - blend.walkWeight, -maxStep, maxStep);
        float weight = blend.walkWeight * blend.walkWeight * (3 - 2 * blend.walkWeight);

        // pose = lerp(idle, walk, weight), where walk is applied on top of the same starting pose idle was
        List<ModelPart> parts = this.root().getAllParts().toList();
        setPose(parts, preIdlePose);
        KeyframeAnimations.animate(this, animDef, i, 1.0f, ANIMATION_VECTOR_CACHE);
        applyBlend(parts, idlePose, weight);
        // currentPoseSource stays as idle, so idle <-> walk never triggers a crossfade in finishPose
    }

    private float walkLayerAgeInTicksDelta(PoseBlend blend) {
        return walkLayerAgeInTicks - blend.lastTicks;
    }

    // original method
    protected void restart(KeyframeAnimated kfa, AnimationState animState, float ageInTicks) {
        if (!animState.isStarted()) {
            // an animation (re)starting from time 0 is a pose discontinuity even if it's the same animation as before,
            // e.g. resetBehaviours() on a new order stops idle and it restarts the next frame
            poseRestarted = true;
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
        boolean restarted = poseRestarted;
        poseRestarted = false;

        List<ModelPart> parts = this.root().getAllParts().toList();
        int size = parts.size() * FLOATS_PER_PART;
        PoseBlend blend = poseBlends.computeIfAbsent(entity, e -> new PoseBlend());

        // walk layer is only carried over frames where beginWalkOverIdle/animateWalk actually ran
        if (!walkLayerUsed) {
            blend.walkWeight = 0;
        }
        walkLayerUsed = false;
        walkLayerPending = false;
        blend.lastTicks = ageInTicks;

        if (blend.lastPose == null || blend.lastPose.length != size) {
            blend.lastPose = new float[size];
            blend.blendFromPose = new float[size];
            blend.lastSource = source;
            blend.blending = false;
            capturePose(parts, blend.lastPose);
            return;
        }
        if (source != blend.lastSource || restarted) {
            System.arraycopy(blend.lastPose, 0, blend.blendFromPose, 0, size);
            blend.blendStartTicks = ageInTicks;
            blend.blending = true;
            blend.lastSource = source;
        }
        if (blend.blending) {
            int blendTicks = entity instanceof KeyframeAnimated kfa ? kfa.getAnimationBlendTicks() : 8;
            // blend quickly into once-off animations (attacks) so they aren't visually delayed, slowly into loops
            if (source != null && !source.looping()) {
                blendTicks = Math.max(1, blendTicks / 3);
            }
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

    private static void setPose(List<ModelPart> parts, float[] pose) {
        for (int i = 0; i < parts.size(); i++) {
            ModelPart p = parts.get(i);
            int o = i * FLOATS_PER_PART;
            p.x = pose[o];
            p.y = pose[o + 1];
            p.z = pose[o + 2];
            p.xRot = pose[o + 3];
            p.yRot = pose[o + 4];
            p.zRot = pose[o + 5];
            p.xScale = pose[o + 6];
            p.yScale = pose[o + 7];
            p.zScale = pose[o + 8];
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
