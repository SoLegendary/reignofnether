package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.GraveyardPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.buildings.monsters.Graveyard;
import com.solegendary.reignofnether.building.production.ProdDupeRule;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchOverflowingGraveyard extends ProductionItem {

    public final static String itemName = "Overflowing Graveyard";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_OVERFLOWING_GRAVEYARD;

    public ResearchOverflowingGraveyard() {
        super(cost, ProdDupeRule.DISALLOW_FOR_BUILDING);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (placement instanceof GraveyardPlacement graveyardPlacement)
                graveyardPlacement.changeStructure(Graveyard.upgradedStructureName);
        };
    }

    public String getItemName() {
        return ResearchOverflowingGraveyard.itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
                ResearchOverflowingGraveyard.itemName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/soul_lantern.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> ProductionItems.RESEARCH_OVERFLOWING_GRAVEYARD.itemIsBeingProducedAt(prodBuilding) ||
                        (prodBuilding instanceof GraveyardPlacement && prodBuilding.getUpgradeLevel() > 0),
                () -> true,
                List.of(
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.overflowing_graveyard"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedTime(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.overflowing_graveyard.tooltip1"), Style.EMPTY)
                ),
                this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
                ResearchOverflowingGraveyard.itemName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/soul_lantern.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                prodBuilding,
                this,
                first
        );
    }
}

