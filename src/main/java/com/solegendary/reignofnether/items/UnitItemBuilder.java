package com.solegendary.reignofnether.items;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
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
    ResourceLocation iconRl = null;
    UnitItemType type = UnitItemType.PASSIVE;
    int sellValue = 0;
    int buyCost = 0;
    String desc = "";
    Keybinding hotkey = null;
    boolean enableTooltip = true;
    final List<Pair<Enchantment, Integer>> enchantments = new ArrayList<>();
    final List<String> pointDescs = new ArrayList<>();
    final List<AttributeModifier> getAttributeModifiers = new ArrayList<>();
    BiPredicate<Unit, BlockPos> onUseGround = null;
    BiPredicate<Unit, LivingEntity> onUseEntity = null;
    BiPredicate<Unit, BuildingPlacement> onUseBuilding = null;
    Predicate<Unit> onUse = null;
    boolean consumeOnUse = false;

    private UnitItemBuilder(Item item) {
        if (item == null)
            throw new IllegalArgumentException("UnitItemBuilder requires a non-null Item");
        this.item = item;
    }

    public static UnitItemBuilder of(Item item) {
        return new UnitItemBuilder(item);
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
        this.buyCost = buyCost;
        return this;
    }

    /** I18n key for the short description line(s) in the tooltip's middle band. */
    public UnitItemBuilder desc(String desc) {
        this.desc = desc == null ? "" : desc;
        return this;
    }

    /** Adds one bullet to the passive stat list; call once per bullet, in display order. */
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
    public UnitItemBuilder attributeModifier(AttributeModifier modifier) {
        if (modifier != null)
            this.getAttributeModifiers.add(modifier);
        return this;
    }

    public UnitItemBuilder attributeModifiers(AttributeModifier... modifiers) {
        for (AttributeModifier modifier : modifiers)
            attributeModifier(modifier);
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