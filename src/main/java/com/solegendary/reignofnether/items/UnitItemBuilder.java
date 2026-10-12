package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.Holder;
import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.items.UnitItemType;
import com.solegendary.reignofnether.items.UnitItemBuilder;
import com.solegendary.reignofnether.items.UnitItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import net.minecraft.client.resources.language.I18n;

/**
 * Fluent builder for UnitItems.
 *
 * Two ways to use it:
 *
 *  1. build() a plain UnitItem with no custom behaviour:
 *
 *      UnitItem sword = UnitItemBuilder.of(Items.IRON_SWORD)
 *          .type(UnitItemType.PASSIVE)
 *          .descKey("item.reignofnether.iron_sword.desc")
 *          .bonus("item.reignofnether.iron_sword.bonus1")
 *          .sellValue(25)
 *          .build();
 *
 *  2. pass it to super() from a subclass that overrides consume()/onUse()/etc:
 *
 *      public class ManaPotion extends UnitItem {
 *          public ManaPotion() {
 *              super(UnitItemBuilder.of(ItemRegistrar.MANA_POTION.get())
 *                  .type(UnitItemType.CONSUMABLE)
 *                  .sellValue(10));
 *          }
 *      }
 */
public class UnitItemBuilder {

    final Item item;
    int defaultStackCount = 1;
    UUID uuid = UUID.randomUUID();
    /** Stable id used for the default icon texture and the default description key. */
    String descId;
    Rarity rarity = Rarity.COMMON;
    boolean toggleActiveOnUse = false;
    boolean showRadiusAtCursor = false;
    boolean doCastAnimation = false;
    boolean resetBehaviours = true;
    boolean forceAutocast = false;
    boolean canRandomDrop = true;
    boolean nonHeroUseable = false;
    int maxStackSize = 1;
    ResourceLocation iconRl = null;
    UnitItemType type = UnitItemType.PASSIVE;
    int sellValue = 0;
    int buyCost = 0;
    String desc = "";
    Keybinding hotkey = null;
    boolean enableTooltip = true;
    final List<Pair<Holder<Enchantment>, Integer>> enchantments = new ArrayList<>();
    final List<String> pointDescs = new ArrayList<>();
    final HashMap<Attribute, AttributeModifier> attributes = new HashMap<>();

    // AttributeModifier ids are ResourceLocations in 1.21.1; these are throwaway per-item
    // modifiers, so a random namespace-and-path pair is the right analogue of the old random UUID.
    private static ResourceLocation randomModifierId() {
        return ResourceLocation.fromNamespaceAndPath(
                ReignOfNether.MOD_ID, UUID.randomUUID().toString());
    }
    BiPredicate<Unit, BlockPos> onUseGround = null;
    BiPredicate<Unit, LivingEntity> onUseEntity = null;
    BiPredicate<Unit, BuildingPlacement> onUseBuilding = null;
    Predicate<Unit> onUse = null;
    boolean suppressDefaultError = false;
    boolean consumeOnUse = false;
    int cooldownTicksMax = 0;
    int channelTicks = 0;
    int manaCost = 0;
    float range = 0;
    float radius = 0;
    boolean showRangeCircle = true;
    boolean showRangeLine = false;
    boolean showRadiusCircle = false;

    private UnitItemBuilder(Item item) {
        if (item == null)
            throw new IllegalArgumentException("UnitItemBuilder requires a non-null Item");
        this.item = item;
    }

    public static UnitItemBuilder of(Item item) {
        return new UnitItemBuilder(item);
    }

    public UnitItemBuilder uuid(String uuid) {
        this.uuid = UUID.fromString(uuid);
        return this;
    }

    /** Emerald cost returned when the item is sold; 0 means unsellable. */
    public UnitItemBuilder defaultStackCount(int defaultStackCount) {
        if (defaultStackCount < 1)
            throw new IllegalArgumentException("sellValue must be >= 1, was " + defaultStackCount);
        this.defaultStackCount = defaultStackCount;
        return this;
    }

    /** Optional override icon; if null the button renders the ItemStack itself. */
    /** Sets the item id; the icon and description key default to being derived from it. */
    public UnitItemBuilder descId(String descId) {
        this.descId = descId;
        return this;
    }

    /** Caps how many of this item a player may hold; units ignore it. */
    public UnitItemBuilder maxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    /** Sets the rarity and derives the emerald buy cost (and half of it as the sell value). */
    public UnitItemBuilder rarity(Rarity rarity) {
        this.rarity = rarity;
        this.buyCost = UnitItem.RARITY_VALUES.get(rarity);
        this.sellValue = UnitItem.RARITY_VALUES.get(rarity) / 2;
        return this;
    }

    public UnitItemBuilder toggleActiveOnUse() {
        this.toggleActiveOnUse = true;
        return this;
    }

    public UnitItemBuilder showRadiusAtCursor() {
        this.showRadiusAtCursor = true;
        return this;
    }

    public UnitItemBuilder doCastAnimation() {
        this.doCastAnimation = true;
        return this;
    }

    /** Opts the unit out of the behaviour reset that normally happens on spawn. */
    public UnitItemBuilder noBehaviourReset() {
        this.resetBehaviours = false;
        return this;
    }

    public UnitItemBuilder forceAutocast() {
        this.forceAutocast = true;
        return this;
    }

    public UnitItemBuilder noRandomDrop() {
        this.canRandomDrop = false;
        return this;
    }

    public UnitItemBuilder icon(@Nullable ResourceLocation iconRl) {
        this.iconRl = iconRl;
        return this;
    }

    public UnitItemBuilder type(UnitItemType type) {
        this.type = type;
        return this;
    }

    /** Emerald cost returned when the item is sold; 0 means unsellable. */
    public UnitItemBuilder sellValue(int sellValue) {
        if (sellValue < 0)
            throw new IllegalArgumentException("sellValue must be >= 0, was " + sellValue);
        this.sellValue = sellValue;
        return this;
    }

    /** Emerald cost at shops */
    public UnitItemBuilder buyCost(int buyCost) {
        if (sellValue < 0)
            throw new IllegalArgumentException("sellValue must be >= 0, was " + sellValue);
        this.buyCost = buyCost;
        return this;
    }

    public UnitItemBuilder cooldownTicks(int cooldownTicks) {
        if (cooldownTicks < 0)
            throw new IllegalArgumentException("cooldownTicks must be >= 0, was " + cooldownTicks);
        this.cooldownTicksMax = cooldownTicks;
        return this;
    }

    public UnitItemBuilder channelTicks(int channelTicks) {
        if (channelTicks < 0)
            throw new IllegalArgumentException("channelTicks must be >= 0, was " + channelTicks);
        this.channelTicks = channelTicks;
        return this;
    }

    public UnitItemBuilder manaCost(int manaCost) {
        if (manaCost < 0)
            throw new IllegalArgumentException("manaCost must be >= 0, was " + manaCost);
        this.manaCost = manaCost;
        return this;
    }

    public UnitItemBuilder range(float range) {
        if (range < 0)
            throw new IllegalArgumentException("range must be >= 0, was " + range);
        this.range = range;
        return this;
    }

    public UnitItemBuilder radius(int radius) {
        if (radius < 0)
            throw new IllegalArgumentException("radius must be >= 0, was " + radius);
        this.radius = radius;
        return this;
    }

    /** I18n key for the short description line(s) in the tooltip's middle band. */
    public UnitItemBuilder desc(String desc) {
        this.desc = desc == null ? "" : desc;
        return this;
    }

    public UnitItemBuilder suppressDefaultError(boolean suppressDefaultError) {
        this.suppressDefaultError = suppressDefaultError;
        return this;
    }

    /**
     * Adds one bullet to the passive stat list.
     *
     * <p>1.5.0 passes format arguments for bullets that interpolate a count. This port keeps the
     * list as plain localisation keys, so the key is stored and the arguments are dropped - the
     * bullet still shows, without the interpolated number.
     */
    public UnitItemBuilder pointDesc(String i18nKey, Object... args) {
        if (i18nKey != null && !i18nKey.isBlank())
            this.pointDescs.add(i18nKey);
        return this;
    }

    /** 1.5.0 uses this as a switch: no argument means "yes". */
    public UnitItemBuilder suppressDefaultError() {
        this.suppressDefaultError = true;
        return this;
    }

    public UnitItemBuilder pointDesc(String i18nKey) {
        if (i18nKey != null && !i18nKey.isBlank())
            this.pointDescs.add(i18nKey);
        return this;
    }

    public UnitItemBuilder pointDescs(String... descs) {
        for (String desc : descs)
            pointDesc(desc);
        return this;
    }

    public UnitItemBuilder enableTooltip(boolean enable) {
        this.enableTooltip = enable;
        return this;
    }

    public UnitItemBuilder enchant(Holder<Enchantment> enchantment, int level) {
        this.enchantments.add(Pair.of(enchantment, level));
        return this;
    }

    /** Shown bottom-right of the tooltip and used by the button's key handler. */
    public UnitItemBuilder hotkey(@Nullable Keybinding hotkey) {
        this.hotkey = hotkey;
        return this;
    }

    /** Adds one attribute modifier applied while the item is held; call once per modifier. */
    /**
     * 1.21.1 exposes vanilla attributes as Holder<Attribute>, and the 1.5.0 unit items are
     * written against that - the Holder overload keeps those call sites unchanged.
     */
    public UnitItemBuilder attribute(Holder<Attribute> attribute, double amount,
                                      AttributeModifier.Operation operation) {
        this.attributes.put(attribute.value(), new AttributeModifier(randomModifierId(), amount, operation));
        return this;
    }

    public UnitItemBuilder attribute(Attribute attribute, double amount, AttributeModifier.Operation operation) {
        this.attributes.put(attribute, new AttributeModifier(randomModifierId(), amount, operation));
        return this;
    }

    public UnitItemBuilder attribute(Attribute attribute, double amount) {
        this.attributes.put(attribute, new AttributeModifier(randomModifierId(), amount, AttributeModifier.Operation.ADD_VALUE));
        return this;
    }

    public UnitItemBuilder onUseGround(BiPredicate<Unit, BlockPos> onUseGround) {
        this.onUseGround = onUseGround;
        return this;
    }

    public UnitItemBuilder onUseEntity(BiPredicate<Unit, LivingEntity> onUseEntity) {
        this.onUseEntity = onUseEntity;
        return this;
    }

    public UnitItemBuilder onUseBuilding(BiPredicate<Unit, BuildingPlacement> onUseBuilding) {
        this.onUseBuilding = onUseBuilding;
        return this;
    }

    public UnitItemBuilder onUse(Predicate<Unit> onUse) {
        this.onUse = onUse;
        return this;
    }

    public UnitItemBuilder consumeOnUse() {
        this.consumeOnUse = true;
        return this;
    }

    public UnitItemBuilder showRangeLine() {
        this.showRangeLine = true;
        return this;
    }

    public UnitItemBuilder showRadiusCircle() {
        this.showRadiusCircle = true;
        return this;
    }

    public UnitItemBuilder showRangeCircle(boolean show) {
        this.showRangeCircle = show;
        return this;
    }

    public UnitItemBuilder showRangeLine(boolean show) {
        this.showRangeLine = show;
        return this;
    }

    public UnitItemBuilder showRadiusCircle(boolean show) {
        this.showRadiusCircle = show;
        return this;
    }

    public UnitItemBuilder showRangeCircle() {
        this.showRangeCircle = true;
        return this;
    }

    /** 1.5.0: lets non-hero units activate the item (see {@link UnitItem#nonHeroUseable}). */
    public UnitItemBuilder nonHeroUseable() {
        this.nonHeroUseable = true;
        return this;
    }

    public UnitItem build() {
        return new BuiltUnitItem(this);
    }

    /** Concrete UnitItem with default behaviour, produced by build(). */
    private static class BuiltUnitItem extends UnitItem {
        private BuiltUnitItem(UnitItemBuilder builder) {
            super(builder);
        }
    }
}