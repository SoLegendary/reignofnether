package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything a player has queued across all of their production buildings, grouped by item. */
public final class GlobalProductionQueue {

    private GlobalProductionQueue() { }

    public static final class Group {
        public final ProductionItem item;
        public final List<ProductionPlacement> placements = new ArrayList<>();
        // the queued entry closest to completion, and where it is
        public ActiveProduction soonest;
        public ProductionPlacement soonestPlacement;
        public int count;
        // true if one of the entries is actually being produced (front of its building's queue)
        public boolean inProgress;

        Group(ProductionItem item) {
            this.item = item;
        }

        void add(ActiveProduction p, int index, ProductionPlacement placement) {
            count++;
            if (!placements.contains(placement))
                placements.add(placement);
            if (index == 0)
                inProgress = true;
            if (soonest == null || p.ticksLeft < soonest.ticksLeft) {
                soonest = p;
                soonestPlacement = placement;
            }
        }

        /** 0 when just started, 1 when done. */
        public float progress() {
            if (soonest == null || !inProgress)
                return 0f;
            float total = soonest.item.getCost(true, soonestPlacement.ownerName).ticks;
            return total <= 0 ? 1f : Math.max(0f, Math.min(1f, 1f - soonest.ticksLeft / total));
        }

        public int secondsLeft() {
            return soonest == null ? 0 : (int) Math.ceil(soonest.ticksLeft / 20f);
        }

        public boolean isPopBlocked() {
            return soonest != null && !soonest.item.isBelowPopulationSupply(soonestPlacement);
        }
    }

    public static List<Group> forPlayer(String playerName) {
        if (playerName == null)
            return Collections.emptyList();
        Map<ProductionItem, Group> grouped = new LinkedHashMap<>();
        for (BuildingPlacement b : BuildingClientEvents.getBuildings()) {
            if (b instanceof ProductionPlacement p && playerName.equals(p.ownerName)) {
                for (int i = 0; i < p.productionQueue.size(); i++) {
                    ActiveProduction production = p.productionQueue.get(i);
                    grouped.computeIfAbsent(production.item, Group::new).add(production, i, p);
                }
            }
        }
        return new ArrayList<>(grouped.values());
    }
}
