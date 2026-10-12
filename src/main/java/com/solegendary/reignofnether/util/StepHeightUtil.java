package com.solegendary.reignofnether.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Replaces {@code Entity#setMaxUpStep(float)}, deleted in 1.21.1.
 *
 * <p>Step height used to be a plain mutable field on {@code Entity}; it is now the
 * {@link Attributes#STEP_HEIGHT} attribute, so the equivalent of setting the field is setting
 * the attribute's base value. Reads go through {@code Entity#maxUpStep()}, which the attribute
 * feeds automatically.
 */
public final class StepHeightUtil {

    private StepHeightUtil() { }

    public static void setMaxUpStep(LivingEntity entity, float height) {
        AttributeInstance attribute = entity.getAttribute(Attributes.STEP_HEIGHT);
        if (attribute != null)
            attribute.setBaseValue(height);
    }
}