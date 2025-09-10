package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.Button;
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

public class ResearchWaterPotions extends ProductionItem {

    public final static String itemName = "Water Potions";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_WATER_POTIONS;

    public ResearchWaterPotions() {
        super(ResourceCosts.RESEARCH_WATER_POTIONS);
        this.onComplete = (Level level, ProductionPlacement building) -> {
            if (level.isClientSide()) {
                ResearchClient.addResearch(building.ownerName, ProductionItems.RESEARCH_WATER_POTIONS);
            } else {
                ResearchServerEvents.addResearch(building.ownerName, ProductionItems.RESEARCH_WATER_POTIONS);
            }
        };
    }

    public Button getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new Button(ResearchWaterPotions.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/splash_potion_water.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> false,
            () -> ProductionItems.RESEARCH_WATER_POTIONS.itemIsBeingProduced(prodBuilding.ownerName)
                || ResearchClient.hasResearch(ProductionItems.RESEARCH_WATER_POTIONS),
            () -> true,
            () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, ProductionItems.RESEARCH_WATER_POTIONS),
            null,
            List.of(FormattedCharSequence.forward(I18n.get("research.reignofnether.water_potions"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.water_potions.tooltip1"),
                    Style.EMPTY
                )
            )
        );
    }

    public Button getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new Button(ResearchWaterPotions.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/splash_potion_water.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            null,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.cancelProduction(prodBuilding.minCorner, ProductionItems.RESEARCH_WATER_POTIONS, first),
            null,
            null
        );
    }
}
