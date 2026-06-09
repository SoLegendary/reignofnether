package com.solegendary.reignofnether.building.buildings.shared;

import com.solegendary.reignofnether.ability.abilities.TradeAbility;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceName;

// Faction-agnostic Market base. AoE2-style resource trading: 6 buttons that swap 100 of one resource
// for the current rate of another. Each trade worsens that direction's rate by RATE_STEP and improves
// the opposite by the same. Rates are stored on RTSPlayer so they survive rebuilding the market.
public abstract class Market extends ProductionBuilding {

    public static final int START_RATE = 75;
    public static final int MIN_RATE = 10;
    public static final int RATE_STEP = 1;
    public static final int TRADE_CHUNK = 100;

    public Market(String structureName, ResourceCost cost) {
        super(structureName, cost, false);
        this.canSetRallyPoint = false;

        this.abilities.add(new TradeAbility(ResourceName.FOOD, ResourceName.WOOD), Keybindings.abilitySlot1);
        this.abilities.add(new TradeAbility(ResourceName.FOOD, ResourceName.ORE),  Keybindings.abilitySlot2);
        this.abilities.add(new TradeAbility(ResourceName.WOOD, ResourceName.FOOD), Keybindings.abilitySlot3);
        this.abilities.add(new TradeAbility(ResourceName.WOOD, ResourceName.ORE),  Keybindings.abilitySlot4);
        this.abilities.add(new TradeAbility(ResourceName.ORE,  ResourceName.FOOD), Keybindings.abilitySlot5);
        this.abilities.add(new TradeAbility(ResourceName.ORE,  ResourceName.WOOD), Keybindings.abilitySlot6);
    }
}
