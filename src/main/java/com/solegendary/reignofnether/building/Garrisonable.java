package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public interface Garrisonable {
    // returns the relative building position units will go to when garrisoning
    BlockPos getEntryPosition();
    // returns the relative building position units will go to when ungarrisoning
    BlockPos getExitPosition();

    static Building getGarrison(Unit unit) {
        List<Building> buildings;
        if (((Entity) unit).getLevel().isClientSide())
            buildings = BuildingClientEvents.getBuildings();
        else
            buildings = BuildingServerEvents.getBuildings();

        for (Building building : buildings) {
            if (unit.getOwnerName().equals(building.ownerName) &&
                    building instanceof Garrisonable && building.isBuilt &&
                    building.isPosInsideBuilding(((LivingEntity) unit).getOnPos()) &&
                    ((LivingEntity) unit).getOnPos().getY() > building.originPos.getY() + 2) {
                return building;
            }
        }
        return null;
    }
}
