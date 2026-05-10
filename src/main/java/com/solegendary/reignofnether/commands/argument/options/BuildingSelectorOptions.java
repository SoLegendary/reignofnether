package com.solegendary.reignofnether.commands.argument.options;

import com.google.common.collect.Maps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.commands.argument.BuildingSelector;
import com.solegendary.reignofnether.commands.argument.BuildingSelectorParser;

import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BuildingSelectorOptions {
	
	public static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION = new DynamicCommandExceptionType((p_121520_) -> Component.translatable("argument.building.options.unknown", p_121520_));
	public static final DynamicCommandExceptionType ERROR_INAPPLICABLE_OPTION = new DynamicCommandExceptionType((p_121516_) -> Component.translatable("argument.building.options.inapplicable", p_121516_));
	public static final SimpleCommandExceptionType ERROR_RANGE_NEGATIVE = new SimpleCommandExceptionType(Component.translatable("argument.building.options.distance.negative"));
	public static final SimpleCommandExceptionType ERROR_LIMIT_TOO_SMALL = new SimpleCommandExceptionType(Component.translatable("argument.building.options.limit.too_small"));
	public static final DynamicCommandExceptionType ERROR_SORT_UNKNOWN = new DynamicCommandExceptionType((p_121508_) -> Component.translatable("argument.building.options.sort.irreversible", p_121508_));
	public static final DynamicCommandExceptionType ERROR_BUILDING_TYPE_INVALID = new DynamicCommandExceptionType((p_121452_) -> Component.translatable("argument.building.options.type.invalid", p_121452_));
	private static final Map<String, BuildingSelectorOptions.Option> OPTIONS = Maps.newHashMap();
	private static final SimpleCommandExceptionType UNKNOWN_BUILDING =
		new SimpleCommandExceptionType(Component.translatable("commands.reignofnether.error.unknown_building"));
	
	public static void register(String pId, BuildingSelectorOptions.Modifier pHandler, Predicate<BuildingSelectorParser> pPredicate, Component pTooltip) {
		OPTIONS.put(pId, new BuildingSelectorOptions.Option(pHandler, pPredicate, pTooltip));
	}
	
	public static void bootStrap() {
		if (OPTIONS.isEmpty()) {
			
			register("distance", (p_121421_) -> {
				int i = p_121421_.getReader().getCursor();
				MinMaxBounds.Doubles minmaxbounds$doubles = MinMaxBounds.Doubles.fromReader(p_121421_.getReader());
				if ((minmaxbounds$doubles.getMin() == null || !(minmaxbounds$doubles.getMin() < 0.0D)) && (minmaxbounds$doubles.getMax() == null || !(minmaxbounds$doubles.getMax() < 0.0D))) {
					p_121421_.setDistance(minmaxbounds$doubles);
				} else {
					p_121421_.getReader().setCursor(i);
					throw ERROR_RANGE_NEGATIVE.createWithContext(p_121421_.getReader());
				}
			}, (p_121419_) -> p_121419_.getDistance().isAny(), Component.translatable("argument.building.options.distance.description"));
			register("rotation", (selector) -> selector.setRotation(selector.getReader().readString()), (p_121411_) -> p_121411_.getRotation() == null, Component.translatable("argument.building.options.rotation.description"));
			register("x", (p_121413_) -> p_121413_.setX(p_121413_.getReader().readDouble()), (p_121411_) -> p_121411_.getX() == null, Component.translatable("argument.building.options.x.description"));
			register("y", (p_121409_) -> p_121409_.setY(p_121409_.getReader().readDouble()), (p_121407_) -> p_121407_.getY() == null, Component.translatable("argument.building.options.y.description"));
			register("z", (p_121405_) -> p_121405_.setZ(p_121405_.getReader().readDouble()), (p_121403_) -> p_121403_.getZ() == null, Component.translatable("argument.building.options.z.description"));
			register("dx", (p_121401_) -> p_121401_.setDeltaX(p_121401_.getReader().readDouble()), (p_121399_) -> p_121399_.getDeltaX() == null, Component.translatable("argument.building.options.dx.description"));
			register("dy", (p_121397_) -> p_121397_.setDeltaY(p_121397_.getReader().readDouble()), (p_121395_) -> p_121395_.getDeltaY() == null, Component.translatable("argument.building.options.dy.description"));
			register("dz", (p_121562_) -> p_121562_.setDeltaZ(p_121562_.getReader().readDouble()), (p_121560_) -> p_121560_.getDeltaZ() == null, Component.translatable("argument.building.options.dz.description"));
			register("limit", (p_121550_) -> {
				int i = p_121550_.getReader().getCursor();
				int j = p_121550_.getReader().readInt();
				if (j < 1) {
					p_121550_.getReader().setCursor(i);
					throw ERROR_LIMIT_TOO_SMALL.createWithContext(p_121550_.getReader());
				} else {
					p_121550_.setMaxResults(j);
					p_121550_.setLimited(true);
				}
			}, (p_121548_) -> !p_121548_.isLimited(), Component.translatable("argument.building.options.limit.description"));
			register("sort", (p_247983_) -> {
				int i = p_247983_.getReader().getCursor();
				String s = p_247983_.getReader().readUnquotedString();
				p_247983_.setSuggestions((p_175153_, p_175154_) -> SharedSuggestionProvider.suggest(Arrays.asList("nearest", "furthest", "random", "arbitrary"), p_175153_));
				BiConsumer<Vec3, List<? extends BuildingPlacement>> biconsumer = switch (s) {
					case "nearest" -> BuildingSelectorParser.ORDER_NEAREST;
					case "furthest" -> BuildingSelectorParser.ORDER_FURTHEST;
					case "random" -> BuildingSelectorParser.ORDER_RANDOM;
					case "arbitrary" -> BuildingSelector.ORDER_ARBITRARY;
					default -> {
						p_247983_.getReader().setCursor(i);
						throw ERROR_SORT_UNKNOWN.createWithContext(p_247983_.getReader(), s);
					}
				};
				
				p_247983_.setOrder(biconsumer);
				p_247983_.setSorted(true);
			}, (p_121544_) -> !p_121544_.isSorted(), Component.translatable("argument.building.options.sort.description"));
			register("type", (p_121534_) -> {
				p_121534_.setSuggestions((ctx, builder) ->
					SharedSuggestionProvider.suggestResource(
						Stream.concat(
							ReignOfNetherRegistries.BUILDING.stream(),
							CustomBuildingClientEvents.customBuildings.stream()
						).collect(Collectors.toList()),
						ctx,
						building -> {
							ResourceLocation id = ReignOfNetherRegistries.BUILDING.getKey(building);
							return id != null ? id : ResourceLocation.fromNamespaceAndPath(
								"custom",
								building.structureName.toLowerCase().replace(' ', '_'));
						},
						building -> Component.literal(building.name))
				);
				
				ResourceLocation resourcelocation = ResourceLocation.read(p_121534_.getReader());
				Building building = resolveBuilding(resourcelocation.toString());
				if (building == null) {
					throw UNKNOWN_BUILDING.create();
				}
				p_121534_.limitToType(building.name);
			}, (p_121532_) -> !p_121532_.isTypeLimited(), Component.translatable("argument.building.options.type.description"));
		}
	}
	
	private static Building resolveBuilding(String input) {
		ResourceLocation location;
		if (input.contains(":")) {
			location = ResourceLocation.tryParse(input);
		} else {
			location = ResourceLocation.tryParse(ReignOfNether.MOD_ID + ":" + input);
		}
		Building building = location == null ? null : ReignOfNetherRegistries.BUILDING.get(location);
		if (building == null) {
			building = CustomBuildingServerEvents.getCustomBuilding(
				StringUtils.capitalize(
					input
						.replace("custom:", "")
						.replace("_", " ")
				)
			);
		}
		return building;
	}
	
	public static BuildingSelectorOptions.Modifier get(BuildingSelectorParser pParser, String pId, int pCursor) throws CommandSyntaxException {
		BuildingSelectorOptions.Option buildingselectoroptions$option = OPTIONS.get(pId);
		if (buildingselectoroptions$option != null) {
			if (buildingselectoroptions$option.canUse.test(pParser)) {
				return buildingselectoroptions$option.modifier;
			} else {
				throw ERROR_INAPPLICABLE_OPTION.createWithContext(pParser.getReader(), pId);
			}
		} else {
			pParser.getReader().setCursor(pCursor);
			throw ERROR_UNKNOWN_OPTION.createWithContext(pParser.getReader(), pId);
		}
	}
	
	public static void suggestNames(BuildingSelectorParser pParser, SuggestionsBuilder pBuilder) {
		String s = pBuilder.getRemaining().toLowerCase(Locale.ROOT);
		
		for (Map.Entry<String, BuildingSelectorOptions.Option> entry : OPTIONS.entrySet()) {
			if ((entry.getValue()).canUse.test(pParser) && entry.getKey().toLowerCase(Locale.ROOT).startsWith(s)) {
				pBuilder.suggest(entry.getKey() + "=", (entry.getValue()).description);
			}
		}
		
	}
	
	public interface Modifier {
		void handle(BuildingSelectorParser pParser) throws CommandSyntaxException;
	}
	
	record Option(BuildingSelectorOptions.Modifier modifier, Predicate<BuildingSelectorParser> canUse, Component description) {
	}
	
}
