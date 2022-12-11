package com.solegendary.reignofnether.unit;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.research.ResearchServer;
import com.solegendary.reignofnether.research.researchItems.ResearchVindicatorAxes;
import com.solegendary.reignofnether.resources.ResourceAnimal;
import com.solegendary.reignofnether.resources.ResourceAnimals;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

            // TODO: remove this on leaving
            if (entity instanceof Unit) {
                ChunkAccess chunk = evt.getLevel().getChunk(entity.getOnPos());
                ForgeChunkManager.forceChunk((ServerLevel) evt.getLevel(), ReignOfNether.MOD_ID, entity, chunk.getPos().x, chunk.getPos().z, true, true);
            }
        }

        // --------------------------- //
        // Projectile damage balancing //
        // --------------------------- //
        // damage should be based only on the unit attack damage + enchantments not based weapon damage
        if (evt.getEntity() instanceof Arrow arrow) {
            if (arrow.getOwner() instanceof AttackerUnit unit &&
                arrow.getOwner() instanceof LivingEntity lEntity) {

                if (lEntity.getItemBySlot(EquipmentSlot.MAINHAND).getItem() == Items.BOW)
                    arrow.setBaseDamage(unit.getAttackDamage() - 2);
                else if (lEntity.getItemBySlot(EquipmentSlot.MAINHAND).getItem() == Items.CROSSBOW)
                    arrow.setBaseDamage(unit.getAttackDamage() - 2.5);
            }
        }
    }

    // award food to killer of wild animal mobs
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent evt) {
        if (evt.getEntity() instanceof Animal animal && !(evt.getEntity() instanceof Unit)) {
            for (ResourceAnimal resAnimal : ResourceAnimals.animals) {
                if (resAnimal.animalName.equals(animal.getName().getString())) {
                    Entity entity = evt.getSource().getEntity();
                    if (entity instanceof Unit unit)
                        ResourcesServerEvents.addSubtractResources(new Resources(
                            unit.getOwnerName(),
                            animal.isBaby() ? resAnimal.foodValue / 2 : resAnimal.foodValue,
                            0,0
                        ));
                    else if (entity instanceof Player player)
                        ResourcesServerEvents.addSubtractResources(new Resources(
                            player.getName().getString(),
                            animal.isBaby() ? resAnimal.foodValue / 2 : resAnimal.foodValue,
                            0,0
                        ));
                }
            }
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
