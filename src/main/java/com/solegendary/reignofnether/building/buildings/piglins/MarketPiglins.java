package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.shared.Market;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class MarketPiglins extends Market {

    public static final String buildingName = "Market";
    public static final String structureName = "market_piglins";
    public static final ResourceCost cost = ResourceCosts.MARKET_PIGLINS;

    public MarketPiglins() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.GOLD_BLOCK;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/gold_block.png");

        this.startingBlockTypes.add(Blocks.NETHER_BRICK_FENCE);
    }

    public Faction getFaction() { return Faction.PIGLINS; }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/gold_block.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.MARKET_PIGLINS,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.EXPLAIN_BUILDINGS),
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.CENTRAL_PORTAL) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.market_piglins"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.market_piglins.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.market_piglins.tooltip2"), Style.EMPTY)
                ),
                this
        );
    }
}
