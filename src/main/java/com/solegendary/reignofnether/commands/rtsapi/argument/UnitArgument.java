package com.solegendary.reignofnether.commands.rtsapi.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UnitArgument implements ArgumentType<EntitySelector> {
	
	public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_UNIT = new SimpleCommandExceptionType(Component.translatable("argument.reignofnether.unit.too_many.error"));
	public static final SimpleCommandExceptionType NO_UNITS_FOUND = new SimpleCommandExceptionType(Component.translatable("argument.reignofnether.unit.not_found.error"));
	private static final Collection<String> EXAMPLES = Arrays.asList("@e", "@e[type=minecraft:zombie]", "Steve");
	final boolean single;
	private final EntityArgument entityArgument;
	
	public UnitArgument(boolean pSingle) {
		this.single = pSingle;
		this.entityArgument = pSingle ? EntityArgument.entity() : EntityArgument.entities();
	}
	
	public static UnitArgument unit() {
		return new UnitArgument(true);
	}
	
	public static UnitArgument units() {
		return new UnitArgument(false);
	}
	
//	public static Unit getUnit(CommandContext<CommandSourceStack> pContext, String pName, String pOwner) throws CommandSyntaxException {
//		EntitySelector selector = pContext.getArgument(pName, EntitySelector.class);
//		Entity entity = selector.findSingleEntity(pContext.getSource());
//		if (!(entity instanceof Unit unit) || entity instanceof Player) {
//			throw NO_UNITS_FOUND.create();
//		}
//		if (pOwner != null && !Objects.equals(unit.getOwnerName(), pOwner)) {
//			throw NO_UNITS_FOUND.create();
//		}
//		return unit;
//	}
	
	public static List<Unit> getUnits(CommandContext<CommandSourceStack> pContext, String pName, String pOwner) throws CommandSyntaxException {
		List<Unit> units = getOptionalUnits(pContext, pName, pOwner);
		if (units.isEmpty()) {
			throw NO_UNITS_FOUND.create();
		}
		return units;
	}
	
	public static List<Unit> getOptionalUnits(CommandContext<CommandSourceStack> pContext, String pName, String pOwner) throws CommandSyntaxException {
		EntitySelector selector = pContext.getArgument(pName, EntitySelector.class);
		Collection<? extends Entity> entities = selector.findEntities(pContext.getSource());
		List<Unit> units = new ArrayList<>();
		for (Entity entity : entities) {
			if (entity instanceof Unit unit && (pOwner == null || unit.getOwnerName().equals(pOwner)))
				units.add(unit);
		}
		return units;
	}
	
	public EntitySelector parse(StringReader pReader) throws CommandSyntaxException {
		EntitySelector selector = entityArgument.parse(pReader);
		if (single && selector.getMaxResults() > 1) {
			pReader.setCursor(0);
			throw ERROR_NOT_SINGLE_UNIT.createWithContext(pReader);
		}
		return selector;
	}
	
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
		S s = pContext.getSource();
		if (s instanceof SharedSuggestionProvider) {
			return entityArgument.listSuggestions(pContext, pBuilder);
		} else {
			return Suggestions.empty();
		}
	}
	
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
	
	public static class Info implements ArgumentTypeInfo<UnitArgument, UnitArgument.Info.Template> {
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
			pJson.addProperty("points", pTemplate.single ? "single" : "multiple");
		}
		
		@Override
		public @NotNull Template unpack(UnitArgument pArgument) {
			return new Template(pArgument.single);
		}
		
		public final class Template implements ArgumentTypeInfo.Template<UnitArgument> {
			final boolean single;
			
			Template(boolean pSingle) {
				this.single = pSingle;
			}
			
			@Override
			public @NotNull UnitArgument instantiate(@NotNull CommandBuildContext pContext) {
				return new UnitArgument(this.single);
			}
			
			@Override
			public @NotNull ArgumentTypeInfo<UnitArgument, ?> type() {
				return Info.this;
			}
		}
	}
	
}
