package com.solegendary.reignofnether.items;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
 *                  .stackQty(3)
 *                  .sellValue(10));
 *          }
 *      }
 */
public class UnitItemBuilder {

    final Item item;
    ResourceLocation iconRl = null;
    UnitItemType type = UnitItemType.PASSIVE;
    int stackQty = 1;
    int sellValue = 0;
    String descKey = "";
    Keybinding hotkey = null;
    boolean canUnitPickup = true;
    boolean canUnitAutopickup = false;
    final List<Pair<Enchantment, Integer>> enchantments = new ArrayList<>();
    final List<String> bonusKeys = new ArrayList<>();

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

    public UnitItemBuilder stackQty(int stackQty) {
        if (stackQty < 1)
            throw new IllegalArgumentException("stackQty must be >= 1, was " + stackQty);
        this.stackQty = stackQty;
        return this;
    }

    /** Emerald cost returned when the item is sold; 0 means unsellable. */
    public UnitItemBuilder sellValue(int sellValue) {
        if (sellValue < 0)
            throw new IllegalArgumentException("sellValue must be >= 0, was " + sellValue);
        this.sellValue = sellValue;
        return this;
    }

    /** I18n key for the short description line(s) in the tooltip's middle band. */
    public UnitItemBuilder descKey(String descKey) {
        this.descKey = descKey == null ? "" : descKey;
        return this;
    }

    /** Adds one bullet to the passive stat list; call once per bullet, in display order. */
    public UnitItemBuilder bonus(String i18nKey) {
        if (i18nKey != null && !i18nKey.isBlank())
            this.bonusKeys.add(i18nKey);
        return this;
    }

    public UnitItemBuilder bonuses(String... i18nKeys) {
        for (String key : i18nKeys)
            bonus(key);
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

    public UnitItemBuilder canUnitPickup(boolean canUnitPickup) {
        this.canUnitPickup = canUnitPickup;
        return this;
    }

    /** Usually true for resources and piglin merchant loot. */
    public UnitItemBuilder canUnitAutopickup(boolean canUnitAutopickup) {
        this.canUnitAutopickup = canUnitAutopickup;
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