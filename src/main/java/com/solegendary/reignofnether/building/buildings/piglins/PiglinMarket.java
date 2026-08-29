package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.NetherZone;
import com.solegendary.reignofnether.building.addon.NetherConvertingAddon;
import com.solegendary.reignofnether.building.buildings.shared.AbstractMarket;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class PiglinMarket extends AbstractMarket implements NetherConvertingAddon {

    public static final String buildingName = "Commercial Portal";
    public static final String structureName = "market_piglins";
    public static final ResourceCost cost = ResourceCosts.PIGLIN_MARKET;

    public PiglinMarket() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.RAW_GOLD_BLOCK;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/raw_gold_block.png");

        this.startingBlockTypes.add(Blocks.BLACKSTONE);
        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE_BRICKS);
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/raw_gold_block.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.EXPLAIN_BUILDINGS),
                () -> BuildingClientEvents.numFinishedBuildings(Buildings.PORTAL_CIVILIAN) >= 4 ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        Component.translatable("buildings.reignofnether.piglin_market").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.EMPTY,
                        Component.translatable("buildings.reignofnether.piglin_market.tooltip1").getVisualOrderText(),
                        Component.translatable("buildings.reignofnether.piglin_market.tooltip2").getVisualOrderText(),
                        FormattedCharSequence.EMPTY,
                        Component.translatable("buildings.reignofnether.piglin_market.tooltip3").getVisualOrderText()
                ),
                this
        );
    }

    @Override
    public void onBuilt(BuildingPlacement buildingPlacement) {
        super.onBuilt(buildingPlacement);
        setNetherZone(buildingPlacement, new NetherZone(buildingPlacement.centrePos.offset(0, -2, 0), getMaxNetherRange(buildingPlacement), getStartingNetherRange(buildingPlacement)), true);
    }

    @Override
    public double getMaxNetherRange(BuildingPlacement placement) {
        return 20;
    }

    @Override
    public double getStartingNetherRange(BuildingPlacement placement) {
        return 3;
    }
}