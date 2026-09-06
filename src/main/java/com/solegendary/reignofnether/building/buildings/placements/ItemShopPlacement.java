package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.ItemShopAddon;
import com.solegendary.reignofnether.items.ItemShopClientboundPacket;
import com.solegendary.reignofnether.items.StockedShopItem;
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

    public static final int UNIT_SERVE_RANGE = 3;

    private Unit servedUnit = null;

    public ItemShopPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks) {
        super(building, level, originPos, rotation, ownerName, blocks, false);
    }

    @Nullable
    public Unit getServedUnit() {
        return servedUnit;
    }

    public boolean canServeUnit(Unit unit) {
        if (unit instanceof LivingEntity le && unit instanceof UnitInventory) {
            boolean friendly = AlliancesClient.isAlliedOrOwned(unit.getOwnerName(), ownerName);
            return friendly && isPosInsideBuilding(le.getOnPos(), UNIT_SERVE_RANGE);
        }
        return false;
    }

    public void setServedUnit(Unit unit) {
        if (canServeUnit(unit)) {
            servedUnit = unit;
        }
    }

    ArrayList<StockedShopItem> getStockedItems() {
        return getDataStorage().getData(ItemShopAddon.STOCKED_ITEMS);
    }
    public void setStockedItems(ArrayList<StockedShopItem> stockedItems) {
        getDataStorage().setData(ItemShopAddon.STOCKED_ITEMS, stockedItems);
    }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);
        for (StockedShopItem stockedItem : getStockedItems())
            stockedItem.tick();

        if (this.tickAge % 20 == 0) {
            if (tickLevel.isClientSide()) {
                if (servedUnit != null && !canServeUnit(servedUnit))
                    servedUnit = null;
                if (servedUnit == null) {
                    List<Mob> mobs = MiscUtil.getEntitiesWithinAABB(
                            getAABB().inflate(UNIT_SERVE_RANGE),
                            Mob.class,
                            this.level);
                    for (Mob mob : mobs) {
                        if (mob instanceof Unit unit && canServeUnit(unit)) {
                            servedUnit = unit;
                            break;
                        }
                    }
                }
            } else {
                ItemShopClientboundPacket.syncItemShopStock(originPos, getStockedItems());
            }
        }
    }
}
