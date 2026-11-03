package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
import com.solegendary.reignofnether.building.production.ProdDupeRule;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ResearchGrandLibrary extends ProductionItem {

    public final static String itemName = "Grand Library";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_GRAND_LIBRARY;

    public ResearchGrandLibrary() {
        super(cost, ProdDupeRule.DISALLOW_FOR_BUILDING);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide() && placement.getBuilding() instanceof Library)
                placement.changeStructure(Library.upgradedStructureName);
        };
    }

    public String getItemName() {
        return ResearchGrandLibrary.itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
                ResearchGrandLibrary.itemName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/bookshelf.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> ProductionItems.RESEARCH_GRAND_LIBRARY.itemIsBeingProducedAt(prodBuilding) ||
                        (prodBuilding.getBuilding() instanceof Library && prodBuilding.getUpgradeLevel() > 0),
                () -> true,
                List.of(
                        fcs(I18n.get("research.reignofnether.grand_library"), true),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedTime(cost),
                        fcs(""),
                        fcs(I18n.get("research.reignofnether.grand_library.tooltip1"))
                ),
                this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
                ResearchGrandLibrary.itemName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/bookshelf.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                prodBuilding,
                this,
                first
        );
    }
}
