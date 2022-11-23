package com.solegendary.reignofnether.unit;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class UnitServerEvents {

    private static final int UNIT_SYNC_TICKS_MAX = 20; // how often we send out unit syncing packets
    private static int unitSyncTicks = UNIT_SYNC_TICKS_MAX;

    private static final ArrayList<UnitActionItem> unitActionQueue = new ArrayList<>();
    private static final ArrayList<LivingEntity> allUnits = new ArrayList<>();

    public static ArrayList<LivingEntity> getAllUnits() {
        return allUnits;
    }

    public static int getCurrentPopulation(ServerLevel level, String ownerName) {
        int currentPopulation = 0;
        for (LivingEntity entity : allUnits)
            if (entity instanceof Unit unit)
                if (unit.getOwnerName().equals(ownerName))
                    currentPopulation += unit.getPopCost();
        for (Building building : BuildingServerEvents.getBuildings())
            if (building.ownerName.equals(ownerName))
                if (building instanceof ProductionBuilding prodBuilding)
                    for (ProductionItem prodItem : prodBuilding.productionQueue)
                        currentPopulation += prodItem.popCost;
        return currentPopulation;
    }

    // manually provide all the variables required to do unit actions
    public static void addActionItem(
            String ownerName,
            UnitAction action,
            int unitId,
            int[] unitIds,
            BlockPos preselectedBlockPos
    ) {
        unitActionQueue.add(
            new UnitActionItem(
                ownerName,
                action,
                unitId,
                unitIds,
                preselectedBlockPos
            )
        );
    }

    // similar to UnitClientEvents getUnitRelationship: given a Unit and Entity, what is the relationship between them
    public static Relationship getUnitToEntityRelationship(Unit unit, Entity entity) {
        if (!(entity instanceof Unit))
            return Relationship.NEUTRAL;

        String ownerName1 = unit.getOwnerName();
        String ownerName2 = ((Unit) entity).getOwnerName();

        if (ownerName1.equals(ownerName2))
            return Relationship.OWNED;
        else
            return Relationship.HOSTILE;
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent evt) {
        if (evt.getEntity() instanceof LivingEntity entity && !evt.getLevel().isClientSide) {
            allUnits.removeIf(e -> e.getId() == entity.getId());

            UnitClientboundPacket.sendLeavePacket(entity);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        if (evt.getEntity() instanceof LivingEntity entity && !evt.getLevel().isClientSide) {
            allUnits.add(entity);
        }
    }

    // for some reason we have to use the level in the same tick as the unit actions or else level.getEntity returns null
    // remember to always reset targets so that users' actions always overwrite any existing action
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide())
            return;

        unitSyncTicks -= 1;
        if (unitSyncTicks <= 0) {
            unitSyncTicks = UNIT_SYNC_TICKS_MAX;
            for (LivingEntity entity : allUnits)
                UnitClientboundPacket.sendSyncPacket(entity);
        }

        for (UnitActionItem actionItem : unitActionQueue)
            actionItem.action(evt.level);

        unitActionQueue.clear();
    }

    @SubscribeEvent
    // assign unit owner when spawned with an egg based on whoever is closest
    public static void onMobSpawn(LivingSpawnEvent.SpecialSpawn evt) {
        if (!evt.getSpawnReason().equals(MobSpawnType.SPAWN_EGG))
            return;

        Entity entity = evt.getEntity();
        if (evt.getEntity() instanceof Unit) {

            Vec3 pos = entity.position();
            List<Player> nearbyPlayers = MiscUtil.getEntitiesWithinRange(
                    new Vector3d(pos.x, pos.y, pos.z),
                    100, Player.class, evt.getEntity().level);

            float closestPlayerDist = 100;
            Player closestPlayer = null;
            for (Player player : nearbyPlayers) {
                if (player.distanceTo(entity) < closestPlayerDist) {
                    closestPlayerDist = player.distanceTo(entity);
                    closestPlayer = player;
                }
            }
            if (closestPlayer != null) {
                ((Unit) entity).setOwnerName(closestPlayer.getName().getString());
            }
        }
    }
}
