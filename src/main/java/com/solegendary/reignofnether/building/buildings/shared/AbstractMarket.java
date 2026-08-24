package com.solegendary.reignofnether.building.buildings.shared;

import com.solegendary.reignofnether.ability.TradeAction;
import com.solegendary.reignofnether.ability.abilities.TradeResources;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;

// AoE2-style resource trading: 6 buttons that swap 100 of one resource for the current rate of another.
// Each trade worsens that direction's rate by RATE_STEP and improves
// the opposite by the same. Rates are stored on RTSPlayer so they survive rebuilding the market.
public abstract class AbstractMarket extends Building {

    public AbstractMarket(String structureName, ResourceCost cost) {
        super(structureName, cost, false);

        this.abilities.add(new TradeResources(UnitAction.TRADE_FOOD_FOR_WOOD), Keybindings.abilitySlot1);
        this.abilities.add(new TradeResources(UnitAction.TRADE_FOOD_FOR_ORE),  Keybindings.abilitySlot2);
        this.abilities.add(new TradeResources(UnitAction.TRADE_WOOD_FOR_FOOD), Keybindings.abilitySlot3);
        this.abilities.add(new TradeResources(UnitAction.TRADE_WOOD_FOR_ORE),  Keybindings.abilitySlot4);
        this.abilities.add(new TradeResources(UnitAction.TRADE_ORE_FOR_FOOD), Keybindings.abilitySlot5);
        this.abilities.add(new TradeResources(UnitAction.TRADE_ORE_FOR_WOOD), Keybindings.abilitySlot6);

        this.maxHealth = 300d;
    }
}