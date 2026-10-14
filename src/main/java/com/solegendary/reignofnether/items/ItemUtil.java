package com.solegendary.reignofnether.items;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.items.unititems.EdibleFoodItem;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.registrars.ParticleRegistrar;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.AbstractTotem;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.ParticleUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
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

    // excludes vanilla items that need enchantments and vanilla food
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

    private static final Rarity[] FORCED_RARITIES = {
            Rarity.COMMON, null, Rarity.UNCOMMON, null, Rarity.RARE, null, Rarity.EPIC
    };

    private static Rarity rollRarity(Random random) {
        double r = random.nextDouble();
        if (r < 0.40) return Rarity.COMMON;    // 40%
        if (r < 0.70) return Rarity.UNCOMMON;  // 30%
        if (r < 0.90) return Rarity.RARE;      // 20%
        return Rarity.EPIC;                    // 10%
    }

    /**
     * @param itemRandom   decides WHICH item of a given rarity drops
     * @param rarityRandom decides the rarity of each drop slot; if null, no forced
     *                     slots are used and rarities are rolled from itemRandom
     */
    private static ArrayDeque<UnitItem> getRandomItemDropsList(Random itemRandom, Random rarityRandom) {
        // 1. Collect eligible, de-duplicated items (stable order) into per-rarity pools
        Set<UnitItem> unique = new LinkedHashSet<>();
        for (UnitItem item : UnitItems.ITEMS)
            if (item != UnitItems.EMPTY && item.canRandomDrop)
                unique.add(item);

        Rarity[] rarities = Rarity.values();
        Map<Rarity, List<UnitItem>> pools = new EnumMap<>(Rarity.class);
        for (Rarity rarity : rarities)
            pools.put(rarity, new ArrayList<>());
        for (UnitItem item : unique)
            pools.get(item.rarity).add(item);

        // Shuffle each pool with itemRandom (fixed iteration order => deterministic per itemSeed)
        for (Rarity rarity : rarities)
            Collections.shuffle(pools.get(rarity), itemRandom);

        // 2. Build the drop list slot by slot until every item has been used
        boolean forced = rarityRandom != null;
        Random raritySource = forced ? rarityRandom : itemRandom;

        ArrayDeque<UnitItem> result = new ArrayDeque<>(unique.size());
        int slot = 0;
        while (result.size() < unique.size()) {
            // Always roll exactly once per slot so the rarity sequence never
            // depends on which items happened to be available.
            Rarity wanted = rollRarity(raritySource);
            if (forced && slot < FORCED_RARITIES.length && FORCED_RARITIES[slot] != null)
                wanted = FORCED_RARITIES[slot];

            List<UnitItem> pool = pools.get(wanted);
            if (pool.isEmpty())
                pool = nearestNonEmptyPool(pools, wanted, rarities);

            // pool can't be null here: result.size() < unique.size() guarantees items remain
            result.add(pool.remove(pool.size() - 1));
            slot++;
        }
        return result;
    }

    /** Fallback when the wanted rarity is used up: closest rarity (lower wins ties) that still has items. */
    private static List<UnitItem> nearestNonEmptyPool(Map<Rarity, List<UnitItem>> pools, Rarity wanted, Rarity[] rarities) {
        List<UnitItem> best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Rarity rarity : rarities) {
            List<UnitItem> pool = pools.get(rarity);
            if (pool.isEmpty()) continue;
            int dist = Math.abs(rarity.ordinal() - wanted.ordinal());
            if (dist < bestDist) { // strict < keeps the lower rarity on ties
                bestDist = dist;
                best = pool;
            }
        }
        return best;
    }

    public static ArrayDeque<UnitItem> getRandomItemDropsList(long itemSeed, long raritySeed) {
        return getRandomItemDropsList(
                new Random(itemSeed),
                raritySeed >= 0 ? new Random(raritySeed) : null
        );
    }
}
