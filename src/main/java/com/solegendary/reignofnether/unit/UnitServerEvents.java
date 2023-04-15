package com.solegendary.reignofnether.unit;

import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class UnitServerEvents {

    private static final int UNIT_SYNC_TICKS_MAX = 20; // how often we send out unit syncing packets
    private static int unitSyncTicks = UNIT_SYNC_TICKS_MAX;

    private static final ArrayList<UnitActionItem> unitActionQueue = new ArrayList<>();
    private static final ArrayList<LivingEntity> allUnits = new ArrayList<>();

    private static final ArrayList<Pair<LivingEntity, ChunkAccess>> forcedUnitChunks = new ArrayList<>();

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
            BlockPos preselectedBlockPos,
            BlockPos selectedBuildingPos
    ) {
        unitActionQueue.add(
            new UnitActionItem(
                ownerName,
                action,
                unitId,
                unitIds,
                preselectedBlockPos,
                selectedBuildingPos
            )
        );
    }

    // similar to UnitClientEvents getUnitRelationship: given a Unit and Entity, what is the relationship between them
    public static Relationship getUnitToEntityRelationship(Unit unit, Entity entity) {
        String ownerName1 = unit.getOwnerName();
        String ownerName2 = "";

        if (entity instanceof Player player)
            ownerName2 = player.getName().getString();
        else if (entity instanceof Unit)
            ownerName2 = ((Unit) entity).getOwnerName();
        else
            return Relationship.NEUTRAL;

        if (ownerName1.equals(ownerName2)) {
            if (entity instanceof Player)
                return Relationship.OWNED;
            else
                return Relationship.FRIENDLY;
        }
        else
            return Relationship.HOSTILE;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        if (evt.getEntity() instanceof Unit &&
            evt.getEntity() instanceof LivingEntity entity && !evt.getLevel().isClientSide) {
            allUnits.add(entity);

            ((Unit) entity).setupEquipmentAndUpgradesServer();

            ChunkAccess chunk = evt.getLevel().getChunk(entity.getOnPos());
            ForgeChunkManager.forceChunk((ServerLevel) evt.getLevel(), ReignOfNether.MOD_ID, entity, chunk.getPos().x, chunk.getPos().z, true, true);
            forcedUnitChunks.add(new Pair<>(entity, chunk));
        }
        // --------------------------- //
        // Projectile damage balancing //
        // --------------------------- //
        // damage should be based only on the unit attack damage + enchantments not based weapon damage
        if (evt.getEntity() instanceof Arrow arrow) {
            if (arrow.getOwner() instanceof AttackerUnit unit &&
                arrow.getOwner() instanceof LivingEntity lEntity) {

                if (lEntity.getItemBySlot(EquipmentSlot.MAINHAND).getItem() == Items.BOW)
                    arrow.setBaseDamage(unit.getUnitAttackDamage() - 2);
                else if (lEntity.getItemBySlot(EquipmentSlot.MAINHAND).getItem() == Items.CROSSBOW)
                    arrow.setBaseDamage(unit.getUnitAttackDamage() - 2.5);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent evt) {
        if (evt.getEntity() instanceof Unit &&
            evt.getEntity() instanceof LivingEntity entity &&
            !evt.getLevel().isClientSide) {

            allUnits.removeIf(e -> e.getId() == entity.getId());
            UnitClientboundPacket.sendLeavePacket(entity);

            //ChunkAccess chunk = evt.getLevel().getChunk(entity.getOnPos());
            //ForgeChunkManager.forceChunk((ServerLevel) evt.getLevel(), ReignOfNether.MOD_ID, entity, chunk.getPos().x, chunk.getPos().z, false, true);
            //forcedUnitChunks.removeIf(p -> p.getFirst().getId() == entity.getId());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent evt) {
        // drop all resources held
        if (evt.getEntity() instanceof Unit unit) {
            List<ItemStack> itemStacks = unit.getItems();
            for (ItemStack itemStack : itemStacks)
                evt.getEntity().spawnAtLocation(itemStack);
        }
    }

    // for some reason we have to use the level in the same tick as the unit actions or else level.getEntity returns null
    // remember to always reset targets so that users' actions always overwrite any existing action
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD)
            return;

        unitSyncTicks -= 1;
        if (unitSyncTicks <= 0) {
            unitSyncTicks = UNIT_SYNC_TICKS_MAX;
            for (LivingEntity entity : allUnits) {
                UnitClientboundPacket.sendSyncStatsPacket(entity);
                UnitWorkerClientBoundPacket.sendSyncWorkerPacket(entity);

                // remove old chunk // add current chunk
                boolean chunkNeedsUpdate = false;
                ChunkAccess newChunk = evt.level.getChunk(entity.getOnPos());

                for (Pair<LivingEntity, ChunkAccess> forcedChunk : forcedUnitChunks) {
                    int id = forcedChunk.getFirst().getId();
                    ChunkAccess chunk = forcedChunk.getSecond();
                    if (id == entity.getId() && (chunk.getPos().x != newChunk.getPos().x || chunk.getPos().z != newChunk.getPos().z)) {
                        ForgeChunkManager.forceChunk((ServerLevel) evt.level, ReignOfNether.MOD_ID, entity, chunk.getPos().x, chunk.getPos().z, false, true);
                        chunkNeedsUpdate = true;
                    }
                }
                if (chunkNeedsUpdate) {
                    forcedUnitChunks.removeIf(p -> p.getFirst().getId() == entity.getId());
                    ForgeChunkManager.forceChunk((ServerLevel) evt.level, ReignOfNether.MOD_ID, entity, newChunk.getPos().x, newChunk.getPos().z, true, true);
                    forcedUnitChunks.add(new Pair<>(entity, newChunk));
                    System.out.println("Updated forced chunk for entity: " + entity.getId() + " at: " + newChunk.getPos().x + "," + newChunk.getPos().z);
                }
            }
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

    // make creepers immune to lightning damage (but still get charged by them)
    @SubscribeEvent
    public static void onEntityDamaged(LivingDamageEvent evt) {
        if (evt.getEntity() instanceof Creeper && (evt.getSource() == DamageSource.LIGHTNING_BOLT || evt.getSource() == DamageSource.ON_FIRE))
            evt.setCanceled(true);

        if (evt.getEntity() instanceof Unit && (evt.getSource() == DamageSource.IN_WALL || evt.getSource() == DamageSource.IN_FIRE))
            evt.setCanceled(true);

        // nerf lightning damage
        if (evt.getSource() == DamageSource.LIGHTNING_BOLT) {
            evt.setAmount(evt.getAmount() / 2);
        }

        // prevent friendly fire damage from ranged units (unless specifically targeted)
        if (evt.getSource().isProjectile() && evt.getSource().getEntity() instanceof Unit unit)
            if (getUnitToEntityRelationship(unit, evt.getEntity()) == Relationship.FRIENDLY && unit.getTargetGoal().getTarget() != evt.getEntity())
                evt.setCanceled(true);
    }

    // prevent friendly fire from ranged units (unless specifically targeted)
    // (just allows piercing, damage is cancelled in LivingDamageEvent)
    @SubscribeEvent
    public static void onProjectileHit(ProjectileImpactEvent evt) {
        Entity owner = evt.getProjectile().getOwner();
        Entity hit = null;
        if (evt.getRayTraceResult().getType() == HitResult.Type.ENTITY)
            hit = ((EntityHitResult) evt.getRayTraceResult()).getEntity();

        if (owner instanceof Unit unit && hit != null)
            if (getUnitToEntityRelationship(unit, hit) == Relationship.FRIENDLY && unit.getTargetGoal().getTarget() != hit)
                evt.setCanceled(true);
    }
}
