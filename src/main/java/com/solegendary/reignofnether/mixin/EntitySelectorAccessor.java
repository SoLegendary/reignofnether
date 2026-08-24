package com.solegendary.reignofnether.mixin;

import net.minecraft.commands.arguments.selector.EntitySelector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntitySelector.class)
public interface EntitySelectorAccessor {
	
	@Accessor("playerName")
	String getPlayerName();
	
}
