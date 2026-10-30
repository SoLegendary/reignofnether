package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProdDupeRule;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ResearchBlazeFirewall extends ProductionItem {

    public final static String itemName = "Walls of Fire";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_BLAZE_FIRE_WALL;

    public ResearchBlazeFirewall() {
        super(cost, ProdDupeRule.DISALLOW);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide()) {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_BLAZE_FIREWALL);
            }
        };
    }

    public String getItemName() {
        return ResearchBlazeFirewall.itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(ResearchBlazeFirewall.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/fire.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> ProductionItems.RESEARCH_BLAZE_FIREWALL.itemIsBeingProduced(prodBuilding.ownerName)
                || ResearchClient.hasResearch(ProductionItems.RESEARCH_BLAZE_FIREWALL),
            () -> true,
            List.of(fcs(I18n.get("research.reignofnether.blaze_firewall"), true),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                fcs(""),
                fcs(I18n.get("research.reignofnether.blaze_firewall.tooltip1")),
                fcs(I18n.get("research.reignofnether.blaze_firewall.tooltip2"))
            ),
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(ResearchBlazeFirewall.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/fire.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            prodBuilding,
            this,
            first
        );
    }
}
