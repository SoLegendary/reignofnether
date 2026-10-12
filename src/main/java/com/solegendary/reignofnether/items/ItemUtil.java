package com.solegendary.reignofnether.items;

import java.util.Random;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.registrars.ParticleRegistrar;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.ParticleUtil;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Rarity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import com.solegendary.reignofnether.util.ItemTagCompat;
import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.items.unititems.EdibleFoodItem;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.time.TimeClientEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Holder;
import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.items.ItemUtil;

public class ItemUtil {

    public static final float HEALTH_PER_BREAD = 12;
    public static final float HEALTH_PER_CHICKEN = 18;
    public static final float HEALTH_PER_BEEF = 24;
    public static final float HEAL_PER_NUTRITION = 2.5f;

    public static boolean hasUUID(ItemStack itemStack) {
        return itemStack != null && ItemTagCompat.tag(itemStack) != null && ItemTagCompat.tag(itemStack).hasUUID("uuid");
    }

    public static UUID getUUID(ItemStack itemStack) { // if no uuid, return a random one so we don't crash but just do nothing
        return hasUUID(itemStack) ? ItemTagCompat.tag(itemStack).getUUID("uuid") : UUID.randomUUID();
    }

    public static boolean isUnitItem(ItemStack itemStack) {
        return itemStack != null && getUnitItem(itemStack) != null;
    }

    public static boolean isUnitItem(ItemEntity entity) {
        return entity != null && isUnitItem(entity.getItem());
    }

    /** Looks a unit item up by the plain item it is built from. */
    @Nullable
    public static UnitItem getUnitItem(Item item) {
        if (item == null)
            return null;
        for (UnitItem unitItem : UnitItems.ITEMS)
            if (unitItem.item == item)
                return unitItem;
        return null;
    }

    @Nullable
    public static UnitItem getUnitItem(ItemStack itemStack) {
        if (itemStack == null)
            return null;
        if (isPreparedEdibleFood(itemStack.getItem()))
            return new EdibleFoodItem(itemStack.getItem());
        outerLoop:
        for (UnitItem unitItem : UnitItems.ITEMS) {
            if (unitItem.item == itemStack.getItem()) {
                for (Pair<Holder<Enchantment>, Integer> pair : unitItem.enchantments) {
                    if (itemStack.getEnchantmentLevel(pair.getFirst()) != pair.getSecond())
                        continue outerLoop;
                }
                return unitItem;
            }
        }
        return null;
    }

    @Nullable
    /** Looks a unit item up by its id; 1.5.0's shop and crafting packets refer to items that way. */
    public static UnitItem getUnitItem(String descId) {
        for (UnitItem unitItem : UnitItems.ITEMS)
            if (unitItem.descId.equals(descId))
                return unitItem;
        return null;
    }

    public static UnitItem getUnitItem(UUID uuid) {
        for (UnitItem unitItem : UnitItems.ITEMS)
            if (unitItem.uuid.equals(uuid))
                return unitItem;

        for (Item item : BuiltInRegistries.ITEM) {
            if (isEdible(item) && EdibleFoodItem.getFoodUUID(item).equals(uuid))
                return new EdibleFoodItem(item);
        }
        return null;
    }

    public static Long getCooldownTicksLeft(ItemStack itemStack, Level level) {
        long gameTime = level.isClientSide() ? TimeClientEvents.serverGameTime : level.getGameTime();
        CompoundTag tag = ItemTagCompat.tag(itemStack);
        if (tag != null) {
            return Math.max(0, tag.getLong(UnitItem.RON$COOLDOWN_KEY) - gameTime);
        }
        return 0L;
    }

    private static List<Item> edibleFoods = List.of(
            Items.COOKED_BEEF,
            Items.COOKED_CHICKEN,
            Items.COOKED_COD,
            Items.COOKED_PORKCHOP,
            Items.COOKED_RABBIT,
            Items.COOKED_SALMON,
            Items.COOKED_MUTTON,
            Items.COOKIE,
            Items.BREAD,
            Items.PUMPKIN_PIE,
            Items.MUSHROOM_STEW,
            Items.RABBIT_STEW,
            Items.BEETROOT_SOUP,
            Items.BAKED_POTATO,
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_CARROT
    );

    /**
     * 1.21.1 removed {@code Item#isEdible()}: whether an item is food now lives in the
     * {@link net.minecraft.core.component.DataComponents#FOOD} component, so an empty stack of the
     * item is the closest equivalent to the old per-item query.
     */
        /** The mod's own drinkables; 1.5.0 plays a potion sound when a unit starts drinking one. */
    public static final java.util.List<Item> edibleDrinks = java.util.List.of(
            ItemRegistrar.MANA_POTION.get(),
            ItemRegistrar.HEALTH_POTION.get()
    );

    public static boolean isEdibleDrink(Item item) {
        return edibleDrinks.contains(item);
    }

public static boolean isEdible(Item item) {
        return new ItemStack(item).has(DataComponents.FOOD);
    }

    public static boolean isPreparedEdibleFood(Item item) {
        return isEdible(item) && edibleFoods.contains(item);
    }

    /** 1.5.0: an edible item a unit will actually eat (the mod's own food whitelist). */
    public static boolean isEdibleFood(Item item) {
        return isPreparedEdibleFood(item);
    }

    /** 1.5.0: anything a unit can eat or drink, used by the edible-item pickup checks. */
    public static boolean isEdibleFoodOrDrink(Item item) {
        return isPreparedEdibleFood(item) || isEdibleDrink(item);
    }

    /** 1.5.0: finishing a drink restores health or mana with its particle burst. */
    public static void applyDrinkEffect(Item item, Mob mob) {
        if (item == ItemRegistrar.HEALTH_POTION.get()) {
            mob.heal(UnitItems.HEALTH_POTION_RESTORE_AMOUNT);
            ParticleUtil.addParticleExplosion(ParticleRegistrar.FLOATING_HEART.get(), 5, mob.level(), mob.getEyePosition());

            // 1.5.0: hero's armour turns any health potion into a small group heal
            if (mob instanceof Unit unit1 && mob instanceof UnitInventory inv && inv.isHolding(UnitItems.HEROS_ARMOUR)) {
                List<Mob> mobs = MiscUtil.getEntitiesWithinRange(mob.position(), UnitItems.ARMOUR_AURA_RANGE, Mob.class, mob.level())
                        .stream()
                        .filter(mob2 -> mob2 instanceof Unit unit2 &&
                                mob != mob2 &&
                                mob2.getHealth() < mob2.getMaxHealth() &&
                                AlliancesServerEvents.isAlliedOrOwned(unit1.getOwnerName(), unit2.getOwnerName()))
                        .sorted(Comparator.comparingDouble((Mob m) -> m.getMaxHealth() - m.getHealth()).reversed())
                        .limit(UnitItems.HEROS_ARMOUR_MAX_TARGETS)
                        .toList();
                for (Mob alliedMob : mobs) {
                    alliedMob.heal(UnitItems.HEROS_ARMOUR_HEAL);
                    ParticleUtil.addParticleExplosion(ParticleRegistrar.FLOATING_HEART.get(), 3, alliedMob.level(), alliedMob.getEyePosition());
                }
            }
        } else if (item == ItemRegistrar.MANA_POTION.get() && mob instanceof HeroUnit heroUnit) {
            heroUnit.setMana(heroUnit.getMana() + UnitItems.MANA_POTION_RESTORE_AMOUNT);
            ParticleUtil.addParticleExplosion(ParticleRegistrar.MANA.get(), 5, mob.level(), mob.getEyePosition());
        }
    }

    public static float getFoodHealAmount(ItemStack itemStack) {
        // 1.21.1: FoodProperties is a record (nutrition()/saturation()) and Item no longer holds
        // one — the food data lives in the stack's DataComponents.FOOD component.
        FoodProperties props = itemStack.get(DataComponents.FOOD);
        int nutrition = props != null ? props.nutrition() : 0;
        if (itemStack.getItem() == Items.BREAD) {
            return HEALTH_PER_BREAD;
        } else if (itemStack.getItem() == Items.COOKED_CHICKEN) {
            return HEALTH_PER_CHICKEN;
        } else if (itemStack.getItem() == Items.COOKED_BEEF) {
            return HEALTH_PER_BEEF;
        } else {
            return nutrition * HEAL_PER_NUTRITION;
        }
    }

    public static ArrayDeque<UnitItem> getRandomItemDropsList() {
        return getRandomItemDropsList(new Random());
    }

    public static ArrayDeque<UnitItem> getRandomItemDropsList(long seed) {
        return getRandomItemDropsList(new Random(seed));
    }

    private static ArrayDeque<UnitItem> getRandomItemDropsList(Random random) {
        // 1. Collect eligible, de-duplicated items
        List<UnitItem> candidates = new ArrayList<>();
        for (UnitItem item : UnitItems.ITEMS) {
            if (item == UnitItems.EMPTY || !item.canRandomDrop || candidates.contains(item))
                continue;
            candidates.add(item);
        }

        // 2. Noisy sort by rarity
        final double JITTER = 1.5;
        Map<UnitItem, Double> sortKeys = new HashMap<>();
        for (UnitItem item : candidates) {
            sortKeys.put(item, item.rarity.ordinal() + random.nextDouble() * JITTER);
        }
        candidates.sort(Comparator.comparingDouble(sortKeys::get));

        // 3. Force 1st = common, 3rd = uncommon
        UnitItem firstCommon = null;
        UnitItem firstUncommon = null;
        for (UnitItem item : candidates) {
            if (firstCommon == null && item.rarity == Rarity.COMMON)
                firstCommon = item;
            else if (firstUncommon == null && item.rarity == Rarity.UNCOMMON)
                firstUncommon = item;
            if (firstCommon != null && firstUncommon != null)
                break;
        }
        if (firstUncommon != null) {
            candidates.remove(firstUncommon);
            candidates.add(Math.min(2, candidates.size()), firstUncommon);
        }
        if (firstCommon != null) {
            candidates.remove(firstCommon);
            candidates.add(0, firstCommon);
        }

        // 4. Force 5th = rare (earliest rare in the list, so overall ordering stays rough)
        UnitItem firstRare = null;
        for (UnitItem item : candidates) {
            if (item.rarity == Rarity.RARE) {
                firstRare = item;
                break;
            }
        }
        if (firstRare != null) {
            candidates.remove(firstRare);
            candidates.add(Math.min(4, candidates.size()), firstRare);
        }

        return new ArrayDeque<>(candidates);
    }


    public static boolean isActive(ItemStack itemStack) {
    // 1.21.1 removed ItemStack#getTag; ItemTagCompat is the port's bridge to the data component
    // that replaced it. A stack with no tag reads as "not active", which is what the old null-NBT
    // path returned.
    CompoundTag tag = ItemTagCompat.tag(itemStack);
    return tag != null && tag.getBoolean("active");
}
}