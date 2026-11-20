package com.solegendary.reignofnether.commands.rtsapi.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.solegendary.reignofnether.mixin.EntitySelectorAccessor;
import com.solegendary.reignofnether.player.PlayerRoles;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class PlayerNameArgument implements ArgumentType<PlayerNameArgument.Result> {
	
	private static final Collection<String> EXAMPLES = Arrays.asList("@b", "@b[type=foo]", "name");
	private static final SimpleCommandExceptionType ERROR_NOT_ALLOWED_ALL =
		new SimpleCommandExceptionType(Component.translatable("argument.player.not_allowed_all"));
	private static final SimpleCommandExceptionType ERROR_NOT_SINGLE_PLAYER =
		new SimpleCommandExceptionType(Component.translatable("argument.player.toomany"));
	private static final SimpleCommandExceptionType ERROR_ONLY_PLAYERS_ALLOWED =
		new SimpleCommandExceptionType(Component.translatable("argument.player.entities"));
	final boolean all;
	
	public PlayerNameArgument(boolean all) {
		this.all = all;
	}
	
	public static PlayerNameArgument players() {
		return new PlayerNameArgument(true);
	}
	
	public static PlayerNameArgument player() {
		return new PlayerNameArgument(false);
	}
	
	// the name as typed, a seat name staying a seat name (for things resolved later, like restrictions); null for "*"
	public static String getTypedName(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
		Result result = pContext.getArgument(pName, Result.class);
		String typed = result.selector != null ? ((EntitySelectorAccessor) result.selector).getPlayerName() : result.rawInput;
		return typed != null ? typed : result.resolve(pContext.getSource());
	}
	
	// the owner a player argument means: a co-commander or soldier means the commander they play for.
	// For the person themselves (camera, teleport) use getPersonName
	public static String getPlayerName(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
		String name = getPersonName(pContext, pName);
		return name != null ? PlayerRoles.commanderOf(name) : null;
	}
	
	// an online player as they are; a name nobody online has (a bot, an offline player, a map seat or role name meaning
	// whoever plays it, or the seat itself) as typed; @s from a source with no player is the owner it acts for
	// (sourceOwner). Real selectors (@a, @p...) select online players as usual. null for "*"
	public static String getPersonName(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
		Result result = pContext.getArgument(pName, Result.class);
		CommandSourceStack source = pContext.getSource();
		String typed = result.selector != null ? ((EntitySelectorAccessor) result.selector).getPlayerName() : result.rawInput;
		// a seat name first, so a player can't take over a seat's commands by picking its name as their username
		String occupant = typed != null ? StartPosServerEvents.seatOccupant(typed) : null;
		if (occupant != null)
			return occupant;
		if (typed != null && source.getServer().getPlayerList().getPlayerByName(typed) == null) {
			synchronized (PlayerServerEvents.rtsPlayers) {
				for (RTSPlayer rtsPlayer : PlayerServerEvents.rtsPlayers)
					if (rtsPlayer.name.equalsIgnoreCase(typed))
						return rtsPlayer.name;
			}
			return typed;
		}
		if (result.selector != null && result.selector.isSelfSelector() && !(source.getEntity() instanceof Player)) {
			String owner = sourceOwner(source);
			if (!owner.isEmpty())
				return owner;
		}
		return result.resolve(source);
	}
	
	// the owner a command source acts for: a player's commander, a unit's owner, or the name of a source with no
	// entity named after an RTS player or seat (RonFiles.asOwner); "" for the server and anything else
	public static String sourceOwner(CommandSourceStack source) {
		Entity entity = source.getEntity();
		if (entity instanceof Player player)
			return PlayerRoles.commanderOf(player.getName().getString());
		if (entity instanceof Unit unit)
			return unit.getOwnerName();
		if (entity == null) {
			String name = source.getTextName();
			if (PlayerServerEvents.isRTSPlayer(name))
				return name;
			String occupant = StartPosServerEvents.seatOccupant(name);
			if (occupant != null)
				return occupant;
		}
		return "";
	}
	
	
	public Result parse(StringReader pReader) throws CommandSyntaxException {
		if (pReader.canRead() && pReader.peek() == '"') {
			return new Result(pReader.readString(), null);
		}
		
		if (pReader.peek() == '*') {
			if (!this.all) {
				throw ERROR_NOT_ALLOWED_ALL.createWithContext(pReader);
			}
			pReader.skip();
			return new Result(null, null);
		}
		int startCursor = pReader.getCursor();
		EntitySelectorParser parser = new EntitySelectorParser(pReader);
		EntitySelector selector = parser.parse();
		
		if (selector.getMaxResults() > 1) {
			pReader.setCursor(startCursor);
			throw ERROR_NOT_SINGLE_PLAYER.createWithContext(pReader);
		}
		if (selector.includesEntities() && !selector.isSelfSelector()) {
			pReader.setCursor(startCursor);
			throw ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(pReader);
		}
		
		return new Result(null, selector);
	}
	
	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
		S s = pContext.getSource();
		if (s instanceof SharedSuggestionProvider sharedsuggestionprovider) {
			StringReader stringreader = new StringReader(pBuilder.getInput());
			stringreader.setCursor(pBuilder.getStart());
			EntitySelectorParser entityselectorparser = new EntitySelectorParser(stringreader, net.minecraftforge.common.ForgeHooks.canUseEntitySelectors(sharedsuggestionprovider));
			
			try {
				entityselectorparser.parse();
			} catch (CommandSyntaxException ignored) {
			}
			
			return entityselectorparser.fillSuggestions(pBuilder, (p_91457_) -> {
				Collection<String> collection = sharedsuggestionprovider.getOnlinePlayerNames();
				if (all)
					collection.add("*");
				SharedSuggestionProvider.suggest(collection, p_91457_);
			});
		} else {
			return Suggestions.empty();
		}
	}
	
	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
	
	public static final class Result {
		
		private final String rawInput;
		private final EntitySelector selector;
		
		private Result(String pInput, EntitySelector selector) {
			this.rawInput = pInput;
			this.selector = selector;
		}
		
		public String resolve(CommandSourceStack source) throws CommandSyntaxException {
			if (selector != null) {
				ServerPlayer player = selector.findSinglePlayer(source);
				return player.getName().getString();
			}
			
			return rawInput;
		}
		
	}
	
	public static class Info implements ArgumentTypeInfo<PlayerNameArgument, PlayerNameArgument.Info.Template> {
		
		@Override
		public void serializeToNetwork(@NotNull Template pTemplate, @NotNull FriendlyByteBuf pBuffer) {
			pBuffer.writeBoolean(pTemplate.can_sharp);
		}
		
		@Override
		public @NotNull Template deserializeFromNetwork(@NotNull FriendlyByteBuf pBuffer) {
			return new Template(pBuffer.readBoolean());
		}
		
		@Override
		public void serializeToJson(@NotNull Template pTemplate, @NotNull JsonObject pJson) {
			pJson.addProperty("can_sharp", pTemplate.can_sharp);
		}
		
		@Override
		public @NotNull Template unpack(@NotNull PlayerNameArgument pArgument) {
			return new Template(pArgument.all);
		}
		
		public final class Template implements ArgumentTypeInfo.Template<PlayerNameArgument> {
			private final boolean can_sharp;
			
			Template(boolean can_sharp) {
				this.can_sharp = can_sharp;
			}
			
			@Override
			public @NotNull PlayerNameArgument instantiate(@NotNull CommandBuildContext pContext) {
				return new PlayerNameArgument(can_sharp);
			}
			
			@Override
			public @NotNull ArgumentTypeInfo<PlayerNameArgument, ?> type() {
				return Info.this;
			}
		}
	}
	
}
