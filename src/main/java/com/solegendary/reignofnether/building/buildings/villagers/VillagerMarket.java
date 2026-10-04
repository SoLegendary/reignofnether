package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.shared.AbstractMarket;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.items.StockedShopItem;
import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.items.unititems.EdibleFoodItem;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

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
    public String getUpgradedName(BuildingPlacement placement) {
        return Component.translatable("buildings.reignofnether.villager_market_upgraded").getString();
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
    public ArrayList<StockedShopItem> getStartingItemsAndStock() {
        return new ArrayList<>(List.of(
                new StockedShopItem(UnitItems.HEALTH_POTION, 3, 60 * 20),
                new StockedShopItem(UnitItems.MANA_POTION, 3, 60 * 20),
                new StockedShopItem(UnitItems.BROADSWORD, 1, 180 * 20),
                new StockedShopItem(UnitItems.IRON_HIDE_AMULET, 1, 180 * 20),
                new StockedShopItem(UnitItems.HEART_MEDALLION, 1, 180 * 20),
                new StockedShopItem(UnitItems.AZURE_MEDALLION, 1, 180 * 20)
        ));
    }

    @Override
    public ArrayList<StockedShopItem> getUpgradedItemsAndStock() {
        return new ArrayList<>(List.of(
                new StockedShopItem(UnitItems.FROST_WALKER_BOOTS, 1, 600 * 20),
                new StockedShopItem(UnitItems.WAR_HORN, 1, 600 * 20),
                new StockedShopItem(UnitItems.BELL_OF_ARMS, 1, 600 * 20)
        ));
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/emerald_block.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                TutorialClientEvents::isEnabled,
                () -> true,
                List.of(
                        Component.translatable("buildings.reignofnether.villager_market").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.EMPTY,
                        Component.translatable("buildings.reignofnether.villager_market.tooltip1").getVisualOrderText(),
                        FormattedCharSequence.EMPTY,
                        Component.translatable("buildings.reignofnether.villager_market.tooltip2").getVisualOrderText()
                ),
                this
        );
    }
}