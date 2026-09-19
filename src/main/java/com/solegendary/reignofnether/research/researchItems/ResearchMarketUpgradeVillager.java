package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.VillagerMarket;
import com.solegendary.reignofnether.building.production.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ResearchMarketUpgradeVillager extends ProductionItem {

    public static final String itemName = "Grand Market";
    public static final ResourceCost cost = ResourceCosts.RESEARCH_UPGRADE_MARKET;

    public ResearchMarketUpgradeVillager() {
        super(cost, ProdDupeRule.DISALLOW_FOR_BUILDING);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide() && placement.getBuilding() instanceof VillagerMarket) {
                placement.changeStructure(VillagerMarket.upgradedStructureName);
            }
        };
    }

    @Override
    public boolean canProduce(ProductionPlacement pp) {
        return pp.getUpgradeLevel() <= 0 && BuildingUtils.numFinishedBuildings(pp.level.isClientSide(), Buildings.VILLAGER_HOUSE, pp.ownerName) >= 6;
    }

    @Override
    public String getItemName() {
        return itemName;
    }

    @Override
    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
                itemName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/emerald_block.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> ProductionItems.RESEARCH_MARKET_UPGRADE_VILLAGER.itemIsBeingProducedAt(prodBuilding) ||
                        (prodBuilding.getBuilding() instanceof VillagerMarket && prodBuilding.getUpgradeLevel() > 0),
                () -> prodBuilding.getBuilding() instanceof VillagerMarket && prodBuilding.getUpgradeLevel() == 0 && canProduce(prodBuilding),
                List.of(
                        fcs(I18n.get("research.reignofnether.villager_market_upgrade"), true),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedTime(cost),
                        fcs(""),
                        fcs(I18n.get("research.reignofnether.villager_market_upgrade.tooltip1")),
                        fcs(""),
                        fcs(I18n.get("research.reignofnether.villager_market_upgrade.tooltip2"))
                ),
                this
        );
    }

    @Override
    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
                itemName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/emerald_block.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                prodBuilding,
                this,
                first
        );
    }
}