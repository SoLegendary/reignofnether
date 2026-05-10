package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.commands.argument.BuildingArgument;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CommandArgumentRegistrar {
	
	public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES =
		DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, ReignOfNether.MOD_ID);
	
	public static final RegistryObject<ArgumentTypeInfo<BuildingArgument, ?>> BUILDING_ARG =
		COMMAND_ARGUMENT_TYPES.register(
			"building",
			BuildingArgument.Info::new
		);
	
	public static void init(FMLJavaModLoadingContext context) {
		COMMAND_ARGUMENT_TYPES.register(context.getModEventBus());
	}
}
