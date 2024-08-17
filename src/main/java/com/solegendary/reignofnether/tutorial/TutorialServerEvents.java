package com.solegendary.reignofnether.tutorial;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.buildings.monsters.Mausoleum;
import com.solegendary.reignofnether.building.buildings.villagers.Barracks;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.AbstractMeleeAttackUnitGoal;
import com.solegendary.reignofnether.unit.goals.MeleeAttackUnitGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import com.solegendary.reignofnether.unit.units.monsters.ZombieUnit;
import com.solegendary.reignofnether.util.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TutorialServerEvents {

    private static final String TUTORIAL_MAP_NAME = "reign_of_nether_tutorial";
    private static final Long TUTORIAL_MAP_SEED = 4756899154123723533L;
    private static final String TUTORIAL_ENEMY_NAME = "Monsters";
    private static boolean enabled = false;

    private static final Vec3i ANIMAL_POS = new Vec3i(-2923, 67, -1184);
    private static final Vec3i MONSTER_SPAWN_POS = new Vec3i(-2968, 64, -1216);
    private static final Vec3i MONSTER_BASE_POS = new Vec3i(-3082, 72, -1293);

    public static boolean isEnabled() { return enabled; }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        MinecraftServer server = evt.getEntity().getServer();
        if (server == null) {
            PlayerServerEvents.sendMessageToAllPlayers("Failed to load tutorial, server couldn't be found.");
            return;
        }
        String levelName = server.getWorldData().getLevelSettings().levelName();
        if (evt.getEntity().getLevel() instanceof ServerLevel serverLevel)
            if (serverLevel.getSeed() == TUTORIAL_MAP_SEED &&
                levelName.equals(TUTORIAL_MAP_NAME)) {
                TutorialClientboundPacket.enableTutorial();
                enabled = true;
            }
    }

    private static ArrayList<Entity> spawnMobs(EntityType<? extends Mob> entityType, Vec3i pos, int qty, String ownerName) {
        ArrayList<Entity> entities = new ArrayList<>();
        if (PlayerServerEvents.players.isEmpty())
            return entities;
        ServerLevel level = PlayerServerEvents.players.get(0).getLevel();
        for (int i = 0; i < qty; i++) {
            Entity entity = entityType.create(level);
            if (entity != null) {
                entity.moveTo(pos.getX() + i, pos.getY(), pos.getZ());
                level.addFreshEntity(entity);
                entities.add(entity);
                if (entity instanceof Unit unit)
                    unit.setOwnerName(ownerName);
            }
        }
        return entities;
    }

    public static void spawnAnimals() {
        spawnMobs(EntityType.PIG, ANIMAL_POS, 3, "");
    }

    public static void spawnMonstersA() {
        PlayerServerEvents.startRTSBot(TUTORIAL_ENEMY_NAME, Vec3.atCenterOf(MONSTER_BASE_POS), Faction.MONSTERS);
        spawnMobs(EntityRegistrar.ZOMBIE_UNIT.get(), MONSTER_SPAWN_POS, 1, TUTORIAL_ENEMY_NAME);
        spawnMobs(EntityRegistrar.SKELETON_UNIT.get(), MONSTER_SPAWN_POS.east(), 1, TUTORIAL_ENEMY_NAME);
    }

    public static void spawnMonstersB() {
        spawnMobs(EntityRegistrar.ZOMBIE_UNIT.get(), MONSTER_SPAWN_POS, 3, TUTORIAL_ENEMY_NAME);
        spawnMobs(EntityRegistrar.CREEPER_UNIT.get(), MONSTER_SPAWN_POS, 2, TUTORIAL_ENEMY_NAME);
    }

    public static void attackWithMonstersA() { // order all monster units to attack move towards the enemy base
        BlockPos attackPos = null;
        for (Building building : BuildingServerEvents.getBuildings())
            if (building instanceof TownCentre)
                attackPos = building.centrePos;

        if (attackPos != null)
            for (LivingEntity entity : UnitServerEvents.getAllUnits())
                if (entity instanceof AttackerUnit aUnit)
                    aUnit.setAttackMoveTarget(attackPos);
    }

    public static void attackWithMonstersB() {
        BlockPos attackTargetZombies = null;
        BlockPos attackTargetCreepers = null;
        for (Building building : BuildingServerEvents.getBuildings()) {
            if (building instanceof Barracks)
                attackTargetZombies = building.originPos;
            else if (building instanceof TownCentre)
                attackTargetCreepers = building.originPos;
        }

        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (attackTargetZombies != null && entity instanceof ZombieUnit zUnit)
                zUnit.setAttackBuildingTarget(attackTargetZombies);
            else if (attackTargetCreepers != null && entity instanceof CreeperUnit cUnit)
                cUnit.setAttackBuildingTarget(attackTargetCreepers);
        }
    }

    public static void spawnMonsterWorkers() {
        spawnMobs(EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get(), MONSTER_SPAWN_POS, 3, TUTORIAL_ENEMY_NAME);
    }

    public static void startBuildingMonsterBase() {
        int[] builderUnitIds = UnitServerEvents.getAllUnits().stream().mapToInt(Entity::getId).toArray();
        if (builderUnitIds.length > 0)
            BuildingServerEvents.placeBuilding(Mausoleum.buildingName, (BlockPos) MONSTER_BASE_POS, Rotation.NONE,
                    TUTORIAL_ENEMY_NAME, builderUnitIds, false, false);
    }
}
