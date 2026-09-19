package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.shared.AbstractMarket;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.items.StockedShopItem;
import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.items.unititems.EdibleFoodItem;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class VillagerMarket extends AbstractMarket {

    public static final String buildingName = "Town Market";
    public static final String structureName = "market_villagers1";
    public static final String upgradedStructureName = "market_villagers2";
    public static final ResourceCost cost = ResourceCosts.VILLAGER_MARKET;

    public VillagerMarket() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.EMERALD_BLOCK;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/emerald_block.png");

        this.buildTimeModifier = 0.8f;
        this.maxHealth = 300d;

        this.startingBlockTypes.add(Blocks.COBBLESTONE);
        this.startingBlockTypes.add(Blocks.STONE);

        this.productions.add(ProductionItems.RESEARCH_MARKET_UPGRADE_VILLAGER, Keybindings.abilitySlot4);
    }

    @Override
    public String getUpgradedStructureName(int upgradeLevel) {
        return upgradeLevel > 0 ? upgradedStructureName : structureName;
    }

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        for (BuildingBlock block : placement.getBlocks())
            if (block.getBlockState().getBlock() == Blocks.YELLOW_WOOL) {
                return 1;
            }
        return 0;
    }

    @Override
    protected ArrayList<StockedShopItem> getStartingItemsAndStock() {
        return new ArrayList<>(List.of(
                new StockedShopItem(UnitItems.HEALTH_POTION, 1, 60 * 20),
                new StockedShopItem(UnitItems.MANA_POTION, 1, 60 * 20)
        ));
    }

    public Faction getFaction() { return Faction.VILLAGERS; }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/emerald_block.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                TutorialClientEvents::isEnabled,
                () -> true,
                List.of(
                        fcs(I18n.get("buildings.reignofnether.villager_market"), true),
                        ResourceCosts.getFormattedCost(cost),
                        fcs(""),
                        fcs(I18n.get("buildings.reignofnether.villager_market.tooltip1")),
                        fcs(""),
                        fcs(I18n.get("buildings.reignofnether.villager_market.tooltip2"))
                ),
                this
        );
    }
}