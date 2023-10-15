package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public interface GarrisonableBuilding {

    // set range for all garrisoned units don't use this for abilities as it may not be balanced
    public int getAttackRange();
    // bonus for units attacking garrisoned units
    public int getExternalAttackRangeBonus();

    // returns the relative building position units will go to when garrisoning
    BlockPos getEntryPosition();
    // returns the relative building position units will go to when ungarrisoning
    BlockPos getExitPosition();

    boolean isFull();

    static GarrisonableBuilding getGarrison(Unit unit) {
        List<Building> buildings;
        if (((Entity) unit).getLevel().isClientSide())
            buildings = BuildingClientEvents.getBuildings();
        else
            buildings = BuildingServerEvents.getBuildings();

        for (Building building : buildings) {
            if (unit.getOwnerName().equals(building.ownerName) &&
                    building instanceof GarrisonableBuilding garr && building.isBuilt &&
                    building.isPosInsideBuilding(((LivingEntity) unit).getOnPos()) &&
                    ((LivingEntity) unit).getOnPos().getY() > building.originPos.getY() + 2) {
                return garr;
            }
        }
        return null;
    }

    static int getNumOccupants(Building building) {
        List<LivingEntity> entities;
        if (building.getLevel().isClientSide())
            entities = UnitClientEvents.getAllUnits();
        else
            entities = UnitServerEvents.getAllUnits();

        int numOccupants = 0;
        for (LivingEntity entity : entities)
            if (building.isPosInsideBuilding(entity.getOnPos()) &&
                entity.getOnPos().getY() > building.originPos.getY() + 2)
                numOccupants += 1;

        return numOccupants;
    }
}
