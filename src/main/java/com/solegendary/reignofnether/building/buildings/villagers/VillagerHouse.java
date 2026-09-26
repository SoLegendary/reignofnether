package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.Buildings;
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

public class VillagerHouse extends Building {

    public final static String buildingName = "Villager House";
    public final static String structureName = "villager_house";
    public final static ResourceCost cost = ResourceCosts.VILLAGER_HOUSE;

    public VillagerHouse() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.OAK_LOG;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/oak_log.png");

        this.buildTimeModifier = 0.8f;
        this.maxHealth = 175d;

        this.startingBlockTypes.add(Blocks.SPRUCE_PLANKS);
        this.startingBlockTypes.add(Blocks.OAK_PLANKS);
        this.startingBlockTypes.add(Blocks.OAK_LOG);

        this.maxHealth = 125;
    }


    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(
            name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/oak_log.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.VILLAGER_HOUSE,
            () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.EXPLAIN_BUILDINGS),
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.TOWN_CENTRE) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(
                Component.translatable("buildings.reignofnether.villager_house").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPop(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.villager_house.tooltip1").getVisualOrderText()
            ),
            this
        );
    }
}
