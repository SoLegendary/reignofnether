package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.unit.UnitAnimationAction;
import net.minecraft.client.animation.AnimationDefinition;

public interface KeyframeAnimated {
    public float getAgeInTicksOffset();
    public void setAgeInTicksOffset(float ticks);
    public void stopAllAnimations();
    public void setAnimateTicksLeft(int ticks);
    public int getAnimateTicksLeft();
    public void playSingleAnimation(UnitAnimationAction animAction);
    public default float getAnimationSpeed() {
        return 1.0f;
    }
    public default int getAttackWindupTicks() {
        return 0;
    }
    public default void startAnimation(AnimationDefinition animDef) {
        if (getAnimateTicksLeft() <= 0) {
            setAnimateTicksLeft((int) ((animDef.lengthInSeconds() * 20) *
                    ((animDef.lengthInSeconds() > 10 ? 1.1f : 1.0f) / getAnimationSpeed()))
            );
        }
    }
}
