package com.solegendary.reignofnether.commands.rtsapi;

import com.google.common.collect.Sets;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingCommand;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.commands.CommandsServerEvents;
import com.solegendary.reignofnether.commands.rtsapi.argument.BuildingArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.PlayerNameArgument;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BuildingCommands {
	
	public static final SuggestionProvider<CommandSourceStack> BUILDINGS = (ctx, builder) -> {
		List<ResourceLocation> locations = Stream.concat(
			ReignOfNetherRegistries.BUILDING.stream(),
			CustomBuildingServerEvents.customBuildings.stream()
		).map(building -> {
			ResourceLocation id = ReignOfNetherRegistries.BUILDING.getKey(building);
			return id != null ? id : ResourceLocation.fromNamespaceAndPath(
				"custom",
				building.name.toLowerCase().replace(' ', '_'));
		}).collect(Collectors.toList());
		
		SharedSuggestionProvider.suggestResource(locations, builder,
			id -> id,
			id -> Component.translatable(
				"buildings." + id.getNamespace() + "." + id.getPath(),
				id.getPath()
			)
		);
		builder.suggest("reignofnether:");
		if (!CustomBuildingServerEvents.customBuildings.isEmpty())
			builder.suggest("custom:");
		return builder.buildFuture();
	};
	
	public static void register(final LiteralArgumentBuilder<CommandSourceStack> commandBuilder) {
		commandBuilder
			.then(Commands.literal("building")
				.then(Commands.literal("place")
					.then(Commands.argument("buildingName", ResourceLocationArgument.id())
						.suggests(BUILDINGS)
						.then(placeBuildingTail(ctx -> ""))
						.then(placeBuildingTail(PlayerNameArgument.player(), ctx -> PlayerNameArgument.getPlayerName(ctx, "ownerName")))
						.executes(ctx -> {
								Entity source = ctx.getSource().getEntity();
								String ownerName = "";
								if (source instanceof Player player)
									ownerName = player.getName().getString();
								else if (source instanceof Unit unit)
									ownerName = unit.getOwnerName();
								return CommandsServerEvents.placeBuilding(
									ctx,
									ResourceLocationArgument.getId(ctx, "buildingName").toString(),
									ownerName,
									true,
									Objects.requireNonNull(ctx.getSource().getEntity()).blockPosition(),
									Rotation.NONE
								);
							}
						)
					)
				)
				.then(Commands.literal("destroy")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(ctx -> CommandsServerEvents.destroyBuildingsAt(BlockPosArgument.getLoadedBlockPos(ctx, "pos"), ctx.getSource()))
					)
					.then(Commands.argument("targets", BuildingArgument.buildings())
						.executes(
							(ctx) -> withBuildings(
								BuildingArgument.getBuildings(ctx, "targets", null),
								b -> b.destroy(ctx.getSource().getLevel()), ctx,
								Component.literal("Destroy %d building(s) successfully")
							)
						)//TODO: replace with translation
						.then(Commands.argument("ownerName", PlayerNameArgument.players())
							.executes(
								(ctx) -> withBuildings(
									b -> b.destroy(ctx.getSource().getLevel()),
									ctx,
									Component.literal("Destroy %d building(s) successfully")
								)
							)
							.then(Commands.argument("preserved", BoolArgumentType.bool())
								.executes(
									(ctx) -> withBuildings(
										b -> SandboxServer.removeBuilding(b.originPos),
										ctx,
										Component.literal("Destroy %d building(s) without broke blocks successfully")
									)
								)
							)
						)
					)
				)
				.then(Commands.literal("owner")
					.then(Commands.argument("from", BlockPosArgument.blockPos())
						.then(Commands.argument("to", BlockPosArgument.blockPos())
							.executes(
								(ctx) -> CommandsServerEvents.setBuildingOwner(
									ctx,
									"",
									BlockPosArgument.getLoadedBlockPos(ctx, "from"),
									BlockPosArgument.getLoadedBlockPos(ctx, "to")
								)
							)
							.then(Commands.argument("ownerName", PlayerNameArgument.player())
								.executes(
									(ctx) -> CommandsServerEvents.setBuildingOwner(
										ctx,
										StringArgumentType.getString(ctx, "ownerName"),
										BlockPosArgument.getLoadedBlockPos(ctx, "from"),
										BlockPosArgument.getLoadedBlockPos(ctx, "to")
									)
								)
							)
						)
					)
					.then(Commands.argument("targets", BuildingArgument.buildings())
						.executes((ctx) -> withBuildings(BuildingArgument.getBuildings(ctx, "targets", null), b -> b.ownerName = "", ctx, Component.literal("Destroy %d building(s) successfully")))
						.then(Commands.argument("ownerName", PlayerNameArgument.player())
							.then(Commands.argument("newOwnerName", PlayerNameArgument.player())
								.executes((ctx) -> withBuildings(
									b -> {
										try {
											b.ownerName = PlayerNameArgument.getPlayerName(ctx, "newOwnerName");
										} catch (CommandSyntaxException ignored) {
										}
									},
									ctx,
									Component.literal("Destroy %d building(s) successfully")
								)))
						)
					)
				)
				.then(Commands.literal("tag")
					.then(Commands.argument("targets", BuildingArgument.buildings())
						.then(Commands.argument("ownerName", PlayerNameArgument.players())
							.then(Commands.literal("add")
								.then(Commands.argument("name", StringArgumentType.word())
									.executes(
										(ctx) -> withBuildings(
											b -> b.addTag(StringArgumentType.getString(ctx, "name")), ctx,
											Component.literal("Add Tag to %d building(s) successfully"))
									)
								)
							)
							.then(Commands.literal("remove")
								.then(Commands.argument("name", StringArgumentType.word())
									.suggests(
										(ctx, builder) -> SharedSuggestionProvider.suggest(getTags(
											BuildingArgument.getBuildings(
												ctx, "targets",
												PlayerNameArgument.getPlayerName(ctx, "ownerName")
											)
										), builder)
									)
									.executes(
										(ctx) -> withBuildings(
											b -> b.removeTag(StringArgumentType.getString(ctx, "name")),
											ctx,
											Component.literal("Remove Tag to %d building(s) successfully")))
								)
							)
							.then(Commands.literal("list")
								.executes((ctx) -> listTags(ctx.getSource(), BuildingArgument.getBuildings(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName"))))
							)
						)
					)
				)
				.then(Commands.literal("hurt")
					.then(Commands.argument("targets", BuildingArgument.buildings())
						.then(Commands.argument("ownerName", PlayerNameArgument.players())
							.then(Commands.argument("points", IntegerArgumentType.integer())
								.executes((ctx) -> withBuildings(
									b -> b.destroyRandomBlocks(IntegerArgumentType.getInteger(ctx, "points")),
									ctx,
									Component.literal("Hurt %d building(s) successfully"))
								)
							)
						)
					)
				)
				.then(Commands.literal("heal")
					.then(Commands.argument("targets", BuildingArgument.buildings())
						.then(Commands.argument("ownerName", PlayerNameArgument.players())
							.then(Commands.argument("points", IntegerArgumentType.integer())
								.executes((ctx) -> withBuildings(
									b -> {
										boolean isBuilt = b.isBuilt;
										b.isBuilt = false;
										for (int i = 0; i < IntegerArgumentType.getInteger(ctx, "points"); i++) {
											try {
												b.queueNextBlock(ctx.getSource().getLevel(), PlayerNameArgument.getPlayerName(ctx, "ownerName"));
											} catch (CommandSyntaxException ignored) {
											}
										}
										b.isBuilt = isBuilt;
										
									},
									ctx,
									Component.literal("Hurt %d building(s) successfully"))
								)
							)
						)
					)
				)
				.then(Commands.literal("command")
					.then(Commands.argument("targets", BuildingArgument.buildings())
						.then(Commands.argument("ownerName", PlayerNameArgument.players())
							.then(Commands.literal("add")
								.then(Commands.argument("commandStr", StringArgumentType.string())
									.then(Commands.argument("condition", StringArgumentType.word())
										.then(Commands.argument("tickCooldownMax", IntegerArgumentType.integer(1))
											.executes((ctx) -> withBuildings(
													b -> b.addCommand(
														StringArgumentType.getString(ctx, "commandStr"),
														StringArgumentType.getString(ctx, "condition"),
														IntegerArgumentType.getInteger(ctx, "tickCooldownMax"),
														IntegerArgumentType.getInteger(ctx, "tickCooldownMax")
													),
													ctx,
													Component.literal("Add Command to %d building(s) successfully")
												)
											)
											.then(Commands.argument("tickCooldown", IntegerArgumentType.integer(0))
												.executes((ctx) -> withBuildings(
														b -> b.addCommand(
															StringArgumentType.getString(ctx, "commandStr"),
															StringArgumentType.getString(ctx, "condition"),
															IntegerArgumentType.getInteger(ctx, "tickCooldown"),
															IntegerArgumentType.getInteger(ctx, "tickCooldownMax")
														),
														ctx,
														Component.literal("Add Command to %d building(s) successfully")
													)
												)
											)
										)
										.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
											List.of("ON_BUILD_COMPLETE", "ON_DESTROY", "ON_DAMAGE_TAKEN", "ON_CAPTURE",
												"OFF_COOLDOWN_IF_COMPLETE", "OFF_COOLDOWN_IF_GARRISONED", "NONE"),
											builder
										))
										.executes((ctx) -> withBuildings(
												b -> b.addCommand(
													StringArgumentType.getString(ctx, "commandStr"),
													StringArgumentType.getString(ctx, "condition"),
													100,
													100
												),
												ctx,
												Component.literal("Add Command to %d building(s) successfully")
											)
										)
									)
								)
							)
							.then(Commands.literal("remove")
								.then(Commands.argument("index", IntegerArgumentType.integer(0))
									.suggests((ctx, builder) -> {
										List<? extends BuildingPlacement> buildings = BuildingArgument.getOptionalBuildings(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName"));
										
										int maxCommands = buildings.stream()
											.mapToInt(b -> b.commands.size())
											.max()
											.orElse(0);
										
										List<String> suggestions = new ArrayList<>();
										for (int i = 0; i < maxCommands; i++) {
											suggestions.add(String.valueOf(i));
										}
										return SharedSuggestionProvider.suggest(suggestions, builder);
									})
									.executes((ctx) -> {
										List<? extends BuildingPlacement> buildings = BuildingArgument.getOptionalBuildings(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName"));
										for (BuildingPlacement building : buildings) {
											int index = IntegerArgumentType.getInteger(ctx, "index");
											if (index <= building.commands.size()) {
												building.removeCommand(index);
												ctx.getSource().sendSuccess(
													() -> Component.literal(String.format(
															"Delete command (%d", index))
														.withStyle(Style.EMPTY.withBold(true))
														.append(Component.literal(String.format(
															" | %s | %s | %d) successfully",
															building.commands.get(index).commandStr,
															building.commands.get(index).condition,
															building.commands.get(index).tickCooldownMax
															))
															.withStyle(Style.EMPTY.withBold(false))
														), true);
											}
										}
										return 1;
									})
								)
								.then(Commands.literal("*")
									.executes((ctx) -> withBuildings(
										b -> {
											for (int i = 0; i < b.commands.size(); i++) {
												b.removeCommand(i);
											}
										},
										ctx,
										Component.literal("Delete all commands of %d buildings successfully")
									))
								)
							)
							.then(Commands.literal("set")
								.then(Commands.argument("index", IntegerArgumentType.integer(0))
									.suggests((ctx, builder) -> {
										List<? extends BuildingPlacement> buildings = BuildingArgument.getOptionalBuildings(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName"));
										int maxCommands = buildings.stream()
											.mapToInt(b -> b.commands.size())
											.max()
											.orElse(0);
										
										List<String> suggestions = new ArrayList<>();
										for (int i = 0; i < maxCommands; i++) {
											suggestions.add(String.valueOf(i));
										}
										return SharedSuggestionProvider.suggest(suggestions, builder);
									})
									.then(Commands.literal("command")
										.then(Commands.argument("value", StringArgumentType.string())
											.executes((ctx) -> withBuildings(
												b -> {
													String value = StringArgumentType.getString(ctx, "value");
													if (value.startsWith("/"))
														value = value.substring(1);
													b.setCommandText(IntegerArgumentType.getInteger(ctx, "index"), value);
												},
												ctx,
												Component.empty()
											))
										)
									)
									.then(Commands.literal("cooldown")
										.then(Commands.argument("value", IntegerArgumentType.integer(0))
											.executes((ctx) -> withBuildings(
												b -> b.setCommandCooldownTicks(IntegerArgumentType.getInteger(ctx, "index"), IntegerArgumentType.getInteger(ctx, "value")),
												ctx,
												Component.empty()
											))
										)
									)
									.then(Commands.literal("trigger")
										.then(Commands.argument("value", StringArgumentType.word())
											.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
												List.of("ON_BUILD_COMPLETE", "ON_DESTROY", "ON_DAMAGE_TAKEN", "ON_CAPTURE",
													"OFF_COOLDOWN_IF_COMPLETE", "OFF_COOLDOWN_IF_GARRISONED", "NONE"),
												builder
											))
											.executes((ctx) -> withBuildings(
												b -> b.setCommandTrigger(IntegerArgumentType.getInteger(ctx, "index"), StringArgumentType.getString(ctx, "value")),
												ctx,
												Component.empty()
											))
										)
									)
								)
							)
							.then(Commands.literal("list")
								.executes((ctx) -> listCommands(ctx.getSource(), BuildingArgument.getBuildings(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName"))))
							)
						)
					)
				)
			);
	}
	
	private static Collection<String> getTags(Collection<? extends BuildingPlacement> pBuildings) {
		Set<String> set = Sets.newHashSet();
		
		for (BuildingPlacement building : pBuildings) {
			set.addAll(building.getTags());
		}
		
		return set;
	}
	
	private static int listTags(CommandSourceStack pSource, Collection<? extends BuildingPlacement> pBuildings) {
		Set<String> set = Sets.newHashSet();
		
		for (BuildingPlacement building : pBuildings) {
			set.addAll(building.getTags());
		}
		
		if (set.isEmpty()) {
			pSource.sendSuccess(() -> Component.translatable("commands.tag.list.multiple.empty", pBuildings.size()), false);
		} else {
			pSource.sendSuccess(() -> Component.translatable("commands.tag.list.multiple.success", pBuildings.size(), set.size(), ComponentUtils.formatList(set)), false);
		}
		
		return set.size();
	}
	
	private static int listCommands(CommandSourceStack pSource, Collection<? extends BuildingPlacement> pBuildings) {
		
		for (BuildingPlacement building : pBuildings) {
			if (building.commands.isEmpty()) {
				pSource.sendSuccess(() -> Component.translatable("commands.command.list.multiple.empty", pBuildings.size()), false);
			} else {
				pSource.sendSuccess(() -> Component.translatable("commands.tag.list.multiple.success", pBuildings.size(), building.commands.size()), false);
				for (BuildingCommand command : building.commands) {
					pSource.sendSuccess(
					() -> Component.literal(String.format(
							"%d", building.commands.indexOf(command)))
						.withStyle(Style.EMPTY.withBold(true))
						.append(Component.literal(String.format(" : %s | %s | %d",
							command.commandStr,
							command.condition,
							command.tickCooldownMax
						)).withStyle(Style.EMPTY.withBold(false))), true);
				}
			}
		}
		
		return pBuildings.size();
	}
	
	private static int withBuildings(List<? extends BuildingPlacement> pBuildings, Consumer<BuildingPlacement> action, CommandContext<CommandSourceStack> ctx, Component msg) {
		for (BuildingPlacement building : pBuildings) {
			action.accept(building);
		}
		ctx.getSource().sendSuccess(() -> Component.literal(String.format(msg.getString(), pBuildings.size())), true);
		return pBuildings.size();
	}
	
	private static int withBuildings(Consumer<BuildingPlacement> action, CommandContext<CommandSourceStack> ctx, Component msg) throws CommandSyntaxException {
		List<? extends BuildingPlacement> pBuildings = BuildingArgument.getBuildings(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName"));
		for (BuildingPlacement building : pBuildings) {
			action.accept(building);
		}
		ctx.getSource().sendSuccess(() -> Component.literal(String.format(msg.getString(), pBuildings.size())), true);
		return pBuildings.size();
	}
	
	private static <T> ArgumentBuilder<CommandSourceStack, ?> placeBuildingTail(ArgumentType<T> argumentType, CommandsServerEvents.NameResolver ownerResolver) {
		return Commands.argument("ownerName", argumentType)
			.executes(ctx -> CommandsServerEvents.placeBuilding(
				ctx,
				ResourceLocationArgument.getId(ctx, "buildingName").toString(),
				ownerResolver.resolve(ctx),
				true,
				Objects.requireNonNull(ctx.getSource().getEntity()).blockPosition(),
				Rotation.NONE))
			.then(placeBuildingTail(ownerResolver));
	}
	
	private static ArgumentBuilder<CommandSourceStack, ?> placeBuildingTail(CommandsServerEvents.NameResolver ownerResolver) {
		return Commands.argument("pos", BlockPosArgument.blockPos())
			.executes(ctx -> CommandsServerEvents.placeBuilding(
				ctx,
				ResourceLocationArgument.getId(ctx, "buildingName").toString(),
				ownerResolver.resolve(ctx),
				true,
				BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
				Rotation.NONE))
			.then(Commands.argument("autoBuild", BoolArgumentType.bool())
				.executes(ctx -> CommandsServerEvents.placeBuilding(
					ctx,
					ResourceLocationArgument.getId(ctx, "buildingName").toString(),
					ownerResolver.resolve(ctx),
					BoolArgumentType.getBool(ctx, "autoBuild"),
					BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
					Rotation.NONE
				))
				// with rotation
				.then(Commands.argument("rotation", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
						List.of("0", "90", "180", "270"),
						builder
					))
					.executes(ctx -> CommandsServerEvents.placeBuilding(
						ctx,
						ResourceLocationArgument.getId(ctx, "buildingName").toString(),
						ownerResolver.resolve(ctx),
						BoolArgumentType.getBool(ctx, "autoBuild"),
						BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
						CommandsServerEvents.parseRotation(StringArgumentType.getString(ctx, "rotation"))
					))
				)
			);
	}
}