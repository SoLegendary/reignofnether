package com.solegendary.reignofnether.items;

import net.minecraft.core.Holder;
import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.blocks.RangeIndicator;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.buttons.UnitItemInventoryButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;
import com.solegendary.reignofnether.util.ItemTagCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import com.solegendary.reignofnether.items.UnitItemType;
import com.solegendary.reignofnether.items.UnitItemBuilder;
import com.solegendary.reignofnether.items.UnitItem;
import mcp.client.Start;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

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

    public static final Map<Rarity, Integer> RARITY_VALUES = Map.of(
            Rarity.COMMON, 16,
            Rarity.UNCOMMON, 24,
            Rarity.RARE, 32,
            Rarity.EPIC, 48
    );

    public Rarity rarity;
    public boolean toggleActiveOnUse;
    public boolean showRadiusAtCursor;
    public boolean doCastAnimation;
    public boolean resetBehaviours;
    public boolean forceAutocast;
    public boolean canRandomDrop;
    // 1.5.0: items any non-hero unit may use (replaces the hardcoded NON_UNIT_USEABLE_ITEMS list)
    public boolean nonHeroUseable;
    public int maxStackSize; // only affects units; players can stack any items
    protected final Item item;
    public final String descId;
    public final int defaultStackCount;
    public final UUID uuid;
    public final ResourceLocation iconRl;
    public final UnitItemType type;
    public final int sellValue;
    public final int buyCost;
    public final String desc;
    public final Keybinding hotkey;
    public boolean enableTooltip;
    protected final List<Pair<Holder<Enchantment>, Integer>> enchantments;
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
        this.rarity = builder.rarity;
        this.toggleActiveOnUse = builder.toggleActiveOnUse;
        this.showRadiusAtCursor = builder.showRadiusAtCursor;
        this.doCastAnimation = builder.doCastAnimation;
        this.resetBehaviours = builder.resetBehaviours;
        this.forceAutocast = builder.forceAutocast;
        this.canRandomDrop = builder.canRandomDrop;
        this.maxStackSize = builder.maxStackSize;
        this.nonHeroUseable = builder.nonHeroUseable;

        this.item = builder.item;
        this.defaultStackCount = builder.defaultStackCount;
        // 1.5.0 gives each unit item a descId and derives the icon texture and description key
        // from it. Not every construction path names itself though - MerchantEquipmentItem wraps a
        // vanilla item without saying so - so fall back to the registry path rather than refusing
        // to construct: a wrong-looking icon is recoverable, a static-initializer crash is not.
        this.descId = (builder.descId == null || builder.descId.isBlank())
                ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(builder.item).getPath()
                : builder.descId;
        this.iconRl = builder.iconRl != null
                ? builder.iconRl
                : ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,
                        "textures/item/" + this.descId + ".png");
        this.uuid = builder.uuid;
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
        for (Pair<Holder<Enchantment>, Integer> pair : enchantments) {
            // these holders were captured when this item definition was built, possibly in a world
            // that no longer exists - a dead holder is unsaveable and disconnects whoever the
            // stack is sent to, so resolve them against the registry in use now
            itemStack.enchant(EnchantmentRegistrar.current(pair.getFirst()), pair.getSecond());
        }
        ItemTagCompat.getOrCreateTag(itemStack).putUUID("uuid", UUID.randomUUID());
        itemStack.setCount(defaultStackCount);
        return itemStack;
    }

    public UnitItemInventoryButton getInventoryButton(int index, ItemStack itemStack, Unit unit, Keybinding hotkey) {
        return new UnitItemInventoryButton(index, this, itemStack, unit, hotkey);
    }

    public Component getName() {
        return new ItemStack(item).getHoverName();
    }

    /** 1.5.0: when nothing was declared in code, read item.reignofnether.<descId>.pointN until
     * the first missing index, so a data-only item can still describe itself. */
    private List<String> autoDiscoverPointDescs() {
        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String key = "item.reignofnether." + descId + ".point" + i;
            String resolved = Component.translatable(key).getString();
            if (resolved.equals(key))
                break;
            lines.add(resolved);
        }
        return lines;
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
        if (lines.isEmpty())
            lines.addAll(autoDiscoverPointDescs());
        return lines;
    }

    // tooltip rendered when mousing over a ground item entity
    public List<FormattedCharSequence> getEntityTooltip(ItemStack itemStack) {
        return List.of();
    }
}