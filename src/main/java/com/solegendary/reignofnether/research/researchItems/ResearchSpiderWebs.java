package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProdDupeRule;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchSpiderWebs extends ProductionItem {

    public final static String itemName = "Sticky Webbing";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_SPIDER_JOCKEYS;

    public ResearchSpiderWebs() {
        super(cost, ProdDupeRule.DISALLOW);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (level.isClientSide())
                ResearchClient.addResearch(placement.ownerName, ProductionItems.RESEARCH_SPIDER_WEBS);
            else {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_SPIDER_WEBS);
            }
        };
    }

    public String getItemName() {
        return ResearchSpiderWebs.itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
                ResearchSpiderWebs.itemName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/cobweb.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> ProductionItems.RESEARCH_SPIDER_WEBS.itemIsBeingProduced(prodBuilding.ownerName) ||
                        ResearchClient.hasResearch(ProductionItems.RESEARCH_SPIDER_WEBS),
                () -> true,
                List.of(
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.sticky_webbing"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedTime(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.sticky_webbing.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.sticky_webbing.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.sticky_webbing.tooltip3"), Style.EMPTY)
                ),
                this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
                ResearchSpiderWebs.itemName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/cobweb.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                prodBuilding,
                this,
                first
        );
    }
}
