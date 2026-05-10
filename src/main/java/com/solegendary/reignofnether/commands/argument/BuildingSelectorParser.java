package com.solegendary.reignofnether.commands.argument;

import com.google.common.primitives.Doubles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.commands.argument.options.BuildingSelectorOptions;

import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

public class BuildingSelectorParser {
	
	public static final char SYNTAX_SELECTOR_START = '@';
	public static final char SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR = '=';
	public static final char SYNTAX_NOT = '!';
	public static final char SYNTAX_TAG = '#';
	public static final SimpleCommandExceptionType ERROR_INVALID_NAME_OR_UUID = new SimpleCommandExceptionType(Component.translatable("argument.buildingPlacement.invalid"));
	public static final DynamicCommandExceptionType ERROR_UNKNOWN_SELECTOR_TYPE = new DynamicCommandExceptionType((p_121301_) -> Component.translatable("argument.buildingPlacement.selector.unknown", p_121301_));
	public static final SimpleCommandExceptionType ERROR_SELECTORS_NOT_ALLOWED = new SimpleCommandExceptionType(Component.translatable("argument.buildingPlacement.selector.not_allowed"));
	public static final SimpleCommandExceptionType ERROR_MISSING_SELECTOR_TYPE = new SimpleCommandExceptionType(Component.translatable("argument.buildingPlacement.selector.missing"));
	public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_OPTIONS = new SimpleCommandExceptionType(Component.translatable("argument.buildingPlacement.options.unterminated"));
	public static final DynamicCommandExceptionType ERROR_EXPECTED_OPTION_VALUE = new DynamicCommandExceptionType((p_121267_) -> Component.translatable("argument.buildingPlacement.options.valueless", p_121267_));
	public static final BiConsumer<Vec3, List<? extends BuildingPlacement>> ORDER_NEAREST = (p_121313_, p_121314_) -> p_121314_.sort((p_175140_, p_175141_) -> Doubles.compare(distanceToSqr(p_175140_, p_121313_), distanceToSqr(p_175141_, p_121313_)));
	public static final BiConsumer<Vec3, List<? extends BuildingPlacement>> ORDER_FURTHEST = (p_121298_, p_121299_) -> p_121299_.sort((p_175131_, p_175132_) -> Doubles.compare(distanceToSqr(p_175132_, p_121298_), distanceToSqr(p_175131_, p_121298_)));
	public static final BiConsumer<Vec3, List<? extends BuildingPlacement>> ORDER_RANDOM = (p_121264_, p_121265_) -> Collections.shuffle(p_121265_);
	public static final BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> SUGGEST_NOTHING = (p_121363_, p_121364_) -> p_121363_.buildFuture();
	private static final char SYNTAX_OPTIONS_START = '[';
	private static final char SYNTAX_OPTIONS_END = ']';
	private static final char SYNTAX_OPTIONS_SEPARATOR = ',';
	private static final char SELECTOR_BUILDING = 'b';
	private final StringReader reader;
	private int maxResults;
	private String rotation;
	private MinMaxBounds.Doubles distance = MinMaxBounds.Doubles.ANY;
	@Nullable
	private Double x;
	@Nullable
	private Double y;
	@Nullable
	private Double z;
	@Nullable
	private Double deltaX;
	@Nullable
	private Double deltaY;
	@Nullable
	private Double deltaZ;
	private BiConsumer<Vec3, List<? extends BuildingPlacement>> order = BuildingSelector.ORDER_ARBITRARY;
	private BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;
	private boolean isLimited;
	private boolean isSorted;
	@Nullable
	private String buildingName;
	private boolean usesSelectors;
	
	public BuildingSelectorParser(StringReader pReader) {
		this.reader = pReader;
	}
	
	public static double distanceToSqr(BuildingPlacement pBuildingPlacement, Vec3 pVec2) {
		Vec3 pVec = pBuildingPlacement.originPos.getCenter();
		double d0 = pVec.x - pVec2.x;
		double d1 = pVec.y - pVec2.y;
		double d2 = pVec.z - pVec2.z;
		return d0 * d0 + d1 * d1 + d2 * d2;
	}
	
	public BuildingSelector getSelector() {
		AABB aabb;
		if (this.deltaX == null && this.deltaY == null && this.deltaZ == null) {
			if (this.distance.getMax() != null) {
				double d0 = this.distance.getMax();
				aabb = new AABB(-d0, -d0, -d0, d0 + 1.0D, d0 + 1.0D, d0 + 1.0D);
			} else {
				aabb = null;
			}
		} else {
			aabb = this.createAabb(this.deltaX == null ? 0.0D : this.deltaX, this.deltaY == null ? 0.0D : this.deltaY, this.deltaZ == null ? 0.0D : this.deltaZ);
		}
		
		Function<Vec3, Vec3> function;
		if (this.x == null && this.y == null && this.z == null) {
			function = (p_121292_) -> p_121292_;
		} else {
			function = (p_121258_) -> new Vec3(this.x == null ? p_121258_.x : this.x, this.y == null ? p_121258_.y : this.y, this.z == null ? p_121258_.z : this.z);
		}
		
		return new BuildingSelector(this.maxResults, function, aabb, this.order, this.buildingName, this.usesSelectors, rotation);
	}
	
	private AABB createAabb(double pSizeX, double pSizeY, double pSizeZ) {
		boolean flag = pSizeX < 0.0D;
		boolean flag1 = pSizeY < 0.0D;
		boolean flag2 = pSizeZ < 0.0D;
		double d0 = flag ? pSizeX : 0.0D;
		double d1 = flag1 ? pSizeY : 0.0D;
		double d2 = flag2 ? pSizeZ : 0.0D;
		double d3 = (flag ? 0.0D : pSizeX) + 1.0D;
		double d4 = (flag1 ? 0.0D : pSizeY) + 1.0D;
		double d5 = (flag2 ? 0.0D : pSizeZ) + 1.0D;
		return new AABB(d0, d1, d2, d3, d4, d5);
	}
	
	protected void parseSelector() throws CommandSyntaxException {
		this.usesSelectors = true;
		this.suggestions = this::suggestSelector;
		if (!this.reader.canRead()) {
			throw ERROR_MISSING_SELECTOR_TYPE.createWithContext(this.reader);
		} else {
			int i = this.reader.getCursor();
			char c0 = this.reader.read();
			if (c0 == 'p') {
				this.maxResults = 1;
				this.order = ORDER_NEAREST;
			} else if (c0 == 'r') {
				this.maxResults = 1;
				this.order = ORDER_RANDOM;
			} else {
				if (c0 != 'b') {
					this.reader.setCursor(i);
					throw ERROR_UNKNOWN_SELECTOR_TYPE.createWithContext(this.reader, "@" + c0);
				}
				this.maxResults = Integer.MAX_VALUE;
				this.order = BuildingSelector.ORDER_ARBITRARY;
			}
			this.suggestions = this::suggestOpenOptions;
			if (this.reader.canRead() && this.reader.peek() == '[') {
				this.reader.skip();
				this.suggestions = this::suggestOptionsKeyOrClose;
				this.parseOptions();
				
			}
		}
	}
	
	public void parseOptions() throws CommandSyntaxException {
		this.suggestions = this::suggestOptionsKey;
		this.reader.skipWhitespace();
		
		while (true) {
			if (this.reader.canRead() && this.reader.peek() != ']') {
				this.reader.skipWhitespace();
				int i = this.reader.getCursor();
				String s = this.reader.readString();
				BuildingSelectorOptions.Modifier buildingPlacementselectoroptions$modifier = BuildingSelectorOptions.get(this, s, i);
				this.reader.skipWhitespace();
				if (!this.reader.canRead() || this.reader.peek() != '=') {
					this.reader.setCursor(i);
					throw ERROR_EXPECTED_OPTION_VALUE.createWithContext(this.reader, s);
				}
				
				this.reader.skip();
				this.reader.skipWhitespace();
				this.suggestions = SUGGEST_NOTHING;
				buildingPlacementselectoroptions$modifier.handle(this);
				this.reader.skipWhitespace();
				this.suggestions = this::suggestOptionsNextOrClose;
				if (!this.reader.canRead()) {
					continue;
				}
				
				if (this.reader.peek() == ',') {
					this.reader.skip();
					this.suggestions = this::suggestOptionsKey;
					continue;
				}
				
				if (this.reader.peek() != ']') {
					throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(this.reader);
				}
			}
			
			if (this.reader.canRead()) {
				this.reader.skip();
				this.suggestions = SUGGEST_NOTHING;
				return;
			}
			
			throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(this.reader);
		}
	}
	
	
	public StringReader getReader() {
		return this.reader;
	}
	
	
	public MinMaxBounds.Doubles getDistance() {
		return this.distance;
	}
	
	public void setDistance(MinMaxBounds.Doubles pDistance) {
		this.distance = pDistance;
	}
	
	@Nullable
	public Double getX() {
		return this.x;
	}
	
	public void setX(double pX) {
		this.x = pX;
	}
	
	@Nullable
	public Double getY() {
		return this.y;
	}
	
	public void setY(double pY) {
		this.y = pY;
	}
	
	@Nullable
	public Double getZ() {
		return this.z;
	}
	
	public void setZ(double pZ) {
		this.z = pZ;
	}
	
	@Nullable
	public Double getDeltaX() {
		return this.deltaX;
	}
	
	public void setDeltaX(double pDeltaX) {
		this.deltaX = pDeltaX;
	}
	
	@Nullable
	public Double getDeltaY() {
		return this.deltaY;
	}
	
	public void setDeltaY(double pDeltaY) {
		this.deltaY = pDeltaY;
	}
	
	@Nullable
	public Double getDeltaZ() {
		return this.deltaZ;
	}
	
	public void setDeltaZ(double pDeltaZ) {
		this.deltaZ = pDeltaZ;
	}
	
	public void setMaxResults(int pMaxResults) {
		this.maxResults = pMaxResults;
	}
	
	public void setOrder(BiConsumer<Vec3, List<? extends BuildingPlacement>> pOrder) {
		this.order = pOrder;
	}
	
	public BuildingSelector parse() throws CommandSyntaxException {
		this.suggestions = this::suggestNameOrSelector;
		if (this.reader.canRead() && this.reader.peek() == '@') {
			this.reader.skip();
			this.parseSelector();
		}
		return this.getSelector();
	}
	
	private CompletableFuture<Suggestions> suggestNameOrSelector(SuggestionsBuilder p_121287_, Consumer<SuggestionsBuilder> p_121288_) {
		p_121288_.accept(p_121287_);
		p_121287_.suggest("@b", Component.translatable("argument.buildingPlacement.selector.allPlayer"));
		p_121287_.suggest("@r", Component.translatable("argument.buildingPlacement.selector.randomPlayer"));
		p_121287_.suggest("@p", Component.translatable("argument.buildingPlacement.selector.nearestPlayer"));
		return p_121287_.buildFuture();
	}
	
	
	private CompletableFuture<Suggestions> suggestSelector(SuggestionsBuilder p_121323_, Consumer<SuggestionsBuilder> p_121324_) {
		SuggestionsBuilder suggestionsbuilder = p_121323_.createOffset(p_121323_.getStart() - 1);
		suggestionsbuilder.suggest("@b", Component.translatable("argument.buildingPlacement.selector.allPlayer"));
		suggestionsbuilder.suggest("@r", Component.translatable("argument.buildingPlacement.selector.randomPlayer"));
		suggestionsbuilder.suggest("@p", Component.translatable("argument.buildingPlacement.selector.nearestPlayer"));
		p_121323_.add(suggestionsbuilder);
		return p_121323_.buildFuture();
	}
	
	private CompletableFuture<Suggestions> suggestOpenOptions(SuggestionsBuilder p_121334_, Consumer<SuggestionsBuilder> p_121335_) {
		p_121334_.suggest(String.valueOf('['));
		return p_121334_.buildFuture();
	}
	
	private CompletableFuture<Suggestions> suggestOptionsKeyOrClose(SuggestionsBuilder p_121342_, Consumer<SuggestionsBuilder> p_121343_) {
		p_121342_.suggest(String.valueOf(']'));
		BuildingSelectorOptions.suggestNames(this, p_121342_);
		return p_121342_.buildFuture();
	}
	
	private CompletableFuture<Suggestions> suggestOptionsKey(SuggestionsBuilder p_121348_, Consumer<SuggestionsBuilder> p_121349_) {
		BuildingSelectorOptions.suggestNames(this, p_121348_);
		return p_121348_.buildFuture();
	}
	
	private CompletableFuture<Suggestions> suggestOptionsNextOrClose(SuggestionsBuilder p_121354_, Consumer<SuggestionsBuilder> p_121355_) {
		p_121354_.suggest(String.valueOf(','));
		p_121354_.suggest(String.valueOf(']'));
		return p_121354_.buildFuture();
	}
	
	
	public void setSuggestions(BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> pSuggestionHandler) {
		this.suggestions = pSuggestionHandler;
	}
	
	public CompletableFuture<Suggestions> fillSuggestions(SuggestionsBuilder pBuilder, Consumer<SuggestionsBuilder> pConsumer) {
		return this.suggestions.apply(pBuilder.createOffset(this.reader.getCursor()), pConsumer);
	}
	
	
	public boolean isLimited() {
		return this.isLimited;
	}
	
	public void setLimited(boolean pIsLimited) {
		this.isLimited = pIsLimited;
	}
	
	public boolean isSorted() {
		return this.isSorted;
	}
	
	public void setSorted(boolean pIsSorted) {
		this.isSorted = pIsSorted;
	}
	
	public void limitToType(String pBuildingName) {
		this.buildingName = pBuildingName;
	}
	
	public boolean isTypeLimited() {
		return this.buildingName != null;
	}
	
	public String getRotation() {
		return rotation;
	}
	
	public void setRotation(String rotation) {
		this.rotation = rotation;
	}
}
