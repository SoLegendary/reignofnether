package com.solegendary.reignofnether.commands.rtsapi;

import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.solegendary.reignofnether.rtsmap.RTSMapInfo;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerEvents;
import com.solegendary.reignofnether.rtsmap.RonBars;
import com.solegendary.reignofnether.rtsmap.RonEvents;
import com.solegendary.reignofnether.rtsmap.RonFiles;
import com.solegendary.reignofnether.rtsmap.RonZones;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

// the map's own files: /rtsapi run <ron file>, /rtsapi map rule <gamerule> <value>
public class RonCommands {

	// every ron/**/*.mcfunction as a path without the extension, e.g. events/unit_died
	private static final SuggestionProvider<CommandSourceStack> RON_FILES = (ctx, builder) -> {
		Path dir = RonFiles.dir(ctx.getSource().getServer());
		if (Files.isDirectory(dir)) {
			try (Stream<Path> files = Files.walk(dir)) {
				List<String> paths = files.filter(p -> p.toString().endsWith(".mcfunction"))
					.map(p -> dir.relativize(p).toString().replace('\\', '/').replaceAll("\\.mcfunction$", ""))
					.toList();
				return SharedSuggestionProvider.suggest(paths, builder);
			} catch (IOException ignored) { }
		}
		return builder.buildFuture();
	};

	// the events a file can wait for: RonFiles.fire's, typed ones as <event>/<type>
	private static final SuggestionProvider<CommandSourceStack> OBJECTIVES = (ctx, builder) -> SharedSuggestionProvider.suggest(
		ctx.getSource().getServer().getCustomBossEvents().getIds().stream()
			.filter(id -> id.getNamespace().equals("reignofnether") && id.getPath().startsWith("objective/"))
			.map(id -> id.getPath().substring("objective/".length())), builder);

	@Nullable
	private static ResourceLocation barId(CommandContext<CommandSourceStack> ctx, String kind) {
		ResourceLocation id = RonBars.id(kind, StringArgumentType.getString(ctx, "id"));
		if (id == null)
			ctx.getSource().sendFailure(Component.translatable("commands.reignofnether.rtsapi.bar.bad_id"));
		return id;
	}

	private static boolean found(CommandContext<CommandSourceStack> ctx, boolean found) {
		if (!found)
			ctx.getSource().sendFailure(Component.translatable("commands.reignofnether.rtsapi.bar.missing", StringArgumentType.getString(ctx, "id")));
		return found;
	}

	private static int notInFile(CommandContext<CommandSourceStack> ctx) {
		ctx.getSource().sendFailure(Component.translatable("commands.reignofnether.rtsapi.wait.file_only"));
		return 0;
	}

	public static void register(final LiteralArgumentBuilder<CommandSourceStack> commandBuilder) {
		commandBuilder
			.then(run())
			.then(waitCommand())
			.then(zone())
			.then(schedule())
			.then(objective())
			.then(timer())
			.then(map());
	}

	private static ArgumentBuilder<CommandSourceStack, ?> run() {
		return Commands.literal("run")
			.then(Commands.argument("path", StringArgumentType.greedyString())
				.suggests(RON_FILES)
				.executes(ctx -> {
					RonFiles.run(ctx.getSource().getServer(), StringArgumentType.getString(ctx, "path"), ctx.getSource(), RonFiles.currentOwner());
					return 1;
				})
			);
	}

	// rtsapi wait <time> / rtsapi wait for <event> / rtsapi wait until <condition>: pauses the ron file it's in (RonFiles.runLines); typed anywhere else it does nothing
	private static ArgumentBuilder<CommandSourceStack, ?> waitCommand() {
		return Commands.literal("wait")
			.then(Commands.literal("until")
				.then(Commands.argument("condition", StringArgumentType.greedyString())
					.executes(ctx -> notInFile(ctx))))
			.then(Commands.literal("for")
				.then(Commands.argument("event", StringArgumentType.greedyString())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RonEvents.WAITABLE, builder))
					.executes(ctx -> notInFile(ctx))))
			.then(Commands.argument("time", TimeArgument.time())
				.executes(ctx -> notInFile(ctx)));
	}

	// rtsapi zone add <name> <from> <to>: events/zone_entered/<name> and zone_left/<name> as units walk in and out
	private static ArgumentBuilder<CommandSourceStack, ?> zone() {
		return Commands.literal("zone")
			.then(Commands.literal("add")
				.then(Commands.argument("name", StringArgumentType.word())
					.then(Commands.argument("from", BlockPosArgument.blockPos())
						.then(Commands.argument("to", BlockPosArgument.blockPos())
							.executes(ctx -> {
								RonZones.add(StringArgumentType.getString(ctx, "name"),
									BlockPosArgument.getSpawnablePos(ctx, "from"),
									BlockPosArgument.getSpawnablePos(ctx, "to"));
								return 1;
							})))))
			.then(Commands.literal("remove")
				.then(Commands.argument("name", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RonZones.names(), builder))
					.executes(ctx -> RonZones.remove(StringArgumentType.getString(ctx, "name")) ? 1 : 0)))
			.then(Commands.literal("clear")
				.executes(ctx -> {
					RonZones.clear();
					return 1;
				}));
	}

	// rtsapi schedule <time> <file> (vanilla time: 100, 5s): runs it later, as this source and $owner; rtsapi schedule clear cancels them all
	private static ArgumentBuilder<CommandSourceStack, ?> schedule() {
		return Commands.literal("schedule")
			.then(Commands.literal("clear")
				.executes(ctx -> {
					RonFiles.cancelScheduled();
					return 1;
				}))
			.then(Commands.argument("ticks", TimeArgument.time())
				.then(Commands.argument("path", StringArgumentType.greedyString())
					.suggests(RON_FILES)
					.executes(ctx -> {
						RonFiles.schedule(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "ticks"),
								StringArgumentType.getString(ctx, "path"), ctx.getSource(), RonFiles.currentOwner());
						return 1;
					})));
	}

	// rtsapi objective add <id> <goal> <players> <text> (goal 0: no count), progress <id> <amount>, complete <id>:
	// a vanilla bossbar reignofnether:objective/<id> the RTS view shows in its Objectives panel (RonBars)
	private static ArgumentBuilder<CommandSourceStack, ?> objective() {
		return Commands.literal("objective")
			.then(Commands.literal("add")
				.then(Commands.argument("id", StringArgumentType.word())
					.then(Commands.argument("goal", IntegerArgumentType.integer(0))
						.then(Commands.argument("players", EntityArgument.players())
							.then(Commands.argument("text", StringArgumentType.greedyString())
								.executes(ctx -> {
									ResourceLocation id = barId(ctx, "objective");
									if (id == null)
										return 0;
									RonBars.objective(ctx.getSource().getServer(), id, IntegerArgumentType.getInteger(ctx, "goal"),
											StringArgumentType.getString(ctx, "text"), EntityArgument.getPlayers(ctx, "players"));
									return 1;
								}))))))
			.then(Commands.literal("progress")
				.then(Commands.argument("id", StringArgumentType.word())
					.suggests(OBJECTIVES)
					.then(Commands.argument("amount", IntegerArgumentType.integer())
						.executes(ctx -> {
							ResourceLocation id = barId(ctx, "objective");
							return id != null && found(ctx, RonBars.progress(ctx.getSource().getServer(), id, IntegerArgumentType.getInteger(ctx, "amount"))) ? 1 : 0;
						}))))
			.then(Commands.literal("complete")
				.then(Commands.argument("id", StringArgumentType.word())
					.suggests(OBJECTIVES)
					.executes(ctx -> {
						ResourceLocation id = barId(ctx, "objective");
						return id != null && found(ctx, RonBars.complete(ctx.getSource().getServer(), id)) ? 1 : 0;
					})));
	}

	// rtsapi timer <id> <time> "<title>" <file>: a countdown everyone sees (bossbar reignofnether:timer/<id>), then
	// runs the file; bossbar remove reignofnether:timer/<id> stops it
	private static ArgumentBuilder<CommandSourceStack, ?> timer() {
		return Commands.literal("timer")
			.then(Commands.argument("id", StringArgumentType.word())
				.then(Commands.argument("time", TimeArgument.time())
					.then(Commands.argument("title", StringArgumentType.string())
						.then(Commands.argument("path", StringArgumentType.greedyString())
							.suggests(RON_FILES)
							.executes(ctx -> {
								ResourceLocation id = barId(ctx, "timer");
								if (id == null)
									return 0;
								RonBars.timer(ctx.getSource().getServer(), id, (IntegerArgumentType.getInteger(ctx, "time") + 19) / 20,
										StringArgumentType.getString(ctx, "title"), StringArgumentType.getString(ctx, "path"),
										ctx.getSource(), RonFiles.currentOwner());
								return 1;
							})))));
	}

	private static ArgumentBuilder<CommandSourceStack, ?> map() {
		return Commands.literal("map")
			.then(Commands.literal("rule")
				.then(Commands.argument("name", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RTSMapInfoServerEvents.MAP_RULES, builder))
					.then(Commands.argument("value", StringArgumentType.word())
						.executes(ctx -> {
							String name = StringArgumentType.getString(ctx, "name");
							String value = StringArgumentType.getString(ctx, "value");
							Component refusal = RTSMapInfoServerEvents.writeRefusal();
							if (refusal != null) {
								ctx.getSource().sendFailure(refusal);
								return 0;
							}
							RTSMapInfo info = RTSMapInfoServerEvents.rtsMapInfo != null ? RTSMapInfoServerEvents.rtsMapInfo : new RTSMapInfo();
							info.getRules().put(name, toJson(value));
							if (!RTSMapInfoServerEvents.setAndSave(ctx.getSource().getServer(), info)) {
								ctx.getSource().sendFailure(Component.translatable("commands.reignofnether.rtsapi.map_rule.failed"));
								return 0;
							}
							ctx.getSource().sendSuccess(() -> Component.translatable("commands.reignofnether.rtsapi.map_rule.set", name, value), true);
							return 1;
						})
					)
				)
			);
	}

	// true/false and whole numbers stay typed in map.json, anything else is kept as text
	private static JsonPrimitive toJson(String value) {
		if (value.equals("true") || value.equals("false"))
			return new JsonPrimitive(Boolean.parseBoolean(value));
		try {
			return new JsonPrimitive(Integer.parseInt(value));
		} catch (NumberFormatException e) {
			return new JsonPrimitive(value);
		}
	}
}
