package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.player.RTSPlayerScoresEnum;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.tps.TPSClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.BiConsumer;

// units and/or research tech that a ProductionBuilding can produce
public abstract class ProductionItem {

    public static String itemName;

    public ResourceCost defaultCost;
    public BiConsumer<Level, ProductionPlacement> onComplete;
    public ProdDupeRule dupeRule;

    public ProductionItem(ResourceCost cost, ProdDupeRule dupeRule) {
        this.defaultCost = cost;
        this.dupeRule = dupeRule;
    }

    public ProductionItem(ResourceCost cost) {
        this.defaultCost = cost;
        this.dupeRule = ProdDupeRule.ALLOW;
    }

    // allows for dynamic costs in subclasses
    public ResourceCost getCost(boolean isClientSide, String ownerName) {
        return defaultCost;
    }

    public String getItemName() {
        return itemName;
    }

    public boolean canAfford(Level level, String ownerName) {
        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName))
                return (resources.food >= getCost(level.isClientSide(), ownerName).food &&
                        resources.wood >= getCost(level.isClientSide(), ownerName).wood &&
                        resources.ore >= getCost(level.isClientSide(), ownerName).ore &&
                        canAffordPopulation(level, ownerName));
        return false;
    }

    public boolean canAffordPopulation(Level level, String ownerName) {
        if (getCost(level.isClientSide(), ownerName).population == 0)
            return true;

        int currentPop = UnitServerEvents.getCurrentPopulation((ServerLevel) level, ownerName);
        int popSupply = BuildingServerEvents.getTotalPopulationSupply(ownerName);

        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName))
                return (currentPop + getCost(level.isClientSide(), ownerName).population) <= popSupply;
        return false;
    }

    // check we didn't dip below pop supply after starting production
    public boolean isBelowPopulationSupply(Level level, String ownerName) {
        if (getCost(level.isClientSide(), ownerName).population == 0)
            return true;

        int currentPop;
        int popSupply;
        if (level.isClientSide()) {
            currentPop = UnitClientEvents.getCurrentPopulation(ownerName);
            popSupply = BuildingClientEvents.getTotalPopulationSupply(ownerName);
        } else {
            currentPop = UnitServerEvents.getCurrentPopulation((ServerLevel) level, ownerName);
            popSupply = BuildingServerEvents.getTotalPopulationSupply(ownerName);
        }
        return currentPop <= popSupply;
    }

    public boolean isBelowMaxPopulation(Level level, String ownerName) {
        if (getCost(level.isClientSide(), ownerName).population == 0)
            return true;

        int currentPop = UnitServerEvents.getCurrentPopulation((ServerLevel) level, ownerName);
        int popSupply = BuildingServerEvents.getTotalPopulationSupply(ownerName);

        for (Resources resources : ResourcesServerEvents.resourcesList) {
            if (resources.ownerName.equals(ownerName)) {
                if (level.isClientSide())
                    return (currentPop + getCost(level.isClientSide(), ownerName).population) <= GameruleClient.maxPopulation;
                else
                    return (currentPop + getCost(level.isClientSide(), ownerName).population) <= UnitServerEvents.maxPopulation;
            }
        }
        return false;
    }

    // some items (eg. research) are enabled only if the item doesn't exist in any existing clientside queue
    public boolean itemIsBeingProduced(String ownerName) {
        return itemIsBeingProduced(true, ownerName);
    }

    public boolean itemIsBeingProduced(boolean isClientSide, String ownerName) {
        List<BuildingPlacement> buildings = isClientSide ? BuildingClientEvents.getBuildings() : BuildingServerEvents.getBuildings();

        for (BuildingPlacement building : buildings)
            if (building.ownerName.equals(ownerName) && building instanceof ProductionPlacement prodBuilding)
                for (ActiveProduction prodItem : prodBuilding.productionQueue)
                    if (prodItem.item == this)
                        return true;
        return false;
    }

    // check if this is being produced at one particular building
    public boolean itemIsBeingProducedAt(ProductionPlacement placement) {
        return itemIsBeingProducedAt(true, placement);
    }

    public boolean itemIsBeingProducedAt(boolean isClientSide, ProductionPlacement placement) {
        List<BuildingPlacement> buildings = isClientSide ? BuildingClientEvents.getBuildings() : BuildingServerEvents.getBuildings();

        for (BuildingPlacement building : buildings)
            if (building == placement)
                for (ActiveProduction prodItem : placement.productionQueue)
                    if (prodItem.item == this)
                        return true;
        return false;
    }

    // Button object to build
    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding keybinding) {
        return null;
    }
    // Button object to show in-progress items
    // firstItem means this button will cancel the currently-building item
    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return null;
    }

    public void recordScore(ProductionPlacement placement) {
        if (!placement.getLevel().isClientSide()) {
            RTSPlayer rtsPlayer = PlayerServerEvents.getRTSPlayer(placement.ownerName);
            if (rtsPlayer != null) {
                rtsPlayer.scores.addToScore(RTSPlayerScoresEnum.TOTAL_UNITS_PRODUCED);
                if (List.of(
                        ProductionItems.VILLAGER,
                        ProductionItems.ZOMBIE_VILLAGER,
                        ProductionItems.GRUNT
                ).contains(this))
                    rtsPlayer.scores.addToScore(RTSPlayerScoresEnum.WORKER_UNITS_PRODUCED);
                else
                    rtsPlayer.scores.addToScore(RTSPlayerScoresEnum.MILITARY_UNITS_PRODUCED);
            }
        }
    }

    // return true if the tick finished
    public boolean tick(ProductionPlacement placement, ActiveProduction active) {
        if (active.ticksLeft > 0 && isBelowPopulationSupply(placement.getLevel(), placement.ownerName) && placement.isBuilt) {
            if ((placement.getLevel().isClientSide() && ResearchClient.hasCheat("warpten")) ||
                (!placement.getLevel().isClientSide() && ResearchServerEvents.playerHasCheat(placement.ownerName, "warpten"))) {
                if (placement.getLevel().isClientSide())
                    active.ticksLeft -= (TPSClientEvents.getCappedTPS() / 20D) * 10;
                else
                    active.ticksLeft -= 10;
            }
            else {
                if (placement.getLevel().isClientSide())
                    active.ticksLeft -= (TPSClientEvents.getCappedTPS() / 20D);
                else
                    active.ticksLeft -= 1;
            }

            if (active.ticksLeft < 0)
                active.ticksLeft = 0;
        }
        if (active.ticksLeft <= 0 && isBelowPopulationSupply(placement.getLevel(), placement.ownerName)) {
            this.recordScore(placement);
            if (!active.completed) {
                onComplete.accept(placement.getLevel(), placement);
                active.completed = true;
                return true;
            }
        }
        return false;
    }
}
