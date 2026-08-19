package com.solegendary.reignofnether.items;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.hud.buttons.UnitItemButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// items that can be held and used by RTS units, especially heroes
// they are still registered as actual Minecraft items
// being a vanilla MC item or a RoN custom item does not determine whether you are a UnitItem, eg.
//    - vanilla golden apple ✔
//    - vanilla dirt block ❌
//    - vanilla fire aspect trident ✔
//    - RoN mana potion ✔
//    - Ron RTS Start Block ❌

// construct via UnitItemBuilder, eg. UnitItemBuilder.of(Items.IRON_SWORD).sellValue(25).build()

public abstract class UnitItem {

    protected final Item item;
    public final ResourceLocation iconRl;
    public final UnitItemType type;
    public final int stackQty;
    public final int sellValue;
    public final String descKey;
    public final Keybinding hotkey;

    protected final List<Pair<Enchantment, Integer>> enchantments;
    protected final List<String> bonusKeys;

    private final boolean canUnitPickup;
    private final boolean canUnitAutopickup;

    // created lazily: building it in the constructor would leak a partially
    // constructed 'this' to UnitItemButton, which calls back into getItemStack()
    private UnitItemButton button = null;

    protected UnitItem(UnitItemBuilder builder) {
        this.item = builder.item;
        this.iconRl = builder.iconRl;
        this.type = builder.type;
        this.stackQty = builder.stackQty;
        this.sellValue = builder.sellValue;
        this.descKey = builder.descKey;
        this.hotkey = builder.hotkey;
        this.enchantments = List.copyOf(builder.enchantments);
        this.bonusKeys = List.copyOf(builder.bonusKeys);
        this.canUnitPickup = builder.canUnitPickup;
        this.canUnitAutopickup = builder.canUnitAutopickup;
    }

    public UnitItemButton getButton() {
        if (button == null)
            button = new UnitItemButton(this);
        return button;
    }

    public Component getName() {
        return new ItemStack(item).getHoverName();
    }

    @Nullable
    public String getDescription() {
        return descKey.isBlank() ? null : I18n.get(descKey);
    }

    /** One string per bullet in the tooltip's passive stat list. */
    public List<String> getBonusLines() {
        List<String> lines = new ArrayList<>();
        for (String key : bonusKeys)
            lines.add(I18n.get(key));
        return lines;
    }

    public ItemStack getItemStack() {
        ItemStack stack = new ItemStack(item);
        for (Pair<Enchantment, Integer> pair : enchantments) {
            stack.enchant(pair.getFirst(), pair.getSecond());
        }
        stack.setCount(stackQty);
        return stack;
    }

    public boolean canUnitPickup() {
        return canUnitPickup;
    }

    // usually for stuff like resources and piglin merchant loot
    public boolean canUnitAutopickup() {
        return canUnitAutopickup;
    }

    // legacy flat tooltip; UnitItemButton renders the banded tooltip instead
    public List<FormattedCharSequence> getTooltip() {
        return List.of();
    }

    // TODO:
    // - consume()
    // - onUse() // (on left click release)
    // - drop()
    // - applyAttributeModifiers()


}