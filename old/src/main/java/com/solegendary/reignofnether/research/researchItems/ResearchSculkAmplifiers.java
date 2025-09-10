package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.Buildings;
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

public class ResearchSculkAmplifiers extends ProductionItem {

    public final static String itemName = "Sculk Amplifiers";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_SCULK_AMPLIFIERS;

    public final static float SPLIT_BOOM_DAMAGE_MULT = 0.666f;
    public final static int SPLIT_BOOM_RANGE = 20;
    public final static int SPLIT_BOOM_AMOUNT = 3;

    public ResearchSculkAmplifiers() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (level.isClientSide())
                ResearchClient.addResearch(placement.ownerName, ProductionItems.RESEARCH_SCULK_AMPLIFIERS);
            else {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_SCULK_AMPLIFIERS);
            }
        };
    }

    public String getItemName() {
        return ResearchSculkAmplifiers.itemName;
    }

    public Button getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new Button(
            ResearchSculkAmplifiers.itemName,
            14,
            new ResourceLocation("minecraft", "textures/block/sculk_shrieker_side.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> false,
            () -> ProductionItems.RESEARCH_SCULK_AMPLIFIERS.itemIsBeingProduced(prodBuilding.ownerName) ||
                    ResearchClient.hasResearch(ProductionItems.RESEARCH_SCULK_AMPLIFIERS),
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.STRONGHOLD),
            () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, ProductionItems.RESEARCH_SCULK_AMPLIFIERS),
            null,
            List.of(
                FormattedCharSequence.forward(I18n.get("research.reignofnether.sculk_amplifiers"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("research.reignofnether.sculk_amplifiers.tooltip1"), Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("research.reignofnether.sculk_amplifiers.tooltip2", SPLIT_BOOM_AMOUNT, SPLIT_BOOM_RANGE), Style.EMPTY),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("research.reignofnether.sculk_amplifiers.tooltip3"), Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new Button(
                ResearchSculkAmplifiers.itemName,
                14,
                new ResourceLocation("minecraft", "textures/block/sculk_shrieker_side.png"),
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                null,
                () -> false,
                () -> false,
                () -> true,
                () -> BuildingServerboundPacket.cancelProduction(prodBuilding.minCorner, ProductionItems.RESEARCH_SCULK_AMPLIFIERS, first),
                null,
                null
        );
    }
}
