package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.shared.AbstractMarket;
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

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

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

        this.startingBlockTypes.add(Blocks.COBBLESTONE);
        this.startingBlockTypes.add(Blocks.STONE);
    }

    public Faction getFaction() { return Faction.VILLAGERS; }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/emerald_block.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.EXPLAIN_BUILDINGS),
                () -> BuildingClientEvents.numFinishedBuildings(Buildings.VILLAGER_HOUSE) >= 6 ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        fcs(I18n.get("buildings.reignofnether.villager_market"), true),
                        ResourceCosts.getFormattedCost(cost),
                        fcs(""),
                        fcs(I18n.get("buildings.reignofnether.villager_market.tooltip1")),
                        fcs(""),
                        fcs(I18n.get("buildings.reignofnether.villager_market.tooltip2"))
                ),
                this
        );
    }
}