package com.solegendary.reignofnether.units;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class UnitServerVanillaEvents {

    private static boolean stopCommand = false;
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
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        ServerLevel level = (ServerLevel) evt.world;

        if (!level.isClientSide()) {
            if (stopCommand) {
                for (int id : selectedUnitIds) {
                    Unit unit = (Unit) level.getEntity(id);
                    if (unit != null)
                        unit.resetTargets();
                }
            }
            for (int id : unitIdsToMove) {
                Unit unit = (Unit) level.getEntity(id);
                if (unit != null)
                    unit.setMoveTarget(preselectedBlockPos);
            }
            for (int id : unitIdsToAttackMove) {
                Unit unit = (Unit) level.getEntity(id);
                if (unit != null)
                    unit.setAttackMoveTarget(preselectedBlockPos);
            }
            for (int id : selectedUnitIds) {
                Unit unit = (Unit) level.getEntity(id);
                if (unit != null) {
                    if (unitIdToAttack >= 0)
                        unit.setAttackTarget((LivingEntity) level.getEntity(unitIdToAttack));
                    if (unitIdToFollow >= 0)
                        unit.setFollowTarget((LivingEntity) level.getEntity(unitIdToFollow));
                }
            }
            stopCommand = false;
            unitIdToAttack = -1;
            unitIdToFollow = -1;
            unitIdsToMove = new int[0];
            unitIdsToAttackMove = new int[0];
        }
    }

    public static void consumeUnitActionQueues(
            boolean stopCommandIn,
            int unitIdToAttackIn,
            int unitIdToFollowIn,
            int[] unitIdsToMoveIn,
            int[] unitIdsToAttackMoveIn,
            int[] preselectedUnitIdsIn,
            int[] selectedUnitIdsIn,
            BlockPos preselectedBlockPosIn
    ) {
        stopCommand = stopCommandIn;
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

            int closestPlayerDist = 100;
            Player closestPlayer = null;
            for (Player player : nearbyPlayers) {
                if (player.distanceTo(entity) < closestPlayerDist)
                    closestPlayer = player;
            }
            if (closestPlayer != null) {
                ((Unit) entity).setOwnerName(closestPlayer.getName().getString());
                System.out.println("Assigned ownerName: " + closestPlayer.getName().getString());

                // set on clientside too - send to all players that have loaded the chunk this entity is in
                PacketHandler.INSTANCE.send(
                    //PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                    PacketDistributor.TRACKING_CHUNK.with(() -> entity.level.getChunkAt(entity.blockPosition())),
                    new UnitClientboundPacket(
                        entity.getId(),
                        closestPlayer.getUUID()
                ));
            }
        }
    }

    // similar to UnitClientVanillaEvents getUnitRelationship  given two
    public static Relationship getUnitToMobRelationship(Unit unit, Entity mob) {
        if (!(mob instanceof Unit))
            return Relationship.NEUTRAL;

        String ownerName1 = unit.getOwnerName();
        String ownerName2 = ((Unit) mob).getOwnerName();

        if (ownerName1.equals(ownerName2))
            return Relationship.OWNED;
        else
            return Relationship.HOSTILE;
    }
}
