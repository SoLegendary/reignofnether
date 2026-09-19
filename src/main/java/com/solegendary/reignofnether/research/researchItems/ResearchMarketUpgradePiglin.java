package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.piglins.PiglinMarket;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
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

public class ResearchMarketUpgradePiglin extends ProductionItem {

    public static final String itemName = "Commercial Portal";
    public static final ResourceCost cost = ResourceCosts.RESEARCH_UPGRADE_MARKET;


    public ResearchMarketUpgradePiglin() {
        super(cost, ProdDupeRule.DISALLOW_FOR_BUILDING);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide() && placement.getBuilding() instanceof PiglinMarket) {
                placement.changeStructure(PiglinMarket.upgradedStructureName);
            }
        };
    }

    @Override
    public boolean canProduce(ProductionPlacement pp) {
        return pp.getUpgradeLevel() <= 0 && BuildingUtils.numFinishedBuildings(pp.level.isClientSide(), Buildings.PORTAL_CIVILIAN, pp.ownerName) >= 4;
    }

    @Override
    public String getItemName() {
        return itemName;
    }

    @Override
    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
                itemName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/raw_gold_block.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> ProductionItems.RESEARCH_MARKET_UPGRADE_PIGLINS.itemIsBeingProducedAt(prodBuilding) ||
                        (prodBuilding.getBuilding() instanceof PiglinMarket && prodBuilding.getUpgradeLevel() > 0),
                () -> prodBuilding.getBuilding() instanceof PiglinMarket && prodBuilding.getUpgradeLevel() == 0 && canProduce(prodBuilding),
                List.of(
                        fcs(I18n.get("research.reignofnether.piglin_market_upgrade"), true),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedTime(cost),
                        fcs(""),
                        fcs(I18n.get("research.reignofnether.piglin_market_upgrade.tooltip1")),
                        fcs(""),
                        fcs(I18n.get("research.reignofnether.piglin_market_upgrade.tooltip2"))
                ),
                this
        );
    }

    @Override
    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
                itemName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/raw_gold_block.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                prodBuilding,
                this,
                first
        );
    }
}