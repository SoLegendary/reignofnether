package com.solegendary.reignofnether.commands.rtsapi;

import com.google.common.collect.Lists;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.commands.rtsapi.argument.BuildingArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.PlayerNameArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.UnitArgument;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.rtsmap.RonFiles;
import com.solegendary.reignofnether.rtsmap.RonZones;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.goals.RangedAttackBuildingGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.List;
import javax.annotation.Nullable;

public class ExecuteCommands {
	
	public static void register(final LiteralCommandNode<CommandSourceStack> executeNode) {
		// execute if|unless rts <check>: the RTS state a map script branches on
		for (boolean want : new boolean[] { true, false }) {
			CommandNode<CommandSourceStack> node = executeNode.getChild(want ? "if" : "unless");
			if (node != null)
				node.addChild(rtsChecks(executeNode, want));
		}
		executeNode.addChild(
			Commands.literal("rts-related")
				.then(Commands.literal("owner")
					.then(Commands.literal("building")
						.fork(executeNode, (ctx) -> {
							List<CommandSourceStack> list = Lists.newArrayList();
							
							BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
							BuildingPlacement building = BuildingUtils.findBuilding(false, pos);
							if (building != null && building.ownerName != null && !building.ownerName.isEmpty())
								list.add(RonFiles.asOwner(ctx.getSource(), building.ownerName));
							return list;
						})
					)
					.then(Commands.literal("entity")
						.fork(executeNode, (ctx) -> {
							List<CommandSourceStack> list = Lists.newArrayList();
							
							Entity entity = ctx.getSource().getEntity();
							if (entity instanceof Unit unit && unit.getOwnerName() != null && !unit.getOwnerName().isEmpty())
								list.add(RonFiles.asOwner(ctx.getSource(), unit.getOwnerName()));
							return list;
						})
					)
				)
				.then(Commands.literal("allies")
					.fork(executeNode, (ctx) -> {
						List<CommandSourceStack> list = Lists.newArrayList();
						
						String owner = PlayerNameArgument.sourceOwner(ctx.getSource());
						if (!owner.isEmpty())
							for (String allyName : AlliancesServerEvents.getAllAllies(owner))
								list.add(RonFiles.asOwner(ctx.getSource(), allyName));
						return list;
					})
				)
				.then(Commands.literal("enemies")
					.fork(executeNode, (ctx) -> {
						List<CommandSourceStack> list = Lists.newArrayList();
						
						String owner = PlayerNameArgument.sourceOwner(ctx.getSource());
						if (!owner.isEmpty())
							synchronized (PlayerServerEvents.rtsPlayers) {
								for (RTSPlayer rtsPlayer : PlayerServerEvents.rtsPlayers)
									if (!AlliancesServerEvents.isAlliedOrOwned(owner, rtsPlayer.name))
										list.add(RonFiles.asOwner(ctx.getSource(), rtsPlayer.name));
							}
						return list;
					})
				)
				.then(Commands.literal("attacker")
					.then(Commands.literal("entity")
						.fork(executeNode, (ctx) -> {
							List<CommandSourceStack> list = Lists.newArrayList();
							
							Entity entity = ctx.getSource().getEntity();
							if (entity instanceof Unit unit) {
								DamageSource source = ((Mob) unit).getLastDamageSource();
								if (source != null) {
									Entity attacker = source.getEntity();
									if (attacker != null) {
										list.add(ctx.getSource().withEntity(attacker));
									}
								}
							}
							return list;
						})
					)
					.then(Commands.literal("building")
						.fork(executeNode, (ctx) -> {
							List<CommandSourceStack> list = Lists.newArrayList();
							
							BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
							BuildingPlacement building = BuildingUtils.findBuilding(false, pos);
							if (building != null) {
								Entity attacker = building.lastAttacker;
								if (attacker != null) {
									list.add(ctx.getSource().withEntity(attacker));
								}
							}
							return list;
						})
					)
				)
				.then(Commands.literal("target")
					.then(Commands.literal("building")
						.fork(executeNode, (ctx) -> {
							List<CommandSourceStack> list = Lists.newArrayList();
							
							Entity entity = ctx.getSource().getEntity();
							if (entity instanceof AttackerUnit unit) {
								Goal attackBuildingGoal = unit.getAttackBuildingGoal();
								if (attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rangedAttackBuildingGoal)
									list.add(ctx.getSource().withPosition(rangedAttackBuildingGoal.getBuildingTarget().originPos.getCenter()));
								else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal meleeAttackBuildingGoal)
									list.add(ctx.getSource().withPosition(meleeAttackBuildingGoal.getBuildingTarget().originPos.getCenter()));
							}
							return list;
						})
					)
					.then(Commands.literal("entity")
						.fork(executeNode, (ctx) -> {
							List<CommandSourceStack> list = Lists.newArrayList();
							
							Entity entity = ctx.getSource().getEntity();
							if (entity instanceof Targeting targeting) {
								Entity target = targeting.getTarget();
								if (target != null)
									list.add(ctx.getSource().withEntity(target));
							}
							return list;
						})
					)
				)
				.build()
		);
		executeNode.addChild(
			Commands.literal("building")
				.then(Commands.argument("targets", BuildingArgument.buildings())
					.then(Commands.argument("ownerName", PlayerNameArgument.players())
						.fork(executeNode, (ctx) -> {
							List<CommandSourceStack> list = Lists.newArrayList();
							
							for (BuildingPlacement building : BuildingArgument.getBuildings(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName"))) {
								list.add(ctx.getSource().withPosition(building.centrePos.getCenter()));
								
							}
							return list;
						})
					)
				)
				.build()
		);
		executeNode.addChild(
			Commands.literal("unit")
				.then(Commands.argument("targets", UnitArgument.units())
					.then(Commands.argument("ownerName", PlayerNameArgument.players())
						.fork(executeNode, (ctx) -> {
							List<CommandSourceStack> list = Lists.newArrayList();
							
							for (Unit unit : UnitArgument.getUnits(ctx, "targets", PlayerNameArgument.getPlayerName(ctx, "ownerName"))) {
								list.add(ctx.getSource().withEntity((Entity) unit));
								
							}
							return list;
						})
					)
				)
				.build()
		);
	}

	private interface Check {
		boolean test(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
	}

	// the source goes on when the check comes out as wanted, else the line stops there
	private static RedirectModifier<CommandSourceStack> when(boolean want, Check check) {
		return ctx -> check.test(ctx) == want ? List.of(ctx.getSource()) : List.of();
	}

	// rts resources <food|wood|ore> <at least> <player>, rts units|buildings <at least> <player>, rts zone <zone> <player>
	private static LiteralCommandNode<CommandSourceStack> rtsChecks(LiteralCommandNode<CommandSourceStack> executeNode, boolean want) {
		return Commands.literal("rts")
			.then(Commands.literal("resources")
				.then(Commands.argument("resource", StringArgumentType.word())
					.suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("food", "wood", "ore"), b))
					.then(Commands.argument("amount", IntegerArgumentType.integer(0))
						.then(Commands.argument("player", PlayerNameArgument.player())
							.fork(executeNode, when(want, ctx -> resources(PlayerNameArgument.getPlayerName(ctx, "player"),
									StringArgumentType.getString(ctx, "resource"))
									>= IntegerArgumentType.getInteger(ctx, "amount")))))))
			.then(Commands.literal("units")
				.then(Commands.argument("amount", IntegerArgumentType.integer(0))
					.then(Commands.argument("player", PlayerNameArgument.player())
						.fork(executeNode, when(want, ctx -> {
							String owner = PlayerNameArgument.getPlayerName(ctx, "player");
							return UnitServerEvents.getAllUnits().stream()
									.filter(e -> e instanceof Unit u && owner != null && owner.equals(u.getOwnerName())).count()
									>= IntegerArgumentType.getInteger(ctx, "amount");
						})))))
			.then(Commands.literal("buildings")
				.then(Commands.argument("amount", IntegerArgumentType.integer(0))
					.then(Commands.argument("player", PlayerNameArgument.player())
						.fork(executeNode, when(want, ctx -> {
							String owner = PlayerNameArgument.getPlayerName(ctx, "player");
							return BuildingServerEvents.getBuildings().stream()
									.filter(b -> owner != null && owner.equals(b.ownerName)).count()
									>= IntegerArgumentType.getInteger(ctx, "amount");
						})))))
			.then(Commands.literal("zone")
				.then(Commands.argument("zone", StringArgumentType.word())
					.suggests((ctx, b) -> SharedSuggestionProvider.suggest(RonZones.names(), b))
					.then(Commands.argument("player", PlayerNameArgument.player())
						.fork(executeNode, when(want, ctx -> RonZones.has(
								StringArgumentType.getString(ctx, "zone"), PlayerNameArgument.getPlayerName(ctx, "player")))))))
			.build();
	}

	private static int resources(@Nullable String owner, String resource) {
		for (Resources r : ResourcesServerEvents.resourcesList)
			if (r.ownerName.equals(owner))
				return switch (resource) {
					case "food" -> r.food;
					case "wood" -> r.wood;
					case "ore" -> r.ore;
					default -> 0;
				};
		return 0;
	}
}
