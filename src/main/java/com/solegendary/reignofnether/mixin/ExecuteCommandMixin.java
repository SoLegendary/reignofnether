package com.solegendary.reignofnether.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.solegendary.reignofnether.commands.rtsapi.ExecuteCommands;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ExecuteCommand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExecuteCommand.class)
public class ExecuteCommandMixin {
	
	@Inject(method = "register", at = @At("RETURN"))
	private static void afterRegister(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, CallbackInfo ci) {
		CommandNode<CommandSourceStack> executeNode = dispatcher.getRoot().getChild("execute");
		if (executeNode instanceof LiteralCommandNode) {
			ExecuteCommands.register((LiteralCommandNode<CommandSourceStack>) executeNode);
		}
		
	}
}
