package com.solegendary.reignofnether.commands.rtsapi;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.commands.CommandsServerEvents;
import com.solegendary.reignofnether.commands.rtsapi.argument.BuildingArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.PlayerNameArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.UnitArgument;
import com.solegendary.reignofnether.player.PlayerClientboundPacket;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlayerCommands {
	
	private static final List<String> RESOURCE_NAMES = List.of("food", "wood", "ore", "emerald");
	
	public static void register(final LiteralArgumentBuilder<CommandSourceStack> commandBuilder) {
		commandBuilder
			.then(Commands.literal("player")
				.then(Commands.literal("owner")
					.then(Commands.literal("entity")
						.then(Commands.argument("targets", UnitArgument.units())
							.executes((ctx) -> withUnits(
								UnitArgument.getUnits(ctx, "targets", null),
								u -> u.setOwnerName(""),
								ctx,
								Component.literal("Removed owner of %d unit(s) successfully")
							))
							.then(Commands.argument("players", PlayerNameArgument.players())
								.executes((ctx) -> withUnits(
									UnitArgument.getUnits(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "players")),
									u -> u.setOwnerName(""),
									ctx,
									Component.literal("Removed owner of %d unit(s) successfully")
								))
								.then(Commands.argument("newOwnerName", PlayerNameArgument.player())
									.executes((ctx) -> withUnits(
										UnitArgument.getUnits(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "players")),
										u -> {
											try {
												u.setOwnerName(PlayerNameArgument.getPlayerName(ctx, "newOwnerName"));
											} catch (CommandSyntaxException ignored) {
											}
										},
										ctx,
										Component.literal("Set owner of %d unit(s) successfully")
									))
								)
							)
						)
					)
					.then(Commands.literal("building")
						.then(Commands.argument("targets", BuildingArgument.buildings())
							.executes((ctx) -> withBuildings(
								BuildingArgument.getBuildings(ctx, "targets", null),
								b -> b.ownerName = "",
								ctx,
								Component.literal("Removed owner of %d building(s) successfully")
							))
							.then(Commands.argument("players", PlayerNameArgument.players())
								.executes((ctx) -> withBuildings(
									BuildingArgument.getBuildings(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "players")),
									b -> b.ownerName = "",
									ctx,
									Component.literal("Removed owner of %d building(s) successfully")
								))
								.then(Commands.argument("newOwnerName", PlayerNameArgument.player())
									.executes((ctx) -> withBuildings(
										BuildingArgument.getBuildings(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "players")),
										b -> {
											try {
												b.ownerName = PlayerNameArgument.getPlayerName(ctx, "newOwnerName");
											} catch (CommandSyntaxException ignored) {
											}
										},
										ctx,
										Component.literal("Set owner of %d building(s) successfully")
									))
								)
							)
						)
					)
				)
				.then(Commands.literal("ally")
					.then(Commands.literal("set")
						.then(Commands.argument("player1", PlayerNameArgument.player())
							.then(Commands.argument("player2", PlayerNameArgument.player())
								.executes(ctx -> CommandsServerEvents.setAlliance(
									ctx,
									true,
									PlayerNameArgument.getPlayerName(ctx, "player1"),
									PlayerNameArgument.getPlayerName(ctx, "player2")
								))
							)
						)
					)
					.then(Commands.literal("cancel")
						.then(Commands.argument("player1", PlayerNameArgument.player())
							.then(Commands.argument("player2", PlayerNameArgument.player())
								.executes(ctx -> CommandsServerEvents.setAlliance(
									ctx,
									false,
									PlayerNameArgument.getPlayerName(ctx, "player1"),
									PlayerNameArgument.getPlayerName(ctx, "player2")
								))
							)
						)
					)
				)
				.then(Commands.literal("resources")
					.then(Commands.literal("add")
						.then(Commands.argument("resource", StringArgumentType.word())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RESOURCE_NAMES, builder))
							.then(Commands.argument("points", IntegerArgumentType.integer(0))
								.then(Commands.argument("player", PlayerNameArgument.player())
									.executes(ctx -> changeResources(
										ctx,
										StringArgumentType.getString(ctx, "resource"),
										IntegerArgumentType.getInteger(ctx, "points"),
										PlayerNameArgument.getPlayerName(ctx, "player"),
										false
									))
								)
							)
						)
					)
					.then(Commands.literal("remove")
						.then(Commands.argument("resource", StringArgumentType.word())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RESOURCE_NAMES, builder))
							.then(Commands.argument("points", IntegerArgumentType.integer(0))
								.then(Commands.argument("player", PlayerNameArgument.player())
									.executes(ctx -> changeResources(
										ctx,
										StringArgumentType.getString(ctx, "resource"),
										-IntegerArgumentType.getInteger(ctx, "points"),
										PlayerNameArgument.getPlayerName(ctx, "player"),
										false
									))
								)
							)
						)
					)
					.then(Commands.literal("set")
						.then(Commands.argument("resource", StringArgumentType.word())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RESOURCE_NAMES, builder))
							.then(Commands.argument("points", IntegerArgumentType.integer(0))
								.then(Commands.argument("player", PlayerNameArgument.player())
									.executes(ctx -> changeResources(
										ctx,
										StringArgumentType.getString(ctx, "resource"),
										IntegerArgumentType.getInteger(ctx, "points"),
										PlayerNameArgument.getPlayerName(ctx, "player"),
										true
									))
								)
							)
						)
					)
					.then(Commands.literal("get")
						.then(Commands.argument("player", PlayerNameArgument.player())
							.executes(ctx -> getResources(
								ctx,
								PlayerNameArgument.getPlayerName(ctx, "player")
							))
						)
					)
				)
				
				.then(Commands.literal("victory")
					.then(Commands.argument("player", PlayerNameArgument.player())
						.executes(ctx -> CommandsServerEvents.victoryPlayer(
							PlayerNameArgument.getPlayerName(ctx, "player"),
							""
						))
						.then(Commands.argument("reason", StringArgumentType.greedyString())
							.executes(ctx -> CommandsServerEvents.victoryPlayer(
								PlayerNameArgument.getPlayerName(ctx, "player"),
								StringArgumentType.getString(ctx, "reason")
							))
						)
					)
				)
				
				.then(Commands.literal("defeat")
					.then(Commands.argument("player", PlayerNameArgument.player())
						.executes(ctx -> CommandsServerEvents.defeatPlayer(
							PlayerNameArgument.getPlayerName(ctx, "player"),
							""
						))
						.then(Commands.argument("reason", StringArgumentType.greedyString())
							.executes(ctx -> CommandsServerEvents.defeatPlayer(
								PlayerNameArgument.getPlayerName(ctx, "player"),
								StringArgumentType.getString(ctx, "reason")
							))
						)
					)
				)
				
				.then(Commands.literal("research")
					.then(Commands.literal("add")
						.then(Commands.argument("researchItem", ResourceLocationArgument.id())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
								ReignOfNetherRegistries.PRODUCTION_ITEM.keySet().stream(), builder))
							.then(Commands.argument("player", PlayerNameArgument.player())
								.executes(ctx -> {
									String playerName = PlayerNameArgument.getPlayerName(ctx, "player");
									ResourceLocation researchItem = ResourceLocationArgument.getId(ctx, "researchItem");
									ResearchServerEvents.addResearch(playerName, researchItem);
									ResearchServerEvents.syncResearch(playerName);
									ctx.getSource().sendSuccess(
										() -> Component.literal("Added research '" + researchItem + "' for " + playerName),
										true
									);
									return 1;
								})
							)
						)
					)
					.then(Commands.literal("remove")
						.then(Commands.argument("researchItem", ResourceLocationArgument.id())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
								ReignOfNetherRegistries.PRODUCTION_ITEM.keySet().stream(), builder))
							.then(Commands.argument("player", PlayerNameArgument.player())
								.executes(ctx -> {
									String playerName = PlayerNameArgument.getPlayerName(ctx, "player");
									ResourceLocation researchItem = ResourceLocationArgument.getId(ctx, "researchItem");
									ResearchServerEvents.removeResearch(playerName, researchItem);
									ResearchServerEvents.syncResearch(playerName);
									ctx.getSource().sendSuccess(
										() -> Component.literal("Removed research '" + researchItem + "' for " + playerName),
										true
									);
									return 1;
								})
							)
						)
					)
					.then(Commands.literal("get")
						.then(Commands.argument("player", PlayerNameArgument.player())
							.executes(ctx -> {
								String playerName = PlayerNameArgument.getPlayerName(ctx, "player");
								List<String> owned = new ArrayList<>();
								for (ResourceLocation key : ReignOfNetherRegistries.PRODUCTION_ITEM.keySet()) {
									if (ResearchServerEvents.playerHasResearch(playerName, key)) {
										owned.add(key.toString());
									}
								}
								if (owned.isEmpty()) {
									ctx.getSource().sendSuccess(
										() -> Component.literal(playerName + " has no research"),
										false
									);
								} else {
									ctx.getSource().sendSuccess(
										() -> Component.literal(playerName + " research: " + String.join(", ", owned)),
										false
									);
								}
								return owned.size();
							})
						)
					)
				)
				
				.then(Commands.literal("camera")
					.then(Commands.argument("value", BoolArgumentType.bool())
						.then(Commands.argument("player", PlayerNameArgument.player())
							.executes(ctx -> {
								String playerName = PlayerNameArgument.getPlayerName(ctx, "player");
								boolean value = BoolArgumentType.getBool(ctx, "value");
								PlayerClientboundPacket.setRTSCamera(playerName, value);
								ctx.getSource().sendSuccess(
									() -> Component.literal("Set RTS camera to " + value + " for " + playerName),
									true
								);
								return 1;
							})
						)
					)
				)
				
				.then(Commands.literal("teammode")
					.then(Commands.argument("mode", StringArgumentType.word())
						.executes(ctx -> CommandsServerEvents.setStartingTeamsMode(ctx, StringArgumentType.getString(ctx, "mode")))
					)
				)
				.then(Commands.literal("starting-teams-mode")
					.then(Commands.argument("mode", StringArgumentType.word())
						.executes(ctx -> CommandsServerEvents.setStartingTeamsMode(ctx, StringArgumentType.getString(ctx, "mode")))
					)
				)
			);
	}
	
	
	private static int changeResources(
		CommandContext<CommandSourceStack> ctx,
		String resourceName,
		int amount,
		String playerName,
		boolean setMode
	) {
		ResourceName resource;
		try {
			resource = ResourceName.valueOf(resourceName.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			ctx.getSource().sendFailure(Component.literal("Unknown resource '" + resourceName + "'. Valid values: food, wood, ore, emerald"));
			return 0;
		}
		if (!PlayerServerEvents.isRTSPlayer(playerName)) {
			ctx.getSource().sendFailure(Component.literal("Unknown RTS player '" + playerName + "'"));
			return 0;
		}
		
		if (setMode) {
			for (Resources r : ResourcesServerEvents.resourcesList) {
				if (r.ownerName.equals(playerName)) {
					switch (resource) {
						case FOOD -> r.food = amount;
						case WOOD -> r.wood = amount;
						case ORE -> r.ore = amount;
						case EMERALD -> r.emerald = amount;
					}
					ResourcesClientboundPacket.syncResources(ResourcesServerEvents.resourcesList);
					ctx.getSource().sendSuccess(
						() -> Component.literal("Set " + resource.name().toLowerCase() + " to " + amount + " for " + playerName),
						true
					);
					return 1;
				}
			}
		} else {
			int food = resource == ResourceName.FOOD ? amount : 0;
			int wood = resource == ResourceName.WOOD ? amount : 0;
			int ore = resource == ResourceName.ORE ? amount : 0;
			int emerald = resource == ResourceName.EMERALD ? amount : 0;
			ResourcesServerEvents.addSubtractResources(new Resources(playerName, food, wood, ore, emerald));
			ctx.getSource().sendSuccess(
				() -> Component.literal("Changed " + resource.name().toLowerCase() + " by " + amount + " for " + playerName),
				true
			);
			return 1;
		}
		return 0;
	}
	
	private static int getResources(
		CommandContext<CommandSourceStack> ctx,
		String playerName
	) {
		if (!PlayerServerEvents.isRTSPlayer(playerName)) {
			ctx.getSource().sendFailure(Component.literal("Unknown RTS player '" + playerName + "'"));
			return 0;
		}
		for (Resources r : ResourcesServerEvents.resourcesList) {
			if (r.ownerName.equals(playerName)) {
				ctx.getSource().sendSuccess(
					() -> Component.literal(playerName + " resources - " +
							"Food: " + r.food +
							", Wood: " + r.wood +
							", Ore: " + r.ore +
							", Emerald: " + r.emerald),
					false
				);
				return 1;
			}
		}
		ctx.getSource().sendFailure(Component.literal("No resources found for '" + playerName + "'"));
		return 0;
	}
	
	
	private static int withUnits(List<? extends Unit> pUnits, Consumer<Unit> action, CommandContext<CommandSourceStack> ctx, Component msg) {
		for (Unit unit : pUnits) {
			action.accept(unit);
		}
		ctx.getSource().sendSuccess(() -> Component.literal(String.format(msg.getString(), pUnits.size())), true);
		return pUnits.size();
	}
	
	private static int withBuildings(List<? extends BuildingPlacement> pBuildings, Consumer<BuildingPlacement> action, CommandContext<CommandSourceStack> ctx, Component msg) {
		for (BuildingPlacement building : pBuildings) {
			action.accept(building);
		}
		ctx.getSource().sendSuccess(() -> Component.literal(String.format(msg.getString(), pBuildings.size())), true);
		return pBuildings.size();
	}
}
