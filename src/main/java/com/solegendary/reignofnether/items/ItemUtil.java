package com.solegendary.reignofnether.items;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.items.unititems.EdibleFoodItem;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.registrars.ParticleRegistrar;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.util.ParticleUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public class ItemUtil {

    public static final float HEALTH_PER_BREAD = 12;
    public static final float HEALTH_PER_CHICKEN = 18;
    public static final float HEALTH_PER_BEEF = 24;
    public static final float HEAL_PER_NUTRITION = 2.5f;

    public static boolean hasUUID(ItemStack itemStack) {
        return itemStack != null && itemStack.getTag() != null && itemStack.getTag().hasUUID("uuid");
    }

    public static UUID getUUID(ItemStack itemStack) { // if no uuid, return a random one so we don't crash but just do nothing
        return hasUUID(itemStack) ? itemStack.getTag().getUUID("uuid") : UUID.randomUUID();
    }

    public static boolean isUnitItem(ItemStack itemStack) {
        return itemStack != null && getUnitItem(itemStack) != null;
    }

    public static boolean isUnitItem(ItemEntity entity) {
        return entity != null && isUnitItem(entity.getItem());
    }

    @Nullable
    public static UnitItem getUnitItem(ItemStack itemStack) {
        if (itemStack == null)
            return null;
        if (isEdibleFood(itemStack.getItem()))
            return new EdibleFoodItem(itemStack.getItem());
        outerLoop:
        for (UnitItem unitItem : UnitItems.ITEMS) {
            if (unitItem.item == itemStack.getItem()) {
                for (Pair<Enchantment, Integer> pair : unitItem.enchantments) {
                    if (itemStack.getEnchantmentLevel(pair.getFirst()) != pair.getSecond())
                        continue outerLoop;
                }
                return unitItem;
            }
        }
        return null;
    }

    @Nullable
    public static UnitItem getUnitItem(String descId) {
        for (UnitItem unitItem : UnitItems.ITEMS)
            if (unitItem.descId.equals(descId))
                return unitItem;

        for (Item item : ForgeRegistries.ITEMS) {
            if (item.isEdible() && EdibleFoodItem.getFoodDescId(item).equals(descId))
                return new EdibleFoodItem(item);
        }
        return null;
    }

    public static Long getCooldownTicksLeft(ItemStack itemStack, Level level) {
        long gameTime = level.isClientSide() ? TimeClientEvents.getClientTime() : level.getGameTime();
        CompoundTag tag = itemStack.getTag();
        if (tag != null) {
            return Math.max(0, tag.getLong(UnitItem.RON$COOLDOWN_KEY) - gameTime);
        }
        return 0L;
    }

    public static boolean isActive(ItemStack itemStack) {
        return itemStack.getTag() != null &&
                itemStack.getTag().contains("active") &&
                itemStack.getTag().getBoolean("active");
    }

    private final static List<Item> edibleFoods = List.of(
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

    private final static List<Item> edibleDrinks = List.of(
            ItemRegistrar.MANA_POTION.get(),
            ItemRegistrar.HEALTH_POTION.get()
    );

    public static boolean isEdibleFood(Item item) {
        return item.isEdible() && edibleFoods.contains(item);
    }

    public static boolean isEdibleDrink(Item item) {
        return edibleDrinks.contains(item);
    }

    public static boolean isEdibleFoodOrDrink(Item item) {
        return (item.isEdible() && edibleFoods.contains(item)) || edibleDrinks.contains(item);
    }

    public static float getFoodHealAmount(ItemStack itemStack) {
        FoodProperties props = itemStack.getItem().getFoodProperties(itemStack, null);
        int nutrition = props != null ? props.getNutrition() : 0;
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

    public static void applyDrinkEffect(Item item, Mob mob) {
        if (item == ItemRegistrar.HEALTH_POTION.get()) {
            mob.heal(UnitItems.HEALTH_POTION_RESTORE_AMOUNT);
            ParticleUtil.addParticleExplosion(ParticleRegistrar.FLOATING_HEART.get(), 5, mob.level(), mob.getEyePosition());
        } else if (item == ItemRegistrar.MANA_POTION.get() && mob instanceof HeroUnit heroUnit) {
            heroUnit.setMana(heroUnit.getMana() + UnitItems.MANA_POTION_RESTORE_AMOUNT);
            ParticleUtil.addParticleExplosion(ParticleRegistrar.MANA.get(), 5, mob.level(), mob.getEyePosition());
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
}
