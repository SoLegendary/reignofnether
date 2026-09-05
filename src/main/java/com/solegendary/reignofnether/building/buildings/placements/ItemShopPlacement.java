package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ItemShopPlacement extends BuildingPlacement {

    private Unit servedUnit = null;

    public ItemShopPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks) {
        super(building, level, originPos, rotation, ownerName, blocks, false);
    }

    @Nullable
    public Unit getServedUnit() {
        return servedUnit;
    }

    private boolean canServeUnit(Unit unit) {
        if (unit instanceof LivingEntity le && unit instanceof UnitInventory) {
            boolean friendly = AlliancesClient.isAlliedOrOwned(unit.getOwnerName(), ownerName);
            return friendly && isPosInsideBuilding(le.getOnPos(), 2);
        }
        return false;
    }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);
        if (tickLevel.isClientSide() && this.tickAge % 20 == 0) {
            if (servedUnit != null && !canServeUnit(servedUnit))
                servedUnit = null;
            if (servedUnit == null) {
                List<Mob> mobs = MiscUtil.getEntitiesWithinAABB(
                        getAABB().inflate(2),
                        Mob.class,
                        this.level);
                for (Mob mob : mobs) {
                    if (mob instanceof Unit unit && canServeUnit(unit)) {
                        servedUnit = unit;
                        break;
                    }
                }
            }
        }
    }
}
