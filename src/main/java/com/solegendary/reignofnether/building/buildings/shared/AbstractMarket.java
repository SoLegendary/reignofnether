package com.solegendary.reignofnether.building.buildings.shared;

import com.solegendary.reignofnether.ability.abilities.TradeResources;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.ItemShopAddon;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.HashMap;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

// AoE2-style resource trading: 6 buttons that swap 100 of one resource for the current rate of another.
// Each trade worsens that direction's rate by RATE_STEP and improves
// the opposite by the same. Rates are stored on RTSPlayer so they survive rebuilding the market.
public abstract class AbstractMarket extends Building implements ItemShopAddon {

    public AbstractMarket(String structureName, ResourceCost cost) {
        super(structureName, cost, false);

        this.abilities.add(new TradeResources(UnitAction.TRADE_FOOD_FOR_WOOD), Keybindings.abilitySlot1);
        this.abilities.add(new TradeResources(UnitAction.TRADE_FOOD_FOR_ORE),  Keybindings.abilitySlot2);
        this.abilities.add(new TradeResources(UnitAction.TRADE_WOOD_FOR_FOOD), Keybindings.abilitySlot3);
        this.abilities.add(new TradeResources(UnitAction.TRADE_WOOD_FOR_ORE),  Keybindings.abilitySlot4);
        this.abilities.add(new TradeResources(UnitAction.TRADE_ORE_FOR_FOOD), Keybindings.abilitySlot5);
        this.abilities.add(new TradeResources(UnitAction.TRADE_ORE_FOR_WOOD), Keybindings.abilitySlot6);

        this.maxHealth = 300d;

        setActiveAddon(ItemShopAddon.class, this, true);
    }

    public ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocksFromNbt(structureName, level);
    }

    protected abstract HashMap<UnitItem, Integer> getStartingItemsAndStock();

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        BuildingPlacement bpl = new MarketPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation));
        bpl.getDataStorage().setData(ItemShopAddon.ITEMS_AND_STOCK, getStartingItemsAndStock());
        return bpl;
    }
}