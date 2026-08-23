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

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
	
	public static String getPlayerName(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
		try {
			pContext.getArgument("rotation1", Integer.class);
			return ((EntitySelectorAccessor) pContext.getArgument(pName, Result.class).selector).getPlayerName();
		} catch (Exception e) {
			return pContext.getArgument(pName, Result.class).resolve(pContext.getSource());
		}
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
