package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.commands.rtsapi.argument.BuildingArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.PlayerNameArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.UnitArgument;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CommandArgumentRegistrar {
	
	public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES =
		DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, ReignOfNether.MOD_ID);
	
	public static final RegistryObject<ArgumentTypeInfo<BuildingArgument, ?>> BUILDING_ARGUMENT =
		COMMAND_ARGUMENT_TYPES.register(
			"building",
			BuildingArgument.Info::new
		);
	
	public static final RegistryObject<ArgumentTypeInfo<PlayerNameArgument, ?>> PLAYER_NAME_ARGUMENT =
		COMMAND_ARGUMENT_TYPES.register(
			"player_name"
			, PlayerNameArgument.Info::new
		);
	
	public static final RegistryObject<ArgumentTypeInfo<UnitArgument, ?>> UNIT_ARGUMENT =
		COMMAND_ARGUMENT_TYPES.register(
			"unit"
			, UnitArgument.Info::new
		);
	
	public static void init(FMLJavaModLoadingContext context) {
		COMMAND_ARGUMENT_TYPES.register(context.getModEventBus());
	}
}
