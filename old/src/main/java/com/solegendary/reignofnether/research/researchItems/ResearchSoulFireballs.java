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

public class ResearchSoulFireballs extends ProductionItem {

    public final static String itemName = "Soul Fireballs";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_SOUL_FIREBALLS;

    public ResearchSoulFireballs() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (level.isClientSide()) {
                ResearchClient.addResearch(placement.ownerName, ProductionItems.RESEARCH_SOUL_FIREBALLS);
            } else {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_SOUL_FIREBALLS);
            }
        };
    }

    public Button getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new Button(ResearchSoulFireballs.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/soul_fireball.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> false,
            () -> ProductionItems.RESEARCH_SOUL_FIREBALLS.itemIsBeingProduced(prodBuilding.ownerName)
                || ResearchClient.hasResearch(ProductionItems.RESEARCH_SOUL_FIREBALLS),
            () -> true,
            () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, ProductionItems.RESEARCH_SOUL_FIREBALLS),
            null,
            List.of(FormattedCharSequence.forward(I18n.get("research.reignofnether.soul_fireballs"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.soul_fireballs.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.soul_fireballs.tooltip2"), Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new Button(ResearchSoulFireballs.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/soul_fireball.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            null,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.cancelProduction(prodBuilding.minCorner, ProductionItems.RESEARCH_SOUL_FIREBALLS, first),
            null,
            null
        );
    }
}
