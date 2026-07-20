package com.solegendary.reignofnether.items;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

// items that can be held and used by RTS units, especially heroes
// they are still registered as actual Minecraft items
// being a vanilla MC item or a RoN custom item does not determine whether you are a UnitItem, eg.
//    - vanilla golden apple ✔
//    - vanilla dirt block ❌
//    - vanilla fire aspect trident ✔
//    - RoN mana potion ✔
//    - Ron RTS Start Block ❌

public abstract class UnitItem {
    final Item item;
    public final ResourceLocation iconRl;
    protected int stackQty = 1;
    List<Pair<Enchantment, Integer>> enchantments = List.of();

    public UnitItem(Item item, ResourceLocation iconRl) {
        this.item = item;
        this.iconRl = iconRl;
    }

    public UnitItem(Item item, ResourceLocation iconRl, List<Pair<Enchantment, Integer>> enchantments) {
        this.item = item;
        this.iconRl = iconRl;
        this.enchantments = enchantments;
    }

    public UnitItem(Item item, ResourceLocation iconRl, int stackQty) {
        this.item = item;
        this.iconRl = iconRl;
        this.stackQty = stackQty;
    }

    public List<FormattedCharSequence> getTooltip() {
        return List.of();
    }

    public Component getName() {
        return new ItemStack(item).getHoverName();
    }

    public ItemStack getItemStack() {
        ItemStack stack = new ItemStack(item);
        for (Pair<Enchantment, Integer> pair : enchantments) {
            stack.enchant(pair.getFirst(), pair.getSecond());
        }
        stack.setCount(stackQty);
        return stack;
    }

    public Button getButton() {
        return new ButtonBuilder("button_" + item.getDescriptionId())
                .tooltipLines(getTooltip())
                .build();
    }

    public boolean canUnitPickup() {
        return true;
    }

    // usually for stuff like resources and piglin merchant loot
    public boolean canUnitAutopickup() {
        return false;
    }
}
