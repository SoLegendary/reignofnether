package com.solegendary.reignofnether.mixin;

import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ObjectiveCriteria.class)
public interface ObjectiveCriteriaAccessor {
	
	@Invoker(value = "registerCustom")
	static ObjectiveCriteria registerCustomCriteria(String pName, boolean pReadOnly, ObjectiveCriteria.RenderType pRenderType) {
		throw new AssertionError();
	}
	
}
