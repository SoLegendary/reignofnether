package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.units.piglins.HeadhunterUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchGreedyTridents extends ProductionItem {

    public final static String itemName = "Greedy Tridents";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_GREEDY_TRIDENTS;

    public ResearchGreedyTridents() {
        super(cost, ProdDupeRule.DISALLOW);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (level.isClientSide()) {
                ResearchClient.addResearch(placement.ownerName, ProductionItems.RESEARCH_GREEDY_TRIDENTS);
            } else {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_GREEDY_TRIDENTS);
                for (LivingEntity le : UnitServerEvents.getAllUnits()) {
                    if (le instanceof HeadhunterUnit headhunterUnit)
                        headhunterUnit.setupEquipmentAndUpgradesServer();
                }
            }
        };
    }

    public String getItemName() {
        return ResearchGreedyTridents.itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        StartProductionButton button = new StartProductionButton(ResearchGreedyTridents.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/trident.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> ProductionItems.RESEARCH_GREEDY_TRIDENTS.itemIsBeingProduced(prodBuilding.ownerName)
                || ResearchClient.hasResearch(ProductionItems.RESEARCH_GREEDY_TRIDENTS),
            () -> true,
            List.of(FormattedCharSequence.forward(I18n.get("research.reignofnether.greedy_tridents"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.greedy_tridents.tooltip1"), Style.EMPTY)
            ),
            this
        );
        button.bgIconResource = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/emerald.png");
        return button;
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        StopProductionButton button = new StopProductionButton(ResearchGreedyTridents.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/trident.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            prodBuilding,
            this,
            first
        );
        button.bgIconResource = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/emerald.png");
        return button;
    }
}
