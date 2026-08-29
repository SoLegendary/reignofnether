package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class ArcaneTower extends ProductionBuilding {

    public final static String buildingName = "Arcane Tower";
    public final static String structureName = "arcane_tower";
    public final static ResourceCost cost = ResourceCosts.ARCANE_TOWER;

    public ArcaneTower() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.AMETHYST_BLOCK;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/amethyst_block.png");

        this.startingBlockTypes.add(Blocks.STONE_BRICKS);
        this.startingBlockTypes.add(Blocks.ANDESITE_WALL);
        this.startingBlockTypes.add(Blocks.POLISHED_ANDESITE_STAIRS);
        this.startingBlockTypes.add(Blocks.POLISHED_ANDESITE);

        this.buildTimeModifier = 0.7f;
        this.explodeChance = 0.2f;
        this.maxHealth = 340d;

        this.productions.add(ProductionItems.EVOKER, Keybindings.abilitySlot1);
        this.productions.add(ProductionItems.WINDCALLER, Keybindings.abilitySlot2);
    }


    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(
            name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/amethyst_block.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.ARCANE_TOWER,
            TutorialClientEvents::isEnabled,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.BARRACKS) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(
                Component.translatable("buildings.reignofnether.arcane_tower").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.arcane_tower.tooltip1").getVisualOrderText(),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.arcane_tower.tooltip2").getVisualOrderText()
            ),
            this
        );
    }

    @Override
    public BlockPos getIndoorSpawnPoint(ServerLevel level, BuildingPlacement placement) {
        return super.getIndoorSpawnPoint(level, placement).offset(0,-10,0);
    }
}
