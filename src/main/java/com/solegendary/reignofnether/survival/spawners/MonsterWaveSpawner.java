package com.solegendary.reignofnether.survival.spawners;

import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.survival.Wave;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.solegendary.reignofnether.survival.SurvivalServerEvents.ENEMY_OWNER_NAME;
import static com.solegendary.reignofnether.survival.SurvivalServerEvents.lastFaction;
import static com.solegendary.reignofnether.survival.spawners.WaveSpawner.*;
import static net.minecraft.world.entity.monster.Creeper.DATA_IS_POWERED;

public class MonsterWaveSpawner {

    private static final Random random = new Random();

    private static final Map<Integer, List<EntityType<? extends Mob>>> MONSTER_UNITS = new HashMap<>();

    static {
        MONSTER_UNITS.put(1, List.of(
                EntityRegistrar.ZOMBIE_PIGLIN_UNIT.get(),
                EntityRegistrar.ZOMBIE_UNIT.get(),
                EntityRegistrar.SKELETON_UNIT.get()
        ));
        MONSTER_UNITS.put(2, List.of(
                EntityRegistrar.ZOMBIE_UNIT.get(),
                EntityRegistrar.SKELETON_UNIT.get(),
                EntityRegistrar.HUSK_UNIT.get(),
                EntityRegistrar.STRAY_UNIT.get(),
                EntityRegistrar.SPIDER_UNIT.get(),
                EntityRegistrar.SLIME_UNIT.get()
        ));
        MONSTER_UNITS.put(3, List.of(
                EntityRegistrar.SKELETON_UNIT.get(),
                EntityRegistrar.HUSK_UNIT.get(),
                EntityRegistrar.STRAY_UNIT.get(),
                EntityRegistrar.SPIDER_UNIT.get(),
                EntityRegistrar.POISON_SPIDER_UNIT.get(),
                EntityRegistrar.SLIME_UNIT.get(),
                EntityRegistrar.DROWNED_UNIT.get(),
                EntityRegistrar.CREEPER_UNIT.get()
                // Spider Jockey
                // spider webs
        ));
        MONSTER_UNITS.put(4, List.of(
                EntityRegistrar.STRAY_UNIT.get(),
                EntityRegistrar.SPIDER_UNIT.get(),
                EntityRegistrar.POISON_SPIDER_UNIT.get(),
                EntityRegistrar.SLIME_UNIT.get(),
                EntityRegistrar.HUSK_UNIT.get(),
                EntityRegistrar.DROWNED_UNIT.get(),
                EntityRegistrar.CREEPER_UNIT.get(),
                EntityRegistrar.ENDERMAN_UNIT.get()
                // Poison Spider Jockey
                // zombies and skeletons gain chest iron armor
        ));
        MONSTER_UNITS.put(5, List.of(
                EntityRegistrar.STRAY_UNIT.get(),
                EntityRegistrar.POISON_SPIDER_UNIT.get(),
                EntityRegistrar.SLIME_UNIT.get(),
                EntityRegistrar.DROWNED_UNIT.get(),
                EntityRegistrar.CREEPER_UNIT.get(),
                EntityRegistrar.ENDERMAN_UNIT.get(),
                EntityRegistrar.WARDEN_UNIT.get()
                // zombies and skeletons gain full iron armor
        ));
        MONSTER_UNITS.put(6, List.of(
                EntityRegistrar.STRAY_UNIT.get(),
                EntityRegistrar.POISON_SPIDER_UNIT.get(),
                EntityRegistrar.SLIME_UNIT.get(),
                EntityRegistrar.DROWNED_UNIT.get(),
                EntityRegistrar.CREEPER_UNIT.get(),
                EntityRegistrar.ENDERMAN_UNIT.get(),
                EntityRegistrar.WARDEN_UNIT.get()
                // zombies and skeletons gain protection-enchanted iron armor
                // Charged creepers
                // slime conversion
        ));
    }

    public static void checkAndApplyUpgrades(int tier) {
        if (tier >= 4 && !ResearchServerEvents.playerHasResearch(ENEMY_OWNER_NAME, ProductionItems.RESEARCH_SPIDER_WEBS))
            ResearchServerEvents.addResearch(ENEMY_OWNER_NAME, ProductionItems.RESEARCH_SPIDER_WEBS);
        if (tier >= 6 && !ResearchServerEvents.playerHasResearch(ENEMY_OWNER_NAME, ProductionItems.RESEARCH_CUBE_MAGMA))
            ResearchServerEvents.addResearch(ENEMY_OWNER_NAME, ProductionItems.RESEARCH_CUBE_MAGMA);
    }

    public static void checkAndApplyArmour(LivingEntity entity, int tier) {
        if (entity instanceof ZombieUnit || entity instanceof HuskUnit || entity instanceof DrownedUnit ||
            entity instanceof SkeletonUnit || entity instanceof StrayUnit) {
            if (tier >= 4)
                entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            if (tier >= 5) {
                entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
                entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
            }
            if (tier >= 6) {
                entity.getItemBySlot(EquipmentSlot.CHEST).enchant(Enchantments.ALL_DAMAGE_PROTECTION, 1);
                entity.getItemBySlot(EquipmentSlot.LEGS).enchant(Enchantments.ALL_DAMAGE_PROTECTION, 1);
                entity.getItemBySlot(EquipmentSlot.FEET).enchant(Enchantments.ALL_DAMAGE_PROTECTION, 1);
            }
        }
    }

    public static EntityType<? extends Mob> getRandomUnitOfTier(int tier) {
        List<EntityType<? extends Mob>> units = MONSTER_UNITS.get(tier);
        return units.get(random.nextInt(units.size()));
    }

    // spawn monsters evenly spread out across from all directions
    public static void spawnMonsterWave(ServerLevel level, Wave wave) {
        checkAndApplyUpgrades(wave.highestUnitTier);

        final int pop = wave.population * PlayerServerEvents.rtsPlayers.size();
        int remainingPop = wave.population * PlayerServerEvents.rtsPlayers.size();

        List<BlockPos> bps = getValidSpawnPoints(remainingPop, level, true, 4);

        for (BlockPos bp : bps) {
            BlockState bs = level.getBlockState(bp);

            EntityType<? extends Mob> mobType = MonsterWaveSpawner.getRandomUnitOfTier(wave.highestUnitTier);

            bp = bp.offset(0,2,0);

            Entity entity = UnitServerEvents.spawnMob(mobType, level,
                    mobType.getDescription().getString().contains("spider") ? bp.above(): bp,
                    ENEMY_OWNER_NAME);

            if (wave.highestUnitTier >= 4 && entity instanceof SpiderUnit spiderUnit) {
                Entity entityPassenger = UnitServerEvents.spawnMob(EntityRegistrar.SKELETON_UNIT.get(),
                        level, bp.above(), ENEMY_OWNER_NAME);
                if (entityPassenger instanceof Unit unit) {
                    entityPassenger.startRiding(spiderUnit);
                    remainingPop -= getModifiedPopCost(unit);
                }
            }

            if (wave.highestUnitTier >= 5 && entity instanceof PoisonSpiderUnit poisonSpiderUnit) {
                Entity entityPassenger = UnitServerEvents.spawnMob(EntityRegistrar.STRAY_UNIT.get(),
                        level, bp.above(), ENEMY_OWNER_NAME);
                if (entityPassenger instanceof Unit unit) {
                    entityPassenger.startRiding(poisonSpiderUnit);
                    remainingPop -= getModifiedPopCost(unit);
                }
            }

            if (random.nextBoolean() && entity instanceof CreeperUnit creeperUnit)
                creeperUnit.getEntityData().set(DATA_IS_POWERED, true);

            if (entity instanceof SlimeUnit slimeUnit)
                slimeUnit.setSize(wave.highestUnitTier, true);

            if (entity instanceof Unit unit) {
                checkAndApplyArmour((LivingEntity) unit, wave.highestUnitTier);
                placeIceOrMagma(bp, level);
                remainingPop -= getModifiedPopCost(unit);
            }

            if (remainingPop <= 0)
                break;
        }
        if (remainingPop > 0) {
            PlayerServerEvents.sendMessageToAllPlayers("Failed to spawn " + remainingPop + "/" + pop + " population worth of monster units");
        }
        lastFaction = Faction.MONSTERS;
    }
}
