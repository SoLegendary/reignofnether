package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
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

public interface GarrisonableBuildingAddon extends BuildingAddon{
    // set range for all garrisoned units don't use this for abilities as it may not be balanced
    public int getAttackRange();
    // bonus for units attacking garrisoned units
    public int getExternalAttackRangeBonus();

    // returns the absolute building position units will go to when garrisoning
    @Nullable
    BlockPos getEntryPosition(BuildingPlacement placement);
    // returns the absolute building position units will go to when ungarrisoning
    @Nullable BlockPos getExitPosition(BuildingPlacement placement);

    int getCapacity();

    default boolean isFull(BuildingPlacement placement) {
        return getNumOccupants(placement) >= getCapacity();
    }

    // will only return actual Units, not any other LivingEntity
    default List<LivingEntity> getOccupants(BuildingPlacement placement) {
        if (placement.level.isClientSide()) {
            List<LivingEntity> list = new ArrayList<>();
            for (LivingEntity le : UnitClientEvents.getAllUnits()) {
                if (le instanceof Unit u &&
                    GarrisonableBuildingAddon.getGarrison(u) == placement) {
                    list.add(le);
                }
            }
            return list;
        }
        else {
            List<LivingEntity> list = new ArrayList<>();
            for (LivingEntity le : UnitServerEvents.getAllUnits()) {
                if (le instanceof Unit u &&
                    GarrisonableBuildingAddon.getGarrison(u) == placement) {
                    list.add(le);
                }
            }
            return list;
        }
    }

    static BuildingPlacement getGarrison(Unit unit) {
        List<BuildingPlacement> buildings;
        Entity entity = (Entity) unit;
        if (entity.level().isClientSide())
            buildings = BuildingClientEvents.getGarrisonableBuildings();
        else
            buildings = BuildingServerEvents.getGarrisonableBuildings();

        for (BuildingPlacement building : buildings) {
            GarrisonableBuildingAddon gba = building.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class);
            if (gba != null) {
                boolean isAllied;
                if (entity.level().isClientSide()) {
                    isAllied = AlliancesClient.isAllied(unit.getOwnerName(), building.ownerName);
                }
                else {
                    isAllied = AlliancesServerEvents.isAllied(unit.getOwnerName(), building.ownerName);
                }

                if ((!unit.getOwnerName().equals(building.ownerName) && !isAllied && (!unit.getOwnerName().isEmpty() || !building.ownerName.isEmpty())) ||
                        gba.getCapacity() <= 0 || !building.isBuilt ||
                        !building.isPosInsideBuilding(((LivingEntity) unit).getOnPos().above())) {
                    continue;
                }

                if (building.getBuilding() instanceof CustomBuilding) {
                    Block onBlock = entity.level().getBlockState(entity.getOnPos().above()).getBlock();
                    if (onBlock == BlockRegistrar.GARRISON_ZONE_BLOCK.get() ||
                            onBlock == BlockRegistrar.GARRISON_ENTRY_BLOCK.get())
                        return building;
                } else {
                    if (((LivingEntity) unit).getOnPos().getY() > building.originPos.getY() + 2) return building;
                }
            }
        }
        return null;
    }

//    static BuildingPlacement getGarrisonPlacement(Unit unit) {
//        List<BuildingPlacement> buildings;
//        Entity entity = (Entity) unit;
//        if (entity.level().isClientSide())
//            buildings = BuildingClientEvents.getBuildings();
//        else
//            buildings = BuildingServerEvents.getBuildings();
//
//        for (BuildingPlacement building : buildings) {
//
//            boolean isAllied;
//            if (entity.level().isClientSide())
//                isAllied = AlliancesClient.isAllied(unit.getOwnerName(), building.ownerName);
//            else
//                isAllied = AlliancesServerEvents.isAllied(unit.getOwnerName(), building.ownerName);
//
//            GarrisonableBuildingAddon garr;
//            if ((unit.getOwnerName().equals(building.ownerName) || isAllied || (unit.getOwnerName().isEmpty() && building.ownerName.isEmpty())) &&
//                    (garr = building.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class)) != null && building.isBuilt &&
//                    building.isPosInsideBuilding(((LivingEntity) unit).getOnPos().above())) {
//
//                if (building.getBuilding() instanceof CustomBuilding) {
//                    Block onBlock = entity.level().getBlockState(entity.getOnPos().above()).getBlock();
//                    if (onBlock == BlockRegistrar.GARRISON_ZONE_BLOCK.get() ||
//                            onBlock == BlockRegistrar.GARRISON_ENTRY_BLOCK.get())
//                        return building;
//                } else {
//                    if (((LivingEntity) unit).getOnPos().getY() > building.originPos.getY() + 2)
//                        return building;
//                }
//            }
//        }
//        return null;
//    }

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
