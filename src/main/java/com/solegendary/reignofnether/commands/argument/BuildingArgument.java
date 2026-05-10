package com.solegendary.reignofnether.commands.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.solegendary.reignofnether.building.BuildingPlacement;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BuildingArgument implements ArgumentType<BuildingSelector> {
	
	public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_BUILDING = new SimpleCommandExceptionType(Component.translatable("argument.building.toomany"));
	public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_PLAYER = new SimpleCommandExceptionType(Component.translatable("argument.player.toomany"));
	public static final SimpleCommandExceptionType ERROR_ONLY_PLAYERS_ALLOWED = new SimpleCommandExceptionType(Component.translatable("argument.player.buildings"));
	public static final SimpleCommandExceptionType NO_BUILDINGS_FOUND = new SimpleCommandExceptionType(Component.translatable("argument.building.notfound.building"));
	public static final SimpleCommandExceptionType NO_PLAYERS_FOUND = new SimpleCommandExceptionType(Component.translatable("argument.building.notfound.player"));
	public static final SimpleCommandExceptionType ERROR_SELECTORS_NOT_ALLOWED = new SimpleCommandExceptionType(Component.translatable("argument.building.selector.not_allowed"));
	private static final Collection<String> EXAMPLES = Arrays.asList("@b", "@b[type=foo]");
	final boolean single;
	
	public BuildingArgument(boolean pSingle) {
		this.single = pSingle;
	}
	
	public static BuildingArgument building() {
		return new BuildingArgument(true);
	}
	
	public static BuildingPlacement getBuilding(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
		return pContext.getArgument(pName, BuildingSelector.class).findSingleBuilding(pContext.getSource());
	}
	
	public static BuildingArgument buildings() {
		return new BuildingArgument(false);
	}
	
	public static List<? extends BuildingPlacement> getBuildings(CommandContext<CommandSourceStack> pContext, String pName, String pOwner) throws CommandSyntaxException {
		List<? extends BuildingPlacement> collection = getOptionalBuildings(pContext, pName, pOwner
		);
		if (collection.isEmpty()) {
			throw NO_BUILDINGS_FOUND.create();
		} else {
			return collection;
		}
	}
	
	public static List<? extends BuildingPlacement> getOptionalBuildings(CommandContext<CommandSourceStack> pContext, String pName, String pOwner) throws CommandSyntaxException {
		BuildingSelector selector = pContext.getArgument(pName, BuildingSelector.class);
		selector.setOwnerName(pOwner);
		return selector.findBuildings(pContext.getSource());
	}
	
	public BuildingSelector parse(StringReader pReader) throws CommandSyntaxException {
		BuildingSelectorParser buildingselectorparser = new BuildingSelectorParser(pReader);
		BuildingSelector buildingselector = buildingselectorparser.parse();
		if (buildingselector.getMaxResults() > 1 && this.single) {
			
			pReader.setCursor(0);
			throw ERROR_NOT_SINGLE_BUILDING.createWithContext(pReader);
			
		} else {
			return buildingselector;
		}
	}
	
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
		S s = pContext.getSource();
		if (s instanceof SharedSuggestionProvider) {
			StringReader stringreader = new StringReader(pBuilder.getInput());
			stringreader.setCursor(pBuilder.getStart());
			BuildingSelectorParser buildingselectorparser = new BuildingSelectorParser(stringreader);
			
			try {
				buildingselectorparser.parse();
			} catch (CommandSyntaxException ignored) {
			}
			
			return buildingselectorparser.fillSuggestions(pBuilder, (p_91457_) -> {
			});
		} else {
			return Suggestions.empty();
		}
	}
	
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
	
	public static class Info implements ArgumentTypeInfo<BuildingArgument, BuildingArgument.Info.Template> {
		private static final byte FLAG_SINGLE = 1;
		
		@Override
		public void serializeToNetwork(Template pTemplate, FriendlyByteBuf pBuffer) {
			int i = pTemplate.single ? FLAG_SINGLE : 0;
			pBuffer.writeByte(i);
		}
		
		@Override
		public @NotNull Template deserializeFromNetwork(FriendlyByteBuf pBuffer) {
			byte b0 = pBuffer.readByte();
			return new Template((b0 & FLAG_SINGLE) != 0);
		}
		
		@Override
		public void serializeToJson(Template pTemplate, JsonObject pJson) {
			pJson.addProperty("amount", pTemplate.single ? "single" : "multiple");
			// 不需要 type 字段
		}
		
		@Override
		public @NotNull Template unpack(BuildingArgument pArgument) {
			return new Template(pArgument.single);
		}
		
		public final class Template implements ArgumentTypeInfo.Template<BuildingArgument> {
			final boolean single;
			
			Template(boolean pSingle) {
				this.single = pSingle;
			}
			
			@Override
			public @NotNull BuildingArgument instantiate(@NotNull CommandBuildContext pContext) {
				return new BuildingArgument(this.single);
			}
			
			@Override
			public @NotNull ArgumentTypeInfo<BuildingArgument, ?> type() {
				return Info.this;
			}
		}
	}
	
}
