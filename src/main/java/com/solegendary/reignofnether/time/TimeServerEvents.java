package com.solegendary.reignofnether.time;

import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.heroAbilities.necromancer.BloodMoon;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static com.solegendary.reignofnether.player.PlayerServerEvents.sendMessageToAllPlayers;

public class TimeServerEvents {

    private static Random RANDOM = new Random();

    private static int bloodMoonTicksLeft = 0;
    private static LivingEntity bloodMoonOwner = null;
    private static BlockPos bloodMoonTarget = null; // area to spawn enemies
    private static boolean zombieOrSkeleton = true;

    public static void resetBloodMoon() {
        bloodMoonTicksLeft = 0;
        bloodMoonOwner = null;
    }

    public static boolean isBloodMoonActive() {
        return bloodMoonTicksLeft > 0;
    }

    public static void startBloodMoon(int tickDuration, Unit owner, BlockPos targetPos) {
        bloodMoonTicksLeft = tickDuration;
        bloodMoonOwner = (LivingEntity) owner;
        bloodMoonTarget = targetPos;
        sendMessageToAllPlayers("abilities.reignofnether.blood_moon.start", 0xFF0000, true, owner.getOwnerName());
        SoundClientboundPacket.playSoundForAllPlayers(SoundAction.RANDOM_CAVE_AMBIENCE);
    }

    // spawns one neutral undead unit (zombie or skeleton at random) in each base owned by bloodMoonTarget
    private static void doRandomBloodMoonSpawn(Level level) {
        if (level.isClientSide() || bloodMoonTarget == null) {
            return;
        }
        Random random = new Random();

        List<BuildingPlacement> list = new ArrayList<>();
        for (BuildingPlacement buildingPlacement : BuildingServerEvents.getBuildings()) {
            if (buildingPlacement.centrePos.distSqr(bloodMoonTarget) < (BloodMoon.RADIUS * BloodMoon.RADIUS) && !buildingPlacement.getBuilding().invulnerable) {
                list.add(buildingPlacement);
            }
        }
        ArrayList<BuildingPlacement> enemyBuildings = new ArrayList<>(list);
        Collections.shuffle(enemyBuildings);

        // one random building per enemyPlayerName
        var enemyBuildingSet = new HashSet<>(enemyBuildings);

        for (BuildingPlacement building : enemyBuildingSet) {
            int x = building.centrePos.getX() + random.nextInt(-10, 10);
            int z = building.centrePos.getZ() + random.nextInt(-10, 10);

            BlockPos bp = building.getClosestGroundPos(new BlockPos(x, building.minCorner.getY(), z), 3);

            EntityType<? extends Mob> mobType = zombieOrSkeleton ? EntityRegistrar.ZOMBIE_UNIT.get() : EntityRegistrar.SKELETON_UNIT.get();
            UnitServerEvents.spawnMobs(mobType, (ServerLevel) level, bp, 1, BloodMoon.ENEMY_NAME);
        }
        zombieOrSkeleton = !zombieOrSkeleton;
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD)
            return;

        if (bloodMoonTicksLeft > 0 && bloodMoonOwner != null) {
            bloodMoonTicksLeft -= 1;
            if (bloodMoonTicksLeft <= 0) {
                sendMessageToAllPlayers("abilities.reignofnether.blood_moon.end", 0xFFFFFF, true);
            } else if (bloodMoonTicksLeft % BloodMoon.SPAWN_INTERVAL_TICKS == 0) {
                doRandomBloodMoonSpawn(evt.level);
            }
            if (bloodMoonTicksLeft % 20 == 0) {
                AbilityClientboundPacket.doAbility(bloodMoonOwner.getId(), UnitAction.BLOOD_MOON, bloodMoonTicksLeft, bloodMoonTarget);
            }
        }
    }
}














