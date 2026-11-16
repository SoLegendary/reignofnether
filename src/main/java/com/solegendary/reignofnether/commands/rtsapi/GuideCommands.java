package com.solegendary.reignofnether.commands.rtsapi;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.commands.rtsapi.argument.BuildingArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.PlayerNameArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.UnitArgument;
import com.solegendary.reignofnether.guide.GuideAction;
import com.solegendary.reignofnether.guide.GuideClientboundPacket;
import com.solegendary.reignofnether.guide.GuideServer;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRoles;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.soldier.SoldierServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

// campaign and tutorial guidance from map scripts:
// /rtsapi say <players> <head> <name> [wait] [then <file>] <text>, .. next, .. clear
// /rtsapi highlight <players> <region> [text], .. button <name> [text], .. pos <x y z> [text], .. unit <targets> [text],
//   .. building <targets> [text], .. clear
// /rtsapi player control <players> <true|false>, /rtsapi player camera follow|rotate|shake <players> ...,
// /rtsapi player soldier <players> <owner> [pos]
public class GuideCommands {

	public static final List<String> REGIONS = List.of("resources", "command_card", "selection", "minimap", "tools", "console");

	public static void register(final LiteralArgumentBuilder<CommandSourceStack> commandBuilder) {
		commandBuilder
			.then(Commands.literal("say")
				.then(Commands.argument("players", EntityArgument.players())
					.then(Commands.literal("clear")
						.executes(ctx -> send(ctx, GuideAction.CLEAR_DIALOGUE)))
					// past the current line: how a script ends a line that waits for the player to do something
					.then(Commands.literal("next")
						.executes(ctx -> send(ctx, GuideAction.NEXT)))
					.then(Commands.argument("head", StringArgumentType.word())
						.then(Commands.argument("name", StringArgumentType.string())
							// wait: SPACE doesn't close it, the script's say <players> next does
							.then(Commands.literal("wait")
								.then(thenTail(true))
								.then(textTail(true, false)))
							// then <file>: runs ron/<file>.mcfunction as the player when the line starts showing
							.then(thenTail(false))
							.then(textTail(false, false))))))
			.then(Commands.literal("highlight")
				.then(Commands.argument("players", EntityArgument.players())
					.then(Commands.literal("clear")
						.executes(ctx -> send(ctx, GuideAction.CLEAR_HIGHLIGHT)))
					.then(Commands.literal("button")
						.then(Commands.argument("name", StringArgumentType.string())
							.executes(ctx -> highlight(ctx, "button:" + StringArgumentType.getString(ctx, "name"), "", BlockPos.ZERO))
							.then(Commands.argument("text", StringArgumentType.greedyString())
								.executes(ctx -> highlight(ctx, "button:" + StringArgumentType.getString(ctx, "name"),
									StringArgumentType.getString(ctx, "text"), BlockPos.ZERO)))))
					.then(Commands.literal("pos")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.executes(ctx -> highlight(ctx, "pos", "", BlockPosArgument.getBlockPos(ctx, "pos")))
							.then(Commands.argument("text", StringArgumentType.greedyString())
								.executes(ctx -> highlight(ctx, "pos", StringArgumentType.getString(ctx, "text"),
									BlockPosArgument.getBlockPos(ctx, "pos"))))))
					// the first unit or building found: the ring follows it, and goes once the player selects it
					.then(Commands.literal("unit")
						.then(Commands.argument("targets", UnitArgument.units())
							.executes(ctx -> highlightUnit(ctx, ""))
							.then(Commands.argument("text", StringArgumentType.greedyString())
								.executes(ctx -> highlightUnit(ctx, StringArgumentType.getString(ctx, "text"))))))
					.then(Commands.literal("building")
						.then(Commands.argument("targets", BuildingArgument.buildings())
							.executes(ctx -> highlightBuilding(ctx, ""))
							.then(Commands.argument("text", StringArgumentType.greedyString())
								.executes(ctx -> highlightBuilding(ctx, StringArgumentType.getString(ctx, "text"))))))
					.then(Commands.argument("region", StringArgumentType.word())
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(REGIONS, builder))
						.executes(ctx -> region(ctx, ""))
						.then(Commands.argument("text", StringArgumentType.greedyString())
							.executes(ctx -> region(ctx, StringArgumentType.getString(ctx, "text")))))))
			.then(Commands.literal("player")
				// first person, fighting for owner (a player or seat name); respawns at pos (default: where they
				// stand) while owner has no building
				.then(Commands.literal("soldier")
					.then(Commands.argument("players", EntityArgument.players())
						.then(Commands.argument("owner", PlayerNameArgument.player())
							.executes(ctx -> soldier(ctx, null))
							.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(ctx -> soldier(ctx, BlockPosArgument.getBlockPos(ctx, "pos")))))))
				// false: no selecting or ordering, the HUD replaced by letterbox bars (cutscenes, explanations)
				.then(Commands.literal("control")
					.then(Commands.argument("players", EntityArgument.players())
						.then(Commands.argument("control", BoolArgumentType.bool())
							.executes(ctx -> {
								boolean control = BoolArgumentType.getBool(ctx, "control");
								var players = EntityArgument.getPlayers(ctx, "players");
									players.forEach(p -> GuideServer.setControl(p, control));
									GuideClientboundPacket.control(players, control);
									return players.size();
							}))))
				.then(Commands.literal("camera")
					// keeps the camera on a unit or entity, for ticks (0 = until follow <players> clear)
					.then(Commands.literal("follow")
						.then(Commands.argument("players", EntityArgument.players())
							.then(Commands.literal("clear")
								.executes(ctx -> follow(ctx, -1, 0)))
							.then(Commands.argument("target", EntityArgument.entity())
								.executes(ctx -> follow(ctx, EntityArgument.getEntity(ctx, "target").getId(), 0))
								.then(Commands.argument("ticks", IntegerArgumentType.integer(0))
									.executes(ctx -> follow(ctx, EntityArgument.getEntity(ctx, "target").getId(), IntegerArgumentType.getInteger(ctx, "ticks")))))))
					// turns the camera by degrees (+ clockwise) over ticks
					.then(Commands.literal("rotate")
						.then(Commands.argument("players", EntityArgument.players())
							.then(Commands.argument("degrees", FloatArgumentType.floatArg(-3600, 3600))
								.executes(ctx -> camera(ctx, GuideAction.ROTATE, FloatArgumentType.getFloat(ctx, "degrees"), 20))
								.then(Commands.argument("ticks", IntegerArgumentType.integer(1))
									.executes(ctx -> camera(ctx, GuideAction.ROTATE, FloatArgumentType.getFloat(ctx, "degrees"), IntegerArgumentType.getInteger(ctx, "ticks")))))))
					// shakes the view, strength in degrees, fading out over ticks
					.then(Commands.literal("shake")
						.then(Commands.argument("players", EntityArgument.players())
							.then(Commands.argument("strength", FloatArgumentType.floatArg(0, 45))
								.then(Commands.argument("ticks", IntegerArgumentType.integer(1))
									.executes(ctx -> camera(ctx, GuideAction.SHAKE, FloatArgumentType.getFloat(ctx, "strength"), IntegerArgumentType.getInteger(ctx, "ticks")))))))));
	}

	private static ArgumentBuilder<CommandSourceStack, ?> thenTail(boolean wait) {
		return Commands.literal("then")
			.then(Commands.argument("file", ResourceLocationArgument.id())
				.then(textTail(wait, true)));
	}

	private static ArgumentBuilder<CommandSourceStack, ?> textTail(boolean wait, boolean then) {
		return Commands.argument("text", StringArgumentType.greedyString())
			.executes(ctx -> say(ctx, wait, then ? ResourceLocationArgument.getId(ctx, "file").getPath() : null));
	}

	// each player gets their own callback id for the line's file (GuideServer)
	private static int say(CommandContext<CommandSourceStack> ctx, boolean wait, String file) throws CommandSyntaxException {
		var players = EntityArgument.getPlayers(ctx, "players");
		for (ServerPlayer player : players) {
			GuideServer.said(player);
			GuideClientboundPacket.say(player, StringArgumentType.getString(ctx, "head"), StringArgumentType.getString(ctx, "name"),
				StringArgumentType.getString(ctx, "text"), wait, file != null ? GuideServer.addCallback(player, file) : -1);
		}
		return players.size();
	}

	private static int highlightUnit(CommandContext<CommandSourceStack> ctx, String text) throws CommandSyntaxException {
		List<Unit> units = UnitArgument.getUnits(ctx, "targets", null);
		if (units.isEmpty()) {
			ctx.getSource().sendFailure(Component.translatable("commands.reignofnether.unit.selection.none"));
			return 0;
		}
		return highlight(ctx, "entity:" + ((Entity) units.get(0)).getId(), text, BlockPos.ZERO);
	}

	private static int highlightBuilding(CommandContext<CommandSourceStack> ctx, String text) throws CommandSyntaxException {
		List<? extends BuildingPlacement> buildings = BuildingArgument.getBuildings(ctx, "targets", null);
		if (buildings.isEmpty()) {
			ctx.getSource().sendFailure(Component.translatable("commands.reignofnether.building.selection.none"));
			return 0;
		}
		return highlight(ctx, "building", text, buildings.get(0).centrePos);
	}

	private static int soldier(CommandContext<CommandSourceStack> ctx, BlockPos pos) throws CommandSyntaxException {
		String owner = PlayerNameArgument.getPlayerName(ctx, "owner");
		if (!PlayerServerEvents.isRTSPlayer(owner)) {
			ctx.getSource().sendFailure(Component.translatable("commands.reignofnether.rtsapi.soldier.not_playing", owner));
			return 0;
		}
		int n = 0;
		for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
			if (PlayerRoles.roleOf(player.getName().getString()) == PlayerRole.COMMANDER) {
				ctx.getSource().sendFailure(Component.translatable("commands.reignofnether.rtsapi.soldier.commander", player.getName()));
				continue;
			}
			SoldierServerEvents.bind(player, owner, pos != null ? pos : player.blockPosition(), true);
			n++;
		}
		return n;
	}

	private static int follow(CommandContext<CommandSourceStack> ctx, int entityId, int ticks) throws CommandSyntaxException {
		var players = EntityArgument.getPlayers(ctx, "players");
		GuideClientboundPacket.follow(players, entityId, ticks);
		return players.size();
	}

	// ROTATE or SHAKE
	private static int camera(CommandContext<CommandSourceStack> ctx, GuideAction action, float amount, int ticks) throws CommandSyntaxException {
		var players = EntityArgument.getPlayers(ctx, "players");
		GuideClientboundPacket.camera(players, action, amount, ticks);
		return players.size();
	}

	private static int region(CommandContext<CommandSourceStack> ctx, String text) throws CommandSyntaxException {
		String region = StringArgumentType.getString(ctx, "region");
		if (!REGIONS.contains(region)) {
			ctx.getSource().sendFailure(Component.translatable("commands.reignofnether.rtsapi.highlight.unknown", region, String.join(", ", REGIONS)));
			return 0;
		}
		return highlight(ctx, region, text, BlockPos.ZERO);
	}

	private static int highlight(CommandContext<CommandSourceStack> ctx, String target, String text, BlockPos pos) throws CommandSyntaxException {
		var players = EntityArgument.getPlayers(ctx, "players");
		GuideClientboundPacket.highlight(players, target, text, pos);
		return players.size();
	}

	private static int send(CommandContext<CommandSourceStack> ctx, GuideAction action) throws CommandSyntaxException {
		var players = EntityArgument.getPlayers(ctx, "players");
		GuideClientboundPacket.send(players, action);
		return players.size();
	}
}
