package com.solegendary.reignofnether.mixin;

import com.google.common.collect.ImmutableList;
import com.solegendary.reignofnether.commands.rtsapi.data_accessor.BuildingDataAccessor;

import net.minecraft.server.commands.data.DataCommands;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Mixin(DataCommands.class)
public class DataCommandsMixin {
	
	@SuppressWarnings("unchecked")
	@Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;"), remap = false)
	private static <E> ImmutableList<Function<String, DataCommands.DataProvider>> addProvider(E e1, E e2, E e3) {
		return ImmutableList.of(
			(Function<String, DataCommands.DataProvider>) e1,
			(Function<String, DataCommands.DataProvider>) e2,
			(Function<String, DataCommands.DataProvider>) e3,
			BuildingDataAccessor.PROVIDER
		);
	}
	
}
