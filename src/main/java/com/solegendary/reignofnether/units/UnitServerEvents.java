package com.solegendary.reignofnether.units;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.hud.ActionName;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class UnitServerEvents {

    private static ActionName specialAction = null;
    private static int unitIdToAttack = -1;
    private static int unitIdToFollow = -1;
    private static int[] unitIdsToMove = new int[0];
    private static int[] unitIdsToAttackMove = new int[0];
    private static int[] preselectedUnitIds = new int[0];
    private static int[] selectedUnitIds = new int[0];
    private static BlockPos preselectedBlockPos = null;

    public static int[] getPreselectedUnitIds() { return preselectedUnitIds; }
    public static int[] getSelectedUnitIds() { return selectedUnitIds; }
    public static BlockPos getPreselectedBlockPos() { return preselectedBlockPos; }



    // for some reason we have to use the level in the same tick as the unit actions or else level.getEntity returns null
    // remember to always reset targets so that users' actions always overwrite any existing action
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        ServerLevel level = (ServerLevel) evt.world;

        if (!level.isClientSide()) {
            if (specialAction == ActionName.STOP) {
                for (int id : selectedUnitIds) {
                    Unit unit = (Unit) level.getEntity(id);
                    if (unit != null)
                        unit.resetBehaviours();
                }
            }
            if (specialAction == ActionName.HOLD) {
                for (int id : selectedUnitIds) {
                    Unit unit = (Unit) level.getEntity(id);
                    if (unit != null) {
                        unit.resetBehaviours();
                        unit.setHoldPosition(true);
                    }
                }
            }
            for (int id : unitIdsToMove) {
                Unit unit = (Unit) level.getEntity(id);
                if (unit != null) {
                    unit.resetBehaviours();
                    unit.setMoveTarget(preselectedBlockPos);
                }
            }
            for (int id : unitIdsToAttackMove) {
                Unit unit = (Unit) level.getEntity(id);
                if (unit != null) {
                    unit.resetBehaviours();
                    unit.setAttackMoveTarget(preselectedBlockPos);
                }
            }
            for (int id : selectedUnitIds) {
                Unit unit = (Unit) level.getEntity(id);
                if (unit != null && id != unitIdToAttack && id != unitIdToFollow) {
                    if (unitIdToAttack >= 0) {
                        unit.resetBehaviours();
                        unit.setAttackTarget((LivingEntity) level.getEntity(unitIdToAttack));
                    }
                    if (unitIdToFollow >= 0) {
                        unit.resetBehaviours();
                        unit.setFollowTarget((LivingEntity) level.getEntity(unitIdToFollow));
                    }
                }
            }
            specialAction = null;
            unitIdToAttack = -1;
            unitIdToFollow = -1;
            unitIdsToMove = new int[0];
            unitIdsToAttackMove = new int[0];
        }
    }

    public static void consumeUnitActionQueues(
            ActionName specialActionIn,
            int unitIdToAttackIn,
            int unitIdToFollowIn,
            int[] unitIdsToMoveIn,
            int[] unitIdsToAttackMoveIn,
            int[] preselectedUnitIdsIn,
            int[] selectedUnitIdsIn,
            BlockPos preselectedBlockPosIn
    ) {
        specialAction = specialActionIn;
        unitIdToAttack = unitIdToAttackIn;
        unitIdToFollow = unitIdToFollowIn;
        unitIdsToMove = unitIdsToMoveIn;
        unitIdsToAttackMove = unitIdsToAttackMoveIn;
        preselectedUnitIds = preselectedUnitIdsIn;
        selectedUnitIds = selectedUnitIdsIn;
        preselectedBlockPos = preselectedBlockPosIn;
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
                System.out.println("Found nearby player: " + player.getName().getString() + " " + player.distanceTo(entity));
                if (player.distanceTo(entity) < closestPlayerDist) {
                    closestPlayerDist = player.distanceTo(entity);
                    closestPlayer = player;
                }
            }
            if (closestPlayer != null) {
                ((Unit) entity).setOwnerName(closestPlayer.getName().getString());
                System.out.println("Assigned ownerName: " + closestPlayer.getName().getString());
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
    public static void onEntityJoin(EntityJoinWorldEvent evt) {
        Entity entity = evt.getEntity();
        if (entity instanceof Unit) {
            String ownerName = ((Unit) entity).getOwnerName();
            ((Unit) entity).setOwnerName(ownerName);
        }
    }
}
