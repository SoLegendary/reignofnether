package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
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

public class ResearchHoglinCavalry extends ProductionItem {

    public final static String itemName = "Hoglin Cavalry";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_HOGLIN_CAVALRY;

    public ResearchHoglinCavalry() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (level.isClientSide()) {
                ResearchClient.addResearch(placement.ownerName, ProductionItems.RESEARCH_HOGLIN_CAVALRY);
            } else {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_HOGLIN_CAVALRY);
            }
        };
    }

    public Button getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new Button(ResearchHoglinCavalry.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/hoglin.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> false,
            () -> ProductionItems.RESEARCH_HOGLIN_CAVALRY.itemIsBeingProduced(prodBuilding.ownerName)
                || ResearchClient.hasResearch(ProductionItems.RESEARCH_HOGLIN_CAVALRY),
            () -> true,
            () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, ProductionItems.RESEARCH_HOGLIN_CAVALRY),
            null,
            List.of(FormattedCharSequence.forward(I18n.get("research.reignofnether.hoglin_cavalry"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.hoglin_cavalry.tooltip1"), Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new Button(ResearchHoglinCavalry.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/hoglin.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            null,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.cancelProduction(prodBuilding.minCorner, ProductionItems.RESEARCH_HOGLIN_CAVALRY, first),
            null,
            null
        );
    }
}
