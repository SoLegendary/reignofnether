package com.solegendary.reignofnether.survival.spawners;

import com.solegendary.reignofnether.ability.abilities.*;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.survival.Wave;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VindicatorUnit;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.*;

import static com.solegendary.reignofnether.survival.SurvivalServerEvents.ENEMY_OWNER_NAME;
import static com.solegendary.reignofnether.survival.SurvivalServerEvents.lastFaction;
import static com.solegendary.reignofnether.survival.spawners.WaveSpawner.*;

public class IllagerWaveSpawner {

    private static final Random random = new Random();

    private static final Map<Integer, List<EntityType<? extends Mob>>> ILLAGER_UNITS = new HashMap<>();

    static {
        ILLAGER_UNITS.put(1, List.of(
                EntityRegistrar.MILITIA_UNIT.get(),
                EntityRegistrar.VINDICATOR_UNIT.get(),
                EntityRegistrar.PILLAGER_UNIT.get()
                // no enchants
        ));
        ILLAGER_UNITS.put(2, List.of(
                EntityRegistrar.MILITIA_UNIT.get(),
                EntityRegistrar.VINDICATOR_UNIT.get(),
                EntityRegistrar.PILLAGER_UNIT.get()
                // + 50% chance of low tier enchants
        ));
        ILLAGER_UNITS.put(3, List.of(
                EntityRegistrar.MILITIA_UNIT.get(),
                EntityRegistrar.VINDICATOR_UNIT.get(),
                EntityRegistrar.PILLAGER_UNIT.get(),
                EntityRegistrar.IRON_GOLEM_UNIT.get()
                // + low tier enchants
        ));
        ILLAGER_UNITS.put(4, List.of(
                EntityRegistrar.MILITIA_UNIT.get(),
                EntityRegistrar.VINDICATOR_UNIT.get(),
                EntityRegistrar.PILLAGER_UNIT.get(),
                EntityRegistrar.IRON_GOLEM_UNIT.get(),
                EntityRegistrar.EVOKER_UNIT.get()
                // + evokers can use vexes
                // 50% chance of low or high tier enchants
        ));
        ILLAGER_UNITS.put(5, List.of(
                EntityRegistrar.VINDICATOR_UNIT.get(),
                EntityRegistrar.PILLAGER_UNIT.get(),
                EntityRegistrar.IRON_GOLEM_UNIT.get(),
                EntityRegistrar.EVOKER_UNIT.get(),
                EntityRegistrar.RAVAGER_UNIT.get()
                // + high tier enchants
        ));
        ILLAGER_UNITS.put(6, List.of(
                EntityRegistrar.VINDICATOR_UNIT.get(),
                EntityRegistrar.PILLAGER_UNIT.get(),
                EntityRegistrar.IRON_GOLEM_UNIT.get(),
                EntityRegistrar.EVOKER_UNIT.get(),
                EntityRegistrar.RAVAGER_UNIT.get()
                // + Ravager Artillery with illager captain rider
        ));
    }

    public static EntityType<? extends Mob> getRandomUnitOfTier(int tier) {
        List<EntityType<? extends Mob>> units = ILLAGER_UNITS.get(tier);
        return units.get(random.nextInt(units.size()));
    }

    public static void checkAndApplyUpgrades(int tier) {
        if (tier >= 6 && !ResearchServerEvents.playerHasResearch(ENEMY_OWNER_NAME, ProductionItems.RESEARCH_EVOKER_VEXES))
            ResearchServerEvents.addResearch(ENEMY_OWNER_NAME, ProductionItems.RESEARCH_EVOKER_VEXES);
    }

    public static void checkAndApplyEnchants(LivingEntity entity, int tier) {
        Enchantment enchantment = null;

        if (entity instanceof VindicatorUnit vUnit && (tier == 2 || tier == 3)) {
            enchantment = EnchantmentRegistrar.MAIMING.get();
        }
        else if (entity instanceof VindicatorUnit vUnit && (tier == 4 || tier == 5)) {
            enchantment = Enchantments.SHARPNESS;
        }
        else if (entity instanceof PillagerUnit vUnit && (tier == 3 || tier == 4)) {
            enchantment = Enchantments.QUICK_CHARGE;
        }
        else if (entity instanceof PillagerUnit vUnit && (tier == 5 || tier == 6)) {
            enchantment = Enchantments.MULTISHOT;
        }
        else if (entity instanceof EvokerUnit vUnit && tier >= 6) {
            enchantment = EnchantmentRegistrar.VIGOR.get();
        }

        ItemStack item = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        if (enchantment != null && item != ItemStack.EMPTY) {
            EnchantmentHelper.setEnchantments(new HashMap<>(), item);
            item.enchant(enchantment, enchantment == Enchantments.SHARPNESS ? 2 : 1);
        }
    }

    public static void spawnIllagerBase(ServerLevel level, Wave wave) {
        List<BlockPos> spawnBps = getValidSpawnPoints(1, level, false, 16);
        if (spawnBps.isEmpty())
            spawnIllagerWave(level, wave);

        boolean flipCoords = random.nextBoolean();

        for (BlockPos bp : spawnBps) {
            BuildingPlacement building = WaveSpawner.spawnBuilding(Buildings.BARRACKS, bp.above());
            if (building != null) {
                BlockPos bp2 = new BlockPos(building.centrePos.getX() - 2, building.minCorner.getY(), building.centrePos.getZ());
                WaveSpawner.spawnBuilding(Buildings.WATCHTOWER, bp2.offset(new BlockPos(flipCoords ? 10 : -0,-1, flipCoords ? 0 : 10)));
                WaveSpawner.spawnBuilding(Buildings.WATCHTOWER, bp2.offset(new BlockPos(flipCoords ? -10 : 0,-1, flipCoords ? 0 : -10)));
            }
        }
    }

    // spawn illagers from one direction
    public static void spawnIllagerWave(ServerLevel level, Wave wave) {
        checkAndApplyUpgrades(wave.highestUnitTier);

        final int pop = wave.population * PlayerServerEvents.rtsPlayers.size();
        int remainingPop = wave.population * PlayerServerEvents.rtsPlayers.size();
        List<BlockPos> spawnBps = getValidSpawnPoints(remainingPop, level, true, 6);
        int spawnsThisDir = 0;
        int spawnUntilNextTurn = -2;

        if (!spawnBps.isEmpty()) {
            BlockPos bp = spawnBps.get(0).above();

            ArrayList<Entity> entities = new ArrayList<>();

            while (remainingPop > 0) {
                EntityType<? extends Mob> mobType = IllagerWaveSpawner.getRandomUnitOfTier(wave.highestUnitTier);

                Entity entity = UnitServerEvents.spawnMob(mobType, level, bp.above(), ENEMY_OWNER_NAME);
                entities.add(entity);

                if (random.nextBoolean() && wave.highestUnitTier >= 6 && entity instanceof RavagerUnit ravagerUnit) {
                    Entity entityPassenger = UnitServerEvents.spawnMob(EntityRegistrar.PILLAGER_UNIT.get(),
                            level, bp.above(), ENEMY_OWNER_NAME);
                    if (entityPassenger instanceof Unit unit) {
                        entityPassenger.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance());
                        entityPassenger.startRiding(ravagerUnit);
                        remainingPop -= getModifiedPopCost(unit);
                    }
                }
                if (entity instanceof Unit unit) {
                    checkAndApplyEnchants((LivingEntity) unit, wave.highestUnitTier);
                    placeIceOrMagma(bp, level);
                    remainingPop -= getModifiedPopCost(unit);
                    spawnsThisDir += 1;
                }
            }
            // spread units out so they don't explode from cramming
            int sqrLen = (int) Math.ceil(Math.sqrt(entities.size()));
            ArrayList<BlockPos> bps = new ArrayList<>();

            for (int x = bp.getX() - (sqrLen/2); x <= bp.getX() + (sqrLen/2); x++)
                for (int z = bp.getZ() - (sqrLen / 2); z <= bp.getZ() + (sqrLen / 2); z++)
                    bps.add(new BlockPos(x, 0, z));

            int j = Math.min(entities.size(), bps.size());
            for (int i = 0; i < j; i++)
                entities.get(i).moveTo(bps.get(i).getX(), entities.get(i).getY() + 0.5f, bps.get(i).getZ());
        }
        if (remainingPop > 0) {
            PlayerServerEvents.sendMessageToAllPlayers("Failed to spawn " + remainingPop + "/" + pop + " population worth of villager units");
        }
        lastFaction = Faction.VILLAGERS;
    }
}
