package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.shared.AbstractMarket;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class VillagerMarket extends AbstractMarket {

    public static final String buildingName = "Town Market";
    public static final String structureName = "market_villagers";
    public static final ResourceCost cost = ResourceCosts.VILLAGER_MARKET;

    public VillagerMarket() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.EMERALD_BLOCK;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/emerald_block.png");

        this.buildTimeModifier = 0.8f;
        this.maxHealth = 300d;

        this.startingBlockTypes.add(Blocks.COBBLESTONE);
        this.startingBlockTypes.add(Blocks.STONE);
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/emerald_block.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                TutorialClientEvents::isEnabled,
                () -> BuildingClientEvents.numFinishedBuildings(Buildings.VILLAGER_HOUSE) >= 6 ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        Component.translatable("buildings.reignofnether.villager_market").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.EMPTY,
                        Component.translatable("buildings.reignofnether.villager_market.tooltip1").getVisualOrderText(),
                        FormattedCharSequence.EMPTY,
                        Component.translatable("buildings.reignofnether.villager_market.tooltip2").getVisualOrderText()
                ),
                this
        );
    }
}