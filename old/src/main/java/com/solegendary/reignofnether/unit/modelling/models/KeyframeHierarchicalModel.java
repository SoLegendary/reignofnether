
package com.solegendary.reignofnether.unit.modelling.models;

import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.function.Function;


@OnlyIn(Dist.CLIENT)
public abstract class KeyframeHierarchicalModel<E extends Entity> extends HierarchicalModel<E> {
    private static final Vector3f ANIMATION_VECTOR_CACHE = new Vector3f();

    protected float ageInTicksOffset = 0;

    public KeyframeHierarchicalModel() { this(RenderType::entityCutoutNoCull); }

    public KeyframeHierarchicalModel(Function<ResourceLocation, RenderType> p_170623_) {
        super(p_170623_);
    }

    // from 1.20 HierarchicalModel
    protected void animateWalk(AnimationDefinition animDef, float limbSwing, float limbSwingAmount, float limbSwingSpeed, float limbSwingAmountSpeed) {
        long i = (long)(limbSwing * 50.0F * limbSwingSpeed);
        float f = Math.min(limbSwingAmount * limbSwingAmountSpeed, 1.0f);
        KeyframeAnimations.animate(this, animDef, i, f, ANIMATION_VECTOR_CACHE);
    }

    // original method
    protected void restart(KeyframeAnimated kfa, AnimationState animState, AnimationDefinition animDef, float ageInTicks) {
        if (!animState.isStarted()) {
            ageInTicksOffset = ageInTicks;
            kfa.stopAllAnimations();
            animState.start(0);
        }
    }

    protected void restartThenAnimate(KeyframeAnimated kfa, AnimationState animState, AnimationDefinition animDef, float ageInTicks) {
        restartThenAnimate(kfa, animState, animDef, ageInTicks, 1.0f);
    }

    protected void restartThenAnimate(KeyframeAnimated kfa, AnimationState animState, AnimationDefinition animDef, float ageInTicks, float scale) {
        restart(kfa, animState, animDef, ageInTicks);
        animState.updateTime(ageInTicks - ageInTicksOffset, 1.0f);
        animState.ifStarted((time) -> {
            KeyframeAnimations.animate(this, animDef, time.getAccumulatedTime(), scale, ANIMATION_VECTOR_CACHE);
        });
    }

    protected void applyStatic(AnimationDefinition animDef) {
        KeyframeAnimations.animate(this, animDef, 0L, 1.0F, ANIMATION_VECTOR_CACHE);
    }
}
