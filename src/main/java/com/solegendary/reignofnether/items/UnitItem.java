package com.solegendary.reignofnether.items;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.buttons.UnitItemButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
    public final int sellValue;
    public final int buyCost;
    public final String desc;
    public final Keybinding hotkey;
    public boolean enableTooltip;
    protected final List<Pair<Enchantment, Integer>> enchantments;
    protected final List<String> pointDescs;
    public final List<AttributeModifier> getAttributeModifiers;
    public final Consumer<BlockPos> onUseGround;
    public final Consumer<LivingEntity> onUseEntity;
    public final Consumer<BuildingPlacement> onUseBuilding;
    public final Runnable onUse;

    protected UnitItem(UnitItemBuilder builder) {
        this.item = builder.item;
        this.iconRl = builder.iconRl;
        this.type = builder.type;
        this.sellValue = builder.sellValue;
        this.buyCost = builder.buyCost;
        this.desc = builder.desc;
        this.hotkey = builder.hotkey;
        this.enchantments = List.copyOf(builder.enchantments);
        this.pointDescs = List.copyOf(builder.pointDescs);
        this.enableTooltip = builder.enableTooltip;
        this.getAttributeModifiers = builder.getAttributeModifiers;
        this.onUseGround = builder.onUseGround;
        this.onUseEntity = builder.onUseEntity;
        this.onUseBuilding = builder.onUseBuilding;
        this.onUse = builder.onUse;
    }

    public Item getItem() {
        return item;
    }

    public ItemStack getNewItemStack() {
        ItemStack itemStack = new ItemStack(item);
        for (Pair<Enchantment, Integer> pair : enchantments) {
            itemStack.enchant(pair.getFirst(), pair.getSecond());
        }
        itemStack.getOrCreateTag().putUUID("uuid", UUID.randomUUID());
        return itemStack;
    }

    public UnitItemButton getButton(int index, ItemStack itemStack, Unit unit) {
        return new UnitItemButton(index, this, itemStack, unit);
    }

    public Component getName() {
        return new ItemStack(item).getHoverName();
    }

    public String getDescription() {
        return desc;
    }

    /** One string per bullet in the tooltip's passive stat list. */
    public List<String> getPointDescs() {
        List<String> lines = new ArrayList<>();
        for (String desc : pointDescs)
            if (!desc.isBlank())
                lines.add(desc);
        return lines;
    }

    // tooltip rendered when mousing over a ground item entity
    public List<FormattedCharSequence> getEntityTooltip(ItemStack itemStack) {
        return List.of();
    }
}