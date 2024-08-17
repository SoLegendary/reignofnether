package com.solegendary.reignofnether.tutorial;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.buildings.monsters.Mausoleum;
import com.solegendary.reignofnether.building.buildings.villagers.Barracks;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonUnit;
import com.solegendary.reignofnether.unit.units.monsters.ZombieUnit;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class TutorialServerEvents {

    private static final String TUTORIAL_MAP_NAME = "reign_of_nether_tutorial";
    private static final Long TUTORIAL_MAP_SEED = 4756899154123723533L;
    private static final String TUTORIAL_ENEMY_NAME = "Monsters";
    private static boolean enabled = false;

    private static final Vec3i ANIMAL_POS = new Vec3i(-2923, 67, -1184);
    private static final Vec3i MONSTER_SPAWN_POS = new Vec3i(-2968, 64, -1216);
    private static final Vec3i MONSTER_BASE_POS = new Vec3i(-3082, 72, -1293);

    public static boolean isEnabled() { return enabled; }

    private static ServerLevel getServerLevel() {
        if (!PlayerServerEvents.players.isEmpty())
            return PlayerServerEvents.players.get(0).getLevel();
        return null;
    }

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

    public static void setDayTime() {
        ServerLevel level = getServerLevel();
        if (level != null) {
            level.setDayTime(1000);
        }
    }

    public static void setNightTime() {
        ServerLevel level = getServerLevel();
        if (level != null) {
            level.setDayTime(13000);
        }
    }

    private static ArrayList<Entity> spawnMobs(EntityType<? extends Mob> entityType, Vec3i pos, int qty, String ownerName) {
        ArrayList<Entity> entities = new ArrayList<>();
        ServerLevel level = getServerLevel();
        if (level != null) {
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
        }
        return entities;
    }

    public static void spawnAnimals() {
        spawnMobs(EntityType.PIG, ANIMAL_POS, 3, "");
    }


    // also officially adds the tutorial bot to the game as an RTSPlayer
    public static void spawnMonsterWorkers() {
        PlayerServerEvents.startRTSBot(TUTORIAL_ENEMY_NAME, Vec3.atCenterOf(MONSTER_BASE_POS), Faction.MONSTERS);
    }

    public static void spawnMonstersA() {
        spawnMobs(EntityRegistrar.ZOMBIE_UNIT.get(), MONSTER_SPAWN_POS, 1, TUTORIAL_ENEMY_NAME);
        spawnMobs(EntityRegistrar.SKELETON_UNIT.get(), MONSTER_SPAWN_POS.east(), 1, TUTORIAL_ENEMY_NAME);
    }

    public static void spawnMonstersB() {
        spawnMobs(EntityRegistrar.ZOMBIE_UNIT.get(), MONSTER_SPAWN_POS, 3, TUTORIAL_ENEMY_NAME);
        spawnMobs(EntityRegistrar.SKELETON_UNIT.get(), MONSTER_SPAWN_POS, 2, TUTORIAL_ENEMY_NAME);
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
        BlockPos attackTargetSkeleton = null;
        for (Building building : BuildingServerEvents.getBuildings()) {
            if (building instanceof Barracks)
                attackTargetZombies = building.originPos;
            else if (building instanceof TownCentre)
                attackTargetSkeleton = building.centrePos;
        }

        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (attackTargetZombies != null && entity instanceof ZombieUnit zUnit)
                zUnit.setAttackBuildingTarget(attackTargetZombies);
            else if (attackTargetSkeleton != null && entity instanceof SkeletonUnit cUnit)
                cUnit.setAttackMoveTarget(attackTargetSkeleton);
        }
    }

    public static void startBuildingMonsterBase() {
        int[] builderUnitIds = UnitServerEvents.getAllUnits().stream().mapToInt(Entity::getId).toArray();
        if (builderUnitIds.length > 0)
            BuildingServerEvents.placeBuilding(Mausoleum.buildingName, (BlockPos) MONSTER_BASE_POS, Rotation.NONE,
                    TUTORIAL_ENEMY_NAME, builderUnitIds, false, false);
    }
}
