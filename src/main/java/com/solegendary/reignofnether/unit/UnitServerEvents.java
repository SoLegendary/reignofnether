package com.solegendary.reignofnether.unit;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;

public class UnitServerEvents {

    private static ServerLevel serverLevel = null;

    private static final ArrayList<UnitActionItem> unitActionQueue = new ArrayList<>();
    private static final ArrayList<Integer> allUnitIds = new ArrayList<>();

    public static int getCurrentPopulation(String ownerName) {
        int currentPopulation = 0;
        for (Integer unitId : allUnitIds) {
            Entity entity = serverLevel.getEntity(unitId);
            if (entity instanceof Unit unit) {
                if (unit.getOwnerName().equals(ownerName))
                    currentPopulation += unit.getPopCost();
            }
        }
        return currentPopulation;
    }

    // manually provide all the variables required to do unit actions
    public static void addActionItem(
            UnitAction action,
            int unitId,
            int[] unitIds,
            BlockPos preselectedBlockPos
    ) {
        unitActionQueue.add(
            new UnitActionItem(
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
        int entityId = evt.getEntity().getId();
        allUnitIds.removeIf(e -> e == entityId);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        Entity entity = evt.getEntity();
        if (entity instanceof Unit && !evt.getLevel().isClientSide)
            allUnitIds.add(entity.getId());
    }

    // for some reason we have to use the level in the same tick as the unit actions or else level.getEntity returns null
    // remember to always reset targets so that users' actions always overwrite any existing action
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide())
            return;

        serverLevel = (ServerLevel) evt.level;
        for (UnitActionItem actionItem : unitActionQueue)
            actionItem.action(serverLevel);

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
                //System.out.println("Found nearby player: " + player.getName().getString() + " " + player.distanceTo(entity));
                if (player.distanceTo(entity) < closestPlayerDist) {
                    closestPlayerDist = player.distanceTo(entity);
                    closestPlayer = player;
                }
            }
            if (closestPlayer != null) {
                ((Unit) entity).setOwnerName(closestPlayer.getName().getString());
                //System.out.println("Assigned ownerName: " + closestPlayer.getName().getString());
            }
        }
    }
}
