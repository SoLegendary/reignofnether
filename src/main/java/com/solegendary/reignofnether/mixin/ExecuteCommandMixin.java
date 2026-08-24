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
//			executeNode.addChild(
//				Commands.literal("owner")
//					.then(Commands.literal("building")
//						.then(Commands.argument("targets", BuildingArgument.buildings())
//							.fork(executeNode, (ctx) -> {
//								List<CommandSourceStack> list = Lists.newArrayList();
//								
//								for (BuildingPlacement building : BuildingArgument.getOptionalBuildings(ctx, "targets", null)) {
//									String ownerName = building.ownerName;
//									if (ownerName == null || ownerName.isEmpty())
//										continue;
//									
//									ServerPlayer ownerPlayer = ctx.getSource().getServer().getPlayerList().getPlayerByName(ownerName);
//									if (ownerPlayer != null) {
//										list.add(ctx.getSource().withEntity(ownerPlayer));
//									}
//								}
//								return list;
//							})
//						)
//					).build()
//			);
//			executeNode.addChild(
//				Commands.literal("owner")
//					.then(Commands.literal("entity")
//						.then(Commands.argument("targets", EntityArgument.entities())
//							.fork(executeNode, (ctx) -> {
//								List<CommandSourceStack> list = Lists.newArrayList();
//								
//								for (Entity entity : EntityArgument.getOptionalEntities(ctx, "targets")) {
//									if (entity instanceof Unit unit) {
//										String ownerName = unit.getOwnerName();
//										if (ownerName == null || ownerName.isEmpty())
//											continue;
//										
//										ServerPlayer ownerPlayer = ctx.getSource().getServer().getPlayerList().getPlayerByName(ownerName);
//										if (ownerPlayer != null) {
//											list.add(ctx.getSource().withEntity(ownerPlayer));
//										}
//									}
//								}
//								return list;
//							})
//						)
//					).build()
//			);
//			executeNode.addChild(
//				Commands.literal("allies")
//					.then(Commands.argument("target", PlayerNameArgument.player())
//						.fork(executeNode, (ctx) -> {
//							List<CommandSourceStack> list = Lists.newArrayList();
//							
//							String playerName = PlayerNameArgument.getPlayerName(ctx, "target");
//							
//							for (String allyName : AlliancesServerEvents.getAllAllies(playerName)) {
//								ServerPlayer allyPlayer = ctx.getSource().getServer().getPlayerList().getPlayerByName(allyName);
//								if (allyPlayer != null) {
//									list.add(ctx.getSource().withEntity(allyPlayer));
//								}
//							}
//							return list;
//						})
//					).build()
//			);
//			executeNode.addChild(
//				Commands.literal("enemies")
//					.then(Commands.argument("target", PlayerNameArgument.player())
//						.fork(executeNode, (ctx) -> {
//							List<CommandSourceStack> list = Lists.newArrayList();
//							
//							String playerName = PlayerNameArgument.getPlayerName(ctx, "target");
//							
//							for (ServerPlayer serverPlayer : ctx.getSource().getServer().getPlayerList().getPlayers()) {
//								if (PlayerServerEvents.isRTSPlayer(serverPlayer.getName().getString()) && !AlliancesServerEvents.isAlliedOrOwned(playerName, serverPlayer.getName().getString())) {
//									list.add(ctx.getSource().withEntity(serverPlayer));
//								}
//							}
//							return list;
//						})
//					).build()
//			);
//			executeNode.addChild(
//				Commands.literal("building")
//					.then(Commands.argument("targets", BuildingArgument.buildings())
//						.then(Commands.argument("ownerName", PlayerNameArgument.player())
//							.fork(executeNode, (ctx) -> {
//								List<CommandSourceStack> list = Lists.newArrayList();
//								
//								for (BuildingPlacement building : BuildingArgument.getBuildings(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName"))) {
//									list.add(ctx.getSource().withPosition(building.centrePos.getCenter()));
//									
//								}
//								return list;
//							})
//						)
//					)
//					.build()
//			);
		}
		
	}
}
