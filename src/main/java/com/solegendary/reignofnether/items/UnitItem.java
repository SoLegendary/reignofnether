package com.solegendary.reignofnether.items;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.blocks.RangeIndicator;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.buttons.UnitItemInventoryButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

// items that can be held and used by RTS units, especially heroes
// they are still registered as actual Minecraft items
// being a vanilla MC item or a RoN custom item does not determine whether you are a UnitItem, eg.
//    - vanilla golden apple ✔
//    - vanilla dirt block ❌
//    - vanilla fire aspect trident ✔
//    - RoN mana potion ✔
//    - Ron RTS Start Block ❌

// construct via UnitItemBuilder, eg. UnitItemBuilder.of(Items.IRON_SWORD).sellValue(25).build()

public abstract class UnitItem implements RangeIndicator {

    public static final boolean ENABLED = false;

    public static final String RON$COOLDOWN_KEY = "reignofnether:CooldownEndTick";

    protected final Item item;
    public final int defaultStackCount;
    public final UUID uuid;
    public final ResourceLocation iconRl;
    public final UnitItemType type;
    public final int sellValue;
    public final int buyCost;
    public final String desc;
    public final Keybinding hotkey;
    public boolean enableTooltip;
    protected final List<Pair<Enchantment, Integer>> enchantments;
    protected final List<String> pointDescs;
    public final HashMap<Attribute, AttributeModifier> attributes;
    public BiPredicate<Unit, BlockPos> onUseGround;
    public BiPredicate<Unit, LivingEntity> onUseEntity;
    public BiPredicate<Unit, BuildingPlacement> onUseBuilding;
    public Predicate<Unit> onUse;
    public final boolean consumeOnUse;
    public int manaCost;
    public int cooldownTicksMax;
    public int channelTicks;
    public float range;
    public float radius;
    public boolean showRangeCircle;
    public boolean showRangeLine;
    public boolean showRadiusCircle;
    public boolean suppressDefaultError;

    private Set<BlockPos> highlightBps = new HashSet<>();

    @Override public Set<BlockPos> getHighlightBps() { return highlightBps; }
    @Override public void setHighlightBps(Set<BlockPos> bps) { highlightBps = bps; }

    protected UnitItem(UnitItemBuilder builder) {
        this.item = builder.item;
        this.defaultStackCount = builder.defaultStackCount;
        this.uuid = builder.uuid;
        this.iconRl = builder.iconRl;
        this.type = builder.type;
        this.sellValue = builder.sellValue;
        this.buyCost = builder.buyCost;
        this.desc = builder.desc;
        this.hotkey = builder.hotkey;
        this.enchantments = List.copyOf(builder.enchantments);
        this.pointDescs = List.copyOf(builder.pointDescs);
        this.enableTooltip = builder.enableTooltip;
        this.attributes = builder.attributes;
        this.onUseGround = builder.onUseGround;
        this.onUseEntity = builder.onUseEntity;
        this.onUseBuilding = builder.onUseBuilding;
        this.onUse = builder.onUse;
        this.consumeOnUse = builder.consumeOnUse;
        this.manaCost = builder.manaCost;
        this.cooldownTicksMax = builder.cooldownTicksMax;
        this.channelTicks = builder.channelTicks;
        this.range = builder.range;
        this.radius = builder.radius;
        this.showRangeCircle = builder.showRangeCircle;
        this.showRangeLine = builder.showRangeLine;
        this.showRadiusCircle = builder.showRadiusCircle;
        this.suppressDefaultError = builder.suppressDefaultError;
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
        itemStack.setCount(defaultStackCount);
        return itemStack;
    }

    public UnitItemInventoryButton getInventoryButton(int index, ItemStack itemStack, Unit unit, Keybinding hotkey) {
        return new UnitItemInventoryButton(index, this, itemStack, unit, hotkey);
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