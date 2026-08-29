package com.solegendary.reignofnether.commands.rtsapi;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.commands.CommandsServerEvents;
import com.solegendary.reignofnether.commands.rtsapi.argument.PlayerNameArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.UnitArgument;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitActionItem;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.command.EnumArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UnitCommands {
	
	public static final List<String> UNIT_ACTIONS = List.of(
		UnitAction.NONE.name(), UnitAction.ATTACK.name(),
		UnitAction.ATTACK_BUILDING.name(), UnitAction.STOP.name(),
		UnitAction.HOLD.name(), UnitAction.MOVE.name(),
		UnitAction.GARRISON.name(), UnitAction.UNGARRISON.name(),
		UnitAction.ATTACK_MOVE.name(), UnitAction.FOLLOW.name(),
		UnitAction.BUILD_REPAIR.name(), UnitAction.FARM.name(),
		UnitAction.RETURN_RESOURCES.name(),
		UnitAction.RETURN_RESOURCES_TO_CLOSEST.name(),
		UnitAction.DELETE.name()
	);
	
	public static void register(final LiteralArgumentBuilder<CommandSourceStack> commandBuilder) {
		commandBuilder
			.then(Commands.literal("unit")
				.then(Commands.literal("summon")
					.then(Commands.argument("entity", ResourceLocationArgument.id())
						.suggests(
							(ctx, builder) -> SharedSuggestionProvider.suggestResource(
								net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getEntries().stream()
									.filter(e -> e.getKey().location().getNamespace().equals(com.solegendary.reignofnether.ReignOfNether.MOD_ID))
									.map(e -> e.getKey().location()),
								builder))
						.executes(ctx -> {
							Entity source = ctx.getSource().getEntity();
							String ownerName = "";
							if (source instanceof Player player)
								ownerName = player.getName().getString();
							else if (source instanceof Unit unit)
								ownerName = unit.getOwnerName();
							return CommandsServerEvents.summonEntity(ctx, ownerName,
								ResourceLocationArgument.getId(ctx, "entity"),
								BlockPos.containing(ctx.getSource().getPosition()), null);
						})
						.then(Commands.argument("ownerName", PlayerNameArgument.player())
							.executes(ctx -> CommandsServerEvents.summonEntity(ctx,
								PlayerNameArgument.getPlayerName(ctx, "ownerName"),
								ResourceLocationArgument.getId(ctx, "entity"),
								BlockPos.containing(ctx.getSource().getPosition()), null))
							.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(ctx -> CommandsServerEvents.summonEntity(ctx,
									PlayerNameArgument.getPlayerName(ctx, "ownerName"),
									ResourceLocationArgument.getId(ctx, "entity"),
									BlockPosArgument.getLoadedBlockPos(ctx, "pos"), null))
								.then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
									.executes(ctx -> CommandsServerEvents.summonEntity(ctx,
										PlayerNameArgument.getPlayerName(ctx, "ownerName"),
										ResourceLocationArgument.getId(ctx, "entity"),
										BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
										CompoundTagArgument.getCompoundTag(ctx, "nbt")))
								)
							)
						)
					)
				)
				.then(Commands.literal("owner")
					.then(setUnitOwnerTail())
					.then(Commands.argument("targets", UnitArgument.units())
						.executes((ctx) -> withUnits(
								UnitArgument.getUnits(ctx, "targets", null),
								u -> u.setOwnerName(""),
								ctx, Component.translatable("commands.reignofnether.unit.destroy.success")
								)
						)
						.then(Commands.argument("ownerName", PlayerNameArgument.players())
							.then(Commands.argument("newOwnerName", PlayerNameArgument.player())
								.executes((ctx) -> withUnits(
										b -> {
											try {
												b.setOwnerName(PlayerNameArgument.getPlayerName(ctx, "newOwnerName"));
											} catch (CommandSyntaxException ignored) {
											}
										},
										ctx,
										Component.translatable("commands.reignofnether.unit.owner.set.success")
									)
								)
							)
						)
					)
				)
				.then(Commands.literal("anchor")
					.then(Commands.literal("set")
						.then(Commands.argument("from", BlockPosArgument.blockPos())
							.then(Commands.argument("to", BlockPosArgument.blockPos())
								.then(Commands.argument("anchor", BlockPosArgument.blockPos())
									.executes(ctx -> CommandsServerEvents.setAnchor(
										ctx,
										BlockPosArgument.getLoadedBlockPos(ctx, "from"),
										BlockPosArgument.getLoadedBlockPos(ctx, "to"),
										BlockPosArgument.getLoadedBlockPos(ctx, "anchor")
									))
								)
							)
						)
						.then(Commands.argument("targets", UnitArgument.units())
							.then(Commands.argument("ownerName", PlayerNameArgument.players())
								.then(Commands.argument("anchor", BlockPosArgument.blockPos())
									.executes((ctx) -> withUnits(
										b -> {
											try {
												b.setAnchor(BlockPosArgument.getLoadedBlockPos(ctx, "anchor"));
											} catch (CommandSyntaxException ignored) {
											}
										},
										ctx,
										Component.translatable("commands.reignofnether.unit.owner.set.success")
									))
								)
							)
						)
					)
					.then(Commands.literal("remove")
						.then(Commands.argument("from", BlockPosArgument.blockPos())
							.then(Commands.argument("to", BlockPosArgument.blockPos())
								.executes(ctx -> CommandsServerEvents.removeAnchor(
									ctx,
									BlockPosArgument.getLoadedBlockPos(ctx, "from"),
									BlockPosArgument.getLoadedBlockPos(ctx, "to")
								))
							)
							.then(Commands.argument("targets", UnitArgument.units())
								.executes((ctx) -> {
									List<? extends Unit> units = UnitArgument.getUnits(ctx, "targets", null);
									int[] ids = units.stream().mapToInt(u -> ((LivingEntity) u).getId()).toArray();
									SandboxServer.removeAnchor(ids);
									ctx.getSource().sendSuccess(
										() -> Component.translatable("commands.reignofnether.unit.anchor.remove.success", ids.length), true);
									return ids.length;
								})
								.then(Commands.argument("ownerName", PlayerNameArgument.player())
									.executes((ctx) -> {
										List<? extends Unit> units = UnitArgument.getUnits(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName"));
										int[] ids = units.stream().mapToInt(u -> ((LivingEntity) u).getId()).toArray();
										SandboxServer.removeAnchor(ids);
										ctx.getSource().sendSuccess(
											() -> Component.translatable("commands.reignofnether.unit.anchor.remove.success", ids.length), true);
										return ids.length;
									})
								)
							)
						)
					)
				)
				.then(Commands.literal("action")
					.then(Commands.argument("selectFrom", BlockPosArgument.blockPos())
						.then(Commands.argument("selectTo", BlockPosArgument.blockPos())
							.then(Commands.argument("ownerName", StringArgumentType.string())
								.then(Commands.argument("action", StringArgumentType.word())
									.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
										UNIT_ACTIONS, builder))
									// (1) action only – no position or entity target
									.executes(ctx -> CommandsServerEvents.issueUnitAction(
										ctx,
										StringArgumentType.getString(ctx, "ownerName"),
										BlockPosArgument.getLoadedBlockPos(ctx, "selectFrom"),
										BlockPosArgument.getLoadedBlockPos(ctx, "selectTo"),
										StringArgumentType.getString(ctx, "action"),
										null, null
									))
									// (2) action + targetPos (MOVE, ATTACK_MOVE, GARRISON, BUILD_REPAIR, FARM, RETURN_RESOURCES, etc)
									.then(Commands.argument("targetPos", BlockPosArgument.blockPos())
										.executes(ctx -> CommandsServerEvents.issueUnitAction(
											ctx,
											StringArgumentType.getString(ctx, "ownerName"),
											BlockPosArgument.getLoadedBlockPos(ctx, "selectFrom"),
											BlockPosArgument.getLoadedBlockPos(ctx, "selectTo"),
											StringArgumentType.getString(ctx, "action"),
											BlockPosArgument.getLoadedBlockPos(ctx, "targetPos"),
											null
										))
									)
									// (3) action + target unit range (first unit found between targetFrom to targetTo)
									.then(Commands.argument("targetFrom", BlockPosArgument.blockPos())
										.then(Commands.argument("targetTo", BlockPosArgument.blockPos())
											.executes(ctx -> CommandsServerEvents.issueUnitAction(
												ctx,
												StringArgumentType.getString(ctx, "ownerName"),
												BlockPosArgument.getLoadedBlockPos(ctx, "selectFrom"),
												BlockPosArgument.getLoadedBlockPos(ctx, "selectTo"),
												StringArgumentType.getString(ctx, "action"),
												BlockPosArgument.getLoadedBlockPos(ctx, "targetFrom"),
												BlockPosArgument.getLoadedBlockPos(ctx, "targetTo")
											))
										)
									)
								)
							)
						)
					)
					.then(Commands.argument("targets", UnitArgument.units())
						.then(Commands.argument("ownerName", PlayerNameArgument.players())
							.then(Commands.argument("action", StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
									UNIT_ACTIONS, builder))
								.executes(ctx ->
									issueUnitAction(
										ctx,
										PlayerNameArgument.getPlayerName(ctx, "ownerName"),
										UnitArgument.getUnits(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName")),
										StringArgumentType.getString(ctx, "action"),
										null,
										null
									)
								)
								.then(Commands.argument("targetPos", BlockPosArgument.blockPos())
									.executes(ctx ->
										issueUnitAction(
											ctx,
											PlayerNameArgument.getPlayerName(ctx, "ownerName"),
											UnitArgument.getUnits(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName")),
											StringArgumentType.getString(ctx, "action"),
											BlockPosArgument.getLoadedBlockPos(ctx, ctx.getSource().getLevel(), "targetPos"),
											null
										)
									)
								)
								.then(Commands.argument("target", EntityArgument.entity())
									.executes(ctx ->
										issueUnitAction(
											ctx,
											PlayerNameArgument.getPlayerName(ctx, "ownerName"),
											UnitArgument.getUnits(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName")),
											StringArgumentType.getString(ctx, "action"),
											null,
											EntityArgument.getEntity(ctx, "target")
										)
									)
								)
							)
						)
					)
				)
				.then(Commands.literal("enemysearch")
					.then(Commands.argument("selectFrom", BlockPosArgument.blockPos())
						.then(Commands.argument("selectTo", BlockPosArgument.blockPos())
							.then(Commands.argument("ownerName", PlayerNameArgument.player())
								.then(Commands.argument("behaviour", StringArgumentType.word())
									.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
										List.of(
											EnemySearchBehaviour.NEAREST_ENEMY_BUILDING.name(),
											EnemySearchBehaviour.NEAREST_ENEMY_UNIT.name(),
											EnemySearchBehaviour.NEAREST_ENEMY_WORKER.name(),
											EnemySearchBehaviour.NONE.name()
										),
										builder
									))
									.executes(ctx -> CommandsServerEvents.setUnitSearchBehaviour(
										ctx,
										PlayerNameArgument.getPlayerName(ctx, "ownerName"),
										BlockPosArgument.getLoadedBlockPos(ctx, "selectFrom"),
										BlockPosArgument.getLoadedBlockPos(ctx, "selectTo"),
										StringArgumentType.getString(ctx, "behaviour")
									))
								)
							)
						)
					)
					.then(Commands.argument("targets", UnitArgument.units())
						.then(Commands.argument("ownerName", PlayerNameArgument.players())
							.then(Commands.argument("behaviour", EnumArgument.enumArgument(EnemySearchBehaviour.class))
								.executes((ctx) -> {
									List<? extends Unit> allUnits = UnitArgument.getUnits(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName"));
									List<Unit> attackers = new ArrayList<>();
									for (Unit unit : allUnits) {
										if (unit instanceof AttackerUnit) {
											attackers.add(unit);
										}
									}
									return withUnits(
										attackers,
										(unit) -> ((AttackerUnit) unit).setEnemySearchBehaviour(ctx.getArgument("behaviour", EnemySearchBehaviour.class)),
										ctx,
										Component.translatable("commands.reignofnether.unit.action.execute.success")
									);
								})
							)
						)
					)
				)
			);
	}
	
	private static ArgumentBuilder<CommandSourceStack, ?> setUnitOwnerTail() {
		return Commands.argument("from", BlockPosArgument.blockPos())
			.then(Commands.argument("to", BlockPosArgument.blockPos())
				.then(Commands.argument("ownerName", PlayerNameArgument.player())
					.executes(ctx -> CommandsServerEvents.setUnitOwner(
						ctx,
						PlayerNameArgument.getPlayerName(ctx, "ownerName"),
						"",
						BlockPosArgument.getLoadedBlockPos(ctx, "from"),
						BlockPosArgument.getLoadedBlockPos(ctx, "to")
					))
					.then(Commands.argument("newOwnerName", PlayerNameArgument.player())
						.executes(ctx -> CommandsServerEvents.setUnitOwner(
							ctx,
							PlayerNameArgument.getPlayerName(ctx, "ownerName"),
							PlayerNameArgument.getPlayerName(ctx, "newOwnerName"),
							BlockPosArgument.getLoadedBlockPos(ctx, "from"),
							BlockPosArgument.getLoadedBlockPos(ctx, "to")
						))
					)
					.executes(ctx -> CommandsServerEvents.setUnitOwner(
						ctx,
						"",
						"",
						BlockPosArgument.getLoadedBlockPos(ctx, "from"),
						BlockPosArgument.getLoadedBlockPos(ctx, "to")
					))
				)
			);
	}
	
	private static int issueUnitAction(CommandContext<CommandSourceStack> ctx, String ownerName, List<Unit> units, String actionName, BlockPos targetPos, Entity target) {
		UnitAction action;
		try {
			action = UnitAction.valueOf(actionName.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			ctx.getSource().sendFailure(Component.translatable("commands.reignofnether.action.unknown", actionName));
			return 0;
		}
		int[] unitIds = new int[units.size()];
		for (int i = 0; i < units.size(); i++) {
			unitIds[i] = ((Entity) units.get(i)).getId();
		}
		
		UnitActionItem item = new UnitActionItem(
			ownerName, action,
			target == null ? -1 : target.getId(),
			unitIds,
			targetPos != null ? targetPos : new BlockPos(0, 0, 0),
			new BlockPos(0, 0, 0)
		);
		item.action(ctx.getSource().getLevel());
		ctx.getSource().sendSuccess(
			() -> Component.translatable("commands.reignofnether.unit.action.issue.success", action.name().toLowerCase(), unitIds.length),  // TODO translatable action name
			true);
		return unitIds.length;
	}
	
	private static int withUnits(List<? extends Unit> pUnits, Consumer<Unit> action, CommandContext<CommandSourceStack> ctx, Component msg) {
		for (Unit unit : pUnits) {
			action.accept(unit);
		}
		ctx.getSource().sendSuccess(() -> Component.literal(String.format(msg.getString(), pUnits.size())), true);
		return pUnits.size();
	}
	
	private static int withUnits(Consumer<Unit> action, CommandContext<CommandSourceStack> ctx, Component msg) throws CommandSyntaxException {
		List<? extends Unit> pUnits = UnitArgument.getUnits(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName"));
		for (Unit unit : pUnits) {
			action.accept(unit);
		}
		ctx.getSource().sendSuccess(() -> Component.literal(String.format(msg.getString(), pUnits.size())), true);
		return pUnits.size();
	}
	
}

