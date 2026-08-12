package com.solegendary.reignofnether.commands.rtsapi;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class RTSApiCommands {
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
//		LiteralArgumentBuilder<CommandSourceStack> ReignOfNetherBuilder = Commands.literal("reignofnether")
//			.requires(commandSourceStack -> commandSourceStack.hasPermission(2));
//		
//		BuildingCommands.register(ReignOfNetherBuilder);
//		
//		dispatcher.register(ReignOfNetherBuilder);
//		
//		LiteralArgumentBuilder<CommandSourceStack> ronBuilder = Commands.literal("ron")
//			.requires(commandSourceStack -> commandSourceStack.hasPermission(2));
//		
//		BuildingCommands.register(ronBuilder);
//		
//		dispatcher.register(ronBuilder);
		
		LiteralArgumentBuilder<CommandSourceStack> RTSApiBuilder = Commands.literal("rtsapi")
			.requires(commandSourceStack -> commandSourceStack.hasPermission(2));
		
		BuildingCommands.register(RTSApiBuilder);
		UnitCommands.register(RTSApiBuilder);
		PlayerCommands.register(RTSApiBuilder);
		
		dispatcher.register(RTSApiBuilder);
	}
	
}
