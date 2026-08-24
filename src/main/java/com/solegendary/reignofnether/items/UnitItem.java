package com.solegendary.reignofnether.items;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.buttons.UnitItemButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
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
    public final String descKey;
    public final Keybinding hotkey;
    public boolean enableTooltip;
    protected final List<Pair<Enchantment, Integer>> enchantments;
    protected final List<String> pointKeys;
    public final boolean canUnitPickup;
    public final boolean canUnitAutopickup;
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
        this.descKey = builder.descKey;
        this.hotkey = builder.hotkey;
        this.enchantments = List.copyOf(builder.enchantments);
        this.pointKeys = List.copyOf(builder.pointKeys);
        this.canUnitPickup = builder.canUnitPickup;
        this.canUnitAutopickup = builder.canUnitAutopickup;
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

    public UnitItemButton getButton(int index, ItemStack itemStack, Unit unit) {
        return new UnitItemButton(index, this, itemStack, unit);
    }

    public Component getName() {
        return new ItemStack(item).getHoverName();
    }

    @Nullable
    public String getDescription() {
        return descKey.isBlank() ? null : I18n.get(descKey);
    }

    /** One string per bullet in the tooltip's passive stat list. */
    public List<String> getPointLines() {
        List<String> lines = new ArrayList<>();
        for (String key : pointKeys)
            lines.add(I18n.get(key));
        return lines;
    }

    // legacy flat tooltip; UnitItemButton renders the banded tooltip instead
    public List<FormattedCharSequence> getTooltip(ItemStack itemStack) {
        return List.of();
    }


}