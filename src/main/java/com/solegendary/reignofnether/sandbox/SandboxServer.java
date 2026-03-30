package com.solegendary.reignofnether.sandbox;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.NetherConvertingAddon;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import static com.solegendary.reignofnether.player.PlayerServerEvents.serverLevel;

public class SandboxServer {

    public static boolean isSandboxPlayer(String playerName) {
        for (RTSPlayer rtsPlayer : PlayerServerEvents.rtsPlayers)
            if (rtsPlayer.faction == Faction.NONE && playerName.equals(rtsPlayer.name))
                return true;
        return false;
    }

    public static boolean isAnyoneASandboxPlayer() {
        for (RTSPlayer rtsPlayer : PlayerServerEvents.rtsPlayers)
            if (rtsPlayer.faction == Faction.NONE)
                return true;
        return false;
    }

    public static void spawnUnit(String playerName, String unitName, BlockPos blockPos) {
        if (serverLevel == null)
            return;

        EntityType<? extends Mob> entityType = EntityRegistrar.getEntityType(unitName);

        if (entityType != null) {
            Entity entity = UnitServerEvents.spawnMob(entityType, serverLevel, blockPos, playerName);
            if (entity instanceof Unit unit && (playerName.isEmpty() || playerName.equals("Enemy"))) {
                unit.setAnchor(blockPos);
            }
        }
    }

    public static void setAnchor(int[] entityIds, BlockPos blockPos) {
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            for (int entityId : entityIds) {
                if (entity.getId() == entityId && entity instanceof Unit unit) {
                    unit.setAnchor(blockPos);
                    UnitSyncClientboundPacket.sendSyncAnchorPosPacket(entity, unit.getAnchor());
                }
            }
        }
    }

    public static void resetToAnchor(int[] entityIds) {
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            for (int entityId : entityIds) {
                if (entity.getId() == entityId && entity instanceof Unit unit && Unit.hasAnchor(unit)) {
                    entity.moveTo(Vec3.atCenterOf(unit.getAnchor()).add(0, 0.5d, 0));
                    entity.setHealth(entity.getMaxHealth());
                    entity.removeAllEffects();
                    Unit.fullResetBehaviours(unit);
                }
            }
        }
    }

    public static void removeAnchor(int[] entityIds) {
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            for (int entityId : entityIds) {
                if (entity.getId() == entityId && entity instanceof Unit unit) {
                    unit.setAnchor(null);
                    UnitSyncClientboundPacket.sendRemoveAnchorPosPacket(entity);
                }
            }
        }
    }

    public static void setUnitOwner(int[] entityIds, String ownerName) {
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            for (int entityId : entityIds) {
                if (entity.getId() == entityId && entity instanceof Unit unit) {
                    unit.setOwnerName(ownerName);
                    UnitSyncClientboundPacket.sendSyncOwnerNamePacket(unit);
                }
            }
        }
    }

    public static void setBuildingOwner(BlockPos pos, String ownerName) {
        for (BuildingPlacement bpl : BuildingServerEvents.getBuildings()) {
            if (bpl.originPos.equals(pos)) {
                bpl.ownerName = ownerName;
                BuildingClientboundPacket.syncBuilding(pos, bpl.getBlocksPlaced(), ownerName, bpl.scenarioRoleIndex);
            }
        }
    }

    public static void removeBuilding(BlockPos pos) {
        BuildingServerEvents.getBuildings().removeIf(b -> {
            if (b.originPos.equals(pos)) {
                BuildingClientboundPacket.removeBuilding(pos);
                NetherConvertingAddon ncb;
                if ((ncb = b.getBuilding().getActiveAddon(NetherConvertingAddon.class)) != null && ncb.getMaxNetherRange(b) > 0 && ncb.getNetherZone(b) != null)
                    ncb.getNetherZone(b).startRestoring();
                return true;
            }
            return false;
        });
    }
}
