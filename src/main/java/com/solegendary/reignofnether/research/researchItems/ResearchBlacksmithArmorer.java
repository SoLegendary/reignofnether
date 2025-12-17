package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.BlacksmithPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.Blacksmith;
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

public class ResearchBlacksmithArmorer extends ProductionItem {

    public final static String itemName = "Blacksmith Armorer";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_BLACKSMITH_ARMORER;

    public ResearchBlacksmithArmorer() {
        super(cost, ProdDupeRule.DISALLOW_FOR_BUILDING);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (placement instanceof BlacksmithPlacement blacksmithPlacement)
                blacksmithPlacement.changeStructure(Blacksmith.upgradedStructureName);
        };
    }

    public String getItemName() {
        return ResearchBlacksmithArmorer.itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
                ResearchBlacksmithArmorer.itemName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_chestplate.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> ProductionItems.RESEARCH_BLACKSMITH_ARMORER.itemIsBeingProducedAt(prodBuilding) ||
                        (prodBuilding instanceof BlacksmithPlacement && prodBuilding.getUpgradeLevel() > 0),
                () -> true,
                List.of(
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.blacksmith_armorer"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedTime(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.blacksmith_armorer.tooltip1"), Style.EMPTY)
                ),
                this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
                ResearchBlacksmithArmorer.itemName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_chestplate.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                prodBuilding,
                this,
                first
        );
    }
}

