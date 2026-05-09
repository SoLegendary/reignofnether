package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchPossession extends ProductionItem {

    public final static String itemName = "Possession";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_POSSESSION;

    public ResearchPossession() {
        super(cost, ProdDupeRule.DISALLOW);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (level.isClientSide())
                ResearchClient.addResearch(placement.ownerName, ProductionItems.RESEARCH_POSSESSION);
            else {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_POSSESSION);
            }
        };
    }

    public String getItemName() {
        return ResearchPossession.itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
                ResearchPossession.itemName,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/possess.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> ProductionItems.RESEARCH_POSSESSION.itemIsBeingProduced(prodBuilding.ownerName) ||
                        ResearchClient.hasResearch(ProductionItems.RESEARCH_POSSESSION),
                () -> true,
                List.of(
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.possession"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedTime(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.possession.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.possession.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.possession.tooltip3"), Style.EMPTY)
                ),
                this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
                ResearchPossession.itemName,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/possess.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                prodBuilding,
                this,
                first
        );
    }
}
