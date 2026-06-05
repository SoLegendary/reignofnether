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

public class ResearchUpgradedWindcallers extends ProductionItem {

    public final static String itemName = "Borne upon a Wind";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_UPGRADED_WINDCALLERS;

    public ResearchUpgradedWindcallers() {
        super(cost, ProdDupeRule.DISALLOW);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (level.isClientSide()) {
                ResearchClient.addResearch(placement.ownerName, ProductionItems.RESEARCH_UPGRADED_WINDCALLERS);
            } else {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_UPGRADED_WINDCALLERS);
            }
        };
    }

    public String getItemName() {
        return ResearchUpgradedWindcallers.itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(ResearchUpgradedWindcallers.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/gale_force.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> ProductionItems.RESEARCH_UPGRADED_WINDCALLERS.itemIsBeingProduced(prodBuilding.ownerName)
                || ResearchClient.hasResearch(ProductionItems.RESEARCH_UPGRADED_WINDCALLERS),
            () -> true,
            List.of(FormattedCharSequence.forward(I18n.get("research.reignofnether.upgraded_windcallers"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.upgraded_windcallers.tooltip1"), Style.EMPTY)
            ),
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(ResearchUpgradedWindcallers.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/gale_force.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            prodBuilding,
            this,
            first
        );
    }
}
