package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public interface GarrisonableBuilding {

    // set range for all garrisoned units don't use this for abilities as it may not be balanced
    public int getAttackRange();
    // bonus for units attacking garrisoned units
    public int getExternalAttackRangeBonus();

    // returns the absolute building position units will go to when garrisoning
    @Nullable BlockPos getEntryPosition();
    // returns the absolute building position units will go to when ungarrisoning
    @Nullable BlockPos getExitPosition();

    int getCapacity();

    default boolean isFull() {
        return getNumOccupants((BuildingPlacement) this) >= getCapacity();
    }

    static BlockPos rotatePos(BlockPos pos, Rotation rot) {
        if (rot == Rotation.NONE) {
            return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
        } else if (rot == Rotation.CLOCKWISE_90) {
            return new BlockPos(-pos.getX(), pos.getY(), pos.getZ());
        } else if (rot == Rotation.CLOCKWISE_180) {
            return new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
        } else {
            return new BlockPos(pos.getX(), pos.getY(), -pos.getZ());
        }
    }

    // will only return actual Units, not any other LivingEntity
    public default List<LivingEntity> getOccupants() {
        if (this instanceof BuildingPlacement building) {
            if (building.level.isClientSide()) {
                List<LivingEntity> list = new ArrayList<>();
                for (LivingEntity le : UnitClientEvents.getAllUnits()) {
                    if (le instanceof Unit u &&
                        GarrisonableBuilding.getGarrison(u) == this) {
                        list.add(le);
                    }
                }
                return list;
            }
            else {
                List<LivingEntity> list = new ArrayList<>();
                for (LivingEntity le : UnitServerEvents.getAllUnits()) {
                    if (le instanceof Unit u &&
                        GarrisonableBuilding.getGarrison(u) == this) {
                        list.add(le);
                    }
                }
                return list;
            }
        }
        return List.of();
    }

    static GarrisonableBuilding getGarrison(Unit unit) {
        List<GarrisonableBuilding> buildings;
        Entity entity = (Entity) unit;
        if (entity.level().isClientSide())
            buildings = BuildingClientEvents.getGarrisonableBuildings();
        else
            buildings = BuildingServerEvents.getGarrisonableBuildings();

        for (GarrisonableBuilding garrisonableBuilding : buildings) {
            if (!(garrisonableBuilding instanceof BuildingPlacement building)) return null;
            boolean isAllied;
            if (entity.level().isClientSide()) {
                isAllied = AlliancesClient.isAllied(unit.getOwnerName(), building.ownerName);
            }
            else {
                isAllied = AlliancesServerEvents.isAllied(unit.getOwnerName(), building.ownerName);
            }

            if ((!unit.getOwnerName().equals(building.ownerName) && !isAllied && (!unit.getOwnerName().isEmpty() || !building.ownerName.isEmpty())) ||
                garrisonableBuilding.getCapacity() <= 0 || !building.isBuilt ||
                !building.isPosInsideBuilding(((LivingEntity) unit).getOnPos().above())) {
                continue;
            }

            if (building.getBuilding() instanceof CustomBuilding) {
                Block onBlock = entity.level().getBlockState(entity.getOnPos().above()).getBlock();
                if (onBlock == BlockRegistrar.GARRISON_ZONE_BLOCK.get() || onBlock == BlockRegistrar.GARRISON_ENTRY_BLOCK.get()) return garrisonableBuilding;
            } else {
                if (((LivingEntity) unit).getOnPos().getY() > building.originPos.getY() + 2) return garrisonableBuilding;
            }
        }
        return null;
    }

    static int getNumOccupants(BuildingPlacement building) {
        List<LivingEntity> entities;
        if (building.getLevel().isClientSide())
            entities = UnitClientEvents.getAllUnits();
        else
            entities = UnitServerEvents.getAllUnits();

        int numOccupants = 0;
        for (LivingEntity entity : entities) {
            if (building.isPosInsideBuilding(entity.getOnPos().above())) {
                if (building.getBuilding() instanceof CustomBuilding) {
                    Block onBlock = entity.level().getBlockState(entity.getOnPos().above()).getBlock();
                    if (onBlock == BlockRegistrar.GARRISON_ZONE_BLOCK.get() ||
                        onBlock == BlockRegistrar.GARRISON_ENTRY_BLOCK.get())
                        numOccupants += 1;;
                } else {
                    if (entity.getOnPos().getY() > building.originPos.getY() + 2)
                        numOccupants += 1;;
                }
            }
        }

        return numOccupants;
    }
}
