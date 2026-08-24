package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.building.buildings.placements.GraveyardPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.resources.ResourceCost;

// units and/or research tech that a ProductionBuilding can produce
public abstract class GraveyardUnitProductionItem extends ProductionItem {

    public GraveyardUnitProductionItem(ResourceCost cost) {
        super(cost, ProdDupeRule.ALLOW);
    }

    private int getMaxSkulls(ProductionPlacement pp) {
        if (pp instanceof GraveyardPlacement gy && gy.getUpgradeLevel() > 0) {
            return gy.getMaxSkulls();
        } else {
            return 0;
        }
    }

    @Override
    public boolean canAffordPopulation(ProductionPlacement pp) {
        if (getCost(pp.getLevel().isClientSide(), pp.ownerName).population == 0)
            return true;

        int maxSkulls = getMaxSkulls(pp);
        if (maxSkulls <= 0) {
            return super.canAffordPopulation(pp);
        } else {
            GraveyardPlacement gy = (GraveyardPlacement) pp;
            int currentSkulls = gy.getTotalSkulls() + gy.getSkullsInProgress();
            return (currentSkulls + getCost(pp.getLevel().isClientSide(), pp.ownerName).population) <= maxSkulls;
        }
    }

    @Override
    public boolean isBelowPopulationSupply(ProductionPlacement pp) {
        if (getCost(pp.getLevel().isClientSide(), pp.ownerName).population == 0)
            return true;

        int maxSkulls = getMaxSkulls(pp);
        if (maxSkulls <= 0) {
            return super.isBelowPopulationSupply(pp);
        } else {
            GraveyardPlacement gy = (GraveyardPlacement) pp;
            return (gy.getTotalSkulls() + gy.getSkullsInProgress()) <= maxSkulls;
        }
    }

    @Override
    public boolean isBelowMaxPopulation(ProductionPlacement pp) {
        if (getCost(pp.getLevel().isClientSide(), pp.ownerName).population == 0)
            return true;

        int maxSkulls = getMaxSkulls(pp);
        if (maxSkulls <= 0) {
            return super.canAffordPopulation(pp);
        } else {
            return true;
        }
    }
}
