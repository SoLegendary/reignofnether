package com.solegendary.reignofnether.items;

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
    String descId;
    ResourceLocation iconRl = null;
    UnitItemType type = UnitItemType.PASSIVE;
    int sellValue = 0;
    int buyCost = 0;
    LocalizedText desc = null;
    Keybinding hotkey = null;
    boolean enableTooltip = true;
    final List<Pair<Enchantment, Integer>> enchantments = new ArrayList<>();
    final List<LocalizedText> pointDescs = new ArrayList<>();
    final HashMap<Attribute, AttributeModifier> attributes = new HashMap<>();
    BiPredicate<Unit, BlockPos> onUseGround = null;
    BiPredicate<Unit, LivingEntity> onUseEntity = null;
    BiPredicate<Unit, BuildingPlacement> onUseBuilding = null;
    Predicate<Unit> onUse = null;
    boolean toggleActiveOnUse = false;
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
    boolean doCastAnimation = false;
    boolean resetBehaviours = true;

    private UnitItemBuilder(Item item) {
        if (item == null)
            throw new IllegalArgumentException("UnitItemBuilder requires a non-null Item");
        this.item = item;
    }

    public static UnitItemBuilder of(Item item) {
        return new UnitItemBuilder(item);
    }

    public UnitItemBuilder descId(String descId) {
        this.descId = descId;
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
    public UnitItemBuilder desc(String i18nKey, Object... args) {
        if (i18nKey != null && !i18nKey.isBlank())
            this.desc = new LocalizedText(i18nKey, args);
        return this;
    }

    public UnitItemBuilder toggleActiveOnUse() {
        this.toggleActiveOnUse = true;
        return this;
    }

    public UnitItemBuilder suppressDefaultError(boolean suppressDefaultError) {
        this.suppressDefaultError = suppressDefaultError;
        return this;
    }

    /** Adds one bullet to the passive stat list; call once per bullet, in display order. */
    public UnitItemBuilder pointDesc(String i18nKey, Object... args) {
        if (i18nKey != null && !i18nKey.isBlank())
            this.pointDescs.add(new LocalizedText(i18nKey, args));
        return this;
    }

    public UnitItemBuilder enableTooltip(boolean enable) {
        this.enableTooltip = enable;
        return this;
    }

    public UnitItemBuilder enchant(Enchantment enchantment, int level) {
        this.enchantments.add(Pair.of(enchantment, level));
        return this;
    }

    /** Shown bottom-right of the tooltip and used by the button's key handler. */
    public UnitItemBuilder hotkey(@Nullable Keybinding hotkey) {
        this.hotkey = hotkey;
        return this;
    }

    /** Adds one attribute modifier applied while the item is held; call once per modifier. */
    public UnitItemBuilder attribute(Attribute attribute, double amount, AttributeModifier.Operation operation) {
        this.attributes.put(attribute, new AttributeModifier(UUID.randomUUID().toString(), amount, operation));
        return this;
    }

    public UnitItemBuilder attribute(Attribute attribute, double amount) {
        this.attributes.put(attribute, new AttributeModifier(UUID.randomUUID().toString(), amount, AttributeModifier.Operation.ADDITION));
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

    public UnitItemBuilder doCastAnimation() {
        this.doCastAnimation = true;
        return this;
    }

    public UnitItemBuilder noBehaviourReset() {
        this.resetBehaviours = false;
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