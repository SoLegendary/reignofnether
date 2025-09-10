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

public class ResearchSlimeConversion extends ProductionItem {

    public final static String itemName = "Slimy Conversion";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_SLIME_CONVERSION;

    public ResearchSlimeConversion() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (level.isClientSide()) {
                ResearchClient.addResearch(placement.ownerName, ProductionItems.RESEARCH_SLIME_CONVERSION);
            } else {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_SLIME_CONVERSION);
            }
        };
    }

    public String getItemName() {
        return ResearchSlimeConversion.itemName;
    }

    public Button getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new Button(ResearchSlimeConversion.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/slime.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> false,
            () -> ProductionItems.RESEARCH_SLIME_CONVERSION.itemIsBeingProduced(prodBuilding.ownerName)
                || ResearchClient.hasResearch(ProductionItems.RESEARCH_SLIME_CONVERSION),
            () -> true,
            () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, ProductionItems.RESEARCH_SLIME_CONVERSION),
            null,
            List.of(FormattedCharSequence.forward(I18n.get("research.reignofnether.slime_conversion"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.slime_conversion.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.slime_conversion.tooltip2"), Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new Button(ResearchSlimeConversion.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/slime.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            null,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.cancelProduction(prodBuilding.minCorner, ProductionItems.RESEARCH_SLIME_CONVERSION, first),
            null,
            null
        );
    }
}
