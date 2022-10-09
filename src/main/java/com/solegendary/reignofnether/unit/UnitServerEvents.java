package com.solegendary.reignofnether.unit;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class UnitServerEvents {

    private static final ArrayList<UnitActionItem> unitActionQueue = new ArrayList<>();

    // for some reason we have to use the level in the same tick as the unit actions or else level.getEntity returns null
    // remember to always reset targets so that users' actions always overwrite any existing action
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        ServerLevel level = (ServerLevel) evt.level;
        for (UnitActionItem actionItem : unitActionQueue)
            actionItem.action(level);

        unitActionQueue.clear();
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

    @SubscribeEvent
    // assign unit controllingPlayerId when spawned based on whoever is closest
    // this is very hacky, can assign to the wrong player if they spawn on top of someone else
    // but probably just leave it until we're spawning with production buildings
    public static void onMobSpawn(LivingSpawnEvent.SpecialSpawn evt) {

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
    // TODO: ownerName is lost on serverside too when server is restarted (inc. singleplayer), maybe have to save to world save file?
    // have to setOwnerName here because its lost on logout; note that players join BEFORE other entities
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        Entity entity = evt.getEntity();
        if (entity instanceof Unit) {
            String ownerName = ((Unit) entity).getOwnerName();
            ((Unit) entity).setOwnerName(ownerName);
        }
    }
}
