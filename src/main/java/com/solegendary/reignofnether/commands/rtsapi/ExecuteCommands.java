package com.solegendary.reignofnether.commands.rtsapi;

import com.google.common.collect.Lists;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.commands.rtsapi.argument.BuildingArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.PlayerNameArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.UnitArgument;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.goals.RangedAttackBuildingGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.List;

public class ExecuteCommands {
	
	public static void register(final LiteralCommandNode<CommandSourceStack> executeNode) {
		executeNode.addChild(
			Commands.literal("rts-related")
				.then(Commands.literal("owner")
					.then(Commands.literal("building")
						.fork(executeNode, (ctx) -> {
							List<CommandSourceStack> list = Lists.newArrayList();
							
							BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
							BuildingPlacement building = BuildingUtils.findBuilding(false, pos);
							if (building != null) {
								String ownerName = building.ownerName;
								if (ownerName != null && !ownerName.isEmpty()) {
									ServerPlayer ownerPlayer = ctx.getSource().getServer().getPlayerList().getPlayerByName(ownerName);
									if (ownerPlayer != null) {
										list.add(ctx.getSource().withEntity(ownerPlayer));
									}
								}
							}
							return list;
						})
					)
					.then(Commands.literal("entity")
						.fork(executeNode, (ctx) -> {
							List<CommandSourceStack> list = Lists.newArrayList();
							
							Entity entity = ctx.getSource().getEntity();
							if (entity instanceof Unit unit) {
								String ownerName = unit.getOwnerName();
								if (ownerName != null && !ownerName.isEmpty()) {
									ServerPlayer ownerPlayer = ctx.getSource().getServer().getPlayerList().getPlayerByName(ownerName);
									if (ownerPlayer != null) {
										list.add(ctx.getSource().withEntity(ownerPlayer));
									}
								}
							}
							return list;
						})
					)
				)
				.then(Commands.literal("allies")
					.fork(executeNode, (ctx) -> {
						List<CommandSourceStack> list = Lists.newArrayList();
						
						ServerPlayer player = ctx.getSource().getPlayer();
						if (player != null) {
							String playerName = player.getName().getString();
							
							for (String allyName : AlliancesServerEvents.getAllAllies(playerName)) {
								ServerPlayer allyPlayer = ctx.getSource().getServer().getPlayerList().getPlayerByName(allyName);
								if (allyPlayer != null)
									list.add(ctx.getSource().withEntity(allyPlayer));
							}
						}
						return list;
					})
				)
				.then(Commands.literal("enemies")
					.fork(executeNode, (ctx) -> {
						List<CommandSourceStack> list = Lists.newArrayList();
						
						ServerPlayer player = ctx.getSource().getPlayer();
						if (player != null) {
							String playerName = player.getName().getString();
							
							for (ServerPlayer serverPlayer : ctx.getSource().getServer().getPlayerList().getPlayers()) {
								if (PlayerServerEvents.isRTSPlayer(serverPlayer.getName().getString()) && !AlliancesServerEvents.isAlliedOrOwned(playerName, serverPlayer.getName().getString())) {
									list.add(ctx.getSource().withEntity(serverPlayer));
								}
							}
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
}
